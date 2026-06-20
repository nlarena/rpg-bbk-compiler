package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.larena.boxbreaker.plugin.bbk.BbkFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider mínimo para los editores del debugger (condición de breakpoint, etc.).
 * La evaluación de expresiones todavía no está soportada; alcanza con el tipo de
 * archivo y un documento plano.
 */
public final class BbkDebuggerEditorsProvider extends XDebuggerEditorsProvider {

    @Override
    public @NotNull FileType getFileType() {
        return BbkFileType.INSTANCE;
    }

    @Override
    public @NotNull Document createDocument(@NotNull Project project, @NotNull XExpression expression,
                                            @Nullable XSourcePosition sourcePosition, @NotNull EvaluationMode mode) {
        return EditorFactory.getInstance().createDocument(expression.getExpression());
    }
}
