package com.larena.boxbreaker.plugin.bbk.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.larena.boxbreaker.plugin.bbk.psi.BbkDsSubfield;
import com.larena.boxbreaker.plugin.bbk.psi.BbkInlineParam;
import com.larena.boxbreaker.plugin.bbk.psi.BbkInzModifier;
import com.larena.boxbreaker.plugin.bbk.psi.BbkLiteral;
import com.larena.boxbreaker.plugin.bbk.psi.BbkVariableDeclaration;
import com.larena.boxbreaker.plugin.bbk.types.BbkAssignability;
import com.larena.boxbreaker.plugin.bbk.types.BbkScalarType;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeFromPsi;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reports {@code DCL-S x T INZ(value)} (or DS subfield / inline param)
 * where {@code value}'s type is not assignable to {@code T}.
 *
 * <p>Severity: WARNING.
 */
public class BbkInzValueTypeMismatchInspection extends BbkInspectionBase {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new BbkInspectionVisitor() {
            @Override
            public void visitInzModifier(@NotNull BbkInzModifier inz) {
                BbkType declared = declaredTypeForInz(inz);
                if (declared == null) return;
                BbkType actual = typeOfInzValue(inz);
                if (actual == null) return;
                if (shouldSkip(declared, actual)) return;
                if (BbkAssignability.areCompatible(actual, declared)) return;

                PsiElement target = targetPsiForReport(inz);
                holder.registerProblem(
                    target,
                    "INZ value of type " + actual.getDisplayName()
                        + " is not assignable to declared type " + declared.getDisplayName(),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                );
            }
        };
    }

    private static @Nullable BbkType declaredTypeForInz(@NotNull BbkInzModifier inz) {
        PsiElement p = inz.getParent();
        while (p != null
            && !(p instanceof BbkVariableDeclaration)
            && !(p instanceof BbkDsSubfield)
            && !(p instanceof BbkInlineParam)) {
            p = p.getParent();
        }
        if (p instanceof BbkVariableDeclaration v) return BbkTypeFromPsi.fromSpec(v.getTypeSpecification());
        if (p instanceof BbkDsSubfield sf) return BbkTypeFromPsi.fromSpec(sf.getTypeSpecification());
        if (p instanceof BbkInlineParam ip) return BbkTypeFromPsi.fromSpec(ip.getTypeSpecification());
        return null;
    }

    private static @Nullable BbkType typeOfInzValue(@NotNull BbkInzModifier inz) {
        BbkLiteral lit = inz.getLiteral();
        if (lit != null) return BbkTypeInferrer.typeOfLiteral(lit);
        PsiElement ident = inz.getIdent();
        if (ident != null) {
            for (PsiReference ref : ident.getReferences()) {
                PsiElement target = ref.resolve();
                if (target instanceof com.intellij.psi.PsiNamedElement n) {
                    return BbkTypeInferrer.typeOf(n);
                }
            }
            return null;
        }
        PsiElement star = inz.getStarIdent();
        if (star != null) {
            String t = star.getText();
            if ("*TRUE".equalsIgnoreCase(t) || "*FALSE".equalsIgnoreCase(t)) return BbkScalarType.BOOL;
            if ("*NULL".equalsIgnoreCase(t)) return BbkScalarType.POINTER;
        }
        return null;
    }

    private static @NotNull PsiElement targetPsiForReport(@NotNull BbkInzModifier inz) {
        if (inz.getLiteral() != null) return inz.getLiteral();
        if (inz.getIdent() != null) return inz.getIdent();
        if (inz.getStarIdent() != null) return inz.getStarIdent();
        return inz;
    }
}
