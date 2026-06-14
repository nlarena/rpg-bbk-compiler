package com.larena.boxbreaker.core.lexer;

/**
 * A single BBK token: its category, exact source text, and 1-based position.
 */
public record BbkToken(BbkTokenType type, String text, int line, int column) {

    public boolean isKeyword(String kw) {
        return type == BbkTokenType.KEYWORD && text.equals(kw);   // BBK is case-sensitive
    }

    @Override
    public String toString() {
        return type + "('" + text + "')@" + line + ":" + column;
    }
}
