package com.larena.boxbreaker.plugin.bbk.types;

import org.jetbrains.annotations.NotNull;

/**
 * Root of the BBK semantic type system. Every node returned by
 * {@link BbkTypeInferrer} implements this interface.
 *
 * <p><b>Naming note:</b> the BNF emits a PSI class also called
 * {@code BbkPrimitiveType} (in the {@code psi} package) which represents the
 * lexical keyword (KW_INT / KW_CHAR / ...). To avoid clash, the semantic
 * counterpart in this package is {@link BbkScalarType}. The two are unrelated.
 *
 * <p>{@code BbkType} is intentionally minimal — assignability lives in
 * {@link BbkAssignability} so the rules table can grow without touching every
 * implementor.
 */
public interface BbkType {

    /** Human-readable name used in completion popups, errors, hover. */
    @NotNull String getDisplayName();

    /**
     * Convenience shortcut for {@code BbkAssignability.areCompatible(this, target)}.
     * Override only if the type carries unusual assignability semantics
     * (e.g., {@link BbkUnknownType}).
     */
    default boolean isAssignableTo(@NotNull BbkType target) {
        return BbkAssignability.areCompatible(this, target);
    }
}
