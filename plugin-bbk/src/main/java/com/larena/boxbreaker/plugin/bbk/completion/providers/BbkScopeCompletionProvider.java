package com.larena.boxbreaker.plugin.bbk.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.util.ProcessingContext;
import com.larena.boxbreaker.plugin.bbk.BbkBundle;
import com.larena.boxbreaker.plugin.bbk.completion.matcher.BbkHyphenAwarePrefixMatcher;
import com.larena.boxbreaker.plugin.bbk.icons.BbkIcons;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import com.larena.boxbreaker.plugin.bbk.scope.BbkScopeWalker;
import org.jetbrains.annotations.NotNull;

/**
 * Emits every visible user-declared identifier as a completion suggestion:
 * variables, constants, procedures, prototypes, files, data structures and
 * (non-qualified) subfields. Subroutines are excluded — they appear only
 * after {@code exsr} (see {@link BbkSubroutineReference}'s variants).
 *
 * <p>The category label (variable / constant / procedure / ...) is shown as the
 * lookup's type text, so the user can tell at a glance what kind of entity each
 * suggestion is. Tail text reproduces the declaration's signature for procedures
 * and prototypes.
 */
public class BbkScopeCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        String prefix = result.getPrefixMatcher().getPrefix();
        CompletionResultSet wrapped = result.withPrefixMatcher(new BbkHyphenAwarePrefixMatcher(prefix));

        for (PsiNamedElement decl : BbkScopeWalker.allVisible(position)) {
            String name = decl.getName();
            if (name == null || name.isEmpty()) continue;
            wrapped.addElement(buildLookup(decl, name));
        }
    }

    private static LookupElementBuilder buildLookup(@NotNull PsiNamedElement decl, @NotNull String name) {
        LookupElementBuilder b = LookupElementBuilder.create(decl, name)
            .withCaseSensitivity(false)
            .withIcon(BbkIcons.forCategory(categoryFor(decl)))
            .withTypeText(typeTextFor(decl));
        String tail = tailTextFor(decl);
        if (tail != null) b = b.withTailText(tail, true);
        return b;
    }

    private static BbkIcons.Category categoryFor(@NotNull PsiNamedElement decl) {
        if (decl instanceof BbkProcedureDeclaration || decl instanceof BbkPrototypeDeclaration) {
            return BbkIcons.Category.KEYWORD;
        }
        if (decl instanceof BbkFileDeclaration) return BbkIcons.Category.FILE_KEYWORD;
        if (decl instanceof BbkDataStructureDeclaration) return BbkIcons.Category.TYPE;
        if (decl instanceof BbkConstantDeclaration) return BbkIcons.Category.MODIFIER;
        return BbkIcons.Category.STATEMENT;
    }

    private static @NotNull String typeTextFor(@NotNull PsiNamedElement decl) {
        if (decl instanceof BbkVariableDeclaration)       return BbkBundle.message("completion.type.userVariable");
        if (decl instanceof BbkConstantDeclaration)       return BbkBundle.message("completion.type.userConstant");
        if (decl instanceof BbkProcedureDeclaration)      return BbkBundle.message("completion.type.userProcedure");
        if (decl instanceof BbkPrototypeDeclaration)      return BbkBundle.message("completion.type.userPrototype");
        if (decl instanceof BbkDataStructureDeclaration)  return BbkBundle.message("completion.type.userDataStruct");
        if (decl instanceof BbkDsSubfield)                return BbkBundle.message("completion.type.userSubfield");
        if (decl instanceof BbkFileDeclaration)           return BbkBundle.message("completion.type.userFile");
        if (decl instanceof BbkInlineParam)               return BbkBundle.message("completion.type.userVariable");
        return "";
    }

    private static String tailTextFor(@NotNull PsiNamedElement decl) {
        if (decl instanceof BbkProcedureDeclaration p) {
            BbkInlineParamList params = com.intellij.psi.util.PsiTreeUtil.findChildOfType(p, BbkInlineParamList.class);
            return params != null ? params.getText() : "()";
        }
        if (decl instanceof BbkPrototypeDeclaration p) {
            BbkInlineParamList params = com.intellij.psi.util.PsiTreeUtil.findChildOfType(p, BbkInlineParamList.class);
            return params != null ? params.getText() : "()";
        }
        return null;
    }
}
