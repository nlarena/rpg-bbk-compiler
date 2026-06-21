package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Pila de ejecución del debugger BBK: el frame actual arriba y la cadena de llamadas debajo. */
final class BbkExecutionStack extends XExecutionStack {

    private final List<BbkStackFrame> frames;

    BbkExecutionStack(@NotNull List<BbkStackFrame> frames) {
        super("BBK");
        this.frames = frames;
    }

    @Override
    public @Nullable XStackFrame getTopFrame() {
        return frames.isEmpty() ? null : frames.get(0);
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, @NotNull XStackFrameContainer container) {
        if (firstFrameIndex < frames.size()) {
            container.addStackFrames(frames.subList(firstFrameIndex, frames.size()), true);
        } else {
            container.addStackFrames(List.of(), true);
        }
    }
}
