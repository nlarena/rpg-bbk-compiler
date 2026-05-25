package com.larena.boxbreaker.plugin.bbk.types;

import com.larena.boxbreaker.plugin.bbk.psi.BbkDataStructureDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Semantic type for a QUALIFIED data structure. Holds a reference to the
 * declaring PSI ({@code DCL-DS ...}) so member access can be resolved on
 * demand without snapshotting the subfields here.
 *
 * <p>Equality is reference-equality on the underlying declaration — two DS
 * with the same shape but different declarations are different types
 * (consistent with the conservative default chosen in
 * {@code smart-completion/theory.md} §8 decision #3).
 */
public final class BbkStructType implements BbkType {

    private final @NotNull BbkDataStructureDeclaration declaration;

    public BbkStructType(@NotNull BbkDataStructureDeclaration declaration) {
        this.declaration = declaration;
    }

    public @NotNull BbkDataStructureDeclaration getDeclaration() {
        return declaration;
    }

    @Override
    public @NotNull String getDisplayName() {
        String name = declaration.getName();
        return "DS " + (name != null ? name : "<anonymous>");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BbkStructType that)) return false;
        return declaration.equals(that.declaration);
    }

    @Override
    public int hashCode() {
        return declaration.hashCode();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    /** Convenience null-safe upcast — never throws, returns null if PSI is gone. */
    public static @Nullable BbkStructType safeOf(@Nullable BbkDataStructureDeclaration decl) {
        return decl != null ? new BbkStructType(decl) : null;
    }
}
