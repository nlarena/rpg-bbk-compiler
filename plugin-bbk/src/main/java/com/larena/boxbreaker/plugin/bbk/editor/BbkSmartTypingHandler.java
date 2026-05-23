package com.larena.boxbreaker.plugin.bbk.editor;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.larena.boxbreaker.plugin.bbk.BbkFileType;
import org.jetbrains.annotations.NotNull;

/**
 * Smart-typing polish for BBK files. Smooths over the friction created by Block A's
 * paren-auto-inserting {@code InsertHandler}s.
 *
 * <p>Two rules:
 *
 * <ol>
 *   <li><b>Semicolon jumps past trailing closing parens.</b> Typing {@code ;} when the
 *       cursor sits immediately before one or more {@code )} characters moves the cursor
 *       past them before the platform inserts the {@code ;}. So
 *       {@code MAIN(procName|)} + {@code ;} becomes {@code MAIN(procName);}, not
 *       {@code MAIN(procName;)}.</li>
 *   <li><b>Closer keys skip over a matching pre-existing closer.</b> Typing {@code )}
 *       when the next char is {@code )} just moves the cursor forward instead of
 *       inserting a second {@code )}. Same for {@code }} and {@code ]}. Matches the
 *       behaviour of Java/Kotlin/etc. in IntelliJ.</li>
 * </ol>
 *
 * <p>Only active inside files of type {@link BbkFileType}, so other languages are
 * untouched.
 */
public class BbkSmartTypingHandler extends TypedHandlerDelegate {

    @Override
    public @NotNull Result beforeCharTyped(char c,
                                           @NotNull Project project,
                                           @NotNull Editor editor,
                                           @NotNull PsiFile file,
                                           @NotNull FileType fileType) {
        if (!(file.getFileType() instanceof BbkFileType)) return Result.CONTINUE;

        // Rule 1: jump past trailing `)` on `;`
        if (c == ';') {
            int caret = editor.getCaretModel().getOffset();
            Document doc = editor.getDocument();
            int p = caret;
            int len = doc.getTextLength();
            CharSequence text = doc.getCharsSequence();
            while (p < len && text.charAt(p) == ')') p++;
            if (p > caret) {
                editor.getCaretModel().moveToOffset(p);
            }
            return Result.CONTINUE;
        }

        // Rule 2: skip-over for closer keys
        if (c == ')' || c == '}' || c == ']') {
            int caret = editor.getCaretModel().getOffset();
            Document doc = editor.getDocument();
            if (caret < doc.getTextLength() && doc.getCharsSequence().charAt(caret) == c) {
                editor.getCaretModel().moveToOffset(caret + 1);
                return Result.STOP;
            }
        }

        return Result.CONTINUE;
    }
}
