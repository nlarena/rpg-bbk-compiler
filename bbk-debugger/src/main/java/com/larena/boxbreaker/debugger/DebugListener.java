package com.larena.boxbreaker.debugger;

/**
 * Observa la ejecución paso a paso y decide si continuar. Es el punto de
 * integración con cualquier UI: un CLI puede simplemente acumular los pasos; el
 * IDE puede pausar en un breakpoint, refrescar el panel de variables y resumir.
 *
 * <p>El intérprete llama {@link #onStep(TraceStep)} <b>después</b> de ejecutar cada
 * sentencia. Devolver {@link Decision#STOP} detiene la ejecución de inmediato.
 */
@FunctionalInterface
public interface DebugListener {

    enum Decision { CONTINUE, STOP }

    Decision onStep(TraceStep step);
}
