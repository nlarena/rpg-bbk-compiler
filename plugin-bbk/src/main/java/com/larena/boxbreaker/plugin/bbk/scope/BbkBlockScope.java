package com.larena.boxbreaker.plugin.bbk.scope;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.larena.boxbreaker.plugin.bbk.psi.BbkBlockStatement;
import com.larena.boxbreaker.plugin.bbk.psi.BbkVariableDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkConstantDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkDataStructureDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Scope of a single {@link BbkBlockStatement} (the {@code { ... }} of an {@code if},
 * {@code while}, {@code for}, etc.).
 *
 * <p>Picks up declarations made <em>directly</em> in this block — not nested inside an
 * inner block, since those have their own scope. The {@link BbkScopeWalker} chains
 * block scopes from innermost to outermost when the cursor sits inside a nested block.
 *
 * <p>Also picks up the inline declaration of a {@code for (DCL-S i ...; ...; ...)}
 * header, exposing {@code i} inside the for body.
 */
public class BbkBlockScope implements BbkScope {

    private final @NotNull PsiElement scopeOwner;
    private final @NotNull BbkScope parent;

    public BbkBlockScope(@NotNull PsiElement scopeOwner, @NotNull BbkScope parent) {
        this.scopeOwner = scopeOwner;
        this.parent = parent;
    }

    @Override
    public @NotNull List<PsiNamedElement> getDeclarations() {
        List<PsiNamedElement> out = new ArrayList<>();
        collectDirect(scopeOwner, out);
        return out;
    }

    @Override
    public @Nullable BbkScope getParent() {
        return parent;
    }

    /**
     * Walks only the direct children of {@code parent}, gathering named declarations
     * but stopping at nested {@link BbkBlockStatement}s (those have their own scope).
     */
    private static void collectDirect(@NotNull PsiElement parent, @NotNull List<PsiNamedElement> out) {
        for (PsiElement child : parent.getChildren()) {
            if (child instanceof BbkBlockStatement && child != parent) {
                continue; // nested block has its own scope
            }
            if (child instanceof BbkVariableDeclaration
                || child instanceof BbkConstantDeclaration
                || child instanceof BbkDataStructureDeclaration) {
                out.add((PsiNamedElement) child);
            } else {
                // TODO Block C: surface `for (DCL-S i ...; ...; ...)` inline declarations as PsiNamedElement.
                // The grammar emits them as BbkForInlineDecl (its own rule, not a BbkVariableDeclaration),
                // so they need their own mixin if we want them visible by name.
                // Recurse only into transparent containers (for headers, etc.) — never into block_statement.
                if (!(child instanceof BbkBlockStatement)) {
                    collectDirect(child, out);
                }
            }
        }
    }
}
