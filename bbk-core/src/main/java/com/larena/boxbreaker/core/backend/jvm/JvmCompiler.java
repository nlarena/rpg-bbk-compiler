package com.larena.boxbreaker.core.backend.jvm;

import com.larena.boxbreaker.core.ast.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
 * Compiles a BBK AST to JVM bytecode (a {@code bbk/Main} class), so BBK runs on
 * the JVM — including in-process inside the IDE.
 *
 * <p>Lowers the language <em>except</em> the IBM&nbsp;i operating-system surface
 * (files {@code DCL-F}/{@code FileOp}, external {@code EXTPGM}/{@code EXTPROC}
 * calls — DB2 / native I/O / program calls):
 *
 * <ul>
 *   <li><b>Scalars</b> — {@code INT/UNS}&rarr;{@code long},
 *       {@code FLOAT}&rarr;{@code double}, {@code PACKED/ZONED/BINDEC}&rarr;
 *       {@code BigDecimal} (exact, with declared scale), {@code BOOL/IND}&rarr;
 *       {@code boolean}, {@code CHAR/VARCHAR}&rarr;{@code String}. Numeric
 *       promotion {@code long &rarr; double &rarr; BigDecimal}.</li>
 *   <li><b>Operators</b> — all arithmetic incl. {@code **}, comparison (numeric
 *       and string), logical, bitwise/shift, {@code +} string concatenation;
 *       {@code @halfup/@halfdown/@trunc} rounding on assignment.</li>
 *   <li><b>Control flow</b> — {@code if/select/while/do-while/for},
 *       {@code break/continue/return}, {@code monitor/on-error/on-exit}.</li>
 *   <li><b>Constants</b> ({@code DCL-C}), <b>special values</b>
 *       ({@code *zero/*blank/...}).</li>
 *   <li><b>Arrays</b> ({@code DIM}) &rarr; JVM arrays, 0-based subscripts —
 *       including arrays of data structures ({@code emp[i].field}).</li>
 *   <li><b>Data structures</b> ({@code DCL-DS}, {@code TEMPLATE},
 *       {@code LIKEDS}) with scalar subfields.</li>
 *   <li><b>Procedures</b> ({@code DCL-PROC}) &rarr; static methods, calls &rarr;
 *       {@code INVOKESTATIC}, value parameters + return value; {@code CTL-OPT
 *       MAIN(p)} designates the entry point.</li>
 *   <li><b>Subroutines</b> ({@code BEGSR/EXSR/LEAVESR}) &rarr; inlined.</li>
 *   <li><b>Builtins</b> (pure) — {@code len/substr/scan/trim/triml/trimr/
 *       lower/upper/replace/char/int/dec/float/sqrt/abs}.</li>
 * </ul>
 *
 * <p>Not lowered (raise a clear {@link UnsupportedOperationException}):
 * {@code OVERLAY} (memory aliasing), date/time types, pointers, and the OS
 * surface above.
 */
public final class JvmCompiler {

    /** JVM-level kind a scalar BBK value lowers to. */
    enum Jt { LONG, DOUBLE, BOOL, STRING, DECIMAL }

    /** A storage location: static field (global) or local slot; scalar or array; decimal scale. */
    private record Binding(boolean global, int slot, String field, Jt jt, boolean array, int dim, int scale) {
        static Binding global(String field, Jt jt, boolean array, int dim, int scale) { return new Binding(true, -1, field, jt, array, dim, scale); }
        static Binding local(int slot, Jt jt, boolean array, int dim, int scale) { return new Binding(false, slot, null, jt, array, dim, scale); }
    }

    private record Sig(List<Jt> paramTypes, List<Boolean> paramArray, Jt ret) {}
    private record Loop(Label continueLabel, Label breakLabel) {}
    private record FieldDef(String name, String desc) {}

    public static final String CLASS_NAME = "bbk.Main";
    private static final String INTERNAL = "bbk/Main";
    private static final String SB = "java/lang/StringBuilder";
    private static final String STR = "java/lang/String";
    private static final String BIGDEC = "java/math/BigDecimal";
    private static final String ROUND = "java/math/RoundingMode";

    private ClassWriter cw;
    private MethodVisitor mv;

    // Module scope.
    private final Map<String, Binding> globals = new LinkedHashMap<>();
    private final Map<String, BbkExpr> constants = new HashMap<>();
    private final Map<String, Sig> procs = new HashMap<>();
    private final Map<String, List<BbkDeclaration.Subfield>> dsTemplates = new HashMap<>();
    private final List<FieldDef> fieldDefs = new ArrayList<>();
    private String mainProc;

    // Per-method state.
    private Map<String, Binding> locals;
    private Deque<Loop> loops;
    private Deque<Label> leavesr;
    private Map<String, List<BbkItem>> subroutines;
    private Set<String> activeSubs;
    private int nextSlot;
    private Jt currentReturn;

    public static byte[] compile(BbkProgram program) {
        return new JvmCompiler().build(program);
    }

    private byte[] build(BbkProgram program) {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, INTERNAL, null, "java/lang/Object", null);

        for (BbkItem item : program.items()) {
            switch (item) {
                case BbkDeclaration.Procedure p -> procs.put(p.name(), signatureOf(p));
                case BbkDeclaration.Constant c -> constants.put(c.name(), c.value());
                case BbkDeclaration.DataStructure d -> registerDs(d.name(), d.subfields(), d.modifiers(), true);
                case BbkDeclaration.Variable v -> registerGlobalVar(v);
                case BbkDeclaration.CtlOpt ctl -> { String m = mainOf(ctl); if (m != null) mainProc = m; }
                default -> { }
            }
        }
        for (FieldDef f : fieldDefs) cw.visitField(ACC_PRIVATE | ACC_STATIC, f.name(), f.desc(), null, null).visitEnd();

        emitMain(program.items());
        for (BbkItem item : program.items()) {
            if (item instanceof BbkDeclaration.Procedure p) emitProcedure(p);
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    // -----------------------------------------------------------------------
    // Methods (main + procedures)
    // -----------------------------------------------------------------------

    private void emitMain(List<BbkItem> items) {
        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        startMethod(1, null);
        collectSubroutines(items);
        for (BbkItem item : items) {
            switch (item) {
                case BbkDeclaration.Variable v -> initGlobalVar(v);
                case BbkDeclaration.DataStructure d -> initGlobalDs(d);
                case BbkDeclaration ignored -> { }
                case BbkStatement s -> { if (mainProc == null) statement(s); }
            }
        }
        if (mainProc != null) {
            Sig sig = procs.get(mainProc);
            if (sig == null) throw unsupported("CTL-OPT MAIN names unknown procedure '" + mainProc + "'");
            mv.visitMethodInsn(INVOKESTATIC, INTERNAL, mainProc, descriptor(sig), false);
            if (sig.ret() != null) mv.visitInsn(wide(sig.ret()) ? POP2 : POP);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitProcedure(BbkDeclaration.Procedure p) {
        Sig sig = procs.get(p.name());
        mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, p.name(), descriptor(sig), null, null);
        mv.visitCode();
        startMethod(0, sig.ret());
        for (int i = 0; i < p.params().size(); i++) {
            BbkDeclaration.Parameter par = p.params().get(i);
            bindParam(par.name(), sig.paramTypes().get(i), sig.paramArray().get(i), scaleOf(par.type()));
        }
        collectSubroutines(p.body());
        for (BbkItem item : p.body()) bodyItem(item);
        if (sig.ret() == null) mv.visitInsn(RETURN);
        else { pushDefaultScalar(sig.ret()); returnValue(sig.ret()); }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void startMethod(int firstSlot, Jt returnType) {
        locals = new HashMap<>();
        loops = new ArrayDeque<>();
        leavesr = new ArrayDeque<>();
        subroutines = new HashMap<>();
        activeSubs = new HashSet<>();
        nextSlot = firstSlot;
        currentReturn = returnType;
    }

    private void collectSubroutines(List<BbkItem> items) {
        for (BbkItem item : items) {
            if (item instanceof BbkStatement.Subroutine s) subroutines.put(s.name(), s.body());
        }
    }

    // -----------------------------------------------------------------------
    // Items / statements
    // -----------------------------------------------------------------------

    private void bodyItem(BbkItem item) {
        switch (item) {
            case BbkDeclaration.Variable v -> declareLocalVar(v);
            case BbkDeclaration.DataStructure d -> { registerDs(d.name(), d.subfields(), d.modifiers(), false); initLocalDs(d); }
            case BbkDeclaration.Constant c -> constants.put(c.name(), c.value());
            case BbkDeclaration ignored -> { }
            case BbkStatement s -> statement(s);
        }
    }

    private void statement(BbkStatement s) {
        switch (s) {
            case BbkStatement.ExpressionStatement es -> exprStatement(es.expr());
            case BbkStatement.Assignment a -> assignment(a);
            case BbkStatement.If f -> ifStmt(f);
            case BbkStatement.Select sel -> select(sel);
            case BbkStatement.While w -> whileStmt(w);
            case BbkStatement.DoWhile d -> doWhile(d);
            case BbkStatement.For f -> forStmt(f);
            case BbkStatement.Break b -> jump(loops.peek().breakLabel());
            case BbkStatement.Continue c -> jump(loops.peek().continueLabel());
            case BbkStatement.Return r -> returnStmt(r);
            case BbkStatement.Monitor m -> monitor(m);
            case BbkStatement.Subroutine ignored -> { }
            case BbkStatement.Exsr e -> exsr(e);
            case BbkStatement.Leavesr l -> jump(leavesrTarget());
            case BbkStatement.Callp cp -> callpStmt(cp);
            case BbkStatement.Directive ignored -> { }
            case BbkStatement.FileOp f -> throw unsupported("file operation '" + f.opcode() + "' (IBM i I/O)");
        }
    }

    private void exprStatement(BbkExpr e) {
        if (e instanceof BbkExpr.Call c && isName(c.target(), "print") && c.args().size() == 1) { print(c.args().get(0)); return; }
        if (e instanceof BbkExpr.Call c) {
            Jt jt = call(c);
            if (jt != null) mv.visitInsn(wide(jt) ? POP2 : POP);
            return;
        }
        Jt jt = expr(e);
        mv.visitInsn(wide(jt) ? POP2 : POP);
    }

    private void print(BbkExpr arg) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        Jt jt = expr(arg);
        String desc;
        switch (jt) {
            case LONG -> desc = "(J)V";
            case DOUBLE -> desc = "(D)V";
            case BOOL -> desc = "(Z)V";
            case STRING -> desc = "(Ljava/lang/String;)V";
            case DECIMAL -> { mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "toPlainString", "()Ljava/lang/String;", false); desc = "(Ljava/lang/String;)V"; }
            default -> throw unsupported("print of " + jt);
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", desc, false);
    }

    private void callpStmt(BbkStatement.Callp cp) {
        if (cp.expr() instanceof BbkExpr.Call c) {
            Jt jt = call(c);
            if (jt != null) mv.visitInsn(wide(jt) ? POP2 : POP);
        } else exprStatement(cp.expr());
    }

    private void returnStmt(BbkStatement.Return r) {
        if (r.value() == null || currentReturn == null) { mv.visitInsn(RETURN); return; }
        coerce(expr(r.value()), currentReturn);
        returnValue(currentReturn);
    }

    private void returnValue(Jt jt) {
        mv.visitInsn(switch (jt) { case LONG -> LRETURN; case DOUBLE -> DRETURN; case BOOL -> IRETURN; case STRING, DECIMAL -> ARETURN; });
    }

    private void ifStmt(BbkStatement.If f) {
        Label elseL = new Label(), end = new Label();
        condition(f.condition());
        mv.visitJumpInsn(IFEQ, f.elseBody().isEmpty() ? end : elseL);
        block(f.thenBody());
        if (!f.elseBody().isEmpty()) { jump(end); mv.visitLabel(elseL); block(f.elseBody()); }
        mv.visitLabel(end);
    }

    private void select(BbkStatement.Select sel) {
        Label end = new Label();
        for (BbkStatement.When w : sel.whens()) {
            Label next = new Label();
            condition(w.condition());
            mv.visitJumpInsn(IFEQ, next);
            block(w.body());
            jump(end);
            mv.visitLabel(next);
        }
        block(sel.otherBody());
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
        if (f.init() != null) bodyItem(f.init());
        Label start = new Label(), cont = new Label(), end = new Label();
        loops.push(new Loop(cont, end));
        mv.visitLabel(start);
        if (f.condition() != null) { condition(f.condition()); mv.visitJumpInsn(IFEQ, end); }
        block(f.body());
        mv.visitLabel(cont);
        if (f.update() != null) statement(f.update());
        jump(start);
        mv.visitLabel(end);
        loops.pop();
    }

    private void monitor(BbkStatement.Monitor m) {
        if (m.body().isEmpty()) { block(m.onExit()); return; }
        Label start = new Label(), endTry = new Label(), handler = new Label(), out = new Label();
        mv.visitTryCatchBlock(start, endTry, handler, "java/lang/Throwable");
        mv.visitLabel(start);
        block(m.body());
        mv.visitLabel(endTry);
        block(m.onExit());
        jump(out);
        mv.visitLabel(handler);
        mv.visitInsn(POP);
        if (!m.onErrors().isEmpty()) block(m.onErrors().get(0).body());
        block(m.onExit());
        mv.visitLabel(out);
    }

    private void exsr(BbkStatement.Exsr e) {
        List<BbkItem> body = subroutines.get(e.name());
        if (body == null) throw unsupported("call to undefined subroutine '" + e.name() + "'");
        if (!activeSubs.add(e.name())) throw unsupported("recursive subroutine '" + e.name() + "' (cannot inline)");
        Label endsr = new Label();
        leavesr.push(endsr);
        block(body);
        mv.visitLabel(endsr);
        leavesr.pop();
        activeSubs.remove(e.name());
    }

    private Label leavesrTarget() {
        if (leavesr.isEmpty()) throw unsupported("LEAVESR outside a subroutine");
        return leavesr.peek();
    }

    private void block(List<BbkItem> items) { for (BbkItem i : items) bodyItem(i); }

    private void condition(BbkExpr e) { coerceTop(expr(e), Jt.BOOL); }

    // -----------------------------------------------------------------------
    // Assignment
    // -----------------------------------------------------------------------

    private void assignment(BbkStatement.Assignment a) {
        BbkExpr target = a.target();
        if (target instanceof BbkExpr.Index idx) { assignArrayElem(arrayBinding(idx), idx.indices().get(0), a); return; }
        if (target instanceof BbkExpr.Member mem && mem.target() instanceof BbkExpr.Index ix) {
            assignArrayElem(dsArrayField(ix, mem.field()), ix.indices().get(0), a); return;
        }
        Binding b = scalarTarget(target, a);
        if (a.op() == BbkStatement.AssignOp.ASSIGN) {
            coerce(expr(a.value()), b.jt());
        } else {
            loadVar(b);
            Jt result = binaryOp(b.jt(), compoundToBin(a.op()), a.value());
            coerceTop(result, b.jt());
        }
        applyScale(b, a.attr());
        storeVar(b);
    }

    private Binding scalarTarget(BbkExpr target, BbkStatement.Assignment a) {
        if (target instanceof BbkExpr.Identifier id) {
            Binding b = lookup(id.name());
            if (b != null) return b;
            if (a.op() != BbkStatement.AssignOp.ASSIGN) throw unsupported("compound assignment to undeclared '" + id.name() + "'");
            return bindParam(id.name(), typeOf(a.value()), false, -1);
        }
        if (target instanceof BbkExpr.Member mem) {
            Binding b = lookup(memberKey(mem));
            if (b == null) throw unsupported("assignment to unknown member '" + memberKey(mem) + "'");
            if (b.array()) throw unsupported("DS-array element requires an index: " + memberKey(mem) + "[i]");
            return b;
        }
        throw unsupported("assignment to " + target.getClass().getSimpleName());
    }

    private void assignArrayElem(Binding arr, BbkExpr indexExpr, BbkStatement.Assignment a) {
        if (a.op() == BbkStatement.AssignOp.ASSIGN) {
            loadVar(arr);
            coerceIndex(indexExpr);
            coerce(expr(a.value()), arr.jt());
            applyScale(arr, a.attr());
            mv.visitInsn(arrayStore(arr.jt()));
        } else {
            loadVar(arr);
            coerceIndex(indexExpr);
            mv.visitInsn(DUP2);
            mv.visitInsn(arrayLoad(arr.jt()));
            Jt result = binaryOp(arr.jt(), compoundToBin(a.op()), a.value());
            coerceTop(result, arr.jt());
            applyScale(arr, a.attr());
            mv.visitInsn(arrayStore(arr.jt()));
        }
    }

    // -----------------------------------------------------------------------
    // Expressions
    // -----------------------------------------------------------------------

    private Jt expr(BbkExpr e) {
        return switch (e) {
            case BbkExpr.Identifier id -> identifier(id);
            case BbkExpr.Literal lit -> literal(lit);
            case BbkExpr.BoolLit b -> { mv.visitInsn(b.value() ? ICONST_1 : ICONST_0); yield Jt.BOOL; }
            case BbkExpr.NullLit n -> { mv.visitInsn(ACONST_NULL); yield Jt.STRING; }
            case BbkExpr.StarIdent s -> starValue(s.name());
            case BbkExpr.Unary u -> unary(u);
            case BbkExpr.Binary b -> binary(b);
            case BbkExpr.Ternary t -> ternary(t);
            case BbkExpr.Call c -> { Jt jt = call(c); if (jt == null) throw unsupported("void call used as a value"); yield jt; }
            case BbkExpr.Index ix -> index(ix);
            case BbkExpr.Member m -> member(m);
        };
    }

    private Jt identifier(BbkExpr.Identifier id) {
        Binding b = lookup(id.name());
        if (b != null) {
            if (b.array()) throw unsupported("array '" + id.name() + "' used as a scalar value");
            loadVar(b);
            return b.jt();
        }
        BbkExpr c = constants.get(id.name());
        if (c != null) return expr(c);
        throw unsupported("undeclared name '" + id.name() + "'");
    }

    private Jt literal(BbkExpr.Literal lit) {
        switch (lit.kind()) {
            case INT -> { mv.visitLdcInsn(Long.parseLong(lit.text())); return Jt.LONG; }
            case HEX -> { mv.visitLdcInsn(Long.parseLong(lit.text().substring(2), 16)); return Jt.LONG; }
            case OCT -> { mv.visitLdcInsn(Long.parseLong(lit.text().substring(2), 8)); return Jt.LONG; }
            case FLOAT -> { mv.visitLdcInsn(Double.parseDouble(lit.text())); return Jt.DOUBLE; }
            case DEC -> { newDecimal(stripDecSuffix(lit.text())); return Jt.DECIMAL; }
            case STRING -> { mv.visitLdcInsn(unescape(lit.text())); return Jt.STRING; }
            default -> throw unsupported("literal " + lit.kind());
        }
    }

    private Jt unary(BbkExpr.Unary u) {
        switch (u.op()) {
            case NEG -> {
                Jt t = expr(u.operand()); requireNumeric(t);
                switch (t) {
                    case LONG -> mv.visitInsn(LNEG);
                    case DOUBLE -> mv.visitInsn(DNEG);
                    case DECIMAL -> mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "negate", "()Ljava/math/BigDecimal;", false);
                    default -> { }
                }
                return t;
            }
            case POS -> { return expr(u.operand()); }
            case NOT -> { coerceTop(expr(u.operand()), Jt.BOOL); mv.visitInsn(ICONST_1); mv.visitInsn(IXOR); return Jt.BOOL; }
            case BIT_NOT -> { coerce(expr(u.operand()), Jt.LONG); mv.visitLdcInsn(-1L); mv.visitInsn(LXOR); return Jt.LONG; }
            default -> throw unsupported("unary " + u.op());
        }
    }

    private Jt binary(BbkExpr.Binary b) {
        return switch (b.op()) {
            case AND, OR -> {
                coerceTop(expr(b.left()), Jt.BOOL);
                coerceTop(expr(b.right()), Jt.BOOL);
                mv.visitInsn(b.op() == BbkExpr.BinOp.AND ? IAND : IOR);
                yield Jt.BOOL;
            }
            case EQ, NE, LT, GT, LE, GE -> comparison(b);
            default -> binaryOp(expr(b.left()), b.op(), b.right());
        };
    }

    /** Emit {@code <left already on stack as leftJt> OP <right>}. Shared by binary() and compound assignment. */
    private Jt binaryOp(Jt leftJt, BbkExpr.BinOp op, BbkExpr rightExpr) {
        switch (op) {
            case ADD:
                if (leftJt == Jt.STRING || typeOf(rightExpr) == Jt.STRING) return concat(leftJt, rightExpr);
                return numericBinary(leftJt, op, rightExpr);
            case SUB: case MUL: case DIV: case MOD:
                return numericBinary(leftJt, op, rightExpr);
            case POW:
                coerceTop(leftJt, Jt.DOUBLE);
                coerce(expr(rightExpr), Jt.DOUBLE);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
                return Jt.DOUBLE;
            case BIT_AND: case BIT_OR: case BIT_XOR:
                coerceTop(leftJt, Jt.LONG);
                coerce(expr(rightExpr), Jt.LONG);
                mv.visitInsn(op == BbkExpr.BinOp.BIT_AND ? LAND : op == BbkExpr.BinOp.BIT_OR ? LOR : LXOR);
                return Jt.LONG;
            case SHL: case SHR:
                coerceTop(leftJt, Jt.LONG);
                coerce(expr(rightExpr), Jt.LONG);
                mv.visitInsn(L2I);
                mv.visitInsn(op == BbkExpr.BinOp.SHL ? LSHL : LSHR);
                return Jt.LONG;
            default:
                throw unsupported("operator " + op);
        }
    }

    private Jt numericBinary(Jt leftJt, BbkExpr.BinOp op, BbkExpr rightExpr) {
        Jt result = wider(leftJt, typeOf(rightExpr));
        coerceTop(leftJt, result);
        coerce(expr(rightExpr), result);
        if (result == Jt.DECIMAL) decimalArith(op);
        else mv.visitInsn(arithOp(op, result));
        return result;
    }

    private void decimalArith(BbkExpr.BinOp op) {
        switch (op) {
            case ADD -> mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "add", "(Ljava/math/BigDecimal;)Ljava/math/BigDecimal;", false);
            case SUB -> mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "subtract", "(Ljava/math/BigDecimal;)Ljava/math/BigDecimal;", false);
            case MUL -> mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "multiply", "(Ljava/math/BigDecimal;)Ljava/math/BigDecimal;", false);
            case MOD -> mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "remainder", "(Ljava/math/BigDecimal;)Ljava/math/BigDecimal;", false);
            case DIV -> {
                mv.visitFieldInsn(GETSTATIC, "java/math/MathContext", "DECIMAL128", "Ljava/math/MathContext;");
                mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "divide", "(Ljava/math/BigDecimal;Ljava/math/MathContext;)Ljava/math/BigDecimal;", false);
            }
            default -> throw unsupported("decimal operator " + op);
        }
    }

    private Jt concat(Jt leftJt, BbkExpr rightExpr) {
        leftToString(leftJt);
        mv.visitTypeInsn(NEW, SB);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, SB, "<init>", "()V", false);
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, SB, "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        Jt rt = expr(rightExpr);
        appendValue(rt);
        mv.visitMethodInsn(INVOKEVIRTUAL, SB, "toString", "()Ljava/lang/String;", false);
        return Jt.STRING;
    }

    private void leftToString(Jt jt) {
        switch (jt) {
            case STRING -> { }
            case LONG -> mv.visitMethodInsn(INVOKESTATIC, STR, "valueOf", "(J)Ljava/lang/String;", false);
            case DOUBLE -> mv.visitMethodInsn(INVOKESTATIC, STR, "valueOf", "(D)Ljava/lang/String;", false);
            case BOOL -> mv.visitMethodInsn(INVOKESTATIC, STR, "valueOf", "(Z)Ljava/lang/String;", false);
            case DECIMAL -> mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "toPlainString", "()Ljava/lang/String;", false);
        }
    }

    private void appendValue(Jt jt) {
        if (jt == Jt.DECIMAL) { mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "toPlainString", "()Ljava/lang/String;", false); jt = Jt.STRING; }
        String desc = switch (jt) {
            case STRING -> "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
            case LONG -> "(J)Ljava/lang/StringBuilder;";
            case DOUBLE -> "(D)Ljava/lang/StringBuilder;";
            case BOOL -> "(Z)Ljava/lang/StringBuilder;";
            default -> throw unsupported("append of " + jt);
        };
        mv.visitMethodInsn(INVOKEVIRTUAL, SB, "append", desc, false);
    }

    private Jt comparison(BbkExpr.Binary b) {
        Jt lt = typeOf(b.left()), rt = typeOf(b.right());
        if (lt == Jt.STRING || rt == Jt.STRING) {
            if (lt != Jt.STRING || rt != Jt.STRING) throw unsupported("cannot compare " + lt + " and " + rt);
            if (b.op() != BbkExpr.BinOp.EQ && b.op() != BbkExpr.BinOp.NE) throw unsupported("ordering comparison on strings");
            expr(b.left());
            expr(b.right());
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
            if (b.op() == BbkExpr.BinOp.NE) { mv.visitInsn(ICONST_1); mv.visitInsn(IXOR); }
            return Jt.BOOL;
        }
        if (lt == Jt.BOOL && rt == Jt.BOOL) {
            expr(b.left());
            expr(b.right());
            branchBool(switch (b.op()) { case EQ -> IF_ICMPEQ; case NE -> IF_ICMPNE; default -> throw unsupported("ordering comparison on booleans"); });
            return Jt.BOOL;
        }
        Jt common = wider(lt, rt);
        coerce(expr(b.left()), common);
        coerce(expr(b.right()), common);
        switch (common) {
            case DECIMAL -> mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "compareTo", "(Ljava/math/BigDecimal;)I", false);
            case DOUBLE -> mv.visitInsn(DCMPG);
            default -> mv.visitInsn(LCMP);
        }
        branchBool(switch (b.op()) { case EQ -> IFEQ; case NE -> IFNE; case LT -> IFLT; case GT -> IFGT; case LE -> IFLE; default -> IFGE; });
        return Jt.BOOL;
    }

    /** Turn a pending conditional jump into a 0/1 boolean on the stack. */
    private void branchBool(int ifOp) {
        Label t = new Label(), end = new Label();
        mv.visitJumpInsn(ifOp, t);
        mv.visitInsn(ICONST_0);
        jump(end);
        mv.visitLabel(t);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(end);
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
        if (thenJt != elseJt) throw unsupported("ternary branches of different types (" + thenJt + " vs " + elseJt + ")");
        return thenJt;
    }

    private Jt index(BbkExpr.Index ix) {
        Binding arr = arrayBinding(ix);
        loadVar(arr);
        coerceIndex(ix.indices().get(0));
        mv.visitInsn(arrayLoad(arr.jt()));
        return arr.jt();
    }

    private Jt member(BbkExpr.Member m) {
        if (m.target() instanceof BbkExpr.Index ix) {        // DS-array element field: emp[i].field
            Binding b = dsArrayField(ix, m.field());
            loadVar(b);
            coerceIndex(ix.indices().get(0));
            mv.visitInsn(arrayLoad(b.jt()));
            return b.jt();
        }
        Binding b = lookup(memberKey(m));
        if (b == null) throw unsupported("unknown member '" + memberKey(m) + "'");
        if (b.array()) throw unsupported("DS-array element requires an index: " + memberKey(m) + "[i]");
        loadVar(b);
        return b.jt();
    }

    // -----------------------------------------------------------------------
    // Calls (procedures + pure builtins)
    // -----------------------------------------------------------------------

    private Jt call(BbkExpr.Call c) {
        if (!(c.target() instanceof BbkExpr.Identifier id)) throw unsupported("call to a non-name target");
        Sig sig = procs.get(id.name());
        if (sig != null) return invokeProcedure(id.name(), sig, c.args());
        return builtin(id.name(), c.args());
    }

    private Jt invokeProcedure(String name, Sig sig, List<BbkExpr> args) {
        if (args.size() != sig.paramTypes().size()) {
            throw unsupported("procedure '" + name + "' expects " + sig.paramTypes().size() + " args, got " + args.size());
        }
        for (int i = 0; i < args.size(); i++) {
            if (sig.paramArray().get(i)) loadVar(arrayArg(args.get(i)));
            else coerce(expr(args.get(i)), sig.paramTypes().get(i));
        }
        mv.visitMethodInsn(INVOKESTATIC, INTERNAL, name, descriptor(sig), false);
        return sig.ret();
    }

    private Jt builtin(String name, List<BbkExpr> args) {
        switch (name) {
            case "len" -> { coerceTop(expr(args.get(0)), Jt.STRING); mv.visitMethodInsn(INVOKEVIRTUAL, STR, "length", "()I", false); mv.visitInsn(I2L); return Jt.LONG; }
            case "substr" -> { return substr(args); }
            case "scan" -> { return scan(args); }
            case "trim" -> { return strMethod(args.get(0), "strip"); }
            case "triml" -> { return strMethod(args.get(0), "stripLeading"); }
            case "trimr" -> { return strMethod(args.get(0), "stripTrailing"); }
            case "lower" -> { return strMethod(args.get(0), "toLowerCase"); }
            case "upper" -> { return strMethod(args.get(0), "toUpperCase"); }
            case "replace" -> { return replace(args); }
            case "char" -> { leftToString(expr(args.get(0))); return Jt.STRING; }
            case "int" -> { return toInt(args.get(0)); }
            case "float" -> { coerce(expr(args.get(0)), Jt.DOUBLE); return Jt.DOUBLE; }
            case "dec" -> { coerce(expr(args.get(0)), Jt.DECIMAL); return Jt.DECIMAL; }
            case "sqrt" -> { coerce(expr(args.get(0)), Jt.DOUBLE); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sqrt", "(D)D", false); return Jt.DOUBLE; }
            case "abs" -> { return absOf(args.get(0)); }
            default -> throw unsupported("function '" + name + "'");
        }
    }

    private Jt substr(List<BbkExpr> args) {
        coerceTop(expr(args.get(0)), Jt.STRING);
        coerce(expr(args.get(1)), Jt.LONG); mv.visitInsn(L2I);
        mv.visitInsn(ICONST_1); mv.visitInsn(ISUB);              // RPG 1-based start
        mv.visitMethodInsn(INVOKEVIRTUAL, STR, "substring", "(I)Ljava/lang/String;", false);
        if (args.size() >= 3) {
            mv.visitInsn(ICONST_0);
            coerce(expr(args.get(2)), Jt.LONG); mv.visitInsn(L2I);
            mv.visitMethodInsn(INVOKEVIRTUAL, STR, "substring", "(II)Ljava/lang/String;", false);
        }
        return Jt.STRING;
    }

    private Jt scan(List<BbkExpr> args) {                        // scan(needle, haystack) -> 1-based pos, 0 if absent
        coerceTop(expr(args.get(1)), Jt.STRING);
        coerce(expr(args.get(0)), Jt.STRING);
        mv.visitMethodInsn(INVOKEVIRTUAL, STR, "indexOf", "(Ljava/lang/String;)I", false);
        mv.visitInsn(ICONST_1); mv.visitInsn(IADD); mv.visitInsn(I2L);
        return Jt.LONG;
    }

    private Jt replace(List<BbkExpr> args) {
        coerceTop(expr(args.get(0)), Jt.STRING);
        coerce(expr(args.get(1)), Jt.STRING);
        coerce(expr(args.get(2)), Jt.STRING);
        mv.visitMethodInsn(INVOKEVIRTUAL, STR, "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false);
        return Jt.STRING;
    }

    private Jt strMethod(BbkExpr arg, String method) {
        coerceTop(expr(arg), Jt.STRING);
        mv.visitMethodInsn(INVOKEVIRTUAL, STR, method, "()Ljava/lang/String;", false);
        return Jt.STRING;
    }

    private Jt toInt(BbkExpr arg) {
        Jt jt = expr(arg);
        if (jt == Jt.STRING) mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "parseLong", "(Ljava/lang/String;)J", false);
        else coerceTop(jt, Jt.LONG);
        return Jt.LONG;
    }

    private Jt absOf(BbkExpr arg) {
        Jt jt = expr(arg);
        switch (jt) {
            case LONG -> mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(J)J", false);
            case DOUBLE -> mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
            case DECIMAL -> mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "abs", "()Ljava/math/BigDecimal;", false);
            default -> throw unsupported("abs of " + jt);
        }
        return jt;
    }

    // -----------------------------------------------------------------------
    // Pure type inference (no emission)
    // -----------------------------------------------------------------------

    private Jt typeOf(BbkExpr e) {
        return switch (e) {
            case BbkExpr.Identifier id -> {
                Binding b = lookup(id.name());
                if (b != null) yield b.jt();
                BbkExpr c = constants.get(id.name());
                if (c != null) yield typeOf(c);
                throw unsupported("undeclared name '" + id.name() + "'");
            }
            case BbkExpr.BoolLit b -> Jt.BOOL;
            case BbkExpr.NullLit n -> Jt.STRING;
            case BbkExpr.StarIdent s -> starType(s.name());
            case BbkExpr.Literal lit -> switch (lit.kind()) {
                case INT, HEX, OCT -> Jt.LONG;
                case FLOAT -> Jt.DOUBLE;
                case DEC -> Jt.DECIMAL;
                case STRING -> Jt.STRING;
            };
            case BbkExpr.Unary u -> u.op() == BbkExpr.UnOp.NOT ? Jt.BOOL : typeOf(u.operand());
            case BbkExpr.Ternary t -> typeOf(t.then());
            case BbkExpr.Index ix -> arrayBinding(ix).jt();
            case BbkExpr.Member m -> typeOfMember(m);
            case BbkExpr.Call c -> typeOfCall(c);
            case BbkExpr.Binary b -> switch (b.op()) {
                case EQ, NE, LT, GT, LE, GE, AND, OR -> Jt.BOOL;
                case BIT_AND, BIT_OR, BIT_XOR, SHL, SHR -> Jt.LONG;
                case POW -> Jt.DOUBLE;
                case ADD -> (typeOf(b.left()) == Jt.STRING || typeOf(b.right()) == Jt.STRING) ? Jt.STRING : wider(typeOf(b.left()), typeOf(b.right()));
                default -> wider(typeOf(b.left()), typeOf(b.right()));
            };
        };
    }

    private Jt typeOfMember(BbkExpr.Member m) {
        if (m.target() instanceof BbkExpr.Index ix) return dsArrayField(ix, m.field()).jt();
        Binding b = lookup(memberKey(m));
        if (b == null) throw unsupported("unknown member '" + memberKey(m) + "'");
        return b.jt();
    }

    private Jt typeOfCall(BbkExpr.Call c) {
        if (!(c.target() instanceof BbkExpr.Identifier id)) throw unsupported("call to a non-name target");
        Sig sig = procs.get(id.name());
        if (sig != null) { if (sig.ret() == null) throw unsupported("void call used as a value"); return sig.ret(); }
        return switch (id.name()) {
            case "len", "int", "scan" -> Jt.LONG;
            case "substr", "trim", "triml", "trimr", "lower", "upper", "replace", "char" -> Jt.STRING;
            case "dec" -> Jt.DECIMAL;
            case "float", "sqrt" -> Jt.DOUBLE;
            case "abs" -> typeOf(c.args().get(0));
            default -> throw unsupported("function '" + id.name() + "'");
        };
    }

    // -----------------------------------------------------------------------
    // Declarations / bindings
    // -----------------------------------------------------------------------

    private Sig signatureOf(BbkDeclaration.Procedure p) {
        List<Jt> types = new ArrayList<>();
        List<Boolean> arrays = new ArrayList<>();
        for (BbkDeclaration.Parameter par : p.params()) {
            types.add(jt(par.type()));
            arrays.add(isArray(par.modifiers()));
        }
        Jt ret = p.returnType() == null ? null : jt(p.returnType());
        return new Sig(types, arrays, ret);
    }

    private void registerGlobalVar(BbkDeclaration.Variable v) {
        if (v.type() instanceof BbkType.Like like && like.kind() == BbkType.LikeKind.LIKEDS) {
            List<BbkDeclaration.Subfield> tpl = template(like.name());
            registerDs(v.name(), tpl, List.of(BbkModifier.bare("QUALIFIED")), true);
            dsTemplates.put(v.name(), tpl);
            return;
        }
        Jt jt = jt(v.type());
        boolean array = isArray(v.modifiers());
        int dim = array ? dimSize(v.modifiers()) : 0;
        globals.put(v.name(), Binding.global(v.name(), jt, array, dim, scaleOf(v.type())));
        fieldDefs.add(new FieldDef(v.name(), descOf(jt, array)));
    }

    private void registerDs(String dsName, List<BbkDeclaration.Subfield> subs, List<BbkModifier> mods, boolean global) {
        String like = likedsTemplate(mods);
        if (subs.isEmpty() && like != null) subs = template(like);   // DCL-DS x LIKEDS(t): expand layout
        dsTemplates.put(dsName, subs);                               // register layout for future LIKEDS
        if (hasMod(mods, "TEMPLATE")) return;                        // template: no storage
        boolean qualified = like != null || hasMod(mods, "QUALIFIED") || hasMod(mods, "QUALI");
        boolean dsArray = isArray(mods);
        int dim = dsArray ? dimSize(mods) : 0;
        for (BbkDeclaration.Subfield sub : subs) {
            if (hasMod(sub.modifiers(), "OVERLAY")) throw unsupported("OVERLAY (memory aliasing) on subfield '" + sub.name() + "'");
            if (isArray(sub.modifiers())) throw unsupported("array subfield '" + sub.name() + "' in a data structure");
            Jt jt = jt(sub.type());
            int scale = scaleOf(sub.type());
            Binding b = global
                ? Binding.global(dsName + "$" + sub.name(), jt, dsArray, dim, scale)
                : bindLocalSlot(jt, dsArray, dim, scale);
            if (global) fieldDefs.add(new FieldDef(dsName + "$" + sub.name(), descOf(jt, dsArray)));
            (global ? globals : locals).put(dsName + "." + sub.name(), b);
            if (!qualified) (global ? globals : locals).put(sub.name(), b);
        }
    }

    private void initGlobalVar(BbkDeclaration.Variable v) {
        if (v.type() instanceof BbkType.Like like && like.kind() == BbkType.LikeKind.LIKEDS) { initClone(v.name(), like.name(), true); return; }
        Binding b = globals.get(v.name());
        pushInit(b, v.modifiers());
        if (!b.array()) applyScale(b, BbkStatement.AttrMod.NONE);
        storeVar(b);
    }

    private void declareLocalVar(BbkDeclaration.Variable v) {
        if (v.type() instanceof BbkType.Like like && like.kind() == BbkType.LikeKind.LIKEDS) {
            List<BbkDeclaration.Subfield> tpl = template(like.name());
            registerDs(v.name(), tpl, List.of(BbkModifier.bare("QUALIFIED")), false);
            dsTemplates.put(v.name(), tpl);
            initClone(v.name(), like.name(), false);
            return;
        }
        Jt jt = jt(v.type());
        boolean array = isArray(v.modifiers());
        int dim = array ? dimSize(v.modifiers()) : 0;
        Binding b = bindLocalSlot(jt, array, dim, scaleOf(v.type()));
        locals.put(v.name(), b);
        pushInit(b, v.modifiers());
        if (!b.array()) applyScale(b, BbkStatement.AttrMod.NONE);
        storeVar(b);
    }

    private void initGlobalDs(BbkDeclaration.DataStructure d) { if (!hasMod(d.modifiers(), "TEMPLATE")) initDsSubfields(d.name(), resolveSubs(d)); }

    private void initLocalDs(BbkDeclaration.DataStructure d) { if (!hasMod(d.modifiers(), "TEMPLATE")) initDsSubfields(d.name(), resolveSubs(d)); }

    private List<BbkDeclaration.Subfield> resolveSubs(BbkDeclaration.DataStructure d) {
        if (!d.subfields().isEmpty()) return d.subfields();
        String like = likedsTemplate(d.modifiers());
        return like != null ? template(like) : d.subfields();
    }

    private void initClone(String dsName, String templateName, boolean global) {
        for (BbkDeclaration.Subfield sub : template(templateName)) {
            Binding b = (global ? globals : locals).get(dsName + "." + sub.name());
            initSubfield(b, sub);
        }
    }

    private void initDsSubfields(String dsName, List<BbkDeclaration.Subfield> subs) {
        for (BbkDeclaration.Subfield sub : subs) initSubfield(lookup(dsName + "." + sub.name()), sub);
    }

    private void initSubfield(Binding b, BbkDeclaration.Subfield sub) {
        if (b.array()) { newArray(b.jt(), b.dim()); storeVar(b); return; }
        BbkExpr inz = inzValue(sub.modifiers());
        if (inz != null) coerce(expr(inz), b.jt()); else pushDefaultScalar(b.jt());
        applyScale(b, BbkStatement.AttrMod.NONE);
        storeVar(b);
    }

    private Binding bindParam(String name, Jt jt, boolean array, int scale) {
        Binding b = bindLocalSlot(jt, array, 0, scale);
        locals.put(name, b);
        return b;
    }

    private Binding bindLocalSlot(Jt jt, boolean array, int dim, int scale) {
        Binding b = Binding.local(nextSlot, jt, array, dim, scale);
        nextSlot += (!array && wide(jt)) ? 2 : 1;
        return b;
    }

    private Binding lookup(String name) {
        Binding b = locals.get(name);
        return b != null ? b : globals.get(name);
    }

    // -----------------------------------------------------------------------
    // Loads / stores / coercion
    // -----------------------------------------------------------------------

    private void loadVar(Binding b) {
        if (b.global()) mv.visitFieldInsn(GETSTATIC, INTERNAL, b.field(), descOf(b.jt(), b.array()));
        else mv.visitVarInsn(b.array() ? ALOAD : scalarLoad(b.jt()), b.slot());
    }

    private void storeVar(Binding b) {
        if (b.global()) mv.visitFieldInsn(PUTSTATIC, INTERNAL, b.field(), descOf(b.jt(), b.array()));
        else mv.visitVarInsn(b.array() ? ASTORE : scalarStore(b.jt()), b.slot());
    }

    private static int scalarLoad(Jt jt) { return switch (jt) { case LONG -> LLOAD; case DOUBLE -> DLOAD; case BOOL -> ILOAD; case STRING, DECIMAL -> ALOAD; }; }
    private static int scalarStore(Jt jt) { return switch (jt) { case LONG -> LSTORE; case DOUBLE -> DSTORE; case BOOL -> ISTORE; case STRING, DECIMAL -> ASTORE; }; }

    private Binding arrayBinding(BbkExpr.Index ix) {
        if (!(ix.target() instanceof BbkExpr.Identifier id)) throw unsupported("indexing a non-name");
        Binding b = lookup(id.name());
        if (b == null || !b.array()) throw unsupported("'" + id.name() + "' is not an array");
        if (ix.indices().size() != 1) throw unsupported("multi-dimensional subscript");
        return b;
    }

    private Binding dsArrayField(BbkExpr.Index ix, String field) {
        if (!(ix.target() instanceof BbkExpr.Identifier id)) throw unsupported("indexing a non-name");
        if (ix.indices().size() != 1) throw unsupported("multi-dimensional subscript");
        Binding b = lookup(id.name() + "." + field);
        if (b == null || !b.array()) throw unsupported("'" + id.name() + "." + field + "' is not a DS-array field");
        return b;
    }

    private Binding arrayArg(BbkExpr arg) {
        if (!(arg instanceof BbkExpr.Identifier id)) throw unsupported("array argument must be a name");
        Binding b = lookup(id.name());
        if (b == null || !b.array()) throw unsupported("argument is not an array");
        return b;
    }

    private void coerceIndex(BbkExpr e) { coerce(expr(e), Jt.LONG); mv.visitInsn(L2I); }

    private void pushInit(Binding b, List<BbkModifier> modifiers) {
        if (b.array()) { newArray(b.jt(), dimSize(modifiers)); return; }
        BbkExpr inz = inzValue(modifiers);
        if (inz != null) coerce(expr(inz), b.jt()); else pushDefaultScalar(b.jt());
    }

    private void pushDefaultScalar(Jt jt) {
        switch (jt) {
            case LONG -> mv.visitInsn(LCONST_0);
            case DOUBLE -> mv.visitInsn(DCONST_0);
            case BOOL -> mv.visitInsn(ICONST_0);
            case STRING -> mv.visitLdcInsn("");
            case DECIMAL -> mv.visitFieldInsn(GETSTATIC, BIGDEC, "ZERO", "Ljava/math/BigDecimal;");
        }
    }

    private void newArray(Jt jt, int size) {
        pushInt(size);
        switch (jt) {
            case LONG -> mv.visitIntInsn(NEWARRAY, T_LONG);
            case DOUBLE -> mv.visitIntInsn(NEWARRAY, T_DOUBLE);
            case BOOL -> mv.visitIntInsn(NEWARRAY, T_BOOLEAN);
            case STRING -> mv.visitTypeInsn(ANEWARRAY, STR);
            case DECIMAL -> mv.visitTypeInsn(ANEWARRAY, BIGDEC);
        }
    }

    private static int arrayLoad(Jt jt) { return switch (jt) { case LONG -> LALOAD; case DOUBLE -> DALOAD; case BOOL -> BALOAD; case STRING, DECIMAL -> AALOAD; }; }
    private static int arrayStore(Jt jt) { return switch (jt) { case LONG -> LASTORE; case DOUBLE -> DASTORE; case BOOL -> BASTORE; case STRING, DECIMAL -> AASTORE; }; }

    /** Apply the declared decimal scale (with rounding from {@code attr}) to a DECIMAL on the stack. */
    private void applyScale(Binding b, BbkStatement.AttrMod attr) {
        if (b.jt() != Jt.DECIMAL || b.scale() < 0) return;
        pushInt(b.scale());
        mv.visitFieldInsn(GETSTATIC, ROUND, roundingMode(attr), "Ljava/math/RoundingMode;");
        mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "setScale", "(ILjava/math/RoundingMode;)Ljava/math/BigDecimal;", false);
    }

    private static String roundingMode(BbkStatement.AttrMod attr) {
        return switch (attr) { case HALFUP -> "HALF_UP"; case HALFDOWN -> "HALF_DOWN"; case TRUNC, NONE -> "DOWN"; };
    }

    private Jt coerce(Jt have, Jt want) { coerceTop(have, want); return want; }

    /** Coerce the top-of-stack value from {@code have} to {@code want} along the numeric tower. */
    private void coerceTop(Jt have, Jt want) {
        if (have == want) return;
        switch (have) {
            case LONG -> {
                if (want == Jt.DOUBLE) { mv.visitInsn(L2D); return; }
                if (want == Jt.DECIMAL) { mv.visitMethodInsn(INVOKESTATIC, BIGDEC, "valueOf", "(J)Ljava/math/BigDecimal;", false); return; }
            }
            case DOUBLE -> {
                if (want == Jt.LONG) { mv.visitInsn(D2L); return; }
                if (want == Jt.DECIMAL) { mv.visitMethodInsn(INVOKESTATIC, BIGDEC, "valueOf", "(D)Ljava/math/BigDecimal;", false); return; }
            }
            case DECIMAL -> {
                if (want == Jt.LONG) { mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "longValue", "()J", false); return; }
                if (want == Jt.DOUBLE) { mv.visitMethodInsn(INVOKEVIRTUAL, BIGDEC, "doubleValue", "()D", false); return; }
            }
            default -> { }
        }
        throw unsupported("conversion " + have + " -> " + want);
    }

    // -----------------------------------------------------------------------
    // Special values
    // -----------------------------------------------------------------------

    private Jt starValue(String name) {
        String n = (name.startsWith("*") ? name.substring(1) : name).toUpperCase();
        switch (n) {
            case "ON" -> { mv.visitInsn(ICONST_1); return Jt.BOOL; }
            case "OFF" -> { mv.visitInsn(ICONST_0); return Jt.BOOL; }
            case "ZERO", "ZEROS" -> { mv.visitInsn(LCONST_0); return Jt.LONG; }
            case "BLANK", "BLANKS" -> { mv.visitLdcInsn(""); return Jt.STRING; }
            case "NULL" -> { mv.visitInsn(ACONST_NULL); return Jt.STRING; }
            default -> throw unsupported("special value '*" + n + "'");
        }
    }

    private Jt starType(String name) {
        String n = (name.startsWith("*") ? name.substring(1) : name).toUpperCase();
        return switch (n) {
            case "ON", "OFF" -> Jt.BOOL;
            case "ZERO", "ZEROS" -> Jt.LONG;
            case "BLANK", "BLANKS", "NULL" -> Jt.STRING;
            default -> throw unsupported("special value '*" + n + "'");
        };
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String descriptor(Sig sig) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < sig.paramTypes().size(); i++) sb.append(descOf(sig.paramTypes().get(i), sig.paramArray().get(i)));
        sb.append(')').append(sig.ret() == null ? "V" : descOf(sig.ret(), false));
        return sb.toString();
    }

    private static String descOf(Jt jt, boolean array) {
        String base = switch (jt) {
            case LONG -> "J"; case DOUBLE -> "D"; case BOOL -> "Z";
            case STRING -> "Ljava/lang/String;"; case DECIMAL -> "Ljava/math/BigDecimal;";
        };
        return array ? "[" + base : base;
    }

    private void newDecimal(String text) {
        mv.visitTypeInsn(NEW, BIGDEC);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(text);
        mv.visitMethodInsn(INVOKESPECIAL, BIGDEC, "<init>", "(Ljava/lang/String;)V", false);
    }

    private void pushInt(int n) {
        if (n >= -1 && n <= 5) mv.visitInsn(ICONST_0 + n);
        else if (n >= Byte.MIN_VALUE && n <= Byte.MAX_VALUE) mv.visitIntInsn(BIPUSH, n);
        else if (n >= Short.MIN_VALUE && n <= Short.MAX_VALUE) mv.visitIntInsn(SIPUSH, n);
        else mv.visitLdcInsn(n);
    }

    private static boolean wide(Jt jt) { return jt == Jt.LONG || jt == Jt.DOUBLE; }

    private static Jt wider(Jt a, Jt b) {
        if (a == Jt.DECIMAL || b == Jt.DECIMAL) return Jt.DECIMAL;
        if (a == Jt.DOUBLE || b == Jt.DOUBLE) return Jt.DOUBLE;
        return Jt.LONG;
    }

    private static int arithOp(BbkExpr.BinOp op, Jt jt) {
        boolean d = jt == Jt.DOUBLE;
        return switch (op) {
            case ADD -> d ? DADD : LADD; case SUB -> d ? DSUB : LSUB; case MUL -> d ? DMUL : LMUL;
            case DIV -> d ? DDIV : LDIV; case MOD -> d ? DREM : LREM;
            default -> throw new IllegalArgumentException("not arithmetic: " + op);
        };
    }

    private static void requireNumeric(Jt jt) {
        if (jt == Jt.BOOL || jt == Jt.STRING) throw new UnsupportedOperationException("numeric operand required, got " + jt);
    }

    private static BbkExpr.BinOp compoundToBin(BbkStatement.AssignOp op) {
        return switch (op) {
            case ADD -> BbkExpr.BinOp.ADD; case SUB -> BbkExpr.BinOp.SUB; case MUL -> BbkExpr.BinOp.MUL;
            case DIV -> BbkExpr.BinOp.DIV; case MOD -> BbkExpr.BinOp.MOD;
            case AND -> BbkExpr.BinOp.BIT_AND; case OR -> BbkExpr.BinOp.BIT_OR; case XOR -> BbkExpr.BinOp.BIT_XOR;
            case SHL -> BbkExpr.BinOp.SHL; case SHR -> BbkExpr.BinOp.SHR;
            default -> throw new UnsupportedOperationException("compound op " + op);
        };
    }

    private static boolean isName(BbkExpr e, String name) { return e instanceof BbkExpr.Identifier id && id.name().equals(name); }

    private static String memberKey(BbkExpr.Member m) {
        if (!(m.target() instanceof BbkExpr.Identifier id)) throw new UnsupportedOperationException("nested member access");
        return id.name() + "." + m.field();
    }

    private static String mainOf(BbkDeclaration.CtlOpt ctl) {
        for (BbkModifier m : ctl.keywords()) {
            if (m.name().equalsIgnoreCase("MAIN") && m.args().size() == 1 && m.args().get(0) instanceof BbkExpr.Identifier id) return id.name();
        }
        return null;
    }

    private static String likedsTemplate(List<BbkModifier> mods) {
        for (BbkModifier m : mods) {
            if (m.name().equalsIgnoreCase("LIKEDS") && m.args().size() == 1 && m.args().get(0) instanceof BbkExpr.Identifier id) return id.name();
        }
        return null;
    }

    private List<BbkDeclaration.Subfield> template(String name) {
        List<BbkDeclaration.Subfield> tpl = dsTemplates.get(name);
        if (tpl == null) throw unsupported("LIKEDS(" + name + ") — data structure not declared before use");
        return tpl;
    }

    private static boolean hasMod(List<BbkModifier> mods, String name) {
        for (BbkModifier m : mods) if (m.name().equalsIgnoreCase(name)) return true;
        return false;
    }

    private static boolean isArray(List<BbkModifier> mods) { return hasMod(mods, "DIM"); }

    private static int dimSize(List<BbkModifier> mods) {
        for (BbkModifier m : mods) {
            if (m.name().equalsIgnoreCase("DIM") && m.args().size() == 1 && m.args().get(0) instanceof BbkExpr.Literal lit) {
                return Integer.parseInt(lit.text());
            }
        }
        throw new UnsupportedOperationException("DIM requires a literal size");
    }

    private static BbkExpr inzValue(List<BbkModifier> modifiers) {
        for (BbkModifier m : modifiers) {
            if (m.name().equalsIgnoreCase("INZ") && m.args().size() == 1) return m.args().get(0);
        }
        return null;
    }

    private void jump(Label l) { mv.visitJumpInsn(GOTO, l); }

    private static Jt jt(BbkType type) {
        if (type instanceof BbkType.Primitive p) {
            return switch (p.name().toUpperCase()) {
                case "INT", "UNS" -> Jt.LONG;
                case "FLOAT" -> Jt.DOUBLE;
                case "PACKED", "ZONED", "BINDEC" -> Jt.DECIMAL;
                case "BOOL", "IND" -> Jt.BOOL;
                case "CHAR", "VARCHAR" -> Jt.STRING;
                case "DATE", "TIME", "TIMESTAMP" -> throw new UnsupportedOperationException("date/time types need a date runtime (not lowered)");
                case "POINTER" -> throw new UnsupportedOperationException("pointers are not supported on the JVM back-end");
                default -> throw new UnsupportedOperationException("type '" + p.name() + "' not supported by the bytecode back-end");
            };
        }
        throw new UnsupportedOperationException(((BbkType.Like) type).kind() + " cannot be resolved to a scalar here");
    }

    private static int scaleOf(BbkType type) {
        if (type instanceof BbkType.Primitive p) {
            String n = p.name().toUpperCase();
            if (n.equals("PACKED") || n.equals("ZONED") || n.equals("BINDEC")) return p.decimals() == null ? 0 : p.decimals();
        }
        return -1;
    }

    private static String stripDecSuffix(String text) {
        return (text.endsWith("d") || text.endsWith("D")) ? text.substring(0, text.length() - 1) : text;
    }

    private static String unescape(String lit) {
        String body = lit.length() >= 2 ? lit.substring(1, lit.length() - 1) : "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '\\' && i + 1 < body.length()) {
                char n = body.charAt(++i);
                sb.append(switch (n) { case 'n' -> '\n'; case 't' -> '\t'; case 'r' -> '\r'; default -> n; });
            } else sb.append(ch);
        }
        return sb.toString();
    }

    private static UnsupportedOperationException unsupported(String what) {
        return new UnsupportedOperationException(what + " is not supported by the bytecode back-end");
    }
}
