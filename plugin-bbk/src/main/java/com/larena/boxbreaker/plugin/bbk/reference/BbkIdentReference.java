package com.larena.boxbreaker.plugin.bbk.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.util.IncorrectOperationException;
import com.larena.boxbreaker.plugin.bbk.index.BbkIndexKeys;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import com.larena.boxbreaker.plugin.bbk.psi.factory.BbkElementFactory;
import com.larena.boxbreaker.plugin.bbk.scope.BbkScopeWalker;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic IDENT → declaration reference. Used for variable uses, procedure call
 * targets, constant references — anywhere a bare identifier names a previously
 * declared module/local entity.
 *
 * <p>Implemented as a poly-variant reference so a name shared by a {@code DCL-PR} and
 * its {@code DCL-PROC} can return both.
 *
 * <p>All resolution goes through {@link ResolveCache}.
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

        // 1) Local scope.
        List<PsiNamedElement> visible = BbkScopeWalker.allVisible(getElement());
        List<ResolveResult> results = new ArrayList<>();
        for (PsiNamedElement d : visible) {
            if (name.equalsIgnoreCase(d.getName())) {
                results.add(new PsiElementResolveResult(d));
            }
        }
        if (!results.isEmpty()) {
            return results.toArray(ResolveResult.EMPTY_ARRAY);
        }

        // 2) Cross-file fallback via stub indexes.
        var project = getElement().getProject();
        for (BbkProcedureDeclaration p : BbkProjectScopeLookup.findInProject(
                project, name, BbkIndexKeys.PROCEDURE, BbkProcedureDeclaration.class)) {
            results.add(new PsiElementResolveResult(p));
        }
        for (BbkPrototypeDeclaration p : BbkProjectScopeLookup.findInProject(
                project, name, BbkIndexKeys.PROTOTYPE, BbkPrototypeDeclaration.class)) {
            results.add(new PsiElementResolveResult(p));
        }
        for (BbkConstantDeclaration c : BbkProjectScopeLookup.findInProject(
                project, name, BbkIndexKeys.CONSTANT, BbkConstantDeclaration.class)) {
            results.add(new PsiElementResolveResult(c));
        }
        for (BbkVariableDeclaration v : BbkProjectScopeLookup.findInProject(
                project, name, BbkIndexKeys.VARIABLE, BbkVariableDeclaration.class)) {
            results.add(new PsiElementResolveResult(v));
        }
        for (BbkDataStructureDeclaration ds : BbkProjectScopeLookup.findInProject(
                project, name, BbkIndexKeys.DATA_STRUCTURE, BbkDataStructureDeclaration.class)) {
            results.add(new PsiElementResolveResult(ds));
        }
        for (BbkFileDeclaration f : BbkProjectScopeLookup.findInProject(
                project, name, BbkIndexKeys.FILE_DECLARATION, BbkFileDeclaration.class)) {
            results.add(new PsiElementResolveResult(f));
        }
        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @Override
    public Object @NotNull [] getVariants() {
        List<Object> out = new ArrayList<>(BbkScopeWalker.allVisible(getElement()));
        return out.toArray();
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newName) throws IncorrectOperationException {
        PsiElement oldId = BbkElementFactory.findIdentInRange(getElement(), getRangeInElement());
        if (oldId == null) return getElement();
        PsiElement newId = BbkElementFactory.createIdentifier(getElement().getProject(), newName);
        oldId.replace(newId);
        return getElement();
    }
}
