package com.larena.boxbreaker.jvmdebug;

import com.larena.boxbreaker.core.ast.BbkExpr;
import com.larena.boxbreaker.core.ast.BbkProgram;
import com.larena.boxbreaker.core.ast.BbkStatement;
import com.larena.boxbreaker.core.parser.BbkParser;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evalúa expresiones BBK (Evaluate, watches, condiciones de breakpoint) contra el
 * frame suspendido. Reusa el parser de BBK y resuelve las variables leyendo sus
 * valores por JDI. <b>No invoca procedimientos</b>: sólo lee estado y aplica los
 * builtins puros, igual de seguro que evaluar al costado.
 */
public final class BbkExprEval {

    /** Error de evaluación presentable. */
    public static final class EvalException extends RuntimeException {
        public EvalException(String message) { super(message); }
    }

    private final Map<String, Object> env;   // nombre emitido (ds$sub para DS) -> valor tipado

    private BbkExprEval(Map<String, Object> env) {
        this.env = env;
    }

    /** Arma el entorno leyendo las variables del frame por JDI. */
    static BbkExprEval forFrame(ThreadReference thread, StackFrame frame) {
        Map<String, Object> env = new LinkedHashMap<>();
        for (Map.Entry<String, Value> e : BbkVariables.rawValues(frame).entrySet()) {
            env.put(e.getKey(), BbkValues.toJava(thread, e.getValue()));
        }
        return new BbkExprEval(env);
    }

    /** Evalúa con un entorno dado (para tests). */
    static BbkExprEval withEnv(Map<String, Object> env) {
        return new BbkExprEval(env);
    }

    /** Evalúa y devuelve el valor formateado estilo BBK (Evaluate / watches). */
    public String evaluateText(String expression) {
        return display(eval(parse(expression)));
    }

    /** Evalúa como condición booleana (breakpoints condicionales). */
    public boolean evaluateCondition(String expression) {
        return truthy(eval(parse(expression)));
    }

    // ----- parseo -----

    private static BbkExpr parse(String expr) {
        if (expr == null || expr.isBlank()) throw new EvalException("expresión vacía");
        BbkProgram program;
        try {
            program = BbkParser.parse(expr + ";");
        } catch (RuntimeException e) {
            throw new EvalException("no se pudo parsear: " + expr);
        }
        if (!program.items().isEmpty() && program.items().get(0) instanceof BbkStatement.ExpressionStatement es) {
            return es.expr();
        }
        throw new EvalException("no es una expresión: " + expr);
    }

