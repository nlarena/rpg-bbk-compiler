package com.larena.boxbreaker.debugger;

import java.util.Map;

/**
 * Un paso de ejecución del debugger de BBK: la sentencia que se acaba de ejecutar,
 * el estado de las variables visibles después de ejecutarla, y la salida que ese
 * paso produjo (vacío si no imprimió nada).
 *
 * <p>Es la unidad que consume cualquier superficie (CLI hoy, el IDE mañana). La
 * {@code line} (1-based, 0 si se desconoce) viene del mapa de posiciones del
 * parser — es lo que habilita mapear breakpoints / resaltar la línea actual.
 *
 * @param step      número de paso (1..n)
 * @param line      línea de fuente (1-based) de la sentencia; 0 si se desconoce
 * @param depth     profundidad de anidamiento (0 = mainline; aumenta dentro de
 *                  procedimientos / subrutinas / bloques) — para indentar
 * @param statement la sentencia ejecutada, renderizada como texto
 * @param variables snapshot {nombre &rarr; valor} de las variables visibles
 * @param output    lo que este paso escribió por salida (puede ser "")
 */
public record TraceStep(int step, int line, int depth, String statement,
                        Map<String, String> variables, String output) {
    public TraceStep {
        variables = Map.copyOf(variables);
    }
}
