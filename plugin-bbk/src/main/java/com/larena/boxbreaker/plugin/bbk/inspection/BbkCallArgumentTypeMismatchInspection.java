package com.larena.boxbreaker.plugin.bbk.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.larena.boxbreaker.plugin.bbk.psi.BbkArgumentList;
import com.larena.boxbreaker.plugin.bbk.psi.BbkExpression;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixExpression;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPrimary;
import com.larena.boxbreaker.plugin.bbk.types.BbkAssignability;
import com.larena.boxbreaker.plugin.bbk.types.BbkProcedureType;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Reports {@code f(arg)} where {@code arg}'s type is not assignable to
 * the corresponding positional parameter of {@code f}. If the callee
 * cannot be resolved to a procedure / prototype, the inspection stays
 * silent (Unresolved Reference reports that).
 *
 * <p>Severity: WARNING.
 */
public class BbkCallArgumentTypeMismatchInspection extends BbkInspectionBase {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new BbkInspectionVisitor() {
            @Override
            public void visitCall(@NotNull BbkPostfixExpression postfix,
                                   @NotNull BbkPrimary callee,
                                   @Nullable BbkArgumentList args) {
                if (args == null) return;  // f() — no args to check; count inspection handles
                BbkProcedureType pt = resolveProcType(callee);
                if (pt == null) return;
                List<BbkProcedureType.Parameter> params = pt.getParameters();
                List<BbkExpression> argExprs = args.getExpressionList();
                int n = Math.min(params.size(), argExprs.size());
                for (int i = 0; i < n; i++) {
                    BbkExpression arg = argExprs.get(i);
                    BbkType argType = BbkTypeInferrer.typeOf(arg);
                    BbkType paramType = params.get(i).type();
                    if (shouldSkip(argType, paramType)) continue;
                    if (!BbkAssignability.areCompatible(argType, paramType)) {
                        holder.registerProblem(
                            arg,
                            "Argument " + (i + 1) + " (" + params.get(i).name() + "): "
                                + "expected " + paramType.getDisplayName()
                                + ", got " + argType.getDisplayName(),
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        );
                    }
                }
            }
        };
    }

    /** Resolves the callee primary to a procedure / prototype type, or null. */
    static @Nullable BbkProcedureType resolveProcType(@NotNull BbkPrimary callee) {
        for (PsiReference ref : callee.getReferences()) {
            if (ref instanceof PsiPolyVariantReference poly) {
                for (ResolveResult rr : poly.multiResolve(false)) {
                    PsiElement target = rr.getElement();
                    if (target instanceof PsiNamedElement n) {
                        BbkType t = BbkTypeInferrer.typeOf(n);
                        if (t instanceof BbkProcedureType pt) return pt;
                    }
                }
            } else {
                PsiElement target = ref.resolve();
                if (target instanceof PsiNamedElement n) {
                    BbkType t = BbkTypeInferrer.typeOf(n);
                    if (t instanceof BbkProcedureType pt) return pt;
                }
            }
        }
        return null;
    }
}
