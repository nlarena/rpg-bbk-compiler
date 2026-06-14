package com.larena.boxbreaker.rpg.lexer;

/**
 * Token categories for free-format RPGLE.
 *
 * <p>Grounded in {@code docs/theory/rpgle-grammar.md} §2 (Lexicon) and §4.4
 * (free-form expressions). Covers the full free-form lexical surface; the
 * parser implements grammar incrementally (slice by slice), but the lexer
 * classifies every token from day one.
 *
 * <p>Out of scope on purpose: fixed-form positional specs and legacy C-spec
 * opcodes ({@code Z-ADD}, {@code MOVE}, {@code CASxx}, {@code GOTO} ...). Those
 * belong to fixed-form, which we do not target.
 *
 * <p>Known deviation from real RPGLE: compound assignment ({@code += -= *= /=})
 * exists since IBM i 7.1 but is NOT in the grammar doc's assignment rule
 * (§4.3.1), so it is deliberately absent here until we decide to add it.
 */
public enum RpgTokenType {

    // ----- Names and words -----
    IDENT,          // letter then { letter | digit | _ | # | $ | @ }*
    KEYWORD,        // reserved words (§2.3) + free-form opcodes (§2.4) + AND/OR/NOT/XOR
    BIF,            // %identifier  (built-in function: %trim, %len, ...)

    // ----- Literals (§2.6) -----
    INT_LITERAL,        // 0, 100
    DEC_LITERAL,        // 199.95
    STRING_LITERAL,     // 'Alice'   (character literal; '' is an escaped quote)
    HEX_LITERAL,        // X'1F'
    DATE_LITERAL,       // D'2024-01-01'
    TIME_LITERAL,       // T'14.30.00'
    TIMESTAMP_LITERAL,  // Z'2024-01-01-14.30.00.000000'
    UCS2_LITERAL,       // U'00410042'
    GRAPHIC_LITERAL,    // G'....'
    STAR_NAME,          // *ON, *OFF, *BLANKS, *NULL, *INLR, *IN01  (figurative consts + indicators)

    // ----- Operators (§2.7 + §4.4) -----
    EQ,          // =
    NE,          // <>
    LT,          // <
    GT,          // >
    LE,          // <=
    GE,          // >=
    PLUS,        // +
    MINUS,       // -
    STAR,        // *   (multiplication)
    SLASH,       // /
    STARSTAR,    // **  (power, right-associative)

    // ----- Punctuators (§2.8 + §4.4) -----
    LPAREN,      // (
    RPAREN,      // )
    COLON,       // :   (argument / subscript separator)
    COMMA,       // ,
    SEMI,        // ;   (statement terminator)
    DOT,         // .   (qualified DS access: customer.id)

    // ----- Directives and end -----
    FREE_DIRECTIVE,  // **FREE
    DIRECTIVE,       // compiler directive line: /COPY, /INCLUDE, /IF, /ELSEIF, /ELSE,
                     //   /ENDIF, /DEFINE, /UNDEFINE, /EOF, /FREE, /END-FREE
    EOF
}
