package com.larena.boxbreaker.rpg.lexer;

/**
 * Thrown when the lexer hits a character it cannot turn into a token.
 * Carries the 1-based line/column so callers can point at the offending spot.
 */
public class RpgLexException extends RuntimeException {

    private final int line;
    private final int column;

    public RpgLexException(String message, int line, int column) {
        super(message + " (at " + line + ":" + column + ")");
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
