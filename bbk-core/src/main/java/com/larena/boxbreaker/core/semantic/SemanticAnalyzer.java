package com.larena.boxbreaker.core.semantic;

import com.larena.boxbreaker.core.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks a {@link BbkProgram} once and produces a {@link SemanticModel}: the type
 * of every expression, the resolved procedure signatures and constants, and a
 * list of {@link Diagnostic}s. Both back-ends consume the model instead of each
 * re-implementing name resolution and type inference.
 *
 * <p>This is pure analysis — it never emits code. The typing rules mirror what
 * the back-ends used to do inline (the numeric tower {@code INT < FLOAT <
 * DECIMAL}, {@code +} as string concatenation, the pure builtins, member/index
 * resolution), so existing programs keep their exact types; {@code **} is
 * canonically {@link Type#FLOAT} in both back-ends.
 */
public final class SemanticAnalyzer {

    /** A lexical scope: a chain of name &rarr; type bindings (module &rarr; procedure &rarr; block). */
    private static final class Scope {
        final Scope parent;
        final Map<String, Type> names = new HashMap<>();
        Scope(Scope parent) { this.parent = parent; }

        Type resolve(String n) {
            for (Scope s = this; s != null; s = s.parent) {
                Type t = s.names.get(n);
                if (t != null) return t;
            }
            return null;
        }
        void define(String n, Type t) { names.put(n, t); }
    }

    private final SemanticModel model = new SemanticModel();
    private final Map<String, List<BbkDeclaration.Subfield>> dsTemplates = new HashMap<>();
    private final Map<String, Type> constantTypes = new HashMap<>();
    private Scope module;

    public static SemanticModel analyze(BbkProgram program) {
        return new SemanticAnalyzer().run(program);
    }

    private SemanticModel run(BbkProgram program) {
        module = new Scope(null);

        // pass 1a: register all data structures first, so LIKEDS in proc signatures/params and
        // in globals resolves regardless of declaration (or cross-file) order
        for (BbkItem item : program.items()) {
            if (item instanceof BbkDeclaration.DataStructure d) registerDs(d.name(), d.subfields(), d.modifiers(), module);
        }
        // pass 1b: signatures, constants, module-level globals (forward refs resolve)
        for (BbkItem item : program.items()) {
            switch (item) {
                case BbkDeclaration.Procedure p -> model.putProcedure(p.name(), signatureOf(p));
                case BbkDeclaration.Constant c -> model.putConstant(c.name(), c.value());
                case BbkDeclaration.DataStructure ignored -> { }    // ya registrada en 1a
                case BbkDeclaration.Variable v -> module.define(v.name(), declaredType(v.type(), v.modifiers(), v.name()));
                default -> { }
            }
        }
        // type constant values (module scope) and remember each constant's type
        for (BbkItem item : program.items()) {
            if (item instanceof BbkDeclaration.Constant c) constantTypes.put(c.name(), typeExpr(c.value(), module));
        }

        // mainline + per-declaration initializers
        for (BbkItem item : program.items()) {
            switch (item) {
                case BbkDeclaration.Variable v -> typeInz(v.modifiers(), module);
                case BbkDeclaration.DataStructure d -> typeDsInz(resolveSubs(d), module);
                case BbkDeclaration.Procedure p -> analyzeProcedure(p);
                case BbkDeclaration ignored -> { }
                case BbkStatement s -> typeStatement(s, module);
            }
        }
        return model;
    }

    private void analyzeProcedure(BbkDeclaration.Procedure p) {
        Scope scope = new Scope(module);
        for (BbkDeclaration.Parameter par : p.params()) {
            scope.define(par.name(), declaredType(par.type(), par.modifiers(), par.name()));
        }
        for (BbkItem item : p.body()) typeItem(item, scope);
    }

    // -----------------------------------------------------------------------
    // Statements / items
    // -----------------------------------------------------------------------

    private void typeItem(BbkItem item, Scope scope) {
        switch (item) {
            case BbkDeclaration.Variable v -> { scope.define(v.name(), declaredType(v.type(), v.modifiers(), v.name())); typeInz(v.modifiers(), scope); }
            case BbkDeclaration.DataStructure d -> { registerDs(d.name(), d.subfields(), d.modifiers(), scope); typeDsInz(resolveSubs(d), scope); }
            case BbkDeclaration.Constant c -> { model.putConstant(c.name(), c.value()); constantTypes.put(c.name(), typeExpr(c.value(), scope)); }
            case BbkDeclaration ignored -> { }
            case BbkStatement s -> typeStatement(s, scope);
        }
    }

    private void typeStatement(BbkStatement s, Scope scope) {
        switch (s) {
            case BbkStatement.ExpressionStatement es -> typeExpr(es.expr(), scope);
            case BbkStatement.Assignment a -> typeAssignment(a, scope);
            case BbkStatement.If f -> { expectBool(typeExpr(f.condition(), scope), "if condition"); block(f.thenBody(), scope); block(f.elseBody(), scope); }
            case BbkStatement.Select sel -> { for (BbkStatement.When w : sel.whens()) { expectBool(typeExpr(w.condition(), scope), "when condition"); block(w.body(), scope); } block(sel.otherBody(), scope); }
            case BbkStatement.While w -> { expectBool(typeExpr(w.condition(), scope), "while condition"); block(w.body(), scope); }
            case BbkStatement.DoWhile d -> { block(d.body(), scope); expectBool(typeExpr(d.condition(), scope), "do-while condition"); }
            case BbkStatement.For f -> { if (f.init() != null) typeItem(f.init(), scope); if (f.condition() != null) expectBool(typeExpr(f.condition(), scope), "for condition"); if (f.update() != null) typeStatement(f.update(), scope); block(f.body(), scope); }
            case BbkStatement.Break b -> { }
            case BbkStatement.Continue c -> { }
            case BbkStatement.Return r -> { if (r.value() != null) typeExpr(r.value(), scope); }
            case BbkStatement.Monitor m -> { block(m.body(), scope); for (BbkStatement.OnError oe : m.onErrors()) block(oe.body(), scope); block(m.onExit(), scope); }
            case BbkStatement.Subroutine sub -> block(sub.body(), scope);
            case BbkStatement.Exsr e -> { }
            case BbkStatement.Leavesr l -> { }
            case BbkStatement.Callp cp -> typeExpr(cp.expr(), scope);
            case BbkStatement.Directive ignored -> { }
            case BbkStatement.FileOp f -> model.diagnose(Diagnostic.error("file operation '" + f.opcode() + "' is an IBM i I/O op (not supported by the back-ends)"));
        }
    }

    private void typeAssignment(BbkStatement.Assignment a, Scope scope) {
        Type value = typeExpr(a.value(), scope);
        BbkExpr target = a.target();
        if (target instanceof BbkExpr.Identifier id && scope.resolve(id.name()) == null && !model.isConstant(id.name())) {
            // auto-declared on first plain assignment (mirrors the back-ends)
            if (a.op() == BbkStatement.AssignOp.ASSIGN) {
                scope.define(id.name(), value);
                model.putType(target, value);
                return;
            }
            model.diagnose(Diagnostic.error("compound assignment to undeclared '" + id.name() + "'"));
        }
        Type t = typeExpr(target, scope);
        if (!t.isUnknown() && !value.isUnknown() && !assignable(value, t)) {
            model.diagnose(Diagnostic.warning("assigning " + show(value) + " to " + show(t)));
        }
    }

    private void block(List<BbkItem> items, Scope scope) { for (BbkItem i : items) typeItem(i, scope); }

    // -----------------------------------------------------------------------
    // Expressions
    // -----------------------------------------------------------------------

    private Type typeExpr(BbkExpr e, Scope scope) {
        Type t = compute(e, scope);
        model.putType(e, t);
        return t;
    }

    private Type compute(BbkExpr e, Scope scope) {
        return switch (e) {
            case BbkExpr.Identifier id -> identifier(id, scope);
            case BbkExpr.Literal lit -> literal(lit);
            case BbkExpr.BoolLit b -> Type.BOOL;
            case BbkExpr.NullLit n -> Type.STRING;
            case BbkExpr.StarIdent s -> starType(s.name());
            case BbkExpr.Unary u -> unary(u, scope);
            case BbkExpr.Binary b -> binary(b, scope);
            case BbkExpr.Ternary t -> ternary(t, scope);
            case BbkExpr.Call c -> call(c, scope);
            case BbkExpr.Index ix -> index(ix, scope);
            case BbkExpr.Member m -> member(m, scope);
        };
    }

    private Type identifier(BbkExpr.Identifier id, Scope scope) {
        Type t = scope.resolve(id.name());
        if (t != null) return t;
        Type c = constantTypes.get(id.name());
        if (c != null) return c;
        if (model.isConstant(id.name())) return Type.UNKNOWN;   // constant declared later; conservatively unknown
        model.diagnose(Diagnostic.error("undeclared name '" + id.name() + "'"));
        return Type.UNKNOWN;
    }

    private Type literal(BbkExpr.Literal lit) {
        return switch (lit.kind()) {
            case INT, HEX, OCT -> Type.INT;
            case FLOAT -> Type.FLOAT;
            case DEC -> Type.decimal(decimalsOf(lit.text()));
            case STRING -> Type.STRING;
        };
    }

    private Type unary(BbkExpr.Unary u, Scope scope) {
        Type operand = typeExpr(u.operand(), scope);
        return switch (u.op()) {
            case NOT -> Type.BOOL;
            case BIT_NOT -> Type.INT;
            case NEG, POS -> operand.isNumeric() ? operand : Type.UNKNOWN;
        };
    }

    private Type binary(BbkExpr.Binary b, Scope scope) {
        Type lt = typeExpr(b.left(), scope);
        Type rt = typeExpr(b.right(), scope);
        return switch (b.op()) {
            case AND, OR, EQ, NE, LT, GT, LE, GE -> Type.BOOL;
            case BIT_AND, BIT_OR, BIT_XOR, SHL, SHR -> Type.INT;
            case POW -> Type.FLOAT;
            case ADD -> (lt.is(Type.Kind.STRING) || rt.is(Type.Kind.STRING)) ? Type.STRING : Type.wider(lt, rt);
            default -> Type.wider(lt, rt);
        };
    }

    private Type ternary(BbkExpr.Ternary t, Scope scope) {
        expectBool(typeExpr(t.condition(), scope), "ternary condition");
        Type then = typeExpr(t.then(), scope);
        Type other = typeExpr(t.otherwise(), scope);
        if (!then.equals(other) && !then.isUnknown() && !other.isUnknown()) {
            model.diagnose(Diagnostic.warning("ternary branches differ in type (" + show(then) + " vs " + show(other) + ")"));
        }
        return then;
    }

    private Type call(BbkExpr.Call c, Scope scope) {
        for (BbkExpr arg : c.args()) typeExpr(arg, scope);
        if (!(c.target() instanceof BbkExpr.Identifier id)) {
            model.diagnose(Diagnostic.error("call target is not a name"));
            return Type.UNKNOWN;
        }
        String name = id.name();
        if (name.equals("print")) return Type.VOID;
        ProcSignature sig = model.signature(name);
        if (sig != null) {
            if (sig.arity() != c.args().size()) {
                model.diagnose(Diagnostic.error("procedure '" + name + "' expects " + sig.arity() + " args, got " + c.args().size()));
            }
            return sig.returnType();
        }
        return builtin(name, c.args(), scope);
    }

    private Type builtin(String name, List<BbkExpr> args, Scope scope) {
        return switch (name) {
            case "len", "int", "scan" -> Type.INT;
            case "substr", "trim", "triml", "trimr", "lower", "upper", "replace", "char" -> Type.STRING;
            case "dec" -> Type.decimal(2);
            case "float", "sqrt" -> Type.FLOAT;
            case "abs" -> args.isEmpty() ? Type.UNKNOWN : model.type(args.get(0));
            // ----- fechas (date runtime) -----
            case "date", "today" -> Type.DATE;
            case "time" -> Type.TIME;
            case "timestamp", "now" -> Type.TIMESTAMP;
            case "year", "month", "day", "hour", "minute", "second", "diffdays", "diffseconds" -> Type.INT;
            case "adddays", "addmonths", "addyears", "addhours", "addminutes", "addseconds"
                -> args.isEmpty() ? Type.UNKNOWN : model.type(args.get(0));   // misma fecha/hora que el argumento
            default -> { model.diagnose(Diagnostic.error("unknown function '" + name + "'")); yield Type.UNKNOWN; }
        };
    }

    private Type index(BbkExpr.Index ix, Scope scope) {
        Type target = typeExpr(ix.target(), scope);
        for (BbkExpr i : ix.indices()) typeExpr(i, scope);
        if (target instanceof Type.Array a) return a.element();
        if (target instanceof Type.Ds ds && ds.array()) return new Type.Ds(ds.name(), ds.fields(), false, 0);
        if (!target.isUnknown()) model.diagnose(Diagnostic.error("indexing a non-array (" + show(target) + ")"));
        return Type.UNKNOWN;
    }

    private Type member(BbkExpr.Member m, Scope scope) {
        Type target = typeExpr(m.target(), scope);
        if (target instanceof Type.Ds ds) {
            if (ds.hasField(m.field())) return ds.fieldType(m.field());
            model.diagnose(Diagnostic.error("data structure '" + ds.name() + "' has no field '" + m.field() + "'"));
            return Type.UNKNOWN;
        }
        if (!target.isUnknown()) model.diagnose(Diagnostic.error("member access '." + m.field() + "' on a non-data-structure"));
        return Type.UNKNOWN;
    }

    // -----------------------------------------------------------------------
    // Declarations / types
    // -----------------------------------------------------------------------

    private ProcSignature signatureOf(BbkDeclaration.Procedure p) {
        List<Type> types = new ArrayList<>();
        List<Boolean> arrays = new ArrayList<>();
        for (BbkDeclaration.Parameter par : p.params()) {
            if (par.type() instanceof BbkType.Like like && like.kind() == BbkType.LikeKind.LIKEDS) {
                types.add(dsTypeFromTemplate(par.name(), like.name(), false, 0));    // DS-por-valor: el tipo lleva el layout
                arrays.add(false);
            } else {
                types.add(scalarType(par.type()));
                arrays.add(isArray(par.modifiers()));
            }
        }
        Type ret;
        if (p.returnType() instanceof BbkType.Like rlike && rlike.kind() == BbkType.LikeKind.LIKEDS) {
            ret = dsTypeFromTemplate(p.name() + "_ret", rlike.name(), false, 0);
        } else {
            ret = p.returnType() == null ? Type.VOID : scalarType(p.returnType());
        }
        return new ProcSignature(types, arrays, ret);
    }

    /** The declared type of a variable/parameter: scalar, array (DIM), or data structure (LIKEDS). */
    private Type declaredType(BbkType type, List<BbkModifier> modifiers, String forName) {
        if (type instanceof BbkType.Like like && like.kind() == BbkType.LikeKind.LIKEDS) {
            return dsTypeFromTemplate(forName, like.name(), false, 0);
        }
        Type scalar = scalarType(type);
        if (isArray(modifiers)) return new Type.Array(scalar, dimSize(modifiers));
        return scalar;
    }

    private Type scalarType(BbkType type) {
        if (type instanceof BbkType.Primitive p) {
            return switch (p.name().toUpperCase()) {
                case "INT", "UNS" -> Type.INT;
                case "FLOAT" -> Type.FLOAT;
                case "PACKED", "ZONED", "BINDEC" -> Type.decimal(p.decimals() == null ? 0 : p.decimals());
                case "BOOL", "IND" -> Type.BOOL;
                case "CHAR", "VARCHAR" -> Type.STRING;
                case "DATE" -> Type.DATE;
                case "TIME" -> Type.TIME;
                case "TIMESTAMP" -> Type.TIMESTAMP;
                case "POINTER" -> { model.diagnose(Diagnostic.error("pointers are not supported by the back-ends")); yield Type.UNKNOWN; }
                default -> { model.diagnose(Diagnostic.error("unsupported type '" + p.name() + "'")); yield Type.UNKNOWN; }
            };
        }
        model.diagnose(Diagnostic.error("type " + ((BbkType.Like) type).kind() + " is not a scalar here"));
        return Type.UNKNOWN;
    }

    private void registerDs(String dsName, List<BbkDeclaration.Subfield> subs, List<BbkModifier> mods, Scope scope) {
        String like = likedsTemplate(mods);
        if (subs.isEmpty() && like != null) subs = template(like);
        dsTemplates.put(dsName, subs);
        if (hasMod(mods, "TEMPLATE")) return;
        boolean qualified = like != null || hasMod(mods, "QUALIFIED") || hasMod(mods, "QUALI");
        boolean array = isArray(mods);
        int dim = array ? dimSize(mods) : 0;

        List<Type.Ds.Field> fields = new ArrayList<>();
        for (BbkDeclaration.Subfield sub : subs) {
            Type ft = scalarType(sub.type());
            fields.add(new Type.Ds.Field(sub.name(), ft));
            if (!qualified) scope.define(sub.name(), ft);
        }
        scope.define(dsName, new Type.Ds(dsName, fields, array, dim));
    }

    private Type dsTypeFromTemplate(String dsName, String templateName, boolean array, int dim) {
        List<BbkDeclaration.Subfield> subs = template(templateName);
        dsTemplates.put(dsName, subs);
        List<Type.Ds.Field> fields = new ArrayList<>();
        for (BbkDeclaration.Subfield sub : subs) fields.add(new Type.Ds.Field(sub.name(), scalarType(sub.type())));
        return new Type.Ds(dsName, fields, array, dim);
    }

    private void typeInz(List<BbkModifier> modifiers, Scope scope) {
        BbkExpr inz = inzValue(modifiers);
        if (inz != null) typeExpr(inz, scope);
    }

    private void typeDsInz(List<BbkDeclaration.Subfield> subs, Scope scope) {
        for (BbkDeclaration.Subfield sub : subs) typeInz(sub.modifiers(), scope);
    }

    private List<BbkDeclaration.Subfield> resolveSubs(BbkDeclaration.DataStructure d) {
        if (!d.subfields().isEmpty()) return d.subfields();
        String like = likedsTemplate(d.modifiers());
        return like != null ? template(like) : d.subfields();
    }

    // -----------------------------------------------------------------------
    // Special values / helpers
    // -----------------------------------------------------------------------

    private Type starType(String name) {
        String n = (name.startsWith("*") ? name.substring(1) : name).toUpperCase();
        return switch (n) {
            case "ON", "OFF" -> Type.BOOL;
            case "ZERO", "ZEROS" -> Type.INT;
            case "BLANK", "BLANKS", "NULL" -> Type.STRING;
            default -> { model.diagnose(Diagnostic.error("special value '*" + n + "' is not supported")); yield Type.UNKNOWN; }
        };
    }

    private void expectBool(Type t, String where) {
        if (!t.isUnknown() && !t.is(Type.Kind.BOOL)) model.diagnose(Diagnostic.warning(where + " is not boolean (" + show(t) + ")"));
    }

    private static boolean assignable(Type from, Type to) {
        if (from.equals(to)) return true;
        return from.isNumeric() && to.isNumeric();      // the back-ends coerce across the numeric tower
    }

    private List<BbkDeclaration.Subfield> template(String name) {
        List<BbkDeclaration.Subfield> tpl = dsTemplates.get(name);
        if (tpl == null) {
            model.diagnose(Diagnostic.error("LIKEDS(" + name + ") — data structure not declared before use"));
            return List.of();
        }
        return tpl;
    }

    private static String likedsTemplate(List<BbkModifier> mods) {
        for (BbkModifier m : mods) {
            if (m.name().equalsIgnoreCase("LIKEDS") && m.args().size() == 1 && m.args().get(0) instanceof BbkExpr.Identifier id) return id.name();
        }
        return null;
    }

    private static boolean hasMod(List<BbkModifier> mods, String name) {
        for (BbkModifier m : mods) if (m.name().equalsIgnoreCase(name)) return true;
        return false;
    }

    private static boolean isArray(List<BbkModifier> mods) { return hasMod(mods, "DIM"); }

    private static int dimSize(List<BbkModifier> mods) {
        for (BbkModifier m : mods) {
            if (m.name().equalsIgnoreCase("DIM") && m.args().size() == 1 && m.args().get(0) instanceof BbkExpr.Literal lit) return Integer.parseInt(lit.text());
        }
        return 0;
    }

    private static BbkExpr inzValue(List<BbkModifier> modifiers) {
        for (BbkModifier m : modifiers) if (m.name().equalsIgnoreCase("INZ") && m.args().size() == 1) return m.args().get(0);
        return null;
    }

    private static int decimalsOf(String text) {
        String t = (text.endsWith("d") || text.endsWith("D")) ? text.substring(0, text.length() - 1) : text;
        int dot = t.indexOf('.');
        return dot < 0 ? 0 : t.length() - dot - 1;
    }

    private static String show(Type t) {
        if (t instanceof Type.Scalar s) return s.kind() == Type.Kind.DECIMAL ? "DECIMAL(" + s.scale() + ")" : s.kind().toString();
        if (t instanceof Type.Array a) return show(a.element()) + "[" + a.dim() + "]";
        if (t instanceof Type.Ds ds) return "DS " + ds.name();
        return t == Type.VOID ? "VOID" : "?";
    }
}
