package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.ast.*;
import com.larena.boxbreaker.core.parser.BbkParser;
import com.larena.boxbreaker.core.semantic.Diagnostic;
import com.larena.boxbreaker.core.semantic.SemanticAnalyzer;
import com.larena.boxbreaker.core.semantic.SemanticModel;
import com.larena.boxbreaker.core.semantic.Type;
import org.junit.Test;

import static org.junit.Assert.*;

/** The shared semantic analysis: expression typing, resolution and diagnostics. */
public class SemanticAnalyzerTest {

    private static SemanticModel analyze(String src) { return SemanticAnalyzer.analyze(BbkParser.parse(src)); }

    /** The single argument of {@code print(...)} in the i-th top-level item. */
    private static BbkExpr arg(BbkProgram p, int item) {
        BbkStatement.ExpressionStatement es = (BbkStatement.ExpressionStatement) p.items().get(item);
        return ((BbkExpr.Call) es.expr()).args().get(0);
    }

    private static boolean anyMessage(SemanticModel m, String fragment) {
        return m.diagnostics().stream().anyMatch(d -> d.message().contains(fragment));
    }

    @Test
    public void numericTowerAndConcat() {
        // note: `2.0` (no exponent) is a DEC literal -> DECIMAL; FLOAT needs an exponent or a FLOAT-typed var
        BbkProgram p = BbkParser.parse(
            "DCL-S f FLOAT(8);\nprint(1 + 2);\nprint(1 + 2.0);\nprint(f + 1);\nprint(\"a\" + 1);\nprint(1 < 2);\nprint(2 ** 3);\nprint(1 & 2);");
        SemanticModel m = SemanticAnalyzer.analyze(p);
        assertEquals(Type.INT, m.type(arg(p, 1)));
        assertEquals(Type.Kind.DECIMAL, ((Type.Scalar) m.type(arg(p, 2))).kind());  // 1 + 2.0 (DEC)
        assertEquals(Type.FLOAT, m.type(arg(p, 3)));      // f + 1, f is FLOAT-typed
        assertEquals(Type.STRING, m.type(arg(p, 4)));     // + on a string is concatenation
        assertEquals(Type.BOOL, m.type(arg(p, 5)));       // comparison
        assertEquals(Type.FLOAT, m.type(arg(p, 6)));      // ** is canonically FLOAT
        assertEquals(Type.INT, m.type(arg(p, 7)));        // bitwise
        assertFalse(m.hasErrors());
    }

    @Test
    public void resolvesIdentifiersAndMembers() {
        BbkProgram p = BbkParser.parse(
            "DCL-DS person QUALIFIED { name VARCHAR(50); age INT(10); }\nprint(person.name);\nprint(person.age);");
        SemanticModel m = SemanticAnalyzer.analyze(p);
        assertEquals(Type.STRING, m.type(arg(p, 1)));
        assertEquals(Type.INT, m.type(arg(p, 2)));
        assertFalse(m.hasErrors());
    }

    @Test
    public void arrayAndDsArrayElementTypes() {
        BbkProgram p = BbkParser.parse(
            "DCL-S a INT(10) DIM(3);\nDCL-DS emp QUALIFIED DIM(2) { id INT(10); }\nprint(a[0]);\nprint(emp[0].id);");
        SemanticModel m = SemanticAnalyzer.analyze(p);
        assertEquals(Type.INT, m.type(arg(p, 2)));        // a[0]
        assertEquals(Type.INT, m.type(arg(p, 3)));        // emp[0].id
        assertFalse(m.hasErrors());
    }

    @Test
    public void builtinReturnTypes() {
        BbkProgram p = BbkParser.parse("print(len(\"abc\"));\nprint(substr(\"abc\", 1, 2));\nprint(sqrt(2.0));");
        SemanticModel m = SemanticAnalyzer.analyze(p);
        assertEquals(Type.INT, m.type(arg(p, 0)));
        assertEquals(Type.STRING, m.type(arg(p, 1)));
        assertEquals(Type.FLOAT, m.type(arg(p, 2)));
    }

    @Test
    public void procedureSignatureAndCallType() {
        BbkProgram p = BbkParser.parse(
            "DCL-PROC add(a INT(10) VALUE, b INT(10) VALUE) -> INT(10) { return a + b; }\nprint(add(1, 2));");
        SemanticModel m = SemanticAnalyzer.analyze(p);
        assertEquals(2, m.signature("add").arity());
        assertEquals(Type.INT, m.signature("add").returnType());
        assertEquals(Type.INT, m.type(arg(p, 1)));
        assertFalse(m.hasErrors());
    }

    @Test
    public void constantsResolve() {
        BbkProgram p = BbkParser.parse("DCL-C MAX 5;\nprint(MAX * 2);");
        SemanticModel m = SemanticAnalyzer.analyze(p);
        assertEquals(Type.INT, m.type(arg(p, 1)));
        assertFalse(m.hasErrors());
    }

    @Test
    public void diagnosesUndeclaredName() {
        SemanticModel m = analyze("print(nope);");
        assertTrue(m.hasErrors());
        assertTrue(anyMessage(m, "undeclared name 'nope'"));
    }

    @Test
    public void diagnosesUnknownFunction() {
        SemanticModel m = analyze("print(bogus(1));");
        assertTrue(anyMessage(m, "unknown function 'bogus'"));
    }

    @Test
    public void diagnosesArityMismatch() {
        SemanticModel m = analyze("DCL-PROC f(a INT(10) VALUE) { } \nf(1, 2);");
        assertTrue(anyMessage(m, "expects 1 args"));
    }

    @Test
    public void diagnosesUnknownDsField() {
        SemanticModel m = analyze("DCL-DS d QUALIFIED { x INT(10); }\nprint(d.y);");
        assertTrue(anyMessage(m, "no field 'y'"));
    }
}
