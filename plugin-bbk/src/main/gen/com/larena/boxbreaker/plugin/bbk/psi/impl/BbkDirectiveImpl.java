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

public class BbkDirectiveImpl extends ASTWrapperPsiElement implements BbkDirective {

  public BbkDirectiveImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitDirective(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public BbkPreDefineDirective getPreDefineDirective() {
    return findChildByClass(BbkPreDefineDirective.class);
  }

  @Override
  @Nullable
  public BbkPreElseDirective getPreElseDirective() {
    return findChildByClass(BbkPreElseDirective.class);
  }

  @Override
  @Nullable
  public BbkPreElseifDirective getPreElseifDirective() {
    return findChildByClass(BbkPreElseifDirective.class);
  }

  @Override
  @Nullable
  public BbkPreEndifDirective getPreEndifDirective() {
    return findChildByClass(BbkPreEndifDirective.class);
  }

  @Override
  @Nullable
  public BbkPreEofDirective getPreEofDirective() {
    return findChildByClass(BbkPreEofDirective.class);
  }

  @Override
  @Nullable
  public BbkPreIfDirective getPreIfDirective() {
    return findChildByClass(BbkPreIfDirective.class);
  }

  @Override
  @Nullable
  public BbkPreIncludeDirective getPreIncludeDirective() {
    return findChildByClass(BbkPreIncludeDirective.class);
  }

  @Override
  @Nullable
  public BbkPreUndefineDirective getPreUndefineDirective() {
    return findChildByClass(BbkPreUndefineDirective.class);
  }

}
