package com.larena.boxbreaker.rpg.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written scanner for free-format RPGLE.
 *
 * <p>Turns raw source text into a flat {@link RpgToken} list. It does not
 * understand grammar — it only recognises lexical pieces (identifiers,
 * keywords, BIFs, literals, operators). The {@code RpgParser} consumes this
 * list. Token categories are grounded in {@code docs/theory/rpgle-grammar.md}
 * §2 (Lexicon) and §4.4 (expressions).
 *
 * <p>RPG is case-insensitive, so keyword matching is case-insensitive; the
 * original text is preserved in each token for faithful error messages.
 */
public final class RpgLexer {

    private final String src;
    private int pos = 0;     // index into src
    private int line = 1;    // 1-based
    private int col = 1;     // 1-based
    private RpgTokenType prev = null;   // last emitted token, for context-sensitive '*'

    public RpgLexer(String src) {
        this.src = src;
    }

    /** Convenience: lex a whole source string in one call. */
    public static List<RpgToken> tokenize(String src) {
        return new RpgLexer(src).lex();
    }

    public List<RpgToken> lex() {
        List<RpgToken> out = new ArrayList<>();
        RpgToken t;
        do {
            t = next();
            out.add(t);
        } while (t.type() != RpgTokenType.EOF);
        return out;
    }

    // -----------------------------------------------------------------------
    // Core dispatch
    // -----------------------------------------------------------------------

    private RpgToken next() {
        RpgToken t = scan();
        prev = t.type();
        return t;
    }

    private RpgToken scan() {
        skipTrivia();
        if (atEnd()) return make(RpgTokenType.EOF, "", line, col);

        int sLine = line, sCol = col;
        char c = peek();

        // **FREE directive — must be checked before the ** operator.
        if (c == '*' && matches("**FREE")) {
            return take(RpgTokenType.FREE_DIRECTIVE, "**FREE".length(), sLine, sCol);
        }
        // Typed literals: X'..' D'..' T'..' Z'..' U'..' G'..'  (prefix letter + quote)
        if (isTypedLiteralPrefix(c) && peekAt(1) == '\'') {
            return typedLiteral(sLine, sCol);
        }
        if (isWordStart(c)) return word(sLine, sCol);
        if (c == '%') return bif(sLine, sCol);
        if (c == '*') return star(sLine, sCol);
        if (c == '/') return slashOrDirective(sLine, sCol);
        if (Character.isDigit(c)) return number(sLine, sCol);
        if (c == '\'') return string(sLine, sCol);
        return operator(sLine, sCol);
    }

    // -----------------------------------------------------------------------
    // Identifiers and keywords (incl. hyphenated opcodes like dcl-s, end-if)
    // -----------------------------------------------------------------------

    private RpgToken word(int sLine, int sCol) {
        int start = pos;
        while (!atEnd() && isWordPart(peek())) advance();

        // Extend across a single hyphen if it forms a known keyword (dcl-s, on-error).
        if (!atEnd() && peek() == '-' && isWordStart(peekAt(1))) {
            int save = pos, saveLine = line, saveCol = col;
            advance(); // '-'
            while (!atEnd() && isWordPart(peek())) advance();
            String combined = src.substring(start, pos);
            if (RpgKeywords.isKeyword(combined)) {
                return make(RpgTokenType.KEYWORD, combined, sLine, sCol);
            }
            pos = save; line = saveLine; col = saveCol; // roll back: hyphen is MINUS
        }

        String text = src.substring(start, pos);
        RpgTokenType type = RpgKeywords.isKeyword(text)
            ? RpgTokenType.KEYWORD : RpgTokenType.IDENT;
        return make(type, text, sLine, sCol);
    }

    // -----------------------------------------------------------------------
    // Built-in functions: %identifier
    // -----------------------------------------------------------------------

    private RpgToken bif(int sLine, int sCol) {
        int start = pos;
        advance(); // '%'
        while (!atEnd() && isWordPart(peek())) advance();
        return make(RpgTokenType.BIF, src.substring(start, pos), sLine, sCol);
    }

    // -----------------------------------------------------------------------
    // Star: figurative/indicator name (*ON, *INLR, *IN01), ** (power), or * (mult)
    // -----------------------------------------------------------------------

