package com.larena.boxbreaker.core.backend.c;

import com.larena.boxbreaker.core.ast.*;
import com.larena.boxbreaker.core.semantic.ProcSignature;
import com.larena.boxbreaker.core.semantic.SemanticAnalyzer;
import com.larena.boxbreaker.core.semantic.SemanticModel;
import com.larena.boxbreaker.core.semantic.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiles a BBK AST to C source text — the second back-end, sharing the same
 * AST as {@link com.larena.boxbreaker.core.backend.jvm.JvmCompiler}.
 *
 * <p>At parity with the JVM back-end for the non-OS language:
 * scalars ({@code INT}&rarr;{@code long long}, {@code FLOAT}&rarr;{@code double},
 * {@code PACKED/ZONED/BINDEC}&rarr;{@code long double} rounded to the declared
 * scale, {@code BOOL}&rarr;{@code int}, {@code CHAR/VARCHAR}&rarr;
 * {@code const char*}); all operators incl. {@code **} and {@code +} string
 * concatenation; {@code if/select/while/do-while/for}; constants; special
 * values; arrays (incl. arrays of DS); data structures
 * ({@code DCL-DS/TEMPLATE/LIKEDS}); procedures ({@code DCL-PROC}, with
 * {@code CTL-OPT MAIN}); subroutines ({@code BEGSR/EXSR/LEAVESR}); the pure
 * builtins; and {@code monitor} (via {@code setjmp}; integer divide-by-zero is
 * trapped — C cannot portably catch other arithmetic traps).
 *
 * <p>A small self-contained C prelude provides the string/decimal/monitor
 * helpers (no external library — see architecture decision on decimals). The OS
 * surface ({@code DCL-F}/{@code FileOp}, {@code EXTPGM}/{@code EXTPROC}),
 * {@code OVERLAY}, dates and pointers raise a clear
 * {@link UnsupportedOperationException}.
 */
public final class CCompiler {

    /** C-level type a scalar BBK value lowers to. */
    private enum Ct { LONG, DOUBLE, BOOL, STRING, DECIMAL }

    /** An emitted expression: its C text, type and (for decimals) display scale. */
    private record Cx(String code, Ct type, int scale) {
        Cx(String code, Ct type) { this(code, type, 0); }
    }

    private record Binding(Ct type, boolean array, int dim, int scale, String cname) {}

    /** A procedure parameter as lowered to C: a scalar/array, or a DS flattened to its subfields. */
    private interface Param {}
    private record ScalarParam(Ct ct, boolean array, int scale) implements Param {}
    private record DsParam(List<DsField> fields) implements Param {}
    private record DsField(String subName, Ct ct, int scale) {}
    private record Sig(List<Param> params, Ct ret) {}

    private static final String PRELUDE = """
        #define __USE_MINGW_ANSI_STDIO 1
        #include <stdio.h>
        #include <stdlib.h>
        #include <string.h>
        #include <ctype.h>
        #include <math.h>
        #include <setjmp.h>
        #include <time.h>

        static jmp_buf bbk_jb[64];
        static int bbk_jsp = 0;

        static long long bbk_div(long long a, long long b) {
            if (b == 0 && bbk_jsp > 0) longjmp(bbk_jb[bbk_jsp - 1], 1);
            return a / b;
        }
        static long long bbk_mod(long long a, long long b) {
            if (b == 0 && bbk_jsp > 0) longjmp(bbk_jb[bbk_jsp - 1], 1);
            return a % b;
        }
        static char* bbk_cat(const char* a, const char* b) {
            size_t la = strlen(a), lb = strlen(b);
            char* r = (char*) malloc(la + lb + 1);
            memcpy(r, a, la); memcpy(r + la, b, lb + 1);
            return r;
        }
        static char* bbk_sll(long long v) { char* r = (char*) malloc(24); snprintf(r, 24, "%lld", v); return r; }
        static char* bbk_sd(double v) { char* r = (char*) malloc(32); snprintf(r, 32, "%g", v); return r; }
        static char* bbk_sdec(long double v, int sc) { char* r = (char*) malloc(48); snprintf(r, 48, "%.*Lf", sc, v); return r; }
        static char* bbk_substr(const char* s, long long st, long long ln) {
            long long n = (long long) strlen(s), b = st - 1;
            if (b < 0) b = 0; if (b > n) b = n;
            if (ln < 0) ln = 0; if (b + ln > n) ln = n - b;
            char* r = (char*) malloc((size_t) ln + 1);
            memcpy(r, s + b, (size_t) ln); r[ln] = '\\0'; return r;
        }
        static char* bbk_trim(const char* s) {
            const char* a = s; while (*a == ' ') a++;
            const char* z = s + strlen(s); while (z > a && z[-1] == ' ') z--;
            size_t n = (size_t)(z - a); char* r = (char*) malloc(n + 1);
            memcpy(r, a, n); r[n] = '\\0'; return r;
        }
        static char* bbk_trimr(const char* s) {
            const char* z = s + strlen(s); while (z > s && z[-1] == ' ') z--;
            size_t n = (size_t)(z - s); char* r = (char*) malloc(n + 1);
            memcpy(r, s, n); r[n] = '\\0'; return r;
        }
        static char* bbk_triml(const char* s) { const char* a = s; while (*a == ' ') a++; return bbk_cat(a, ""); }
        static char* bbk_lower(const char* s) { size_t n = strlen(s); char* r = (char*) malloc(n + 1); for (size_t i = 0; i < n; i++) r[i] = (char) tolower((unsigned char) s[i]); r[n] = '\\0'; return r; }
        static char* bbk_upper(const char* s) { size_t n = strlen(s); char* r = (char*) malloc(n + 1); for (size_t i = 0; i < n; i++) r[i] = (char) toupper((unsigned char) s[i]); r[n] = '\\0'; return r; }
        static long long bbk_scan(const char* needle, const char* hay) { const char* p = strstr(hay, needle); return p ? (long long)(p - hay + 1) : 0; }
        static char* bbk_replace(const char* s, const char* a, const char* b) {
            size_t la = strlen(a); if (la == 0) return bbk_cat(s, "");
            size_t lb = strlen(b), n = 0; const char* p = s;
            while ((p = strstr(p, a)) != NULL) { n++; p += la; }
            char* r = (char*) malloc(strlen(s) + n * (lb > la ? lb - la : 0) + 1);
            char* o = r; const char* q; p = s;
            while ((q = strstr(p, a)) != NULL) { memcpy(o, p, (size_t)(q - p)); o += q - p; memcpy(o, b, lb); o += lb; p = q + la; }
            strcpy(o, p); return r;
        }
        static long double bbk_round(long double v, int sc) { long double f = powl(10.0L, sc); return roundl(v * f) / f; }
        static long double bbk_truncs(long double v, int sc) {
            long double f = powl(10.0L, sc), scaled = v * f, r = roundl(scaled);
            if (fabsl(scaled - r) < 1e-6L) scaled = r;   // snap away binary-rep noise near an integer
            return truncl(scaled) / f;
        }

        /* --- date runtime: epoch-day / seconds, calendario civil (mismos resultados que java.time) --- */
        static long long bbk_dfc(long long y, unsigned m, unsigned d) {        /* days_from_civil */
            y -= m <= 2;
            long long era = (y >= 0 ? y : y - 399) / 400;
            unsigned yoe = (unsigned)(y - era * 400);
            unsigned doy = (153u * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
            unsigned doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
            return era * 146097 + (long long)doe - 719468;
        }
        static void bbk_cfd(long long z, long long* y, unsigned* m, unsigned* d) {   /* civil_from_days */
            z += 719468;
            long long era = (z >= 0 ? z : z - 146096) / 146097;
            unsigned doe = (unsigned)(z - era * 146097);
            unsigned yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
            long long yy = (long long)yoe + era * 400;
            unsigned doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
            unsigned mp = (5 * doy + 2) / 153;
            *d = doy - (153 * mp + 2) / 5 + 1;
            *m = mp < 10 ? mp + 3 : mp - 9;
            *y = yy + (*m <= 2);
        }
        static long long bbk_fdiv(long long a, long long b) { long long q = a / b; if ((a % b != 0) && ((a < 0) != (b < 0))) q--; return q; }
        static long long bbk_fmodll(long long a, long long b) { long long r = a % b; if (r != 0 && ((r < 0) != (b < 0))) r += b; return r; }
        static unsigned bbk_lastday(long long y, unsigned m) {
            static const unsigned dim[] = {31,28,31,30,31,30,31,31,30,31,30,31};
            if (m == 2) return (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) ? 29 : 28;
            return dim[m - 1];
        }
        static long long bbk_date_parse(const char* s) { long long y; unsigned m, d; sscanf(s, "%lld-%u-%u", &y, &m, &d); return bbk_dfc(y, m, d); }
        static char* bbk_date_str(long long ed) { long long y; unsigned m, d; bbk_cfd(ed, &y, &m, &d); char* r = (char*) malloc(16); snprintf(r, 16, "%04lld-%02u-%02u", y, m, d); return r; }
        static long long bbk_date_today(void) { return bbk_fdiv((long long) time(NULL), 86400); }
        static long long bbk_date_addmonths(long long ed, long long n) {
            long long y; unsigned m, d; bbk_cfd(ed, &y, &m, &d);
            long long tot = y * 12 + (long long)(m - 1) + n, ny = bbk_fdiv(tot, 12);
            unsigned nm = (unsigned)(tot - ny * 12) + 1, ld = bbk_lastday(ny, nm);
            if (d > ld) d = ld;
            return bbk_dfc(ny, nm, d);
        }
        static long long bbk_date_addyears(long long ed, long long n) {
            long long y; unsigned m, d; bbk_cfd(ed, &y, &m, &d);
            y += n; unsigned ld = bbk_lastday(y, m); if (d > ld) d = ld;
            return bbk_dfc(y, m, d);
        }
        static long long bbk_date_year(long long ed) { long long y; unsigned m, d; bbk_cfd(ed, &y, &m, &d); return y; }
        static long long bbk_date_month(long long ed) { long long y; unsigned m, d; bbk_cfd(ed, &y, &m, &d); return m; }
        static long long bbk_date_day(long long ed) { long long y; unsigned m, d; bbk_cfd(ed, &y, &m, &d); return d; }
        static long long bbk_time_parse(const char* s) { unsigned h, mi, se; sscanf(s, "%u:%u:%u", &h, &mi, &se); return (long long) h * 3600 + mi * 60 + se; }
        static char* bbk_time_str(long long s) { s = bbk_fmodll(s, 86400); char* r = (char*) malloc(12); snprintf(r, 12, "%02lld:%02lld:%02lld", s / 3600, (s / 60) % 60, s % 60); return r; }
        static long long bbk_time_add(long long s, long long delta) { return bbk_fmodll(s + delta, 86400); }
        static long long bbk_time_hour(long long s) { return bbk_fmodll(s, 86400) / 3600; }
        static long long bbk_time_minute(long long s) { return (bbk_fmodll(s, 86400) / 60) % 60; }
        static long long bbk_time_second(long long s) { return bbk_fmodll(s, 86400) % 60; }
        static long long bbk_ts_parse(const char* s) { long long y; unsigned mo, d, h, mi, se; sscanf(s, "%lld-%u-%uT%u:%u:%u", &y, &mo, &d, &h, &mi, &se); return bbk_dfc(y, mo, d) * 86400 + (long long) h * 3600 + mi * 60 + se; }
        static char* bbk_ts_str(long long es) { long long ed = bbk_fdiv(es, 86400), tod = bbk_fmodll(es, 86400), y; unsigned mo, d; bbk_cfd(ed, &y, &mo, &d); char* r = (char*) malloc(24); snprintf(r, 24, "%04lld-%02u-%02uT%02lld:%02lld:%02lld", y, mo, d, tod / 3600, (tod / 60) % 60, tod % 60); return r; }
        static long long bbk_ts_now(void) { return (long long) time(NULL); }
        static long long bbk_ts_addmonths(long long es, long long n) { return bbk_date_addmonths(bbk_fdiv(es, 86400), n) * 86400 + bbk_fmodll(es, 86400); }
        static long long bbk_ts_addyears(long long es, long long n) { return bbk_date_addyears(bbk_fdiv(es, 86400), n) * 86400 + bbk_fmodll(es, 86400); }
        static long long bbk_ts_year(long long es) { return bbk_date_year(bbk_fdiv(es, 86400)); }
        static long long bbk_ts_month(long long es) { return bbk_date_month(bbk_fdiv(es, 86400)); }
        static long long bbk_ts_day(long long es) { return bbk_date_day(bbk_fdiv(es, 86400)); }
        static long long bbk_ts_hour(long long es) { return bbk_fmodll(es, 86400) / 3600; }
        static long long bbk_ts_minute(long long es) { return (bbk_fmodll(es, 86400) / 60) % 60; }
        static long long bbk_ts_second(long long es) { return bbk_fmodll(es, 86400) % 60; }
        """;

