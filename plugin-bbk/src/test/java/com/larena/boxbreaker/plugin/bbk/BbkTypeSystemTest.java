package com.larena.boxbreaker.plugin.bbk;

import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.larena.boxbreaker.plugin.bbk.psi.BbkConstantDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkDataStructureDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkProcedureDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkVariableDeclaration;
import com.larena.boxbreaker.plugin.bbk.types.BbkAssignability;
import com.larena.boxbreaker.plugin.bbk.types.BbkProcedureType;
import com.larena.boxbreaker.plugin.bbk.types.BbkScalarType;
import com.larena.boxbreaker.plugin.bbk.types.BbkStructType;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;

/**
 * Unit-style tests of the type representation, assignability rules, and
 * inferrer applied to declarations. Smart Completion end-to-end is in
 * {@code BbkSmartCompletionTest}.
 */
public class BbkTypeSystemTest extends BasePlatformTestCase {

    // ----- Assignability -----

    public void testIntWidening() {
        assertTrue(BbkAssignability.areCompatible(BbkScalarType.of(BbkScalarType.Kind.INT, 5),
            BbkScalarType.of(BbkScalarType.Kind.INT, 10)));
        assertFalse(BbkAssignability.areCompatible(BbkScalarType.of(BbkScalarType.Kind.INT, 10),
            BbkScalarType.of(BbkScalarType.Kind.INT, 5)));
        assertTrue(BbkAssignability.areCompatible(BbkScalarType.of(BbkScalarType.Kind.INT, 10),
            BbkScalarType.of(BbkScalarType.Kind.INT, 10)));
    }

    public void testCharWidening() {
        assertTrue(BbkAssignability.areCompatible(BbkScalarType.of(BbkScalarType.Kind.CHAR, 20),
            BbkScalarType.of(BbkScalarType.Kind.CHAR, 50)));
        assertFalse(BbkAssignability.areCompatible(BbkScalarType.of(BbkScalarType.Kind.CHAR, 50),
            BbkScalarType.of(BbkScalarType.Kind.CHAR, 20)));
    }

    public void testCrossKindRejected() {
        assertFalse("INT not assignable to CHAR",
            BbkAssignability.areCompatible(BbkScalarType.of(BbkScalarType.Kind.INT, 10),
                BbkScalarType.of(BbkScalarType.Kind.CHAR, 10)));
        assertFalse("INT not assignable to PACKED (no implicit promotion in V1)",
            BbkAssignability.areCompatible(BbkScalarType.of(BbkScalarType.Kind.INT, 10),
                BbkScalarType.of(BbkScalarType.Kind.PACKED, 10, 0)));
        assertFalse("BOOL not assignable to INT",
            BbkAssignability.areCompatible(BbkScalarType.BOOL,
                BbkScalarType.of(BbkScalarType.Kind.INT, 10)));
    }

    public void testGenericIntLiteralAssignableToAnyNumeric() {
        assertTrue(BbkAssignability.areCompatible(BbkScalarType.INT_LITERAL,
            BbkScalarType.of(BbkScalarType.Kind.INT, 10)));
        assertTrue(BbkAssignability.areCompatible(BbkScalarType.INT_LITERAL,
            BbkScalarType.of(BbkScalarType.Kind.PACKED, 7, 2)));
        assertFalse("INT literal not assignable to CHAR",
            BbkAssignability.areCompatible(BbkScalarType.INT_LITERAL,
                BbkScalarType.of(BbkScalarType.Kind.CHAR, 10)));
    }

    public void testPackedDecimalWidening() {
        // PACKED(5:2) → PACKED(7:2): int part grows from 3 to 5, scale unchanged → OK
        assertTrue(BbkAssignability.areCompatible(
            BbkScalarType.of(BbkScalarType.Kind.PACKED, 5, 2),
            BbkScalarType.of(BbkScalarType.Kind.PACKED, 7, 2)));
        // PACKED(7:2) → PACKED(5:2): int part shrinks → reject
        assertFalse(BbkAssignability.areCompatible(
            BbkScalarType.of(BbkScalarType.Kind.PACKED, 7, 2),
            BbkScalarType.of(BbkScalarType.Kind.PACKED, 5, 2)));
        // PACKED(5:2) → PACKED(5:3): scale grows, int part shrinks → reject
        assertFalse(BbkAssignability.areCompatible(
            BbkScalarType.of(BbkScalarType.Kind.PACKED, 5, 2),
            BbkScalarType.of(BbkScalarType.Kind.PACKED, 5, 3)));
    }

