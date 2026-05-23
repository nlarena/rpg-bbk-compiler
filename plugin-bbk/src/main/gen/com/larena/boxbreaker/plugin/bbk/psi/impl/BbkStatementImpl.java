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

public class BbkStatementImpl extends ASTWrapperPsiElement implements BbkStatement {

  public BbkStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public BbkBreakStatement getBreakStatement() {
    return findChildByClass(BbkBreakStatement.class);
  }

  @Override
  @Nullable
  public BbkCallpStatement getCallpStatement() {
    return findChildByClass(BbkCallpStatement.class);
  }

  @Override
  @Nullable
  public BbkContinueStatement getContinueStatement() {
    return findChildByClass(BbkContinueStatement.class);
  }

  @Override
  @Nullable
  public BbkDoWhileStatement getDoWhileStatement() {
    return findChildByClass(BbkDoWhileStatement.class);
  }

  @Override
  @Nullable
  public BbkExpressionStatement getExpressionStatement() {
    return findChildByClass(BbkExpressionStatement.class);
  }

  @Override
  @Nullable
  public BbkExsrStatement getExsrStatement() {
    return findChildByClass(BbkExsrStatement.class);
  }

  @Override
  @Nullable
  public BbkFileOpStatement getFileOpStatement() {
    return findChildByClass(BbkFileOpStatement.class);
  }

  @Override
  @Nullable
  public BbkForStatement getForStatement() {
    return findChildByClass(BbkForStatement.class);
  }

  @Override
  @Nullable
  public BbkIfStatement getIfStatement() {
    return findChildByClass(BbkIfStatement.class);
  }

  @Override
  @Nullable
  public BbkLeavesrStatement getLeavesrStatement() {
    return findChildByClass(BbkLeavesrStatement.class);
  }

  @Override
  @Nullable
  public BbkMonitorStatement getMonitorStatement() {
    return findChildByClass(BbkMonitorStatement.class);
  }

  @Override
  @Nullable
  public BbkReturnStatement getReturnStatement() {
    return findChildByClass(BbkReturnStatement.class);
  }

  @Override
  @Nullable
  public BbkSelectStatement getSelectStatement() {
    return findChildByClass(BbkSelectStatement.class);
  }

  @Override
  @Nullable
  public BbkWhileStatement getWhileStatement() {
    return findChildByClass(BbkWhileStatement.class);
  }

}
