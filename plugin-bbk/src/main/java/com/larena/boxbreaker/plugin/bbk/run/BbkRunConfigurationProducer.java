package com.larena.boxbreaker.plugin.bbk.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Crea automáticamente una configuración "Run BBK" a partir del archivo .bbk en
 * contexto. Es lo que hace que el botón ▶ "Run (Current File)" de la barra y el
 * "Run 'archivo.bbk'" del menú aparezcan para un .bbk.
 */
public final class BbkRunConfigurationProducer extends LazyRunConfigurationProducer<BbkRunConfiguration> {

    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return ConfigurationTypeUtil.findConfigurationType(BbkRunConfigurationType.class).getFactory();
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull BbkRunConfiguration configuration,
                                                    @NotNull ConfigurationContext context,
                                                    @NotNull Ref<PsiElement> sourceElement) {
        VirtualFile file = bbkFile(context);
        if (file == null) return false;
        configuration.setFilePath(file.getPath());
        configuration.setName(file.getName());
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull BbkRunConfiguration configuration,
                                              @NotNull ConfigurationContext context) {
        VirtualFile file = bbkFile(context);
        return file != null && file.getPath().equals(configuration.getFilePath());
    }

    private static VirtualFile bbkFile(ConfigurationContext context) {
        PsiElement location = context.getPsiLocation();
        if (location == null) return null;
        PsiFile psiFile = location.getContainingFile();
        if (psiFile == null) return null;
        VirtualFile vf = psiFile.getVirtualFile();
        return (vf != null && "bbk".equals(vf.getExtension())) ? vf : null;
    }
}
