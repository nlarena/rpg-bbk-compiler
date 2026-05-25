package com.larena.boxbreaker.plugin.bbk.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.util.ProcessingContext;
import com.larena.boxbreaker.plugin.bbk.completion.expected.BbkExpectedTypeProvider;
import com.larena.boxbreaker.plugin.bbk.completion.matcher.BbkHyphenAwarePrefixMatcher;
import com.larena.boxbreaker.plugin.bbk.icons.BbkIcons;
import com.larena.boxbreaker.plugin.bbk.index.BbkIndexKeys;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import com.larena.boxbreaker.plugin.bbk.scope.BbkScopeWalker;
import com.larena.boxbreaker.plugin.bbk.types.BbkAssignability;
import com.larena.boxbreaker.plugin.bbk.types.BbkProcedureType;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;
import com.larena.boxbreaker.plugin.bbk.types.BbkUnknownType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Smart Completion ({@code Ctrl+Shift+Space}) provider — filters the list of
 * visible declarations by the type expected at the caret. When no expected
 * type can be inferred, contributes nothing (so the BASIC fallback runs).
 *
 * <p>Sources of candidates are the same as
 * {@link BbkScopeCompletionProvider}: local scope walker + cross-file stub
 * indexes. The difference is the per-candidate type check.
 *
 * <p>For procedures and prototypes, the type used for filtering is the
 * <em>return type</em> (i.e., the result of calling them) — so a function
 * returning {@code INT(10)} appears when an INT is expected, since the user
 * will usually want to type {@code f()} to fill that slot.
 */
public class BbkSmartCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        BbkType expected = BbkExpectedTypeProvider.find(position);
        if (expected == null || expected instanceof BbkUnknownType) {
            // No typed slot at the caret — let the BASIC contributor handle it.
            return;
        }

        String prefix = BbkKeywordProviderBase.computeBbkPrefix(parameters);
        CompletionResultSet wrapped = result.withPrefixMatcher(new BbkHyphenAwarePrefixMatcher(prefix));

        Set<String> emitted = new HashSet<>();

        // 1) Local scope.
        for (PsiNamedElement decl : BbkScopeWalker.allVisible(position)) {
            offerIfFits(decl, expected, emitted, wrapped);
        }

        // 2) Cross-file via stub indexes.
        Project project = position.getProject();
        emitFromIndex(project, BbkIndexKeys.PROCEDURE,        BbkProcedureDeclaration.class,     expected, emitted, wrapped);
        emitFromIndex(project, BbkIndexKeys.PROTOTYPE,        BbkPrototypeDeclaration.class,     expected, emitted, wrapped);
        emitFromIndex(project, BbkIndexKeys.CONSTANT,         BbkConstantDeclaration.class,      expected, emitted, wrapped);
        emitFromIndex(project, BbkIndexKeys.VARIABLE,         BbkVariableDeclaration.class,      expected, emitted, wrapped);
        emitFromIndex(project, BbkIndexKeys.DATA_STRUCTURE,   BbkDataStructureDeclaration.class, expected, emitted, wrapped);
    }

    private <T extends PsiNamedElement> void emitFromIndex(
            @NotNull Project project,
            @NotNull StubIndexKey<String, T> key,
            @NotNull Class<T> clazz,
            @NotNull BbkType expected,
            @NotNull Set<String> emitted,
            @NotNull CompletionResultSet wrapped) {
        for (String name : StubIndex.getInstance().getAllKeys(key, project)) {
            for (T decl : StubIndex.getElements(key, name, project,
                    GlobalSearchScope.allScope(project), clazz)) {
                offerIfFits(decl, expected, emitted, wrapped);
            }
        }
    }

    private static void offerIfFits(@NotNull PsiNamedElement decl,
                                    @NotNull BbkType expected,
                                    @NotNull Set<String> emitted,
                                    @NotNull CompletionResultSet wrapped) {
        String name = decl.getName();
        if (name == null || name.isEmpty()) return;
        BbkType declType = BbkTypeInferrer.typeOf(decl);
        BbkType comparable = declType instanceof BbkProcedureType pt ? pt.getReturnType() : declType;
        if (!BbkAssignability.areCompatible(comparable, expected)) return;

        String key = decl.getClass().getName() + "::" + name.toLowerCase();
        if (!emitted.add(key)) return;

        wrapped.addElement(buildLookup(decl, name, declType));
    }

    private static @NotNull LookupElementBuilder buildLookup(@NotNull PsiNamedElement decl,
                                                              @NotNull String name,
                                                              @NotNull BbkType declType) {
        return LookupElementBuilder.create(decl, name)
            .withCaseSensitivity(false)
            .withIcon(BbkIcons.forCategory(categoryFor(decl)))
            .withTypeText(declType.getDisplayName());
    }

    private static @NotNull BbkIcons.Category categoryFor(@NotNull PsiNamedElement decl) {
        if (decl instanceof BbkProcedureDeclaration || decl instanceof BbkPrototypeDeclaration) {
            return BbkIcons.Category.KEYWORD;
        }
        if (decl instanceof BbkFileDeclaration) return BbkIcons.Category.FILE_KEYWORD;
        if (decl instanceof BbkDataStructureDeclaration) return BbkIcons.Category.TYPE;
        if (decl instanceof BbkConstantDeclaration) return BbkIcons.Category.MODIFIER;
        return BbkIcons.Category.STATEMENT;
    }

    @SuppressWarnings("unused")
    private static @Nullable String unused() { return null; }
}
