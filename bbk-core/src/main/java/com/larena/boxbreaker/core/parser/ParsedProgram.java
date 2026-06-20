package com.larena.boxbreaker.core.parser;

import com.larena.boxbreaker.core.ast.BbkProgram;

/** Resultado de parsear con posiciones: el AST + el mapa de posiciones de fuente. */
public record ParsedProgram(BbkProgram program, SourceMap positions) {}
