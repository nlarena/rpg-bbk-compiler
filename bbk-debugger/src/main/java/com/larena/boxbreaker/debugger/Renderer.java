package com.larena.boxbreaker.debugger;

import com.larena.boxbreaker.core.ast.BbkExpr;
import com.larena.boxbreaker.core.ast.BbkStatement;
import com.larena.boxbreaker.core.ast.BbkStatement.AssignOp;
import com.larena.boxbreaker.core.ast.BbkExpr.BinOp;
import com.larena.boxbreaker.core.ast.BbkExpr.UnOp;

import java.util.stream.Collectors;

/** Renderiza sentencias y expresiones de BBK a una línea de texto (para la traza). */
final class Renderer {

    private Renderer() {}

    static String stmt(BbkStatement s) {
        return switch (s) {
            case BbkStatement.ExpressionStatement e -> expr(e.expr());
            case BbkStatement.Assignment a -> expr(a.target()) + " " + assignOp(a.op()) + " " + expr(a.value())
                + (a.attr() == BbkStatement.AttrMod.NONE ? "" : " @" + a.attr().name().toLowerCase());
            case BbkStatement.If i -> "if (" + expr(i.condition()) + ")";
            case BbkStatement.Select ignored -> "select";
            case BbkStatement.While w -> "while (" + expr(w.condition()) + ")";
            case BbkStatement.DoWhile d -> "do-while (" + expr(d.condition()) + ")";
            case BbkStatement.For f -> "for (...; " + (f.condition() == null ? "" : expr(f.condition())) + "; ...)";
            case BbkStatement.Break ignored -> "break";
            case BbkStatement.Continue ignored -> "continue";
            case BbkStatement.Return r -> "return" + (r.value() == null ? "" : " " + expr(r.value()));
            case BbkStatement.Subroutine sr -> "BEGSR " + sr.name();
            case BbkStatement.Exsr x -> "EXSR " + x.name();
            case BbkStatement.Leavesr ignored -> "LEAVESR";
            case BbkStatement.Callp c -> "CALLP " + expr(c.expr());
            case BbkStatement.Monitor ignored -> "monitor";
            case BbkStatement.FileOp f -> f.opcode() + " " + f.operands().stream().map(Renderer::expr).collect(Collectors.joining(" "));
            case BbkStatement.Directive d -> d.keyword();
        };
    }

    static String expr(BbkExpr e) {
        return switch (e) {
            case BbkExpr.Identifier id -> id.name();
            case BbkExpr.Literal l -> l.text();
            case BbkExpr.BoolLit b -> String.valueOf(b.value());
            case BbkExpr.NullLit ignored -> "null";
            case BbkExpr.StarIdent s -> "*" + s.name();
            case BbkExpr.Unary u -> unOp(u.op()) + expr(u.operand());
            case BbkExpr.Binary b -> expr(b.left()) + " " + binOp(b.op()) + " " + expr(b.right());
            case BbkExpr.Ternary t -> expr(t.condition()) + " ? " + expr(t.then()) + " : " + expr(t.otherwise());
            case BbkExpr.Call c -> expr(c.target()) + "(" + c.args().stream().map(Renderer::expr).collect(Collectors.joining(", ")) + ")";
            case BbkExpr.Index i -> expr(i.target()) + "[" + i.indices().stream().map(Renderer::expr).collect(Collectors.joining(", ")) + "]";
            case BbkExpr.Member m -> expr(m.target()) + (m.pointer() ? "->" : ".") + m.field();
        };
    }

    private static String assignOp(AssignOp op) {
        return switch (op) {
            case ASSIGN -> "=";
            case ADD -> "+="; case SUB -> "-="; case MUL -> "*="; case DIV -> "/="; case MOD -> "%=";
            case AND -> "&="; case OR -> "|="; case XOR -> "^="; case SHL -> "<<="; case SHR -> ">>=";
        };
    }

    private static String binOp(BinOp op) {
        return switch (op) {
            case ADD -> "+"; case SUB -> "-"; case MUL -> "*"; case DIV -> "/"; case MOD -> "%"; case POW -> "**";
            case EQ -> "=="; case NE -> "!="; case LT -> "<"; case GT -> ">"; case LE -> "<="; case GE -> ">=";
            case AND -> "&&"; case OR -> "||";
            case BIT_AND -> "&"; case BIT_OR -> "|"; case BIT_XOR -> "^"; case SHL -> "<<"; case SHR -> ">>";
        };
    }

    private static String unOp(UnOp op) {
        return switch (op) {
            case POS -> "+"; case NEG -> "-"; case NOT -> "!"; case BIT_NOT -> "~";
        };
    }
}