    private SemanticModel model;                 // shared name resolution + types (single source of truth)
    private final StringBuilder out = new StringBuilder();
    private final Map<String, Binding> globals = new LinkedHashMap<>();
    private final Map<String, Sig> procs = new LinkedHashMap<>();
    private final Map<String, List<BbkDeclaration.Subfield>> dsTemplates = new HashMap<>();
    private final List<String> globalDecls = new ArrayList<>();
    private String mainProc;

    private Map<String, Binding> locals;
    private Map<String, List<BbkItem>> subroutines;
    private Deque<String> leavesr;
    private int srCounter;
    private Ct currentReturn;
    private int indent = 1;

    public static String compile(BbkProgram program) {
        return new CCompiler().build(program);
    }

    private String build(BbkProgram program) {
        model = SemanticAnalyzer.analyze(program);      // resolve names + types once, up front
        model.procedures().forEach((name, sig) -> procs.put(name, toSig(sig)));
        for (BbkItem item : program.items()) {
            switch (item) {
                case BbkDeclaration.DataStructure d -> registerDs(d.name(), d.subfields(), d.modifiers());
                case BbkDeclaration.Variable v -> registerGlobalVar(v);
                case BbkDeclaration.CtlOpt ctl -> { String m = mainOf(ctl); if (m != null) mainProc = m; }
                default -> { }
            }
        }

        out.append(PRELUDE).append('\n');
        for (Map.Entry<String, Sig> e : procs.entrySet()) {
            out.append("static ").append(prototype(e.getKey(), e.getValue())).append(";\n");
        }
        if (!procs.isEmpty()) out.append('\n');
        for (String d : globalDecls) out.append(d).append('\n');
        if (!globalDecls.isEmpty()) out.append('\n');
        for (BbkItem item : program.items()) {
            if (item instanceof BbkDeclaration.Procedure p) emitProcedure(p);
        }

        out.append("int main(void) {\n");
        indent = 1;
        locals = new HashMap<>();
        subroutines = new HashMap<>();
        leavesr = new ArrayDeque<>();
        collectSubroutines(program.items());
        for (BbkItem item : program.items()) {
            switch (item) {
                case BbkDeclaration.Variable v -> initGlobalVar(v);
                case BbkDeclaration.DataStructure d -> initGlobalDs(d);
                case BbkDeclaration ignored -> { }
                case BbkStatement s -> { if (mainProc == null) statement(s); }
            }
        }
        if (mainProc != null) line(mainProc + "();");
        out.append("  return 0;\n}\n");
        return out.toString();
    }

    private void emitProcedure(BbkDeclaration.Procedure p) {
        Sig sig = procs.get(p.name());
        out.append("static ").append(prototype(p.name(), p, sig)).append(" {\n");
        indent = 1;
        locals = new HashMap<>();
        subroutines = new HashMap<>();
        leavesr = new ArrayDeque<>();
        currentReturn = sig.ret();
        for (int i = 0; i < p.params().size(); i++) {
            BbkDeclaration.Parameter par = p.params().get(i);
            Param param = sig.params().get(i);
            if (param instanceof ScalarParam s) {
                locals.put(par.name(), new Binding(s.ct(), s.array(), 0, s.scale(), par.name()));
            } else {
                for (DsField f : ((DsParam) param).fields()) {
                    locals.put(par.name() + "." + f.subName(), new Binding(f.ct(), false, 0, f.scale(), par.name() + "_" + f.subName()));
                }
            }
        }
        collectSubroutines(p.body());
        for (BbkItem item : p.body()) bodyItem(item);
        if (sig.ret() != null) line("return " + defaultValue(sig.ret()) + ";");
        out.append("}\n\n");
        currentReturn = null;
    }

