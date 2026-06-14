package com.larena.boxbreaker.rpg.parser;

import com.larena.boxbreaker.rpg.lexer.RpgToken;

/**
 * Thrown when the token stream does not match the grammar. Carries the offending
 * token so callers can point at the 1-based line/column.
 */
public class RpgParseException extends RuntimeException {

    private final RpgToken token;

    public RpgParseException(String message, RpgToken token) {
        super(message + " (at " + token.line() + ":" + token.column()
            + ", got " + token.type() + " '" + token.text() + "')");
        this.token = token;
    }

    public RpgToken getToken() {
        return token;
    }
}
