package com.larena.boxbreaker.debugger;

import com.larena.boxbreaker.core.ast.*;
import com.larena.boxbreaker.core.parser.BbkParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Intérprete (tree-walking) de BBK: ejecuta el AST emitiendo un paso por cada
 * sentencia. Cubre el núcleo imperativo no-SO: variables/constantes escalares,
 * asignaciones, control de flujo, procedimientos, subrutinas, builtins puros y
 * {@code print}. Lo no soportado (estructuras, arrays, monitor, file I/O,
 * fechas/punteros) corta con un {@link DebugException} claro.
 */
final class Interpreter {

    /** Error de debug presentable (no soportado, variable no declarada, etc.). */
    static final class DebugException extends RuntimeException {
        DebugException(String message) { super(message); }
    }

    /** El listener pidió detener la ejecución. */
    static final class Stopped extends RuntimeException {
        Stopped() { super(null, null, false, false); }
    }

    // --- señales de control de flujo ---
    private static final class BreakSignal extends RuntimeException { BreakSignal() { super(null, null, false, false); } }
    private static final class ContinueSignal extends RuntimeException { ContinueSignal() { super(null, null, false, false); } }
    private static final class LeavesrSignal extends RuntimeException { LeavesrSignal() { super(null, null, false, false); } }
    private static final class ReturnSignal extends RuntimeException {
        final Object value;
        ReturnSignal(Object value) { super(null, null, false, false); this.value = value; }
    }

    private enum Cat { LONG, DOUBLE, DECIMAL, BOOL, STRING, DS }

    private final DebugListener listener;
    private final Locations locations;
    private final StringBuilder out = new StringBuilder();
    private final Map<String, Object> globals = new LinkedHashMap<>();
    private final Deque<Map<String, Object>> frames = new ArrayDeque<>();
    private final Map<String, BbkDeclaration.Procedure> procedures = new LinkedHashMap<>();
    private final Map<String, List<BbkItem>> subroutines = new LinkedHashMap<>();
    private final Map<String, BbkDeclaration.DataStructure> dsShapes = new LinkedHashMap<>();
    private int step = 0;
    private int depth = 0;

    /** Modo evaluación (watch / condición): bloquea invocar procedimientos. */
    private volatile boolean evaluating = false;

    /** Evaluador ligado a este intérprete; usa el entorno vigente en cada llamada. */
    private final Evaluator evaluator = new Evaluator() {
        @Override public String evaluate(String expression) { return evalExpression(expression); }
        @Override public boolean evaluateCondition(String expression) { return evalConditionExpression(expression); }
    };

    Interpreter(DebugListener listener, Locations locations) {
        this.listener = listener;
        this.locations = locations;
    }

    String output() {
        return out.toString();
    }

    // =======================================================================
    // Entrada
    // =======================================================================

    void run(BbkProgram program) {
        hoist(program.items());
        String main = mainProcedure(program.items());

        for (BbkItem item : program.items()) {
            if (item instanceof BbkDeclaration.Procedure) continue;       // ya registrado
            if (item instanceof BbkStatement && main != null) continue;   // con MAIN, el mainline es el proc
            execItem(item);
        }

        if (main != null) {
            BbkDeclaration.Procedure p = procedures.get(main);
            if (p == null) throw new DebugException("CTL-OPT MAIN apunta a un procedimiento inexistente: " + main);
            callProcedure(p, List.of());
        }
    }

    /** Registra procedimientos y subrutinas (se pueden invocar antes de su declaración). */
    private void hoist(List<BbkItem> items) {
        for (BbkItem item : items) {
            if (item instanceof BbkDeclaration.Procedure p) procedures.put(p.name(), p);
            else if (item instanceof BbkStatement.Subroutine s) subroutines.put(s.name(), s.body());
            else if (item instanceof BbkDeclaration.DataStructure ds) dsShapes.put(ds.name(), ds);
        }
    }

    private static String mainProcedure(List<BbkItem> items) {
        for (BbkItem item : items) {
            if (item instanceof BbkDeclaration.CtlOpt opt) {
                for (BbkModifier m : opt.keywords()) {
                    if (m.name().equalsIgnoreCase("MAIN") && m.args().size() == 1
                        && m.args().get(0) instanceof BbkExpr.Identifier id) {
                        return id.name();
                    }
                }
            }
        }
        return null;
    }

