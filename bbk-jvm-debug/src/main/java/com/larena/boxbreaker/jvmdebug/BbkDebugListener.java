package com.larena.boxbreaker.jvmdebug;

/**
 * Recibe los eventos de la sesión de debug. Lo llama el event loop de JDI desde su
 * propio hilo: cuando el programa frena (breakpoint o step) y cuando termina.
 */
public interface BbkDebugListener {

    /** El programa frenó: la VM está suspendida y desde {@code context} se lee el estado. */
    void onPaused(BbkPausedContext context);

    /** El programa terminó (la VM murió); {@code exitCode} es el código de salida del proceso. */
    void onExited(int exitCode);

    /** Salida (stdout+stderr) del programa, línea a línea. Por defecto se ignora. */
    default void onOutput(String text) {
    }
}
