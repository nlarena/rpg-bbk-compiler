package com.larena.boxbreaker.debugger;

/**
 * Evalúa una expresión BBK en el <b>entorno actual</b> del intérprete (las
 * variables visibles en el paso donde se está). Es la costura que habilita
 * tanto los breakpoints condicionales como el "Evaluate expression" del IDE.
 *
 * <p>Sólo lee el entorno (no invoca procedimientos del usuario), así que es
 * seguro llamarlo mientras el intérprete está pausado.
 */
public interface Evaluator {

    /** Evalúa la expresión y devuelve su valor renderizado; lanza si no compila/evalúa. */
    String evaluate(String expression);

    /** Evalúa la expresión como condición y devuelve su verdad (para breakpoints condicionales). */
    boolean evaluateCondition(String expression);
}
