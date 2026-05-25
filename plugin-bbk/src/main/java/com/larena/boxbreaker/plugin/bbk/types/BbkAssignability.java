package com.larena.boxbreaker.plugin.bbk.types;

import org.jetbrains.annotations.NotNull;

/**
 * Rules for "is a value of type S usable where a value of type T is expected?"
 *
 * <p>Conservative V1 — only safe widenings:
 * <ul>
 *   <li>Identity ({@code INT(10) → INT(10)}).</li>
 *   <li>{@link BbkUnknownType} compatible with anything in both directions
 *       (keeps liveness when the inferrer hits unparsed PSI).</li>
 *   <li>Generic literal widening: {@code INT(*)} → any sized
 *       {@code INT/UNS/PACKED/ZONED/BINDEC}; {@code DEC(*)} → any
 *       {@code PACKED/ZONED/BINDEC}; {@code CHAR(*)} → any
 *       {@code CHAR/VARCHAR}; {@code FLOAT(*)} → {@code FLOAT}.</li>
 *   <li>Numeric precision widening within the same kind:
 *       {@code INT(5) → INT(10)} OK, reverse rejected.</li>
 *   <li>Character widening within the same kind:
 *       {@code CHAR(20) → CHAR(50)} OK, reverse rejected.</li>
 *   <li>Decimal widening: target precision must be ≥ source precision AND
 *       target scale must be ≥ source scale (no truncation in either
 *       part).</li>
 * </ul>
 *
 * <p>Cross-kind promotions ({@code INT → PACKED}, etc.) are intentionally
 * NOT permitted in V1. See {@code smart-completion/theory.md} §8 decision #1
 * — promoting them requires picking semantics that affect every inspection.
 *
 * <p>For struct / procedure / array types: identity only (handled by
 * {@code equals} on the type object). No structural compatibility yet.
 */
public final class BbkAssignability {

    private BbkAssignability() {}

    public static boolean areCompatible(@NotNull BbkType source, @NotNull BbkType target) {
        if (source == target) return true;
        if (source instanceof BbkUnknownType || target instanceof BbkUnknownType) return true;
        if (source.equals(target)) return true;

        if (source instanceof BbkScalarType s && target instanceof BbkScalarType t) {
            return scalarCompatible(s, t);
        }

        // Different kinds (struct vs scalar, etc.) — incompatible.
        return false;
    }

    private static boolean scalarCompatible(@NotNull BbkScalarType source, @NotNull BbkScalarType target) {
        BbkScalarType.Kind sk = source.getKind();
        BbkScalarType.Kind tk = target.getKind();

        // Generic literal widening — source has null length, target is a concrete kind in the same family.
        if (source.isGeneric()) {
            return isLiteralCompatibleWith(sk, tk);
        }

        // Beyond literals, kinds must match exactly. No INT↔PACKED promotion in V1.
        if (sk != tk) return false;

        // Parameter-less kinds (BOOL, DATE, ...) — identity already handled, equal kinds match.
        if (source.getLength() == null && target.getLength() == null) return true;
        if (target.getLength() == null) return false;  // target is concrete-less, source isn't — odd, reject
        if (source.getLength() == null) return true;   // shouldn't happen (covered by isGeneric)

        // Same kind, both sized.
        switch (sk) {
            case INT, UNS, CHAR, VARCHAR -> {
                return source.getLength() <= target.getLength();
            }
            case PACKED, ZONED, BINDEC -> {
                int sP = source.getLength();
                int sD = source.getDecimals() != null ? source.getDecimals() : 0;
                int tP = target.getLength();
                int tD = target.getDecimals() != null ? target.getDecimals() : 0;
                // Neither integer part nor fractional part may shrink.
                int sIntDigits = sP - sD;
                int tIntDigits = tP - tD;
                return sIntDigits <= tIntDigits && sD <= tD;
            }
            case FLOAT, BOOL, DATE, TIME, TIMESTAMP, POINTER, VOID -> {
                return true;  // no precision distinctions
            }
        }
        return false;
    }

    private static boolean isLiteralCompatibleWith(@NotNull BbkScalarType.Kind literalKind,
                                                    @NotNull BbkScalarType.Kind target) {
        return switch (literalKind) {
            case INT -> target == BbkScalarType.Kind.INT
                || target == BbkScalarType.Kind.UNS
                || target == BbkScalarType.Kind.PACKED
                || target == BbkScalarType.Kind.ZONED
                || target == BbkScalarType.Kind.BINDEC
                || target == BbkScalarType.Kind.FLOAT;
            case PACKED -> target == BbkScalarType.Kind.PACKED
                || target == BbkScalarType.Kind.ZONED
                || target == BbkScalarType.Kind.BINDEC
                || target == BbkScalarType.Kind.FLOAT;
            case CHAR -> target == BbkScalarType.Kind.CHAR
                || target == BbkScalarType.Kind.VARCHAR;
            case FLOAT -> target == BbkScalarType.Kind.FLOAT;
            default -> false;
        };
    }
}
