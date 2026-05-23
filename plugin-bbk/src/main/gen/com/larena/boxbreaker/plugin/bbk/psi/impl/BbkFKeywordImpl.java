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

public class BbkFKeywordImpl extends ASTWrapperPsiElement implements BbkFKeyword {

  public BbkFKeywordImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitFKeyword(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public BbkExtfileFKeyword getExtfileFKeyword() {
    return findChildByClass(BbkExtfileFKeyword.class);
  }

  @Override
  @Nullable
  public BbkExtnameFKeyword getExtnameFKeyword() {
    return findChildByClass(BbkExtnameFKeyword.class);
  }

  @Override
  @Nullable
  public BbkInddsFKeyword getInddsFKeyword() {
    return findChildByClass(BbkInddsFKeyword.class);
  }

  @Override
  @Nullable
  public BbkInfdsFKeyword getInfdsFKeyword() {
    return findChildByClass(BbkInfdsFKeyword.class);
  }

  @Override
  @Nullable
  public BbkPrefixFKeyword getPrefixFKeyword() {
    return findChildByClass(BbkPrefixFKeyword.class);
  }

  @Override
  @Nullable
  public BbkRenameFKeyword getRenameFKeyword() {
    return findChildByClass(BbkRenameFKeyword.class);
  }

  @Override
  @Nullable
  public BbkSimpleFKeyword getSimpleFKeyword() {
    return findChildByClass(BbkSimpleFKeyword.class);
  }

  @Override
  @Nullable
  public BbkUsageFKeyword getUsageFKeyword() {
    return findChildByClass(BbkUsageFKeyword.class);
  }

}
