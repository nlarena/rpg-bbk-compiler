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

public class BbkTopLevelItemImpl extends ASTWrapperPsiElement implements BbkTopLevelItem {

  public BbkTopLevelItemImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitTopLevelItem(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public BbkConstantDeclaration getConstantDeclaration() {
    return findChildByClass(BbkConstantDeclaration.class);
  }

  @Override
  @Nullable
  public BbkCtlOptStatement getCtlOptStatement() {
    return findChildByClass(BbkCtlOptStatement.class);
  }

  @Override
  @Nullable
  public BbkDataStructureDeclaration getDataStructureDeclaration() {
    return findChildByClass(BbkDataStructureDeclaration.class);
  }

  @Override
  @Nullable
  public BbkDirective getDirective() {
    return findChildByClass(BbkDirective.class);
  }

  @Override
  @Nullable
  public BbkFileDeclaration getFileDeclaration() {
    return findChildByClass(BbkFileDeclaration.class);
  }

  @Override
  @Nullable
  public BbkProcedureDeclaration getProcedureDeclaration() {
    return findChildByClass(BbkProcedureDeclaration.class);
  }

  @Override
  @Nullable
  public BbkPrototypeDeclaration getPrototypeDeclaration() {
    return findChildByClass(BbkPrototypeDeclaration.class);
  }

  @Override
  @Nullable
  public BbkVariableDeclaration getVariableDeclaration() {
    return findChildByClass(BbkVariableDeclaration.class);
  }

}
