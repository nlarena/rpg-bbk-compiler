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
 * <p>In the BBK grammar, every top-level construct (DCL-S, DCL-C, DCL-DS, DCL-PR,
 * DCL-PROC, DCL-F, CTL-OPT, directive, ...) is wrapped in a {@link BbkTopLevelItem}.
 * So {@code file.getChildren()} returns {@code BbkTopLevelItem}s, NOT the declarations
 * directly. We walk one extra level via {@code BbkTopLevelItem.getXxxDeclaration()}
 * accessors to reach the actual named element.
 */
public class BbkModuleScope implements BbkScope {

    private final @NotNull BbkFile file;

    public BbkModuleScope(@NotNull BbkFile file) {
        this.file = file;
    }

    @Override
    public @NotNull List<PsiNamedElement> getDeclarations() {
        List<PsiNamedElement> out = new ArrayList<>();
        for (BbkTopLevelItem item : PsiTreeUtil.getChildrenOfTypeAsList(file, BbkTopLevelItem.class)) {
            collectFromItem(item, out);
        }
        return out;
    }

    @Override
    public @Nullable BbkScope getParent() {
        return null;
    }

    private static void collectFromItem(@NotNull BbkTopLevelItem item, @NotNull List<PsiNamedElement> out) {
        // BbkTopLevelItem has one populated accessor per declaration kind. Try them in order.
        PsiElement decl = unwrap(item);
        if (decl == null) return;

        if (decl instanceof BbkVariableDeclaration
            || decl instanceof BbkConstantDeclaration
            || decl instanceof BbkPrototypeDeclaration
            || decl instanceof BbkProcedureDeclaration
            || decl instanceof BbkFileDeclaration) {
            out.add((PsiNamedElement) decl);
            return;
        }
        if (decl instanceof BbkDataStructureDeclaration ds) {
            out.add(ds);
            // Flatten subfields when the DS is not QUALIFIED.
            if (!isQualified(ds)) {
                for (BbkDsSubfield sub : PsiTreeUtil.findChildrenOfType(ds, BbkDsSubfield.class)) {
                    out.add(sub);
                }
            }
        }
    }

    /**
     * Returns the actual declaration PSI inside a {@code BbkTopLevelItem}, or {@code null}
     * if the wrapped element is not one we care about (e.g., a CTL-OPT statement,
     * directive, or unknown_item).
     */
    private static @Nullable PsiElement unwrap(@NotNull BbkTopLevelItem item) {
        if (item.getVariableDeclaration() != null)        return item.getVariableDeclaration();
        if (item.getConstantDeclaration() != null)        return item.getConstantDeclaration();
        if (item.getDataStructureDeclaration() != null)   return item.getDataStructureDeclaration();
        if (item.getFileDeclaration() != null)            return item.getFileDeclaration();
        if (item.getPrototypeDeclaration() != null)       return item.getPrototypeDeclaration();
        if (item.getProcedureDeclaration() != null)       return item.getProcedureDeclaration();
        return null;
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
