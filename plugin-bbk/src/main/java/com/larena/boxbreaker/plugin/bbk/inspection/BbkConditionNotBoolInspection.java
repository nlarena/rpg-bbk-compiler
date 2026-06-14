package com.larena.boxbreaker.plugin.bbk.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.larena.boxbreaker.plugin.bbk.psi.BbkExpression;
import com.larena.boxbreaker.plugin.bbk.types.BbkAssignability;
import com.larena.boxbreaker.plugin.bbk.types.BbkScalarType;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;
import org.jetbrains.annotations.NotNull;

/**
 * Reports {@code if (e)}, {@code while (e)}, {@code when (e)},
 * {@code for (...; e; ...)}, {@code do {} while (e)} where {@code e}'s
 * type is not assignable to {@link BbkScalarType#BOOL}.
 *
 * <p>Severity: WARNING.
 */
public class BbkConditionNotBoolInspection extends BbkInspectionBase {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new BbkInspectionVisitor() {
            @Override
            public void visitBoolCondition(@NotNull PsiElement enclosing,
                                            @NotNull BbkExpression condition) {
                BbkType condType = BbkTypeInferrer.typeOf(condition);
                if (shouldSkip(condType)) return;
                if (BbkAssignability.areCompatible(condType, BbkScalarType.BOOL)) return;

                holder.registerProblem(
                    condition,
                    "Condition must be BOOL, got " + condType.getDisplayName(),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                );
            }
        };
    }
}
