package com.larena.boxbreaker.plugin.bbk.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.larena.boxbreaker.plugin.bbk.psi.BbkDataStructureDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkVariableDeclaration;
import com.larena.boxbreaker.plugin.bbk.scope.BbkScopeWalker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reference for the IDENT inside {@code LIKE(...)} / {@code LIKEDS(...)} /
 * {@code LIKEREC(...)}.
 *
 * <p>The universe of candidates depends on the wrapper keyword:
 * {@code LIKE} → another {@code DCL-S}; {@code LIKEDS} → a {@code DCL-DS}; {@code LIKEREC} →
 * a file record format (resolution beyond the file name is a Block C concern).
 *
 * <p>For Block B we accept both DCL-S and DCL-DS for any of them and let the user's
 * intent + later inspections sort out type-correctness.
 */
public class BbkTypeReference extends PsiReferenceBase<PsiElement> {

    public BbkTypeReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
        super(element, rangeInElement);
    }

    public BbkTypeReference(@NotNull PsiElement element) {
        super(element, new TextRange(0, element.getTextLength()));
    }

    private static final ResolveCache.Resolver RESOLVER =
        (ref, incomplete) -> ((BbkTypeReference) ref).resolveUncached();

    @Override
    public @Nullable PsiElement resolve() {
        return ResolveCache.getInstance(getElement().getProject())
            .resolveWithCaching(this, RESOLVER, /*needToPreventRecursion*/ true, /*incompleteCode*/ false);
    }

    private @Nullable PsiElement resolveUncached() {
        String name = getValue();
        if (name.isEmpty()) return null;
        // Prefer a DS match; fall back to a DCL-S match for LIKE.
        PsiElement ds = BbkScopeWalker.resolveOfType(getElement(), name, BbkDataStructureDeclaration.class);
        if (ds != null) return ds;
        return BbkScopeWalker.resolveOfType(getElement(), name, BbkVariableDeclaration.class);
    }

    @Override
    public Object @NotNull [] getVariants() {
        return BbkScopeWalker.allVisible(getElement()).stream()
            .filter(d -> d instanceof BbkDataStructureDeclaration || d instanceof BbkVariableDeclaration)
            .toArray();
    }
}