    // ----- Inferrer on declarations -----

    public void testInferVariableInt() {
        myFixture.configureByText("main.bbk", "DCL-S counter INT(10);\n");
        BbkVariableDeclaration decl = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkVariableDeclaration.class);
        assertNotNull(decl);
        BbkType t = BbkTypeInferrer.typeOf(decl);
        assertEquals(BbkScalarType.of(BbkScalarType.Kind.INT, 10), t);
    }

    public void testInferVariableChar() {
        myFixture.configureByText("main.bbk", "DCL-S name CHAR(50);\n");
        BbkVariableDeclaration decl = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkVariableDeclaration.class);
        BbkType t = BbkTypeInferrer.typeOf(decl);
        assertEquals(BbkScalarType.of(BbkScalarType.Kind.CHAR, 50), t);
    }

    public void testInferVariablePackedWithDecimals() {
        myFixture.configureByText("main.bbk", "DCL-S amount PACKED(7:2);\n");
        BbkVariableDeclaration decl = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkVariableDeclaration.class);
        BbkType t = BbkTypeInferrer.typeOf(decl);
        assertEquals(BbkScalarType.of(BbkScalarType.Kind.PACKED, 7, 2), t);
    }

    public void testInferConstantInt() {
        myFixture.configureByText("main.bbk", "DCL-C MAX_RETRIES 5;\n");
        BbkConstantDeclaration decl = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkConstantDeclaration.class);
        BbkType t = BbkTypeInferrer.typeOf(decl);
        // Constant value 5 → INT_LITERAL (generic)
        assertTrue("Should be a scalar", t instanceof BbkScalarType);
        assertEquals(BbkScalarType.Kind.INT, ((BbkScalarType) t).getKind());
    }

    public void testInferDataStructure() {
        myFixture.configureByText("main.bbk",
            "DCL-DS customer QUALIFIED;\n" +
            "  DCL-SUBF id INT(10);\n" +
            "END-DS;\n");
        BbkDataStructureDeclaration ds = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkDataStructureDeclaration.class);
        BbkType t = BbkTypeInferrer.typeOf(ds);
        assertTrue("Should be a struct", t instanceof BbkStructType);
    }

    public void testInferProcedureReturnType() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) -> INT(20) {\n" +
            "  return n;\n" +
            "}\n");
        BbkProcedureDeclaration proc = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkProcedureDeclaration.class);
        BbkType t = BbkTypeInferrer.typeOf(proc);
        assertTrue(t instanceof BbkProcedureType);
        BbkProcedureType pt = (BbkProcedureType) t;
        assertEquals(BbkScalarType.of(BbkScalarType.Kind.INT, 20), pt.getReturnType());
        assertEquals(1, pt.getParameters().size());
        assertEquals(BbkScalarType.of(BbkScalarType.Kind.INT, 10), pt.getParameters().get(0).type());
    }

    public void testInferLikedsResolvesToStruct() {
        myFixture.configureByText("main.bbk",
            "DCL-DS customer QUALIFIED;\n" +
            "  DCL-SUBF id INT(10);\n" +
            "END-DS;\n" +
            "DCL-S currentOrder LIKEDS(customer);\n");
        BbkVariableDeclaration order = null;
        for (BbkVariableDeclaration v : PsiTreeUtil.findChildrenOfType(myFixture.getFile(), BbkVariableDeclaration.class)) {
            if ("currentOrder".equals(v.getName())) order = v;
        }
        assertNotNull(order);
        BbkType t = BbkTypeInferrer.typeOf(order);
        assertTrue("LIKEDS should resolve to a struct", t instanceof BbkStructType);
    }
}
