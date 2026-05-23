package com.larena.boxbreaker.plugin.bbk;

import com.intellij.lexer.FlexAdapter;

public class BbkLexerAdapter extends FlexAdapter {

    public BbkLexerAdapter() {
        super(new _BbkLexer(null));
    }
}
