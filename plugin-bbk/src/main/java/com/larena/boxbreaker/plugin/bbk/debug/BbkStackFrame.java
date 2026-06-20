package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** El (único) frame del debugger: su posición (línea actual) + las variables visibles. */
final class BbkStackFrame extends XStackFrame {

    private final XSourcePosition position;
    private final Map<String, String> variables;

    BbkStackFrame(@Nullable XSourcePosition position, @NotNull Map<String, String> variables) {
        this.position = position;
        this.variables = variables;
    }

    @Override
    public @Nullable XSourcePosition getSourcePosition() {
        return position;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        XValueChildrenList list = new XValueChildrenList();
        for (Map.Entry<String, String> e : variables.entrySet()) {
            list.add(new BbkValue(e.getKey(), e.getValue()));
        }
        node.addChildren(list, true);
    }
}