    private RpgToken star(int sLine, int sCol) {
        if (peekAt(1) == '*') {
            return take(RpgTokenType.STARSTAR, 2, sLine, sCol);   // power
        }
        // A '*' glued to a letter is a star-name (*ON, *NO, *INPUT, *INLR, *IN01)
        // UNLESS it follows an operand, where it is multiplication (a*b, x()*y).
        if (isWordStart(peekAt(1)) && !prevEndsOperand()) {
            int start = pos;
            advance(); // '*'
            while (!atEnd() && isWordPart(peek())) advance();
            return make(RpgTokenType.STAR_NAME, src.substring(start, pos), sLine, sCol);
        }
        return take(RpgTokenType.STAR, 1, sLine, sCol);   // multiplication
    }

    /** Whether the previous token can end an operand (so a following '*' is multiply). */
    private boolean prevEndsOperand() {
        if (prev == null) return false;
        return switch (prev) {
            case IDENT, INT_LITERAL, DEC_LITERAL, STRING_LITERAL, HEX_LITERAL,
                 DATE_LITERAL, TIME_LITERAL, TIMESTAMP_LITERAL, UCS2_LITERAL,
                 GRAPHIC_LITERAL, STAR_NAME, RPAREN -> true;
            default -> false;
        };
    }

    // -----------------------------------------------------------------------
    // Numbers: integer or decimal ('.' only if followed by a digit)
    // -----------------------------------------------------------------------

    private RpgToken number(int sLine, int sCol) {
        int start = pos;
        while (!atEnd() && Character.isDigit(peek())) advance();
        boolean isDecimal = false;
        if (!atEnd() && peek() == '.' && Character.isDigit(peekAt(1))) {
            isDecimal = true;
            advance(); // '.'
            while (!atEnd() && Character.isDigit(peek())) advance();
        }
        String text = src.substring(start, pos);
        return make(isDecimal ? RpgTokenType.DEC_LITERAL : RpgTokenType.INT_LITERAL,
            text, sLine, sCol);
    }

    // -----------------------------------------------------------------------
    // Character literal '...' with '' as an escaped quote
    // -----------------------------------------------------------------------

    private RpgToken string(int sLine, int sCol) {
        int start = pos;
        consumeQuoted();
        return make(RpgTokenType.STRING_LITERAL, src.substring(start, pos), sLine, sCol);
    }

    // -----------------------------------------------------------------------
    // Typed literals: X'..' (hex), D'..' (date), T'..' (time), Z'..' (timestamp),
    // U'..' (ucs2), G'..' (graphic). Cursor is on the prefix letter.
    // -----------------------------------------------------------------------

    private RpgToken typedLiteral(int sLine, int sCol) {
        int start = pos;
        char prefix = Character.toUpperCase(peek());
        advance();        // consume prefix letter
        consumeQuoted();  // consume '...'
        RpgTokenType type = switch (prefix) {
            case 'X' -> RpgTokenType.HEX_LITERAL;
            case 'D' -> RpgTokenType.DATE_LITERAL;
            case 'T' -> RpgTokenType.TIME_LITERAL;
            case 'Z' -> RpgTokenType.TIMESTAMP_LITERAL;
            case 'U' -> RpgTokenType.UCS2_LITERAL;
            case 'G' -> RpgTokenType.GRAPHIC_LITERAL;
            default  -> RpgTokenType.STRING_LITERAL; // unreachable
        };
        return make(type, src.substring(start, pos), sLine, sCol);
    }

    /** Consumes a single-quoted run starting at the opening quote; '' stays inside. */
    private void consumeQuoted() {
        advance(); // opening quote
        while (!atEnd()) {
            char c = peek();
            if (c == '\'') {
                if (peekAt(1) == '\'') { advance(); advance(); continue; } // escaped ''
                advance(); // closing quote
                return;
            }
            advance();
        }
    }

    // -----------------------------------------------------------------------
    // Slash: compiler directive (/COPY, /IF ...) or division operator
    // -----------------------------------------------------------------------

