package com.larena.boxbreaker.plugin.bbk.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wires every IDENT use-site to its appropriate reference.
 *
 * <p><b>Why composites, not the leaf:</b> IntelliJ's {@code findReferenceAt} (used by
 * Ctrl+B / Ctrl+Click) walks the PSI tree from the leaf upward calling
 * {@code element.getReferences()} at each level. For {@code LeafPsiElement},
 * {@code getReferences()} does NOT consult the {@code PsiReferenceContributor}
 * registry — it returns {@code EMPTY_ARRAY}. So we must register on the
 * <em>composite parents</em> that wrap the IDENT, with the reference's
 * {@code getRangeInElement()} pointing at the IDENT's offset within the composite.
 *
 * <p>Composites we cover:
 * <ul>
 *   <li>{@link BbkPrimary} — generic IDENT in an expression (variable/proc call target).</li>
 *   <li>{@link BbkLikeReference} — IDENT inside {@code LIKE/LIKEDS/LIKEREC(...)}.</li>
 *   <li>{@link BbkExsrStatement} — IDENT after {@code exsr}.</li>
 *   <li>{@link BbkPostfixSuffix} starting with {@code .} — IDENT in member access.</li>
 * </ul>
 */
public class BbkReferenceContributor extends PsiReferenceContributor {


    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

        // ----- BbkPrimary: IDENT inside an expression (variable/proc call target) -----
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(BbkPrimary.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                       @NotNull ProcessingContext ctx) {
                    PsiElement ident = findIdentChild(element);
                    if (ident == null) return PsiReference.EMPTY_ARRAY;
                    TextRange range = ident.getTextRangeInParent();
                    return new PsiReference[]{ new BbkIdentReference(element, range) };
                }
            });

        // ----- BbkLikeReference: IDENT inside LIKE/LIKEDS/LIKEREC(...) -----
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(BbkLikeReference.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                       @NotNull ProcessingContext ctx) {
                    PsiElement ident = findIdentChild(element);
                    if (ident == null) return PsiReference.EMPTY_ARRAY;
                    TextRange range = ident.getTextRangeInParent();
                    return new PsiReference[]{ new BbkTypeReference(element, range) };
                }
            });

        // ----- BbkExsrStatement: IDENT for the subroutine name -----
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(BbkExsrStatement.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                       @NotNull ProcessingContext ctx) {
                    PsiElement ident = findIdentChild(element);
                    if (ident == null) return PsiReference.EMPTY_ARRAY;
                    TextRange range = ident.getTextRangeInParent();
                    return new PsiReference[]{ new BbkSubroutineReference(element, range) };
                }
            });

        // ----- BbkPostfixSuffix: IDENT after `.` (member access) -----
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(BbkPostfixSuffix.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                       @NotNull ProcessingContext ctx) {
                    if (!startsWithDot(element)) return PsiReference.EMPTY_ARRAY;
                    PsiElement ident = findIdentChild(element);
                    if (ident == null) return PsiReference.EMPTY_ARRAY;
                    PsiElement lhs = findLhsOfMemberAccess(element);
                    if (lhs == null) return PsiReference.EMPTY_ARRAY;
                    TextRange range = ident.getTextRangeInParent();
                    return new PsiReference[]{ new BbkMemberReference(element, range, lhs) };
                }
            });
    }

    // ----- Helpers -----

    private static @Nullable PsiElement findIdentChild(@NotNull PsiElement composite) {
        com.intellij.lang.ASTNode node = composite.getNode().findChildByType(BbkTypes.IDENT);
        return node != null ? node.getPsi() : null;
    }

    private static boolean startsWithDot(@NotNull PsiElement suffix) {
        PsiElement first = suffix.getFirstChild();
        return first != null && first.getNode() != null && first.getNode().getElementType() == BbkTypes.DOT;
    }

    private static @Nullable PsiElement findLhsOfMemberAccess(@NotNull PsiElement suffix) {
        BbkPostfixExpression postfix = PsiTreeUtil.getParentOfType(suffix, BbkPostfixExpression.class);
        if (postfix == null) return null;
        PsiElement primary = postfix.getFirstChild();
        return primary instanceof BbkPrimary ? primary : null;
    }
}
