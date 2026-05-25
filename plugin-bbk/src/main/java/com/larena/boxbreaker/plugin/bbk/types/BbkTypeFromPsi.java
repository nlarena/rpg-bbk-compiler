package com.larena.boxbreaker.plugin.bbk.types;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.larena.boxbreaker.plugin.bbk.psi.BbkDataStructureDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkLikeReference;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPrimitiveType;
import com.larena.boxbreaker.plugin.bbk.psi.BbkPrimitiveTypeSpec;
import com.larena.boxbreaker.plugin.bbk.psi.BbkTypeArgs;
import com.larena.boxbreaker.plugin.bbk.psi.BbkTypeSpecification;
import com.larena.boxbreaker.plugin.bbk.psi.BbkTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Converts a {@link BbkTypeSpecification} PSI node into the corresponding
 * {@link BbkType} from this package. Two flavours:
 *
 * <ul>
 *   <li>{@code primitive_type_spec} → {@link BbkScalarType} parsing the
 *       integer args (precision / decimals).</li>
 *   <li>{@code like_reference} → resolves the IDENT inside
 *       {@code LIKE/LIKEDS/LIKEREC(x)} to its declaration, then recurses /
 *       wraps in {@link BbkStructType} as appropriate.</li>
 * </ul>
 *
 * <p>Cycle protection: a per-call {@code Set<PsiElement>} prevents infinite
 * recursion across pathological {@code LIKE} chains
 * ({@code a LIKE(b); b LIKE(a);}).
 */
public final class BbkTypeFromPsi {

    private BbkTypeFromPsi() {}

    public static @NotNull BbkType fromSpec(@Nullable BbkTypeSpecification spec) {
        return fromSpec(spec, new HashSet<>());
    }

    private static @NotNull BbkType fromSpec(@Nullable BbkTypeSpecification spec, @NotNull Set<PsiElement> visited) {
        if (spec == null) return BbkUnknownType.INSTANCE;

        BbkPrimitiveTypeSpec prim = spec.getPrimitiveTypeSpec();
        if (prim != null) return fromPrimitive(prim);

        BbkLikeReference like = spec.getLikeReference();
        if (like != null) return fromLike(like, visited);

        return BbkUnknownType.INSTANCE;
    }

    private static @NotNull BbkType fromPrimitive(@NotNull BbkPrimitiveTypeSpec prim) {
        BbkPrimitiveType kwNode = prim.getPrimitiveType();
        BbkScalarType.Kind kind = kindFromKeyword(kwNode);
        if (kind == null) return BbkUnknownType.INSTANCE;

        BbkTypeArgs args = prim.getTypeArgs();
        if (args == null) {
            return BbkScalarType.of(kind);
        }
        int[] ints = readIntArgs(args);
        if (ints.length == 0) return BbkScalarType.of(kind);
        if (ints.length == 1) return BbkScalarType.of(kind, ints[0]);
        return BbkScalarType.of(kind, ints[0], ints[1]);
    }

    private static @NotNull BbkType fromLike(@NotNull BbkLikeReference like, @NotNull Set<PsiElement> visited) {
        PsiElement ident = like.getIdent();
        if (ident == null) return BbkUnknownType.INSTANCE;

        // LIKEDS / LIKEREC → expected a DS declaration; LIKE → expected a variable / subfield.
        String kwText = firstKeywordText(like);
        boolean isStructForm = "LIKEDS".equalsIgnoreCase(kwText) || "LIKEREC".equalsIgnoreCase(kwText);

        PsiElement resolved = resolveIdent(ident);
        if (resolved == null) return BbkUnknownType.INSTANCE;
        if (!visited.add(resolved)) return BbkUnknownType.INSTANCE; // cycle

        if (isStructForm) {
            if (resolved instanceof BbkDataStructureDeclaration ds) {
                return new BbkStructType(ds);
            }
            return BbkUnknownType.INSTANCE;
        }

        // Plain LIKE(x) — clone the type of x. Walk its type spec.
        BbkTypeSpecification innerSpec = PsiTreeUtil.findChildOfType(resolved, BbkTypeSpecification.class);
        return fromSpec(innerSpec, visited);
    }

    // ----- helpers -----

    private static @Nullable PsiElement resolveIdent(@NotNull PsiElement ident) {
        for (com.intellij.psi.PsiReference ref : ident.getReferences()) {
            PsiElement r = ref.resolve();
            if (r != null) return r;
        }
        // Fallback: look at parent composite that owns the reference (BbkLikeReference itself).
        PsiElement parent = ident.getParent();
        if (parent != null) {
            for (com.intellij.psi.PsiReference ref : parent.getReferences()) {
                PsiElement r = ref.resolve();
                if (r != null) return r;
            }
        }
        return null;
    }

    private static @Nullable BbkScalarType.Kind kindFromKeyword(@NotNull BbkPrimitiveType kwNode) {
        ASTNode child = kwNode.getNode().getFirstChildNode();
        if (child == null) return null;
        com.intellij.psi.tree.IElementType type = child.getElementType();
        if (type == BbkTypes.KW_INT) return BbkScalarType.Kind.INT;
        if (type == BbkTypes.KW_UNS) return BbkScalarType.Kind.UNS;
        if (type == BbkTypes.KW_CHAR) return BbkScalarType.Kind.CHAR;
        if (type == BbkTypes.KW_VARCHAR) return BbkScalarType.Kind.VARCHAR;
        if (type == BbkTypes.KW_PACKED) return BbkScalarType.Kind.PACKED;
        if (type == BbkTypes.KW_ZONED) return BbkScalarType.Kind.ZONED;
        if (type == BbkTypes.KW_BINDEC) return BbkScalarType.Kind.BINDEC;
        if (type == BbkTypes.KW_FLOAT) return BbkScalarType.Kind.FLOAT;
        if (type == BbkTypes.KW_BOOL) return BbkScalarType.Kind.BOOL;
        if (type == BbkTypes.KW_DATE) return BbkScalarType.Kind.DATE;
        if (type == BbkTypes.KW_TIME) return BbkScalarType.Kind.TIME;
        if (type == BbkTypes.KW_TIMESTAMP) return BbkScalarType.Kind.TIMESTAMP;
        if (type == BbkTypes.KW_POINTER) return BbkScalarType.Kind.POINTER;
        if (type == BbkTypes.KW_VOID) return BbkScalarType.Kind.VOID;
        return null;
    }

    private static @NotNull String firstKeywordText(@NotNull BbkLikeReference like) {
        ASTNode child = like.getNode().getFirstChildNode();
        return child != null ? child.getText() : "";
    }

    /** Returns the integer literal values inside {@code (INT_LIT [: INT_LIT])}. */
    private static int @NotNull [] readIntArgs(@NotNull BbkTypeArgs args) {
        java.util.List<Integer> out = new java.util.ArrayList<>(2);
        for (ASTNode n = args.getNode().getFirstChildNode(); n != null; n = n.getTreeNext()) {
            if (n.getElementType() == BbkTypes.INT_LIT) {
                try {
                    out.add(Integer.parseInt(n.getText()));
                } catch (NumberFormatException ignored) {}
            }
        }
        int[] arr = new int[out.size()];
        for (int i = 0; i < out.size(); i++) arr[i] = out.get(i);
        return arr;
    }
}
