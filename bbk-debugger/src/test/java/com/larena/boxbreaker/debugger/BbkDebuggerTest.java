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
    public void unsupportedFeatureFailsGracefully() {
        // Una estructura (DCL-DS) no está en v1: debe reportar el error capturado, sin reventar.
        DebugResult r = BbkDebugger.trace("DCL-DS rec { a INT(10); }\n");

        assertFalse("debería reportar error en vez de crashear", r.ok());
        assertNotNull(r.error());
    }
}
