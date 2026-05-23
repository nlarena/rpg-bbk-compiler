// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface BbkExpressionStatement extends BbkPsiElement {

  @Nullable
  BbkAssignmentOp getAssignmentOp();

  @Nullable
  BbkAttributeModifier getAttributeModifier();

  @NotNull
  List<BbkExpression> getExpressionList();

}
