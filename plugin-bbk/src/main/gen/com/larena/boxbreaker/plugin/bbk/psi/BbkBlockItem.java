// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface BbkBlockItem extends BbkPsiElement {

  @Nullable
  BbkConstantDeclaration getConstantDeclaration();

  @Nullable
  BbkDataStructureDeclaration getDataStructureDeclaration();

  @Nullable
  BbkDirective getDirective();

  @Nullable
  BbkStatement getStatement();

  @Nullable
  BbkSubroutineDefinition getSubroutineDefinition();

  @Nullable
  BbkVariableDeclaration getVariableDeclaration();

}
