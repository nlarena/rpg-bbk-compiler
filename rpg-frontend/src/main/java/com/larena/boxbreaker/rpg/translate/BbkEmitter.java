package com.larena.boxbreaker.rpg.translate;

import com.larena.boxbreaker.rpg.ast.*;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Translates an RPG AST into BBK source text. Covers the full free-form
 * declaration, statement and expression surface.
 *
 * <p>Key translations RPG &rarr; BBK:
 * <ul>
 *   <li>type names upper-cased ({@code int(10)} &rarr; {@code INT(10)}; {@code ind} &rarr; {@code BOOL})</li>
 *   <li>comparison {@code =} &rarr; {@code ==}, {@code <>} &rarr; {@code !=}; logical {@code and/or/not} &rarr; {@code && || !}</li>
 *   <li>{@code if/dow/dou/for/select/monitor} &rarr; brace blocks ({@code while}, {@code do/while}, {@code for(...)}, ...)</li>
 *   <li>{@code dcl-pi}/{@code dcl-parm} params folded into BBK inline param lists</li>
 *   <li>character literal {@code 'a''b'} &rarr; {@code "a'b"}; decimal {@code 1.5} &rarr; {@code 1.5d}</li>
 *   <li>figuratives {@code *ON/*OFF/*NULL} &rarr; {@code true/false/null}; arg separator {@code :} &rarr; {@code ,}</li>
 * </ul>
 */
public final class BbkEmitter {

    private final StringBuilder out = new StringBuilder();
    private int indent = 0;

    public static String emit(RpgProgram program) {
        BbkEmitter e = new BbkEmitter();
        for (RpgItem item : program.items()) e.emitItem(item);
        return e.out.toString();
    }

    /** Emits the BBK for a single top-level item (used by the translation debugger). */
    public static String emit(RpgItem item) {
        BbkEmitter e = new BbkEmitter();
        e.emitItem(item);
        return e.out.toString();
    }

    private void emitItem(RpgItem item) {
        if (item instanceof RpgDeclaration d) emitDeclaration(d);
        else if (item instanceof RpgStatement s) emitStatement(s);
    }

    // =======================================================================
    // Declarations
    // =======================================================================

    private void emitDeclaration(RpgDeclaration d) {
        switch (d) {
            case RpgDeclaration.Variable v ->
                line("DCL-S " + v.name() + " " + type(v.type()) + modifiers(v.keywords()) + ";");
            case RpgDeclaration.Constant c ->
                line("DCL-C " + c.name() + " " + expr(c.value()) + ";");
            case RpgDeclaration.File f ->
                line("DCL-F " + f.name() + modifiers(f.keywords()) + ";");
            case RpgDeclaration.CtlOpt o ->
                line("CTL-OPT" + modifiers(o.keywords()) + ";");
            case RpgDeclaration.DataStructure ds -> emitDataStructure(ds);
            case RpgDeclaration.Prototype pr ->
                line("DCL-PR " + pr.name() + params(pr.params()) + ret(pr.returnType())
                    + modifiers(pr.keywords()) + ";");
            case RpgDeclaration.Procedure proc -> emitProcedure(proc);
            // appear only nested; emitted by their owners
            case RpgDeclaration.Subfield sf ->
                line(sf.name() + " " + type(sf.type()) + modifiers(sf.keywords()) + ";");
            case RpgDeclaration.ProcInterface pi -> { /* folded into Procedure */ }
            case RpgDeclaration.Parameter p -> { /* folded into param lists */ }
        }
    }

    private void emitDataStructure(RpgDeclaration.DataStructure ds) {
        String head = "DCL-DS " + ds.name() + modifiers(ds.keywords());
        if (ds.subfields().isEmpty()) {
            line(head + ";");
            return;
        }
        line(head + " {");
        indent++;
        for (RpgDeclaration.Subfield sf : ds.subfields()) emitDeclaration(sf);
        indent--;
        line("}");
    }

    private void emitProcedure(RpgDeclaration.Procedure proc) {
        List<RpgDeclaration.Parameter> ps = proc.pi() != null ? proc.pi().params() : List.of();
        RpgType retType = proc.pi() != null ? proc.pi().returnType() : null;
        line("DCL-PROC " + proc.name() + params(ps) + ret(retType) + modifiers(proc.keywords()) + " {");
        indent++;
        for (RpgItem item : proc.body()) emitItem(item);
        indent--;
        line("}");
    }

    /** BBK inline parameter list: {@code (name TYPE mods, ...)}; empty list omits the parens. */
    private String params(List<RpgDeclaration.Parameter> ps) {
        if (ps.isEmpty()) return "";
        return "(" + ps.stream()
            .map(p -> p.name() + " " + type(p.type()) + modifiers(p.keywords()))
            .collect(Collectors.joining(", ")) + ")";
    }

    private String ret(RpgType returnType) {
        return returnType == null ? "" : " -> " + type(returnType);
    }

    private String modifiers(List<RpgKeyword> keywords) {
        StringBuilder sb = new StringBuilder();
        for (RpgKeyword k : keywords) {
            sb.append(' ').append(k.name().toUpperCase(Locale.ROOT));
            if (!k.args().isEmpty()) {
                sb.append('(').append(k.args().stream().map(this::expr)
                    .collect(Collectors.joining(", "))).append(')');
            }
        }
        return sb.toString();
    }

    // =======================================================================
    // Statements
    // =======================================================================

    private void emitStatement(RpgStatement s) {
        switch (s) {
            case RpgStatement.Assignment a ->
                line(expr(a.target()) + " = " + expr(a.value()) + ";");
            case RpgStatement.If f -> emitIf(f);
            case RpgStatement.Select sel -> emitSelect(sel);
            case RpgStatement.Dow d -> { line("while (" + expr(d.condition()) + ") {"); block(d.body()); line("}"); }
            case RpgStatement.Dou d -> { line("do {"); block(d.body()); line("} while (!(" + expr(d.condition()) + "));"); }
            case RpgStatement.For f -> emitFor(f);
            case RpgStatement.Monitor m -> emitMonitor(m);
            case RpgStatement.Subroutine sr -> {
                line("begsr " + sr.name() + ";"); block(sr.body()); line("endsr;");
            }
            case RpgStatement.Return r ->
                line(r.value() == null ? "return;" : "return " + expr(r.value()) + ";");
            case RpgStatement.Leave l -> line("break;");
            case RpgStatement.Iter i -> line("continue;");
            case RpgStatement.Leavesr l -> line("leavesr;");
            case RpgStatement.Op o -> line(op(o));
            case RpgStatement.ExprStatement es -> line(expr(es.expr()) + ";");
            case RpgStatement.Directive dir -> line("// " + dir.text());
        }
    }

    private void emitIf(RpgStatement.If f) {
        line("if (" + expr(f.condition()) + ") {");
        block(f.thenBody());
        for (RpgStatement.ElseIf elif : f.elseIfs()) {
            line("} else if (" + expr(elif.condition()) + ") {");
            block(elif.body());
        }
        if (!f.elseBody().isEmpty()) {
            line("} else {");
            block(f.elseBody());
        }
        line("}");
    }

    private void emitSelect(RpgStatement.Select sel) {
        line("select {");
        indent++;
        for (RpgStatement.WhenClause w : sel.whens()) {
            line("when (" + expr(w.condition()) + ") {");
            block(w.body());
            line("}");
        }
        if (!sel.otherBody().isEmpty()) {
            line("other {");
            block(sel.otherBody());
            line("}");
        }
        indent--;
        line("}");
    }

    private void emitFor(RpgStatement.For f) {
        boolean up = f.dir() == RpgStatement.ForDir.TO;
        String cmp = up ? " <= " : " >= ";
        String step = f.by() != null ? expr(f.by()) : "1";
        String update = f.var() + (up ? " += " : " -= ") + step;
        line("for (" + f.var() + " = " + expr(f.from()) + "; "
            + f.var() + cmp + expr(f.to()) + "; " + update + ") {");
        block(f.body());
        line("}");
    }

    private void emitMonitor(RpgStatement.Monitor m) {
        line("monitor {");
        block(m.body());
        for (RpgStatement.OnError oe : m.onErrors()) {
            String statuses = oe.statusList().isEmpty() ? ""
                : " (" + oe.statusList().stream().map(this::expr).collect(Collectors.joining(", ")) + ")";
            line("} on-error" + statuses + " {");
            block(oe.body());
        }
        if (!m.onExit().isEmpty()) {
            line("} on-exit {");
            block(m.onExit());
        }
        line("}");
    }

    /** A file op / generic opcode: lowercase opcode + space-separated operands. */
    private String op(RpgStatement.Op o) {
        String operands = o.operands().stream().map(this::expr).collect(Collectors.joining(" "));
        return o.opcode().toLowerCase(Locale.ROOT) + (operands.isEmpty() ? "" : " " + operands) + ";";
    }

    private void block(List<RpgStatement> statements) {
        indent++;
        for (RpgStatement s : statements) emitStatement(s);
        indent--;
    }

    // =======================================================================
    // Types
    // =======================================================================

    private String type(RpgType t) {
        return switch (t) {
            case RpgType.Scalar s -> scalar(s);
            case RpgType.Like l -> "LIKE(" + l.name() + ")";
            case RpgType.LikeDs l -> "LIKEDS(" + l.name() + ")";
            case RpgType.LikeRec l -> "LIKEREC(" + l.recName() + ")";
        };
    }

    private String scalar(RpgType.Scalar s) {
        String name = mapTypeName(s.name());
        if (s.length() == null) return name;
        if (s.decimals() == null) return name + "(" + s.length() + ")";
        return name + "(" + s.length() + ":" + s.decimals() + ")";
    }

    private static String mapTypeName(String rpg) {
        String upper = rpg.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "IND" -> "BOOL";
            default -> upper;
        };
    }

    // =======================================================================
    // Expressions
    // =======================================================================

    private String expr(RpgExpr e) {
        return switch (e) {
            case RpgExpr.Identifier id -> id.name();
            case RpgExpr.Literal lit -> literal(lit);
            case RpgExpr.Figurative fig -> figurative(fig.name());
            case RpgExpr.IndicatorRef ind -> ind.name();
            case RpgExpr.Member m -> expr(m.target()) + "." + m.field();
            case RpgExpr.Call c -> expr(c.target()) + "(" + argList(c.args()) + ")";
            case RpgExpr.BifCall b -> bifName(b.name()) + "(" + argList(b.args()) + ")";
            case RpgExpr.Unary u -> unary(u);
            case RpgExpr.Binary b -> binary(b);
        };
    }

    private String argList(List<RpgExpr> args) {
        return args.stream().map(this::expr).collect(Collectors.joining(", "));
    }

    /** RPG BIF name ({@code %trim}) → BBK builtin name ({@code trim}); BBK has no '%' prefix. */
    private static String bifName(String rpgBif) {
        String n = (rpgBif.startsWith("%") ? rpgBif.substring(1) : rpgBif).toLowerCase(Locale.ROOT);
        return switch (n) {
            case "subst" -> "substr";   // RPG %SUBST → BBK substr
            default -> n;
        };
    }

    private String unary(RpgExpr.Unary u) {
        String op = switch (u.op()) { case NOT -> "!"; case NEG -> "-"; case POS -> "+"; };
        String operand = expr(u.operand());
        if (u.operand() instanceof RpgExpr.Binary) operand = "(" + operand + ")";
        return op + operand;
    }

    private String binary(RpgExpr.Binary b) {
        return operand(b.left(), b.op(), false) + " " + binOp(b.op()) + " " + operand(b.right(), b.op(), true);
    }

    private String operand(RpgExpr child, RpgExpr.BinOp parentOp, boolean isRight) {
        String s = expr(child);
        if (child instanceof RpgExpr.Binary cb && needsParens(cb.op(), parentOp, isRight)) {
            return "(" + s + ")";
        }
        return s;
    }

    private static boolean needsParens(RpgExpr.BinOp child, RpgExpr.BinOp parent, boolean isRight) {
        int c = prec(child), p = prec(parent);
        if (c < p) return true;
        if (c > p) return false;
        boolean rightAssoc = parent == RpgExpr.BinOp.POW;
        return isRight != rightAssoc;
    }

    private static int prec(RpgExpr.BinOp op) {
        return switch (op) {
            case OR -> 1;
            case AND, XOR -> 2;
            case EQ, NE, LT, GT, LE, GE -> 3;
            case ADD, SUB -> 4;
            case MUL, DIV -> 5;
            case POW -> 6;
        };
    }

    private static String binOp(RpgExpr.BinOp op) {
        return switch (op) {
            case ADD -> "+"; case SUB -> "-"; case MUL -> "*"; case DIV -> "/"; case POW -> "**";
            case EQ -> "=="; case NE -> "!="; case LT -> "<"; case GT -> ">"; case LE -> "<="; case GE -> ">=";
            case AND -> "&&"; case OR -> "||"; case XOR -> "^";
        };
    }

    private static String literal(RpgExpr.Literal lit) {
        return switch (lit.kind()) {
            case INT -> lit.text();
            case DEC -> lit.text() + "d";
            case STRING -> rpgStringToBbk(lit.text());
            default -> lit.text();
        };
    }

    private static String rpgStringToBbk(String rpg) {
        String inner = rpg.length() >= 2 ? rpg.substring(1, rpg.length() - 1) : "";
        inner = inner.replace("''", "'").replace("\"", "\\\"");
        return "\"" + inner + "\"";
    }

    private static String figurative(String name) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "*ON" -> "true";
            case "*OFF" -> "false";
            case "*NULL" -> "null";
            default -> name;
        };
    }

    // =======================================================================
    // Output
    // =======================================================================

    private void line(String text) {
        out.append("  ".repeat(indent)).append(text).append('\n');
    }
}
