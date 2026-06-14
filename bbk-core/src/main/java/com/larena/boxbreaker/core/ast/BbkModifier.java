package com.larena.boxbreaker.core.ast;

import java.util.List;

/**
 * A declaration modifier with optional arguments: {@code INZ(0)},
 * {@code QUALIFIED}, {@code DIM(10)}, {@code VALUE}, {@code EXTPROC("name")},
 * {@code USAGE(*INPUT)}, a CTL-OPT keyword, etc.
 *
 * <p>The grammar models every modifier list uniformly as {@code modifier*};
 * this record captures any of them. The semantic phase interprets each name.
 */
public record BbkModifier(String name, List<BbkExpr> args) {
    public BbkModifier { args = List.copyOf(args); }

    public static BbkModifier bare(String name) { return new BbkModifier(name, List.of()); }
}
