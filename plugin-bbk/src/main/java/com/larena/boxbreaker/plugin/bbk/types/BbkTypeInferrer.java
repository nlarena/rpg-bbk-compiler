package com.larena.boxbreaker.plugin.bbk.types;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The core engine of the type system. Two public entry points:
 * <ul>
 *   <li>{@link #typeOf(PsiNamedElement)} — infer the type of a declaration
 *       (variable, constant, DS, subfield, procedure / prototype, parameter).</li>
 *   <li>{@link #typeOf(BbkExpression)} — infer the type of an expression by
 *       walking the precedence chain down to {@code primary}.</li>
 * </ul>
 *
 * <p>Falls back to {@link BbkUnknownType#INSTANCE} when the PSI is partial or
 * the construct is not yet handled — keeps smart completion live in the
 * presence of half-typed code.
 */
public final class BbkTypeInferrer {

    private BbkTypeInferrer() {}

    // -----------------------------------------------------------------------
    // Declarations
    // -----------------------------------------------------------------------

    public static @NotNull BbkType typeOf(@Nullable PsiNamedElement decl) {
        if (decl == null) return BbkUnknownType.INSTANCE;

        if (decl instanceof BbkVariableDeclaration v) {
            return BbkTypeFromPsi.fromSpec(v.getTypeSpecification());
        }
        if (decl instanceof BbkDsSubfield sf) {
            return BbkTypeFromPsi.fromSpec(sf.getTypeSpecification());
        }
        if (decl instanceof BbkInlineParam p) {
            return BbkTypeFromPsi.fromSpec(p.getTypeSpecification());
        }
        if (decl instanceof BbkConstantDeclaration c) {
            return typeOfConstant(c);
        }
        if (decl instanceof BbkDataStructureDeclaration ds) {
            return new BbkStructType(ds);
        }
        if (decl instanceof BbkProcedureDeclaration proc) {
            return procedureType(proc.getInlineParamList(), proc.getReturnType());
        }
        if (decl instanceof BbkPrototypeDeclaration pr) {
            return procedureType(pr.getInlineParamList(), pr.getReturnType());
        }
        if (decl instanceof BbkFileDeclaration f) {
            return BbkUnknownType.INSTANCE;  // file handles are not a value-type for smart completion
        }
        if (decl instanceof BbkSubroutineDefinition) {
            return BbkUnknownType.INSTANCE;  // subroutines are not call targets in expressions
        }
        return BbkUnknownType.INSTANCE;
    }

    private static @NotNull BbkType typeOfConstant(@NotNull BbkConstantDeclaration c) {
        BbkConstantValue value = c.getConstantValue();
        if (value == null) return BbkUnknownType.INSTANCE;
        BbkLiteral lit = value.getLiteral();
        if (lit != null) return typeOfLiteral(lit);
        // CONST(IDENT) or CONST(literal): unwrap.
        BbkConstWrapper w = value.getConstWrapper();
        if (w != null) {
            BbkLiteral innerLit = PsiTreeUtil.findChildOfType(w, BbkLiteral.class);
            if (innerLit != null) return typeOfLiteral(innerLit);
        }
        // Bare TRUE/FALSE/NULL after `=` — model as BOOL or POINTER respectively.
        String text = value.getText().trim();
        if ("*TRUE".equalsIgnoreCase(text) || "*FALSE".equalsIgnoreCase(text)) return BbkScalarType.BOOL;
        if ("*NULL".equalsIgnoreCase(text)) return BbkScalarType.POINTER;
        return BbkUnknownType.INSTANCE;
    }

    private static @NotNull BbkType procedureType(@Nullable BbkInlineParamList paramList, @Nullable BbkReturnType ret) {
        List<BbkProcedureType.Parameter> params = new ArrayList<>();
        if (paramList != null) {
            for (BbkInlineParam p : PsiTreeUtil.getChildrenOfTypeAsList(paramList, BbkInlineParam.class)) {
                String name = p.getName();
                if (name == null) name = "?";
                BbkType pType = BbkTypeFromPsi.fromSpec(p.getTypeSpecification());
                boolean byValue = false, byConst = false;
                for (BbkParamModifier m : p.getParamModifierList()) {
                    String t = m.getText();
                    if ("VALUE".equalsIgnoreCase(t)) byValue = true;
                    else if ("CONST".equalsIgnoreCase(t)) byConst = true;
                }
                params.add(new BbkProcedureType.Parameter(name, pType, byValue, byConst));
            }
        }
        BbkType retType = ret != null ? BbkTypeFromPsi.fromSpec(ret.getTypeSpecification()) : BbkScalarType.VOID;
        return new BbkProcedureType(params, retType);
    }

    // -----------------------------------------------------------------------
    // Expressions
    // -----------------------------------------------------------------------

    public static @NotNull BbkType typeOf(@Nullable BbkExpression expr) {
        if (expr == null) return BbkUnknownType.INSTANCE;
        return typeOfAny(expr);
    }

    /** Walks the precedence chain. Each layer either has a binary operator or unwraps to the next. */
    private static @NotNull BbkType typeOfAny(@NotNull PsiElement e) {
        if (e instanceof BbkExpression ex) return typeOfAny(ex.getTernaryExpression());

        if (e instanceof BbkTernaryExpression t) {
            // condition ? then : else  — type is the type of `then` (operands assumed compatible).
            List<BbkLogicalOrExpression> first = PsiTreeUtil.getChildrenOfTypeAsList(t, BbkLogicalOrExpression.class);
            List<BbkExpression> nestedExpr = PsiTreeUtil.getChildrenOfTypeAsList(t, BbkExpression.class);
            if (!nestedExpr.isEmpty()) return typeOfAny(nestedExpr.get(0));     // `then` branch
            if (!first.isEmpty()) return typeOfAny(first.get(0));
            return BbkUnknownType.INSTANCE;
        }

        if (e instanceof BbkLogicalOrExpression || e instanceof BbkLogicalAndExpression
            || e instanceof BbkEqualityExpression || e instanceof BbkRelationalExpression) {
            // Boolean-yielding operators.
            // BUT: when the chain has only one operand the type passes through.
            PsiElement[] children = e.getChildren();
            if (children.length == 1) return typeOfAny(children[0]);
            return BbkScalarType.BOOL;
        }

        if (e instanceof BbkBitwiseOrExpression || e instanceof BbkBitwiseXorExpression
            || e instanceof BbkBitwiseAndExpression || e instanceof BbkShiftExpression
            || e instanceof BbkAdditiveExpression || e instanceof BbkMultiplicativeExpression
            || e instanceof BbkPowerExpression) {
            // Arithmetic / bitwise — type of the first operand (V1; promotion deferred).
            PsiElement[] children = e.getChildren();
            if (children.length == 0) return BbkUnknownType.INSTANCE;
            return typeOfAny(children[0]);
        }

        if (e instanceof BbkUnaryExpression u) {
            PsiElement[] children = u.getChildren();
            // unary_expression ::= (PLUS|MINUS|BANG|TILDE) unary_expression | postfix_expression
            BbkPostfixExpression pe = PsiTreeUtil.getChildOfType(u, BbkPostfixExpression.class);
            if (pe != null) return typeOfAny(pe);
            BbkUnaryExpression inner = PsiTreeUtil.getChildOfType(u, BbkUnaryExpression.class);
            if (inner != null) {
                // ! and ~ → BOOL / same; + - → numeric same. V1 returns inner type.
                return typeOfAny(inner);
            }
            return BbkUnknownType.INSTANCE;
        }

        if (e instanceof BbkPostfixExpression pe) return typeOfPostfix(pe);
        if (e instanceof BbkPrimary p) return typeOfPrimary(p);
        if (e instanceof BbkLiteral lit) return typeOfLiteral(lit);

        return BbkUnknownType.INSTANCE;
    }

    private static @NotNull BbkType typeOfPostfix(@NotNull BbkPostfixExpression pe) {
        BbkType current = typeOfPrimary(pe.getPrimary());
        for (BbkPostfixSuffix suffix : pe.getPostfixSuffixList()) {
            current = applySuffix(current, suffix);
        }
        return current;
    }

    private static @NotNull BbkType applySuffix(@NotNull BbkType base, @NotNull BbkPostfixSuffix suffix) {
        // Call: (args) — applies to a procedure type; result is the return type.
        if (suffix.getArgumentList() != null || hasEmptyParens(suffix)) {
            if (base instanceof BbkProcedureType pt) return pt.getReturnType();
            return BbkUnknownType.INSTANCE;
        }
        // Subscript: [idx] — element of array.
        if (suffix.getSubscriptList() != null) {
            if (base instanceof BbkArrayType arr) return arr.getElement();
            return base;  // V1: passthrough if not strictly array
        }
        // Member access: .IDENT — DS subfield.
        PsiElement memberId = suffix.getIdent();
        if (memberId != null && base instanceof BbkStructType st) {
            BbkDsSubfield sf = findSubfield(st.getDeclaration(), memberId.getText());
            if (sf != null) return BbkTypeFromPsi.fromSpec(sf.getTypeSpecification());
        }
        return BbkUnknownType.INSTANCE;
    }

    private static boolean hasEmptyParens(@NotNull BbkPostfixSuffix s) {
        // Suffix `()` has no argumentList and no subscriptList and no ident.
        return s.getArgumentList() == null && s.getSubscriptList() == null && s.getIdent() == null;
    }

    private static @Nullable BbkDsSubfield findSubfield(@NotNull BbkDataStructureDeclaration ds, @NotNull String name) {
        for (BbkDsSubfield sf : PsiTreeUtil.findChildrenOfType(ds, BbkDsSubfield.class)) {
            if (name.equalsIgnoreCase(sf.getName())) return sf;
        }
        return null;
    }

    private static @NotNull BbkType typeOfPrimary(@NotNull BbkPrimary p) {
        BbkLiteral lit = p.getLiteral();
        if (lit != null) return typeOfLiteral(lit);
        if (p.getExpression() != null) return typeOf(p.getExpression());
        PsiElement ident = p.getIdent();
        if (ident != null) return typeOfIdentReference(p);
        PsiElement star = p.getStarIdent();
        if (star != null) {
            String t = star.getText();
            if ("*TRUE".equalsIgnoreCase(t) || "*FALSE".equalsIgnoreCase(t)) return BbkScalarType.BOOL;
            if ("*NULL".equalsIgnoreCase(t)) return BbkScalarType.POINTER;
        }
        return BbkUnknownType.INSTANCE;
    }

    private static @NotNull BbkType typeOfIdentReference(@NotNull BbkPrimary primary) {
        for (PsiReference ref : primary.getReferences()) {
            if (ref instanceof com.intellij.psi.PsiPolyVariantReference poly) {
                ResolveResult[] rs = poly.multiResolve(false);
                for (ResolveResult r : rs) {
                    PsiElement target = r.getElement();
                    if (target instanceof PsiNamedElement n) {
                        BbkType t = typeOf(n);
                        if (!(t instanceof BbkUnknownType)) return t;
                    }
                }
            } else {
                PsiElement target = ref.resolve();
                if (target instanceof PsiNamedElement n) {
                    BbkType t = typeOf(n);
                    if (!(t instanceof BbkUnknownType)) return t;
                }
            }
        }
        return BbkUnknownType.INSTANCE;
    }

    // -----------------------------------------------------------------------
    // Literals
    // -----------------------------------------------------------------------

    public static @NotNull BbkType typeOfLiteral(@NotNull BbkLiteral lit) {
        if (lit.getIntLit() != null || lit.getIntLitHex() != null || lit.getIntLitOct() != null) {
            return BbkScalarType.INT_LITERAL;
        }
        if (lit.getDecLit() != null) return BbkScalarType.DEC_LITERAL;
        if (lit.getFloatLit() != null) return BbkScalarType.FLOAT_LITERAL;
        if (lit.getStrLit() != null) return BbkScalarType.CHAR_LITERAL;
        return BbkUnknownType.INSTANCE;
    }
}
