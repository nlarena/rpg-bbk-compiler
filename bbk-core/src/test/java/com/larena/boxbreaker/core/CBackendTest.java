package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.backend.c.CCompiler;
import com.larena.boxbreaker.core.backend.c.CRunner;
import com.larena.boxbreaker.core.parser.BbkParser;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the BBK &rarr; C back-end by asserting the generated C source. (A live
 * gcc compile+run is exercised by {@link #runsViaGccWhenAvailable}, which is a
 * no-op when no C compiler is installed.)
 */
public class CBackendTest {

    private String c(String bbk) {
        return CCompiler.compile(BbkParser.parse(bbk));
    }

    @Test
    public void emitsAWellFormedProgram() {
        String out = c("print(42);");
        assertTrue(out.contains("#include <stdio.h>"));
        assertTrue(out.contains("int main(void) {"));
        assertTrue(out.contains("printf(\"%lld\\n\", 42LL);"));
        assertTrue(out.trim().endsWith("}"));
        assertTrue(out.contains("return 0;"));
    }

    @Test
    public void declarationAndArithmetic() {
        String out = c(
            "DCL-S x INT(10) INZ(3);\n" +
            "DCL-S y INT(10);\n" +
            "y = x + 4 * 2;\n" +
            "print(y);");
        // module-level vars become file-scope globals (so procedures can see them), inited in main
        assertTrue(out.contains("static long long x;"));
        assertTrue(out.contains("static long long y;"));
        assertTrue(out.contains("x = 3LL;"));
        assertTrue(out.contains("y = x + 4LL * 2LL;"));      // minimal parens, * binds tighter
        assertTrue(out.contains("printf(\"%lld\\n\", y);"));
    }

    @Test
    public void minimalParensRespectPrecedence() {
        assertTrue(c("print((2 + 3) * 4);").contains("(2LL + 3LL) * 4LL"));
        assertTrue(c("print(2 + 3 * 4);").contains("2LL + 3LL * 4LL"));
        assertTrue(c("DCL-S a INT(10);\nDCL-S b INT(10);\nDCL-S c INT(10);\nprint(a - (b + c));")
            .contains("a - (b + c)"));
    }

    @Test
    public void stringAndBoolPrint() {
        assertTrue(c("print(\"hi\");").contains("printf(\"%s\\n\", \"hi\");"));
        assertTrue(c("print(1 < 2);").contains("printf(\"%s\\n\", (1LL < 2LL) ? \"true\" : \"false\");"));
    }

    @Test
    public void controlFlowMapsToC() {
        assertTrue(c("DCL-S a INT(10) INZ(1);\nif (a > 0) { print(a); }").contains("if (a > 0LL) {"));
        assertTrue(c("DCL-S i INT(10) INZ(0);\nwhile (i < 3) { i = i + 1; }").contains("while (i < 3LL) {"));
        assertTrue(c("DCL-S i INT(10) INZ(0);\ndo { i = i + 1; } while (i < 3);").contains("} while (i < 3LL);"));
    }

    @Test
    public void forLoopWithInlineDeclaredVar() {
        String out = c(
            "DCL-S sum INT(10) INZ(0);\n" +
            "for (i = 1; i <= 5; i += 1) {\n" +
            "  sum = sum + i;\n" +
            "}\n" +
            "print(sum);");
        // i is auto-declared in the for-init (C scopes it to the loop)
        assertTrue(out.contains("for (long long i = 1LL; i <= 5LL; i += 1LL) {"));
        assertTrue(out.contains("sum = sum + i;"));
    }

    @Test
    public void ternaryAndCompound() {
        assertTrue(c("DCL-S x INT(10) INZ(4);\nx += 6;").contains("x += 6LL;"));
        assertTrue(c("print(1 < 2 ? 10 : 20);").contains("(1LL < 2LL ? 10LL : 20LL)"));
    }

    @Test
    public void elseIfChain() {
        String out = c(
            "DCL-S n INT(10) INZ(2);\n" +
            "if (n == 1) { print(\"one\"); }\n" +
            "else if (n == 2) { print(\"two\"); }\n" +
            "else { print(\"many\"); }");
        assertTrue(out.contains("} else if (n == 2LL) {"));
        assertTrue(out.contains("} else {"));
    }

    /** When a C compiler is present, the generated C must build and run correctly. */
    @Test
    public void runsViaGccWhenAvailable() throws Exception {
        if (!CRunner.hasCompiler()) return;   // no gcc here: skip (still tested via text above)
        String bbk =
            "DCL-S sum INT(10) INZ(0);\n" +
            "for (i = 1; i <= 5; i += 1) { sum = sum + i; }\n" +
            "print(sum);";
        assertEquals("15", CRunner.compileAndRun(bbk).strip());
    }
}
