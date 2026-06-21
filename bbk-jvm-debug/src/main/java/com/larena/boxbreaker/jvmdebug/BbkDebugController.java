package com.larena.boxbreaker.jvmdebug;

import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orquesta la sesión de debug sobre el bytecode real. Instala breakpoints (diferidos
 * hasta que se prepara {@code bbk.Main}, y en vivo después), corre el event loop de
 * JDI en su propio hilo y traduce cada parada a {@link BbkDebugListener#onPaused}.
 * Maneja step nativo (over/into/out), breakpoints condicionales, el resume, y vuelca
 * la salida del programa por {@link BbkDebugListener#onOutput}.
 *
 * <p>El {@code listener} se invoca desde el hilo del event loop; los controles
 * ({@link #resume()}, {@link #stepOver()}, …) se llaman desde afuera con la VM
 * suspendida. Una parada <b>no</b> reanuda su {@code EventSet}: la VM queda suspendida
 * hasta que el control de turno la reanuda.
 */
public final class BbkDebugController implements AutoCloseable {

    private final BbkDebuggee debuggee;
    private final BbkDebugSession session;
    private final BbkDebugListener listener;

    private final Object bpLock = new Object();
    private final Map<Integer, String> conditions = new ConcurrentHashMap<>();          // línea -> condición ("" = sin condición)
    private final Map<Integer, BreakpointRequest> installed = new ConcurrentHashMap<>(); // línea -> request instalado
    private final AtomicBoolean exited = new AtomicBoolean();

    private volatile ReferenceType mainType;     // != null una vez preparada bbk.Main
    private volatile BbkLineMap lineMap;
    private volatile Thread loop;
    private volatile Thread outputPump;
    private volatile boolean stopped;
    private volatile ThreadReference pausedThread;

    public BbkDebugController(BbkDebuggee debuggee, BbkDebugSession session, BbkDebugListener listener) {
        this.debuggee = debuggee;
        this.session = session;
        this.listener = listener;
    }

    /** Compila, forkea la JVM de debug y se conecta por JDI; queda listo para breakpoints + {@link #start()}. */
    public static BbkDebugController launch(String bbkSource, BbkDebugListener listener) throws IOException {
        return launch(bbkSource, List.of(), listener);
    }

    /** Igual, combinando el fuente principal con otros {@code .bbk} (declaraciones cross-file). */
    public static BbkDebugController launch(String mainSource, List<String> otherSources, BbkDebugListener listener) throws IOException {
        BbkDebuggee debuggee = BbkDebuggee.launch(mainSource, otherSources);
        try {
            BbkDebugSession session = BbkDebugSession.attach(debuggee.host(), debuggee.port());
            return new BbkDebugController(debuggee, session, listener);
        } catch (IOException e) {
            debuggee.close();
            throw e;
        }
    }

    /** Breakpoint en una línea BBK (1-based), sin condición. */
    public void addBreakpoint(int line) {
        addBreakpoint(line, null);
    }

    /** Breakpoint en una línea BBK con condición opcional ({@code null}/vacía = incondicional). */
    public void addBreakpoint(int line, String condition) {
        synchronized (bpLock) {
            conditions.put(line, condition == null ? "" : condition);
            if (mainType != null) install(line);          // ya preparada: instalar en vivo
        }
    }

    /** Quita el breakpoint de una línea. */
    public void removeBreakpoint(int line) {
        synchronized (bpLock) {
            conditions.remove(line);
            BreakpointRequest req = installed.remove(line);
            if (req != null) {
                req.disable();
                session.vm().eventRequestManager().deleteEventRequest(req);
            }
        }
    }

    /** Pide el ClassPrepare de bbk.Main, arranca el event loop y el volcado de salida. */
    public void start() {
        session.requestMainClassPrepare();
        loop = daemon(this::pump, "bbk-jdi-events");
        outputPump = daemon(this::pumpOutput, "bbk-output");
        loop.start();
        outputPump.start();
    }

    // ----- event loop -----

    private void pump() {
        EventQueue queue = session.vm().eventQueue();
        try {
            while (!stopped) {
                EventSet set = queue.remove();
                boolean resume = true;
                for (Event e : set) {
                    if (e instanceof ClassPrepareEvent cpe) {
                        onClassPrepare(cpe.referenceType());
                    } else if (e instanceof BreakpointEvent be) {
                        if (shouldPause(be)) { resume = false; pause(be.thread()); }
                    } else if (e instanceof StepEvent se) {
                        session.vm().eventRequestManager().deleteEventRequest(se.request());
                        resume = false;
                        pause(se.thread());
                    } else if (e instanceof VMDeathEvent || e instanceof VMDisconnectEvent) {
                        exit();
                        return;
                    }
                }
                if (resume) set.resume();      // VMStart/ClassPrepare/condición-falsa: reanudar; las paradas no
            }
        } catch (VMDisconnectedException e) {
            exit();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void onClassPrepare(ReferenceType type) {
        synchronized (bpLock) {
            mainType = type;
            lineMap = new BbkLineMap(type);
            for (int line : conditions.keySet()) install(line);
        }
    }

    /** Instala (o re-instala) el breakpoint de una línea. Debe llamarse con {@link #bpLock}. */
    private void install(int line) {
        if (lineMap == null || installed.containsKey(line)) return;
        List<Location> locs = lineMap.locationsOfLine(line);
        if (locs.isEmpty()) return;                       // la línea no tiene código: se ignora
        BreakpointRequest bp = session.vm().eventRequestManager().createBreakpointRequest(locs.get(0));
        bp.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        bp.enable();
        installed.put(line, bp);
    }

    /** ¿Frenar en este breakpoint? Evalúa la condición (si tiene) en el frame del evento. */
    private boolean shouldPause(BreakpointEvent be) {
        String cond = conditions.get(be.location().lineNumber());
        if (cond == null || cond.isBlank()) return true;
        try {
            return BbkExprEval.forFrame(be.thread(), be.thread().frame(0)).evaluateCondition(cond);
        } catch (Exception e) {
            return true;                                  // condición inválida: frenar igual (más seguro)
        }
    }

    private void pause(ThreadReference thread) {
        pausedThread = thread;
        try {
            listener.onPaused(new BbkPausedContext(thread));
        } catch (RuntimeException e) {
            // un fallo del listener no debe matar el event loop (dejaría la sesión colgada)
        }
    }

    private void pumpOutput() {
        try {
            BufferedReader reader = debuggee.output();
            String line;
            while ((line = reader.readLine()) != null) listener.onOutput(line + "\n");
        } catch (IOException ignored) {
            // el proceso terminó / se cerró el stream
        }
    }

    // ----- controles (desde afuera, con la VM suspendida) -----

    /** Continúa hasta el próximo breakpoint o el final. */
    public synchronized void resume() {
        pausedThread = null;
        session.vm().resume();
    }

    /** Ejecuta la línea actual sin entrar a los procedimientos que llame. */
    public void stepOver() {
        requestStep(StepRequest.STEP_OVER);
    }

    /** Entra al procedimiento que llame la línea actual. */
    public void stepInto() {
        requestStep(StepRequest.STEP_INTO);
    }

    /** Sale del procedimiento actual y frena en quien lo llamó. */
    public void stepOut() {
        requestStep(StepRequest.STEP_OUT);
    }

    private synchronized void requestStep(int depth) {
        ThreadReference thread = pausedThread;
        if (thread == null) return;
        EventRequestManager erm = session.vm().eventRequestManager();
        for (StepRequest sr : new ArrayList<>(erm.stepRequests())) {   // JDI: un solo StepRequest por thread
            if (thread.equals(sr.thread())) erm.deleteEventRequest(sr);
        }
        StepRequest step = erm.createStepRequest(thread, StepRequest.STEP_LINE, depth);
        step.addCountFilter(1);
        step.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        step.enable();
        pausedThread = null;
        session.vm().resume();
    }

    private void exit() {
        if (exited.getAndSet(true)) return;
        int code = 0;
        try {
            if (debuggee.process().waitFor(5, TimeUnit.SECONDS)) code = debuggee.process().exitValue();
            Thread pump = outputPump;
            if (pump != null) pump.join(2000);            // drenar la salida pendiente antes de avisar
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        listener.onExited(code);
    }

    @Override
    public void close() {
        stopped = true;
        interrupt(loop);
        interrupt(outputPump);
        session.close();
        debuggee.close();
    }

    private static Thread daemon(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }

    private static void interrupt(Thread t) {
        if (t != null) t.interrupt();
    }
}
