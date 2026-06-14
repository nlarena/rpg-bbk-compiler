package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.backend.jvm.BbkRunner;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiny CLI: compile a {@code .bbk} file to JVM bytecode and run it.
 *
 * <pre>
 *   gradlew :bbk-core:run --args="examples/sample.bbk"
 * </pre>
 */
public final class BbkCli {

    private BbkCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: BbkCli <file.bbk>");
            System.exit(2);
        }
        String source = Files.readString(Path.of(args[0]));
        System.out.print(BbkRunner.compileAndRun(source));
    }
}
