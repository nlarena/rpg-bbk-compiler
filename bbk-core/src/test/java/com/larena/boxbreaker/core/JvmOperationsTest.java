package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.backend.jvm.BbkRunner;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * End-to-end coverage of the non-OS subsystems: procedures, constants, exact
 * decimals, arrays, data structures, subroutines, monitor, special values and
 * the pure builtins. Each program is compiled to bytecode and run on the JVM.
 */
public class JvmOperationsTest {

    private String run(String bbk) { return BbkRunner.compileAndRun(bbk).replace("\r\n", "\n").strip(); }

    // ---- procedures -------------------------------------------------------

    @Test
    public void procedureCalledFromMainline() {
        assertEquals("12", run(
            "DCL-PROC add(a INT(10) VALUE, b INT(10) VALUE) -> INT(10) { return a + b; }\n" +
            "print(char(add(5, 7)));"));
    }

    @Test
    public void ctlOptMainIsTheEntryPoint() {
        assertEquals("hi", run(
            "CTL-OPT MAIN(go);\n" +
            "DCL-PROC go { print(\"hi\"); }"));
    }

    @Test
    public void recursiveProcedure() {
        assertEquals("120", run(
            "DCL-PROC fact(n INT(10) VALUE) -> INT(10) {\n" +
            "  if (n <= 1) { return 1; }\n" +
            "  return n * fact(n - 1);\n" +
            "}\n" +
            "print(fact(5));"));
    }

    @Test
    public void procedureWithStringParam() {
        assertEquals("Hello, World!", run(
            "DCL-PROC greet(name VARCHAR(50) CONST) { print(\"Hello, \" + trim(name) + \"!\"); }\n" +
            "greet(\"World\");"));
    }

    // ---- constants --------------------------------------------------------

    @Test
    public void constants() {
        assertEquals("Acme\n10", run(
            "DCL-C MAX 5;\n" +
            "DCL-C NAME \"Acme\";\n" +
            "print(NAME);\n" +
            "print(MAX * 2);"));
    }

    // ---- exact decimals ---------------------------------------------------

    @Test
    public void decimalKeepsDeclaredScale() {
        // 199.95 * 3 = 599.85, stored at scale 2 (not 599.8500)
        assertEquals("599.85", run(
            "DCL-S price PACKED(9:2) INZ(199.95d);\n" +
            "DCL-S qty INT(10) INZ(3);\n" +
            "DCL-S subtotal PACKED(11:2);\n" +
            "subtotal = price * qty;\n" +
            "print(subtotal);"));
    }

    @Test
    public void decimalDivisionWithHalfUp() {
        assertEquals("3.33", run(
            "DCL-S x PACKED(11:2);\n" +
            "x = 10.00d / 3.00d @halfup;\n" +
            "print(x);"));
    }

    @Test
    public void decimalDivisionWithTrunc() {
        assertEquals("3.33", run(
            "DCL-S x PACKED(11:2);\n" +
            "x = 10.00d / 3.00d @trunc;\n" +
            "print(x);"));
    }

    // ---- arrays -----------------------------------------------------------

    @Test
    public void arrayReadWrite() {
        assertEquals("60", run(
            "DCL-S a INT(10) DIM(3);\n" +
            "a[0] = 10; a[1] = 20; a[2] = 30;\n" +
            "print(a[0] + a[1] + a[2]);"));
    }

    @Test
    public void arrayCompoundAssignment() {
        assertEquals("8", run(
            "DCL-S a INT(10) DIM(2);\n" +
            "a[0] = 5;\n" +
            "a[0] += 3;\n" +
            "print(a[0]);"));
    }

    @Test
    public void arraySummedInLoop() {
        assertEquals("10", run(
            "DCL-S a INT(10) DIM(5);\n" +
            "DCL-S total INT(10) INZ(0);\n" +
            "for (i = 0; i < 5; i += 1) { a[i] = i; }\n" +
            "for (j = 0; j < 5; j += 1) { total += a[j]; }\n" +   // 0+1+2+3+4
            "print(total);"));
    }

    // ---- data structures --------------------------------------------------

    @Test
    public void qualifiedDataStructure() {
        assertEquals("Nico 28", run(
            "DCL-DS person QUALIFIED { name VARCHAR(50); age INT(10); }\n" +
            "person.name = \"Nico\";\n" +
            "person.age = 28;\n" +
            "print(person.name + \" \" + char(person.age));"));
    }

    @Test
    public void arrayOfDataStructures() {
        assertEquals("1001-Alice\n1002-Bob", run(
            "DCL-DS emp QUALIFIED DIM(2) { id INT(10); name VARCHAR(50); }\n" +
            "emp[0].id = 1001; emp[0].name = \"Alice\";\n" +
            "emp[1].id = 1002; emp[1].name = \"Bob\";\n" +
            "print(char(emp[0].id) + \"-\" + emp[0].name);\n" +
            "print(char(emp[1].id) + \"-\" + emp[1].name);"));
    }

    @Test
    public void templateAndLikeds() {
        assertEquals("Main St, Buenos Aires", run(
            "DCL-DS addr TEMPLATE { street VARCHAR(50); city VARCHAR(50); }\n" +
            "DCL-DS home LIKEDS(addr);\n" +
            "home.street = \"Main St\";\n" +
            "home.city = \"Buenos Aires\";\n" +
            "print(home.street + \", \" + home.city);"));
    }

    // ---- subroutines ------------------------------------------------------

    @Test
    public void subroutineViaExsr() {
        assertEquals("42", run(
            "DCL-S x INT(10) INZ(0);\n" +
            "EXSR setup;\n" +
            "print(x);\n" +
            "BEGSR setup; x = 42; ENDSR;"));
    }

    @Test
    public void leavesrExitsEarly() {
        assertEquals("1", run(
            "DCL-S x INT(10) INZ(0);\n" +
            "EXSR work;\n" +
            "print(x);\n" +
            "BEGSR work; x = 1; if (x == 1) { LEAVESR; } x = 99; ENDSR;"));
    }

    // ---- monitor ----------------------------------------------------------

    @Test
    public void monitorCatchesRuntimeError() {
        assertEquals("-1", run(
            "DCL-S r INT(10) INZ(0);\n" +
            "monitor { r = 10 / 0; } on-error { r = -1; }\n" +
            "print(r);"));
    }

    // ---- special values + string equality ---------------------------------

    @Test
    public void blankSpecialValue() {
        assertEquals("[]", run(
            "DCL-S s VARCHAR(10) INZ(*blank);\n" +
            "print(\"[\" + s + \"]\");"));
    }

    @Test
    public void stringEquality() {
        assertEquals("match", run(
            "DCL-S s VARCHAR(10) INZ(\"yes\");\n" +
            "print(s == \"yes\" ? \"match\" : \"no\");"));
    }

    // ---- pure builtins ----------------------------------------------------

    @Test
    public void stringBuiltins() {
        assertEquals("Hello", run("print(substr(\"Hello World\", 1, 5));"));
        assertEquals("7", run("print(scan(\"World\", \"Hello World\"));"));
        assertEquals("ABC", run("print(upper(\"abc\"));"));
        assertEquals("hi", run("print(trim(\"  hi  \"));"));
        assertEquals("4", run("print(len(\"abcd\"));"));
        assertEquals("axc", run("print(replace(\"abc\", \"b\", \"x\"));"));
    }

    @Test
    public void numericBuiltins() {
        assertEquals("4.0", run("print(sqrt(16.0));"));
        assertEquals("5", run("print(abs(-5));"));
        assertEquals("42", run("print(int(\"42\"));"));
    }
}
