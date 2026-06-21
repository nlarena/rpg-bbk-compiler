package com.larena.boxbreaker.core.backend.jvm;

import com.larena.boxbreaker.core.ast.BbkDeclaration;
import com.larena.boxbreaker.core.ast.BbkItem;
import com.larena.boxbreaker.core.ast.BbkProgram;
import com.larena.boxbreaker.core.parser.BbkParser;
import com.larena.boxbreaker.core.parser.ParsedProgram;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Escribe el bytecode de BBK a disco como {@code bbk/Main.class} bajo un directorio
 * temporal nuevo. A diferencia de {@link BbkRunner} (que corre el programa in-process
 * en el JVM del IDE), esto produce el artefacto que una <b>JVM aparte</b> puede
 * arrancar con {@code java -cp <raíz> bbk.Main} &mdash; el paso previo a depurar el
 * bytecode real con JDI/JDWP.
 *
 * <p>El ciclo de vida del directorio es del que llama: cuando el proceso de debug
 * termina, hay que borrarlo con {@link #deleteRecursively(Path)}.
 */
public final class BbkClassFile {

    private BbkClassFile() {}

    /**
     * Compila el fuente BBK <b>con info de debug</b> (líneas + SourceFile + locales) y escribe
     * {@code bbk/Main.class} en un directorio temporal nuevo. Devuelve la raíz del classpath.
     */
    public static Path writeToTempDir(String bbkSource) throws IOException {
        ParsedProgram parsed = BbkParser.parseWithPositions(bbkSource);
        return write(JvmCompiler.compile(parsed.program(), parsed.positions()));
    }

    /**
     * Como {@link #writeToTempDir(String)} pero combinando el fuente principal con otros
     * {@code .bbk} (cross-file): las declaraciones de {@code otherSources} (DS, prototipos,
     * etc.) se anteponen para que estén disponibles antes de su uso (p.ej. {@code LIKEDS}).
     * La info de debug es la del fuente principal; los otros archivos no llevan números de
     * línea (son declaraciones), así que los breakpoints del archivo principal mapean bien.
     *
     * <p>De los otros archivos se toman <b>solo declaraciones</b> (DS, constantes, prototipos,
     * procedimientos), no su mainline ni su {@code CTL-OPT MAIN}: aportan los nombres que el
     * archivo principal necesita, sin arrastrar la lógica de otro programa.
     */
    public static Path writeToTempDir(String mainSource, List<String> otherSources) throws IOException {
        if (otherSources == null || otherSources.isEmpty()) return writeToTempDir(mainSource);
        List<BbkItem> items = new ArrayList<>();
        for (String other : otherSources) {
            for (BbkItem item : BbkParser.parse(other).items()) {
                if (item instanceof BbkDeclaration && !(item instanceof BbkDeclaration.CtlOpt)) items.add(item);
            }
        }
        ParsedProgram main = BbkParser.parseWithPositions(mainSource);
        items.addAll(main.program().items());
        return write(JvmCompiler.compile(new BbkProgram(items), main.positions()));
    }

    /**
     * Escribe bytecode ya compilado como {@code bbk/Main.class} bajo un directorio temporal
     * nuevo y devuelve la raíz del classpath (la que contiene el paquete {@code bbk/}).
     */
    public static Path write(byte[] classBytes) throws IOException {
        Path root = Files.createTempDirectory("bbk-debug");
        Path classFile = root.resolve(JvmCompiler.CLASS_NAME.replace('.', '/') + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, classBytes);
        return root;
    }

    /** Borra recursivamente un directorio creado por {@link #write} (al terminar el debug). */
    public static void deleteRecursively(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) return;
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
