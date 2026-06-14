package com.larena.boxbreaker.rpg.lexer;

/**
 * A single lexical token.
 *
 * @param type   the token category
 * @param text   the exact source text (original case preserved)
 * @param line   1-based line number where the token starts
 * @param column 1-based column number where the token starts
 */
public record RpgToken(RpgTokenType type, String text, int line, int column) {

    /** Case-insensitive keyword check. */
    public boolean isKeyword(String kw) {
        return type == RpgTokenType.KEYWORD && text.equalsIgnoreCase(kw);
    }

    @Override
    public String toString() {
        return type + "('" + text + "')@" + line + ":" + column;
    }
}
