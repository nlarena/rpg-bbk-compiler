package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.larena.boxbreaker.plugin.bbk.run.BbkRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runner del executor Debug para configuraciones BBK: arranca una sesión del
 * XDebugger con {@link BbkDebugProcess}. Es lo que hace funcionar el botón 🐛
 * sobre un {@code .bbk}.
 */
public final class BbkDebugRunner extends GenericProgramRunner<RunnerSettings> {

    @Override
    public @NotNull String getRunnerId() {
        return "BbkDebugRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof BbkRunConfiguration;
    }

    @Override
    protected @Nullable RunContentDescriptor doExecute(@NotNull RunProfileState state,
                                                       @NotNull ExecutionEnvironment environment) throws ExecutionException {
        FileDocumentManager.getInstance().saveAllDocuments();   // debuggear el archivo guardado

        XDebugSession session = XDebuggerManager.getInstance(environment.getProject()).startSession(
            environment,
            new XDebugProcessStarter() {
                @Override
                public @NotNull XDebugProcess start(@NotNull XDebugSession session) {
                    String path = ((BbkRunConfiguration) environment.getRunProfile()).getFilePath();
                    return new BbkDebugProcess(session, path);
                }
            });
        return session.getRunContentDescriptor();
    }
}
