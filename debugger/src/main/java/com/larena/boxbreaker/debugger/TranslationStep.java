package com.larena.boxbreaker.debugger;

import java.util.List;

/**
 * One unit of the RPG &rarr; BBK translation: a top-level RPG construct, the
 * source lines it occupies, and the BBK lines it produced.
 *
 * @param rpgStartLine 1-based first RPG source line of the construct
 * @param rpgEndLine   1-based last RPG source line (inclusive)
 * @param rpgLines     the RPG source lines [start..end]
 * @param bbkLines     the BBK lines emitted for this construct (may be empty)
 */
public record TranslationStep(int rpgStartLine, int rpgEndLine,
                              List<String> rpgLines, List<String> bbkLines) {
    public TranslationStep {
        rpgLines = List.copyOf(rpgLines);
        bbkLines = List.copyOf(bbkLines);
    }
}
