package com.larena.boxbreaker.plugin.bbk.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.BbkDataStructureDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkDsSubfield;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference for the IDENT after a {@code .} (member access on a DS).
 *
 * <p>The reference's owning element is the field IDENT; the left-hand side is
 * supplied at construction time so resolution can look up its type without
 * re-traversing the PSI.
 *
 * <p>Resolution flow:
 *
 * <ol>
 *   <li>Find the {@link BbkDataStructureDeclaration} that the LHS refers to via
 *       {@link BbkTypeResolver#dsOf(PsiElement)}.</li>
 *   <li>Among the subfields of that DS, return the one whose name matches.</li>
 * </ol>
 */
public class BbkMemberReference extends PsiReferenceBase<PsiElement> {

    private final @NotNull PsiElement lhs;

    public BbkMemberReference(@NotNull PsiElement element, @NotNull PsiElement lhs) {
        super(element, new TextRange(0, element.getTextLength()));
        this.lhs = lhs;
    }

    private static final ResolveCache.Resolver RESOLVER =
        (ref, incomplete) -> ((BbkMemberReference) ref).resolveUncached();

    @Override
    public @Nullable PsiElement resolve() {
        return ResolveCache.getInstance(getElement().getProject())
            .resolveWithCaching(this, RESOLVER, /*needToPreventRecursion*/ true, /*incompleteCode*/ false);
    }

    private @Nullable PsiElement resolveUncached() {
        String name = getValue();
        if (name.isEmpty()) return null;
        BbkDataStructureDeclaration ds = BbkTypeResolver.dsOf(lhs);
        if (ds == null) return null;
        for (BbkDsSubfield sf : PsiTreeUtil.findChildrenOfType(ds, BbkDsSubfield.class)) {
            if (name.equalsIgnoreCase(sf.getName())) return sf;
        }
        return null;
    }

    @Override
    public Object @NotNull [] getVariants() {
        BbkDataStructureDeclaration ds = BbkTypeResolver.dsOf(lhs);
        if (ds == null) return new Object[0];
        List<BbkDsSubfield> out = new ArrayList<>();
        for (BbkDsSubfield sf : PsiTreeUtil.findChildrenOfType(ds, BbkDsSubfield.class)) {
            out.add(sf);
        }
        return out.toArray();
    }
}
