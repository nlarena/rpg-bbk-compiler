package com.larena.boxbreaker.core.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written, headless scanner for BBK. Mirrors the token rules of the
 * IntelliJ plugin's grammar ({@code BBK.bnf}) but with no platform dependency,
 * so the compiler core can run anywhere (CLI, tests, in-IDE process).
 *
 * <p>BBK is case-sensitive. Keywords (incl. hyphenated {@code DCL-S},
 * {@code on-error}, {@code CTL-OPT}) are matched by exact spelling; a hyphen
 * that does not complete a keyword stays as the {@code -} operator.
 */
public final class BbkLexer {

    private final String src;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    public BbkLexer(String src) {
        this.src = src;
    }

    public static List<BbkToken> tokenize(String src) {
        return new BbkLexer(src).lex();
    }

    public List<BbkToken> lex() {
        List<BbkToken> out = new ArrayList<>();
        BbkToken t;
        do {
            t = next();
            out.add(t);
        } while (t.type() != BbkTokenType.EOF);
        return out;
    }

    private BbkToken next() {
        skipTrivia();
        if (atEnd()) return make(BbkTokenType.EOF, "", line, col);

        int sLine = line, sCol = col;
        char c = peek();

        if (isWordStart(c)) return word(sLine, sCol);
        if (c == '@') return attribute(sLine, sCol);
        if (c == '*' && isWordStart(peekAt(1))) return starIdent(sLine, sCol);
        if (Character.isDigit(c)) return number(sLine, sCol);
        if (c == '"') return string(sLine, sCol);
        return operator(sLine, sCol);
    }

    // ----- identifiers and keywords (incl. hyphenated keywords) -----

    private BbkToken word(int sLine, int sCol) {
        int start = pos;
        while (!atEnd() && isWordPart(peek())) advance();
        // Extend across hyphens while the result is (a prefix toward) a known keyword.
        while (!atEnd() && peek() == '-' && isWordStart(peekAt(1))) {
            int save = pos, sl = line, sc = col;
            advance();
            while (!atEnd() && isWordPart(peek())) advance();
            if (!BbkKeywords.isKeyword(src.substring(start, pos))) {
                pos = save; line = sl; col = sc;   // hyphen is the MINUS operator
                break;
            }
        }
        String text = src.substring(start, pos);
        return make(BbkKeywords.isKeyword(text) ? BbkTokenType.KEYWORD : BbkTokenType.IDENT,
            text, sLine, sCol);
    }

    private BbkToken attribute(int sLine, int sCol) {
        int start = pos;
        advance(); // '@'
        while (!atEnd() && isWordPart(peek())) advance();
        String text = src.substring(start, pos);
        return make(BbkKeywords.isAttribute(text) ? BbkTokenType.ATTR : BbkTokenType.AT, text, sLine, sCol);
    }

    private BbkToken starIdent(int sLine, int sCol) {
        int start = pos;
        advance(); // '*'
        while (!atEnd() && isWordPart(peek())) advance();
        return make(BbkTokenType.STAR_IDENT, src.substring(start, pos), sLine, sCol);
    }

    // ----- numbers: hex 0x, octal 0o, float (e), decimal (optional d), integer -----

    private BbkToken number(int sLine, int sCol) {
        int start = pos;
        if (peek() == '0' && (peekAt(1) == 'x' || peekAt(1) == 'X')) {
            advance(); advance();
            while (!atEnd() && isHex(peek())) advance();
            return make(BbkTokenType.INT_LIT_HEX, src.substring(start, pos), sLine, sCol);
        }
        if (peek() == '0' && (peekAt(1) == 'o' || peekAt(1) == 'O')) {
            advance(); advance();
            while (!atEnd() && peek() >= '0' && peek() <= '7') advance();
            return make(BbkTokenType.INT_LIT_OCT, src.substring(start, pos), sLine, sCol);
        }
        while (!atEnd() && Character.isDigit(peek())) advance();
        if (!atEnd() && peek() == '.' && Character.isDigit(peekAt(1))) {
            advance();
            while (!atEnd() && Character.isDigit(peek())) advance();
            // float: an exponent part
            if (!atEnd() && (peek() == 'e' || peek() == 'E')) {
                advance();
                if (peek() == '+' || peek() == '-') advance();
                while (!atEnd() && Character.isDigit(peek())) advance();
                return make(BbkTokenType.FLOAT_LIT, src.substring(start, pos), sLine, sCol);
            }
            if (!atEnd() && (peek() == 'd' || peek() == 'D')) advance();   // decimal suffix
            return make(BbkTokenType.DEC_LIT, src.substring(start, pos), sLine, sCol);
        }
        return make(BbkTokenType.INT_LIT, src.substring(start, pos), sLine, sCol);
    }

    // ----- string literal "..." -----

    private BbkToken string(int sLine, int sCol) {
        int start = pos;
        advance(); // opening quote
        while (!atEnd() && peek() != '"') {
            if (peek() == '\\' && peekAt(1) != '\0') advance();   // escape
            advance();
        }
        if (!atEnd()) advance(); // closing quote
        return make(BbkTokenType.STR_LIT, src.substring(start, pos), sLine, sCol);
    }

    // ----- operators and punctuators (longest match first) -----

