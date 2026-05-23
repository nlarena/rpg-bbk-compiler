package com.larena.boxbreaker.plugin.bbk.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.larena.boxbreaker.plugin.bbk.BbkBundle;
import com.larena.boxbreaker.plugin.bbk.completion.matcher.BbkHyphenAwarePrefixMatcher;
import com.larena.boxbreaker.plugin.bbk.icons.BbkIcons;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Emits the subfields of the DS that backs the LHS of a member-access expression
 * ({@code employee.|}).
 *
 * <p>Fires only when the cursor's enclosing {@link BbkPostfixSuffix} starts with a
 * {@code .} token. The LHS is looked up via {@link BbkTypeResolver#dsOf}.
 */
public class BbkMemberCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        PsiElement suffix = enclosingMemberSuffix(position);
        if (suffix == null) return;
        PsiElement lhs = findLhs(suffix);
        if (lhs == null) return;

        BbkDataStructureDeclaration ds = BbkTypeResolver.dsOf(lhs);
        if (ds == null) return;

        String prefix = result.getPrefixMatcher().getPrefix();
        CompletionResultSet wrapped = result.withPrefixMatcher(new BbkHyphenAwarePrefixMatcher(prefix));
        String typeText = BbkBundle.message("completion.type.userSubfield");

        for (BbkDsSubfield sf : PsiTreeUtil.findChildrenOfType(ds, BbkDsSubfield.class)) {
            String name = sf.getName();
            if (name == null || name.isEmpty()) continue;
            wrapped.addElement(
                LookupElementBuilder.create(sf, name)
                    .withCaseSensitivity(false)
                    .withIcon(BbkIcons.forCategory(BbkIcons.Category.MODIFIER))
                    .withTypeText(typeText)
            );
        }
    }

    private static @Nullable PsiElement enclosingMemberSuffix(@NotNull PsiElement position) {
        BbkPostfixSuffix suffix = PsiTreeUtil.getParentOfType(position, BbkPostfixSuffix.class);
        if (suffix == null) return null;
        PsiElement first = suffix.getFirstChild();
        if (first != null && first.getNode() != null && first.getNode().getElementType() == BbkTypes.DOT) {
            return suffix;
        }
        return null;
    }

    private static @Nullable PsiElement findLhs(@NotNull PsiElement suffix) {
        BbkPostfixExpression postfix = PsiTreeUtil.getParentOfType(suffix, BbkPostfixExpression.class);
        if (postfix == null) return null;
        PsiElement primary = postfix.getFirstChild();
        return primary instanceof BbkPrimary ? primary : null;
    }
}
