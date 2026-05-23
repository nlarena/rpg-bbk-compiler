// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface BbkDirective extends BbkPsiElement {

  @Nullable
  BbkPreDefineDirective getPreDefineDirective();

  @Nullable
  BbkPreElseDirective getPreElseDirective();

  @Nullable
  BbkPreElseifDirective getPreElseifDirective();

  @Nullable
  BbkPreEndifDirective getPreEndifDirective();

  @Nullable
  BbkPreEofDirective getPreEofDirective();

  @Nullable
  BbkPreIfDirective getPreIfDirective();

  @Nullable
  BbkPreIncludeDirective getPreIncludeDirective();

  @Nullable
  BbkPreUndefineDirective getPreUndefineDirective();

}