    private void collectSubroutines(List<BbkItem> items) {
        for (BbkItem item : items) if (item instanceof BbkStatement.Subroutine s) subroutines.put(s.name(), s.body());
    }

    // -----------------------------------------------------------------------
    // Items / statements
    // -----------------------------------------------------------------------

    private void bodyItem(BbkItem item) {
        switch (item) {
            case BbkDeclaration.Variable v -> declareLocalVar(v);
            case BbkDeclaration.DataStructure d -> { registerDs(d.name(), d.subfields(), d.modifiers()); declareLocalDs(d); }
            case BbkDeclaration.Constant c -> { }       // constants are inlined; the shared model holds them
            case BbkDeclaration ignored -> { }
            case BbkStatement s -> statement(s);
        }
    }

    private void statement(BbkStatement s) {
        switch (s) {
            case BbkStatement.ExpressionStatement es -> line(exprStatement(es.expr()) + ";");
            case BbkStatement.Assignment a -> line(assignInline(a) + ";");
            case BbkStatement.If f -> ifStmt(f);
            case BbkStatement.Select sel -> select(sel);
            case BbkStatement.While w -> { line("while (" + cond(w.condition()) + ") {"); body(w.body()); line("}"); }
            case BbkStatement.DoWhile d -> { line("do {"); body(d.body()); line("} while (" + cond(d.condition()) + ");"); }
            case BbkStatement.For f -> forStmt(f);
            case BbkStatement.Break b -> line("break;");
            case BbkStatement.Continue c -> line("continue;");
            case BbkStatement.Return r -> returnStmt(r);
            case BbkStatement.Monitor m -> monitor(m);
            case BbkStatement.Subroutine ignored -> { }
            case BbkStatement.Exsr e -> exsr(e);
            case BbkStatement.Leavesr l -> line("goto " + leavesrTarget() + ";");
            case BbkStatement.Callp cp -> line(exprStatement(cp.expr()) + ";");
            case BbkStatement.Directive ignored -> { }
            case BbkStatement.FileOp f -> throw unsupported("file operation '" + f.opcode() + "' (IBM i I/O)");
        }
    }

    private void ifStmt(BbkStatement.If f) {
        line("if (" + cond(f.condition()) + ") {");
        body(f.thenBody());
        List<BbkItem> elseB = f.elseBody();
        while (elseB.size() == 1 && elseB.get(0) instanceof BbkStatement.If nested) {
            line("} else if (" + cond(nested.condition()) + ") {");
            body(nested.thenBody());
            elseB = nested.elseBody();
        }
        if (!elseB.isEmpty()) { line("} else {"); body(elseB); }
        line("}");
    }

    private void select(BbkStatement.Select sel) {
        boolean first = true;
        for (BbkStatement.When w : sel.whens()) {
            line((first ? "if (" : "} else if (") + cond(w.condition()) + ") {");
            body(w.body());
            first = false;
        }
        if (first) {                                   // no whens: just the other-body
            body(sel.otherBody());
            return;
        }
        if (!sel.otherBody().isEmpty()) { line("} else {"); body(sel.otherBody()); }
        line("}");
    }

    private void forStmt(BbkStatement.For f) {
        String init = f.init() == null ? "" : inlineInit(f.init());
        String c = f.condition() == null ? "" : cond(f.condition());
        String upd = f.update() == null ? "" : inlineStmt(f.update());
        line("for (" + init + "; " + c + "; " + upd + ") {");
        body(f.body());
        line("}");
    }

    private void monitor(BbkStatement.Monitor m) {
        if (m.body().isEmpty()) { body(m.onExit()); return; }
        line("if (setjmp(bbk_jb[bbk_jsp++]) == 0) {");
        body(m.body());
        indent++; line("--bbk_jsp;"); indent--;
        bodyInner(m.onExit());
        line("} else {");
        indent++; line("--bbk_jsp;"); indent--;
        if (!m.onErrors().isEmpty()) bodyInner(m.onErrors().get(0).body());
        bodyInner(m.onExit());
        line("}");
    }

    private void exsr(BbkStatement.Exsr e) {
        List<BbkItem> sub = subroutines.get(e.name());
        if (sub == null) throw unsupported("call to undefined subroutine '" + e.name() + "'");
        String label = "bbk_sr_" + e.name() + "_" + (srCounter++);
        leavesr.push(label);
        line("{");
        body(sub);
        indent++; line(label + ": ;"); indent--;
        line("}");
        leavesr.pop();
    }

    private String leavesrTarget() {
        if (leavesr.isEmpty()) throw unsupported("LEAVESR outside a subroutine");
        return leavesr.peek();
    }

    private void returnStmt(BbkStatement.Return r) {
        if (r.value() == null || currentReturn == null) { line("return" + (currentReturn == null ? "" : " " + defaultValue(currentReturn)) + ";"); return; }
        line("return " + coerce(expr(r.value()), currentReturn) + ";");
    }

    private void body(List<BbkItem> items) { indent++; for (BbkItem i : items) bodyItem(i); indent--; }

    /** Emit items at the current indent (already inside a brace block opened by the caller). */
    private void bodyInner(List<BbkItem> items) { indent++; for (BbkItem i : items) bodyItem(i); indent--; }

    private String cond(BbkExpr e) { return coerce(expr(e), Ct.BOOL); }

    // -----------------------------------------------------------------------
    // Declarations / assignments
    // -----------------------------------------------------------------------

    private void registerGlobalVar(BbkDeclaration.Variable v) {
        if (v.type() instanceof BbkType.Like like && like.kind() == BbkType.LikeKind.LIKEDS) {
            registerDsFrom(v.name(), template(like.name()), true);
            return;
        }
        Ct ct = ct(v.type());
        boolean array = isArray(v.modifiers());
        int dim = array ? dimSize(v.modifiers()) : 0;
        globals.put(v.name(), new Binding(ct, array, dim, scaleOf(v.type()), v.name()));
        globalDecls.add("static " + ctype(ct) + " " + v.name() + (array ? "[" + dim + "]" : "") + ";");
    }

    private void registerDs(String dsName, List<BbkDeclaration.Subfield> subs, List<BbkModifier> mods) {
        String like = likedsTemplate(mods);
        if (subs.isEmpty() && like != null) subs = template(like);
        dsTemplates.put(dsName, subs);
        if (hasMod(mods, "TEMPLATE")) return;
        boolean dsArray = isArray(mods);
        int dim = dsArray ? dimSize(mods) : 0;
        boolean qualified = like != null || hasMod(mods, "QUALIFIED") || hasMod(mods, "QUALI");
        boolean global = locals == null;
        for (BbkDeclaration.Subfield sub : subs) {
            if (hasMod(sub.modifiers(), "OVERLAY")) throw unsupported("OVERLAY (memory aliasing) on '" + sub.name() + "'");
            if (isArray(sub.modifiers())) throw unsupported("array subfield '" + sub.name() + "'");
            Ct ct = ct(sub.type());
            String cname = dsName + "_" + sub.name();
            Binding b = new Binding(ct, dsArray, dim, scaleOf(sub.type()), cname);
            (global ? globals : locals).put(dsName + "." + sub.name(), b);
            if (!qualified) (global ? globals : locals).put(sub.name(), b);
            if (global) globalDecls.add("static " + ctype(ct) + " " + cname + (dsArray ? "[" + dim + "]" : "") + ";");
        }
    }

    private void registerDsFrom(String dsName, List<BbkDeclaration.Subfield> subs, boolean global) {
        registerDs(dsName, subs, List.of(BbkModifier.bare("QUALIFIED")));
    }

