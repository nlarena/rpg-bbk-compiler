package com.larena.boxbreaker.plugin.bbk.completion.providers;

import com.larena.boxbreaker.plugin.bbk.BbkBundle;
import com.larena.boxbreaker.plugin.bbk.completion.BbkCompletionPatterns;
import com.larena.boxbreaker.plugin.bbk.completion.InsertHandlers;
import com.larena.boxbreaker.plugin.bbk.completion.Suggestion;
import com.larena.boxbreaker.plugin.bbk.icons.BbkIcons;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggests control-flow and structured statement keywords inside a procedure body
 * (and any nested block): {@code if}, {@code else}, {@code while}, {@code do},
 * {@code for}, {@code select}, {@code when}, {@code other}, {@code return},
 * {@code break}, {@code continue}, {@code monitor}, {@code on-error}, {@code on-exit},
 * {@code begsr}, {@code endsr}, {@code exsr}, {@code leavesr}.
 *
 * <p>Context-sensitive keywords are gated by extra conditions:
 * {@code when}/{@code other} only inside a {@code select} block;
 * {@code on-error}/{@code on-exit} only inside a {@code monitor};
 * {@code leavesr} only inside a {@code begsr ... endsr} body.
 */
public class BbkStatementKeywordProvider extends BbkKeywordProviderBase {

    @Override
    protected boolean applies(@NotNull PsiElement position) {
        return BbkCompletionPatterns.atStatementStart(position);
    }

    @Override
    protected @NotNull List<Suggestion> suggestions(@NotNull PsiElement position) {
        String type = BbkBundle.message("completion.type.statementKeyword");
        List<Suggestion> out = new ArrayList<>();

        // Always available statement starters
        out.add(Suggestion.full("if",       "(cond) { ... }",  type, BbkIcons.Category.STATEMENT, InsertHandlers.CONTROL_FLOW_BLOCK));
        out.add(Suggestion.full("while",    "(cond) { ... }",  type, BbkIcons.Category.STATEMENT, InsertHandlers.CONTROL_FLOW_BLOCK));
        out.add(Suggestion.full("do",       "{ ... } while ()", type, BbkIcons.Category.STATEMENT, InsertHandlers.BLOCK_ONLY));
        out.add(Suggestion.full("for",      "(init;cond;upd) { ... }", type, BbkIcons.Category.STATEMENT, InsertHandlers.CONTROL_FLOW_BLOCK));
        out.add(Suggestion.full("select",   "{ when ... }",    type, BbkIcons.Category.STATEMENT, InsertHandlers.BLOCK_ONLY));
        out.add(Suggestion.full("monitor",  "{ ... } on-error", type, BbkIcons.Category.STATEMENT, InsertHandlers.BLOCK_ONLY));
        out.add(Suggestion.plain("return",                       type, BbkIcons.Category.STATEMENT));
        out.add(Suggestion.plain("break",                        type, BbkIcons.Category.STATEMENT));
        out.add(Suggestion.plain("continue",                     type, BbkIcons.Category.STATEMENT));
        out.add(Suggestion.full("begsr",    " <name> { ... }", type, BbkIcons.Category.STATEMENT, InsertHandlers.TRAILING_SPACE));
        out.add(Suggestion.full("exsr",     " <subr>;",        type, BbkIcons.Category.STATEMENT, InsertHandlers.TRAILING_SPACE));

        // else: only when there is an if at the cursor's parent chain whose block has just closed.
        if (elseLikelyValid(position)) {
            out.add(Suggestion.full("else", "{ ... }",         type, BbkIcons.Category.STATEMENT, InsertHandlers.BLOCK_ONLY));
        }

        // when / other: only inside a select block
        if (BbkCompletionPatterns.insideSelectBlock(position)) {
            out.add(Suggestion.full("when",  "(cond) { ... }", type, BbkIcons.Category.STATEMENT, InsertHandlers.CONTROL_FLOW_BLOCK));
            out.add(Suggestion.full("other", "{ ... }",        type, BbkIcons.Category.STATEMENT, InsertHandlers.BLOCK_ONLY));
        }

        // on-error / on-exit: only inside a monitor
        if (BbkCompletionPatterns.afterMonitorBlock(position)) {
            out.add(Suggestion.full("on-error", "(<status>) { ... }", type, BbkIcons.Category.STATEMENT, InsertHandlers.CONTROL_FLOW_BLOCK));
            out.add(Suggestion.full("on-exit",  "{ ... }",     type, BbkIcons.Category.STATEMENT, InsertHandlers.BLOCK_ONLY));
        }

        // endsr / leavesr: only inside a subroutine
        if (BbkCompletionPatterns.insideSubroutine(position)) {
            out.add(Suggestion.plain("endsr",                    type, BbkIcons.Category.STATEMENT));
            out.add(Suggestion.plain("leavesr",                  type, BbkIcons.Category.STATEMENT));
        }

        return out;
    }

    /**
     * Heuristic: {@code else} is offered if the cursor sits where an {@link BbkIfStatement}
     * ancestor exists whose body ends before the cursor — i.e. the user just closed an
     * {@code if} block and is typing the next token at the same nesting level. We accept
     * any position inside or right after an {@code if} as a permissive default; false
     * positives are acceptable since the keyword has no harmful effect when wrongly chosen.
     */
    private static boolean elseLikelyValid(@NotNull PsiElement position) {
        return com.intellij.psi.util.PsiTreeUtil.getParentOfType(position,
            com.larena.boxbreaker.plugin.bbk.psi.BbkIfStatement.class) != null
            || com.intellij.psi.util.PsiTreeUtil.getPrevSiblingOfType(position,
                com.larena.boxbreaker.plugin.bbk.psi.BbkIfStatement.class) != null;
    }
}
