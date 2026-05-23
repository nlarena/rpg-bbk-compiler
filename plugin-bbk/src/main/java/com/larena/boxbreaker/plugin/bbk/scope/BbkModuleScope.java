package com.larena.boxbreaker.plugin.bbk.scope;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The outermost scope of a BBK file: every top-level declaration.
 *
 * <p>Includes:
 * <ul>
 *   <li>Standalone variables ({@code DCL-S})</li>
 *   <li>Constants ({@code DCL-C})</li>
 *   <li>Data structures ({@code DCL-DS}) — and, for non-{@code QUALIFIED} DSs, the
 *       subfields themselves so they are visible by bare name (decision #2 of
 *       {@code reference-scope/classes.md} §7).</li>
 *   <li>Prototypes ({@code DCL-PR}) and procedures ({@code DCL-PROC}). When both exist
 *       for the same name, both are returned; consumers can prefer one over the other
 *       (decision #5: definition wins).</li>
 *   <li>Files ({@code DCL-F})</li>
 * </ul>
 */
public class BbkModuleScope implements BbkScope {

    private final @NotNull BbkFile file;

    public BbkModuleScope(@NotNull BbkFile file) {
        this.file = file;
    }

    @Override
    public @NotNull List<PsiNamedElement> getDeclarations() {
        List<PsiNamedElement> out = new ArrayList<>();
        for (PsiElement child : file.getChildren()) {
            collect(child, out);
        }
        return out;
    }

    @Override
    public @Nullable BbkScope getParent() {
        return null;
    }

    private void collect(@NotNull PsiElement element, @NotNull List<PsiNamedElement> out) {
        if (element instanceof BbkVariableDeclaration
            || element instanceof BbkConstantDeclaration
            || element instanceof BbkPrototypeDeclaration
            || element instanceof BbkProcedureDeclaration
            || element instanceof BbkFileDeclaration) {
            out.add((PsiNamedElement) element);
            return;
        }
        if (element instanceof BbkDataStructureDeclaration ds) {
            out.add(ds);
            // Flatten subfields when the DS is not QUALIFIED (decision #2).
            if (!isQualified(ds)) {
                for (BbkDsSubfield sub : PsiTreeUtil.findChildrenOfType(ds, BbkDsSubfield.class)) {
                    out.add(sub);
                }
            }
        }
    }

    private static boolean isQualified(@NotNull BbkDataStructureDeclaration ds) {
        for (BbkDsModifier mod : PsiTreeUtil.findChildrenOfType(ds, BbkDsModifier.class)) {
            if (mod.getText() != null && mod.getText().toUpperCase().startsWith("QUALIFIED")) {
                return true;
            }
        }
        return false;
    }
}
