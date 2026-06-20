package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Pila de ejecución del debugger BBK: por ahora un solo frame (el actual). */
final class BbkExecutionStack extends XExecutionStack {

    private final BbkStackFrame top;

    BbkExecutionStack(@NotNull BbkStackFrame top) {
        super("BBK");
        this.top = top;
    }

    @Override
    public @Nullable XStackFrame getTopFrame() {
        return top;
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, @NotNull XStackFrameContainer container) {
        container.addStackFrames(List.of(top), true);
    }
}