    // ----- evaluación (modelo del intérprete, leyendo de env) -----

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
            case BbkExpr.Member m -> lookup(memberKey(m));
            case BbkExpr.Index ignored -> throw new EvalException("no se pueden evaluar arrays todavía");
        };
    }

    private Object lookup(String name) {
        if (!env.containsKey(name)) throw new EvalException("variable no visible: " + name.replace('$', '.'));
        return env.get(name);
    }

    private static String memberKey(BbkExpr.Member m) {
        if (m.target() instanceof BbkExpr.Identifier id) return id.name() + "$" + m.field();
        throw new EvalException("acceso a miembro anidado no soportado");
    }

    private Object evalBinary(BbkExpr.Binary b) {
        if (b.op() == BbkExpr.BinOp.AND) return truthy(eval(b.left())) && truthy(eval(b.right()));
        if (b.op() == BbkExpr.BinOp.OR) return truthy(eval(b.left())) || truthy(eval(b.right()));
        return binary(eval(b.left()), b.op(), eval(b.right()));
    }

    private Object evalCall(BbkExpr.Call c) {
        if (!(c.target() instanceof BbkExpr.Identifier id)) throw new EvalException("sólo llamadas por nombre");
        String name = id.name();
        return switch (name) {
            case "len" -> (long) asText(eval(c.args().get(0))).length();
            case "upper" -> asText(eval(c.args().get(0))).toUpperCase();
            case "lower" -> asText(eval(c.args().get(0))).toLowerCase();
            case "trim" -> asText(eval(c.args().get(0))).strip();
            case "triml" -> asText(eval(c.args().get(0))).stripLeading();
            case "trimr" -> asText(eval(c.args().get(0))).stripTrailing();
            case "char" -> asText(eval(c.args().get(0)));
            case "int" -> toLong(eval(c.args().get(0)));
            case "float" -> toDouble(eval(c.args().get(0)));
            case "dec" -> toBigDecimal(eval(c.args().get(0)));
            case "abs" -> absOf(eval(c.args().get(0)));
            case "sqrt" -> Math.sqrt(toDouble(eval(c.args().get(0))));
            case "substr" -> substr(c.args());
            default -> throw new EvalException("no se puede invocar '" + name + "' al evaluar");
        };
    }

    private Object substr(List<BbkExpr> args) {
        String s = asText(eval(args.get(0)));
        int from = Math.max(0, (int) toLong(eval(args.get(1))) - 1);   // 1-based, estilo RPG
        if (args.size() >= 3) {
            int to = Math.min(s.length(), from + Math.max(0, (int) toLong(eval(args.get(2)))));
            return from <= to ? s.substring(from, to) : "";
        }
        return from <= s.length() ? s.substring(from) : "";
    }

    // ----- operaciones y modelo numérico -----

    private Object unary(BbkExpr.UnOp op, Object v) {
        return switch (op) {
            case POS -> v;
            case NEG -> negate(v);
            case NOT -> !truthy(v);
            case BIT_NOT -> ~toLong(v);
        };
    }

    private Object binary(Object l, BbkExpr.BinOp op, Object r) {
        return switch (op) {
            case ADD -> (l instanceof String || r instanceof String) ? asText(l) + asText(r) : arith(l, r, op);
            case SUB, MUL, DIV, MOD, POW -> arith(l, r, op);
            case EQ -> equalsValue(l, r);
            case NE -> !equalsValue(l, r);
            case LT -> compare(l, r) < 0;
            case GT -> compare(l, r) > 0;
            case LE -> compare(l, r) <= 0;
            case GE -> compare(l, r) >= 0;
            case AND -> truthy(l) && truthy(r);
            case OR -> truthy(l) || truthy(r);
            case BIT_AND -> toLong(l) & toLong(r);
            case BIT_OR -> toLong(l) | toLong(r);
            case BIT_XOR -> toLong(l) ^ toLong(r);
            case SHL -> toLong(l) << toLong(r);
            case SHR -> toLong(l) >> toLong(r);
        };
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
        throw new EvalException("división por cero");
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

    // ----- literales, star-idents, display -----

    private Object literal(BbkExpr.Literal l) {
        String t = l.text();
        return switch (l.kind()) {
            case INT -> Long.parseLong(t);
            case HEX -> Long.parseLong(stripPrefix(t), 16);
            case OCT -> Long.parseLong(stripPrefix(t), 8);
            case FLOAT -> Double.parseDouble(t);
            case DEC -> new BigDecimal(stripDecSuffix(t));
            case STRING -> unquote(t);
        };
    }

    private static String stripDecSuffix(String t) {
        if (!t.isEmpty()) {
            char last = t.charAt(t.length() - 1);
            if (last == 'd' || last == 'D') return t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static String stripPrefix(String t) {
        return t.length() > 2 ? t.substring(2) : t;   // 0x.. / 0o..
    }

    private static String unquote(String t) {
        if (t.length() >= 2 && (t.charAt(0) == '"' || t.charAt(0) == '\'')) {
            char q = t.charAt(0);
            return t.substring(1, t.length() - 1).replace("" + q + q, "" + q);
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
            default -> throw new EvalException("valor especial no soportado al evaluar: *" + name);
        };
    }

    private static String display(Object o) {
        if (o == null) return "*NULL";
        if (o instanceof Boolean b) return b ? "*ON" : "*OFF";
        if (o instanceof BigDecimal bd) return bd.toPlainString();
        return String.valueOf(o);
    }
}
