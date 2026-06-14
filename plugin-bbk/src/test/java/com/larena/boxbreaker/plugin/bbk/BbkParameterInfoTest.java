package com.larena.boxbreaker.plugin.bbk;

import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.larena.boxbreaker.plugin.bbk.parameterinfo.BbkParameterInfoHandler;
import com.larena.boxbreaker.plugin.bbk.psi.BbkArgumentList;
import com.larena.boxbreaker.plugin.bbk.psi.BbkTypes;
import com.larena.boxbreaker.plugin.bbk.types.BbkProcedureType;

import java.awt.Color;

/**
 * Exercises {@link BbkParameterInfoHandler} directly, bypassing the IntelliJ
 * popup machinery — we verify (a) it correctly locates the {@link BbkArgumentList}
 * at the caret, (b) it resolves the callee to a {@link BbkProcedureType}, and
 * (c) {@code updateUI} renders the signature with the right substring as the
 * current parameter highlight.
 *
 * <p>This is more direct than {@code myFixture.testParameterInfo(...)} which
 * only checks the rendered text — we want to assert the highlight range too.
 */
public class BbkParameterInfoTest extends BasePlatformTestCase {

    public void testFindCallSuffixAtCaretWithArgs() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) { return; }\n" +
            "DCL-PROC main {\n" +
            "  helper(<caret>42);\n" +
            "}\n");

        com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix suffix = findCallSuffixAtCaret();
        assertNotNull("Should find call suffix around caret", suffix);
    }

    public void testFindCallSuffixAtCaretWithoutArgs() {
        // f(<caret>) — no BbkArgumentList exists, only LPAREN/RPAREN inside BbkPostfixSuffix
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) { return; }\n" +
            "DCL-PROC main {\n" +
            "  helper(<caret>);\n" +
            "}\n");

        com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix suffix = findCallSuffixAtCaret();
        assertNotNull("Should find empty-args call suffix around caret", suffix);
        assertNull("No BbkArgumentList in an empty-args call", suffix.getArgumentList());
    }

    public void testCurrentParameterIndexAtFirstArg() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(a INT(10), b CHAR(20)) { return; }\n" +
            "DCL-PROC main {\n" +
            "  helper(<caret>1, 'x');\n" +
            "}\n");

        BbkArgumentList args = findArgsAtCaret();
        assertNotNull(args);
        int idx = com.intellij.lang.parameterInfo.ParameterInfoUtils
            .getCurrentParameterIndex(args.getNode(), myFixture.getCaretOffset(), BbkTypes.COMMA);
        assertEquals("Caret on slot 0 should map to index 0", 0, idx);
    }

    public void testCurrentParameterIndexAtSecondArg() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(a INT(10), b CHAR(20)) { return; }\n" +
            "DCL-PROC main {\n" +
            "  helper(1, <caret>'x');\n" +
            "}\n");

        BbkArgumentList args = findArgsAtCaret();
        assertNotNull(args);
        int idx = com.intellij.lang.parameterInfo.ParameterInfoUtils
            .getCurrentParameterIndex(args.getNode(), myFixture.getCaretOffset(), BbkTypes.COMMA);
        assertEquals("Caret on slot 1 should map to index 1", 1, idx);
    }

    public void testRenderedSignatureSingleParam() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) { return; }\n" +
            "DCL-PROC main {\n" +
            "  helper(<caret>);\n" +
            "}\n");

        CapturingUIContext ui = renderUIForCurrentCallee(0);
        assertEquals("n INT(10) VALUE", ui.text);
        assertEquals(0, ui.hlStart);
        assertEquals("n INT(10) VALUE".length(), ui.hlEnd);
    }

    public void testRenderedSignatureTwoParamsHighlightSecond() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(a INT(10) VALUE, b CHAR(20) CONST) { return; }\n" +
            "DCL-PROC main {\n" +
            "  helper(1, <caret>'x');\n" +
            "}\n");

        CapturingUIContext ui = renderUIForCurrentCallee(1);
        assertEquals("a INT(10) VALUE, b CHAR(20) CONST", ui.text);
        int expectedStart = "a INT(10) VALUE, ".length();
        assertEquals("highlight starts at 'b'", expectedStart, ui.hlStart);
        assertEquals("highlight covers full second param",
            expectedStart + "b CHAR(20) CONST".length(), ui.hlEnd);
    }

    public void testNoParametersRendersPlaceholder() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC noargs { return; }\n" +
            "DCL-PROC main {\n" +
            "  noargs(<caret>);\n" +
            "}\n");

        CapturingUIContext ui = renderUIForCurrentCallee(0);
        assertEquals("<no parameters>", ui.text);
    }

    // ----- helpers -----

    private BbkArgumentList findArgsAtCaret() {
        PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(leaf);
        return PsiTreeUtil.getParentOfType(leaf, BbkArgumentList.class, false);
    }

    private com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix findCallSuffixAtCaret() {
        PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(leaf);
        com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix suffix = PsiTreeUtil.getParentOfType(
            leaf, com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix.class, false);
        if (suffix == null) return null;
        PsiElement first = suffix.getFirstChild();
        if (first == null || first.getNode() == null
            || first.getNode().getElementType() != BbkTypes.LPAREN) return null;
        return suffix;
    }

    /**
     * Drives the handler end-to-end without the popup framework: resolves the
     * callee from the caret, picks the first signature, and renders it via
     * {@code updateUI}. Returns the captured rendered text + highlight range.
     */
    private CapturingUIContext renderUIForCurrentCallee(int currentParamIdx) {
        BbkParameterInfoHandler handler = new BbkParameterInfoHandler();
        com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix suffix = findCallSuffixAtCaret();
        assertNotNull("Caret must be inside a call's parens", suffix);

        java.util.List<BbkProcedureType> sigs = collectSignaturesFor(suffix);
        assertFalse("Expected at least one resolved signature", sigs.isEmpty());

        CapturingUIContext ui = new CapturingUIContext(currentParamIdx);
        handler.updateUI(sigs.get(0), ui);
        return ui;
    }

    private java.util.List<BbkProcedureType> collectSignaturesFor(
            com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixSuffix suffix) {
        com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixExpression postfix =
            PsiTreeUtil.getParentOfType(suffix, com.larena.boxbreaker.plugin.bbk.psi.BbkPostfixExpression.class);
        com.larena.boxbreaker.plugin.bbk.psi.BbkPrimary primary = postfix.getPrimary();
        java.util.List<BbkProcedureType> out = new java.util.ArrayList<>();
        for (com.intellij.psi.PsiReference ref : primary.getReferences()) {
            if (ref instanceof com.intellij.psi.PsiPolyVariantReference poly) {
                for (com.intellij.psi.ResolveResult rr : poly.multiResolve(false)) {
                    PsiElement t = rr.getElement();
                    if (t instanceof com.intellij.psi.PsiNamedElement n) {
                        com.larena.boxbreaker.plugin.bbk.types.BbkType type =
                            com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer.typeOf(n);
                        if (type instanceof BbkProcedureType pt) out.add(pt);
                    }
                }
            } else {
                PsiElement t = ref.resolve();
                if (t instanceof com.intellij.psi.PsiNamedElement n) {
                    com.larena.boxbreaker.plugin.bbk.types.BbkType type =
                        com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer.typeOf(n);
                    if (type instanceof BbkProcedureType pt) out.add(pt);
                }
            }
        }
        return out;
    }

    /** Captures what {@code updateUI} produced — text + highlight range. */
    private static class CapturingUIContext implements ParameterInfoUIContext {
        final int currentIdx;
        String text;
        int hlStart, hlEnd;
        boolean disabled, strikeout;

        CapturingUIContext(int currentIdx) { this.currentIdx = currentIdx; }

        @Override public String setupUIComponentPresentation(String text, int highlightStartOffset,
                                                              int highlightEndOffset, boolean isDisabled,
                                                              boolean strikeout, boolean isDisabledBeforeHighlight,
                                                              Color background) {
            this.text = text;
            this.hlStart = highlightStartOffset;
            this.hlEnd = highlightEndOffset;
            this.disabled = isDisabled;
            this.strikeout = strikeout;
            return text;
        }

        @Override public boolean isUIComponentEnabled() { return true; }
        @Override public void setUIComponentEnabled(boolean enabled) {}
        @Override public int getCurrentParameterIndex() { return currentIdx; }
        @Override public PsiElement getParameterOwner() { return null; }
        @Override public boolean isSingleOverload() { return true; }
        @Override public boolean isSingleParameterInfo() { return false; }
        @Override public Color getDefaultParameterColor() { return null; }
        @Override public void setupRawUIComponentPresentation(String htmlText) {
            this.text = htmlText;
        }
    }
}
