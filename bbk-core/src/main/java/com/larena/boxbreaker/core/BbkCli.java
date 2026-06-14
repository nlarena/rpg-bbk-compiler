package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.backend.c.CRunner;
import com.larena.boxbreaker.core.backend.jvm.BbkRunner;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiny CLI for the BBK core. Compile a {@code .bbk} file and either run it on
 * the JVM (default) or emit C.
 *
 * <pre>
 *   gradlew :bbk-core:run --args="examples/sample.bbk"            # run on the JVM
 *   gradlew :bbk-core:run --args="--emit-c examples/sample.bbk"   # print the generated C
 *   gradlew :bbk-core:run --args="--run-c examples/sample.bbk"    # compile with gcc and run (needs gcc)
 * </pre>
 */
public final class BbkCli {

    private BbkCli() {}

    public static void main(String[] args) throws Exception {
        String mode = "jvm";
        String file = null;
        for (String a : args) {
            switch (a) {
                case "--emit-c" -> mode = "emit-c";
                case "--run-c" -> mode = "run-c";
                default -> file = a;
            }
        }
        if (file == null) {
            System.err.println("usage: BbkCli [--emit-c | --run-c] <file.bbk>");
            System.exit(2);
        }
        String source = Files.readString(Path.of(file));
        switch (mode) {
            case "emit-c" -> System.out.print(CRunner.toC(source));
            case "run-c" -> System.out.print(CRunner.compileAndRun(source));
            default -> System.out.print(BbkRunner.compileAndRun(source));
        }
    }
}
