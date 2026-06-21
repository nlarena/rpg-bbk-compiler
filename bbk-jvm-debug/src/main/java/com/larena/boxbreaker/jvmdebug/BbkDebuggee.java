package com.larena.boxbreaker.jvmdebug;

import com.larena.boxbreaker.core.backend.jvm.BbkClassFile;
import com.larena.boxbreaker.core.backend.jvm.JvmCompiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Una JVM aparte corriendo {@code bbk.Main} con el agente JDWP, suspendida y esperando
 * que el debugger se conecte por JDI. Es el debuggee real: a diferencia de
 * {@code BbkRunner} (in-process), acá se depura el bytecode que de verdad se ejecuta.
 *
 * <p>Se forkea con {@code address=127.0.0.1:0} (puerto libre elegido por el SO) y
 * {@code suspend=y} (frena antes de {@code main}, así los breakpoints del arranque
 * valen). El puerto real se lee de la línea que el agente imprime al levantar.
 *
 * <p>{@link #close()} mata el proceso y borra el directorio del {@code .class}.
 */
public final class BbkDebuggee implements AutoCloseable {

    private static final String JDWP =
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:0";
    private static final Pattern LISTENING =
        Pattern.compile("Listening for transport \\S+ at address:\\s*(?:.*:)?(\\d+)");
    private static final String HOST = "127.0.0.1";

    private final Process process;
    private final int port;
    private final Path classpathRoot;
    private final BufferedReader output;

    private BbkDebuggee(Process process, int port, Path classpathRoot, BufferedReader output) {
        this.process = process;
        this.port = port;
        this.classpathRoot = classpathRoot;
        this.output = output;
    }

    /** Compila el fuente BBK a {@code .class} y lanza la JVM de debug suspendida en JDWP. */
    public static BbkDebuggee launch(String bbkSource) throws IOException {
        return launch(bbkSource, java.util.List.of());
    }

    /** Igual, combinando el fuente principal con otros {@code .bbk} (declaraciones cross-file). */
    public static BbkDebuggee launch(String mainSource, java.util.List<String> otherSources) throws IOException {
        Path root = BbkClassFile.writeToTempDir(mainSource, otherSources);
        ProcessBuilder pb = new ProcessBuilder(javaBinary(), JDWP, "-cp", root.toString(), JvmCompiler.CLASS_NAME);
        pb.redirectErrorStream(true);              // juntamos stderr con stdout: la línea del agente y la salida del programa
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "bbk-jdwp-port");
            t.setDaemon(true);
            return t;
        });
        try {
            Future<Integer> portFuture = exec.submit(() -> readPort(reader, process));
            int port = portFuture.get(20, TimeUnit.SECONDS);
            return new BbkDebuggee(process, port, root, reader);
        } catch (TimeoutException | ExecutionException e) {
            abort(process, root);
            throw new IOException("no se pudo lanzar la JVM de debug: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            abort(process, root);
            throw new IOException("interrumpido al lanzar la JVM de debug", e);
        } finally {
            exec.shutdownNow();
        }
    }

    /** Lee la línea {@code "Listening for transport ... at address: <puerto>"} del agente JDWP. */
    private static int readPort(BufferedReader reader, Process process) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher m = LISTENING.matcher(line);
            if (m.find()) return Integer.parseInt(m.group(1));
        }
        throw new IOException("la JVM de debug terminó sin anunciar el puerto JDWP (exit "
            + (process.isAlive() ? "?" : process.exitValue()) + ")");
    }

    /** Host JDWP donde escucha el debuggee (localhost). */
    public String host() {
        return HOST;
    }

    /** Puerto JDWP que el debuggee eligió: por acá se conecta el conector JDI. */
    public int port() {
        return port;
    }

    /** El proceso forkeado. */
    public Process process() {
        return process;
    }

    /** La raíz del classpath en disco (contiene {@code bbk/Main.class}). */
    public Path classpathRoot() {
        return classpathRoot;
    }

    /** Salida (stdout+stderr) del programa, disponible una vez que se reanuda. */
    public BufferedReader output() {
        return output;
    }

    @Override
    public void close() {
        process.destroy();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        try {
            BbkClassFile.deleteRecursively(classpathRoot);
        } catch (IOException ignored) {
            // best-effort: si no se puede borrar el temporal, no es fatal
        }
    }

    private static void abort(Process process, Path root) {
        process.destroyForcibly();
        try {
            BbkClassFile.deleteRecursively(root);
        } catch (IOException ignored) {
            // best-effort
        }
    }

    private static String javaBinary() {
        String home = System.getProperty("java.home");
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return Path.of(home, "bin", windows ? "java.exe" : "java").toString();
    }
}
