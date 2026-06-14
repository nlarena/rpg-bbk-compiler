package com.larena.boxbreaker.plugin.bbk.inspection;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.BbkArgumentList;
import com.larena.boxbreaker.plugin.bbk.psi.BbkDoWhileStatement;
import com.larena.boxbreaker.plugin.bbk.psi.BbkExpression;
import com.larena.boxbreaker.plugin.bbk.psi.BbkExpressionStatement;
import com.larena.boxbreaker.plugin.bbk.psi.BbkForStatement;
import com.larena.boxbreaker.plugin.bbk.psi.BbkIfStatement;
import com.larena.boxbreaker.plugin.bbk.psi.BbkInzModifier;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixExpression;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPrimary;
import com.larena.boxbreaker.plugin.bbk.psi.BbkReturnStatement;
import com.larena.boxbreaker.plugin.bbk.psi.BbkWhenClause;
import com.larena.boxbreaker.plugin.bbk.psi.BbkWhileStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Abstract {@link PsiElementVisitor} that exposes semantic hooks
 * (assignment / call / return / condition / INZ) so inspections do not
 * each re-implement the syntactic detection.
 *
 * <p>Subclasses override only the hooks they care about. The default
 * implementation of each hook is a no-op.
 *
 * <p>{@link #visitElement(PsiElement)} is the single entry point IntelliJ
 * calls; it dispatches by type and translates the PSI shape into a
 * semantic event.
 */
public abstract class BbkInspectionVisitor extends PsiElementVisitor {

    // ----- Hooks subclasses override -----

    /**
     * Called for every {@code lhs = rhs} (any assignment operator).
     * {@code lhs} is the first expression, {@code rhs} the second.
     */
    public void visitAssignment(@NotNull BbkExpressionStatement stmt,
                                @NotNull BbkExpression lhs,
                                @NotNull BbkExpression rhs) {}

    /**
     * Called for every call site {@code callee(arg, arg, ...)}.
     * {@code callee} is the primary holding the IDENT being called.
     * {@code args} is {@code null} when the call is {@code f()} with no
     * arguments — subclasses interested in count should treat null as 0.
     */
    public void visitCall(@NotNull BbkPostfixExpression postfix,
                          @NotNull BbkPrimary callee,
                          @Nullable BbkArgumentList args) {}

    /**
     * Called for {@code return EXPR;} (only when EXPR is present).
     */
    public void visitReturnWithValue(@NotNull BbkReturnStatement stmt,
                                     @NotNull BbkExpression value) {}

    /**
     * Called for any BOOL-context expression: {@code if (e)}, {@code while (e)},
     * {@code do {} while (e)}, {@code when (e)}, {@code for (...; e; ...)}.
     */
    public void visitBoolCondition(@NotNull PsiElement enclosing,
                                   @NotNull BbkExpression condition) {}

    /**
     * Called for {@code INZ(value)} where {@code value} is parseable as an
     * expression (a literal or IDENT).
     */
    public void visitInzModifier(@NotNull BbkInzModifier inz) {}

    // ----- Dispatch -----

    @Override
    public final void visitElement(@NotNull PsiElement element) {
        // Assignments
        if (element instanceof BbkExpressionStatement stmt) {
            if (stmt.getAssignmentOp() != null) {
                List<BbkExpression> exprs = stmt.getExpressionList();
                if (exprs.size() >= 2) {
                    visitAssignment(stmt, exprs.get(0), exprs.get(1));
                }
            }
            return;
        }

        // Calls — a postfix expression whose last suffix is `( args )` or `()`.
        if (element instanceof BbkPostfixExpression pe) {
            List<BbkPostfixSuffix> suffixes = pe.getPostfixSuffixList();
            if (!suffixes.isEmpty()) {
                BbkPostfixSuffix last = suffixes.get(suffixes.size() - 1);
                if (last.getArgumentList() != null || isEmptyParens(last)) {
                    visitCall(pe, pe.getPrimary(), last.getArgumentList());
                }
            }
            return;
        }

        // Return with value
        if (element instanceof BbkReturnStatement ret) {
            BbkExpression e = PsiTreeUtil.getChildOfType(ret, BbkExpression.class);
            if (e != null) visitReturnWithValue(ret, e);
            return;
        }

        // BOOL conditions
        if (element instanceof BbkIfStatement is) {
            BbkExpression cond = is.getExpression();
            if (cond != null) visitBoolCondition(is, cond);
            return;
        }
        if (element instanceof BbkWhileStatement ws) {
            BbkExpression cond = PsiTreeUtil.getChildOfType(ws, BbkExpression.class);
            if (cond != null) visitBoolCondition(ws, cond);
            return;
        }
        if (element instanceof BbkDoWhileStatement dws) {
            BbkExpression cond = PsiTreeUtil.getChildOfType(dws, BbkExpression.class);
            if (cond != null) visitBoolCondition(dws, cond);
            return;
        }
        if (element instanceof BbkWhenClause wc) {
            BbkExpression cond = PsiTreeUtil.getChildOfType(wc, BbkExpression.class);
            if (cond != null) visitBoolCondition(wc, cond);
            return;
        }
        if (element instanceof BbkForStatement fs) {
            // for (init; test; update) — `test` is the middle BbkExpression child.
            BbkExpression test = fs.getExpression();
            if (test != null) visitBoolCondition(fs, test);
            return;
        }

        // INZ(value)
        if (element instanceof BbkInzModifier inz) {
            visitInzModifier(inz);
        }
    }

    // ----- helpers -----

    private static boolean isEmptyParens(@NotNull BbkPostfixSuffix s) {
        return s.getArgumentList() == null
            && s.getSubscriptList() == null
            && s.getIdent() == null;
    }
}
