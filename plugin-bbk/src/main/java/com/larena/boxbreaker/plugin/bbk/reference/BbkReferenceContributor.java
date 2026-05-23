package com.larena.boxbreaker.plugin.bbk.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.larena.boxbreaker.plugin.bbk.BbkLanguage;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wires every IDENT use-site to its appropriate reference.
 *
 * <p>Strategy: register one provider for any IDENT leaf token in a BBK file, then
 * dispatch on the parent shape:
 *
 * <ul>
 *   <li>IDENT inside {@code exsr} → {@link BbkSubroutineReference}</li>
 *   <li>IDENT inside {@code LIKE/LIKEDS/LIKEREC} → {@link BbkTypeReference}</li>
 *   <li>IDENT preceded by a {@code .} (member access RHS) → {@link BbkMemberReference}</li>
 *   <li>IDENT that is a declaration's own name → no reference (it <em>is</em> the declaration)</li>
 *   <li>Otherwise (use-site in an expression / call / lvalue) → {@link BbkIdentReference}</li>
 * </ul>
 */
public class BbkReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withLanguage(BbkLanguage.INSTANCE),
            new PsiReferenceProvider() {
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                       @NotNull ProcessingContext ctx) {
                    return referencesFor(element);
                }
            });
    }

    private static PsiReference @NotNull [] referencesFor(@NotNull PsiElement element) {
        if (element.getNode() == null || element.getNode().getElementType() != BbkTypes.IDENT) {
            return PsiReference.EMPTY_ARRAY;
        }
        PsiElement parent = element.getParent();
        if (parent == null) return PsiReference.EMPTY_ARRAY;

        // IDENT that is the name of its enclosing declaration → no reference.
        if (isDeclarationName(parent, element)) return PsiReference.EMPTY_ARRAY;

        // exsr <name>
        if (parent instanceof BbkExsrStatement) {
            return wrap(new BbkSubroutineReference(element));
        }
        // LIKE(<name>) / LIKEDS(<name>) / LIKEREC(<name>)
        if (PsiTreeUtil.getParentOfType(element, BbkLikeReference.class) != null) {
            return wrap(new BbkTypeReference(element));
        }
        // Member access: parent is a postfix_suffix that starts with DOT and contains this IDENT.
        if (parent instanceof BbkPostfixSuffix && startsWithDot(parent)) {
            PsiElement lhs = findLhsOfMemberAccess(parent);
            if (lhs != null) {
                return wrap(new BbkMemberReference(element, lhs));
            }
        }
        // Default: generic identifier reference.
        return wrap(new BbkIdentReference(element));
    }

    private static boolean isDeclarationName(@NotNull PsiElement parent, @NotNull PsiElement ident) {
        if (parent instanceof PsiNameIdentifierOwner owner) {
            PsiElement nameId = owner.getNameIdentifier();
            return nameId != null && nameId.getNode() == ident.getNode();
        }
        return false;
    }

    private static boolean startsWithDot(@NotNull PsiElement suffix) {
        PsiElement first = suffix.getFirstChild();
        return first != null && first.getNode() != null && first.getNode().getElementType() == BbkTypes.DOT;
    }

    /**
     * Walks the postfix-expression chain to find the receiver of a member access.
     * For {@code employee.firstName}, the receiver is the {@code BbkPrimary} containing
     * {@code employee}.
     */
    private static @Nullable PsiElement findLhsOfMemberAccess(@NotNull PsiElement suffix) {
        BbkPostfixExpression postfix = PsiTreeUtil.getParentOfType(suffix, BbkPostfixExpression.class);
        if (postfix == null) return null;
        PsiElement primary = postfix.getFirstChild();
        return primary instanceof BbkPrimary ? primary : null;
    }

    private static PsiReference @NotNull [] wrap(@NotNull PsiReference ref) {
        return new PsiReference[]{ref};
    }
}
