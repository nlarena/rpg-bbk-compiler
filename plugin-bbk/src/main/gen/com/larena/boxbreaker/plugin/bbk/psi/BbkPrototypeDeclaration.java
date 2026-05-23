// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface BbkPrototypeDeclaration extends BbkPsiElement {

  @Nullable
  BbkInlineParamList getInlineParamList();

  @NotNull
  List<BbkPrModifier> getPrModifierList();

  @Nullable
  BbkReturnType getReturnType();

  @Nullable
  PsiElement getIdent();

}
