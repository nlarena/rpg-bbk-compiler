package com.larena.boxbreaker.core.parser;

import com.larena.boxbreaker.core.ast.BbkItem;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Posiciones de fuente de los items del AST, indexadas <b>por identidad</b> de
 * nodo (consistente con cómo el análisis semántico keya por identidad). Es la
 * base para mapear líneas &harr; nodos (breakpoints, línea actual) sin meter
 * offsets dentro de los {@code record} del AST.
 */
public final class SourceMap {

    private final Map<BbkItem, SourceSpan> spans;

    public SourceMap(Map<BbkItem, SourceSpan> spans) {
        this.spans = new IdentityHashMap<>(spans);
    }

    /** Rango de un item, o {@code null} si no se registró. */
    public SourceSpan span(BbkItem item) {
        return item == null ? null : spans.get(item);
    }

    /** Línea (1-based) de inicio del item, o 0 si se desconoce. */
    public int lineOf(BbkItem item) {
        SourceSpan s = span(item);
        return s == null ? 0 : s.startLine();
    }

    public boolean isEmpty() {
        return spans.isEmpty();
    }
}
