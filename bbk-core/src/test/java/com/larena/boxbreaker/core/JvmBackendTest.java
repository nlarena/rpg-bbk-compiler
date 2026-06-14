package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.backend.jvm.BbkRunner;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * End-to-end: BBK source &rarr; JVM bytecode &rarr; runs on the JVM &rarr; output.
 */
public class JvmBackendTest {

    private String run(String bbk) {
        return BbkRunner.compileAndRun(bbk).strip();
    }

    @Test
    public void printsAnInteger() {
        assertEquals("42", run("print(42);"));
    }

    @Test
    public void printsAString() {
        assertEquals("hello", run("print(\"hello\");"));
    }

    @Test
    public void arithmetic() {
        assertEquals("14", run("print(2 + 3 * 4);"));
        assertEquals("20", run("print((2 + 3) * 4);"));
        assertEquals("2", run("print(17 % 5);"));
        assertEquals("-6", run("print(4 - 2 * 5);"));
        assertEquals("12", run("print(3 << 2);"));      // bitwise shift
    }

    @Test
    public void variablesAndAssignment() {
        assertEquals("7", run(
            "DCL-S x INT(10) INZ(3);\n" +
            "DCL-S y INT(10);\n" +
            "y = x + 4;\n" +
            "print(y);"));
    }

    @Test
    public void compoundAssignment() {
        assertEquals("10", run(
            "DCL-S x INT(10) INZ(4);\n" +
            "x += 6;\n" +
            "print(x);"));
    }

    @Test
    public void ifElse() {
        assertEquals("big", run(
            "DCL-S n INT(10) INZ(7);\n" +
            "if (n > 5) { print(\"big\"); } else { print(\"small\"); }"));
    }

    @Test
    public void whileLoop() {
        assertEquals("6", run(
            "DCL-S sum INT(10) INZ(0);\n" +
            "DCL-S i INT(10) INZ(1);\n" +
            "while (i <= 3) { sum = sum + i; i = i + 1; }\n" +
            "print(sum);"));
    }

    @Test
    public void forLoopSumsToFifteen() {
        // the classic: sum 1..5 with a C-style for; i auto-declares as long
        assertEquals("15", run(
            "DCL-S sum INT(10) INZ(0);\n" +
            "for (i = 1; i <= 5; i += 1) {\n" +
            "  sum = sum + i;\n" +
            "}\n" +
            "print(sum);"));
    }

    @Test
    public void breakAndContinue() {
        // sum of even numbers 1..9, stop at 8
        assertEquals("20", run(
            "DCL-S sum INT(10) INZ(0);\n" +
            "for (i = 1; i <= 100; i += 1) {\n" +
            "  if (i > 8) { break; }\n" +
            "  if (i % 2 == 1) { continue; }\n" +
            "  sum = sum + i;\n" +
            "}\n" +
            "print(sum);"));   // 2+4+6+8 = 20
    }

    @Test
    public void booleanAndTernary() {
        assertEquals("yes", run(
            "DCL-S a INT(10) INZ(3);\n" +
            "DCL-S b INT(10) INZ(4);\n" +
            "print(a < b && b < 10 ? \"yes\" : \"no\");"));
    }
}
