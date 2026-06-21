package com.larena.boxbreaker.jvmdebug;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Depura el bytecode real: breakpoint + step nativo + lectura de variables mapeadas a BBK + exit. */
public class BbkDebugControllerTest {

    /** Listener de test: encola cada parada y registra la salida. */
    private static final class Recorder implements BbkDebugListener {
        final BlockingQueue<BbkPausedContext> pauses = new LinkedBlockingQueue<>();
        final CountDownLatch exited = new CountDownLatch(1);
        final AtomicInteger exitCode = new AtomicInteger(Integer.MIN_VALUE);
        final StringBuilder output = new StringBuilder();

        @Override public void onPaused(BbkPausedContext ctx) { pauses.add(ctx); }
        @Override public void onExited(int code) { exitCode.set(code); exited.countDown(); }
        @Override public synchronized void onOutput(String text) { output.append(text); }
    }

    @Test
    public void breakpointStepAndExit() throws Exception {
        String src = "DCL-S x INT(10) INZ(0);\n"   // 1
            + "x = 1;\n"                            // 2
            + "x = 2;\n"                            // 3  <- breakpoint
            + "x = 3;\n"                            // 4
            + "print(char(x));\n";                  // 5
        Recorder rec = new Recorder();
        try (BbkDebugController controller = BbkDebugController.launch(src, rec)) {
            controller.addBreakpoint(3);
            controller.start();

            BbkPausedContext at3 = rec.pauses.poll(15, TimeUnit.SECONDS);
            assertNotNull("debería frenar en el breakpoint", at3);
            assertEquals(3, at3.position().line());
            assertEquals("1", valueOf(at3, "x"));        // la línea 2 ya corrió

            controller.stepOver();
            BbkPausedContext at4 = rec.pauses.poll(15, TimeUnit.SECONDS);
            assertNotNull(at4);
            assertEquals(4, at4.position().line());
            assertEquals("2", valueOf(at4, "x"));

            controller.stepOver();
            BbkPausedContext at5 = rec.pauses.poll(15, TimeUnit.SECONDS);
            assertNotNull(at5);
            assertEquals(5, at5.position().line());
            assertEquals("3", valueOf(at5, "x"));

            controller.resume();
            assertTrue("debería terminar", rec.exited.await(15, TimeUnit.SECONDS));
            assertEquals(0, rec.exitCode.get());
        }
    }

    @Test
    public void readsVariablesMappedToBbk() throws Exception {
        String src = "DCL-S total INT(10) INZ(0);\n"        // 1
            + "DCL-S price PACKED(7:2) INZ(10.50);\n"        // 2
            + "DCL-S flag BOOL INZ(*ON);\n"                  // 3
            + "DCL-DS info QUALIFIED { count INT(10); }\n"   // 4
            + "total = 5;\n"                                 // 5
            + "info.count = 9;\n"                            // 6
            + "print(char(total));\n";                       // 7  <- breakpoint
        Recorder rec = new Recorder();
        try (BbkDebugController controller = BbkDebugController.launch(src, rec)) {
            controller.addBreakpoint(7);
            controller.start();

            BbkPausedContext ctx = rec.pauses.poll(15, TimeUnit.SECONDS);
            assertNotNull(ctx);
            List<BbkVariable> vars = ctx.variables();

            assertEquals("5", valueOf(ctx, "total"));
            assertEquals("10.50", valueOf(ctx, "price"));   // decimal con su escala
            assertEquals("*ON", valueOf(ctx, "flag"));      // booleano estilo BBK

            // la DS reconstruida como árbol: info -> { count = 9 }
            BbkVariable info = find(vars, "info");
            assertNotNull("debería estar la DS 'info'", info);
            assertTrue("info debería ser compuesta", info.isComposite());
            assertEquals("9", find(info.children(), "count").value());

            controller.resume();
            assertTrue(rec.exited.await(15, TimeUnit.SECONDS));
        }
    }

