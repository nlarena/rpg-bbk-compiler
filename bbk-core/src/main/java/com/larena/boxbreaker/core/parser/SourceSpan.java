package com.larena.boxbreaker.core.parser;

/**
 * Rango de fuente (1-based) de un nodo del AST: posición de inicio y de fin.
 * Lo produce el parser en un mapa lateral, sin tocar los {@code record} del AST.
 */
public record SourceSpan(int startLine, int startColumn, int endLine, int endColumn) {}
