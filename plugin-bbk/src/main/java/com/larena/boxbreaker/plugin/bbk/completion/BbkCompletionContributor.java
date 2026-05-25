package com.larena.boxbreaker.plugin.bbk.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionType;
import com.larena.boxbreaker.plugin.bbk.completion.providers.*;
import org.jetbrains.annotations.NotNull;

/**
 * Single registration point for every BBK completion provider.
 *
 * <p>Each {@code extend(...)} call wires one provider to the broad
 * {@link BbkCompletionPatterns#anyBbkElement()} pattern. The provider itself decides
 * (via its {@code applies(...)} method) whether the cursor position belongs to its
 * syntactic context.
 *
 * <p>This contributor also opts every BBK file into auto-popup after the first typed
 * character via {@link #invokeAutoPopup}.
 */
public class BbkCompletionContributor extends CompletionContributor {

    public BbkCompletionContributor() {
        // ----- Keywords / declarations / types / modifiers -----
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkTopLevelKeywordProvider());
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkCtlOptKeywordProvider());
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkTypeProvider());
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkVarModifierProvider());
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkDsModifierProvider());
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkParamModifierProvider());
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkFileKeywordProvider());
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkProcModifierProvider());
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkPrModifierProvider());

        // ----- Statements / file ops inside procedure bodies -----
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkStatementKeywordProvider());
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkFileOpProvider());

        // ----- Directives -----
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkDirectiveProvider());

        // ----- Star identifiers (*INPUT, *NOPASS, *NO/*YES, ...) -----
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkStarIdentProvider());

        // ----- Builtin functions -----
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkBifProvider());

        // ----- Block B: user-declared identifiers (variables, procs, constants, ...) -----
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkScopeCompletionProvider());

        // ----- Block B: member access (subfields after `.`) -----
        extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(), new BbkMemberCompletionProvider());

        // ----- Smart Completion: identifier list filtered by expected type at the caret -----
        extend(CompletionType.SMART, BbkCompletionPatterns.anyBbkElement(), new BbkSmartCompletionProvider());
    }

    @Override
    public boolean invokeAutoPopup(@NotNull com.intellij.psi.PsiElement position, char typeChar) {
        // Open the popup as soon as the user starts typing a letter or hyphen.
        return Character.isLetter(typeChar) || typeChar == '-' || typeChar == '*';
    }
}
