package com.larena.boxbreaker.plugin.bbk;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.larena.boxbreaker.plugin.bbk.inspection.BbkAssignmentTypeMismatchInspection;
import com.larena.boxbreaker.plugin.bbk.inspection.BbkCallArgumentCountMismatchInspection;
import com.larena.boxbreaker.plugin.bbk.inspection.BbkCallArgumentTypeMismatchInspection;
import com.larena.boxbreaker.plugin.bbk.inspection.BbkConditionNotBoolInspection;
import com.larena.boxbreaker.plugin.bbk.inspection.BbkInzValueTypeMismatchInspection;
import com.larena.boxbreaker.plugin.bbk.inspection.BbkReturnTypeMismatchInspection;
import com.larena.boxbreaker.plugin.bbk.inspection.BbkUnresolvedReferenceInspection;

/**
 * Integration tests for the 7 pillar type-aware inspections. Uses
 * IntelliJ's {@code <warning>...</warning>} / {@code <error>...</error>}
 * markup convention: any squiggly raised by the enabled inspections must
 * match a marker; any marker must be raised.
 */
public class BbkInspectionsTest extends BasePlatformTestCase {

    // ============================================================
    // BbkAssignmentTypeMismatchInspection
    // ============================================================

    public void testAssignment_IntCannotAcceptChar() {
        myFixture.enableInspections(BbkAssignmentTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S counter INT(10);\n" +
            "  DCL-S name CHAR(50);\n" +
            "  counter = <warning descr=\"Cannot assign CHAR(50) to INT(10)\">name</warning>;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testAssignment_SameTypePasses() {
        myFixture.enableInspections(BbkAssignmentTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S a INT(10);\n" +
            "  DCL-S b INT(10);\n" +
            "  a = b;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testAssignment_IntWideningOk() {
        myFixture.enableInspections(BbkAssignmentTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S big INT(20);\n" +
            "  DCL-S small INT(5);\n" +
            "  big = small;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testAssignment_LiteralIntFitsAnyInt() {
        myFixture.enableInspections(BbkAssignmentTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S counter INT(10);\n" +
            "  counter = 42;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    // ============================================================
    // BbkReturnTypeMismatchInspection
    // ============================================================

    public void testReturn_CharNotAssignableToInt() {
        myFixture.enableInspections(BbkReturnTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper -> INT(10) {\n" +
            "  DCL-S s CHAR(20);\n" +
            "  return <warning descr=\"Cannot return CHAR(20) from procedure returning INT(10)\">s</warning>;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testReturn_VoidProcReturningValue() {
        myFixture.enableInspections(BbkReturnTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC noReturn {\n" +
            "  return <warning descr=\"Procedure returns VOID; remove the return value\">42</warning>;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testReturn_MatchingTypePasses() {
        myFixture.enableInspections(BbkReturnTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper -> INT(10) {\n" +
            "  return 0;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    // ============================================================
    // BbkCallArgumentTypeMismatchInspection
    // ============================================================

    public void testCallArg_CharPassedToIntParam() {
        myFixture.enableInspections(BbkCallArgumentTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) { return; }\n" +
            "DCL-PROC main {\n" +
            "  DCL-S s CHAR(20);\n" +
            "  helper(<warning descr=\"Argument 1 (n): expected INT(10), got CHAR(20)\">s</warning>);\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testCallArg_MatchingPasses() {
        myFixture.enableInspections(BbkCallArgumentTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) { return; }\n" +
            "DCL-PROC main {\n" +
            "  helper(42);\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    // ============================================================
    // BbkCallArgumentCountMismatchInspection
    // ============================================================

    public void testCallCount_TooFewArgs() {
        myFixture.enableInspections(BbkCallArgumentCountMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) { return; }\n" +
            "DCL-PROC main {\n" +
            "  <warning descr=\"Expected 1 argument, got 0\">helper()</warning>;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testCallCount_TooManyArgs() {
        myFixture.enableInspections(BbkCallArgumentCountMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) { return; }\n" +
            "DCL-PROC main {\n" +
            "  helper(<warning descr=\"Expected 1 argument, got 2\">1, 2</warning>);\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testCallCount_MatchingPasses() {
        myFixture.enableInspections(BbkCallArgumentCountMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) { return; }\n" +
            "DCL-PROC main {\n" +
            "  helper(1);\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    // ============================================================
    // BbkConditionNotBoolInspection
    // ============================================================

    public void testCondition_IntInIf() {
        myFixture.enableInspections(BbkConditionNotBoolInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S counter INT(10);\n" +
            "  if (<warning descr=\"Condition must be BOOL, got INT(10)\">counter</warning>) {}\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testCondition_BoolPasses() {
        myFixture.enableInspections(BbkConditionNotBoolInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S flag BOOL;\n" +
            "  if (flag) {}\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testCondition_ComparisonPasses() {
        myFixture.enableInspections(BbkConditionNotBoolInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S counter INT(10);\n" +
            "  if (counter > 0) {}\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    // ============================================================
    // BbkInzValueTypeMismatchInspection
    // ============================================================

    public void testInz_StringOnIntDecl() {
        myFixture.enableInspections(BbkInzValueTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-S counter INT(10) INZ(<warning descr=\"INZ value of type CHAR is not assignable to declared type INT(10)\">'hola'</warning>);\n");
        myFixture.checkHighlighting();
    }

    public void testInz_IntLiteralOnIntDeclPasses() {
        myFixture.enableInspections(BbkInzValueTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-S counter INT(10) INZ(0);\n");
        myFixture.checkHighlighting();
    }

    // ============================================================
    // BbkUnresolvedReferenceInspection
    // ============================================================

    public void testUnresolved_FlagsUnknownIdent() {
        myFixture.enableInspections(BbkUnresolvedReferenceInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S x INT(10);\n" +
            "  x = <error descr=\"Unresolved reference: 'notDeclared'\">notDeclared</error>;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }

    public void testUnresolved_DeclaredIdentPasses() {
        myFixture.enableInspections(BbkUnresolvedReferenceInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S x INT(10);\n" +
            "  DCL-S y INT(10);\n" +
            "  x = y;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }
}
