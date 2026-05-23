// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.larena.boxbreaker.plugin.bbk.psi.BbkTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.larena.boxbreaker.plugin.bbk.psi.*;

public class BbkVarModifierImpl extends ASTWrapperPsiElement implements BbkVarModifier {

  public BbkVarModifierImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitVarModifier(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public BbkBasedModifier getBasedModifier() {
    return findChildByClass(BbkBasedModifier.class);
  }

  @Override
  @Nullable
  public BbkDimModifier getDimModifier() {
    return findChildByClass(BbkDimModifier.class);
  }

  @Override
  @Nullable
  public BbkExportModifier getExportModifier() {
    return findChildByClass(BbkExportModifier.class);
  }

  @Override
  @Nullable
  public BbkInzModifier getInzModifier() {
    return findChildByClass(BbkInzModifier.class);
  }

  @Override
  @Nullable
  public BbkQualifiedModifier getQualifiedModifier() {
    return findChildByClass(BbkQualifiedModifier.class);
  }

  @Override
  @Nullable
  public BbkStaticModifier getStaticModifier() {
    return findChildByClass(BbkStaticModifier.class);
  }

}
