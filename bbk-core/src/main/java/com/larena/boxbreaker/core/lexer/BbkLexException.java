package com.larena.boxbreaker.core.lexer;

/** Thrown when the BBK lexer hits a character it cannot tokenize. */
public class BbkLexException extends RuntimeException {
    private final int line;
    private final int column;

    public BbkLexException(String message, int line, int column) {
        super(message + " (at " + line + ":" + column + ")");
        this.line = line;
        this.column = column;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
}