    private RpgToken slashOrDirective(int sLine, int sCol) {
        // A directive is `/name` with the name glued to the slash and recognised
        // in RpgKeywords.DIRECTIVES; it owns the rest of the line. Anything else
        // is the division operator.
        if (isWordStart(peekAt(1))) {
            int save = pos, saveLine = line, saveCol = col;
            advance(); // '/'
            int wordStart = pos;
            while (!atEnd() && isWordPart(peek())) advance();
            // allow the single hyphen in /END-FREE
            if (!atEnd() && peek() == '-' && isWordStart(peekAt(1))) {
                advance();
                while (!atEnd() && isWordPart(peek())) advance();
            }
            String name = src.substring(wordStart, pos);
            if (RpgKeywords.isDirective(name)) {
                while (!atEnd() && peek() != '\n') advance(); // rest of the line
                return make(RpgTokenType.DIRECTIVE, src.substring(save, pos), sLine, sCol);
            }
            pos = save; line = saveLine; col = saveCol; // not a directive: division
        }
        return take(RpgTokenType.SLASH, 1, sLine, sCol);
    }

    // -----------------------------------------------------------------------
    // Operators and punctuators
    // -----------------------------------------------------------------------

    private RpgToken operator(int sLine, int sCol) {
        char c = peek();
        switch (c) {
            case '=': return take(RpgTokenType.EQ, 1, sLine, sCol);
            case '+': return take(RpgTokenType.PLUS, 1, sLine, sCol);
            case '(': return take(RpgTokenType.LPAREN, 1, sLine, sCol);
            case ')': return take(RpgTokenType.RPAREN, 1, sLine, sCol);
            case ':': return take(RpgTokenType.COLON, 1, sLine, sCol);
            case ',': return take(RpgTokenType.COMMA, 1, sLine, sCol);
            case ';': return take(RpgTokenType.SEMI, 1, sLine, sCol);
            case '.': return take(RpgTokenType.DOT, 1, sLine, sCol);
            case '-': return take(RpgTokenType.MINUS, 1, sLine, sCol);
            case '<':
                if (peekAt(1) == '>') return take(RpgTokenType.NE, 2, sLine, sCol);
                if (peekAt(1) == '=') return take(RpgTokenType.LE, 2, sLine, sCol);
                return take(RpgTokenType.LT, 1, sLine, sCol);
            case '>':
                if (peekAt(1) == '=') return take(RpgTokenType.GE, 2, sLine, sCol);
                return take(RpgTokenType.GT, 1, sLine, sCol);
            default:
                throw new RpgLexException("Unexpected character '" + c + "'", sLine, sCol);
        }
    }

    // -----------------------------------------------------------------------
    // Trivia: whitespace and // line comments
    // -----------------------------------------------------------------------

    private void skipTrivia() {
        while (!atEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && peekAt(1) == '/') {
                while (!atEnd() && peek() != '\n') advance();
            } else {
                break;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Character classification
    // -----------------------------------------------------------------------

    private static boolean isWordStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '@' || c == '#' || c == '$';
    }

    private static boolean isWordPart(char c) {
        return isWordStart(c) || Character.isDigit(c);
    }

    private static boolean isTypedLiteralPrefix(char c) {
        char u = Character.toUpperCase(c);
        return u == 'X' || u == 'D' || u == 'T' || u == 'Z' || u == 'U' || u == 'G';
    }

    // -----------------------------------------------------------------------
    // Low-level cursor helpers
    // -----------------------------------------------------------------------

    private boolean atEnd() {
        return pos >= src.length();
    }

    private char peek() {
        return src.charAt(pos);
    }

    private char peekAt(int offset) {
        int i = pos + offset;
        return i < src.length() ? src.charAt(i) : '\0';
    }

    /** Does the source from the cursor start with {@code literal} (case-insensitive)? */
    private boolean matches(String literal) {
        if (pos + literal.length() > src.length()) return false;
        return src.regionMatches(true, pos, literal, 0, literal.length());
    }

    private void advance() {
        if (peek() == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        pos++;
    }

    private RpgToken make(RpgTokenType type, String text, int sLine, int sCol) {
        return new RpgToken(type, text, sLine, sCol);
    }

    /** Consume {@code n} chars and emit a token of the given type. */
    private RpgToken take(RpgTokenType type, int n, int sLine, int sCol) {
        int start = pos;
        for (int i = 0; i < n; i++) advance();
        return make(type, src.substring(start, pos), sLine, sCol);
    }
}
