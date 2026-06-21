package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.larena.boxbreaker.jvmdebug.BbkVariable;
import org.jetbrains.annotations.NotNull;

/**
 * Una variable en el panel de Variables: un escalar (nombre + valor) o una
 * estructura de datos (nodo expandible con sus subcampos como hijos).
 */
final class BbkValue extends XNamedValue {

    private final BbkVariable variable;

    BbkValue(@NotNull BbkVariable variable) {
        super(variable.name());
        this.variable = variable;
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        if (variable.isComposite()) {
            node.setPresentation(AllIcons.Debugger.Value, "DS", "", true);   // estructura: tiene hijos
        } else {
            String value = variable.value() == null ? "*NULL" : variable.value();
            node.setPresentation(AllIcons.Debugger.Value, null, value, false);
        }
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        if (!variable.isComposite()) {
            super.computeChildren(node);
            return;
        }
        XValueChildrenList list = new XValueChildrenList();
        for (BbkVariable child : variable.children()) list.add(new BbkValue(child));
        node.addChildren(list, true);
    }
}
