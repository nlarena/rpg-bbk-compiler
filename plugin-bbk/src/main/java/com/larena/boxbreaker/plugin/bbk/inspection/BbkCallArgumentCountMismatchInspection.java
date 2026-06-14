package com.larena.boxbreaker.plugin.bbk.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.larena.boxbreaker.plugin.bbk.psi.BbkArgumentList;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixExpression;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPrimary;
import com.larena.boxbreaker.plugin.bbk.types.BbkProcedureType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reports {@code f(a, b)} where {@code f} expects a different number of
 * positional arguments.
 *
 * <p>Severity: WARNING.
 */
public class BbkCallArgumentCountMismatchInspection extends BbkInspectionBase {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new BbkInspectionVisitor() {
            @Override
            public void visitCall(@NotNull BbkPostfixExpression postfix,
                                   @NotNull BbkPrimary callee,
                                   @Nullable BbkArgumentList args) {
                BbkProcedureType pt = BbkCallArgumentTypeMismatchInspection.resolveProcType(callee);
                if (pt == null) return;
                int expected = pt.getParameters().size();
                int actual = args == null ? 0 : args.getExpressionList().size();
                if (expected == actual) return;

                // Anchor the squiggly at the args (or at the empty parens via the suffix).
                // We point at args when available; otherwise at the postfix expression.
                holder.registerProblem(
                    args != null ? args : (com.intellij.psi.PsiElement) postfix,
                    "Expected " + expected + " argument" + (expected == 1 ? "" : "s")
                        + ", got " + actual,
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                );
            }
        };
    }
}
