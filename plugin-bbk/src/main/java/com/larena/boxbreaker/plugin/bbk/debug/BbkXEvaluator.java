package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.larena.boxbreaker.jvmdebug.BbkVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Evaluador del XDebugger (el cuadro "Evaluate" y los Watches): delega en el
 * evaluador de expresiones BBK del frame pausado, que lee los valores por JDI.
 */
final class BbkXEvaluator extends XDebuggerEvaluator {

    private final Function<String, String> evaluator;

    BbkXEvaluator(@NotNull Function<String, String> evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public void evaluate(@NotNull String expression, @NotNull XEvaluationCallback callback,
                         @Nullable XSourcePosition expressionPosition) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String value = evaluator.apply(expression);
                callback.evaluated(new BbkValue(new BbkVariable(expression, value, List.of())));
            } catch (Throwable t) {
                callback.errorOccurred(t.getMessage() != null ? t.getMessage() : "no se pudo evaluar");
            }
        });
    }
}
