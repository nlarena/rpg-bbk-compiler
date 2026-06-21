package com.larena.boxbreaker.jvmdebug;

/**
 * Una posición en el fuente BBK: archivo (el {@code SourceFile} de la clase, p.ej.
 * {@code Main.bbk}) y línea 1-based. Es lo que el debugger usa para ubicar una pausa
 * en el editor y para instalar breakpoints.
 *
 * @param sourceFile nombre del fuente BBK, o {@code null} si la clase no trae SourceFile
 * @param line       línea 1-based en ese fuente
 */
public record BbkPosition(String sourceFile, int line) {
}
