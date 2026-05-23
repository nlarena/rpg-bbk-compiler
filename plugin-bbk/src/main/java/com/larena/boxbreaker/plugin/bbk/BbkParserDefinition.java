package com.larena.boxbreaker.plugin.bbk;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.larena.boxbreaker.plugin.bbk.parser.BbkParser;
import com.larena.boxbreaker.plugin.bbk.psi.BbkFile;
import com.larena.boxbreaker.plugin.bbk.psi.BbkTypes;
import org.jetbrains.annotations.NotNull;

public class BbkParserDefinition implements ParserDefinition {

    public static final IFileElementType FILE = new IFileElementType(BbkLanguage.INSTANCE);

    private static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    private static final TokenSet COMMENTS = TokenSet.create(BbkTypes.LINE_COMMENT, BbkTypes.BLOCK_COMMENT);
    private static final TokenSet STRING_LITERALS = TokenSet.create(BbkTypes.STR_LIT);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new BbkLexerAdapter();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new BbkParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return STRING_LITERALS;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return BbkTypes.Factory.createElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new BbkFile(viewProvider);
    }
}
