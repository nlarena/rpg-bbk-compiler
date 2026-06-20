package com.larena.boxbreaker.debugger;

import com.larena.boxbreaker.core.parser.BbkParser;
import com.larena.boxbreaker.core.parser.ParsedProgram;

import java.util.ArrayList;
import java.util.List;

/**
 * Fachada del debugger de BBK. Parsea el fuente con el parser de bbk-core y lo
 * <b>interpreta</b> (tercer modo de ejecución, además de los dos backends),
 * emitiendo un paso por cada sentencia.
 *
 * <pre>
 *   DebugResult r = BbkDebugger.trace(source);          // corre entero, junta los pasos
 *   BbkDebugger.run(source, step -> { ...; return CONTINUE; });  // streaming (el IDE controla)
 * </pre>
 */
public final class BbkDebugger {

    private BbkDebugger() {}

    /** Corre el programa hasta el final y devuelve todos los pasos + la salida. */
    public static DebugResult trace(String source) {
        return run(source, null);
    }

    /**
     * Corre el programa notificando cada paso al {@code listener} (que puede pedir
     * parar). Igual junta todos los pasos en el {@link DebugResult}.
     */
    public static DebugResult run(String source, DebugListener listener) {
        List<TraceStep> collected = new ArrayList<>();
        DebugListener sink = step -> {
            collected.add(step);
            return listener == null ? DebugListener.Decision.CONTINUE : listener.onStep(step);
        };

        Interpreter interp = null;
        try {
            ParsedProgram parsed = BbkParser.parseWithPositions(source);
            interp = new Interpreter(sink, parsed.positions());
            interp.run(parsed.program());
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