    // =======================================================================
    // Ejecución
    // =======================================================================

    private void execBody(List<BbkItem> items) {
        hoist(items);
        for (BbkItem item : items) {
            if (item instanceof BbkDeclaration.Procedure) continue;
            if (item instanceof BbkStatement.Subroutine) continue;   // sólo se ejecuta vía EXSR
            execItem(item);
        }
    }

    private void execItem(BbkItem item) {
        if (item instanceof BbkDeclaration d) execDecl(d);
        else execStmt((BbkStatement) item);
    }

    private void execDecl(BbkDeclaration d) {
        switch (d) {
            case BbkDeclaration.Variable v -> {
                int start = out.length();
                Object val = inz(v.modifiers()) != null
                    ? coerce(eval(inz(v.modifiers())), category(v.type()))
                    : defaultValue(v.type());
                define(v.name(), val);
                emit(v, "DCL-S " + v.name() + " = " + display(val), out.substring(start));
            }
            case BbkDeclaration.Constant c -> define(c.name(), eval(c.value()));
            case BbkDeclaration.CtlOpt ignored -> { /* MAIN ya resuelto */ }
            case BbkDeclaration.Prototype ignored -> { /* sin efecto en runtime */ }
            case BbkDeclaration.DataStructure ds -> {
                dsShapes.put(ds.name(), ds);
                if (!isTemplate(ds)) define(ds.name(), instantiateDs(ds));   // DS con storage
            }
            case BbkDeclaration.File ignored ->
                throw new DebugException("DCL-F / acceso a archivo no está soportado en el debugger v1");
            default -> throw new DebugException("declaración no soportada en el debugger v1");
        }
    }

    private void execStmt(BbkStatement s) {
        switch (s) {
            case BbkStatement.ExpressionStatement e -> {
                int start = out.length();
                eval(e.expr());
                emit(s, Renderer.stmt(s), out.substring(start));
            }
            case BbkStatement.Assignment a -> {
                int start = out.length();
                assignStatement(a);
                emit(s, Renderer.stmt(s), out.substring(start));
            }
            case BbkStatement.If i -> {
                emit(s, Renderer.stmt(s), "");
                if (truthy(eval(i.condition()))) nested(i.thenBody());
                else nested(i.elseBody());
            }
            case BbkStatement.Select sel -> {
                emit(s, Renderer.stmt(s), "");
                boolean done = false;
                for (BbkStatement.When w : sel.whens()) {
                    if (truthy(eval(w.condition()))) { nested(w.body()); done = true; break; }
                }
                if (!done) nested(sel.otherBody());
            }
            case BbkStatement.While w -> {
                while (true) {
                    emit(s, Renderer.stmt(s), "");
                    if (!truthy(eval(w.condition()))) break;
                    if (!loopBody(w.body())) break;
                }
            }
            case BbkStatement.DoWhile d -> {
                do {
                    if (!loopBody(d.body())) break;
                    emit(s, Renderer.stmt(s), "");
                } while (truthy(eval(d.condition())));
            }
            case BbkStatement.For f -> {
                if (f.init() != null) execItem(f.init());
                while (true) {
                    emit(s, Renderer.stmt(s), "");
                    if (f.condition() != null && !truthy(eval(f.condition()))) break;
                    if (!loopBody(f.body())) break;
                    if (f.update() != null) execStmt(f.update());
                }
            }
            case BbkStatement.Break ignored -> { emit(s, "break", ""); throw new BreakSignal(); }
            case BbkStatement.Continue ignored -> { emit(s, "continue", ""); throw new ContinueSignal(); }
            case BbkStatement.Return r -> {
                emit(s, Renderer.stmt(s), "");
                throw new ReturnSignal(r.value() == null ? null : eval(r.value()));
            }
            case BbkStatement.Exsr x -> {
                emit(s, Renderer.stmt(s), "");
                List<BbkItem> body = subroutines.get(x.name());
                if (body == null) throw new DebugException("subrutina no declarada: " + x.name());
                depth++;
                try { execBody(body); }
                catch (LeavesrSignal ignored) { /* LEAVESR sale de la subrutina */ }
                finally { depth--; }
            }
            case BbkStatement.Leavesr ignored -> { emit(s, "LEAVESR", ""); throw new LeavesrSignal(); }
            case BbkStatement.Callp c -> {
                int start = out.length();
                eval(c.expr());
                emit(s, Renderer.stmt(s), out.substring(start));
            }
            case BbkStatement.Monitor ignored ->
                throw new DebugException("monitor/on-error no soportado en el debugger v1");
            case BbkStatement.FileOp f ->
                throw new DebugException("operación de archivo (" + f.opcode() + ") no soportada en el debugger v1");
            case BbkStatement.Directive ignored -> { /* directiva de preprocesador: sin efecto */ }
            case BbkStatement.Subroutine ignored -> { /* definición: se ejecuta vía EXSR */ }
        }
    }

