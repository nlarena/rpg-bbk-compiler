package com.larena.boxbreaker.jvmdebug;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Se conecta por JDI al debuggee y verifica que ve preparar la clase bbk.Main. */
public class BbkDebugSessionTest {

    @Test
    public void attachesAndSeesMainClassPrepare() throws Exception {
        try (BbkDebuggee debuggee = BbkDebuggee.launch("print(char(12));\n");
             BbkDebugSession session = BbkDebugSession.attach(debuggee.host(), debuggee.port())) {

            session.requestMainClassPrepare();      // pedir ANTES de reanudar el arranque
            session.resume();                       // soltar la suspensión de suspend=y

            ReferenceType mainType = awaitMainClassPrepare(session.vm(), 15000);
            assertNotNull("debería recibir el ClassPrepare de bbk.Main", mainType);
            assertEquals("bbk.Main", mainType.name());
        }
    }

    /** Bombea la cola de eventos JDI hasta ver el ClassPrepare de bbk.Main (o agotar el tiempo). */
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
            set.resume();   // VMStart u otros eventos: reanudar y seguir esperando
        }
        return null;
    }
}
