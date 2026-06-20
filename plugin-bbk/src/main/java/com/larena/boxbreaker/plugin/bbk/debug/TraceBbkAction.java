package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import com.larena.boxbreaker.debugger.BbkDebugger;
import com.larena.boxbreaker.debugger.DebugListener;
import com.larena.boxbreaker.debugger.DebugResult;
import com.larena.boxbreaker.debugger.TraceStep;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Acción "Trace BBK": ejecuta el archivo BBK abierto con el intérprete del módulo
 * {@code bbk-debugger} y muestra la traza paso a paso (sentencia + variables +
 * salida). Primer eslabón de la integración del debugger al IDE; la vista
 * dockeable (ToolWindow) y el debug interactivo (breakpoints/step) vienen después.
 */
public final class TraceBbkAction extends AnAction {

    /** Tope de pasos: evita que un programa con loop infinito cuelgue el IDE. */
    private static final int STEP_CAP = 50_000;

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(BbkActions.isBbkFile(e.getData(CommonDataKeys.PSI_FILE)));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String source = BbkActions.readSource(e);
        if (source == null) return;
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        String name = file != null ? file.getName() : "BBK";

        int[] count = {0};
        DebugResult result = BbkDebugger.run(source, step ->
            count[0]++ < STEP_CAP ? DebugListener.Decision.CONTINUE : DebugListener.Decision.STOP);

        new BbkOutputDialog(e.getProject(), "Trace BBK — " + name, render(result)).show();
    }

    private static String render(DebugResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Traza ===\n");
        for (TraceStep s : result.steps()) {
            String indent = "  ".repeat(s.depth());
            String loc = s.line() > 0 ? "L" + s.line() : "  -";
            sb.append(String.format("%3d  %-4s %s%s%n", s.step(), loc, indent, s.statement()));
            sb.append("     ").append(indent).append("    { ").append(vars(s.variables())).append(" }\n");
            if (!s.output().isEmpty()) {
                sb.append("     ").append(indent).append("    >> ").append(s.output().replace("\n", "\\n")).append('\n');
            }
        }
        if (result.steps().size() >= STEP_CAP) {
            sb.append("\n(detenido tras ").append(STEP_CAP).append(" pasos — ¿loop infinito?)\n");
        }

        sb.append("\n=== Salida ===\n").append(result.output());
        if (!result.output().endsWith("\n")) sb.append('\n');

        if (!result.ok()) {
            sb.append("\n=== Error ===\n").append(result.error()).append('\n');
        }
        return sb.toString();
    }

    private static String vars(Map<String, String> variables) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }
}
