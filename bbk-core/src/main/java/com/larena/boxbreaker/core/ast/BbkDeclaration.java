package com.larena.boxbreaker.core.ast;

import java.util.List;

/**
 * A BBK declaration (grammar L1-L3). Sealed; an {@link BbkItem} like
 * {@link BbkStatement}. Modifier lists are captured uniformly as
 * {@link BbkModifier}s.
 */
public sealed interface BbkDeclaration extends BbkItem
        permits BbkDeclaration.Variable, BbkDeclaration.Constant,
                BbkDeclaration.DataStructure, BbkDeclaration.Subfield,
                BbkDeclaration.File, BbkDeclaration.Prototype,
                BbkDeclaration.Procedure, BbkDeclaration.Parameter,
                BbkDeclaration.CtlOpt {

    /** {@code DCL-S name type modifiers*;} */
    record Variable(String name, BbkType type, List<BbkModifier> modifiers) implements BbkDeclaration {
        public Variable { modifiers = List.copyOf(modifiers); }
    }

    /** {@code DCL-C name value;} — value is a literal, true/false/null, or CONST(...). */
    record Constant(String name, BbkExpr value) implements BbkDeclaration {}

    /** {@code DCL-DS name modifiers* { subfields }}  (or {@code ;} for an empty DS). */
    record DataStructure(String name, List<BbkModifier> modifiers,
                         List<Subfield> subfields) implements BbkDeclaration {
        public DataStructure {
            modifiers = List.copyOf(modifiers);
            subfields = List.copyOf(subfields);
        }
    }

    /** {@code name type modifiers*;} inside a {@code DCL-DS}. */
    record Subfield(String name, BbkType type, List<BbkModifier> modifiers) implements BbkDeclaration {
        public Subfield { modifiers = List.copyOf(modifiers); }
    }

    /** {@code DCL-F name keywords*;} */
    record File(String name, List<BbkModifier> keywords) implements BbkDeclaration {
        public File { keywords = List.copyOf(keywords); }
    }

    /** {@code DCL-PR name(params)? -> ret? modifiers*;} */
    record Prototype(String name, List<Parameter> params, BbkType returnType,
                     List<BbkModifier> modifiers) implements BbkDeclaration {
        public Prototype {
            params = List.copyOf(params);
            modifiers = List.copyOf(modifiers);
        }
    }

    /** {@code DCL-PROC name(params)? -> ret? modifiers* { body }} */
    record Procedure(String name, List<Parameter> params, BbkType returnType,
                     List<BbkModifier> modifiers, List<BbkItem> body) implements BbkDeclaration {
        public Procedure {
            params = List.copyOf(params);
            modifiers = List.copyOf(modifiers);
            body = List.copyOf(body);
        }
    }

    /** An inline parameter of a prototype/procedure: {@code name type modifiers*}. */
    record Parameter(String name, BbkType type, List<BbkModifier> modifiers) implements BbkDeclaration {
        public Parameter { modifiers = List.copyOf(modifiers); }
    }

    /** {@code CTL-OPT keywords*;} */
    record CtlOpt(List<BbkModifier> keywords) implements BbkDeclaration {
        public CtlOpt { keywords = List.copyOf(keywords); }
    }
}
