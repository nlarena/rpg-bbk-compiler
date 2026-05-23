package com.larena.boxbreaker.plugin.bbk.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.BbkProcedureDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkSubroutineDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference for the IDENT inside {@code exsr <name>} → its {@code begsr <name>}.
 *
 * <p>Subroutine visibility is procedure-scoped (decision #4 of
 * {@code reference-scope/classes.md} §7): {@code exsr} from one procedure cannot reach
 * a {@code begsr} declared in another.
 */
public class BbkSubroutineReference extends PsiReferenceBase<PsiElement> {

    public BbkSubroutineReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
        super(element, rangeInElement);
    }

    public BbkSubroutineReference(@NotNull PsiElement element) {
        super(element, new TextRange(0, element.getTextLength()));
    }

    private static final ResolveCache.Resolver RESOLVER =
        (ref, incomplete) -> ((BbkSubroutineReference) ref).resolveUncached();

    @Override
    public @Nullable PsiElement resolve() {
        return ResolveCache.getInstance(getElement().getProject())
            .resolveWithCaching(this, RESOLVER, /*needToPreventRecursion*/ true, /*incompleteCode*/ false);
    }

    private @Nullable PsiElement resolveUncached() {
        String name = getValue();
        if (name.isEmpty()) return null;
        BbkProcedureDeclaration proc = PsiTreeUtil.getParentOfType(getElement(), BbkProcedureDeclaration.class);
        if (proc == null) return null;
        for (BbkSubroutineDefinition sr : PsiTreeUtil.findChildrenOfType(proc, BbkSubroutineDefinition.class)) {
            if (name.equalsIgnoreCase(sr.getName())) return sr;
        }
        return null;
    }

    @Override
    public Object @NotNull [] getVariants() {
        BbkProcedureDeclaration proc = PsiTreeUtil.getParentOfType(getElement(), BbkProcedureDeclaration.class);
        if (proc == null) return new Object[0];
        List<BbkSubroutineDefinition> out = new ArrayList<>();
        for (BbkSubroutineDefinition sr : PsiTreeUtil.findChildrenOfType(proc, BbkSubroutineDefinition.class)) {
            out.add(sr);
        }
        return out.toArray();
    }
}