    @Test
    public void evaluatesExpressionsAtBreakpoint() throws Exception {
        String src = "DCL-S a INT(10) INZ(0);\n"     // 1
            + "DCL-S b INT(10) INZ(0);\n"             // 2
            + "a = 6;\n"                              // 3
            + "b = 7;\n"                              // 4
            + "print(char(a * b));\n";                // 5  <- breakpoint
        Recorder rec = new Recorder();
        try (BbkDebugController controller = BbkDebugController.launch(src, rec)) {
            controller.addBreakpoint(5);
            controller.start();

            BbkPausedContext ctx = rec.pauses.poll(15, TimeUnit.SECONDS);
            assertNotNull(ctx);
            assertEquals("42", ctx.evaluate("a * b"));
            assertEquals("13", ctx.evaluate("a + b"));
            assertEquals("*ON", ctx.evaluate("a < b"));
            assertTrue(ctx.evaluateCondition("a < b"));
            assertTrue(!ctx.evaluateCondition("a == b"));

            controller.resume();
            assertTrue(rec.exited.await(15, TimeUnit.SECONDS));
        }
    }

    @Test
    public void conditionalBreakpointOnlyStopsWhenTrue() throws Exception {
        String src = "DCL-S total INT(10) INZ(0);\n"   // 1
            + "total = 5;\n"                            // 2
            + "print(char(total));\n";                  // 3  <- breakpoint condicional

        // condición verdadera: frena
        Recorder hit = new Recorder();
        try (BbkDebugController c = BbkDebugController.launch(src, hit)) {
            c.addBreakpoint(3, "total == 5");
            c.start();
            assertNotNull("debería frenar cuando la condición es verdadera", hit.pauses.poll(15, TimeUnit.SECONDS));
            c.resume();
            assertTrue(hit.exited.await(15, TimeUnit.SECONDS));
        }

        // condición falsa: no frena, corre hasta el final
        Recorder miss = new Recorder();
        try (BbkDebugController c = BbkDebugController.launch(src, miss)) {
            c.addBreakpoint(3, "total == 99");
            c.start();
            assertTrue("debería terminar sin frenar", miss.exited.await(15, TimeUnit.SECONDS));
            assertEquals("no debería haber frenado", 0, miss.pauses.size());
        }
    }

    @Test
    public void debugsAcrossFiles() throws Exception {
        // 'customer' está declarado en OTRO fuente (cross-file), igual que el caso que falló en runIde
        String customer = "DCL-DS customer TEMPLATE QUALIFIED { id INT(10); name VARCHAR(50); }\n";
        String main = "DCL-DS c LIKEDS(customer);\n"   // 1
            + "c.id = 7;\n"                             // 2
            + "print(char(c.id));\n";                   // 3  <- breakpoint
        Recorder rec = new Recorder();
        try (BbkDebugController controller = BbkDebugController.launch(main, List.of(customer), rec)) {
            controller.addBreakpoint(3);
            controller.start();

            BbkPausedContext ctx = rec.pauses.poll(15, TimeUnit.SECONDS);
            assertNotNull("debería compilar y frenar pese al LIKEDS cross-file", ctx);
            assertEquals(3, ctx.position().line());
            assertEquals("7", ctx.evaluate("c.id"));

            controller.resume();
            assertTrue(rec.exited.await(15, TimeUnit.SECONDS));
        }
    }

    @Test
    public void forwardsProgramOutput() throws Exception {
        Recorder rec = new Recorder();
        try (BbkDebugController controller = BbkDebugController.launch("print(char(12));\n", rec)) {
            controller.start();   // sin breakpoints: corre de una
            assertTrue("debería terminar", rec.exited.await(15, TimeUnit.SECONDS));
            assertEquals(0, rec.exitCode.get());
            assertTrue("debería capturar la salida del programa: " + rec.output,
                rec.output.toString().contains("12"));
        }
    }

    private static String valueOf(BbkPausedContext ctx, String name) {
        BbkVariable v = find(ctx.variables(), name);
        return v == null ? null : v.value();
    }

    private static BbkVariable find(List<BbkVariable> vars, String name) {
        for (BbkVariable v : vars) if (v.name().equals(name)) return v;
        return null;
    }
}
