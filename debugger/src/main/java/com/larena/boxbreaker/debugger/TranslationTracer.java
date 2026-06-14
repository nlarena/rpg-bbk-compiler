package com.larena.boxbreaker.debugger;

import com.larena.boxbreaker.rpg.parser.RpgParser;
import com.larena.boxbreaker.rpg.translate.BbkEmitter;

import java.util.ArrayList;
import java.util.List;

/**
 * Traces an RPG &rarr; BBK translation construct by construct, producing the
 * source-to-BBK line mapping consumed by {@link TranslationView}.
 *
 * <p>Granularity is one top-level RPG construct per {@link TranslationStep}
 * (a declaration or statement, including its whole body). Within each step the
 * RPG and BBK lines are paired for a line-by-line view.
 */
public final class TranslationTracer {

    private TranslationTracer() {}

    public static List<TranslationStep> trace(String rpgSource) {
        String[] src = splitLines(rpgSource);
        List<TranslationStep> steps = new ArrayList<>();
        for (RpgParser.ItemSpan span : RpgParser.parseSpans(rpgSource)) {
            List<String> rpgLines = sourceLines(src, span.startLine(), span.endLine());
            List<String> bbkLines = emittedLines(BbkEmitter.emit(span.item()));
            steps.add(new TranslationStep(span.startLine(), span.endLine(), rpgLines, bbkLines));
        }
        return steps;
    }

    private static String[] splitLines(String text) {
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].endsWith("\r")) lines[i] = lines[i].substring(0, lines[i].length() - 1);
        }
        return lines;
    }

    /** 1-based inclusive slice of the source lines. */
    private static List<String> sourceLines(String[] all, int start1, int end1) {
        List<String> out = new ArrayList<>();
        for (int i = start1; i <= end1 && i >= 1 && i <= all.length; i++) {
            out.add(all[i - 1]);
        }
        return out;
    }

    private static List<String> emittedLines(String bbk) {
        String trimmed = bbk.stripTrailing();
        if (trimmed.isEmpty()) return List.of();
        return List.of(trimmed.split("\n", -1));
    }
}
