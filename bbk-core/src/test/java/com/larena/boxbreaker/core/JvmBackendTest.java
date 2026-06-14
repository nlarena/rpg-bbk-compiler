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

    @Test
    public void floatingPointArithmetic() {
        assertEquals("3.5", run("print(1.5 + 2.0);"));
        assertEquals("3.5", run("print(7.0 / 2.0);"));
        // mixed int/double promotes to double
        assertEquals("5.5", run("print(5 + 0.5);"));
        assertEquals("2.5", run(
            "DCL-S x FLOAT(8) INZ(5.0);\n" +
            "print(x / 2);"));
    }

    @Test
    public void powerOperator() {
        assertEquals("1024.0", run("print(2 ** 10);"));
        assertEquals("9.0", run("print(3.0 ** 2.0);"));
    }

    @Test
    public void stringConcatenation() {
        assertEquals("count: 5", run("print(\"count: \" + 5);"));
        assertEquals("ab", run("print(\"a\" + \"b\");"));
        assertEquals("n=3.5", run("print(\"n=\" + 3.5);"));
        assertEquals("flag: true", run("print(\"flag: \" + (1 < 2));"));
    }

    @Test
    public void selectStatement() {
        String prog =
            "DCL-S n INT(10) INZ(2);\n" +
            "select {\n" +
            "  when (n == 1) { print(\"one\"); }\n" +
            "  when (n == 2) { print(\"two\"); }\n" +
            "  other { print(\"many\"); }\n" +
            "}";
        assertEquals("two", run(prog));
    }

    @Test
    public void selectFallsThroughToOther() {
        String prog =
            "DCL-S n INT(10) INZ(9);\n" +
            "select {\n" +
            "  when (n == 1) { print(\"one\"); }\n" +
            "  other { print(\"many\"); }\n" +
            "}";
        assertEquals("many", run(prog));
    }

    @Test
    public void compoundBitwiseAssignment() {
        assertEquals("8", run(
            "DCL-S x INT(10) INZ(2);\n" +
            "x <<= 2;\n" +     // 2 << 2 = 8
            "print(x);"));
        assertEquals("7", run(
            "DCL-S y INT(10) INZ(5);\n" +
            "y |= 2;\n" +      // 0b101 | 0b010 = 0b111 = 7
            "print(y);"));
    }
}
