package com.larena.boxbreaker.core.semantic;

import java.util.List;

/**
 * A procedure's resolved signature: the type of each parameter (and whether it
 * is an array), and the return type ({@link Type#VOID} for a procedure with no
 * return). Shared by both back-ends to type calls and coerce arguments.
 */
public record ProcSignature(List<Type> paramTypes, List<Boolean> paramArray, Type returnType) {

    public ProcSignature {
        paramTypes = List.copyOf(paramTypes);
        paramArray = List.copyOf(paramArray);
    }

    public int arity() { return paramTypes.size(); }

    public boolean isVoid() { return returnType == Type.VOID; }
}
