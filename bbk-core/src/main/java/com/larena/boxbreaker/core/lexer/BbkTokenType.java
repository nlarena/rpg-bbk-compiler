package com.larena.boxbreaker.core.lexer;

/**
 * Token categories for BBK, grounded in the authoritative grammar
 * {@code plugin-bbk/src/main/grammar/BBK.bnf} (the {@code tokens} block).
 *
 * <p>BBK is <b>case-sensitive</b>: declaration keywords are upper-case
 * ({@code DCL-S}, {@code INT}), control flow is lower-case ({@code if},
 * {@code while}, {@code read}). Keywords are a single {@link #KEYWORD} category
 * recognised by exact spelling; operators and punctuators are distinct so the
 * parser builds expressions without re-reading text.
 */
public enum BbkTokenType {

    // ----- Names and literals -----
    IDENT,           // [a-zA-Z_][a-zA-Z0-9_]*
    STAR_IDENT,      // *name  (figurative constants, *INLR, ...)
    INT_LIT,         // 123
    INT_LIT_HEX,     // 0x1F
    INT_LIT_OCT,     // 0o17
    FLOAT_LIT,       // 1.5e3
    DEC_LIT,         // 199.95  or  199.95d
    STR_LIT,         // "..."

    // ----- Keywords (exact spelling) and attribute modifiers -----
    KEYWORD,         // DCL-S, INT, if, while, read, CTL-OPT, PRE-IF, true, ...
    ATTR,            // @halfup, @halfdown, @trunc

    // ----- Punctuators -----
    SEMI, COMMA, DOT, ARROW, LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, COLON, AT,

    // ----- Arithmetic -----
    PLUS, MINUS, STAR, SLASH, PERCENT, STAR_STAR,

    // ----- Comparison -----
    EQ_EQ, BANG_EQ, LT, GT, LT_EQ, GT_EQ,

    // ----- Logical -----
    AMP_AMP, PIPE_PIPE, BANG,

    // ----- Bitwise -----
    AMP, PIPE, CARET, TILDE, LT_LT, GT_GT,

    // ----- Assignment -----
    EQ, PLUS_EQ, MINUS_EQ, STAR_EQ, SLASH_EQ, PERCENT_EQ,
    AMP_EQ, PIPE_EQ, CARET_EQ, LT_LT_EQ, GT_GT_EQ,

    // ----- Ternary -----
    QUESTION,

    // ----- End -----
    EOF
}
