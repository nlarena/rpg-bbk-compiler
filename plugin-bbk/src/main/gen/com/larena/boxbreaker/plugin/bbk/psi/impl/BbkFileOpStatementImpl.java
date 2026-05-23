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

public class BbkFileOpStatementImpl extends ASTWrapperPsiElement implements BbkFileOpStatement {

  public BbkFileOpStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitFileOpStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public BbkChainOp getChainOp() {
    return findChildByClass(BbkChainOp.class);
  }

  @Override
  @Nullable
  public BbkCloseOp getCloseOp() {
    return findChildByClass(BbkCloseOp.class);
  }

  @Override
  @Nullable
  public BbkDeleteOp getDeleteOp() {
    return findChildByClass(BbkDeleteOp.class);
  }

  @Override
  @Nullable
  public BbkExfmtOp getExfmtOp() {
    return findChildByClass(BbkExfmtOp.class);
  }

  @Override
  @Nullable
  public BbkOpenOp getOpenOp() {
    return findChildByClass(BbkOpenOp.class);
  }

  @Override
  @Nullable
  public BbkReadOp getReadOp() {
    return findChildByClass(BbkReadOp.class);
  }

  @Override
  @Nullable
  public BbkReadeOp getReadeOp() {
    return findChildByClass(BbkReadeOp.class);
  }

  @Override
  @Nullable
  public BbkReadpOp getReadpOp() {
    return findChildByClass(BbkReadpOp.class);
  }

  @Override
  @Nullable
  public BbkReadpeOp getReadpeOp() {
    return findChildByClass(BbkReadpeOp.class);
  }

  @Override
  @Nullable
  public BbkSetgtOp getSetgtOp() {
    return findChildByClass(BbkSetgtOp.class);
  }

  @Override
  @Nullable
  public BbkSetllOp getSetllOp() {
    return findChildByClass(BbkSetllOp.class);
  }

  @Override
  @Nullable
  public BbkUnlockOp getUnlockOp() {
    return findChildByClass(BbkUnlockOp.class);
  }

  @Override
  @Nullable
  public BbkUpdateOp getUpdateOp() {
    return findChildByClass(BbkUpdateOp.class);
  }

  @Override
  @Nullable
  public BbkWriteOp getWriteOp() {
    return findChildByClass(BbkWriteOp.class);
  }

}
