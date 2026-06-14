package com.larena.boxbreaker.plugin.bbk.scope;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.BbkBlockStatement;
import com.larena.boxbreaker.plugin.bbk.psi.BbkFile;
import com.larena.boxbreaker.plugin.bbk.psi.BbkProcedureDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and queries the BBK scope chain for a cursor position.
 *
 * <p>The chain is constructed innermost → outermost:
 *
 * <pre>
 *   BlockScope(innermost) → BlockScope(next) → ... → ProcedureScope → ModuleScope
 * </pre>
 *
 * <p>For a position outside any procedure, the chain reduces to {@code ModuleScope}.
 *
 * <p>All API is static — no per-call object allocation other than the scope chain
 * itself, which is intentionally small (a few elements) and short-lived per
 * {@code resolve()} / {@code getVariants()} call.
 */
public final class BbkScopeWalker {

    private BbkScopeWalker() {}

    /**
     * Builds the scope chain rooted at the innermost scope containing {@code position}.
     * Returns {@code null} only if the position is not inside a BBK file.
     */
    public static @Nullable BbkScope scopeAt(@NotNull PsiElement position) {
        PsiFile file = position.getContainingFile();
        if (!(file instanceof BbkFile bbkFile)) {
            return null;
        }

        BbkScope chain = new BbkModuleScope(bbkFile);

        BbkProcedureDeclaration proc = PsiTreeUtil.getParentOfType(position, BbkProcedureDeclaration.class);
        if (proc != null) {
            chain = new BbkProcedureScope(proc, chain);

            // Walk every BlockStatement from outermost (the procedure body) to innermost,
            // wrapping the chain so the innermost ends up at the head.
            List<BbkBlockStatement> blocks = new ArrayList<>();
            for (PsiElement el = position; el != null && el != proc; el = el.getParent()) {
                if (el instanceof BbkBlockStatement bs) blocks.add(bs);
            }
            // Reverse so we attach outermost block first.
            for (int i = blocks.size() - 1; i >= 0; i--) {
                chain = new BbkBlockScope(blocks.get(i), chain);
            }
        }
        return chain;
    }

    /**
     * Convenience: walks the chain looking for any declaration that matches
     * {@code name} (case-insensitive). Returns the first hit or {@code null}.
     */
    public static @Nullable PsiNamedElement resolve(@NotNull PsiElement position,
                                                    @NotNull String name) {
        BbkScope scope = scopeAt(position);
        while (scope != null) {
            for (PsiNamedElement d : scope.findByName(name)) {
                return d;
            }
            scope = scope.getParent();
        }
        return null;
    }

    /**
     * Convenience: walks the chain looking for a declaration of a specific concrete
     * type. Used by typed references (e.g. {@code LIKEDS} only resolves to a DCL-DS).
     */
    public static <T extends PsiNamedElement> @Nullable T resolveOfType(@NotNull PsiElement position,
                                                                        @NotNull String name,
                                                                        @NotNull Class<T> kind) {
        BbkScope scope = scopeAt(position);
        while (scope != null) {
            for (PsiNamedElement d : scope.findByName(name)) {
                if (kind.isInstance(d)) return kind.cast(d);
            }
            scope = scope.getParent();
        }
        return null;
    }

    /**
     * Convenience: flattens the chain into a single list (innermost first).
     * Used by {@code getVariants()} for completion.
     */
    public static @NotNull List<PsiNamedElement> allVisible(@NotNull PsiElement position) {
        List<PsiNamedElement> out = new ArrayList<>();
        BbkScope scope = scopeAt(position);
        while (scope != null) {
            out.addAll(scope.getDeclarations());
            scope = scope.getParent();
        }
        return out;
    }
}
