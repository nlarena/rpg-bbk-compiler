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

public class BbkForAssignmentImpl extends ASTWrapperPsiElement implements BbkForAssignment {

  public BbkForAssignmentImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitForAssignment(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public BbkAssignmentOp getAssignmentOp() {
    return findNotNullChildByClass(BbkAssignmentOp.class);
  }

  @Override
  @Nullable
  public BbkExpression getExpression() {
    return findChildByClass(BbkExpression.class);
  }

  @Override
  @NotNull
  public BbkLvalue getLvalue() {
    return findNotNullChildByClass(BbkLvalue.class);
  }

}
