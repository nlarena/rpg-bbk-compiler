package com.larena.boxbreaker.core.ast;

import java.util.List;

/**
 * A BBK expression (grammar L4 precedence chain). Sealed: the compiler
 * back-ends {@code switch} over every kind exhaustively.
 */
public sealed interface BbkExpr
        permits BbkExpr.Identifier, BbkExpr.Literal, BbkExpr.BoolLit, BbkExpr.NullLit,
                BbkExpr.StarIdent, BbkExpr.Unary, BbkExpr.Binary, BbkExpr.Ternary,
                BbkExpr.Call, BbkExpr.Index, BbkExpr.Member {

    /** A bare name: {@code counter}. */
    record Identifier(String name) implements BbkExpr {}

    /** A literal (int/hex/oct/float/dec/string); text preserved verbatim. */
    record Literal(LitKind kind, String text) implements BbkExpr {}

    /** {@code true} / {@code false}. */
    record BoolLit(boolean value) implements BbkExpr {}

    /** {@code null}. */
    record NullLit() implements BbkExpr {}

    /** A star-identifier: {@code *INLR}, {@code *BLANKS}. */
    record StarIdent(String name) implements BbkExpr {}

    /** Unary operation: {@code +x}, {@code -x}, {@code !x}, {@code ~x}. */
    record Unary(UnOp op, BbkExpr operand) implements BbkExpr {}

    /** Binary operation: {@code left op right}. */
    record Binary(BbkExpr left, BinOp op, BbkExpr right) implements BbkExpr {}

    /** Ternary: {@code cond ? then : otherwise}. */
    record Ternary(BbkExpr condition, BbkExpr then, BbkExpr otherwise) implements BbkExpr {}

    /** Call: {@code target(args)} (procedure / builtin). */
    record Call(BbkExpr target, List<BbkExpr> args) implements BbkExpr {
        public Call { args = List.copyOf(args); }
    }

    /** Array subscript: {@code target[indices]}. */
    record Index(BbkExpr target, List<BbkExpr> indices) implements BbkExpr {
        public Index { indices = List.copyOf(indices); }
    }

    /** Member access: {@code target.field} or {@code target->field}. */
    record Member(BbkExpr target, String field, boolean pointer) implements BbkExpr {}

    // ----- enums -----

    enum LitKind { INT, HEX, OCT, FLOAT, DEC, STRING }

    enum UnOp { POS, NEG, NOT, BIT_NOT }

    enum BinOp {
        ADD, SUB, MUL, DIV, MOD, POW,           // arithmetic ( + - * / % ** )
        EQ, NE, LT, GT, LE, GE,                 // comparison
        AND, OR,                                // logical ( && || )
        BIT_AND, BIT_OR, BIT_XOR, SHL, SHR      // bitwise ( & | ^ << >> )
    }
}
