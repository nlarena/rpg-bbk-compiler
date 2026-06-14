package com.larena.boxbreaker.core.backend.jvm;

import com.larena.boxbreaker.core.ast.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * Compiles a BBK AST to JVM bytecode (a {@code bbk/Main} class with a
 * {@code main} method), so BBK can run on the JVM — including inside the IDE.
 *
 * <p>This first back-end covers the imperative integer/boolean/string core:
 * {@code INT}&rarr;{@code long}, {@code BOOL}&rarr;{@code boolean},
 * {@code CHAR/VARCHAR}&rarr;{@code String}; arithmetic, comparison, logical and
 * bitwise expressions; {@code if/while/do-while/for}, {@code break/continue},
 * assignment (incl. compound) and {@code print(...)}. Constructs not yet
 * lowered (decimals/float, procedures, data structures, file ops, ...) raise a
 * clear {@link UnsupportedOperationException} — extended as needed.
 */
public final class JvmCompiler {

    /** JVM-level type a BBK value lowers to in this back-end. */
    enum Jt { LONG, BOOL, STRING }

    private record Var(int slot, Jt type) {}
    private record Loop(Label continueLabel, Label breakLabel) {}

    public static final String CLASS_NAME = "bbk.Main";
    private static final String INTERNAL_NAME = "bbk/Main";

    private MethodVisitor mv;
    private final Map<String, Var> vars = new HashMap<>();
    private final Deque<Loop> loops = new ArrayDeque<>();
    private int nextSlot = 1;   // slot 0 = String[] args

