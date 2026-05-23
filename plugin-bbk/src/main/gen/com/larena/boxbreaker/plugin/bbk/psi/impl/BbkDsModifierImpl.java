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

public class BbkDsModifierImpl extends ASTWrapperPsiElement implements BbkDsModifier {

  public BbkDsModifierImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitDsModifier(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public BbkAlignModifier getAlignModifier() {
    return findChildByClass(BbkAlignModifier.class);
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
  public BbkExtnameDsModifier getExtnameDsModifier() {
    return findChildByClass(BbkExtnameDsModifier.class);
  }

  @Override
  @Nullable
  public BbkInfdsDsModifier getInfdsDsModifier() {
    return findChildByClass(BbkInfdsDsModifier.class);
  }

  @Override
  @Nullable
  public BbkInzModifier getInzModifier() {
    return findChildByClass(BbkInzModifier.class);
  }

  @Override
  @Nullable
  public BbkLikedsDsModifier getLikedsDsModifier() {
    return findChildByClass(BbkLikedsDsModifier.class);
  }

  @Override
  @Nullable
  public BbkLikerecDsModifier getLikerecDsModifier() {
    return findChildByClass(BbkLikerecDsModifier.class);
  }

  @Override
  @Nullable
  public BbkQualifiedModifier getQualifiedModifier() {
    return findChildByClass(BbkQualifiedModifier.class);
  }

  @Override
  @Nullable
  public BbkTemplateModifier getTemplateModifier() {
    return findChildByClass(BbkTemplateModifier.class);
  }

}