    private void initGlobalVar(BbkDeclaration.Variable v) {
        if (v.type() instanceof BbkType.Like like && like.kind() == BbkType.LikeKind.LIKEDS) { initCloneSubfields(v.name(), like.name()); return; }
        Binding b = globals.get(v.name());
        if (b.array()) return;                                   // C zero-inits file-scope arrays
        line(v.name() + " = " + initValue(b, v.modifiers()) + ";");
    }

    private void declareLocalVar(BbkDeclaration.Variable v) {
        if (v.type() instanceof BbkType.Like like && like.kind() == BbkType.LikeKind.LIKEDS) {
            registerDsFrom(v.name(), template(like.name()), false);
            declareCloneSubfields(v.name(), like.name());
            return;
        }
        Ct ct = ct(v.type());
        boolean array = isArray(v.modifiers());
        int dim = array ? dimSize(v.modifiers()) : 0;
        Binding b = new Binding(ct, array, dim, scaleOf(v.type()), v.name());
        locals.put(v.name(), b);
        if (array) line(ctype(ct) + " " + v.name() + "[" + dim + "] = {0};");
        else line(ctype(ct) + " " + v.name() + " = " + initValue(b, v.modifiers()) + ";");
    }

    private void initGlobalDs(BbkDeclaration.DataStructure d) {
        if (hasMod(d.modifiers(), "TEMPLATE")) return;
        for (BbkDeclaration.Subfield sub : resolveSubs(d)) {
            Binding b = lookup(d.name() + "." + sub.name());
            if (!b.array()) line(b.cname() + " = " + initSubfieldValue(b, sub) + ";");
        }
    }

    private void declareLocalDs(BbkDeclaration.DataStructure d) {
        if (hasMod(d.modifiers(), "TEMPLATE")) return;
        for (BbkDeclaration.Subfield sub : resolveSubs(d)) {
            Binding b = lookup(d.name() + "." + sub.name());
            if (b.array()) line(ctype(b.type()) + " " + b.cname() + "[" + b.dim() + "] = {0};");
            else line(ctype(b.type()) + " " + b.cname() + " = " + initSubfieldValue(b, sub) + ";");
        }
    }

    private void initCloneSubfields(String dsName, String templateName) {
        for (BbkDeclaration.Subfield sub : template(templateName)) {
            Binding b = lookup(dsName + "." + sub.name());
            line(b.cname() + " = " + defaultScaled(b) + ";");
        }
    }

    private void declareCloneSubfields(String dsName, String templateName) {
        for (BbkDeclaration.Subfield sub : template(templateName)) {
            Binding b = lookup(dsName + "." + sub.name());
            line(ctype(b.type()) + " " + b.cname() + " = " + defaultScaled(b) + ";");
        }
    }

    private String initValue(Binding b, List<BbkModifier> mods) {
        BbkExpr inz = inzValue(mods);
        return inz != null ? scaled(coerce(expr(inz), b.type()), b) : defaultScaled(b);
    }

    private String initSubfieldValue(Binding b, BbkDeclaration.Subfield sub) {
        BbkExpr inz = inzValue(sub.modifiers());
        return inz != null ? scaled(coerce(expr(inz), b.type()), b) : defaultScaled(b);
    }

    private String defaultScaled(Binding b) {
        return b.type() == Ct.DECIMAL ? roundCall("0.0L", b.scale(), BbkStatement.AttrMod.NONE) : defaultValue(b.type());
    }

    // ----- assignment -----

    private String assignInline(BbkStatement.Assignment a) {
        BbkExpr target = a.target();
        if (target instanceof BbkExpr.Index ix) return assignArray(arrayRef(ix), a);
        if (target instanceof BbkExpr.Member mem && mem.target() instanceof BbkExpr.Index ix) return assignArray(dsArrayRef(ix, mem.field()), a);

        Binding b = scalarTarget(target, a);
        if (a.op() == BbkStatement.AssignOp.ASSIGN) {
            return b.cname() + " = " + scaled(coerce(expr(a.value()), b.type()), b, a.attr());
        }
        return compoundAssign(b.cname(), b.type(), b.scale(), a);
    }

    /** Compound assignment ({@code +=}, {@code <<=}, ...): native C op for the simple cases,
     *  helper/desugar for string concat, decimals (with scale) and checked integer division. */
    private String compoundAssign(String name, Ct type, int scale, BbkStatement.Assignment a) {
        BbkExpr.BinOp op = compoundToBin(a.op());
        BbkExpr value = a.value();
        if (type == Ct.STRING && op == BbkExpr.BinOp.ADD) {
            return name + " = bbk_cat(" + name + ", " + toStr(expr(value)) + ")";
        }
        if (type == Ct.DECIMAL) {
            String r = coerce(expr(value), Ct.DECIMAL);
            String comb = op == BbkExpr.BinOp.MOD ? "fmodl(" + name + ", " + r + ")" : name + " " + cBinOp(op) + " " + paren(r);
            return name + " = " + scaled(comb, Ct.DECIMAL, scale, a.attr());
        }
        if (type == Ct.LONG && (op == BbkExpr.BinOp.DIV || op == BbkExpr.BinOp.MOD)) {
            return name + " = " + (op == BbkExpr.BinOp.DIV ? "bbk_div(" : "bbk_mod(") + name + ", " + coerce(expr(value), Ct.LONG) + ")";
        }
        if (type == Ct.DOUBLE && op == BbkExpr.BinOp.MOD) {
            return name + " = fmod(" + name + ", " + coerce(expr(value), Ct.DOUBLE) + ")";
        }
        return name + " " + cAssignOp(a.op()) + " " + coerce(expr(value), type);
    }

    private Binding scalarTarget(BbkExpr target, BbkStatement.Assignment a) {
        if (target instanceof BbkExpr.Identifier id) {
            Binding b = lookup(id.name());
            if (b != null) return b;
            if (a.op() != BbkStatement.AssignOp.ASSIGN) throw unsupported("compound assignment to undeclared '" + id.name() + "'");
            // declared-on-first-assignment: register the clean binding, but return one whose
            // cname carries the C type so the caller emits a typed declaration.
            Ct vt = typeOf(a.value());
            locals.put(id.name(), new Binding(vt, false, 0, -1, id.name()));
            return new Binding(vt, false, 0, -1, ctype(vt) + " " + id.name());
        }
        if (target instanceof BbkExpr.Member mem) {
            Binding b = lookup(memberKey(mem));
            if (b == null) throw unsupported("assignment to unknown member '" + memberKey(mem) + "'");
            return b;
        }
        throw unsupported("assignment to " + target.getClass().getSimpleName());
    }

    private String assignArray(Ref ref, BbkStatement.Assignment a) {
        String lhs = ref.code();
        if (a.op() == BbkStatement.AssignOp.ASSIGN) {
            return lhs + " = " + scaled(coerce(expr(a.value()), ref.type()), ref.type(), ref.scale(), a.attr());
        }
        return compoundAssign(lhs, ref.type(), ref.scale(), a);
    }

    private String inlineInit(BbkItem init) {
        if (init instanceof BbkDeclaration.Variable v) {
            Ct ct = ct(v.type());
            Binding b = new Binding(ct, false, 0, scaleOf(v.type()), v.name());
            locals.put(v.name(), b);
            return ctype(ct) + " " + v.name() + " = " + initValue(b, v.modifiers());
        }
        if (init instanceof BbkStatement.Assignment a) return assignInline(a);
        if (init instanceof BbkStatement.ExpressionStatement es) return expr(es.expr()).code();
        throw unsupported("for-init " + init.getClass().getSimpleName());
    }

    private String inlineStmt(BbkStatement s) {
        if (s instanceof BbkStatement.Assignment a) return assignInline(a);
        if (s instanceof BbkStatement.ExpressionStatement es) return expr(es.expr()).code();
        throw unsupported("for-update " + s.getClass().getSimpleName());
    }