    /** Compile a program to the bytes of the {@code bbk.Main} class. */
    public static byte[] compile(BbkProgram program) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, INTERNAL_NAME, null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main",
            "([Ljava/lang/String;)V", null, null);
        mv.visitCode();

        JvmCompiler c = new JvmCompiler();
        c.mv = mv;
        for (BbkItem item : program.items()) c.item(item);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);   // ignored: COMPUTE_FRAMES
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    // -----------------------------------------------------------------------
    // Items / statements
    // -----------------------------------------------------------------------

    private void item(BbkItem item) {
        if (item instanceof BbkDeclaration.Variable v) declareVariable(v);
        else if (item instanceof BbkStatement s) statement(s);
        else throw unsupported(item.getClass().getSimpleName());
    }

    private void declareVariable(BbkDeclaration.Variable v) {
        Jt jt = jt(v.type());
        Var var = declare(v.name(), jt);
        BbkExpr init = inzValue(v.modifiers());
        if (init != null) {
            coerce(expr(init), jt);
        } else {
            pushDefault(jt);
        }
        store(var);
    }

    private void statement(BbkStatement s) {
        switch (s) {
            case BbkStatement.ExpressionStatement es -> exprStatement(es.expr());
            case BbkStatement.Assignment a -> assignment(a);
            case BbkStatement.If f -> ifStmt(f);
            case BbkStatement.While w -> whileStmt(w);
            case BbkStatement.DoWhile d -> doWhile(d);
            case BbkStatement.For f -> forStmt(f);
            case BbkStatement.Break b -> jump(loops.peek().breakLabel());
            case BbkStatement.Continue c -> jump(loops.peek().continueLabel());
            case BbkStatement.Return r -> mv.visitInsn(RETURN);   // main is void
            default -> throw unsupported(s.getClass().getSimpleName());
        }
    }

    private void exprStatement(BbkExpr e) {
        if (e instanceof BbkExpr.Call call
            && call.target() instanceof BbkExpr.Identifier id && id.name().equals("print")
            && call.args().size() == 1) {
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            Jt jt = expr(call.args().get(0));
            String desc = switch (jt) {
                case LONG -> "(J)V";
                case BOOL -> "(Z)V";
                case STRING -> "(Ljava/lang/String;)V";
            };
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", desc, false);
            return;
        }
        Jt jt = expr(e);
        mv.visitInsn(jt == Jt.LONG ? POP2 : POP);
    }

    private void assignment(BbkStatement.Assignment a) {
        if (!(a.target() instanceof BbkExpr.Identifier id)) {
            throw unsupported("assignment to " + a.target().getClass().getSimpleName());
        }
        if (a.op() == BbkStatement.AssignOp.ASSIGN) {
            Jt valueJt = expr(a.value());
            Var var = vars.containsKey(id.name()) ? vars.get(id.name()) : declare(id.name(), valueJt);
            coerceTop(valueJt, var.type());
            store(var);
        } else {
            Var var = require(id.name());
            if (var.type() != Jt.LONG) throw unsupported("compound assignment on " + var.type());
            load(var);
            coerce(expr(a.value()), Jt.LONG);
            mv.visitInsn(longArithOp(compoundToBin(a.op())));
            store(var);
        }
    }

    private void ifStmt(BbkStatement.If f) {
        Label elseL = new Label(), end = new Label();
        condition(f.condition());
        mv.visitJumpInsn(IFEQ, f.elseBody().isEmpty() ? end : elseL);
        block(f.thenBody());
        if (!f.elseBody().isEmpty()) {
            jump(end);
            mv.visitLabel(elseL);
            block(f.elseBody());
        }
        mv.visitLabel(end);
    }

    private void whileStmt(BbkStatement.While w) {
        Label start = new Label(), end = new Label();
        loops.push(new Loop(start, end));
        mv.visitLabel(start);
        condition(w.condition());
        mv.visitJumpInsn(IFEQ, end);
        block(w.body());
        jump(start);
        mv.visitLabel(end);
        loops.pop();
    }

    private void doWhile(BbkStatement.DoWhile d) {
        Label start = new Label(), cont = new Label(), end = new Label();
        loops.push(new Loop(cont, end));
        mv.visitLabel(start);
        block(d.body());
        mv.visitLabel(cont);
        condition(d.condition());
        mv.visitJumpInsn(IFNE, start);
        mv.visitLabel(end);
        loops.pop();
    }

    private void forStmt(BbkStatement.For f) {
        if (f.init() != null) item(f.init());
        Label start = new Label(), cont = new Label(), end = new Label();
        loops.push(new Loop(cont, end));
        mv.visitLabel(start);
        if (f.condition() != null) {
            condition(f.condition());
            mv.visitJumpInsn(IFEQ, end);
        }
        block(f.body());
        mv.visitLabel(cont);
        if (f.update() != null) statement(f.update());
        jump(start);
        mv.visitLabel(end);
        loops.pop();
    }

    private void block(List<BbkItem> items) {
        for (BbkItem i : items) item(i);
    }

    /** Compiles an expression expected to yield a boolean (int 0/1) on the stack. */
    private void condition(BbkExpr e) {
        coerceTop(expr(e), Jt.BOOL);
    }

    // -----------------------------------------------------------------------
    // Expressions — leave a value on the stack, return its Jt
    // -----------------------------------------------------------------------

    private Jt expr(BbkExpr e) {
        return switch (e) {
            case BbkExpr.Identifier id -> { Var v = require(id.name()); load(v); yield v.type(); }
            case BbkExpr.Literal lit -> literal(lit);
            case BbkExpr.BoolLit b -> { mv.visitInsn(b.value() ? ICONST_1 : ICONST_0); yield Jt.BOOL; }
            case BbkExpr.Unary u -> unary(u);
            case BbkExpr.Binary b -> binary(b);
            case BbkExpr.Ternary t -> ternary(t);
            default -> throw unsupported("expression " + e.getClass().getSimpleName());
        };
    }

    private Jt literal(BbkExpr.Literal lit) {
        switch (lit.kind()) {
            case INT -> { mv.visitLdcInsn(Long.parseLong(lit.text())); return Jt.LONG; }
            case HEX -> { mv.visitLdcInsn(Long.parseLong(lit.text().substring(2), 16)); return Jt.LONG; }
            case OCT -> { mv.visitLdcInsn(Long.parseLong(lit.text().substring(2), 8)); return Jt.LONG; }
            case STRING -> { mv.visitLdcInsn(unescape(lit.text())); return Jt.STRING; }
            default -> throw unsupported("literal " + lit.kind());
        }
    }

    private Jt unary(BbkExpr.Unary u) {
        switch (u.op()) {
            case NEG -> { coerce(expr(u.operand()), Jt.LONG); mv.visitInsn(LNEG); return Jt.LONG; }
            case POS -> { return coerce(expr(u.operand()), Jt.LONG); }
            case NOT -> { coerceTop(expr(u.operand()), Jt.BOOL); mv.visitInsn(ICONST_1); mv.visitInsn(IXOR); return Jt.BOOL; }
            case BIT_NOT -> { coerce(expr(u.operand()), Jt.LONG); mv.visitLdcInsn(-1L); mv.visitInsn(LXOR); return Jt.LONG; }
            default -> throw unsupported("unary " + u.op());
        }
    }

    private Jt binary(BbkExpr.Binary b) {
        return switch (b.op()) {
            case ADD, SUB, MUL, DIV, MOD -> {
                coerce(expr(b.left()), Jt.LONG);
                coerce(expr(b.right()), Jt.LONG);
                mv.visitInsn(longArithOp(b.op()));
                yield Jt.LONG;
            }
            case BIT_AND, BIT_OR, BIT_XOR -> {
                coerce(expr(b.left()), Jt.LONG);
                coerce(expr(b.right()), Jt.LONG);
                mv.visitInsn(b.op() == BbkExpr.BinOp.BIT_AND ? LAND
                    : b.op() == BbkExpr.BinOp.BIT_OR ? LOR : LXOR);
                yield Jt.LONG;
            }
            case SHL, SHR -> {
                coerce(expr(b.left()), Jt.LONG);
                coerce(expr(b.right()), Jt.LONG);
                mv.visitInsn(L2I);   // shift count is an int
                mv.visitInsn(b.op() == BbkExpr.BinOp.SHL ? LSHL : LSHR);
                yield Jt.LONG;
            }
            case EQ, NE, LT, GT, LE, GE -> comparison(b);
            case AND, OR -> {
                coerceTop(expr(b.left()), Jt.BOOL);
                coerceTop(expr(b.right()), Jt.BOOL);
                mv.visitInsn(b.op() == BbkExpr.BinOp.AND ? IAND : IOR);
                yield Jt.BOOL;
            }
            default -> throw unsupported("binary " + b.op());
        };
    }

    private Jt comparison(BbkExpr.Binary b) {
        coerce(expr(b.left()), Jt.LONG);
        coerce(expr(b.right()), Jt.LONG);
        mv.visitInsn(LCMP);                 // -1 / 0 / 1
        int ifOp = switch (b.op()) {
            case EQ -> IFEQ; case NE -> IFNE; case LT -> IFLT;
            case GT -> IFGT; case LE -> IFLE; default -> IFGE;
        };
        Label t = new Label(), end = new Label();
        mv.visitJumpInsn(ifOp, t);
        mv.visitInsn(ICONST_0);
        jump(end);
        mv.visitLabel(t);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(end);
        return Jt.BOOL;
    }

    private Jt ternary(BbkExpr.Ternary t) {
        Label elseL = new Label(), end = new Label();
        condition(t.condition());
        mv.visitJumpInsn(IFEQ, elseL);
        Jt thenJt = expr(t.then());
        jump(end);
        mv.visitLabel(elseL);
        Jt elseJt = expr(t.otherwise());
        mv.visitLabel(end);
        if (thenJt != elseJt) throw unsupported("ternary branches of different types");
        return thenJt;
    }

    // -----------------------------------------------------------------------
    // Locals, loads/stores, coercion
    // -----------------------------------------------------------------------

    private Var declare(String name, Jt jt) {
        Var v = new Var(nextSlot, jt);
        nextSlot += (jt == Jt.LONG) ? 2 : 1;
        vars.put(name, v);
        return v;
    }

    private Var require(String name) {
        Var v = vars.get(name);
        if (v == null) throw unsupported("undeclared variable '" + name + "'");
        return v;
    }

    private void load(Var v) {
        mv.visitVarInsn(switch (v.type()) { case LONG -> LLOAD; case BOOL -> ILOAD; case STRING -> ALOAD; }, v.slot());
    }

    private void store(Var v) {
        mv.visitVarInsn(switch (v.type()) { case LONG -> LSTORE; case BOOL -> ISTORE; case STRING -> ASTORE; }, v.slot());
    }

    private void pushDefault(Jt jt) {
        switch (jt) {
            case LONG -> mv.visitInsn(LCONST_0);
            case BOOL -> mv.visitInsn(ICONST_0);
            case STRING -> mv.visitLdcInsn("");
        }
    }

    /** Coerce the value just produced (with the given Jt) to the wanted Jt; returns wanted. */
    private Jt coerce(Jt have, Jt want) {
        coerceTop(have, want);
        return want;
    }

    private void coerceTop(Jt have, Jt want) {
        if (have == want) return;
        throw unsupported("implicit conversion " + have + " -> " + want);
    }

    private static int longArithOp(BbkExpr.BinOp op) {
        return switch (op) {
            case ADD -> LADD; case SUB -> LSUB; case MUL -> LMUL;
            case DIV -> LDIV; case MOD -> LREM;
            default -> throw new IllegalArgumentException("not a long arith op: " + op);
        };
    }

    private static BbkExpr.BinOp compoundToBin(BbkStatement.AssignOp op) {
        return switch (op) {
            case ADD -> BbkExpr.BinOp.ADD; case SUB -> BbkExpr.BinOp.SUB;
            case MUL -> BbkExpr.BinOp.MUL; case DIV -> BbkExpr.BinOp.DIV;
            case MOD -> BbkExpr.BinOp.MOD;
            default -> throw new UnsupportedOperationException("compound op " + op + " not supported");
        };
    }

    private static BbkExpr inzValue(List<BbkModifier> modifiers) {
        for (BbkModifier m : modifiers) {
            if (m.name().equals("INZ") && m.args().size() == 1) return m.args().get(0);
        }
        return null;
    }

    private void jump(Label l) { mv.visitJumpInsn(GOTO, l); }

    private static Jt jt(BbkType type) {
        if (type instanceof BbkType.Primitive p) {
            return switch (p.name()) {
                case "INT", "UNS" -> Jt.LONG;
                case "BOOL" -> Jt.BOOL;
                case "CHAR", "VARCHAR" -> Jt.STRING;
                default -> throw new UnsupportedOperationException(
                    "type '" + p.name() + "' not supported by the bytecode back-end yet");
            };
        }
        throw new UnsupportedOperationException("LIKE types not supported by the bytecode back-end yet");
    }

    /** Convert a BBK string literal {@code "..."} to its runtime value. */
    private static String unescape(String lit) {
        String body = lit.length() >= 2 ? lit.substring(1, lit.length() - 1) : "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '\\' && i + 1 < body.length()) {
                char n = body.charAt(++i);
                sb.append(switch (n) { case 'n' -> '\n'; case 't' -> '\t'; case 'r' -> '\r'; default -> n; });
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static UnsupportedOperationException unsupported(String what) {
        return new UnsupportedOperationException(what + " is not supported by the bytecode back-end yet");
    }
}
