package com.larena.boxbreaker.plugin.bbk.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;

public class BbkParserUtil extends GeneratedParserUtilBase {

    public static boolean consumeAnyToken(PsiBuilder builder, int level) {
        if (builder.eof()) {
            return false;
        }
        builder.advanceLexer();
        return true;
    }
}