    /** Ejecuta el cuerpo de un loop una vez; devuelve false si hubo break. */
    private boolean loopBody(List<BbkItem> body) {
        depth++;
        try {
            execBody(body);
            return true;
        } catch (BreakSignal b) {
            return false;
        } catch (ContinueSignal c) {
            return true;
        } finally {
            depth--;
        }
    }

    /** Ejecuta un bloque anidado (then/else/when) con un nivel más de profundidad. */
    private void nested(List<BbkItem> body) {
        depth++;
        try { execBody(body); }
        finally { depth--; }
    }

    private void assignStatement(BbkStatement.Assignment a) {
        if (a.target() instanceof BbkExpr.Member m) {
            assignMember(m, a);
            return;
        }
        String name = targetName(a.target());
        Object rhs = eval(a.value());
        Object result = (a.op() == BbkStatement.AssignOp.ASSIGN)
            ? rhs
            : binary(lookup(name), compoundOp(a.op()), rhs);
        assign(name, result);
    }

    @SuppressWarnings("unchecked")
    private void assignMember(BbkExpr.Member m, BbkStatement.Assignment a) {
        Object target = eval(m.target());
        if (!(target instanceof Map)) {
            throw new DebugException("asignación a campo '." + m.field() + "' sobre un valor que no es estructura");
        }
        Map<String, Object> ds = (Map<String, Object>) target;
        Object rhs = eval(a.value());
        Object result = (a.op() == BbkStatement.AssignOp.ASSIGN)
            ? rhs
            : binary(ds.get(m.field()), compoundOp(a.op()), rhs);
        ds.put(m.field(), result);
    }

    private Object callProcedure(BbkDeclaration.Procedure p, List<Object> args) {
        Map<String, Object> frame = new LinkedHashMap<>();
        for (int i = 0; i < p.params().size(); i++) {
            BbkDeclaration.Parameter param = p.params().get(i);
            Object val = i < args.size() ? coerce(args.get(i), category(param.type())) : defaultValue(param.type());
            frame.put(param.name(), val);
        }
        frames.push(frame);
        depth++;
        try {
            execBody(p.body());
            return null;
        } catch (ReturnSignal r) {
            return r.value;
        } finally {
            depth--;
            frames.pop();
        }
    }

    // =======================================================================
    // Evaluación de expresiones
    // =======================================================================

    private Object eval(BbkExpr e) {
        return switch (e) {
            case BbkExpr.Identifier id -> lookup(id.name());
            case BbkExpr.Literal l -> literal(l);
            case BbkExpr.BoolLit b -> b.value();
            case BbkExpr.NullLit ignored -> null;
            case BbkExpr.StarIdent s -> starIdent(s.name());
            case BbkExpr.Unary u -> unary(u.op(), eval(u.operand()));
            case BbkExpr.Binary b -> evalBinary(b);
            case BbkExpr.Ternary t -> truthy(eval(t.condition())) ? eval(t.then()) : eval(t.otherwise());
            case BbkExpr.Call c -> evalCall(c);
            case BbkExpr.Index ignored -> throw new DebugException("arrays no soportados en el debugger v1");
            case BbkExpr.Member m -> readMember(m);
        };
    }

