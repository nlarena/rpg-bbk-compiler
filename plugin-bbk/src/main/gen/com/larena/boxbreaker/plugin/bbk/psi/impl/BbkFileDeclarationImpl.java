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

public class BbkFileDeclarationImpl extends BbkNamedElementMixin implements BbkFileDeclaration {

  public BbkFileDeclarationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BbkVisitor visitor) {
    visitor.visitFileDeclaration(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BbkVisitor) accept((BbkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<BbkFKeyword> getFKeywordList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, BbkFKeyword.class);
  }

  @Override
  @Nullable
  public PsiElement getIdent() {
    return findChildByType(IDENT);
  }

}
