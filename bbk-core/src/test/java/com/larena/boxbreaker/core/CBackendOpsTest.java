package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.backend.c.CCompiler;
import com.larena.boxbreaker.core.backend.c.CRunner;
import com.larena.boxbreaker.core.parser.BbkParser;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Parity coverage for the C back-end on the non-OS subsystems. The generated C
 * is asserted as text (there is no C compiler in this environment); a handful of
 * live gcc compile+run checks ({@code *WhenGcc}) exercise it end-to-end on a
 * machine that has a compiler, and no-op otherwise.
 */
public class CBackendOpsTest {

    private String c(String bbk) { return CCompiler.compile(BbkParser.parse(bbk)); }

    // ---- procedures + CTL-OPT MAIN ---------------------------------------

    @Test
    public void procedureBecomesFunctionWithForwardDecl() {
        String out = c("DCL-PROC add(a INT(10) VALUE, b INT(10) VALUE) -> INT(10) { return a + b; }\nprint(char(add(5, 7)));");
        assertTrue(out.contains("static long long add(long long p0, long long p1);"));   // prototype
        assertTrue(out.contains("static long long add(long long a, long long b) {"));     // definition
        assertTrue(out.contains("return a + b;"));
        assertTrue(out.contains("add(5LL, 7LL)"));
    }

    @Test
    public void ctlOptMainCallsTheEntryProcedure() {
        String out = c("CTL-OPT MAIN(go);\nDCL-PROC go { print(\"hi\"); }");
        assertTrue(out.contains("static void go(void)"));
        assertTrue(out.contains("go();"));      // invoked from main
    }

    // ---- decimals ---------------------------------------------------------

    @Test
    public void decimalUsesLongDoubleAndScales() {
        String out = c("DCL-S price PACKED(9:2) INZ(199.95d);\nDCL-S qty INT(10) INZ(3);\nDCL-S subtotal PACKED(11:2);\nsubtotal = price * qty;\nprint(subtotal);");
        assertTrue(out.contains("static long double price;"));
        assertTrue(out.contains("price = bbk_truncs(199.95L, 2);"));                       // INZ scaled
        assertTrue(out.contains("subtotal = bbk_truncs(price * (long double)(qty), 2);")); // result scaled to 2
        assertTrue(out.contains("printf(\"%.*Lf\\n\", 2, subtotal);"));
    }

    @Test
    public void decimalHalfUpRounding() {
        assertTrue(c("DCL-S x PACKED(11:2);\nx = 10.00d / 3.00d @halfup;\nprint(x);")
            .contains("x = bbk_round(10.00L / 3.00L, 2);"));
    }

    // ---- arrays + data structures ----------------------------------------

    @Test
    public void arraysAndDataStructures() {
        String out = c("DCL-DS person QUALIFIED { name VARCHAR(50); age INT(10); }\nDCL-S a INT(10) DIM(3);\na[0] = 10;\nperson.name = \"Nico\";\nprint(a[0]);");
        assertTrue(out.contains("static long long a[3];"));        // array global
        assertTrue(out.contains("static const char* person_name;"));// flattened DS subfield
        assertTrue(out.contains("a[0LL] = 10LL;"));
        assertTrue(out.contains("person_name = \"Nico\";"));
    }

    @Test
    public void arrayOfDataStructures() {
        String out = c("DCL-DS emp QUALIFIED DIM(2) { id INT(10); name VARCHAR(50); }\nemp[0].id = 1001;\nprint(char(emp[0].id));");
        assertTrue(out.contains("static long long emp_id[2];"));    // parallel array per subfield
        assertTrue(out.contains("emp_id[0LL] = 1001LL;"));
        assertTrue(out.contains("emp_id[0LL]"));
    }

