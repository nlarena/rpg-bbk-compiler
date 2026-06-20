package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

/** Utilidades compartidas por las acciones del debugger (Run / Trace). */
final class BbkActions {

    private BbkActions() {}

    static boolean isBbkFile(@Nullable PsiFile file) {
        return file != null && file.getName().endsWith(".bbk");
    }

    /** El texto del .bbk abierto (del editor si está, si no del PSI). null si no es un .bbk. */
    static @Nullable String readSource(AnActionEvent e) {
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (!isBbkFile(file)) return null;
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        return editor != null ? editor.getDocument().getText() : file.getText();
    }
}