    private Object readMember(BbkExpr.Member m) {
        Object target = eval(m.target());
        if (target instanceof Map<?, ?> ds) {
            if (!ds.containsKey(m.field())) throw new DebugException("campo inexistente: " + m.field());
            return ds.get(m.field());
        }
        throw new DebugException("acceso a campo '." + m.field() + "' sobre un valor que no es estructura");
    }

    private Object evalBinary(BbkExpr.Binary b) {
        // && y || cortocircuitan
        if (b.op() == BbkExpr.BinOp.AND) return truthy(eval(b.left())) && truthy(eval(b.right()));
        if (b.op() == BbkExpr.BinOp.OR) return truthy(eval(b.left())) || truthy(eval(b.right()));
        return binary(eval(b.left()), b.op(), eval(b.right()));
    }

    // ----- evaluación de expresiones (watches / condiciones de breakpoint) -----

    String evalExpression(String expr) {
        BbkExpr e = parseExpr(expr);
        boolean prev = evaluating;
        evaluating = true;
        try {
            return display(eval(e));
        } finally {
            evaluating = prev;
        }
    }

    boolean evalConditionExpression(String expr) {
        BbkExpr e = parseExpr(expr);
        boolean prev = evaluating;
        evaluating = true;
        try {
            return truthy(eval(e));
        } finally {
            evaluating = prev;
        }
    }

    private static BbkExpr parseExpr(String expr) {
        if (expr == null || expr.isBlank()) throw new DebugException("expresión vacía");
        BbkProgram program = BbkParser.parse(expr + ";");
        if (!program.items().isEmpty() && program.items().get(0) instanceof BbkStatement.ExpressionStatement es) {
            return es.expr();
        }
        throw new DebugException("no es una expresión: " + expr);
    }

    private Object evalCall(BbkExpr.Call c) {
        if (!(c.target() instanceof BbkExpr.Identifier id)) {
            throw new DebugException("sólo se soportan llamadas por nombre en el debugger v1");
        }
        String name = id.name();
        if (name.equals("print")) {
            Object v = c.args().isEmpty() ? "" : eval(c.args().get(0));
            out.append(asText(v)).append('\n');
            return null;
        }
        if (procedures.containsKey(name)) {
            if (evaluating) throw new DebugException("no se pueden invocar procedimientos al evaluar");
            java.util.List<Object> args = c.args().stream().map(this::eval).toList();
            return callProcedure(procedures.get(name), args);
        }
        return builtin(name, c.args());
    }

    private Object builtin(String name, List<BbkExpr> args) {
        return switch (name) {
            case "len" -> (long) asText(eval(args.get(0))).length();
            case "upper" -> asText(eval(args.get(0))).toUpperCase();
            case "lower" -> asText(eval(args.get(0))).toLowerCase();
            case "trim" -> asText(eval(args.get(0))).strip();
            case "triml" -> asText(eval(args.get(0))).stripLeading();
            case "trimr" -> asText(eval(args.get(0))).stripTrailing();
            case "char" -> asText(eval(args.get(0)));
            case "substr" -> substr(args);
            case "int" -> toLong(eval(args.get(0)));
            case "float" -> toDouble(eval(args.get(0)));
            case "dec" -> toBigDecimal(eval(args.get(0)));
            case "abs" -> absOf(eval(args.get(0)));
            case "sqrt" -> Math.sqrt(toDouble(eval(args.get(0))));
            default -> throw new DebugException("función desconocida: " + name);
        };
    }

    private Object substr(List<BbkExpr> args) {
        String s = asText(eval(args.get(0)));
        int start = (int) toLong(eval(args.get(1)));   // 1-based, estilo RPG
        int from = Math.max(0, start - 1);
        if (args.size() >= 3) {
            int len = (int) toLong(eval(args.get(2)));
            int to = Math.min(s.length(), from + Math.max(0, len));
            return from <= to ? s.substring(from, to) : "";
        }
        return from <= s.length() ? s.substring(from) : "";
    }

    // =======================================================================
    // Operaciones y modelo numérico
    // =======================================================================

