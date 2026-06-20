package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import com.larena.boxbreaker.core.backend.jvm.BbkRunner;
import org.jetbrains.annotations.NotNull;

/**
 * Acción "Run BBK": ejecuta el archivo BBK abierto con el <b>backend JVM</b> de
 * bbk-core (cobertura completa del lenguaje no-SO: incluye DS, arrays, decimales)
 * y muestra la salida del programa. Es el "correr" de verdad; "Trace BBK" es la
 * vista de debug paso a paso (subconjunto).
 */
public final class RunBbkAction extends AnAction {

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

        String body;
        try {
            String out = BbkRunner.compileAndRun(source);
            body = out.isEmpty() ? "(el programa no produjo salida)" : out;
        } catch (Throwable t) {   // error de compilación o de ejecución
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
            body = "No se pudo ejecutar:\n\n" + msg;
        }

        new BbkOutputDialog(e.getProject(), "Run BBK — " + name, body).show();
    }
}
