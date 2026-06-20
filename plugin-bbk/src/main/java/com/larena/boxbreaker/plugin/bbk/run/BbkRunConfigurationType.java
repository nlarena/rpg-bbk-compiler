package com.larena.boxbreaker.plugin.bbk.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/** Tipo de configuración de ejecución para programas BBK (el "Run" de la barra). */
public final class BbkRunConfigurationType implements ConfigurationType {

    public static final String ID = "BbkRunConfiguration";

    private final ConfigurationFactory factory = new ConfigurationFactory(this) {
        @Override
        public @NotNull String getId() {
            return "BBK";
        }

        @Override
        public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new BbkRunConfiguration(project, this, "BBK");
        }
    };

    @Override
    public @NotNull String getDisplayName() {
        return "BBK";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Ejecuta un programa BBK en la JVM";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.RunConfigurations.Application;
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{ factory };
    }

    public ConfigurationFactory getFactory() {
        return factory;
    }
}
