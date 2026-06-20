package com.larena.boxbreaker.debugger;

import java.util.List;

/**
 * Resultado de una corrida de debug: todos los pasos, la salida completa del
 * programa y, si algo falló, el mensaje de error (null si terminó bien).
 */
public record DebugResult(List<TraceStep> steps, String output, String error) {
    public DebugResult {
        steps = List.copyOf(steps);
    }

    public boolean ok() {
        return error == null;
    }
}
