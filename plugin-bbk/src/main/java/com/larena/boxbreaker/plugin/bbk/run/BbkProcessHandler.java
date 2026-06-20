package com.larena.boxbreaker.plugin.bbk.run;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.larena.boxbreaker.core.backend.jvm.BbkRunner;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * "Proceso" en-proceso: compila y corre el .bbk con el backend JVM en un hilo del
 * pool, vuelca su salida a la consola del Run, y termina. No lanza un proceso
 * externo (BBK corre dentro del IDE, como ya lo hace el plugin con bbk-core).
 */
final class BbkProcessHandler extends ProcessHandler {

    private final String filePath;

    BbkProcessHandler(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void startNotify() {
        super.startNotify();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            int exitCode = 0;
            try {
                String source = Files.readString(Path.of(filePath));
                String output = BbkRunner.compileAndRun(source);
                if (!output.isEmpty()) {
                    notifyTextAvailable(output, ProcessOutputTypes.STDOUT);
                }
            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                notifyTextAvailable("No se pudo ejecutar: " + msg + "\n", ProcessOutputTypes.STDERR);
                exitCode = 1;
            }
            notifyProcessTerminated(exitCode);
        });
    }

    @Override
    protected void destroyProcessImpl() {
        notifyProcessTerminated(0);
    }

    @Override
    protected void detachProcessImpl() {
        notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
        return false;
    }

    @Override
    public @Nullable OutputStream getProcessInput() {
        return null;
    }
}
