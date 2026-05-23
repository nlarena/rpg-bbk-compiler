package com.larena.boxbreaker.plugin.bbk;

import com.intellij.lang.Language;

public final class BbkLanguage extends Language {

    public static final BbkLanguage INSTANCE = new BbkLanguage();

    private BbkLanguage() {
        super("BBK");
    }
}
