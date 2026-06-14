package com.larena.boxbreaker.core.backend.c;

import com.larena.boxbreaker.core.parser.BbkParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compiles BBK to C, then to a native executable with a C compiler, and runs it.
 * Requires {@code gcc} (or {@code cc}) on the PATH; otherwise raises a clear error.
 */
public final class CRunner {

    private CRunner() {}

    /** Translate BBK to C source. */
    public static String toC(String bbkSource) {
        return CCompiler.compile(BbkParser.parse(bbkSource));
    }

    /** Whether a usable C compiler is available. */
    public static boolean hasCompiler() {
        return findCompiler() != null;
    }

    /** Compile BBK to C, build a native binary, run it, return its stdout. */
    public static String compileAndRun(String bbkSource) throws IOException, InterruptedException {
        String compiler = findCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No C compiler (gcc/cc) found on PATH");
        }
        Path dir = Files.createTempDirectory("bbk-c");
        Path cFile = dir.resolve("program.c");
        Path exe = dir.resolve(isWindows() ? "program.exe" : "program");
        Files.writeString(cFile, toC(bbkSource), StandardCharsets.UTF_8);

        run(new ProcessBuilder(compiler, cFile.toString(), "-o", exe.toString()), dir);
        return run(new ProcessBuilder(exe.toString()), dir);
    }

    private static String run(ProcessBuilder pb, Path dir) throws IOException, InterruptedException {
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        if (code != 0) {
            throw new IllegalStateException("'" + String.join(" ", pb.command())
                + "' failed (exit " + code + "):\n" + output);
        }
        return output;
    }

    private static String findCompiler() {
        for (String c : new String[]{"gcc", "cc", "clang"}) {
            try {
                Process p = new ProcessBuilder(c, "--version").redirectErrorStream(true).start();
                if (p.waitFor() == 0) return c;
            } catch (IOException | InterruptedException ignored) { /* try next */ }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
