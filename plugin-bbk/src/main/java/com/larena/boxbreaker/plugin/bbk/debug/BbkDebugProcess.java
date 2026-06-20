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
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.larena.boxbreaker.debugger.BbkDebugger;
import com.larena.boxbreaker.debugger.DebugListener;
import com.larena.boxbreaker.debugger.DebugResult;
import com.larena.boxbreaker.debugger.NamedSource;
import com.larena.boxbreaker.debugger.TraceStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Proceso de debug de BBK: breakpoints (incluso cross-file), panel de Variables,
 * step over/into/out y la salida del programa en la consola.
 *
 * <p>Junta todos los .bbk de la carpeta del archivo en un solo programa (los
 * símbolos cruzados resuelven), corre el intérprete en un hilo y, en cada paso,
 * decide si pausar (breakpoint o step). La pausa bloquea el hilo del intérprete
 * hasta Resume/Step — la costura del {@link DebugListener}.
 */
public final class BbkDebugProcess extends XDebugProcess {

    private enum StepMode { NONE, INTO, OVER, OUT }

    private final String filePath;
    private final List<VirtualFile> bbkFiles = new ArrayList<>();
    private final Map<String, VirtualFile> byPath = new HashMap<>();
    private final BbkDebuggerEditorsProvider editorsProvider = new BbkDebuggerEditorsProvider();

    private final Map<String, Set<Integer>> breakpointsByFile = new ConcurrentHashMap<>();   // path -> 1-based lines
    private final BlockingQueue<Object> resumeSignal = new LinkedBlockingQueue<>();

    private volatile StepMode stepMode = StepMode.NONE;
    private volatile int stepBaseDepth = 0;
    private volatile int currentDepth = 0;
    private volatile boolean stopped = false;
    private ConsoleView console;

    public BbkDebugProcess(@NotNull XDebugSession session, @NotNull String filePath) {
        super(session);
        this.filePath = filePath;
        VirtualFile main = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (main != null && main.getParent() != null) {
            for (VirtualFile child : main.getParent().getChildren()) {
                if ("bbk".equals(child.getExtension())) {
                    bbkFiles.add(child);
                    byPath.put(child.getPath(), child);
                }
            }
        }
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
                    if (p != null) {
                        breakpointsByFile.computeIfAbsent(p.getFile().getPath(), k -> ConcurrentHashMap.newKeySet())
                            .add(bp.getLine() + 1);
                    }
                }

                @Override
                public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> bp, boolean temporary) {
                    XSourcePosition p = bp.getSourcePosition();
                    if (p != null) {
                        Set<Integer> lines = breakpointsByFile.get(p.getFile().getPath());
                        if (lines != null) lines.remove(bp.getLine() + 1);
                    }
                }
            }
        };
    }

    @Override
    public void sessionInitialized() {
        ApplicationManager.getApplication().executeOnPooledThread(this::runProgram);
    }

    private void runProgram() {
        List<NamedSource> sources = new ArrayList<>();
        try {
            if (bbkFiles.isEmpty()) {
                sources.add(new NamedSource(filePath, Files.readString(Path.of(filePath))));
            } else {
                for (VirtualFile vf : bbkFiles) {
                    sources.add(new NamedSource(vf.getPath(), Files.readString(Path.of(vf.getPath()))));
                }
            }
        } catch (Exception e) {
            printError("No se pudo leer el fuente: " + e.getMessage());
            getSession().stop();
            return;
        }

        DebugResult result = BbkDebugger.runFiles(sources, this::onStep);
        if (!result.ok() && !stopped) {
            printError(result.error());
        }
        if (!stopped) {
            getSession().stop();
        }
    }

    /** Por cada sentencia, en el hilo del intérprete. */
    private DebugListener.Decision onStep(TraceStep step) {
        if (stopped) return DebugListener.Decision.STOP;

        print(step.output());

        boolean pause = switch (stepMode) {
            case INTO -> true;
            case OVER -> step.depth() <= stepBaseDepth;
            case OUT -> step.depth() < stepBaseDepth;
            case NONE -> false;
        };
        if (!pause) pause = hasBreakpoint(step);

        if (pause) {
            VirtualFile vf = byPath.get(step.file());
            if (vf == null || step.line() <= 0) return DebugListener.Decision.CONTINUE;   // sin posición: no para

            stepMode = StepMode.NONE;
            currentDepth = step.depth();
            XSourcePosition pos = XDebuggerUtil.getInstance().createPosition(vf, step.line() - 1);
            BbkStackFrame frame = new BbkStackFrame(pos, step.variables());
            getSession().positionReached(new BbkSuspendContext(new BbkExecutionStack(frame)));

            try {
                resumeSignal.take();
            } catch (InterruptedException e) {
                return DebugListener.Decision.STOP;
            }
            if (stopped) return DebugListener.Decision.STOP;
        }
        return DebugListener.Decision.CONTINUE;
    }

    private boolean hasBreakpoint(TraceStep step) {
        Set<Integer> lines = breakpointsByFile.get(step.file());
        return lines != null && step.line() > 0 && lines.contains(step.line());
    }

    // ----- controles -----

    @Override
    public void resume(@Nullable XSuspendContext context) {
        stepMode = StepMode.NONE;
        resumeSignal.offer(Boolean.TRUE);
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        step(StepMode.OVER);
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        step(StepMode.INTO);
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        step(StepMode.OUT);
    }

    private void step(StepMode mode) {
        stepMode = mode;
        stepBaseDepth = currentDepth;
        resumeSignal.offer(Boolean.TRUE);
    }

    @Override
    public void stop() {
        stopped = true;
        resumeSignal.offer(Boolean.TRUE);
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