    private String exprStatement(BbkExpr e) {
        if (e instanceof BbkExpr.Call call && isName(call.target(), "print") && call.args().size() == 1) {
            BbkExpr parg = call.args().get(0);
            if (model.type(parg) instanceof Type.Scalar ds && isDate(ds.kind())) {   // print(fecha) -> ISO
                return "printf(\"%s\\n\", " + dateToString(parg, ds.kind()).code() + ")";
            }
            Cx arg = expr(parg);
            return switch (arg.type()) {
                case LONG -> "printf(\"%lld\\n\", " + arg.code() + ")";
                case DOUBLE -> "printf(\"%g\\n\", " + arg.code() + ")";
                case STRING -> "printf(\"%s\\n\", " + arg.code() + ")";
                case BOOL -> "printf(\"%s\\n\", (" + arg.code() + ") ? \"true\" : \"false\")";
                case DECIMAL -> "printf(\"%.*Lf\\n\", " + Math.max(arg.scale(), 0) + ", " + arg.code() + ")";
            };
        }
        if (e instanceof BbkExpr.Call call) return callStatement(call);
        return expr(e).code();
    }

    // -----------------------------------------------------------------------
    // Expressions
    // -----------------------------------------------------------------------

    private Cx expr(BbkExpr e) {
        return switch (e) {
            case BbkExpr.Identifier id -> identifier(id);
            case BbkExpr.Literal lit -> literal(lit);
            case BbkExpr.BoolLit b -> new Cx(b.value() ? "1" : "0", Ct.BOOL);
            case BbkExpr.NullLit n -> new Cx("\"\"", Ct.STRING);
            case BbkExpr.StarIdent s -> starValue(s.name());
            case BbkExpr.Unary u -> unary(u);
            case BbkExpr.Binary b -> binary(b);
            case BbkExpr.Ternary t -> ternary(t);
            case BbkExpr.Call c -> call(c);
            case BbkExpr.Index ix -> { Ref r = arrayRef(ix); yield new Cx(r.code(), r.type(), r.scale()); }
            case BbkExpr.Member m -> member(m);
        };
    }

    private Cx identifier(BbkExpr.Identifier id) {
        Binding b = lookup(id.name());
        if (b != null) {
            if (b.array()) throw unsupported("array '" + id.name() + "' used as a scalar value");
            return new Cx(b.cname(), b.type(), b.scale());
        }
        BbkExpr c = model.constant(id.name());
        if (c != null) return expr(c);
        throw unsupported("undeclared name '" + id.name() + "'");
    }

    private Cx literal(BbkExpr.Literal lit) {
        return switch (lit.kind()) {
            case INT, HEX -> new Cx(lit.text() + "LL", Ct.LONG);
            case OCT -> new Cx("0" + lit.text().substring(2) + "LL", Ct.LONG);
            case FLOAT -> new Cx(lit.text(), Ct.DOUBLE);
            case DEC -> new Cx(stripDecSuffix(lit.text()) + "L", Ct.DECIMAL, decimalsOf(lit.text()));
            case STRING -> new Cx(lit.text(), Ct.STRING);
        };
    }

    private Cx unary(BbkExpr.Unary u) {
        Cx o = expr(u.operand());
        return switch (u.op()) {
            case NEG -> new Cx("-" + paren(o.code()), o.type(), o.scale());
            case POS -> o;
            case NOT -> new Cx("!" + paren(coerce(o, Ct.BOOL)), Ct.BOOL);
            case BIT_NOT -> new Cx("~" + paren(coerce(o, Ct.LONG)), Ct.LONG);
        };
    }

    private Cx binary(BbkExpr.Binary b) {
        BbkExpr.BinOp op = b.op();
        if (op == BbkExpr.BinOp.ADD && (typeOf(b.left()) == Ct.STRING || typeOf(b.right()) == Ct.STRING)) {
            return new Cx("bbk_cat(" + toStr(expr(b.left())) + ", " + toStr(expr(b.right())) + ")", Ct.STRING);
        }
        if (op == BbkExpr.BinOp.POW) {
            // ** is canonically FLOAT in the shared model (matches the JVM back-end's Math.pow)
            return new Cx("pow(" + coerce(expr(b.left()), Ct.DOUBLE) + ", " + coerce(expr(b.right()), Ct.DOUBLE) + ")", Ct.DOUBLE);
        }
        switch (op) {
            case EQ: case NE: case LT: case GT: case LE: case GE: return comparison(b);
            case AND: case OR:
                return new Cx(operand(b.left(), op, false, Ct.BOOL) + " " + cBinOp(op) + " " + operand(b.right(), op, true, Ct.BOOL), Ct.BOOL);
            case BIT_AND: case BIT_OR: case BIT_XOR: case SHL: case SHR:
                return new Cx(operand(b.left(), op, false, Ct.LONG) + " " + cBinOp(op) + " " + operand(b.right(), op, true, Ct.LONG), Ct.LONG);
            default: break;
        }
        Ct result = wider(typeOf(b.left()), typeOf(b.right()));
        if (result == Ct.LONG && (op == BbkExpr.BinOp.DIV || op == BbkExpr.BinOp.MOD)) {
            return new Cx((op == BbkExpr.BinOp.DIV ? "bbk_div(" : "bbk_mod(") + coerce(expr(b.left()), Ct.LONG) + ", " + coerce(expr(b.right()), Ct.LONG) + ")", Ct.LONG);
        }
        if (op == BbkExpr.BinOp.MOD && result == Ct.DECIMAL) {
            return new Cx("fmodl(" + coerce(expr(b.left()), Ct.DECIMAL) + ", " + coerce(expr(b.right()), Ct.DECIMAL) + ")", Ct.DECIMAL, Math.max(scaleHint(b.left()), scaleHint(b.right())));
        }
        if (op == BbkExpr.BinOp.MOD && result == Ct.DOUBLE) {
            return new Cx("fmod(" + coerce(expr(b.left()), Ct.DOUBLE) + ", " + coerce(expr(b.right()), Ct.DOUBLE) + ")", Ct.DOUBLE);
        }
        int sc = result == Ct.DECIMAL ? Math.max(scaleHint(b.left()), scaleHint(b.right())) : 0;
        return new Cx(operand(b.left(), op, false, result) + " " + cBinOp(op) + " " + operand(b.right(), op, true, result), result, sc);
    }

    private Cx comparison(BbkExpr.Binary b) {
        Ct lt = typeOf(b.left()), rt = typeOf(b.right());
        if (lt == Ct.STRING || rt == Ct.STRING) {
            if (lt != Ct.STRING || rt != Ct.STRING) throw unsupported("cannot compare " + lt + " and " + rt);
            if (b.op() != BbkExpr.BinOp.EQ && b.op() != BbkExpr.BinOp.NE) throw unsupported("ordering comparison on strings");
            String cmp = "strcmp(" + expr(b.left()).code() + ", " + expr(b.right()).code() + ")";
            return new Cx("(" + cmp + " " + (b.op() == BbkExpr.BinOp.EQ ? "==" : "!=") + " 0)", Ct.BOOL);
        }
        Ct common = wider(lt, rt);
        return new Cx(operand(b.left(), b.op(), false, common) + " " + cBinOp(b.op()) + " " + operand(b.right(), b.op(), true, common), Ct.BOOL);
    }

    /** Emit a child operand coerced to {@code want}, with minimal parentheses by BBK/C precedence. */
    private String operand(BbkExpr child, BbkExpr.BinOp parent, boolean isRight, Ct want) {
        Cx c = expr(child);
        String code = coerce(c, want);
        if (c.type() == want && child instanceof BbkExpr.Binary cb && needsParens(cb.op(), parent, isRight)) return "(" + code + ")";
        return code;
    }

    private static boolean needsParens(BbkExpr.BinOp child, BbkExpr.BinOp parent, boolean isRight) {
        int pc = prec(child), pp = prec(parent);
        if (pc < pp) return true;
        if (pc > pp) return false;
        return isRight;     // these operators are left-associative
    }

    /** Precedence (higher = binds tighter); identical in BBK and C. */
    private static int prec(BbkExpr.BinOp op) {
        return switch (op) {
            case POW -> 11;
            case MUL, DIV, MOD -> 10;
            case ADD, SUB -> 9;
            case SHL, SHR -> 8;
            case LT, GT, LE, GE -> 7;
            case EQ, NE -> 6;
            case BIT_AND -> 5;
            case BIT_XOR -> 4;
            case BIT_OR -> 3;
            case AND -> 2;
            case OR -> 1;
        };
    }

