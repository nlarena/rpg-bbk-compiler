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

public class BbkUnaryExpressionImpl extends ASTWrapperPsiElement implements BbkUnaryExpression {

  public BbkUnaryExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitUnaryExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public BbkPostfixExpression getPostfixExpression() {
    return findChildByClass(BbkPostfixExpression.class);
  }

  @Override
  @Nullable
  public BbkUnaryExpression getUnaryExpression() {
    return findChildByClass(BbkUnaryExpression.class);
  }

}
