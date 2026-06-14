package com.larena.boxbreaker.rpg.ast;

import java.util.List;

/**
 * A type specification (grammar §4.2.1 {@code <type-spec>}): either a scalar
 * type with optional size, or a {@code LIKE} / {@code LIKEDS} / {@code LIKEREC}
 * reference to another declaration's shape.
 *
 * <p>The base name is stored as written; the BBK emitter upper-cases it
 * ({@code int} &rarr; {@code INT}).
 */
public sealed interface RpgType
        permits RpgType.Scalar, RpgType.Like, RpgType.LikeDs, RpgType.LikeRec {

    /** {@code INT}, {@code INT(10)}, {@code PACKED(11:2)}, {@code CHAR(50)}, {@code DATE}. */
    record Scalar(String name, Integer length, Integer decimals) implements RpgType {}

    /** {@code LIKE(otherVar)} — same shape as another variable. */
    record Like(String name) implements RpgType {}

    /** {@code LIKEDS(otherDs)} — same shape as a data structure. */
    record LikeDs(String name) implements RpgType {}

    /** {@code LIKEREC(record : part...)} — same shape as a record format. */
    record LikeRec(String recName, List<String> parts) implements RpgType {
        public LikeRec { parts = List.copyOf(parts); }
    }

    // ----- convenience builders for scalars -----
    static Scalar scalar(String name) { return new Scalar(name, null, null); }
    static Scalar scalar(String name, int length) { return new Scalar(name, length, null); }
    static Scalar scalar(String name, int length, int decimals) { return new Scalar(name, length, decimals); }
}