    @Test
    public void templateAndLikeds() {
        String out = c("DCL-DS addr TEMPLATE { street VARCHAR(50); city VARCHAR(50); }\nDCL-DS home LIKEDS(addr);\nhome.street = \"Main\";\nprint(home.street);");
        assertFalse(out.contains("addr_street"));                   // TEMPLATE allocates no storage
        assertTrue(out.contains("static const char* home_street;"));
        assertTrue(out.contains("home_street = \"Main\";"));
    }

    // ---- strings ----------------------------------------------------------

    @Test
    public void stringConcatAndEquality() {
        assertTrue(c("DCL-S s VARCHAR(20) INZ(\"a\");\nprint(s + \"b\");").contains("bbk_cat(s, \"b\")"));
        assertTrue(c("DCL-S s VARCHAR(20) INZ(\"x\");\nprint(s == \"x\" ? \"y\" : \"n\");").contains("strcmp(s, \"x\") == 0"));
    }

    @Test
    public void stringBuiltins() {
        assertTrue(c("print(substr(\"Hello\", 1, 3));").contains("bbk_substr(\"Hello\", 1LL, 3LL)"));
        assertTrue(c("print(scan(\"lo\", \"Hello\"));").contains("bbk_scan(\"lo\", \"Hello\")"));
        assertTrue(c("print(upper(\"abc\"));").contains("bbk_upper(\"abc\")"));
        assertTrue(c("print(trim(\"  x  \"));").contains("bbk_trim(\"  x  \")"));
        assertTrue(c("print(len(\"abc\"));").contains("strlen(\"abc\")"));
        assertTrue(c("print(replace(\"abc\", \"b\", \"x\"));").contains("bbk_replace(\"abc\", \"b\", \"x\")"));
    }

    // ---- operators / control flow ----------------------------------------

    @Test
    public void powerAndSelectAndMonitor() {
        assertTrue(c("print(2 ** 10);").contains("pow("));   // ** is canonically FLOAT (matches the JVM back-end)
        String sel = c("DCL-S n INT(10) INZ(2);\nselect { when (n == 1) { print(\"a\"); } other { print(\"b\"); } }");
        assertTrue(sel.contains("if (n == 1LL) {"));
        assertTrue(sel.contains("} else {"));
        String mon = c("DCL-S r INT(10) INZ(0);\nmonitor { r = 10 / 0; } on-error { r = -1; }\nprint(r);");
        assertTrue(mon.contains("setjmp(bbk_jb[bbk_jsp++])"));
        assertTrue(mon.contains("bbk_div(10LL, 0LL)"));     // integer divide-by-zero is trapped
    }

    @Test
    public void subroutineInlinesWithLeavesr() {
        String out = c("DCL-S x INT(10) INZ(0);\nEXSR work;\nprint(x);\nBEGSR work; x = 1; if (x == 1) { LEAVESR; } x = 99; ENDSR;");
        assertTrue(out.contains("goto bbk_sr_work_"));      // LEAVESR -> goto the inlined end label
        assertTrue(out.contains(": ;"));                    // the end label
    }

    @Test
    public void specialBlankValue() {
        assertTrue(c("DCL-S s VARCHAR(10) INZ(*blank);\nprint(s);").contains("s = \"\";"));
    }

    // ---- live gcc compile + run (skipped when no compiler) ----------------

    @Test
    public void procedureRunsWhenGcc() throws Exception {
        if (!CRunner.hasCompiler()) return;
        assertEquals("12", CRunner.compileAndRun(
            "DCL-PROC add(a INT(10) VALUE, b INT(10) VALUE) -> INT(10) { return a + b; }\nprint(add(5, 7));").strip());
    }

    @Test
    public void decimalRunsWhenGcc() throws Exception {
        if (!CRunner.hasCompiler()) return;
        assertEquals("599.85", CRunner.compileAndRun(
            "DCL-S p PACKED(9:2) INZ(199.95d);\nDCL-S q INT(10) INZ(3);\nDCL-S s PACKED(11:2);\ns = p * q;\nprint(s);").strip());
    }

