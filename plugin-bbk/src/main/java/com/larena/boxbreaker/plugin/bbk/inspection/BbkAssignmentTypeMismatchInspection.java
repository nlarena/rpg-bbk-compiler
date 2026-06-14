package com.larena.boxbreaker.plugin.bbk.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.larena.boxbreaker.plugin.bbk.psi.BbkExpression;
import com.larena.boxbreaker.plugin.bbk.psi.BbkExpressionStatement;
import com.larena.boxbreaker.plugin.bbk.types.BbkAssignability;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;
import org.jetbrains.annotations.NotNull;

/**
 * Reports {@code LHS = RHS} where the right-hand side's type is not
 * assignable to the left-hand side's. Only fires when both sides are
 * fully resolvable.
 *
 * <p>Severity: WARNING — see {@code inspections/theory.md} §7.
 */
public class BbkAssignmentTypeMismatchInspection extends BbkInspectionBase {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new BbkInspectionVisitor() {
            @Override
            public void visitAssignment(@NotNull BbkExpressionStatement stmt,
                                        @NotNull BbkExpression lhs,
                                        @NotNull BbkExpression rhs) {
                BbkType lhsType = BbkTypeInferrer.typeOf(lhs);
                BbkType rhsType = BbkTypeInferrer.typeOf(rhs);
                if (shouldSkip(lhsType, rhsType)) return;
                if (BbkAssignability.areCompatible(rhsType, lhsType)) return;

                holder.registerProblem(
                    rhs,
                    "Cannot assign " + rhsType.getDisplayName()
                        + " to " + lhsType.getDisplayName(),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                );
            }
        };
    }
}
