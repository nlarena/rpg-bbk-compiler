package com.larena.boxbreaker.core.ast;

/**
 * A BBK type specification (grammar {@code type_specification}): a primitive
 * type with optional size args, or a {@code LIKE} / {@code LIKEDS} /
 * {@code LIKEREC} reference.
 */
public sealed interface BbkType permits BbkType.Primitive, BbkType.Like {

    /** {@code INT}, {@code INT(10)}, {@code PACKED(11:2)}, {@code DATE}, {@code BOOL}. */
    record Primitive(String name, Integer length, Integer decimals) implements BbkType {}

    /** {@code LIKE(x)} / {@code LIKEDS(x)} / {@code LIKEREC(x)}. */
    record Like(LikeKind kind, String name) implements BbkType {}

    enum LikeKind { LIKE, LIKEDS, LIKEREC }

    static Primitive prim(String name) { return new Primitive(name, null, null); }
    static Primitive prim(String name, int length) { return new Primitive(name, length, null); }
    static Primitive prim(String name, int length, int decimals) { return new Primitive(name, length, decimals); }
}
