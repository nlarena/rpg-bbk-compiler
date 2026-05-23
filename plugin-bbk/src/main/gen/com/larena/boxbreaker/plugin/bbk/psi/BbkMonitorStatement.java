// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface BbkMonitorStatement extends BbkPsiElement {

  @Nullable
  BbkBlockStatement getBlockStatement();

  @NotNull
  List<BbkOnErrorClause> getOnErrorClauseList();

  @Nullable
  BbkOnExitClause getOnExitClause();

}
