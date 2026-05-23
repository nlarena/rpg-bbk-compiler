package com.larena.boxbreaker.plugin.bbk;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public final class BbkFileType extends LanguageFileType {

    public static final BbkFileType INSTANCE = new BbkFileType();

    private static final Icon ICON = IconLoader.getIcon("/icons/bbk.svg", BbkFileType.class);

    private BbkFileType() {
        super(BbkLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "BBK File";
    }

    @Override
    public @NotNull String getDescription() {
        return "BoxBreaker source file";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "bbk";
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }
}
