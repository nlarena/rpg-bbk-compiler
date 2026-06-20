package com.larena.boxbreaker.debugger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CLI del debugger de BBK: corre un {@code .bbk} y muestra la traza paso a paso
 * (sentencia + variables + salida producida), y al final la salida completa.
 *
 * <pre>
 *   gradlew :bbk-debugger:run --args="examples/sample.bbk"
 * </pre>
 */
public final class DebugCli {

    private DebugCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("uso: DebugCli <archivo.bbk>");
            System.exit(2);
        }

        String source = Files.readString(Path.of(args[0]));
        DebugResult result = BbkDebugger.trace(source);

        System.out.println("=== Traza ===");
        for (TraceStep s : result.steps()) {
            String indent = "  ".repeat(s.depth());
            String loc = s.line() > 0 ? "L" + s.line() : "  -";
            System.out.printf("%3d  %-4s %s%s%n", s.step(), loc, indent, s.statement());
            System.out.printf("     %s    { %s }%n", indent, vars(s.variables()));
            if (!s.output().isEmpty()) {
                System.out.printf("     %s    >> %s%n", indent, s.output().replace("\n", "\\n"));
            }
        }

        System.out.println("\n=== Salida ===");
        System.out.print(result.output());
        if (!result.output().endsWith("\n")) System.out.println();

        if (!result.ok()) {
            System.out.println("\n=== Error ===");
            System.out.println(result.error());
        }
    }

    private static String vars(Map<String, String> variables) {
        return variables.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", "));
    }
}
