package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;

/** Una variable en el panel de Variables del debugger: nombre + valor (ya renderizado). */
final class BbkValue extends XNamedValue {

    private final String value;

    BbkValue(@NotNull String name, String value) {
        super(name);
        this.value = value;
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        node.setPresentation(AllIcons.Debugger.Value, null, value == null ? "*NULL" : value, false);
    }
}