    private int scaleHint(BbkExpr e) { return model.type(e).scaleOrZero(); }

    private Cx ternary(BbkExpr.Ternary t) {
        Cx then = expr(t.then());
        Cx other = expr(t.otherwise());
        if (then.type() != other.type()) throw unsupported("ternary branches of different types (" + then.type() + " vs " + other.type() + ")");
        return new Cx("(" + coerce(expr(t.condition()), Ct.BOOL) + " ? " + then.code() + " : " + other.code() + ")", then.type(), then.scale());
    }

    private Cx member(BbkExpr.Member m) {
        if (m.target() instanceof BbkExpr.Index ix) { Ref r = dsArrayRef(ix, m.field()); return new Cx(r.code(), r.type(), r.scale()); }
        Binding b = lookup(memberKey(m));
        if (b == null) throw unsupported("unknown member '" + memberKey(m) + "'");
        if (b.array()) throw unsupported("DS-array element requires an index: " + memberKey(m) + "[i]");
        return new Cx(b.cname(), b.type(), b.scale());
    }

    // ----- array references -----

    private record Ref(String code, Ct type, int scale) {}

    private Ref arrayRef(BbkExpr.Index ix) {
        if (!(ix.target() instanceof BbkExpr.Identifier id)) throw unsupported("indexing a non-name");
        Binding b = lookup(id.name());
        if (b == null || !b.array()) throw unsupported("'" + id.name() + "' is not an array");
        if (ix.indices().size() != 1) throw unsupported("multi-dimensional subscript");
        return new Ref(b.cname() + "[" + coerce(expr(ix.indices().get(0)), Ct.LONG) + "]", b.type(), b.scale());
    }

    private Ref dsArrayRef(BbkExpr.Index ix, String field) {
        if (!(ix.target() instanceof BbkExpr.Identifier id)) throw unsupported("indexing a non-name");
        if (ix.indices().size() != 1) throw unsupported("multi-dimensional subscript");
        Binding b = lookup(id.name() + "." + field);
        if (b == null || !b.array()) throw unsupported("'" + id.name() + "." + field + "' is not a DS-array field");
        return new Ref(b.cname() + "[" + coerce(expr(ix.indices().get(0)), Ct.LONG) + "]", b.type(), b.scale());
    }

    // -----------------------------------------------------------------------
    // Calls (procedures + pure builtins)
    // -----------------------------------------------------------------------

    private Cx call(BbkExpr.Call c) {
        if (!(c.target() instanceof BbkExpr.Identifier id)) throw unsupported("call to a non-name target");
        Sig sig = procs.get(id.name());
        if (sig != null) return invokeProcedure(id.name(), sig, c.args());
        return builtin(id.name(), c.args());
    }

    private Cx invokeProcedure(String name, Sig sig, List<BbkExpr> args) {
        if (sig.ret() == null) throw unsupported("void call used as a value");
        return new Cx(emitCall(name, sig, args), sig.ret());
    }

    /** A procedure call used as a statement (may be void). */
    private String callStatement(BbkExpr.Call c) {
        if (!(c.target() instanceof BbkExpr.Identifier id)) throw unsupported("call to a non-name target");
        Sig sig = procs.get(id.name());
        if (sig == null) return builtin(id.name(), c.args()).code();
        return emitCall(id.name(), sig, c.args());
    }

