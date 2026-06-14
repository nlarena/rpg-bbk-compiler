package com.larena.boxbreaker.rpg.ast;

import java.util.List;

/**
 * An RPG declaration (grammar §4.2). Sealed; an {@link RpgItem} like
 * {@link RpgStatement}, since declarations and statements interleave in bodies.
 *
 * <p>Modifier keywords are captured uniformly as {@link RpgKeyword} lists,
 * matching the grammar's {@code { keyword }*}.
 */
public sealed interface RpgDeclaration extends RpgItem
        permits RpgDeclaration.Variable, RpgDeclaration.Constant,
                RpgDeclaration.DataStructure, RpgDeclaration.Subfield,
                RpgDeclaration.Prototype, RpgDeclaration.ProcInterface,
                RpgDeclaration.Parameter, RpgDeclaration.File,
                RpgDeclaration.Procedure, RpgDeclaration.CtlOpt {

    /** {@code dcl-s name type keywords*;} */
    record Variable(String name, RpgType type, List<RpgKeyword> keywords) implements RpgDeclaration {
        public Variable { keywords = List.copyOf(keywords); }
    }

    /** {@code dcl-c name value;} — value is a literal, figurative, or constant expression. */
    record Constant(String name, RpgExpr value) implements RpgDeclaration {}

    /** {@code dcl-ds name keywords*; subfields* end-ds;} */
    record DataStructure(String name, List<RpgKeyword> keywords,
                         List<Subfield> subfields) implements RpgDeclaration {
        public DataStructure {
            keywords = List.copyOf(keywords);
            subfields = List.copyOf(subfields);
        }
    }

    /** {@code name type keywords*;} inside a {@code dcl-ds}. */
    record Subfield(String name, RpgType type, List<RpgKeyword> keywords) implements RpgDeclaration {
        public Subfield { keywords = List.copyOf(keywords); }
    }

    /** {@code dcl-pr name returnType? keywords*; params* end-pr;} */
    record Prototype(String name, RpgType returnType, List<RpgKeyword> keywords,
                     List<Parameter> params) implements RpgDeclaration {
        public Prototype {
            keywords = List.copyOf(keywords);
            params = List.copyOf(params);
        }
    }

    /** {@code dcl-pi name returnType? keywords*; params* end-pi;} */
    record ProcInterface(String name, RpgType returnType, List<RpgKeyword> keywords,
                         List<Parameter> params) implements RpgDeclaration {
        public ProcInterface {
            keywords = List.copyOf(keywords);
            params = List.copyOf(params);
        }
    }

    /** {@code dcl-parm name type keywords*;} */
    record Parameter(String name, RpgType type, List<RpgKeyword> keywords) implements RpgDeclaration {
        public Parameter { keywords = List.copyOf(keywords); }
    }

    /** {@code dcl-f name keywords*;} */
    record File(String name, List<RpgKeyword> keywords) implements RpgDeclaration {
        public File { keywords = List.copyOf(keywords); }
    }

    /**
     * {@code dcl-proc name keywords*; [dcl-pi ...] body* end-proc;}
     *
     * @param pi   the procedure interface (parameters/return), or null
     * @param body declarations and statements, in source order
     */
    record Procedure(String name, List<RpgKeyword> keywords, ProcInterface pi,
                     List<RpgItem> body) implements RpgDeclaration {
        public Procedure {
            keywords = List.copyOf(keywords);
            body = List.copyOf(body);
        }
    }

    /** {@code ctl-opt keywords*;} — module-level control options. */
    record CtlOpt(List<RpgKeyword> keywords) implements RpgDeclaration {
        public CtlOpt { keywords = List.copyOf(keywords); }
    }
}
