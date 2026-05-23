package com.larena.boxbreaker.plugin.bbk.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.larena.boxbreaker.plugin.bbk.BbkFileType;
import com.larena.boxbreaker.plugin.bbk.BbkLanguage;
import org.jetbrains.annotations.NotNull;

public class BbkFile extends PsiFileBase {

    public BbkFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, BbkLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return BbkFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "BBK File";
    }
}
