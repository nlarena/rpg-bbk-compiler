package com.larena.boxbreaker.plugin.bbk.run;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

/** Editor mínimo de la configuración: el path del archivo BBK. */
public final class BbkRunSettingsEditor extends SettingsEditor<BbkRunConfiguration> {

    private final JBTextField fileField = new JBTextField();
    private final JPanel panel;

    public BbkRunSettingsEditor() {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Archivo BBK:", fileField)
            .getPanel();
    }

    @Override
    protected void resetEditorFrom(@NotNull BbkRunConfiguration configuration) {
        fileField.setText(configuration.getFilePath());
    }

    @Override
    protected void applyEditorTo(@NotNull BbkRunConfiguration configuration) {
        configuration.setFilePath(fileField.getText());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return panel;
    }
}