    private BbkToken operator(int sLine, int sCol) {
        char c = peek();
        switch (c) {
            case ';': return take(BbkTokenType.SEMI, 1, sLine, sCol);
            case ',': return take(BbkTokenType.COMMA, 1, sLine, sCol);
            case '.': return take(BbkTokenType.DOT, 1, sLine, sCol);
            case '(': return take(BbkTokenType.LPAREN, 1, sLine, sCol);
            case ')': return take(BbkTokenType.RPAREN, 1, sLine, sCol);
            case '{': return take(BbkTokenType.LBRACE, 1, sLine, sCol);
            case '}': return take(BbkTokenType.RBRACE, 1, sLine, sCol);
            case '[': return take(BbkTokenType.LBRACKET, 1, sLine, sCol);
            case ']': return take(BbkTokenType.RBRACKET, 1, sLine, sCol);
            case ':': return take(BbkTokenType.COLON, 1, sLine, sCol);
            case '?': return take(BbkTokenType.QUESTION, 1, sLine, sCol);
            case '~': return take(BbkTokenType.TILDE, 1, sLine, sCol);
            case '+': return peekAt(1) == '=' ? take(BbkTokenType.PLUS_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.PLUS, 1, sLine, sCol);
            case '-': if (peekAt(1) == '>') return take(BbkTokenType.ARROW, 2, sLine, sCol);
                      return peekAt(1) == '=' ? take(BbkTokenType.MINUS_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.MINUS, 1, sLine, sCol);
            case '*': if (peekAt(1) == '*') return take(BbkTokenType.STAR_STAR, 2, sLine, sCol);
                      return peekAt(1) == '=' ? take(BbkTokenType.STAR_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.STAR, 1, sLine, sCol);
            case '/': return peekAt(1) == '=' ? take(BbkTokenType.SLASH_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.SLASH, 1, sLine, sCol);
            case '%': return peekAt(1) == '=' ? take(BbkTokenType.PERCENT_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.PERCENT, 1, sLine, sCol);
            case '=': return peekAt(1) == '=' ? take(BbkTokenType.EQ_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.EQ, 1, sLine, sCol);
            case '!': return peekAt(1) == '=' ? take(BbkTokenType.BANG_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.BANG, 1, sLine, sCol);
            case '&': if (peekAt(1) == '&') return take(BbkTokenType.AMP_AMP, 2, sLine, sCol);
                      return peekAt(1) == '=' ? take(BbkTokenType.AMP_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.AMP, 1, sLine, sCol);
            case '|': if (peekAt(1) == '|') return take(BbkTokenType.PIPE_PIPE, 2, sLine, sCol);
                      return peekAt(1) == '=' ? take(BbkTokenType.PIPE_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.PIPE, 1, sLine, sCol);
            case '^': return peekAt(1) == '=' ? take(BbkTokenType.CARET_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.CARET, 1, sLine, sCol);
            case '<': if (peekAt(1) == '<') return peekAt(2) == '=' ? take(BbkTokenType.LT_LT_EQ, 3, sLine, sCol)
                                                                    : take(BbkTokenType.LT_LT, 2, sLine, sCol);
                      return peekAt(1) == '=' ? take(BbkTokenType.LT_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.LT, 1, sLine, sCol);
            case '>': if (peekAt(1) == '>') return peekAt(2) == '=' ? take(BbkTokenType.GT_GT_EQ, 3, sLine, sCol)
                                                                    : take(BbkTokenType.GT_GT, 2, sLine, sCol);
                      return peekAt(1) == '=' ? take(BbkTokenType.GT_EQ, 2, sLine, sCol)
                                              : take(BbkTokenType.GT, 1, sLine, sCol);
            default:
                throw new BbkLexException("Unexpected character '" + c + "'", sLine, sCol);
        }
    }

    // ----- trivia: whitespace + // line comments + block comments -----

    private void skipTrivia() {
        while (!atEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && peekAt(1) == '/') {
                while (!atEnd() && peek() != '\n') advance();
            } else if (c == '/' && peekAt(1) == '*') {
                advance(); advance();
                while (!atEnd() && !(peek() == '*' && peekAt(1) == '/')) advance();
                if (!atEnd()) { advance(); advance(); }
            } else {
                break;
            }
        }
    }

    // ----- classification -----

    private static boolean isWordStart(char c) { return Character.isLetter(c) || c == '_'; }
    private static boolean isWordPart(char c) { return isWordStart(c) || Character.isDigit(c); }
    private static boolean isHex(char c) {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    // ----- cursor -----

    private boolean atEnd() { return pos >= src.length(); }
    private char peek() { return src.charAt(pos); }
    private char peekAt(int off) { int i = pos + off; return i < src.length() ? src.charAt(i) : '\0'; }

    private void advance() {
        if (peek() == '\n') { line++; col = 1; } else { col++; }
        pos++;
    }

    private BbkToken make(BbkTokenType type, String text, int sLine, int sCol) {
        return new BbkToken(type, text, sLine, sCol);
    }

    private BbkToken take(BbkTokenType type, int n, int sLine, int sCol) {
        int start = pos;
        for (int i = 0; i < n; i++) advance();
        return make(type, src.substring(start, pos), sLine, sCol);
    }
}
