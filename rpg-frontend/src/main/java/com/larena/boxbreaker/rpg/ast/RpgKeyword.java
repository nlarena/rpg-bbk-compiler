package com.larena.boxbreaker.rpg.ast;

import java.util.List;

/**
 * A declaration modifier keyword with optional arguments, e.g.
 * {@code INZ(0)}, {@code DIM(10)}, {@code QUALIFIED}, {@code CONST},
 * {@code EXTPROC('name')}.
 *
 * <p>The grammar models declaration modifiers uniformly as {@code { keyword }*}
 * (var-keyword, ds-keyword, f-keyword, pr-keyword, parm-keyword, proc-keyword,
 * ctl-opt keyword). This single record captures them all; the emitter maps each
 * name to its BBK equivalent.
 *
 * @param name keyword name as written (e.g. {@code "inz"}, {@code "qualified"})
 * @param args argument expressions inside the parentheses; empty if none
 */
public record RpgKeyword(String name, List<RpgExpr> args) {
    public RpgKeyword { args = List.copyOf(args); }

    public static RpgKeyword bare(String name) {
        return new RpgKeyword(name, List.of());
    }
}
