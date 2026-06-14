package com.larena.boxbreaker.rpg.ast;

import java.util.List;

/**
 * An RPG expression (grammar §4.4). Sealed: the BBK emitter {@code switch}es
 * over every kind and the compiler guarantees no case is forgotten.
 */
public sealed interface RpgExpr
        permits RpgExpr.Identifier, RpgExpr.Literal, RpgExpr.Figurative,
                RpgExpr.IndicatorRef, RpgExpr.Binary, RpgExpr.Unary,
                RpgExpr.Call, RpgExpr.BifCall, RpgExpr.Member {

    /** A bare name: {@code counter}, {@code customerId}. */
    record Identifier(String name) implements RpgExpr {}

    /** A literal of any kind (§2.6); text preserved verbatim. */
    record Literal(LiteralKind kind, String text) implements RpgExpr {}

    /** A figurative constant: {@code *ON}, {@code *OFF}, {@code *BLANKS}, {@code *NULL}. */
    record Figurative(String name) implements RpgExpr {}

    /** An indicator reference: {@code *IN01}, {@code *INLR}. */
    record IndicatorRef(String name) implements RpgExpr {}

    /** A binary operation: {@code left op right}. */
    record Binary(RpgExpr left, BinOp op, RpgExpr right) implements RpgExpr {}

    /** A unary operation: {@code op operand}. */
    record Unary(UnOp op, RpgExpr operand) implements RpgExpr {}

    /**
     * A parenthesised application {@code target(args)} — a procedure call or
     * an array subscript. RPG uses {@code ( )} for both; which one it is can
     * only be decided with type information, so disambiguation is deferred to a
     * later typed phase (the emitter then picks BBK {@code ()} vs {@code []}).
     */
    record Call(RpgExpr target, List<RpgExpr> args) implements RpgExpr {
        public Call { args = List.copyOf(args); }
    }

    /** A built-in function call: {@code %trim(name)}, {@code %subst(s : 1 : 3)}. */
    record BifCall(String name, List<RpgExpr> args) implements RpgExpr {
        public BifCall { args = List.copyOf(args); }
    }

    /** Qualified data-structure access: {@code customer.id}. */
    record Member(RpgExpr target, String field) implements RpgExpr {}

    // ----- enums -----

    /** Literal categories (§2.6). */
    enum LiteralKind {
        INT, DEC, STRING, HEX, DATE, TIME, TIMESTAMP, UCS2, GRAPHIC
    }

    /** Binary operators (§2.7 / §4.4). */
    enum BinOp {
        ADD, SUB, MUL, DIV, POW,            // arithmetic ( + - * / ** )
        EQ, NE, LT, GT, LE, GE,             // comparison
        AND, OR, XOR                        // logical
    }

    /** Unary operators. */
    enum UnOp {
        NOT,    // logical not
        NEG,    // arithmetic negation (-x)
        POS     // arithmetic plus (+x)
    }
}
