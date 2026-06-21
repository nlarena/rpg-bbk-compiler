package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.larena.boxbreaker.jvmdebug.BbkDebugController;
import com.larena.boxbreaker.jvmdebug.BbkDebugListener;
import com.larena.boxbreaker.jvmdebug.BbkFrame;
import com.larena.boxbreaker.jvmdebug.BbkPausedContext;
import com.larena.boxbreaker.jvmdebug.BbkPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proceso de debug de BBK <b>sobre el bytecode real</b>: orquesta un
 * {@link BbkDebugController} (que forkea la JVM con JDWP y se conecta por JDI) y
 * traduce sus eventos a la UI del XDebugger &mdash; breakpoints (con condición),
 * step over/into/out, panel de Variables, Evaluate/Watches y la salida en consola.
 *
 * <p>Hoy depura el archivo abierto (single-file). El multi-archivo (cross-file)
 * depende del SMAP del compilador, aún no implementado.
 */
public final class BbkDebugProcess extends XDebugProcess implements BbkDebugListener {

    private final String filePath;
    private final VirtualFile file;
    private final BbkDebuggerEditorsProvider editorsProvider = new BbkDebuggerEditorsProvider();
    // línea 1-based -> breakpoint (para leer su condición); también se reenvían al controller
    private final Map<Integer, XLineBreakpoint<XBreakpointProperties>> breakpoints = new ConcurrentHashMap<>();

    private ConsoleView console;
    private volatile BbkDebugController controller;

    public BbkDebugProcess(@NotNull XDebugSession session, @NotNull String filePath) {
        super(session);
        this.filePath = filePath;
        this.file = LocalFileSystem.getInstance().findFileByPath(filePath);
    }

    @Override
    public @NotNull XDebuggerEditorsProvider getEditorsProvider() {
        return editorsProvider;
    }

    @Override
    public @NotNull ExecutionConsole createConsole() {
        console = TextConsoleBuilderFactory.getInstance().createBuilder(getSession().getProject()).getConsole();
        return console;
    }

    @Override
    public XBreakpointHandler<?> @NotNull [] getBreakpointHandlers() {
        return new XBreakpointHandler<?>[]{
            new XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>>(BbkLineBreakpointType.class) {
                @Override
                public void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> bp) {
                    XSourcePosition p = bp.getSourcePosition();
                    if (p == null) return;
                    int line = p.getLine() + 1;
                    breakpoints.put(line, bp);
                    BbkDebugController c = controller;
                    if (c != null) c.addBreakpoint(line, conditionOf(bp));   // sesión ya viva: alta en vivo
                }

                @Override
                public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> bp, boolean temporary) {
                    XSourcePosition p = bp.getSourcePosition();
                    if (p == null) return;
                    int line = p.getLine() + 1;
                    breakpoints.remove(line);
                    BbkDebugController c = controller;
                    if (c != null) c.removeBreakpoint(line);
                }
            }
        };
    }

    @Override
    public void sessionInitialized() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String source = Files.readString(Path.of(filePath));
                BbkDebugController c = BbkDebugController.launch(source, siblingSources(), this);
                for (Map.Entry<Integer, XLineBreakpoint<XBreakpointProperties>> e : breakpoints.entrySet()) {
                    c.addBreakpoint(e.getKey(), conditionOf(e.getValue()));
                }
                controller = c;
                c.start();
            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                printError("No se pudo iniciar el debug: " + (cause.getMessage() != null ? cause.getMessage() : cause.toString()));
                printError(stackTraceOf(cause));     // detalle completo para diagnosticar
                getSession().stop();
            }
        });
    }

    /** Los demás {@code .bbk} de la carpeta: aportan declaraciones cross-file (DS, prototipos). */
    private List<String> siblingSources() {
        List<String> others = new ArrayList<>();
        if (file == null || file.getParent() == null) return others;
        for (VirtualFile child : file.getParent().getChildren()) {
            if ("bbk".equals(child.getExtension()) && !child.getPath().equals(filePath)) {
                try {
                    others.add(Files.readString(Path.of(child.getPath())));
                } catch (Exception ignored) {
                    // archivo ilegible: lo salteamos
                }
            }
        }
        return others;
    }

    // ----- eventos del motor (BbkDebugListener) -----

    @Override
    public void onPaused(BbkPausedContext ctx) {
        // solo posiciones acá (barato); las variables las lee cada frame perezosamente, en el hilo del IDE
        List<BbkStackFrame> frames = new ArrayList<>();
        for (BbkFrame f : ctx.frames()) {
            frames.add(new BbkStackFrame(positionOf(f.position()), f, ctx::evaluate));
        }
        if (frames.isEmpty()) {
            frames.add(new BbkStackFrame(positionOf(ctx.position()), null, ctx::evaluate));
        }
        getSession().positionReached(new BbkSuspendContext(new BbkExecutionStack(frames)));
    }

    @Override
    public void onExited(int exitCode) {
        if (exitCode != 0) printError("El programa terminó con código " + exitCode);
        getSession().stop();
    }

    @Override
    public void onOutput(String text) {
        print(text);
    }

    private @Nullable XSourcePosition positionOf(@Nullable BbkPosition pos) {
        if (pos == null || file == null || pos.line() <= 0) return null;
        return XDebuggerUtil.getInstance().createPosition(file, pos.line() - 1);
    }

    private static @Nullable String conditionOf(XLineBreakpoint<XBreakpointProperties> bp) {
        XExpression c = bp.getConditionExpression();
        return c == null ? null : c.getExpression();
    }

    private static String stackTraceOf(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    // ----- controles -----

    @Override
    public void resume(@Nullable XSuspendContext context) {
        BbkDebugController c = controller;
        if (c != null) c.resume();
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        BbkDebugController c = controller;
        if (c != null) c.stepOver();
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        BbkDebugController c = controller;
        if (c != null) c.stepInto();
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        BbkDebugController c = controller;
        if (c != null) c.stepOut();
    }

    @Override
    public void stop() {
        BbkDebugController c = controller;
        if (c != null) c.close();
    }

    // ----- salida en consola -----

    private void print(String text) {
        ConsoleView c = console;
        if (c != null && text != null && !text.isEmpty()) {
            ApplicationManager.getApplication().invokeLater(() -> c.print(text, ConsoleViewContentType.NORMAL_OUTPUT));
        }
    }

    private void printError(String text) {
        ConsoleView c = console;
        if (c != null && text != null && !text.isEmpty()) {
            ApplicationManager.getApplication().invokeLater(() -> c.print(text + "\n", ConsoleViewContentType.ERROR_OUTPUT));
        }
    }
}
