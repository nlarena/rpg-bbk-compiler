package com.larena.boxbreaker.plugin.bbk.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.BbkExpression;
import com.larena.boxbreaker.plugin.bbk.psi.BbkProcedureDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkReturnStatement;
import com.larena.boxbreaker.plugin.bbk.psi.BbkReturnType;
import com.larena.boxbreaker.plugin.bbk.types.BbkAssignability;
import com.larena.boxbreaker.plugin.bbk.types.BbkScalarType;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeFromPsi;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;
import org.jetbrains.annotations.NotNull;

/**
 * Reports {@code return EXPR;} where EXPR's type does not fit the
 * enclosing procedure's declared return type. Also flags
 * {@code return value;} inside a {@code -> VOID} procedure (returning
 * a value from a procedure declared to return nothing).
 *
 * <p>Severity: WARNING.
 */
public class BbkReturnTypeMismatchInspection extends BbkInspectionBase {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new BbkInspectionVisitor() {
            @Override
            public void visitReturnWithValue(@NotNull BbkReturnStatement stmt,
                                              @NotNull BbkExpression value) {
                BbkProcedureDeclaration proc = PsiTreeUtil.getParentOfType(stmt, BbkProcedureDeclaration.class);
                if (proc == null) return;
                BbkReturnType returnTypeNode = proc.getReturnType();
                BbkType expected = returnTypeNode != null
                    ? BbkTypeFromPsi.fromSpec(returnTypeNode.getTypeSpecification())
                    : BbkScalarType.VOID;
                BbkType actual = BbkTypeInferrer.typeOf(value);
                if (shouldSkip(expected, actual)) return;

                if (expected.equals(BbkScalarType.VOID)) {
                    holder.registerProblem(
                        value,
                        "Procedure returns VOID; remove the return value",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    );
                    return;
                }

                if (!BbkAssignability.areCompatible(actual, expected)) {
                    holder.registerProblem(
                        value,
                        "Cannot return " + actual.getDisplayName()
                            + " from procedure returning " + expected.getDisplayName(),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    );
                }
            }
        };
    }
}
