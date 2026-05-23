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

public class BbkBlockItemImpl extends ASTWrapperPsiElement implements BbkBlockItem {

  public BbkBlockItemImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitBlockItem(this);
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
  public BbkStatement getStatement() {
    return findChildByClass(BbkStatement.class);
  }

  @Override
  @Nullable
  public BbkSubroutineDefinition getSubroutineDefinition() {
    return findChildByClass(BbkSubroutineDefinition.class);
  }

  @Override
  @Nullable
  public BbkVariableDeclaration getVariableDeclaration() {
    return findChildByClass(BbkVariableDeclaration.class);
  }

}
