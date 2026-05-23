package com.larena.boxbreaker.plugin.bbk.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.larena.boxbreaker.plugin.bbk.scope.BbkScopeWalker;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Generic IDENT → declaration reference. Used for variable uses, procedure call
 * targets, constant references — anywhere a bare identifier names a previously
 * declared module/local entity.
 *
 * <p>Implemented as a poly-variant reference so a name shared by a {@code DCL-PR} and
 * its {@code DCL-PROC} can return both (decision #5 prefers the procedure definition
 * but the prototype stays reachable via find-usages).
 *
 * <p>All resolution goes through {@link ResolveCache} so opening a 500-line file does
 * not trigger N² re-resolution on every keystroke.
 */
public class BbkIdentReference extends PsiPolyVariantReferenceBase<PsiElement> {

    public BbkIdentReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
        super(element, rangeInElement);
    }

    public BbkIdentReference(@NotNull PsiElement element) {
        super(element, new TextRange(0, element.getTextLength()));
    }

    private static final ResolveCache.PolyVariantResolver<BbkIdentReference> RESOLVER =
        (ref, incomplete) -> ref.resolveUncached();

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        return ResolveCache.getInstance(getElement().getProject())
            .resolveWithCaching(this, RESOLVER, /*needToPreventRecursion*/ true, incompleteCode);
    }

    private ResolveResult @NotNull [] resolveUncached() {
        String name = getValue();
        if (name.isEmpty()) return ResolveResult.EMPTY_ARRAY;
        List<PsiNamedElement> visible = BbkScopeWalker.allVisible(getElement());
        // Collect every visible declaration matching by name (case-insensitive).
        return visible.stream()
            .filter(d -> name.equalsIgnoreCase(d.getName()))
            .map(d -> (ResolveResult) new PsiElementResolveResult(d))
            .toArray(ResolveResult[]::new);
    }

    @Override
    public Object @NotNull [] getVariants() {
        // Each visible declaration becomes a candidate; the lookup-element layer in
        // BbkScopeCompletionProvider will decorate them. Returning the PsiNamedElements
        // directly lets IntelliJ use their getName() as the lookup string.
        return BbkScopeWalker.allVisible(getElement()).toArray();
    }
}
