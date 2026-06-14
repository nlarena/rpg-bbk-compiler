package com.larena.boxbreaker.core.parser;

import com.larena.boxbreaker.core.lexer.BbkToken;

/** Thrown when the BBK token stream does not match the grammar. */
public class BbkParseException extends RuntimeException {
    private final BbkToken token;

    public BbkParseException(String message, BbkToken token) {
        super(message + " (at " + token.line() + ":" + token.column()
            + ", got " + token.type() + " '" + token.text() + "')");
        this.token = token;
    }

    public BbkToken getToken() { return token; }
}
