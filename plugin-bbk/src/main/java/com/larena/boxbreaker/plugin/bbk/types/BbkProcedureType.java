package com.larena.boxbreaker.plugin.bbk.types;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Type of a procedure or prototype: an ordered parameter list + a return type.
 * {@code VOID} return means a subroutine-like procedure.
 *
 * <p>Smart completion uses this in two ways:
 * <ul>
 *   <li>When the user is typing a call argument, the parameter type at the
 *       current position is the expected type.</li>
 *   <li>When a procedure name appears as a candidate, the return type is what
 *       the smart filter compares against the expected type at the caret.</li>
 * </ul>
 */
public final class BbkProcedureType implements BbkType {

    public record Parameter(@NotNull String name, @NotNull BbkType type, boolean byValue, boolean byConst) {}

    private final @NotNull List<Parameter> parameters;
    private final @NotNull BbkType returnType;

    public BbkProcedureType(@NotNull List<Parameter> parameters, @NotNull BbkType returnType) {
        this.parameters = List.copyOf(parameters);
        this.returnType = returnType;
    }

    public @NotNull List<Parameter> getParameters() { return parameters; }
    public @NotNull BbkType getReturnType() { return returnType; }

    @Override
    public @NotNull String getDisplayName() {
        String params = parameters.stream()
            .map(p -> p.name() + " " + p.type().getDisplayName())
            .collect(Collectors.joining(", "));
        return "(" + params + ") -> " + returnType.getDisplayName();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
