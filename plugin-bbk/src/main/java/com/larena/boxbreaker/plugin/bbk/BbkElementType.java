package com.larena.boxbreaker.plugin.bbk;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class BbkElementType extends IElementType {

    public BbkElementType(@NotNull @NonNls String debugName) {
        super(debugName, BbkLanguage.INSTANCE);
    }
}
