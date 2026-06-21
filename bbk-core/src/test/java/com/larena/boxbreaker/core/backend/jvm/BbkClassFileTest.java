package com.larena.boxbreaker.core.backend.jvm;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** El bytecode de BBK se escribe a disco como un bbk/Main.class real, listo para una JVM aparte. */
public class BbkClassFileTest {

    @Test
    public void writesRunnableClassToDisk() throws Exception {
        Path root = BbkClassFile.writeToTempDir("print(char(12));\n");
        try {
            Path classFile = root.resolve("bbk/Main.class");
            assertTrue("debería existir bbk/Main.class en " + root, Files.exists(classFile));

            // magic de un .class de la JVM: 0xCAFEBABE
            byte[] head = Files.readAllBytes(classFile);
            assertTrue("debería tener el magic 0xCAFEBABE",
                head.length > 4 && (head[0] & 0xFF) == 0xCA && (head[1] & 0xFF) == 0xFE
                    && (head[2] & 0xFF) == 0xBA && (head[3] & 0xFF) == 0xBE);

            // cargarlo desde el dir temporal y correrlo: lo mismo que hará `java -cp <root> bbk.Main`
            assertEquals("12", runFromDir(root).strip());
        } finally {
            BbkClassFile.deleteRecursively(root);
            assertTrue("el dir temporal debería borrarse", !Files.exists(root));
        }
    }

    /** Carga bbk.Main desde el classpath en disco y captura lo que imprime su main. */
    private static String runFromDir(Path root) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (URLClassLoader cl = new URLClassLoader(new URL[]{root.toUri().toURL()},
                ClassLoader.getSystemClassLoader())) {
            Class<?> main = cl.loadClass("bbk.Main");
            Method m = main.getMethod("main", String[].class);
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            m.invoke(null, (Object) new String[0]);
        } finally {
            System.setOut(original);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }
}
