package com.larena.boxbreaker.plugin.bbk;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import static com.larena.boxbreaker.plugin.bbk.psi.BbkTypes.*;

public class BbkSyntaxHighlighter extends SyntaxHighlighterBase {

    // ----- Attribute keys (auto-adapt to the IDE color scheme) -----

    private static final TextAttributesKey KEYWORD = TextAttributesKey.createTextAttributesKey(
        "BBK_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

    private static final TextAttributesKey TYPE_KEYWORD = TextAttributesKey.createTextAttributesKey(
        "BBK_TYPE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

    private static final TextAttributesKey DIRECTIVE = TextAttributesKey.createTextAttributesKey(
        "BBK_DIRECTIVE", DefaultLanguageHighlighterColors.METADATA);

    private static final TextAttributesKey ATTRIBUTE = TextAttributesKey.createTextAttributesKey(
        "BBK_ATTRIBUTE", DefaultLanguageHighlighterColors.METADATA);

    private static final TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey(
        "BBK_NUMBER", DefaultLanguageHighlighterColors.NUMBER);

    private static final TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey(
        "BBK_STRING", DefaultLanguageHighlighterColors.STRING);

    private static final TextAttributesKey LINE_COMMENT_KEY = TextAttributesKey.createTextAttributesKey(
        "BBK_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);

    private static final TextAttributesKey BLOCK_COMMENT_KEY = TextAttributesKey.createTextAttributesKey(
        "BBK_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);

    private static final TextAttributesKey IDENTIFIER = TextAttributesKey.createTextAttributesKey(
        "BBK_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);

    private static final TextAttributesKey CONSTANT = TextAttributesKey.createTextAttributesKey(
        "BBK_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT);

    private static final TextAttributesKey OPERATOR = TextAttributesKey.createTextAttributesKey(
        "BBK_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    private static final TextAttributesKey PUNCTUATION = TextAttributesKey.createTextAttributesKey(
        "BBK_PUNCTUATION", DefaultLanguageHighlighterColors.SEMICOLON);

    private static final TextAttributesKey PARENTHESES = TextAttributesKey.createTextAttributesKey(
        "BBK_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);

    private static final TextAttributesKey BRACES = TextAttributesKey.createTextAttributesKey(
        "BBK_BRACES", DefaultLanguageHighlighterColors.BRACES);

    private static final TextAttributesKey BRACKETS = TextAttributesKey.createTextAttributesKey(
        "BBK_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);

    private static final TextAttributesKey BAD_CHAR = TextAttributesKey.createTextAttributesKey(
        "BBK_BAD_CHARACTER", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);

    // ----- Token category sets -----

    private static final TokenSet KEYWORDS = TokenSet.create(
        KW_IF, KW_ELSE, KW_WHILE, KW_DO, KW_FOR, KW_BREAK, KW_CONTINUE, KW_RETURN,
        KW_SELECT, KW_WHEN, KW_OTHER, KW_MONITOR, KW_ON_ERROR, KW_ON_EXIT,
        KW_DCL_S, KW_DCL_C, KW_DCL_DS, KW_DCL_PR, KW_DCL_PROC, KW_DCL_F, KW_DCL_PARM, KW_DCL_SUBF,
        KW_INZ, KW_BASED, KW_DIM, KW_OVERLAY, KW_POS, KW_LIKE, KW_LIKEDS, KW_LIKEREC,
        KW_TEMPLATE, KW_QUALIFIED, KW_ALIGN, KW_VALUE, KW_CONST, KW_OPTIONS, KW_RTNPARM,
        KW_OPDESC, KW_STATIC, KW_EXPORT, KW_IMPORT, KW_EXTPGM, KW_EXTPROC,
        KW_USAGE, KW_KEYED, KW_EXTNAME, KW_EXTFILE, KW_PREFIX, KW_RENAME,
        KW_DISK, KW_PRINTER, KW_WORKSTN, KW_SEQ, KW_USROPN, KW_INFDS, KW_INDDS
    );

    private static final TokenSet TYPE_KEYWORDS = TokenSet.create(
        KW_CHAR, KW_VARCHAR, KW_PACKED, KW_ZONED, KW_BINDEC,
        KW_INT, KW_UNS, KW_FLOAT, KW_DATE, KW_TIME, KW_TIMESTAMP,
        KW_BOOL, KW_POINTER, KW_VOID
    );

    private static final TokenSet DIRECTIVES = TokenSet.create(
        KW_CTL_OPT, KW_PRE_IF, KW_PRE_ELSEIF, KW_PRE_ELSE, KW_PRE_ENDIF,
        KW_PRE_DEFINE, KW_PRE_UNDEFINE, KW_PRE_INCLUDE, KW_PRE_EOF
    );

    private static final TokenSet ATTRIBUTES = TokenSet.create(
        ATTR_HALFUP, ATTR_HALFDOWN, ATTR_TRUNC, AT
    );

    private static final TokenSet NUMBERS = TokenSet.create(
        INT_LIT, INT_LIT_HEX, INT_LIT_OCT, FLOAT_LIT, DEC_LIT
    );

    private static final TokenSet CONSTANTS = TokenSet.create(
        KW_TRUE, KW_FALSE, KW_NULL, STAR_IDENT
    );

    private static final TokenSet OPERATORS = TokenSet.create(
        PLUS, MINUS, STAR, SLASH, PERCENT, STAR_STAR,
        EQ, EQ_EQ, BANG_EQ, LT, GT, LT_EQ, GT_EQ,
        AMP_AMP, PIPE_PIPE, BANG,
        AMP, PIPE, CARET, TILDE, LT_LT, GT_GT,
        PLUS_EQ, MINUS_EQ, STAR_EQ, SLASH_EQ, PERCENT_EQ,
        AMP_EQ, PIPE_EQ, CARET_EQ, LT_LT_EQ, GT_GT_EQ,
        QUESTION, ARROW
    );

    // ----- Cached key arrays -----

    private static final TextAttributesKey[] KEYWORD_KEYS         = wrapKey(KEYWORD);
    private static final TextAttributesKey[] TYPE_KEYWORD_KEYS    = wrapKey(TYPE_KEYWORD);
    private static final TextAttributesKey[] DIRECTIVE_KEYS       = wrapKey(DIRECTIVE);
    private static final TextAttributesKey[] ATTRIBUTE_KEYS       = wrapKey(ATTRIBUTE);
    private static final TextAttributesKey[] NUMBER_KEYS          = wrapKey(NUMBER);
    private static final TextAttributesKey[] STRING_KEYS          = wrapKey(STRING);
    private static final TextAttributesKey[] LINE_COMMENT_KEYS    = wrapKey(LINE_COMMENT_KEY);
    private static final TextAttributesKey[] BLOCK_COMMENT_KEYS   = wrapKey(BLOCK_COMMENT_KEY);
    private static final TextAttributesKey[] IDENTIFIER_KEYS      = wrapKey(IDENTIFIER);
    private static final TextAttributesKey[] CONSTANT_KEYS        = wrapKey(CONSTANT);
    private static final TextAttributesKey[] OPERATOR_KEYS        = wrapKey(OPERATOR);
    private static final TextAttributesKey[] PUNCTUATION_KEYS     = wrapKey(PUNCTUATION);
    private static final TextAttributesKey[] PARENTHESES_KEYS     = wrapKey(PARENTHESES);
    private static final TextAttributesKey[] BRACES_KEYS          = wrapKey(BRACES);
    private static final TextAttributesKey[] BRACKETS_KEYS        = wrapKey(BRACKETS);
    private static final TextAttributesKey[] BAD_CHAR_KEYS        = wrapKey(BAD_CHAR);
    private static final TextAttributesKey[] EMPTY_KEYS           = TextAttributesKey.EMPTY_ARRAY;

    private static TextAttributesKey[] wrapKey(TextAttributesKey key) {
        return new TextAttributesKey[]{key};
    }

    // ----- API -----

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new BbkLexerAdapter();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(LINE_COMMENT))             return LINE_COMMENT_KEYS;
        if (tokenType.equals(BLOCK_COMMENT))            return BLOCK_COMMENT_KEYS;
        if (tokenType.equals(STR_LIT))                  return STRING_KEYS;
        if (NUMBERS.contains(tokenType))                return NUMBER_KEYS;
        if (CONSTANTS.contains(tokenType))              return CONSTANT_KEYS;
        if (DIRECTIVES.contains(tokenType))             return DIRECTIVE_KEYS;
        if (ATTRIBUTES.contains(tokenType))             return ATTRIBUTE_KEYS;
        if (TYPE_KEYWORDS.contains(tokenType))          return TYPE_KEYWORD_KEYS;
        if (KEYWORDS.contains(tokenType))               return KEYWORD_KEYS;
        if (OPERATORS.contains(tokenType))              return OPERATOR_KEYS;
        if (tokenType.equals(LPAREN) || tokenType.equals(RPAREN))   return PARENTHESES_KEYS;
        if (tokenType.equals(LBRACE) || tokenType.equals(RBRACE))   return BRACES_KEYS;
        if (tokenType.equals(LBRACKET) || tokenType.equals(RBRACKET)) return BRACKETS_KEYS;
        if (tokenType.equals(SEMI) || tokenType.equals(COMMA)
            || tokenType.equals(DOT) || tokenType.equals(COLON))    return PUNCTUATION_KEYS;
        if (tokenType.equals(IDENT))                    return IDENTIFIER_KEYS;
        if (tokenType.equals(TokenType.BAD_CHARACTER))  return BAD_CHAR_KEYS;
        return EMPTY_KEYS;
    }
}
