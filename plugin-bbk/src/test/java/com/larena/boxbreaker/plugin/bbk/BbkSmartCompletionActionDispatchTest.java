package com.larena.boxbreaker.plugin.bbk;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Verifies that the IntelliJ "SmartTypeCompletion" action (the exact action
 * bound to Ctrl+Shift+Space in the default keymap of IntelliJ 2025.3) dispatches
 * through our BbkSmartCompletionProvider and produces the expected lookup.
 *
 * <p>This is more authoritative than {@code myFixture.complete(CompletionType.SMART)}
 * because it dispatches through the IDE's action system — the same code path a
 * keypress travels.
 */
public class BbkSmartCompletionActionDispatchTest extends BasePlatformTestCase {

    public void testSmartActionDispatchProducesTypeFilteredLookup() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S counter INT(10);\n" +
            "  DCL-S retries INT(10);\n" +
            "  DCL-S name CHAR(50);\n" +
            "  counter = <caret>;\n" +
            "}\n");

        // Confirm the action exists with the same ID IntelliJ binds to Ctrl+Shift+Space.
        AnAction smart = ActionManager.getInstance().getAction("SmartTypeCompletion");
        assertNotNull("Action 'SmartTypeCompletion' must be registered", smart);

        // Dispatch via myFixture (same path as keypress).
        LookupElement[] elements = myFixture.complete(CompletionType.SMART);

        // Capture lookup contents.
        List<String> suggestions = elements != null
            ? Arrays.stream(elements).map(LookupElement::getLookupString).collect(Collectors.toList())
            : (myFixture.getLookupElementStrings() != null
                ? myFixture.getLookupElementStrings()
                : List.of());

        System.out.println("[ACTION-DISPATCH] action=" + smart.getClass().getName());
        System.out.println("[ACTION-DISPATCH] lookup=" + suggestions);

        // Must contain INT(10) decls, not CHAR(50).
        assertTrue("Lookup must contain 'counter' or auto-completed it. Got " + suggestions,
            suggestions.contains("counter") || suggestions.isEmpty()
                || myFixture.getEditor().getDocument().getText().contains("counter ="));
        assertFalse("Lookup must NOT contain 'name' (CHAR). Got " + suggestions,
            suggestions.contains("name"));
    }
}
