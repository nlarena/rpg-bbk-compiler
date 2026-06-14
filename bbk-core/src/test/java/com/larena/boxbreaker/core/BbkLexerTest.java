package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.lexer.BbkLexer;
import com.larena.boxbreaker.core.lexer.BbkToken;
import com.larena.boxbreaker.core.lexer.BbkTokenType;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class BbkLexerTest {

    private List<String> shape(String src) {
        return BbkLexer.tokenize(src).stream()
            .filter(t -> t.type() != BbkTokenType.EOF)
            .map(t -> t.type() + ":" + t.text())
            .collect(Collectors.toList());
    }

    private BbkTokenType first(String src) {
        return BbkLexer.tokenize(src).get(0).type();
    }

    @Test
    public void declarationUppercaseKeywords() {
        assertEquals(
            List.of("KEYWORD:DCL-S", "IDENT:counter", "KEYWORD:INT",
                    "LPAREN:(", "INT_LIT:10", "RPAREN:)", "SEMI:;"),
            shape("DCL-S counter INT(10);"));
    }

    @Test
    public void caseSensitive() {
        // declaration keywords are upper-case; control flow is lower-case
        assertEquals(BbkTokenType.KEYWORD, first("DCL-S"));
        assertEquals(BbkTokenType.KEYWORD, first("if"));
        // wrong case is NOT a keyword
        assertEquals(BbkTokenType.IDENT, first("dcl-s"));   // 'dcl' ident then '-' '... — first token is IDENT
        assertEquals(BbkTokenType.IDENT, first("IF"));
    }

    @Test
    public void hyphenKeywordVsMinus() {
        assertEquals(List.of("KEYWORD:DCL-PROC", "IDENT:main"), shape("DCL-PROC main"));
        assertEquals(List.of("KEYWORD:on-error"), shape("on-error"));
        assertEquals(List.of("KEYWORD:CTL-OPT"), shape("CTL-OPT"));
        assertEquals(List.of("IDENT:a", "MINUS:-", "INT_LIT:1"), shape("a - 1"));
        assertEquals(List.of("IDENT:a", "MINUS:-", "INT_LIT:1"), shape("a-1"));
    }

    @Test
    public void cStyleOperators() {
        assertEquals(List.of("IDENT:a", "EQ_EQ:==", "IDENT:b"), shape("a == b"));
        assertEquals(List.of("IDENT:a", "BANG_EQ:!=", "IDENT:b"), shape("a != b"));
        assertEquals(List.of("IDENT:a", "AMP_AMP:&&", "IDENT:b", "PIPE_PIPE:||", "IDENT:c"),
            shape("a && b || c"));
        assertEquals(List.of("IDENT:x", "PLUS_EQ:+=", "INT_LIT:1"), shape("x += 1"));
        assertEquals(List.of("IDENT:x", "LT_LT_EQ:<<=", "INT_LIT:2"), shape("x <<= 2"));
        assertEquals(List.of("IDENT:a", "STAR_STAR:**", "IDENT:b"), shape("a ** b"));
        assertEquals(List.of("IDENT:f", "ARROW:->", "IDENT:g"), shape("f -> g"));
    }

    @Test
    public void literals() {
        assertEquals(BbkTokenType.INT_LIT, first("123"));
        assertEquals(BbkTokenType.INT_LIT_HEX, first("0x1F"));
        assertEquals(BbkTokenType.INT_LIT_OCT, first("0o17"));
        assertEquals(BbkTokenType.DEC_LIT, first("199.95"));
        assertEquals(BbkTokenType.DEC_LIT, first("199.95d"));
        assertEquals(BbkTokenType.FLOAT_LIT, first("1.5e3"));
        assertEquals(BbkTokenType.STR_LIT, first("\"Alice\""));
        assertEquals("\"it\\\"s\"", BbkLexer.tokenize("\"it\\\"s\"").get(0).text());
    }

    @Test
    public void starIdentAndAttributesAndBraces() {
        assertEquals(BbkTokenType.STAR_IDENT, first("*INLR"));
        assertEquals(BbkTokenType.ATTR, first("@halfup"));
        assertEquals(List.of("KEYWORD:if", "LPAREN:(", "IDENT:c", "RPAREN:)", "LBRACE:{", "RBRACE:}"),
            shape("if (c) {}"));
        assertEquals(List.of("IDENT:arr", "LBRACKET:[", "IDENT:i", "RBRACKET:]"), shape("arr[i]"));
    }

    @Test
    public void commentsSkipped() {
        assertEquals(List.of("KEYWORD:return", "SEMI:;"),
            shape("// line\nreturn; /* block */\n"));
    }

    @Test
    public void tracksLineAndColumn() {
        BbkToken ret = BbkLexer.tokenize("\n  return;").get(0);
        assertEquals(2, ret.line());
        assertEquals(3, ret.column());
    }

    /** A program as produced by rpg-frontend must lex without surprises. */
    @Test
    public void lexesFrontendOutput() {
        String bbk =
            "DCL-S counter INT(10) INZ(0);\n" +
            "DCL-DS customer QUALIFIED {\n" +
            "  id INT(10);\n" +
            "  active BOOL;\n" +
            "}\n" +
            "if (counter > 0 && customer.active == true) {\n" +
            "  name = \"active\";\n" +
            "  for (i = 1; i <= counter; i += 1) {\n" +
            "    counter = counter - 1;\n" +
            "  }\n" +
            "}\n";
        List<BbkToken> tokens = BbkLexer.tokenize(bbk);
        // last is EOF; nothing threw
        assertEquals(BbkTokenType.EOF, tokens.get(tokens.size() - 1).type());
        // spot-check a few
        assertTrue(tokens.stream().anyMatch(t -> t.isKeyword("DCL-DS")));
        assertTrue(tokens.stream().anyMatch(t -> t.isKeyword("QUALIFIED")));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == BbkTokenType.EQ_EQ));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == BbkTokenType.PLUS_EQ));
    }
}
