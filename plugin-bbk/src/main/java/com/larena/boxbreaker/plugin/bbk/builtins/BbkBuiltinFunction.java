package com.larena.boxbreaker.plugin.bbk.builtins;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A built-in function (BIF) of the BBK language — {@code trim}, {@code substr},
 * {@code len}, etc. Carries enough metadata for both completion (signature
 * template) and quick documentation (description + parameter docs + return type).
 *
 * @param name          bare name without parens, e.g. {@code "substr"}
 * @param signature     human-readable signature, e.g. {@code "substr(s, start [, len])"}
 * @param returnType    short description of the return type, e.g. {@code "CHAR"}
 * @param summary       one-line summary shown in quick-navigate hover
 * @param parameters    per-parameter documentation lines (may be empty)
 * @param description   full prose description shown in the Ctrl+Q popup
 */
public record BbkBuiltinFunction(
        @NotNull String name,
        @NotNull String signature,
        @NotNull String returnType,
        @NotNull String summary,
        @NotNull List<Parameter> parameters,
        @NotNull String description) {

    /** A single documented parameter. */
    public record Parameter(@NotNull String name, @NotNull String doc) {}

    public BbkBuiltinFunction {
        parameters = List.copyOf(parameters);
    }
}