    private Object unary(BbkExpr.UnOp op, Object v) {
        return switch (op) {
            case POS -> v;
            case NEG -> negate(v);
            case NOT -> !truthy(v);
            case BIT_NOT -> ~toLong(v);
        };
    }

    private Object binary(Object l, BbkExpr.BinOp op, Object r) {
        switch (op) {
            case ADD:
                if (l instanceof String || r instanceof String) return asText(l) + asText(r);  // concatenación
                return arith(l, r, op);
            case SUB: case MUL: case DIV: case MOD: case POW:
                return arith(l, r, op);
            case EQ: return equalsValue(l, r);
            case NE: return !equalsValue(l, r);
            case LT: return compare(l, r) < 0;
            case GT: return compare(l, r) > 0;
            case LE: return compare(l, r) <= 0;
            case GE: return compare(l, r) >= 0;
            case AND: return truthy(l) && truthy(r);
            case OR: return truthy(l) || truthy(r);
            case BIT_AND: return toLong(l) & toLong(r);
            case BIT_OR: return toLong(l) | toLong(r);
            case BIT_XOR: return toLong(l) ^ toLong(r);
            case SHL: return toLong(l) << toLong(r);
            case SHR: return toLong(l) >> toLong(r);
            default: throw new DebugException("operador no soportado: " + op);
        }
    }

    private Object arith(Object l, Object r, BbkExpr.BinOp op) {
        if (op == BbkExpr.BinOp.POW) return Math.pow(toDouble(l), toDouble(r));   // ** -> FLOAT
        if (l instanceof Double || r instanceof Double) {
            double a = toDouble(l), b = toDouble(r);
            return switch (op) {
                case ADD -> a + b; case SUB -> a - b; case MUL -> a * b;
                case DIV -> a / b; case MOD -> a % b; default -> 0.0;
            };
        }
        if (l instanceof BigDecimal || r instanceof BigDecimal) {
            BigDecimal a = toBigDecimal(l), b = toBigDecimal(r);
            return switch (op) {
                case ADD -> a.add(b); case SUB -> a.subtract(b); case MUL -> a.multiply(b);
                case DIV -> a.divide(b, 9, RoundingMode.HALF_UP); case MOD -> a.remainder(b);
                default -> BigDecimal.ZERO;
            };
        }
        long a = toLong(l), b = toLong(r);
        return switch (op) {
            case ADD -> a + b; case SUB -> a - b; case MUL -> a * b;
            case DIV -> b == 0 ? raiseDivZero() : a / b;
            case MOD -> b == 0 ? raiseDivZero() : a % b;
            default -> 0L;
        };
    }

    private static long raiseDivZero() {
        throw new DebugException("división por cero");
    }

    private Object negate(Object v) {
        if (v instanceof Double d) return -d;
        if (v instanceof BigDecimal bd) return bd.negate();
        return -toLong(v);
    }

    private Object absOf(Object v) {
        if (v instanceof Double d) return Math.abs(d);
        if (v instanceof BigDecimal bd) return bd.abs();
        return Math.abs(toLong(v));
    }

    private boolean equalsValue(Object l, Object r) {
        if (isNumber(l) && isNumber(r)) return compare(l, r) == 0;
        if (l == null || r == null) return l == r;
        return l.equals(r);
    }

    private int compare(Object l, Object r) {
        if (isNumber(l) && isNumber(r)) return toBigDecimal(l).compareTo(toBigDecimal(r));
        return asText(l).compareTo(asText(r));
    }

    private static boolean isNumber(Object o) {
        return o instanceof Long || o instanceof Double || o instanceof BigDecimal;
    }

    private boolean truthy(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        if (o instanceof Long l) return l != 0;
        if (o instanceof Double d) return d != 0;
        if (o instanceof BigDecimal bd) return bd.signum() != 0;
        if (o instanceof String s) return !s.isEmpty();
        return true;
    }

    private long toLong(Object o) {
        if (o instanceof Long l) return l;
        if (o instanceof Double d) return d.longValue();
        if (o instanceof BigDecimal bd) return bd.longValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        if (o instanceof String s) try { return Long.parseLong(s.strip()); } catch (NumberFormatException e) { return 0; }
        return 0;
    }

