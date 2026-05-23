package com.larena.boxbreaker.plugin.bbk.scope;

import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Scope of the enclosing {@link BbkProcedureDeclaration}: inline parameters plus the
 * top-level locals declared in the procedure body (variables, constants, data
 * structures, subroutines).
 *
 * <p>Per decision #1 of {@code reference-scope/classes.md} §7, V1 is permissive:
 * forward references are allowed. Locals are visible anywhere inside the procedure,
 * not only from their line onward.
 */
public class BbkProcedureScope implements BbkScope {

    private final @NotNull BbkProcedureDeclaration procedure;
    private final @NotNull BbkScope parent;

    public BbkProcedureScope(@NotNull BbkProcedureDeclaration procedure, @NotNull BbkScope parent) {
        this.procedure = procedure;
        this.parent = parent;
    }

    @Override
    public @NotNull List<PsiNamedElement> getDeclarations() {
        List<PsiNamedElement> out = new ArrayList<>();

        // Inline parameters
        BbkInlineParamList paramList = PsiTreeUtil.findChildOfType(procedure, BbkInlineParamList.class);
        if (paramList != null) {
            for (BbkInlineParam p : PsiTreeUtil.findChildrenOfType(paramList, BbkInlineParam.class)) {
                out.add(p);
            }
        }

        // Locals: variables, constants, DSs and subroutines inside the procedure body.
        BbkBlockStatement body = PsiTreeUtil.findChildOfType(procedure, BbkBlockStatement.class);
        if (body != null) {
            for (BbkVariableDeclaration v : PsiTreeUtil.findChildrenOfType(body, BbkVariableDeclaration.class)) {
                out.add(v);
            }
            for (BbkConstantDeclaration c : PsiTreeUtil.findChildrenOfType(body, BbkConstantDeclaration.class)) {
                out.add(c);
            }
            for (BbkDataStructureDeclaration ds : PsiTreeUtil.findChildrenOfType(body, BbkDataStructureDeclaration.class)) {
                out.add(ds);
                // Non-qualified DS subfields flattened, like module scope.
                if (!isQualified(ds)) {
                    for (BbkDsSubfield s : PsiTreeUtil.findChildrenOfType(ds, BbkDsSubfield.class)) {
                        out.add(s);
                    }
                }
            }
            for (BbkSubroutineDefinition sr : PsiTreeUtil.findChildrenOfType(body, BbkSubroutineDefinition.class)) {
                out.add(sr);
            }
        }

        return out;
    }

    @Override
    public @Nullable BbkScope getParent() {
        return parent;
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
