package com.larena.boxbreaker.debugger;

import com.larena.boxbreaker.rpg.RpgToBbk;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end: RPG free-format &rarr; (frontend) &rarr; BBK &rarr; (intérprete) &rarr; ejecución.
 * Demuestra que ya se pueden <b>ejecutar sentencias de RPG</b> del subconjunto no-SO.
 */
public class RpgPipelineTest {

    @Test
    public void runsRpgEndToEnd() throws Exception {
        String rpg = """
            **FREE
            dcl-s total int(10) inz(0);
            dcl-s i int(10);
            for i = 1 to 5;
              total = total + i;
            endfor;
            """;

        String bbk = RpgToBbk.translate(rpg);               // RPG -> BBK (frontend)
        DebugResult result = BbkDebugger.trace(bbk);        // BBK -> ejecución (intérprete)

        // Dejar el BBK generado + el resultado en un archivo para poder inspeccionarlo.
        Files.writeString(Path.of(System.getProperty("java.io.tmpdir"), "rpg_e2e.txt"),
            "=== RPG ===\n" + rpg
                + "\n=== BBK generado por el frontend ===\n" + bbk
                + "\n=== Ejecución ===\nok=" + result.ok() + "  error=" + result.error()
                + "  total(final)=" + finalVar(result, "total") + "\n");

        assertTrue("debería ejecutar bien: " + result.error(), result.ok());
        assertEquals("1+2+3+4+5", "15", finalVar(result, "total"));   // RPG ejecutado de verdad
    }

    private static String finalVar(DebugResult result, String name) {
        String value = null;
        for (TraceStep s : result.steps()) {
            if (s.variables().containsKey(name)) value = s.variables().get(name);
        }
        return value;
    }
}
