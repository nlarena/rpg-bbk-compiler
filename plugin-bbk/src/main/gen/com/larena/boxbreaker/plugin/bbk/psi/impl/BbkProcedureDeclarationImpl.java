// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.larena.boxbreaker.plugin.bbk.psi.BbkTypes.*;
import com.larena.boxbreaker.plugin.bbk.psi.BbkNamedElementMixin;
import com.larena.boxbreaker.plugin.bbk.psi.*;

public class BbkProcedureDeclarationImpl extends BbkNamedElementMixin implements BbkProcedureDeclaration {

  public BbkProcedureDeclarationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitProcedureDeclaration(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public BbkBlockStatement getBlockStatement() {
    return findChildByClass(BbkBlockStatement.class);
  }

  @Override
  @Nullable
  public BbkInlineParamList getInlineParamList() {
    return findChildByClass(BbkInlineParamList.class);
  }

  @Override
  @NotNull
  public List<BbkProcModifier> getProcModifierList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, BbkProcModifier.class);
  }

  @Override
  @Nullable
  public BbkReturnType getReturnType() {
    return findChildByClass(BbkReturnType.class);
  }

  @Override
  @Nullable
  public PsiElement getIdent() {
    return findChildByType(IDENT);
  }

}
