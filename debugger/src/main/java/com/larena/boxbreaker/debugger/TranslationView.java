package com.larena.boxbreaker.debugger;

import com.larena.boxbreaker.rpg.RpgToBbk;

import java.util.List;

/**
 * Renders a translation trace as a readable, line-by-line side-by-side view:
 * RPG source on the left, the BBK it produces on the right.
 *
 * <pre>
 *   RPG                                  │ BBK
 *   ─────────────────────────────────────┼──────────────────────────────
 *    1  dcl-s counter int(10) inz(0);    │ DCL-S counter INT(10) INZ(0);
 *    3  if counter &gt; 0;                   │ if (counter &gt; 0) {
 *    4    counter = counter - 1;          │   counter = counter - 1;
 *    5  endif;                            │ }
 * </pre>
 */
public final class TranslationView {

    private static final int MIN_RPG_WIDTH = 24;
    private static final int MAX_RPG_WIDTH = 56;

    private TranslationView() {}

    /** Convenience: translate and render in one call. */
    public static String render(String rpgSource) {
        return render(TranslationTracer.trace(rpgSource));
    }

    public static String render(List<TranslationStep> steps) {
        int rpgWidth = rpgColumnWidth(steps);
        StringBuilder sb = new StringBuilder();

        String left = pad("RPG", rpgWidth);
        sb.append(left).append(" | BBK\n");
        sb.append("-".repeat(rpgWidth + 1)).append("+").append("-".repeat(30)).append('\n');

        for (TranslationStep step : steps) {
            int rows = Math.max(step.rpgLines().size(), step.bbkLines().size());
            for (int i = 0; i < rows; i++) {
                String rpg = i < step.rpgLines().size() ? step.rpgLines().get(i) : "";
                String bbk = i < step.bbkLines().size() ? step.bbkLines().get(i) : "";
                String num = i == 0 ? String.format("%3d  ", step.rpgStartLine()) : "     ";
                sb.append(pad(num + rpg, rpgWidth)).append(" | ").append(bbk).append('\n');
            }
        }
        return sb.toString();
    }

    private static int rpgColumnWidth(List<TranslationStep> steps) {
        int max = MIN_RPG_WIDTH;
        for (TranslationStep step : steps) {
            for (String line : step.rpgLines()) {
                max = Math.max(max, line.length() + 5);   // + line-number prefix
            }
        }
        return Math.min(max, MAX_RPG_WIDTH);
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    /**
     * Tiny CLI: {@code java ... TranslationView <file.rpgle>} prints the trace,
     * then the full generated BBK.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: TranslationView <file.rpgle>");
            System.exit(2);
        }
        String rpg = java.nio.file.Files.readString(java.nio.file.Path.of(args[0]));
        System.out.println(render(rpg));
        System.out.println("===== full BBK =====");
        System.out.println(RpgToBbk.translate(rpg));
    }
}
