package com.larena.boxbreaker.plugin.bbk.types;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.index.BbkIndexKeys;
import com.larena.boxbreaker.plugin.bbk.psi.*;
import com.larena.boxbreaker.plugin.bbk.reference.BbkProjectScopeLookup;
import com.larena.boxbreaker.plugin.bbk.scope.BbkScopeWalker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Shallow, intentionally minimal type resolver.
 *
 * <p>Its only job in Block B is to answer:
 *
 * <blockquote>
 *   "Given this expression — typically the LHS of {@code obj.field} — which
 *   {@link BbkDataStructureDeclaration} does it eventually refer to?"
 * </blockquote>
 *
 * <p>It walks {@code LIKEDS(...)} chains of unbounded depth (with cycle detection) to
 * resolve aliases. It does not do full type inference; that's Block D's job. Anything
 * more complex than "identifier whose declaration is or aliases a DS" returns
 * {@code null}.
 */
public final class BbkTypeResolver {

    private BbkTypeResolver() {}

    /**
     * Returns the DS declaration that backs an expression, or {@code null} if unknown.
     * Currently supports:
     *
     * <ul>
     *   <li>A bare identifier that resolves to a {@code DCL-DS} (directly).</li>
     *   <li>A bare identifier that resolves to a {@code DCL-S name LIKEDS(otherDs)}
     *       (one or more aliasing hops).</li>
     *   <li>A bare identifier that resolves to a {@code DCL-S name LIKE(otherVar)} where
     *       the chain eventually lands on a DS.</li>
     * </ul>
     */
    public static @Nullable BbkDataStructureDeclaration dsOf(@NotNull PsiElement expression) {
        String name = bareIdentName(expression);
        if (name == null) return null;
        PsiElement target = resolveName(expression, name);
        if (target == null) return null;
        return followToDs(target, new HashSet<>());
    }

    /**
     * Resolves {@code name} from {@code context}, preferring local scope and
     * falling back to the project-wide data-structure stub index. The fallback
     * is what makes member access work when the {@code LIKEDS(...)} target DS
     * lives in another file.
     */
    private static @Nullable PsiElement resolveName(@NotNull PsiElement context, @NotNull String name) {
        PsiElement local = BbkScopeWalker.resolve(context, name);
        if (local != null) return local;
        return BbkProjectScopeLookup.findFirst(
            context.getProject(), name, BbkIndexKeys.DATA_STRUCTURE, BbkDataStructureDeclaration.class);
    }

    /**
     * Walks LIKE / LIKEDS / LIKEREC aliasing from a declaration toward a concrete DS.
     * Uses a visited set to avoid infinite loops on (malformed) cyclic chains.
     */
    private static @Nullable BbkDataStructureDeclaration followToDs(@NotNull PsiElement decl,
                                                                     @NotNull Set<PsiElement> visited) {
        if (!visited.add(decl)) return null;

        if (decl instanceof BbkDataStructureDeclaration ds) return ds;

        if (decl instanceof BbkVariableDeclaration v) {
            BbkLikeReference like = PsiTreeUtil.findChildOfType(v, BbkLikeReference.class);
            if (like != null) {
                String referenced = identNameInside(like);
                if (referenced != null) {
                    PsiElement next = resolveName(decl, referenced);
                    if (next != null) return followToDs(next, visited);
                }
            }
        }
        if (decl instanceof BbkInlineParam p) {
            BbkLikeReference like = PsiTreeUtil.findChildOfType(p, BbkLikeReference.class);
            if (like != null) {
                String referenced = identNameInside(like);
                if (referenced != null) {
                    PsiElement next = resolveName(decl, referenced);
                    if (next != null) return followToDs(next, visited);
                }
            }
        }
        if (decl instanceof BbkDsSubfield sf) {
            BbkLikeReference like = PsiTreeUtil.findChildOfType(sf, BbkLikeReference.class);
            if (like != null) {
                String referenced = identNameInside(like);
                if (referenced != null) {
                    PsiElement next = resolveName(decl, referenced);
                    if (next != null) return followToDs(next, visited);
                }
            }
        }
        return null;
    }

    private static @Nullable String bareIdentName(@NotNull PsiElement expr) {
        // For now, accept either an IDENT leaf or a one-token expression whose text is an IDENT.
        if (expr instanceof BbkPrimary primary) {
            PsiElement child = primary.getFirstChild();
            if (child != null && child.getNode().getElementType() == BbkTypes.IDENT) {
                return child.getText();
            }
        }
        if (expr.getNode() != null && expr.getNode().getElementType() == BbkTypes.IDENT) {
            return expr.getText();
        }
        // Fall back: look for the first IDENT leaf under expr.
        PsiElement id = PsiTreeUtil.findChildOfAnyType(expr, com.intellij.psi.impl.source.tree.LeafPsiElement.class);
        if (id != null && id.getNode().getElementType() == BbkTypes.IDENT) {
            return id.getText();
        }
        return null;
    }

    private static @Nullable String identNameInside(@NotNull PsiElement parens) {
        for (PsiElement c = parens.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c.getNode() != null && c.getNode().getElementType() == BbkTypes.IDENT) {
                return c.getText();
            }
        }
        return null;
    }
}
