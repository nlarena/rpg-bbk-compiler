package com.larena.boxbreaker.core.semantic;

/**
 * A semantic finding produced by the {@link SemanticAnalyzer} — an error or a
 * warning, raised <em>before</em> code generation so problems surface with a
 * clear message instead of blowing up mid-emit.
 */
public record Diagnostic(Severity severity, String message) {

    public enum Severity { ERROR, WARNING }

    public static Diagnostic error(String message) { return new Diagnostic(Severity.ERROR, message); }
    public static Diagnostic warning(String message) { return new Diagnostic(Severity.WARNING, message); }

    public boolean isError() { return severity == Severity.ERROR; }

    @Override
    public String toString() { return severity + ": " + message; }
}