    // ---- data-structure parameters (by value) -----------------------------

    @Test
    public void dataStructureParamFlattensToSubfields() {
        String out = c(
            "DCL-DS point TEMPLATE QUALIFIED { x INT(10); y INT(10); }\n" +
            "DCL-PROC sumPoint(p LIKEDS(point)) -> INT(10) { return p.x + p.y; }\n" +
            "DCL-DS a LIKEDS(point);\n" +
            "a.x = 3; a.y = 4;\n" +
            "print(char(sumPoint(a)));");
        assertTrue(out.contains("static long long sumPoint(long long p_x, long long p_y) {"));   // DS param -> 2 escalares
        assertTrue(out.contains("return p_x + p_y;"));                                            // miembros -> p_x/p_y
        assertTrue(out.contains("sumPoint(a_x, a_y)"));                                           // DS argumento -> sus subcampos
    }

    @Test
    public void dataStructureParamRunsWhenGcc() throws Exception {
        if (!CRunner.hasCompiler()) return;
        assertEquals("7", CRunner.compileAndRun(
            "DCL-DS point TEMPLATE QUALIFIED { x INT(10); y INT(10); }\n" +
            "DCL-PROC sumPoint(p LIKEDS(point)) -> INT(10) { return p.x + p.y; }\n" +
            "DCL-DS a LIKEDS(point);\n" +
            "a.x = 3; a.y = 4;\n" +
            "print(sumPoint(a));").strip());
    }

    // ---- date runtime (paridad con el backend JVM) ------------------------

    @Test
    public void dateRuntimeEmitsCalendarHelpers() {
        String out = c("print(char(addmonths(date(\"2024-01-31\"), 1)));");
        assertTrue(out.contains("bbk_date_parse("));
        assertTrue(out.contains("bbk_date_addmonths("));
        assertTrue(out.contains("bbk_date_str("));
    }

    @Test
    public void dateRuntimeRunsWhenGcc() throws Exception {
        if (!CRunner.hasCompiler()) return;
        assertEquals("2024-01-15", CRunner.compileAndRun("print(char(date(\"2024-01-15\")));").strip());
        assertEquals("2024-01-15", CRunner.compileAndRun("print(date(\"2024-01-15\"));").strip());
        assertEquals("2024-02-04", CRunner.compileAndRun("print(char(adddays(date(\"2024-01-15\"), 20)));").strip());
        assertEquals("2024-02-29", CRunner.compileAndRun("print(char(addmonths(date(\"2024-01-31\"), 1)));").strip());
        assertEquals("2026-01-15", CRunner.compileAndRun("print(char(addyears(date(\"2024-01-15\"), 2)));").strip());
        assertEquals("2024", CRunner.compileAndRun("print(char(year(date(\"2024-01-15\"))));").strip());
        assertEquals("15", CRunner.compileAndRun("print(char(day(date(\"2024-01-15\"))));").strip());
        assertEquals("31", CRunner.compileAndRun("print(char(diffdays(date(\"2024-02-01\"), date(\"2024-01-01\"))));").strip());
        assertEquals("14:05:30", CRunner.compileAndRun("print(char(addminutes(time(\"13:45:30\"), 20)));").strip());
        assertEquals("16", CRunner.compileAndRun("print(char(hour(addhours(time(\"13:00:00\"), 3))));").strip());
        assertEquals("2024-01-16T14:45:30", CRunner.compileAndRun("print(char(addhours(timestamp(\"2024-01-15T13:45:30\"), 25)));").strip());
        assertEquals("before", CRunner.compileAndRun(
            "if (date(\"2024-01-15\") < date(\"2024-02-01\")) { print(\"before\"); } else { print(\"after\"); }").strip());
        assertEquals("2024-03-10", CRunner.compileAndRun(
            "DCL-DS r QUALIFIED { d DATE; }\nr.d = date(\"2024-03-10\");\nprint(char(r.d));").strip());
    }
}
