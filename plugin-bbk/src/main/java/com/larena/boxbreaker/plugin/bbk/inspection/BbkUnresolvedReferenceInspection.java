package com.larena.boxbreaker.plugin.bbk.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.larena.boxbreaker.plugin.bbk.builtins.BbkBuiltinRegistry;
import com.larena.boxbreaker.plugin.bbk.psi.BbkLikeReference;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPrimary;
import com.larena.boxbreaker.plugin.bbk.psi.BbkTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Reports an IDENT use whose {@code reference.resolve()} (or
 * {@code multiResolve}) finds no declaration anywhere — neither in local
 * scope nor in the project-wide stub indexes.
 *
 * <p>Severity is ERROR — the code is genuinely broken.
 */
public class BbkUnresolvedReferenceInspection extends BbkInspectionBase {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                // Look at composites that own an IDENT-bearing reference.
                if (!(element instanceof BbkPrimary)
                    && !(element instanceof BbkLikeReference)
                    && !(element instanceof BbkPostfixSuffix)) {
                    return;
                }

                // Locate the IDENT inside this composite. If there's none, skip
                // (BbkPrimary may hold a literal / parenthesis / *NULL / etc.).
                com.intellij.lang.ASTNode identNode = element.getNode().findChildByType(BbkTypes.IDENT);
                if (identNode == null) return;

                // BbkPostfixSuffix only carries a reference when it's a member access
                // (starts with `.`). Filter accordingly.
                if (element instanceof BbkPostfixSuffix && !startsWithDot(element)) return;

                PsiReference[] refs = element.getReferences();
                if (refs.length == 0) return;

                boolean anyResolved = false;
                for (PsiReference ref : refs) {
                    if (ref instanceof PsiPolyVariantReference poly) {
                        ResolveResult[] rr = poly.multiResolve(false);
                        for (ResolveResult r : rr) {
                            if (r.getElement() != null) {
                                anyResolved = true;
                                break;
                            }
                        }
                    } else if (ref.resolve() != null) {
                        anyResolved = true;
                    }
                    if (anyResolved) break;
                }

                if (!anyResolved) {
                    PsiElement ident = identNode.getPsi();
                    // Los builtins (print, trim, len, ...) no tienen declaración PSI:
                    // se reconocen por el catálogo, no se marcan como sin resolver.
                    if (element instanceof BbkPrimary && BbkBuiltinRegistry.isBuiltin(ident.getText())) {
                        return;
                    }
                    holder.registerProblem(
                        ident,
                        "Unresolved reference: '" + ident.getText() + "'",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    );
                }
            }
        };
    }

    private static boolean startsWithDot(@NotNull PsiElement suffix) {
        PsiElement first = suffix.getFirstChild();
        return first != null && first.getNode() != null
            && first.getNode().getElementType() == BbkTypes.DOT;
    }
}