    private double toDouble(Object o) {
        if (o instanceof Double d) return d;
        if (o instanceof Long l) return l;
        if (o instanceof BigDecimal bd) return bd.doubleValue();
        if (o instanceof String s) try { return Double.parseDouble(s.strip()); } catch (NumberFormatException e) { return 0; }
        return 0;
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Long l) return BigDecimal.valueOf(l);
        if (o instanceof Double d) return BigDecimal.valueOf(d);
        if (o instanceof String s) try { return new BigDecimal(s.strip()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
        return BigDecimal.ZERO;
    }

    private String asText(Object o) {
        if (o == null) return "";
        if (o instanceof BigDecimal bd) return bd.toPlainString();
        return String.valueOf(o);
    }

    // =======================================================================
    // Literales, star-idents, tipos
    // =======================================================================

    private Object literal(BbkExpr.Literal l) {
        String t = l.text();
        return switch (l.kind()) {
            case INT -> Long.parseLong(t);
            case HEX -> Long.parseLong(stripPrefix(t, "0x", "0X"), 16);
            case OCT -> Long.parseLong(stripPrefix(t, "0o", "0O"), 8);
            case FLOAT -> Double.parseDouble(t);
            case DEC -> new BigDecimal(stripDecSuffix(t));
            case STRING -> unquote(t);
        };
    }

    /** Los literales decimales de BBK llevan sufijo 'd' (199.95d); BigDecimal no lo acepta. */
    private static String stripDecSuffix(String t) {
        if (!t.isEmpty()) {
            char last = t.charAt(t.length() - 1);
            if (last == 'd' || last == 'D') return t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static String stripPrefix(String t, String p1, String p2) {
        if (t.startsWith(p1) || t.startsWith(p2)) return t.substring(2);
        return t;
    }

    private static String unquote(String t) {
        if (t.length() >= 2 && (t.charAt(0) == '"' || t.charAt(0) == '\'')) {
            char q = t.charAt(0);
            String inner = t.substring(1, t.length() - 1);
            return inner.replace("" + q + q, "" + q);
        }
        return t;
    }

    private Object starIdent(String name) {
        return switch (name) {
            case "ON" -> Boolean.TRUE;
            case "OFF" -> Boolean.FALSE;
            case "ZERO", "ZEROS" -> 0L;
            case "BLANK", "BLANKS" -> "";
            case "NULL" -> null;
            default -> globals.getOrDefault("*" + name, Boolean.FALSE);   // indicadores (*INLR, *IN01, ...)
        };
    }

    private static String targetName(BbkExpr target) {
        if (target instanceof BbkExpr.Identifier id) return id.name();
        if (target instanceof BbkExpr.StarIdent s) return "*" + s.name();
        throw new DebugException("destino de asignación no soportado en el debugger v1");
    }

    private static BbkExpr.BinOp compoundOp(BbkStatement.AssignOp op) {
        return switch (op) {
            case ADD -> BbkExpr.BinOp.ADD; case SUB -> BbkExpr.BinOp.SUB; case MUL -> BbkExpr.BinOp.MUL;
            case DIV -> BbkExpr.BinOp.DIV; case MOD -> BbkExpr.BinOp.MOD;
            case AND -> BbkExpr.BinOp.BIT_AND; case OR -> BbkExpr.BinOp.BIT_OR; case XOR -> BbkExpr.BinOp.BIT_XOR;
            case SHL -> BbkExpr.BinOp.SHL; case SHR -> BbkExpr.BinOp.SHR;
            case ASSIGN -> throw new DebugException("operador compuesto inválido");
        };
    }

    private static BbkExpr inz(List<BbkModifier> modifiers) {
        for (BbkModifier m : modifiers) {
            if (m.name().equalsIgnoreCase("INZ") && m.args().size() == 1) return m.args().get(0);
        }
        return null;
    }

    private Cat category(BbkType type) {
        if (type instanceof BbkType.Primitive p) {
            return switch (p.name().toUpperCase()) {
                case "INT", "UNS" -> Cat.LONG;
                case "FLOAT" -> Cat.DOUBLE;
                case "PACKED", "ZONED", "BINDEC" -> Cat.DECIMAL;
                case "BOOL", "IND" -> Cat.BOOL;
                case "CHAR", "VARCHAR" -> Cat.STRING;
                case "DATE", "TIME", "TIMESTAMP" -> Cat.STRING;   // simplificado: fecha/hora como texto
                default -> throw new DebugException("tipo no soportado en el debugger v1: " + p.name());
            };
        }
        if (type instanceof BbkType.Like like && like.kind() == BbkType.LikeKind.LIKEDS) {
            return Cat.DS;
        }
        throw new DebugException("tipos LIKE/LIKEREC no soportados en el debugger v1");
    }

    private Object defaultValue(BbkType type) {
        return switch (category(type)) {
            case LONG -> 0L;
            case DOUBLE -> 0.0;
            case DECIMAL -> BigDecimal.ZERO;
            case BOOL -> Boolean.FALSE;
            case STRING -> "";
            case DS -> instantiateDs(dsName(type));
        };
    }

    private Object coerce(Object v, Cat cat) {
        return switch (cat) {
            case LONG -> toLong(v);
            case DOUBLE -> toDouble(v);
            case DECIMAL -> toBigDecimal(v);
            case BOOL -> truthy(v);
            case STRING -> asText(v);
            case DS -> copyDs(v);
        };
    }

    // ----- estructuras de datos (DS) -----

    private static boolean isTemplate(BbkDeclaration.DataStructure ds) {
        for (BbkModifier m : ds.modifiers()) {
            if (m.name().equalsIgnoreCase("TEMPLATE")) return true;
        }
        return false;
    }

    private static String dsName(BbkType type) {
        if (type instanceof BbkType.Like like) return like.name();
        throw new DebugException("se esperaba un tipo LIKEDS");
    }

    private Object instantiateDs(String name) {
        BbkDeclaration.DataStructure ds = dsShapes.get(name);
        if (ds == null) throw new DebugException("estructura no declarada: " + name);
        return instantiateDs(ds);
    }

    private Object instantiateDs(BbkDeclaration.DataStructure ds) {
        Map<String, Object> instance = new LinkedHashMap<>();
        for (BbkDeclaration.Subfield sub : ds.subfields()) {
            BbkExpr inz = inz(sub.modifiers());
            Object val = inz != null ? coerce(eval(inz), category(sub.type())) : defaultValue(sub.type());
            instance.put(sub.name(), val);
        }
        return instance;
    }

    private static Object copyDs(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) copy.put(String.valueOf(e.getKey()), e.getValue());
            return copy;
        }
        return v;
    }

    // =======================================================================
    // Entorno y traza
    // =======================================================================

    private Object lookup(String name) {
        if (!frames.isEmpty() && frames.peek().containsKey(name)) return frames.peek().get(name);
        if (globals.containsKey(name)) return globals.get(name);
        throw new DebugException("variable no declarada: " + name);
    }

    private void assign(String name, Object value) {
        if (!frames.isEmpty() && frames.peek().containsKey(name)) { frames.peek().put(name, value); return; }
        if (globals.containsKey(name)) { globals.put(name, value); return; }
        define(name, value);
    }

    private void define(String name, Object value) {
        (frames.isEmpty() ? globals : frames.peek()).put(name, value);
    }

    private void emit(BbkItem item, String statement, String outputDelta) {
        int line = locations == null ? 0 : locations.lineOf(item);
        String file = locations == null ? "" : locations.fileOf(item);

        Map<String, Object> scope = new LinkedHashMap<>(globals);
        if (!frames.isEmpty()) scope.putAll(frames.peek());

        Map<String, String> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : scope.entrySet()) snapshot.put(e.getKey(), display(e.getValue()));

        TraceStep ts = new TraceStep(++step, file, line, depth, statement, snapshot, outputDelta);
        if (listener.onStep(ts, evaluator) == DebugListener.Decision.STOP) throw new Stopped();
    }

    private static String display(Object o) {
        if (o == null) return "*NULL";
        if (o instanceof Boolean b) return b ? "*ON" : "*OFF";
        if (o instanceof String s) return "\"" + s + "\"";
        if (o instanceof BigDecimal bd) return bd.toPlainString();
        return String.valueOf(o);
    }
}