    /** Emit a call, flattening each DS argument to its subfields (by value) in the parameter's field order. */
    private String emitCall(String name, Sig sig, List<BbkExpr> args) {
        if (args.size() != sig.params().size()) throw unsupported("procedure '" + name + "' arity mismatch");
        StringBuilder sb = new StringBuilder(name).append('(');
        boolean first = true;
        for (int i = 0; i < args.size(); i++) {
            Param param = sig.params().get(i);
            if (param instanceof ScalarParam s) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(s.array() ? arrayArg(args.get(i)) : coerce(expr(args.get(i)), s.ct()));
            } else {
                for (DsField f : ((DsParam) param).fields()) {
                    if (!first) sb.append(", ");
                    first = false;
                    Binding sub = dsArgField(args.get(i), f.subName());
                    sb.append(coerce(new Cx(sub.cname(), sub.type(), sub.scale()), f.ct()));
                }
            }
        }
        return sb.append(')').toString();
    }

    private Binding dsArgField(BbkExpr arg, String subName) {
        if (!(arg instanceof BbkExpr.Identifier id)) throw unsupported("a data-structure argument must be a variable name");
        Binding b = lookup(id.name() + "." + subName);
        if (b == null) throw unsupported("'" + id.name() + "' has no field '" + subName + "' (data structures must match)");
        return b;
    }

    private String arrayArg(BbkExpr arg) {
        if (!(arg instanceof BbkExpr.Identifier id)) throw unsupported("array argument must be a name");
        Binding b = lookup(id.name());
        if (b == null || !b.array()) throw unsupported("argument is not an array");
        return b.cname();
    }

    private Cx builtin(String name, List<BbkExpr> args) {
        return switch (name) {
            case "len" -> new Cx("(long long) strlen(" + coerce(expr(args.get(0)), Ct.STRING) + ")", Ct.LONG);
            case "substr" -> new Cx("bbk_substr(" + coerce(expr(args.get(0)), Ct.STRING) + ", " + coerce(expr(args.get(1)), Ct.LONG)
                + ", " + (args.size() >= 3 ? coerce(expr(args.get(2)), Ct.LONG) : "(long long) strlen(" + coerce(expr(args.get(0)), Ct.STRING) + ")") + ")", Ct.STRING);
            case "scan" -> new Cx("bbk_scan(" + coerce(expr(args.get(0)), Ct.STRING) + ", " + coerce(expr(args.get(1)), Ct.STRING) + ")", Ct.LONG);
            case "trim" -> new Cx("bbk_trim(" + coerce(expr(args.get(0)), Ct.STRING) + ")", Ct.STRING);
            case "triml" -> new Cx("bbk_triml(" + coerce(expr(args.get(0)), Ct.STRING) + ")", Ct.STRING);
            case "trimr" -> new Cx("bbk_trimr(" + coerce(expr(args.get(0)), Ct.STRING) + ")", Ct.STRING);
            case "lower" -> new Cx("bbk_lower(" + coerce(expr(args.get(0)), Ct.STRING) + ")", Ct.STRING);
            case "upper" -> new Cx("bbk_upper(" + coerce(expr(args.get(0)), Ct.STRING) + ")", Ct.STRING);
            case "replace" -> new Cx("bbk_replace(" + coerce(expr(args.get(0)), Ct.STRING) + ", " + coerce(expr(args.get(1)), Ct.STRING) + ", " + coerce(expr(args.get(2)), Ct.STRING) + ")", Ct.STRING);
            case "char" -> charOf(args.get(0));
            case "int" -> toInt(args.get(0));
            case "float" -> new Cx(coerce(expr(args.get(0)), Ct.DOUBLE), Ct.DOUBLE);
            case "dec" -> new Cx(coerce(expr(args.get(0)), Ct.DECIMAL), Ct.DECIMAL, 2);
            case "sqrt" -> new Cx("sqrt(" + coerce(expr(args.get(0)), Ct.DOUBLE) + ")", Ct.DOUBLE);
            case "abs" -> absOf(args.get(0));
            default -> { if (DATE_FUNCS.contains(name)) yield dateBuiltin(name, args); throw unsupported("function '" + name + "'"); }
        };
    }

    // -----------------------------------------------------------------------
    // Date runtime (DATE/TIME/TIMESTAMP; epoch-day / seconds as long long)
    // -----------------------------------------------------------------------

    private static final Set<String> DATE_FUNCS = Set.of(
        "date", "time", "timestamp", "today", "now", "year", "month", "day", "hour", "minute", "second",
        "adddays", "addmonths", "addyears", "addhours", "addminutes", "addseconds", "diffdays", "diffseconds");

    private static boolean isDate(Type.Kind k) {
        return k == Type.Kind.DATE || k == Type.Kind.TIME || k == Type.Kind.TIMESTAMP;
    }

    /** {@code char(x)}: an ISO string for a date/time value, else the usual scalar-to-string. */
    private Cx charOf(BbkExpr arg) {
        if (model.type(arg) instanceof Type.Scalar s && isDate(s.kind())) return dateToString(arg, s.kind());
        return new Cx(toStr(expr(arg)), Ct.STRING);
    }

    private Cx dateToString(BbkExpr arg, Type.Kind k) {
        String fn = k == Type.Kind.DATE ? "bbk_date_str" : k == Type.Kind.TIME ? "bbk_time_str" : "bbk_ts_str";
        return new Cx(fn + "(" + coerce(expr(arg), Ct.LONG) + ")", Ct.STRING);
    }

    private Cx dateBuiltin(String name, List<BbkExpr> args) {
        switch (name) {
            case "date": return new Cx("bbk_date_parse(" + coerce(expr(args.get(0)), Ct.STRING) + ")", Ct.LONG);
            case "time": return new Cx("bbk_time_parse(" + coerce(expr(args.get(0)), Ct.STRING) + ")", Ct.LONG);
            case "timestamp": return new Cx("bbk_ts_parse(" + coerce(expr(args.get(0)), Ct.STRING) + ")", Ct.LONG);
            case "today": return new Cx("bbk_date_today()", Ct.LONG);
            case "now": return new Cx("bbk_ts_now()", Ct.LONG);
            default: break;
        }
        Type.Kind k = ((Type.Scalar) model.type(args.get(0))).kind();
        String a0 = coerce(expr(args.get(0)), Ct.LONG);
        boolean date = k == Type.Kind.DATE, time = k == Type.Kind.TIME;
        switch (name) {
            case "year": return new Cx((date ? "bbk_date_year" : "bbk_ts_year") + "(" + a0 + ")", Ct.LONG);
            case "month": return new Cx((date ? "bbk_date_month" : "bbk_ts_month") + "(" + a0 + ")", Ct.LONG);
            case "day": return new Cx((date ? "bbk_date_day" : "bbk_ts_day") + "(" + a0 + ")", Ct.LONG);
            case "hour": return new Cx((time ? "bbk_time_hour" : "bbk_ts_hour") + "(" + a0 + ")", Ct.LONG);
            case "minute": return new Cx((time ? "bbk_time_minute" : "bbk_ts_minute") + "(" + a0 + ")", Ct.LONG);
            case "second": return new Cx((time ? "bbk_time_second" : "bbk_ts_second") + "(" + a0 + ")", Ct.LONG);
            case "adddays": case "addmonths": case "addyears": case "addhours": case "addminutes": case "addseconds":
                return addUnit(name, k, a0, args.get(1));
            case "diffdays": return new Cx("((" + a0 + ") - (" + coerce(expr(args.get(1)), Ct.LONG) + "))"
                + (k == Type.Kind.TIMESTAMP ? " / 86400LL" : ""), Ct.LONG);
            case "diffseconds": return new Cx("((" + a0 + ") - (" + coerce(expr(args.get(1)), Ct.LONG) + "))", Ct.LONG);
            default: throw unsupported("date function '" + name + "'");
        }
    }

    private Cx addUnit(String name, Type.Kind k, String a0, BbkExpr nArg) {
        String n = "(" + coerce(expr(nArg), Ct.LONG) + ")";
        boolean ts = k == Type.Kind.TIMESTAMP;
        String code = switch (name) {
            case "adddays" -> "(" + a0 + ") + " + n + (ts ? " * 86400LL" : "");
            case "addmonths" -> (ts ? "bbk_ts_addmonths" : "bbk_date_addmonths") + "(" + a0 + ", " + n + ")";
            case "addyears" -> (ts ? "bbk_ts_addyears" : "bbk_date_addyears") + "(" + a0 + ", " + n + ")";
            case "addhours" -> ts ? "(" + a0 + ") + " + n + " * 3600LL" : "bbk_time_add(" + a0 + ", " + n + " * 3600LL)";
            case "addminutes" -> ts ? "(" + a0 + ") + " + n + " * 60LL" : "bbk_time_add(" + a0 + ", " + n + " * 60LL)";
            case "addseconds" -> ts ? "(" + a0 + ") + " + n : "bbk_time_add(" + a0 + ", " + n + ")";
            default -> throw unsupported("add unit " + name);
        };
        return new Cx(code, Ct.LONG);
    }

    private Cx toInt(BbkExpr arg) {
        Cx v = expr(arg);
        if (v.type() == Ct.STRING) return new Cx("strtoll(" + v.code() + ", NULL, 10)", Ct.LONG);
        return new Cx(coerce(v, Ct.LONG), Ct.LONG);
    }

    private Cx absOf(BbkExpr arg) {
        Cx v = expr(arg);
        return switch (v.type()) {
            case LONG -> new Cx("llabs(" + v.code() + ")", Ct.LONG);
            case DOUBLE -> new Cx("fabs(" + v.code() + ")", Ct.DOUBLE);
            case DECIMAL -> new Cx("fabsl(" + v.code() + ")", Ct.DECIMAL, v.scale());
            default -> throw unsupported("abs of " + v.type());
        };
    }

    // -----------------------------------------------------------------------
    // Pure type inference (no emission)
    // -----------------------------------------------------------------------

    /** The lowered type of an expression — delegated to the shared {@link SemanticModel}. */
    private Ct typeOf(BbkExpr e) { return ct(model.type(e)); }

    /** Map a shared semantic {@link Type} onto this back-end's {@link Ct}. */
    private static Ct ct(Type t) {
        if (t instanceof Type.Scalar s) {
            return switch (s.kind()) {
                case INT -> Ct.LONG; case FLOAT -> Ct.DOUBLE; case DECIMAL -> Ct.DECIMAL;
                case STRING -> Ct.STRING; case BOOL -> Ct.BOOL;
                case DATE, TIME, TIMESTAMP -> Ct.LONG;     // epoch-day / segundos: long long
            };
        }
        if (t instanceof Type.Array a) return ct(a.element());
        if (t == Type.VOID) throw unsupported("void call used as a value");
        throw unsupported("expression has no scalar type");
    }

    private Sig toSig(ProcSignature sig) {
        List<Param> params = new ArrayList<>();
        for (int i = 0; i < sig.paramTypes().size(); i++) {
            Type t = sig.paramTypes().get(i);
            if (t instanceof Type.Ds ds) {
                if (ds.array()) throw unsupported("an array of data structures as a parameter");
                List<DsField> fields = new ArrayList<>();
                for (Type.Ds.Field f : ds.fields()) fields.add(new DsField(f.name(), ct(f.type()), scaleFromType(f.type())));
                params.add(new DsParam(fields));
            } else {
                params.add(new ScalarParam(ct(t), sig.paramArray().get(i), scaleFromType(t)));
            }
        }
        Ct ret = sig.isVoid() ? null : retCt(sig.returnType());
        return new Sig(params, ret);
    }

    private static Ct retCt(Type t) {
        if (t instanceof Type.Ds) throw unsupported("returning a data structure from a procedure (use a parameter instead)");
        return ct(t);
    }

    /** The declared decimal scale of a semantic type (for DECIMAL), else -1. */
    private static int scaleFromType(Type t) {
        return t instanceof Type.Scalar s && s.kind() == Type.Kind.DECIMAL ? s.scale() : -1;
    }

    // -----------------------------------------------------------------------
    // Special values
    // -----------------------------------------------------------------------

    private Cx starValue(String name) {
        String n = (name.startsWith("*") ? name.substring(1) : name).toUpperCase();
        return switch (n) {
            case "ON" -> new Cx("1", Ct.BOOL);
            case "OFF" -> new Cx("0", Ct.BOOL);
            case "ZERO", "ZEROS" -> new Cx("0LL", Ct.LONG);
            case "BLANK", "BLANKS" -> new Cx("\"\"", Ct.STRING);
            case "NULL" -> new Cx("\"\"", Ct.STRING);
            default -> throw unsupported("special value '*" + n + "'");
        };
    }

    // -----------------------------------------------------------------------
    // Coercion / scaling / mapping helpers
    // -----------------------------------------------------------------------

    private String coerce(Cx v, Ct want) {
        if (v.type() == want) return v.code();
        String c = v.code();
        switch (want) {
            case DOUBLE -> { if (v.type() == Ct.LONG || v.type() == Ct.DECIMAL) return "(double)(" + c + ")"; }
            case LONG -> { if (v.type() == Ct.DOUBLE || v.type() == Ct.DECIMAL) return "(long long)(" + c + ")"; }
            case DECIMAL -> { if (v.type() == Ct.LONG || v.type() == Ct.DOUBLE) return "(long double)(" + c + ")"; }
            default -> { }
        }
        throw unsupported("conversion " + v.type() + " -> " + want);
    }

    private String scaled(String code, Binding b, BbkStatement.AttrMod attr) { return scaled(code, b.type(), b.scale(), attr); }
    private String scaled(String code, Binding b) { return scaled(code, b.type(), b.scale(), BbkStatement.AttrMod.NONE); }

    private String scaled(String code, Ct type, int scale, BbkStatement.AttrMod attr) {
        if (type != Ct.DECIMAL || scale < 0) return code;
        return roundCall(code, scale, attr);
    }

    private String roundCall(String code, int scale, BbkStatement.AttrMod attr) {
        String fn = (attr == BbkStatement.AttrMod.TRUNC || attr == BbkStatement.AttrMod.NONE) ? "bbk_truncs" : "bbk_round";
        return fn + "(" + code + ", " + scale + ")";
    }

    private String toStr(Cx v) {
        return switch (v.type()) {
            case STRING -> v.code();
            case LONG -> "bbk_sll(" + v.code() + ")";
            case DOUBLE -> "bbk_sd(" + v.code() + ")";
            case DECIMAL -> "bbk_sdec(" + v.code() + ", " + Math.max(v.scale(), 0) + ")";
            case BOOL -> "((" + v.code() + ") ? \"true\" : \"false\")";
        };
    }

    private String prototype(String name, Sig sig) {
        return prototype(name, null, sig);
    }

    private String prototype(String name, BbkDeclaration.Procedure p, Sig sig) {
        List<String> params = cParams(sig, p);
        String body = params.isEmpty() ? "void" : String.join(", ", params);
        return (sig.ret() == null ? "void" : ctype(sig.ret())) + " " + name + "(" + body + ")";
    }

    /** The flattened C parameter list: a scalar/array per scalar param, one per subfield for a DS param.
     *  The definition ({@code p != null}) uses the BBK names ({@code c_x}); the forward decl uses {@code pN}. */
    private List<String> cParams(Sig sig, BbkDeclaration.Procedure p) {
        List<String> out = new ArrayList<>();
        int slot = 0;
        for (int i = 0; i < sig.params().size(); i++) {
            Param param = sig.params().get(i);
            if (param instanceof ScalarParam s) {
                String name = p != null ? p.params().get(i).name() : "p" + slot;
                out.add(ctype(s.ct()) + " " + name + (s.array() ? "[]" : ""));
                slot++;
            } else {
                for (DsField f : ((DsParam) param).fields()) {
                    String name = p != null ? p.params().get(i).name() + "_" + f.subName() : "p" + slot;
                    out.add(ctype(f.ct()) + " " + name);
                    slot++;
                }
            }
        }
        return out;
    }

    private Binding lookup(String name) {
        if (locals != null) { Binding b = locals.get(name); if (b != null) return b; }
        return globals.get(name);
    }

    private List<BbkDeclaration.Subfield> resolveSubs(BbkDeclaration.DataStructure d) {
        if (!d.subfields().isEmpty()) return d.subfields();
        String like = likedsTemplate(d.modifiers());
        return like != null ? template(like) : d.subfields();
    }

    private List<BbkDeclaration.Subfield> template(String name) {
        List<BbkDeclaration.Subfield> tpl = dsTemplates.get(name);
        if (tpl == null) throw unsupported("LIKEDS(" + name + ") — data structure not declared before use");
        return tpl;
    }

    private static String cAssignOp(BbkStatement.AssignOp op) {
        return switch (op) {
            case ASSIGN -> "="; case ADD -> "+="; case SUB -> "-="; case MUL -> "*="; case DIV -> "/=";
            case MOD -> "%="; case AND -> "&="; case OR -> "|="; case XOR -> "^="; case SHL -> "<<="; case SHR -> ">>=";
        };
    }

    private static int decimalsOf(String text) {
        String t = stripDecSuffix(text);
        int dot = t.indexOf('.');
        return dot < 0 ? 0 : t.length() - dot - 1;
    }

    private static Ct ct(BbkType type) {
        if (type instanceof BbkType.Primitive p) {
            return switch (p.name().toUpperCase()) {
                case "INT", "UNS" -> Ct.LONG;
                case "FLOAT" -> Ct.DOUBLE;
                case "PACKED", "ZONED", "BINDEC" -> Ct.DECIMAL;
                case "BOOL", "IND" -> Ct.BOOL;
                case "CHAR", "VARCHAR" -> Ct.STRING;
                case "DATE", "TIME", "TIMESTAMP" -> Ct.LONG;   // epoch-day / segundos: long long
                case "POINTER" -> throw new UnsupportedOperationException("pointers are not supported by the C back-end");
                default -> throw new UnsupportedOperationException("type '" + p.name() + "' not supported by the C back-end");
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

    private static String ctype(Ct ct) {
        return switch (ct) {
            case LONG -> "long long"; case DOUBLE -> "double"; case BOOL -> "int";
            case STRING -> "const char*"; case DECIMAL -> "long double";
        };
    }

    private static String defaultValue(Ct ct) {
        return switch (ct) { case LONG -> "0LL"; case DOUBLE -> "0.0"; case BOOL -> "0"; case STRING -> "\"\""; case DECIMAL -> "0.0L"; };
    }

    private static Ct wider(Ct a, Ct b) {
        if (a == Ct.DECIMAL || b == Ct.DECIMAL) return Ct.DECIMAL;
        if (a == Ct.DOUBLE || b == Ct.DOUBLE) return Ct.DOUBLE;
        return Ct.LONG;
    }

    private static String cBinOp(BbkExpr.BinOp op) {
        return switch (op) {
            case ADD -> "+"; case SUB -> "-"; case MUL -> "*"; case DIV -> "/"; case MOD -> "%";
            case EQ -> "=="; case NE -> "!="; case LT -> "<"; case GT -> ">"; case LE -> "<="; case GE -> ">=";
            case AND -> "&&"; case OR -> "||";
            case BIT_AND -> "&"; case BIT_OR -> "|"; case BIT_XOR -> "^"; case SHL -> "<<"; case SHR -> ">>";
            case POW -> throw new UnsupportedOperationException("** handled via powl");
        };
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

    private static boolean hasMod(List<BbkModifier> mods, String name) {
        for (BbkModifier m : mods) if (m.name().equalsIgnoreCase(name)) return true;
        return false;
    }

    private static boolean isArray(List<BbkModifier> mods) { return hasMod(mods, "DIM"); }

    private static int dimSize(List<BbkModifier> mods) {
        for (BbkModifier m : mods) {
            if (m.name().equalsIgnoreCase("DIM") && m.args().size() == 1 && m.args().get(0) instanceof BbkExpr.Literal lit) return Integer.parseInt(lit.text());
        }
        throw new UnsupportedOperationException("DIM requires a literal size");
    }

    private static BbkExpr inzValue(List<BbkModifier> modifiers) {
        for (BbkModifier m : modifiers) if (m.name().equalsIgnoreCase("INZ") && m.args().size() == 1) return m.args().get(0);
        return null;
    }

    private static String stripDecSuffix(String text) {
        return (text.endsWith("d") || text.endsWith("D")) ? text.substring(0, text.length() - 1) : text;
    }

    private static String paren(String code) {
        return code.matches("[A-Za-z_][A-Za-z0-9_]*|\\d+LL|\"[^\"]*\"|\\(.*\\)") ? code : "(" + code + ")";
    }

    private void line(String text) { out.append("  ".repeat(indent)).append(text).append('\n'); }

    private static UnsupportedOperationException unsupported(String what) {
        return new UnsupportedOperationException(what + " is not supported by the C back-end");
    }
}
