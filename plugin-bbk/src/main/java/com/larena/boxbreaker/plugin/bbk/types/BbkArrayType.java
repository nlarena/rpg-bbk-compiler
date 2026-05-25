package com.larena.boxbreaker.plugin.bbk.types;

import org.jetbrains.annotations.NotNull;

/**
 * Wraps an element type with a {@code DIM(n)} modifier. Smart completion
 * treats a bare array variable as the array (not the element) — subscripting
 * (parser already attaches {@code [i]} as a postfix suffix) yields the element
 * type, which is computed by unwrapping one level.
 *
 * <p>Open decision (see {@code smart-completion/theory.md} §8): this initial
 * implementation keeps the array as its own type. Inspections and parameter
 * info can refine later.
 */
public final class BbkArrayType implements BbkType {

    private final @NotNull BbkType element;
    private final int dimension;

    public BbkArrayType(@NotNull BbkType element, int dimension) {
        this.element = element;
        this.dimension = dimension;
    }

    public @NotNull BbkType getElement() { return element; }
    public int getDimension() { return dimension; }

    @Override
    public @NotNull String getDisplayName() {
        return element.getDisplayName() + " DIM(" + dimension + ")";
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
