package com.larena.boxbreaker.plugin.bbk.scope;

import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One link in BBK's lexical-scope chain.
 *
 * <p>A scope answers two questions:
 *
 * <ul>
 *   <li>"What names are visible from here?" — used by completion variants.</li>
 *   <li>"Is there a declaration named {@code X} visible from here?" — used by references.</li>
 * </ul>
 *
 * <p>Scopes form a linked chain: {@link #getParent()} returns the enclosing scope or
 * {@code null} at the file root. Resolution walks the chain inside-out, stopping at the
 * first match. The chain is built on demand by {@link BbkScopeWalker#scopeAt}.
 *
 * <p>BBK is case-insensitive, so implementations compare names with
 * {@link String#equalsIgnoreCase(String)}.
 */
public interface BbkScope {

    /** All named declarations visible at this scope level (excluding parents). */
    @NotNull List<PsiNamedElement> getDeclarations();

    /** Convenience filter: declarations whose name matches (case-insensitive). */
    default @NotNull List<PsiNamedElement> findByName(@NotNull String name) {
        return getDeclarations().stream()
            .filter(d -> name.equalsIgnoreCase(d.getName()))
            .toList();
    }

    @Nullable BbkScope getParent();
}
