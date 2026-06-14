package com.larena.boxbreaker.rpg;

import com.larena.boxbreaker.rpg.ast.RpgProgram;
import com.larena.boxbreaker.rpg.parser.RpgParser;
import com.larena.boxbreaker.rpg.translate.BbkEmitter;

/**
 * Entry point of the RPG &rarr; BBK frontend: the whole pipeline in one call.
 *
 * <pre>
 *   RPG source ──▶ RpgLexer ──▶ RpgParser ──▶ RpgProgram ──▶ BbkEmitter ──▶ BBK text
 * </pre>
 */
public final class RpgToBbk {

    private RpgToBbk() {}

    /** Translate free-format RPGLE source text to BBK source text. */
    public static String translate(String rpgSource) {
        RpgProgram program = RpgParser.parse(rpgSource);
        return BbkEmitter.emit(program);
    }
}
