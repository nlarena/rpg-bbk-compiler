package com.larena.boxbreaker.core.backend.jvm;

import com.larena.boxbreaker.core.parser.BbkParser;
import com.larena.boxbreaker.core.parser.ParsedProgram;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

/**
 * Loads BBK bytecode into the current JVM and runs it — the "run on the JVM"
 * modality. From the IDE this runs in-process; from a CLI it's a single JVM.
 */
public final class BbkRunner {

    private BbkRunner() {}

    /** A classloader that defines a class straight from bytes. */
    private static final class BytesClassLoader extends ClassLoader {
        BytesClassLoader() { super(BbkRunner.class.getClassLoader()); }
        Class<?> define(String name, byte[] bytes) { return defineClass(name, bytes, 0, bytes.length); }
    }

    /** Compile BBK source (with debug info), run {@code bbk.Main.main}, and return everything it printed. */
    public static String compileAndRun(String bbkSource) {
        ParsedProgram parsed = BbkParser.parseWithPositions(bbkSource);
        return run(JvmCompiler.compile(parsed.program(), parsed.positions()));
    }

    /** Run already-compiled bytecode and capture its stdout. */
    public static String run(byte[] classBytes) {
        Class<?> main = new BytesClassLoader().define(JvmCompiler.CLASS_NAME, classBytes);
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured, true, java.nio.charset.StandardCharsets.UTF_8));
            Method m = main.getMethod("main", String[].class);
            m.invoke(null, (Object) new String[0]);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("BBK execution failed: " + cause, cause);
        } finally {
            System.setOut(original);
        }
        return captured.toString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
