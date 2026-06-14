package com.larena.boxbreaker.core.ast;

import java.util.List;

/**
 * The root of the BBK AST: a whole compilation unit (grammar
 * {@code translation_unit}) — top-level declarations and statements in order.
 */
public record BbkProgram(List<BbkItem> items) {
    public BbkProgram { items = List.copyOf(items); }
}
