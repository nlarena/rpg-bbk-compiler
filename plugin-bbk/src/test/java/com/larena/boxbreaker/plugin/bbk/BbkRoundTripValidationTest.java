package com.larena.boxbreaker.plugin.bbk;

import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.larena.boxbreaker.rpg.RpgToBbk;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Closes the loop frontend &harr; plugin: translates RPG with {@code rpg-frontend},
 * then confirms the generated BBK <b>parses without errors</b> using this
 * plugin's BBK parser. A failure means the emitter produced text the BBK
 * grammar does not accept — a real discrepancy to fix in the emitter.
 */
public class BbkRoundTripValidationTest extends BasePlatformTestCase {

    /** Translate RPG, parse the BBK, and return any parse-error messages (with the BBK for context). */
    private void assertTranslatesToValidBbk(String rpg) {
        String bbk = RpgToBbk.translate(rpg);
        PsiFile file = myFixture.configureByText("gen.bbk", bbk);
        List<PsiErrorElement> errors =
            List.copyOf(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class));
        if (!errors.isEmpty()) {
            String detail = errors.stream()
                .map(e -> "  @" + e.getTextOffset() + " near '" + snippet(bbk, e.getTextOffset())
                    + "': " + e.getErrorDescription())
                .collect(Collectors.joining("\n"));
            fail("Generated BBK has " + errors.size() + " parse error(s):\n" + detail
                + "\n--- generated BBK ---\n" + bbk);
        }
    }

    private static String snippet(String text, int offset) {
        int end = Math.min(text.length(), offset + 20);
        return text.substring(Math.max(0, offset), end).replace("\n", "\\n");
    }

    // -----------------------------------------------------------------------

    public void testDeclarationsParse() {
        assertTranslatesToValidBbk(
            "dcl-s counter int(10) inz(0);\n" +
            "dcl-s name char(50);\n" +
            "dcl-s amount packed(11:2);\n" +
            "dcl-s ok ind;\n" +
            "dcl-c MAX 100;\n");
    }

    public void testAssignmentsAndExpressionsParse() {
        assertTranslatesToValidBbk(
            "x = a + b * c;\n" +
            "y = (a + b) * c;\n" +
            "flag = a = 1 and b <> 2;\n" +
            "z = %subst(name : 1 : 3);\n" +
            "total = computeTax(amount);\n");
    }

    public void testControlFlowParses() {
        assertTranslatesToValidBbk(
            "if counter > 0;\n" +
            "  counter = counter - 1;\n" +
            "elseif counter = 0;\n" +
            "  counter = 10;\n" +
            "else;\n" +
            "  counter = 0;\n" +
            "endif;\n");
    }

    public void testLoopsParse() {
        assertTranslatesToValidBbk(
            "for i = 1 to 10;\n  total = total + i;\nendfor;\n" +
            "dow x < 10;\n  x = x + 1;\nenddo;\n" +
            "dou x >= 10;\n  x = x + 1;\nenddo;\n");
    }

    public void testSelectParses() {
        assertTranslatesToValidBbk(
            "select;\n" +
            "when code = 1;\n  msg = 'one';\n" +
            "when code = 2;\n  msg = 'two';\n" +
            "other;\n  msg = 'other';\n" +
            "endsl;\n");
    }

    public void testDataStructureParses() {
        assertTranslatesToValidBbk(
            "dcl-ds customer qualified;\n" +
            "  id int(10);\n" +
            "  name char(50);\n" +
            "  active ind;\n" +
            "end-ds;\n");
    }

    public void testProcedureParses() {
        assertTranslatesToValidBbk(
            "dcl-proc computeTax export;\n" +
            "  dcl-pi computeTax packed(11:2);\n" +
            "    amount packed(11:2) value;\n" +
            "  end-pi;\n" +
            "  return amount * 21;\n" +
            "end-proc;\n");
    }

    public void testFullProgramParses() {
        assertTranslatesToValidBbk(
            "dcl-s counter int(10) inz(0);\n" +
            "dcl-s name char(50);\n" +
            "dcl-ds customer qualified;\n" +
            "  id int(10);\n" +
            "  active ind;\n" +
            "end-ds;\n" +
            "counter = counter + 1;\n" +
            "if counter > 0 and customer.active = *on;\n" +
            "  name = 'active';\n" +
            "  for i = 1 to counter;\n" +
            "    counter = counter - 1;\n" +
            "  endfor;\n" +
            "endif;\n");
    }
}
