package com.larena.boxbreaker.debugger;

import com.larena.boxbreaker.core.ast.BbkItem;
import com.larena.boxbreaker.core.ast.BbkProgram;
import com.larena.boxbreaker.core.parser.BbkParser;
import com.larena.boxbreaker.core.parser.ParsedProgram;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Fachada del debugger de BBK. Parsea el/los fuente(s) con el parser de bbk-core
 * y los <b>interpreta</b>, emitiendo un paso por cada sentencia.
 *
 * <p>Soporta varios archivos: {@link #runFiles} combina sus items en un solo
 * programa (con posiciones por archivo), así un programa con declaraciones
 * cruzadas entre archivos corre como uno solo.
 */
public final class BbkDebugger {

    private BbkDebugger() {}

    /** Corre un único fuente hasta el final y devuelve todos los pasos + la salida. */
    public static DebugResult trace(String source) {
        return run(source, null);
    }

    /** Corre un único fuente notificando cada paso al {@code listener}. */
    public static DebugResult run(String source, DebugListener listener) {
        return runFiles(List.of(new NamedSource("", source)), listener);
    }

    /** Corre un programa multi-archivo: combina todos los fuentes y los interpreta. */
    public static DebugResult runFiles(List<NamedSource> sources, DebugListener listener) {
        List<TraceStep> collected = new ArrayList<>();
        DebugListener sink = step -> {
            collected.add(step);
            return listener == null ? DebugListener.Decision.CONTINUE : listener.onStep(step);
        };

        Interpreter interp = null;
        try {
            List<BbkItem> items = new ArrayList<>();
            IdentityHashMap<BbkItem, String> files = new IdentityHashMap<>();
            IdentityHashMap<BbkItem, Integer> lines = new IdentityHashMap<>();

            for (NamedSource ns : sources) {
                ParsedProgram parsed = BbkParser.parseWithPositions(ns.text());
                items.addAll(parsed.program().items());
                for (BbkItem item : parsed.positions().items()) {
                    files.put(item, ns.name());
                    lines.put(item, parsed.positions().lineOf(item));
                }
            }

            interp = new Interpreter(sink, new Locations(files, lines));
            interp.run(new BbkProgram(items));
            return new DebugResult(collected, interp.output(), null);
        } catch (Interpreter.Stopped stopped) {
            return new DebugResult(collected, output(interp), null);   // parado por el listener
        } catch (Interpreter.DebugException e) {
            return new DebugResult(collected, output(interp), e.getMessage());
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new DebugResult(collected, output(interp), "error de ejecución: " + msg);
        }
    }

    private static String output(Interpreter interp) {
        return interp == null ? "" : interp.output();
    }
}
