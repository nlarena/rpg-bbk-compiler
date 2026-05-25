package com.larena.boxbreaker.plugin.bbk.completion.expected;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import com.larena.boxbreaker.plugin.bbk.types.BbkProcedureType;
import com.larena.boxbreaker.plugin.bbk.types.BbkScalarType;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeFromPsi;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;
import com.larena.boxbreaker.plugin.bbk.types.BbkUnknownType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Recognises the syntactic context at the caret and returns the
 * {@link BbkType} that position requires. Returns {@code null} when no
 * typed context is recognised — smart completion then contributes nothing
 * and the user falls back to BASIC.
 *
 * <p>Shape A from {@code smart-completion/classes.md}: one class with
 * pattern-dispatch by walking the PSI upward from the caret leaf.
 *
 * <p>Recognised contexts:
 * <ol>
 *   <li>Assignment RHS — {@code LHS = <caret>} → type of LHS.</li>
 *   <li>{@code if/while/when (<caret>)} condition → {@link BbkScalarType#BOOL}.</li>
 *   <li>{@code for (...; <caret>; ...)} loop test → BOOL.</li>
 *   <li>{@code return <caret>;} → enclosing procedure's return type.</li>
 *   <li>{@code DCL-S x T INZ(<caret>)} → declared type T.</li>
 *   <li>{@code f(<caret>)} or {@code f(a, <caret>)} → matching parameter type.</li>
 * </ol>
 */
public final class BbkExpectedTypeProvider {

    private BbkExpectedTypeProvider() {}

    public static @Nullable BbkType find(@NotNull PsiElement caretLeaf) {
        // Walk upward. At each ancestor decide whether the caret position is
        // a "type-bearing slot" of that ancestor.
        PsiElement prev = caretLeaf;
        PsiElement cur = caretLeaf.getParent();
        while (cur != null) {
            BbkType t = tryAt(cur, prev);
            if (t != null) return unwrap(t);
            prev = cur;
            cur = cur.getParent();
            // Don't walk past the file.
            if (cur instanceof BbkFile) break;
        }
        return null;
    }

    private static @NotNull BbkType unwrap(@NotNull BbkType t) {
        return t;
    }

    private static @Nullable BbkType tryAt(@NotNull PsiElement node, @NotNull PsiElement childAtCaret) {
        // ----- Assignment: expression_statement (expr = expr) -----
        if (node instanceof BbkExpressionStatement stmt) {
            List<BbkExpression> exprs = stmt.getExpressionList();
            if (stmt.getAssignmentOp() != null && exprs.size() >= 2) {
                // RHS expression is exprs.get(1). Check the caret is inside it.
                if (PsiTreeUtil.isAncestor(exprs.get(1), childAtCaret, false)
                    || childAtCaret == exprs.get(1)) {
                    return BbkTypeInferrer.typeOf(exprs.get(0));
                }
            }
        }

        // ----- if / while / when / do-while / monitor on-error condition -----
        if (node instanceof BbkIfStatement
            || node instanceof BbkWhileStatement
            || node instanceof BbkDoWhileStatement
            || node instanceof BbkWhenClause) {
            BbkExpression condExpr = PsiTreeUtil.getChildOfType(node, BbkExpression.class);
            if (condExpr != null && (PsiTreeUtil.isAncestor(condExpr, childAtCaret, false) || childAtCaret == condExpr)) {
                return BbkScalarType.BOOL;
            }
        }

        // ----- for-loop test (middle slot) — BOOL -----
        if (node instanceof BbkForStatement forStmt) {
            BbkExpression test = forStmt.getExpression();
            if (test != null && (PsiTreeUtil.isAncestor(test, childAtCaret, false) || childAtCaret == test)) {
                return BbkScalarType.BOOL;
            }
        }

        // ----- return EXPR; -----
        if (node instanceof BbkReturnStatement ret) {
            BbkExpression e = PsiTreeUtil.getChildOfType(ret, BbkExpression.class);
            if (e != null && (PsiTreeUtil.isAncestor(e, childAtCaret, false) || childAtCaret == e)) {
                BbkProcedureDeclaration proc = PsiTreeUtil.getParentOfType(ret, BbkProcedureDeclaration.class);
                if (proc != null && proc.getReturnType() != null) {
                    return BbkTypeFromPsi.fromSpec(proc.getReturnType().getTypeSpecification());
                }
                return BbkScalarType.VOID;
            }
        }

        // ----- INZ(<caret>) on a DCL-S / DCL-SUBF / inline param -----
        if (node instanceof BbkInzModifier inz) {
            PsiElement decl = inz.getParent();
            while (decl != null
                && !(decl instanceof BbkVariableDeclaration)
                && !(decl instanceof BbkDsSubfield)
                && !(decl instanceof BbkInlineParam)) {
                decl = decl.getParent();
            }
            if (decl instanceof BbkVariableDeclaration v) return BbkTypeFromPsi.fromSpec(v.getTypeSpecification());
            if (decl instanceof BbkDsSubfield sf) return BbkTypeFromPsi.fromSpec(sf.getTypeSpecification());
            if (decl instanceof BbkInlineParam p) return BbkTypeFromPsi.fromSpec(p.getTypeSpecification());
        }

        // ----- Call argument: f(<caret>) or f(a, <caret>, ...) -----
        if (node instanceof BbkArgumentList argList) {
            // Locate which positional arg the caret is in.
            int argIdx = -1;
            List<BbkExpression> args = argList.getExpressionList();
            for (int i = 0; i < args.size(); i++) {
                if (PsiTreeUtil.isAncestor(args.get(i), childAtCaret, false) || childAtCaret == args.get(i)) {
                    argIdx = i;
                    break;
                }
            }
            if (argIdx < 0) {
                // Caret in whitespace just before/after a token? Fall back to argIdx = number of expressions
                // (i.e. the slot the user is about to type into).
                argIdx = args.size();
            }
            // The call is the parent BbkPostfixSuffix whose parent BbkPostfixExpression's primary holds the callee.
            BbkPostfixSuffix suffix = PsiTreeUtil.getParentOfType(argList, BbkPostfixSuffix.class);
            BbkPostfixExpression postfix = suffix != null ? PsiTreeUtil.getParentOfType(suffix, BbkPostfixExpression.class) : null;
            if (postfix != null) {
                BbkType calleeType = BbkTypeInferrer.typeOf(postfix.getPrimary().getExpression() != null
                    ? postfix.getPrimary().getExpression()
                    : null);
                // The above only handles parenthesised; the typical case is primary IDENT — read via reference.
                if (calleeType instanceof BbkUnknownType) {
                    // Resolve the primary's reference to find the procedure.
                    BbkType primaryType = BbkTypeInferrer.typeOf((BbkExpression) null);  // placeholder
                    // Use the primary directly:
                    calleeType = typeOfCallee(postfix.getPrimary());
                }
                if (calleeType instanceof BbkProcedureType pt) {
                    List<BbkProcedureType.Parameter> params = pt.getParameters();
                    if (argIdx >= 0 && argIdx < params.size()) {
                        return params.get(argIdx).type();
                    }
                }
            }
        }

        return null;
    }

    private static @NotNull BbkType typeOfCallee(@NotNull BbkPrimary primary) {
        // typeOf on a primary that is an IDENT returns the procedure type via reference resolution.
        // Reuse the inferrer through a synthetic expression containing the primary:
        // simpler — call resolve on the primary's reference directly.
        for (com.intellij.psi.PsiReference ref : primary.getReferences()) {
            com.intellij.psi.PsiElement t = ref.resolve();
            if (t instanceof com.intellij.psi.PsiNamedElement n) {
                BbkType type = BbkTypeInferrer.typeOf(n);
                if (type instanceof BbkProcedureType) return type;
            }
            if (ref instanceof com.intellij.psi.PsiPolyVariantReference poly) {
                for (com.intellij.psi.ResolveResult rr : poly.multiResolve(false)) {
                    PsiElement target = rr.getElement();
                    if (target instanceof com.intellij.psi.PsiNamedElement n) {
                        BbkType type = BbkTypeInferrer.typeOf(n);
                        if (type instanceof BbkProcedureType) return type;
                    }
                }
            }
        }
        return BbkUnknownType.INSTANCE;
    }
}
