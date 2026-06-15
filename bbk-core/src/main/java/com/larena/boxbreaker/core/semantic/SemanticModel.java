package com.larena.boxbreaker.core.semantic;

import com.larena.boxbreaker.core.ast.BbkExpr;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of {@link SemanticAnalyzer}: the single source of truth the two
 * back-ends consult instead of re-deriving types and resolution.
 *
 * <p>Expression types are keyed by <em>identity</em> (each occurrence is a
 * distinct AST node), so two equal-looking sub-expressions in different scopes
 * keep their own types.
 */
public final class SemanticModel {

    private final Map<BbkExpr, Type> types = new IdentityHashMap<>();
    private final Map<String, ProcSignature> procedures = new LinkedHashMap<>();
    private final Map<String, BbkExpr> constants = new LinkedHashMap<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    SemanticModel() {}

    // ----- populated by the analyzer (package-private) -----

    void putType(BbkExpr e, Type type) { types.put(e, type); }
    void putProcedure(String name, ProcSignature sig) { procedures.put(name, sig); }
    void putConstant(String name, BbkExpr value) { constants.put(name, value); }
    void diagnose(Diagnostic d) { diagnostics.add(d); }

    // ----- queries used by the back-ends -----

    /** Type of an expression occurrence, or {@link Type#UNKNOWN} if it was not analyzed. */
    public Type type(BbkExpr e) {
        Type t = types.get(e);
        return t != null ? t : Type.UNKNOWN;
    }

    public ProcSignature signature(String name) { return procedures.get(name); }
    public boolean isProcedure(String name) { return procedures.containsKey(name); }
    public Map<String, ProcSignature> procedures() { return Map.copyOf(procedures); }

    public BbkExpr constant(String name) { return constants.get(name); }
    public boolean isConstant(String name) { return constants.containsKey(name); }

    public List<Diagnostic> diagnostics() { return List.copyOf(diagnostics); }
    public boolean hasErrors() {
        for (Diagnostic d : diagnostics) if (d.isError()) return true;
        return false;
    }
}
