package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.larena.boxbreaker.jvmdebug.BbkFrame;
import com.larena.boxbreaker.jvmdebug.BbkVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Un frame del debugger: su posición (línea actual) + las variables visibles.
 *
 * <p>Las variables se leen <b>perezosamente</b> en {@link #computeChildren} (que el IDE
 * llama en su propio hilo), no al pausar. Leerlas al pausar, desde el hilo del event
 * loop de JDI, puede invocar métodos en la VM remota (p.ej. formatear decimales) y
 * colgar la sesión.
 */
final class BbkStackFrame extends XStackFrame {

    private final XSourcePosition position;
    private final BbkFrame frame;
    private final Function<String, String> evaluator;

    BbkStackFrame(@Nullable XSourcePosition position, @Nullable BbkFrame frame,
                  @Nullable Function<String, String> evaluator) {
        this.position = position;
        this.frame = frame;
        this.evaluator = evaluator;
    }

    @Override
    public @Nullable XSourcePosition getSourcePosition() {
        return position;
    }

    @Override
    public @Nullable XDebuggerEvaluator getEvaluator() {
        return evaluator == null ? null : new BbkXEvaluator(evaluator);
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        XValueChildrenList list = new XValueChildrenList();
        if (frame != null) {
            for (BbkVariable v : frame.variables()) list.add(new BbkValue(v));   // lectura perezosa, en hilo del IDE
        }
        node.addChildren(list, true);
    }
}
