package com.larena.boxbreaker.plugin.bbk.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Semantic representation of a BBK primitive scalar type:
 * {@code INT(n)}, {@code UNS(n)}, {@code CHAR(n)}, {@code VARCHAR(n)},
 * {@code PACKED(p:d)}, {@code ZONED(p:d)}, {@code BINDEC(p:d)}, {@code FLOAT},
 * {@code BOOL}, {@code DATE}, {@code TIME}, {@code TIMESTAMP},
 * {@code POINTER}, {@code VOID}.
 *
 * <p>Length / precision / decimals are stored on this object. {@code null}
 * means the type is parameter-less (BOOL, DATE, POINTER, ...) or generic
 * (used for literal types — see {@link #INT_LITERAL}).
 *
 * <p>Equality compares {@link Kind} + length + decimals. Two {@code INT(10)}
 * instances are equal regardless of where they were synthesised.
 */
public final class BbkScalarType implements BbkType {

    public enum Kind {
        INT, UNS,
        CHAR, VARCHAR,
        PACKED, ZONED, BINDEC,
        FLOAT,
        BOOL,
        DATE, TIME, TIMESTAMP,
        POINTER,
        VOID
    }

    /** Generic integer literal — {@code 42}. Assignable to any sized INT/UNS/PACKED. */
    public static final BbkScalarType INT_LITERAL = new BbkScalarType(Kind.INT, null, null);
    /** Generic char literal — {@code 'abc'}. Assignable to CHAR(n) of sufficient size. */
    public static final BbkScalarType CHAR_LITERAL = new BbkScalarType(Kind.CHAR, null, null);
    /** Generic decimal literal — {@code 1.5}. Assignable to PACKED(p:d). */
    public static final BbkScalarType DEC_LITERAL = new BbkScalarType(Kind.PACKED, null, null);
    /** Generic float literal — {@code 1.5e3}. Assignable to FLOAT. */
    public static final BbkScalarType FLOAT_LITERAL = new BbkScalarType(Kind.FLOAT, null, null);

    public static final BbkScalarType BOOL = new BbkScalarType(Kind.BOOL, null, null);
    public static final BbkScalarType DATE = new BbkScalarType(Kind.DATE, null, null);
    public static final BbkScalarType TIME = new BbkScalarType(Kind.TIME, null, null);
    public static final BbkScalarType TIMESTAMP = new BbkScalarType(Kind.TIMESTAMP, null, null);
    public static final BbkScalarType POINTER = new BbkScalarType(Kind.POINTER, null, null);
    public static final BbkScalarType VOID = new BbkScalarType(Kind.VOID, null, null);

    private final @NotNull Kind kind;
    private final @Nullable Integer length;   // CHAR(n) length, INT(n) precision, PACKED(p:d) p
    private final @Nullable Integer decimals; // PACKED(p:d) d, otherwise null

    public BbkScalarType(@NotNull Kind kind, @Nullable Integer length, @Nullable Integer decimals) {
        this.kind = kind;
        this.length = length;
        this.decimals = decimals;
    }

    public static @NotNull BbkScalarType of(@NotNull Kind kind) {
        return new BbkScalarType(kind, null, null);
    }

    public static @NotNull BbkScalarType of(@NotNull Kind kind, int length) {
        return new BbkScalarType(kind, length, null);
    }

    public static @NotNull BbkScalarType of(@NotNull Kind kind, int length, int decimals) {
        return new BbkScalarType(kind, length, decimals);
    }

    public @NotNull Kind getKind() { return kind; }
    public @Nullable Integer getLength() { return length; }
    public @Nullable Integer getDecimals() { return decimals; }

    /** {@code true} for a literal-sourced generic type (length == null and kind is numeric/char). */
    public boolean isGeneric() {
        return length == null && (kind == Kind.INT || kind == Kind.UNS
            || kind == Kind.CHAR || kind == Kind.VARCHAR
            || kind == Kind.PACKED || kind == Kind.ZONED
            || kind == Kind.BINDEC || kind == Kind.FLOAT);
    }

    @Override
    public @NotNull String getDisplayName() {
        if (length == null) return kind.name();
        if (decimals == null) return kind.name() + "(" + length + ")";
        return kind.name() + "(" + length + ":" + decimals + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BbkScalarType that)) return false;
        return kind == that.kind
            && Objects.equals(length, that.length)
            && Objects.equals(decimals, that.decimals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, length, decimals);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
