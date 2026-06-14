package com.larena.boxbreaker.rpg;

import com.larena.boxbreaker.rpg.lexer.RpgLexer;
import com.larena.boxbreaker.rpg.lexer.RpgToken;
import com.larena.boxbreaker.rpg.lexer.RpgTokenType;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class RpgLexerTest {

    private List<RpgToken> lex(String src) {
        return RpgLexer.tokenize(src);
    }

    /** Compact representation "TYPE:text" for readable assertions, EOF dropped. */
    private List<String> shape(String src) {
        return lex(src).stream()
            .filter(t -> t.type() != RpgTokenType.EOF)
            .map(t -> t.type() + ":" + t.text())
            .collect(Collectors.toList());
    }

    @Test
    public void declarationAndAssignment() {
        assertEquals(
            List.of("KEYWORD:dcl-s", "IDENT:counter", "IDENT:int",
                    "LPAREN:(", "INT_LITERAL:10", "RPAREN:)", "SEMI:;"),
            shape("dcl-s counter int(10);"));
    }

    @Test
    public void hyphenInKeywordVsMinusOperator() {
        // dcl-s is one KEYWORD token; i - 1 is three tokens with a MINUS.
        assertEquals(List.of("KEYWORD:dcl-s", "IDENT:x"), shape("dcl-s x"));
        assertEquals(List.of("IDENT:i", "MINUS:-", "INT_LITERAL:1"), shape("i - 1"));
        assertEquals(List.of("IDENT:i", "MINUS:-", "INT_LITERAL:1"), shape("i-1"));
        // Real hyphenated keywords stay a single token (RPG spells it END-DS, ON-ERROR;
        // note: the end of an IF block is ENDIF, no hyphen).
        assertEquals(List.of("KEYWORD:end-ds"), shape("end-ds"));
        assertEquals(List.of("KEYWORD:on-error"), shape("on-error"));
        assertEquals(List.of("KEYWORD:endif"), shape("endif"));
        // 'end-if' is NOT a keyword: lexes as IDENT '-' KEYWORD
        assertEquals(List.of("IDENT:end", "MINUS:-", "KEYWORD:if"), shape("end-if"));
    }

    @Test
    public void comparisonOperators() {
        assertEquals(
            List.of("IDENT:a", "NE:<>", "IDENT:b"),
            shape("a <> b"));
        assertEquals(
            List.of("IDENT:a", "LE:<=", "IDENT:b", "GE:>=", "IDENT:c", "LT:<", "GT:>"),
            shape("a <= b >= c < >"));
    }

    @Test
    public void caseInsensitiveKeywords() {
        assertEquals(RpgTokenType.KEYWORD, lex("IF").get(0).type());
        assertEquals(RpgTokenType.KEYWORD, lex("Dcl-S").get(0).type());
        assertEquals(RpgTokenType.IDENT, lex("counter").get(0).type());
    }

    @Test
    public void stringWithDoubledQuote() {
        List<RpgToken> t = lex("'it''s ok'");
        assertEquals(RpgTokenType.STRING_LITERAL, t.get(0).type());
        assertEquals("'it''s ok'", t.get(0).text());
    }

    @Test
    public void decimalLiteral() {
        assertEquals(List.of("DEC_LITERAL:199.95"), shape("199.95"));
        // a trailing dot that is not followed by a digit is NOT part of the number
        assertEquals(List.of("INT_LITERAL:5", "IDENT:x"), shape("5 x"));
    }

    @Test
    public void commentsAndWhitespaceAreSkipped() {
        assertEquals(
            List.of("KEYWORD:return", "SEMI:;"),
            shape("  // leading comment\n  return;  // trailing\n"));
    }

    @Test
    public void freeDirective() {
        assertEquals(RpgTokenType.FREE_DIRECTIVE, lex("**FREE").get(0).type());
    }

    // ----- new tokens: full free-form surface -----

    @Test
    public void builtinFunctions() {
        assertEquals(List.of("BIF:%trim", "LPAREN:(", "IDENT:name", "RPAREN:)"),
            shape("%trim(name)"));
        assertEquals(RpgTokenType.BIF, lex("%LEN").get(0).type()); // case-insensitive prefix
    }

    @Test
    public void powerVsMultiplyVsFree() {
        assertEquals(List.of("IDENT:a", "STARSTAR:**", "IDENT:b"), shape("a ** b"));
        assertEquals(List.of("IDENT:a", "STAR:*", "IDENT:b"), shape("a * b"));
        assertEquals(List.of("IDENT:a", "STAR:*", "IDENT:b"), shape("a*b")); // no-space multiply
        assertEquals(RpgTokenType.FREE_DIRECTIVE, lex("**FREE").get(0).type());
    }

    @Test
    public void figurativeConstantsAndIndicators() {
        assertEquals(RpgTokenType.STAR_NAME, lex("*ON").get(0).type());
        assertEquals(RpgTokenType.STAR_NAME, lex("*BLANKS").get(0).type());
        assertEquals(RpgTokenType.STAR_NAME, lex("*NULL").get(0).type());
        assertEquals(RpgTokenType.STAR_NAME, lex("*INLR").get(0).type());
        assertEquals(RpgTokenType.STAR_NAME, lex("*IN01").get(0).type());
        // any *-word in value position is a star-name (covers *NO, *INPUT, *SRCSTMT...)
        assertEquals(RpgTokenType.STAR_NAME, lex("*NO").get(0).type());
        assertEquals(List.of("STAR_NAME:*foo"), shape("*foo"));
        // but after an operand, '*' is multiplication
        assertEquals(List.of("IDENT:a", "STAR:*", "IDENT:b"), shape("a * b"));
        assertEquals(List.of("IDENT:a", "STAR:*", "STAR_NAME:*on"), shape("a * *on"));
    }

    @Test
    public void typedLiterals() {
        assertEquals(RpgTokenType.HEX_LITERAL, lex("X'1F'").get(0).type());
        assertEquals(RpgTokenType.DATE_LITERAL, lex("D'2024-01-01'").get(0).type());
        assertEquals(RpgTokenType.TIME_LITERAL, lex("T'14.30.00'").get(0).type());
        assertEquals(RpgTokenType.TIMESTAMP_LITERAL, lex("Z'2024-01-01-14.30.00.000000'").get(0).type());
        assertEquals(RpgTokenType.UCS2_LITERAL, lex("U'00410042'").get(0).type());
        assertEquals(RpgTokenType.GRAPHIC_LITERAL, lex("G' abc '").get(0).type());
        assertEquals("D'2024-01-01'", lex("D'2024-01-01'").get(0).text());
        // a lone D that is NOT followed by a quote is just an identifier
        assertEquals(List.of("IDENT:d", "IDENT:x"), shape("d x"));
    }

    @Test
    public void qualifiedAccessAndComma() {
        assertEquals(List.of("IDENT:customer", "DOT:.", "IDENT:id"), shape("customer.id"));
        assertEquals(List.of("IDENT:a", "COMMA:,", "IDENT:b"), shape("a, b"));
        // decimal point still wins over DOT when between digits
        assertEquals(List.of("DEC_LITERAL:1.5"), shape("1.5"));
    }

    @Test
    public void colonAndStatementTerminator() {
        assertEquals(List.of("BIF:%subst", "LPAREN:(", "IDENT:s", "COLON::",
                             "INT_LITERAL:1", "COLON::", "INT_LITERAL:3", "RPAREN:)", "SEMI:;"),
            shape("%subst(s : 1 : 3);"));
    }

    @Test
    public void moreKeywords() {
        for (String kw : new String[]{"dcl-ds", "end-ds", "dcl-proc", "begsr", "endsr",
                                       "select", "when", "endsl", "dow", "dou", "for",
                                       "monitor", "on-error", "qualified", "likeds", "const"}) {
            assertEquals("expected keyword: " + kw,
                RpgTokenType.KEYWORD, lex(kw).get(0).type());
        }
        assertEquals(RpgTokenType.KEYWORD, lex("and").get(0).type());
        assertEquals(RpgTokenType.KEYWORD, lex("not").get(0).type());
    }

    @Test
    public void compilerDirectivesVsDivision() {
        // directives own the whole line
        assertEquals(RpgTokenType.DIRECTIVE, lex("/COPY MYLIB/MYSRC,MEMBER").get(0).type());
        assertEquals("/COPY MYLIB/MYSRC,MEMBER", lex("/COPY MYLIB/MYSRC,MEMBER").get(0).text());
        assertEquals(RpgTokenType.DIRECTIVE, lex("/IF DEFINED(DEBUG)").get(0).type());
        assertEquals(RpgTokenType.DIRECTIVE, lex("/ENDIF").get(0).type());
        assertEquals(RpgTokenType.DIRECTIVE, lex("/END-FREE").get(0).type());
        // division stays SLASH — '/' with a space, or before a non-directive word
        assertEquals(List.of("IDENT:total", "SLASH:/", "IDENT:count"), shape("total / count"));
        assertEquals(List.of("IDENT:a", "SLASH:/", "IDENT:b"), shape("a/b"));
    }

    @Test
    public void directiveStopsAtLineEnd() {
        // a directive does not swallow the next line's statement
        assertEquals(
            List.of("DIRECTIVE:/IF DEFINED(DEBUG)", "KEYWORD:return", "SEMI:;",
                    "DIRECTIVE:/ENDIF"),
            shape("/IF DEFINED(DEBUG)\nreturn;\n/ENDIF\n"));
    }

    @Test
    public void realisticLine() {
        // currentOrder.total = computeTax(amount) + 5;
        assertEquals(
            List.of("IDENT:currentOrder", "DOT:.", "IDENT:total", "EQ:=",
                    "IDENT:computeTax", "LPAREN:(", "IDENT:amount", "RPAREN:)",
                    "PLUS:+", "INT_LITERAL:5", "SEMI:;"),
            shape("currentOrder.total = computeTax(amount) + 5;"));
    }

    @Test
    public void tracksLineAndColumn() {
        // second line, 'return' starts at column 3
        RpgToken ret = lex("\n  return;").get(0);
        assertEquals(2, ret.line());
        assertEquals(3, ret.column());
    }
}
