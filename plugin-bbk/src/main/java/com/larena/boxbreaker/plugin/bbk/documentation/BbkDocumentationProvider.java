package com.larena.boxbreaker.plugin.bbk.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.builtins.BbkBuiltinFunction;
import com.larena.boxbreaker.plugin.bbk.builtins.BbkBuiltinRegistry;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import com.larena.boxbreaker.plugin.bbk.types.BbkProcedureType;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkTypeInferrer;
import com.larena.boxbreaker.plugin.bbk.types.BbkUnknownType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides {@code Ctrl+Q} (Quick Documentation) and on-hover info for BBK.
 *
 * <p>Two kinds of targets:
 * <ul>
 *   <li><b>User declarations</b> (variable, constant, DS, subfield, procedure,
 *       prototype, parameter, file): shows the kind, the name, and the inferred
 *       type.</li>
 *   <li><b>Built-in functions</b>: when the caret is on an IDENT that does not
 *       resolve to a declaration but matches a BIF name, shows the BIF's
 *       signature, parameter docs, and description from
 *       {@link BbkBuiltinRegistry}.</li>
 * </ul>
 */
public class BbkDocumentationProvider extends AbstractDocumentationProvider {

    // -----------------------------------------------------------------------
    // Quick navigate (single-line, shown on hover / Ctrl hover)
    // -----------------------------------------------------------------------

    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        BbkBuiltinFunction bif = builtinFor(element, originalElement);
        if (bif != null) {
            return "(builtin) " + bif.signature() + " : " + bif.returnType();
        }
        if (element instanceof PsiNamedElement named) {
            return quickInfoForDeclaration(named);
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Full doc (Ctrl+Q)
    // -----------------------------------------------------------------------

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        BbkBuiltinFunction bif = builtinFor(element, originalElement);
        if (bif != null) {
            return renderBuiltin(bif);
        }
        if (element instanceof PsiNamedElement named) {
            return renderDeclaration(named);
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Catch builtin IDENTs that do not resolve to a declaration
    // -----------------------------------------------------------------------

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor,
                                                              @NotNull PsiFile file,
                                                              @Nullable PsiElement contextElement,
                                                              int targetOffset) {
        if (contextElement == null) return null;
        if (contextElement.getNode() == null
            || contextElement.getNode().getElementType() != BbkTypes.IDENT) {
            return null;
        }
        // Only claim it if it's a builtin name AND does not resolve to a declaration.
        String text = contextElement.getText();
        if (!BbkBuiltinRegistry.isBuiltin(text)) return null;
        if (resolvesToDeclaration(contextElement)) return null;
        return contextElement;
    }

    // -----------------------------------------------------------------------
    // Rendering — declarations
    // -----------------------------------------------------------------------

    private static @Nullable String quickInfoForDeclaration(@NotNull PsiNamedElement decl) {
        String kind = kindOf(decl);
        if (kind == null) return null;
        String name = decl.getName();
        BbkType type = BbkTypeInferrer.typeOf(decl);
        String typeStr = type instanceof BbkUnknownType ? "" : " : " + type.getDisplayName();
        return kind + " " + (name != null ? name : "<anonymous>") + typeStr;
    }

    private static @Nullable String renderDeclaration(@NotNull PsiNamedElement decl) {
        String kind = kindOf(decl);
        if (kind == null) return null;
        String name = decl.getName();
        BbkType type = BbkTypeInferrer.typeOf(decl);

        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(kind).append("</b> ")
          .append(escape(name != null ? name : "<anonymous>"));

        if (type instanceof BbkProcedureType pt) {
            sb.append("<br/>").append(escape(pt.getDisplayName()));
        } else if (!(type instanceof BbkUnknownType)) {
            sb.append(" : <code>").append(escape(type.getDisplayName())).append("</code>");
        }
        return sb.toString();
    }

    private static @Nullable String kindOf(@NotNull PsiNamedElement decl) {
        if (decl instanceof BbkVariableDeclaration)      return "Variable";
        if (decl instanceof BbkConstantDeclaration)      return "Constant";
        if (decl instanceof BbkDataStructureDeclaration) return "Data structure";
        if (decl instanceof BbkDsSubfield)               return "Subfield";
        if (decl instanceof BbkProcedureDeclaration)     return "Procedure";
        if (decl instanceof BbkPrototypeDeclaration)     return "Prototype";
        if (decl instanceof BbkInlineParam)              return "Parameter";
        if (decl instanceof BbkFileDeclaration)          return "File";
        if (decl instanceof BbkSubroutineDefinition)     return "Subroutine";
        return null;
    }

    // -----------------------------------------------------------------------
    // Rendering — builtins
    // -----------------------------------------------------------------------

    private static @NotNull String renderBuiltin(@NotNull BbkBuiltinFunction bif) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escape(bif.signature())).append("</b>");
        sb.append(" &rarr; <code>").append(escape(bif.returnType())).append("</code>");
        sb.append("<br/><br/>").append(bif.description());
        if (!bif.parameters().isEmpty()) {
            sb.append("<br/><br/><b>Parameters:</b><br/>");
            for (BbkBuiltinFunction.Parameter p : bif.parameters()) {
                sb.append("<code>").append(escape(p.name())).append("</code> &mdash; ")
                  .append(p.doc()).append("<br/>");
            }
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the builtin for {@code element} — either because {@code element}
     * is itself an IDENT builtin (from getCustomDocumentationElement), or
     * because the original element under the caret names a builtin.
     */
    private static @Nullable BbkBuiltinFunction builtinFor(@Nullable PsiElement element,
                                                           @Nullable PsiElement originalElement) {
        if (element != null && element.getNode() != null
            && element.getNode().getElementType() == BbkTypes.IDENT) {
            BbkBuiltinFunction f = BbkBuiltinRegistry.find(element.getText());
            if (f != null && !resolvesToDeclaration(element)) return f;
        }
        if (originalElement != null && originalElement.getNode() != null
            && originalElement.getNode().getElementType() == BbkTypes.IDENT) {
            BbkBuiltinFunction f = BbkBuiltinRegistry.find(originalElement.getText());
            if (f != null && !resolvesToDeclaration(originalElement)) return f;
        }
        return null;
    }

    /** True if the IDENT's enclosing reference resolves to a real declaration. */
    private static boolean resolvesToDeclaration(@NotNull PsiElement ident) {
        PsiElement composite = PsiTreeUtil.getParentOfType(ident, BbkPrimary.class);
        PsiElement owner = composite != null ? composite : ident;
        for (com.intellij.psi.PsiReference ref : owner.getReferences()) {
            if (ref instanceof com.intellij.psi.PsiPolyVariantReference poly) {
                for (com.intellij.psi.ResolveResult rr : poly.multiResolve(false)) {
                    if (rr.getElement() != null) return true;
                }
            } else if (ref.resolve() != null) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull String escape(@Nullable String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
