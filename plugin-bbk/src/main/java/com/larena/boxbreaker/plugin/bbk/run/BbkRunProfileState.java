package com.larena.boxbreaker.plugin.bbk.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;

/**
 * Estado de ejecución: arma la consola del Run y conecta el {@link BbkProcessHandler}.
 * {@link CommandLineState} se encarga de crear la consola y adjuntarla.
 */
final class BbkRunProfileState extends CommandLineState {

    private final BbkRunConfiguration configuration;

    BbkRunProfileState(@NotNull ExecutionEnvironment environment, @NotNull BbkRunConfiguration configuration) {
        super(environment);
        this.configuration = configuration;
    }

    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        return new BbkProcessHandler(configuration.getFilePath());
    }
}
