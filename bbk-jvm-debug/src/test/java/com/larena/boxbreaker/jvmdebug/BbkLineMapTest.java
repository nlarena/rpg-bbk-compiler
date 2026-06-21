package com.larena.boxbreaker.jvmdebug;

import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** El mapeo línea BBK ↔ Location del bytecode funciona en los dos sentidos, contra el debuggee real. */
public class BbkLineMapTest {

    @Test
    public void mapsBetweenBytecodeLocationsAndBbkLines() throws Exception {
        String src = "DCL-S x INT(10) INZ(0);\n"   // línea 1: declaración (sin código mapeable)
            + "x = 5;\n"                            // línea 2: asignación
            + "print(char(x));\n";                  // línea 3: print
        try (BbkDebuggee debuggee = BbkDebuggee.launch(src);
             BbkDebugSession session = BbkDebugSession.attach(debuggee.host(), debuggee.port())) {

            session.requestMainClassPrepare();
            session.resume();
            ReferenceType mainType = awaitMainClassPrepare(session.vm(), 15000);
            assertNotNull("debería preparar bbk.Main", mainType);

            BbkLineMap lineMap = new BbkLineMap(mainType);

            // líneas con código: la asignación (2) y el print (3)
            assertTrue("líneas ejecutables esperadas 2 y 3, fueron " + lineMap.executableLines(),
                lineMap.executableLines().containsAll(List.of(2, 3)));
            assertTrue("debería poder frenar en la línea 2", lineMap.hasCodeAt(2));
            assertFalse("no debería haber código en una línea inexistente", lineMap.hasCodeAt(999));

            // forward (línea 2 -> Location) + reverse (Location -> Main.bbk:2)
            List<Location> locs = lineMap.locationsOfLine(2);
            assertFalse("debería haber un Location para la línea 2", locs.isEmpty());
            BbkPosition pos = BbkLineMap.positionOf(locs.get(0));
            assertEquals(2, pos.line());
            assertEquals("Main.bbk", pos.sourceFile());
        }
    }

    private static ReferenceType awaitMainClassPrepare(VirtualMachine vm, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            EventSet set = vm.eventQueue().remove(500);
            if (set == null) continue;
            for (Event e : set) {
                if (e instanceof ClassPrepareEvent cpe && "bbk.Main".equals(cpe.referenceType().name())) {
                    return cpe.referenceType();
                }
            }
            set.resume();
        }
        return null;
    }
}
