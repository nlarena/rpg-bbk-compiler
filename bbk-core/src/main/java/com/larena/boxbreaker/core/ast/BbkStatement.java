package com.larena.boxbreaker.core.ast;

import java.util.List;

/**
 * A BBK statement (grammar L4 + L5). Sealed; an {@link BbkItem} like
 * {@link BbkDeclaration}. Block bodies are {@code List<BbkItem>} (they may hold
 * nested declarations); control-flow inner blocks are {@code List<BbkItem>} too
 * since BBK blocks allow declarations.
 */
public sealed interface BbkStatement extends BbkItem
        permits BbkStatement.ExpressionStatement, BbkStatement.Assignment,
                BbkStatement.If, BbkStatement.Select, BbkStatement.While,
                BbkStatement.DoWhile, BbkStatement.For, BbkStatement.Break,
                BbkStatement.Continue, BbkStatement.Return, BbkStatement.Monitor,
                BbkStatement.Subroutine, BbkStatement.Exsr, BbkStatement.Leavesr,
                BbkStatement.Callp, BbkStatement.FileOp, BbkStatement.Directive {

    /** A bare expression used as a statement: {@code f(x);}. */
    record ExpressionStatement(BbkExpr expr) implements BbkStatement {}

    /** {@code target op value [@attr];} — assignment with optional half-adjust attribute. */
    record Assignment(BbkExpr target, AssignOp op, BbkExpr value, AttrMod attr) implements BbkStatement {}

    /** {@code if (c) { ... } [else { ... } | else if ...]} — elseBody may hold a nested {@code If}. */
    record If(BbkExpr condition, List<BbkItem> thenBody, List<BbkItem> elseBody) implements BbkStatement {
        public If {
            thenBody = List.copyOf(thenBody);
            elseBody = List.copyOf(elseBody);
        }
    }

    /** {@code select { when (c) { ... } other { ... } }} */
    record Select(List<When> whens, List<BbkItem> otherBody) implements BbkStatement {
        public Select {
            whens = List.copyOf(whens);
            otherBody = List.copyOf(otherBody);
        }
    }

    /** {@code while (c) { ... }} */
    record While(BbkExpr condition, List<BbkItem> body) implements BbkStatement {
        public While { body = List.copyOf(body); }
    }

    /** {@code do { ... } while (c);} */
    record DoWhile(List<BbkItem> body, BbkExpr condition) implements BbkStatement {
        public DoWhile { body = List.copyOf(body); }
    }

    /**
     * {@code for (init; condition; update) { ... }} — C-style.
     *
     * @param init   an inline {@code DCL-S}, an {@link Assignment}, or an
     *               {@link ExpressionStatement}; null if omitted
     * @param update an {@link Assignment} or {@link ExpressionStatement}; null if omitted
     */
    record For(BbkItem init, BbkExpr condition, BbkStatement update,
               List<BbkItem> body) implements BbkStatement {
        public For { body = List.copyOf(body); }
    }

    /** {@code break;} */
    record Break() implements BbkStatement {}

    /** {@code continue;} */
    record Continue() implements BbkStatement {}

    /** {@code return [value];} — value null for a bare return. */
    record Return(BbkExpr value) implements BbkStatement {}

    /** {@code monitor { ... } on-error (s...) { ... } on-exit { ... }} */
    record Monitor(List<BbkItem> body, List<OnError> onErrors,
                   List<BbkItem> onExit) implements BbkStatement {
        public Monitor {
            body = List.copyOf(body);
            onErrors = List.copyOf(onErrors);
            onExit = List.copyOf(onExit);
        }
    }

    /** {@code BEGSR name; ... ENDSR;} */
    record Subroutine(String name, List<BbkItem> body) implements BbkStatement {
        public Subroutine { body = List.copyOf(body); }
    }

    /** {@code EXSR name;} */
    record Exsr(String name) implements BbkStatement {}

    /** {@code LEAVESR;} */
    record Leavesr() implements BbkStatement {}

    /** {@code CALLP expr;} */
    record Callp(BbkExpr expr) implements BbkStatement {}

    /** A file operation: {@code read file ds;}, {@code chain key file;}, ... */
    record FileOp(String opcode, List<BbkExpr> operands) implements BbkStatement {
        public FileOp { operands = List.copyOf(operands); }
    }

    /** A preprocessor directive: {@code PRE-IF expr}, {@code PRE-INCLUDE "f"}, ... */
    record Directive(String keyword, List<BbkExpr> args) implements BbkStatement {
        public Directive { args = List.copyOf(args); }
    }

    // ----- helper records -----

    /** One {@code when (c) { body }} arm of a {@link Select}. */
    record When(BbkExpr condition, List<BbkItem> body) {
        public When { body = List.copyOf(body); }
    }

    /** One {@code on-error (statuses) { body }} arm of a {@link Monitor}. */
    record OnError(List<BbkExpr> statusList, List<BbkItem> body) {
        public OnError {
            statusList = List.copyOf(statusList);
            body = List.copyOf(body);
        }
    }

    // ----- enums -----

    /** Assignment operators (grammar {@code assignment_op}). */
    enum AssignOp {
        ASSIGN, ADD, SUB, MUL, DIV, MOD, AND, OR, XOR, SHL, SHR
    }

    /** Attribute modifiers on an assignment (grammar {@code attribute_modifier}). */
    enum AttrMod { NONE, HALFUP, HALFDOWN, TRUNC }
}
