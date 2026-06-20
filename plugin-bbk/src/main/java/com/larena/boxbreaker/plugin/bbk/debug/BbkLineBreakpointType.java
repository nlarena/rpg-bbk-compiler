package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Permite poner breakpoints en el margen de los archivos .bbk. */
public final class BbkLineBreakpointType extends XLineBreakpointType<XBreakpointProperties> {

    public BbkLineBreakpointType() {
        super("bbk-line", "BBK Breakpoints");
    }

    @Override
    public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
        return "bbk".equals(file.getExtension());
    }

    @Override
    public @Nullable XBreakpointProperties createBreakpointProperties(@NotNull VirtualFile file, int line) {
        return null;   // sin propiedades extra (condición/log se manejan por el platform)
    }
}
