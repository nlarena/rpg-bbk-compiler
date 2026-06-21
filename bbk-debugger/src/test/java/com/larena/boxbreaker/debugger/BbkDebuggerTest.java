package com.larena.boxbreaker.debugger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BbkDebuggerTest {

    @Test
    public void mainlineLoopTracesAndPrints() {
        String src = """
            DCL-S total INT(10) INZ(0);
            DCL-S n INT(10) INZ(0);
            while (n < 4) {
              n += 1;
              total += n;
            }
            print(char(total));
            """;

        DebugResult r = BbkDebugger.trace(src);

        assertTrue("debería terminar bien: " + r.error(), r.ok());
        assertEquals("10\n", r.output());
        assertFalse("debería emitir pasos", r.steps().isEmpty());
        // el último paso (el print) deja total=10 en el snapshot
        TraceStep last = r.steps().get(r.steps().size() - 1);
        assertEquals("10", last.variables().get("total"));
    }

    @Test
    public void procedureRecursionRuns() {
        String src = """
            CTL-OPT MAIN(run);
            DCL-PROC fact(n INT(10) VALUE) -> INT(10) {
              if (n <= 1) {
                return 1;
              }
              return n * fact(n - 1);
            }
            DCL-PROC run {
              print(char(fact(5)));
            }
            """;

        DebugResult r = BbkDebugger.trace(src);

        assertTrue("debería terminar bien: " + r.error(), r.ok());
        assertEquals("120\n", r.output());
        // la recursión anida: algún paso tiene depth > 0
        assertTrue("la recursión debería anidar la profundidad",
            r.steps().stream().anyMatch(s -> s.depth() > 0));
    }

    @Test
    public void crossFileWithDataStructures() {
        NamedSource types = new NamedSource("types.bbk",
            "DCL-DS point QUALIFIED {\n  x INT(10);\n  y INT(10);\n}\n");
        NamedSource main = new NamedSource("main.bbk", """
            CTL-OPT MAIN(run);
            DCL-PROC sumPoint(p LIKEDS(point) CONST) -> INT(10) {
              return p.x + p.y;
            }
            DCL-PROC run {
              DCL-S a LIKEDS(point);
              a.x = 3;
              a.y = 4;
              print(char(sumPoint(a)));
            }
            """);

        DebugResult r = BbkDebugger.runFiles(java.util.List.of(types, main), null);

        assertTrue("debería correr cross-file con DS: " + r.error(), r.ok());
        assertEquals("7\n", r.output());   // la DS de types.bbk se usa desde main.bbk
    }

    @Test
    public void evaluatesExpressionsInScope() {
        String src = """
            DCL-S a INT(10) INZ(7);
            DCL-S b INT(10) INZ(3);
            print(char(a + b));
            """;

        String[] value = {null};
        boolean[] cond = {false};
        BbkDebugger.run(src, (step, evaluator) -> {
            if (step.statement().contains("print")) {   // en el print, a=7 y b=3 están en scope
                value[0] = evaluator.evaluate("a * b");
                cond[0] = evaluator.evaluateCondition("a > b");
            }
            return DebugListener.Decision.CONTINUE;
        });

        assertEquals("21", value[0]);            // watch: a * b
        assertTrue(cond[0]);                     // condición de breakpoint: a > b
    }

    @Test
    public void decimalLiteralWithDSuffix() {
        // Los literales decimales llevan sufijo 'd': 199.95d. No debe romper.
        DebugResult r = BbkDebugger.trace("""
            DCL-S total PACKED(11:2) INZ(0);
            total = 199.95d + 0.05d;
            print(char(total));
            """);

        assertTrue("debería parsear 199.95d: " + r.error(), r.ok());
        assertEquals("200.00\n", r.output());
    }

    @Test
    public void unsupportedFeatureFailsGracefully() {
        // Los arrays todavía no están: debe reportar el error capturado, sin reventar.
        DebugResult r = BbkDebugger.trace("DCL-S a INT(10);\na = a[1];\n");

        assertFalse("debería reportar error en vez de crashear", r.ok());
        assertNotNull(r.error());
    }
}
