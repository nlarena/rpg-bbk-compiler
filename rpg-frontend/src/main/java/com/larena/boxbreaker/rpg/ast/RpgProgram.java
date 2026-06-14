package com.larena.boxbreaker.rpg.ast;

import java.util.List;

/**
 * The root of the AST: a whole free-format RPGLE compilation unit.
 *
 * @param free  whether the source opened with the {@code **FREE} directive
 * @param items the top-level declarations and statements, in source order
 */
public record RpgProgram(boolean free, List<RpgItem> items) {
    public RpgProgram {
        items = List.copyOf(items);
    }
}
