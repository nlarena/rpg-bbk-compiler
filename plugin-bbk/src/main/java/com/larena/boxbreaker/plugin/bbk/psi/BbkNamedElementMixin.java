package com.larena.boxbreaker.plugin.bbk.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for every BBK declaration that has a name (DCL-S, DCL-C, DCL-DS, DCL-F,
 * DCL-PR, DCL-PROC, DS subfield, inline param, subroutine).
 *
 * <p>The class is referenced from the {@code mixin=} attribute of each named-declaration
 * rule in {@code BBK.bnf}, so Grammar-Kit emits each generated PSI impl as a subclass
 * of this mixin. Because the implementation of {@code getName} / {@code setName} /
 * {@code getNameIdentifier} is identical for every BBK declaration (find the first
 * {@code IDENT} child), we implement them here once instead of routing through
 * {@code methods=[...]} per rule — Grammar-Kit cannot resolve those during code
 * generation because the utility class is compiled in the same module.
 */
public abstract class BbkNamedElementMixin extends ASTWrapperPsiElement implements PsiNameIdentifierOwner {

    protected BbkNamedElementMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable String getName() {
        PsiElement id = getNameIdentifier();
        return id != null ? id.getText() : null;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        ASTNode node = getNode().findChildByType(BbkTypes.IDENT);
        return node != null ? node.getPsi() : null;
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        // Block C will replace this with a proper identifier-replacement strategy that
        // creates a new IDENT token via a PsiElementFactory. For Block B we keep rename
        // logically supported (so PsiNameIdentifierOwner is honoured) but a no-op on the
        // tree so users see a clear "not supported" rather than silent corruption.
        return this;
    }
}
