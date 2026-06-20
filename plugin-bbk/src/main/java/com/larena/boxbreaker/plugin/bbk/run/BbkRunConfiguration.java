package com.larena.boxbreaker.plugin.bbk.run;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Configuración "Run BBK": ejecuta el archivo .bbk indicado con el backend JVM. */
public final class BbkRunConfiguration extends LocatableConfigurationBase<Element> {

    private String filePath = "";

    BbkRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath == null ? "" : filePath;
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new BbkRunSettingsEditor();
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new BbkRunProfileState(environment, this);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (filePath == null || filePath.isBlank()) {
            throw new RuntimeConfigurationException("Indicá el archivo BBK a ejecutar");
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        element.setAttribute("bbkFile", filePath);
    }

    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);
        String value = element.getAttributeValue("bbkFile");
        if (value != null) filePath = value;
    }
}
