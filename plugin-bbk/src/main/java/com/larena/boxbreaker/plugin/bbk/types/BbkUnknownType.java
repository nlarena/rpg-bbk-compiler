package com.larena.boxbreaker.plugin.bbk.types;

import org.jetbrains.annotations.NotNull;

/**
 * Sentinel returned by {@link BbkTypeInferrer} when a sub-expression cannot be
 * resolved (unparsed reference, recursive {@code LIKE} chain, etc.).
 *
 * <p>Has a non-restrictive {@link #isAssignableTo} so {@code Unknown} never
 * blocks completion or hides a candidate. The smart-completion provider treats
 * an expected-type of {@code Unknown} as "no filter" → fall back to BASIC.
 */
public final class BbkUnknownType implements BbkType {

    public static final BbkUnknownType INSTANCE = new BbkUnknownType();

    private BbkUnknownType() {}

    @Override
    public @NotNull String getDisplayName() { return "<unknown>"; }

    @Override
    public boolean isAssignableTo(@NotNull BbkType target) {
        // Unknown is compatible with anything (and anything is compatible with Unknown,
        // see BbkAssignability) — preserves liveness in presence of partial PSI.
        return true;
    }
}
