package com.larena.boxbreaker.plugin.bbk.debug;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.awt.Font;

/** Diálogo simple, scrolleable y read-only, con texto monoespaciado (salida de Run/Trace). */
final class BbkOutputDialog extends DialogWrapper {

    private final String text;

    BbkOutputDialog(@Nullable Project project, String title, String text) {
        super(project);
        this.text = text;
        setTitle(title);
        init();
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        JTextArea area = new JTextArea(text, 30, 96);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setCaretPosition(0);
        return new JBScrollPane(area);
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{ getOKAction() };
    }
}
