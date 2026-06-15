package com.larena.boxbreaker.core.semantic;

import java.util.List;

/**
 * A backend-neutral semantic type — the single vocabulary the two back-ends map
 * onto their own representations ({@code Jt} for the JVM, {@code Ct} for C).
 *
 * <p>Scalars collapse the BBK primitives to the five kinds the back-ends actually
 * distinguish: {@code INT} (INT/UNS), {@code FLOAT}, {@code DECIMAL}
 * (PACKED/ZONED/BINDEC, carrying its declared scale), {@code STRING}
 * (CHAR/VARCHAR) and {@code BOOL} (BOOL/IND). Arrays and data structures wrap
 * those. {@link #UNKNOWN} is the error-recovery type (a diagnostic was already
 * reported) and {@link #VOID} is a void procedure's result.
 */
public sealed interface Type permits Type.Scalar, Type.Array, Type.Ds, Type.Special {

    enum Kind { INT, FLOAT, DECIMAL, STRING, BOOL }

    /** A scalar value; {@code scale} is the decimal scale for {@code DECIMAL}, else -1. */
    record Scalar(Kind kind, int scale) implements Type {}

    /** A fixed-size array of {@code element}. */
    record Array(Type element, int dim) implements Type {}

    /** A data structure (or array of data structures) with named scalar subfields. */
    record Ds(String name, List<Field> fields, boolean array, int dim) implements Type {
        public Ds { fields = List.copyOf(fields); }
        public record Field(String name, Type type) {}

        public Type fieldType(String field) {
            for (Field f : fields) if (f.name().equalsIgnoreCase(field)) return f.type();
            return UNKNOWN;
        }
        public boolean hasField(String field) {
            for (Field f : fields) if (f.name().equalsIgnoreCase(field)) return true;
            return false;
        }
    }

    enum SpecialKind { UNKNOWN, VOID }
    record Special(SpecialKind kind) implements Type {}

    // ----- well-known instances -----
    Type INT = new Scalar(Kind.INT, -1);
    Type FLOAT = new Scalar(Kind.FLOAT, -1);
    Type STRING = new Scalar(Kind.STRING, -1);
    Type BOOL = new Scalar(Kind.BOOL, -1);
    Type UNKNOWN = new Special(SpecialKind.UNKNOWN);
    Type VOID = new Special(SpecialKind.VOID);

    static Type decimal(int scale) { return new Scalar(Kind.DECIMAL, scale); }

    // ----- queries -----
    default boolean is(Kind k) { return this instanceof Scalar s && s.kind() == k; }

    default boolean isNumeric() {
        return this instanceof Scalar s && (s.kind() == Kind.INT || s.kind() == Kind.FLOAT || s.kind() == Kind.DECIMAL);
    }

    default boolean isUnknown() { return this == UNKNOWN; }

    /** The decimal scale, or 0 for non-decimals (handy for display/rounding). */
    default int scaleOrZero() { return this instanceof Scalar s && s.kind() == Kind.DECIMAL ? Math.max(s.scale(), 0) : 0; }

    /**
     * The wider of two numeric types along {@code INT < FLOAT < DECIMAL}. A
     * {@code DECIMAL} result carries the larger of the two scales. Non-numeric
     * operands yield {@link #UNKNOWN}.
     */
    static Type wider(Type a, Type b) {
        if (!a.isNumeric() || !b.isNumeric()) return UNKNOWN;
        Kind ka = ((Scalar) a).kind(), kb = ((Scalar) b).kind();
        if (ka == Kind.DECIMAL || kb == Kind.DECIMAL) return decimal(Math.max(a.scaleOrZero(), b.scaleOrZero()));
        if (ka == Kind.FLOAT || kb == Kind.FLOAT) return FLOAT;
        return INT;
    }
}
