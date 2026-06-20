package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Contexto de suspensión: expone la pila activa cuando el programa está pausado. */
final class BbkSuspendContext extends XSuspendContext {

    private final BbkExecutionStack stack;

    BbkSuspendContext(@NotNull BbkExecutionStack stack) {
        this.stack = stack;
    }

    @Override
    public @Nullable XExecutionStack getActiveExecutionStack() {
        return stack;
    }
}
