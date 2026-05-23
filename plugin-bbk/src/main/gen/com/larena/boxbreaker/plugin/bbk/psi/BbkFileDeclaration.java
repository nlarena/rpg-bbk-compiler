// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;

public interface BbkFileDeclaration extends PsiNamedElement {

  @NotNull
  List<BbkFKeyword> getFKeywordList();

  @Nullable
  PsiElement getIdent();

}
