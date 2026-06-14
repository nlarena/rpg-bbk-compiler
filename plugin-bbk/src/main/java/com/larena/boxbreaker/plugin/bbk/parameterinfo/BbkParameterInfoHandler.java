package com.larena.boxbreaker.plugin.bbk.parameterinfo;

import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.ParameterInfoUtils;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.BbkArgumentList;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixExpression;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPrimary;
import com.larena.boxbreaker.plugin.bbk.psi.BbkTypes;
import com.larena.boxbreaker.plugin.bbk.types.BbkProcedureType;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements {@code Ctrl+P} parameter info hints for BBK calls.
 *
 * <p>When the caret is inside the parentheses of {@code f(...)}, IntelliJ
 * pops up a hint showing {@code f}'s signature with the parameter under the
 * caret highlighted in bold.
 *
 * <p>Type parameters:
 * <ul>
 *   <li>{@code ParameterOwner = BbkPostfixSuffix} — the {@code (...)} suffix of the call.
 *       Chosen over {@code BbkArgumentList} because {@code f()} (no args) does NOT
 *       produce a BbkArgumentList node (the BNF rule requires at least one expression).</li>
 *   <li>{@code ParameterType = BbkProcedureType} — the resolved signature
 *       (whole list of parameters). One per overload (BBK has none today, but
 *       a DCL-PR and a DCL-PROC sharing a name produce 2 entries).</li>
 * </ul>
 */
public class BbkParameterInfoHandler
        implements ParameterInfoHandler<BbkPostfixSuffix, BbkProcedureType> {

    // -----------------------------------------------------------------------
    // Phase 1 — locate the call when Ctrl+P is pressed
    // -----------------------------------------------------------------------

    @Override
    public @Nullable BbkPostfixSuffix findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        return findCallSuffixAround(context.getFile().findElementAt(context.getOffset()));
    }

    @Override
    public void showParameterInfo(@NotNull BbkPostfixSuffix suffix, @NotNull CreateParameterInfoContext context) {
        BbkPrimary callee = resolveCallee(suffix);
        if (callee == null) return;
        List<BbkProcedureType> sigs = collectSignatures(callee);
        if (sigs.isEmpty()) return;
        context.setItemsToShow(sigs.toArray(new Object[0]));
        context.showHint(suffix, suffix.getTextRange().getStartOffset(), this);
    }

    // -----------------------------------------------------------------------
    // Phase 2 — update which parameter is bold as the caret moves
    // -----------------------------------------------------------------------

    @Override
    public @Nullable BbkPostfixSuffix findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        return findCallSuffixAround(context.getFile().findElementAt(context.getOffset()));
    }

    @Override
    public void updateParameterInfo(@NotNull BbkPostfixSuffix suffix, @NotNull UpdateParameterInfoContext context) {
        BbkArgumentList args = suffix.getArgumentList();
        int index = args == null
            ? 0   // f()  — no args, caret sits in the empty (), highlight first param
            : ParameterInfoUtils.getCurrentParameterIndex(
                args.getNode(), context.getOffset(), BbkTypes.COMMA);
        context.setCurrentParameter(index);
    }

    // -----------------------------------------------------------------------
    // Phase 3 — render the signature with current parameter highlighted
    // -----------------------------------------------------------------------

    @Override
    public void updateUI(BbkProcedureType sig, @NotNull ParameterInfoUIContext context) {
        List<BbkProcedureType.Parameter> params = sig.getParameters();
        if (params.isEmpty()) {
            context.setupUIComponentPresentation(
                "<no parameters>",
                -1, -1,
                /*isDisabled*/ false,
                /*strikeout*/ false,
                /*isDisabledBeforeHighlight*/ false,
                context.getDefaultParameterColor()
            );
            return;
        }

        StringBuilder out = new StringBuilder();
        int currentIndex = context.getCurrentParameterIndex();
        int hlStart = -1, hlEnd = -1;
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) out.append(", ");
            int start = out.length();
            BbkProcedureType.Parameter p = params.get(i);
            out.append(p.name()).append(' ').append(p.type().getDisplayName());
            if (p.byValue()) out.append(" VALUE");
            else if (p.byConst()) out.append(" CONST");
            int end = out.length();
            if (i == currentIndex) {
                hlStart = start;
                hlEnd = end;
            }
        }

        context.setupUIComponentPresentation(
            out.toString(),
            hlStart, hlEnd,
            /*isDisabled*/ !context.isUIComponentEnabled(),
            /*strikeout*/ false,
            /*isDisabledBeforeHighlight*/ false,
            context.getDefaultParameterColor()
        );
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Walks up from {@code leaf} (typically the token at the caret) to find
     * the enclosing call-style {@link BbkPostfixSuffix} — one whose first
     * child is {@code LPAREN}. Returns null otherwise (e.g., caret is in a
     * subscript {@code [..]} or a member access {@code .x}).
     */
    private static @Nullable BbkPostfixSuffix findCallSuffixAround(@Nullable PsiElement leaf) {
        if (leaf == null) return null;
        BbkPostfixSuffix suffix = PsiTreeUtil.getParentOfType(leaf, BbkPostfixSuffix.class, /*strict*/ false);
        if (suffix == null) return null;
        PsiElement first = suffix.getFirstChild();
        if (first == null || first.getNode() == null
            || first.getNode().getElementType() != BbkTypes.LPAREN) {
            return null;  // not a call — it's `[idx]` or `.member`
        }
        return suffix;
    }

    /**
     * Given the {@code (...)} suffix of a call, returns {@code f} — the
     * primary node holding the IDENT being called.
     */
    private static @Nullable BbkPrimary resolveCallee(@NotNull BbkPostfixSuffix suffix) {
        BbkPostfixExpression postfix = PsiTreeUtil.getParentOfType(suffix, BbkPostfixExpression.class);
        if (postfix == null) return null;
        return postfix.getPrimary();
    }

    /**
     * Resolves a primary IDENT to every procedure / prototype declaration it
     * could refer to (poly-resolve) and returns the resulting
     * {@link BbkProcedureType}s. Empty if the callee does not resolve to any
     * callable.
     */
    private static @NotNull List<BbkProcedureType> collectSignatures(@NotNull BbkPrimary callee) {
        List<BbkProcedureType> out = new ArrayList<>();
        for (PsiReference ref : callee.getReferences()) {
            if (ref instanceof PsiPolyVariantReference poly) {
                for (ResolveResult rr : poly.multiResolve(false)) {
                    addIfProcType(out, rr.getElement());
                }
            } else {
                addIfProcType(out, ref.resolve());
            }
        }
        return out;
    }

    private static void addIfProcType(@NotNull List<BbkProcedureType> out, @Nullable PsiElement target) {
        if (target instanceof PsiNamedElement n) {
            BbkType t = BbkTypeInferrer.typeOf(n);
            if (t instanceof BbkProcedureType pt && !out.contains(pt)) {
                out.add(pt);
            }
        }
    }

}
