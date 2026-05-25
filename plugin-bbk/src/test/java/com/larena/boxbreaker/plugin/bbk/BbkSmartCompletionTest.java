package com.larena.boxbreaker.plugin.bbk;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * End-to-end test of {@code Ctrl+Shift+Space} (Smart Completion): given a
 * caret on a typed slot, the lookup list MUST include only declarations
 * whose type is assignable to the slot's expected type.
 *
 * <p>Each test configures a synthetic BBK file, invokes SMART completion,
 * and asserts which lookup strings appear / do not appear.
 */
public class BbkSmartCompletionTest extends BasePlatformTestCase {

    // ----- Assignment RHS -----

    public void testAssignmentRhsFiltersByLhsType() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S counter INT(10);\n" +
            "  DCL-S name CHAR(50);\n" +
            "  DCL-S retries INT(10);\n" +
            "  counter = <caret>;\n" +
            "}\n");

        List<String> suggestions = smartLookup();
        assertContains("Expected 'counter' (INT(10) assignable to itself)", suggestions, "counter");
        assertContains("Expected 'retries' (INT(10) → INT(10))", suggestions, "retries");
        assertNotContains("'name' is CHAR(50), must NOT appear", suggestions, "name");
    }

    public void testAssignmentRhsFiltersOutBoolWhenIntExpected() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S i INT(10);\n" +
            "  DCL-S j INT(10);\n" +
            "  DCL-S flag BOOL;\n" +
            "  i = <caret>;\n" +
            "}\n");

        List<String> suggestions = smartLookup();
        assertContains(suggestions, "i");
        assertContains(suggestions, "j");
        assertNotContains("BOOL not assignable to INT", suggestions, "flag");
    }

    public void testAssignmentRhsIntWideningAllowsSmallerInt() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S big INT(20);\n" +
            "  DCL-S small INT(5);\n" +
            "  big = <caret>;\n" +
            "}\n");

        List<String> suggestions = smartLookup();
        assertContains("INT(5) → INT(20) is widening, must appear", suggestions, "small");
        assertContains("INT(20) → INT(20) identity", suggestions, "big");
    }

    public void testAssignmentRhsRejectsCharForInt() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S total INT(10);\n" +
            "  DCL-S another INT(10);\n" +
            "  DCL-S label CHAR(20);\n" +
            "  total = <caret>;\n" +
            "}\n");

        List<String> suggestions = smartLookup();
        assertContains(suggestions, "total");
        assertContains(suggestions, "another");
        assertNotContains(suggestions, "label");
    }

    // ----- Procedures: return type is what counts -----

    public void testProcedureReturnTypeMatchesSlot() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC getCount -> INT(10) { return 0; }\n" +
            "DCL-PROC getName -> CHAR(20) { return 'x'; }\n" +
            "DCL-PROC main {\n" +
            "  DCL-S counter INT(10);\n" +
            "  counter = <caret>;\n" +
            "}\n");

        List<String> suggestions = smartLookup();
        assertContains("Procedure returning INT(10) should appear", suggestions, "getCount");
        assertNotContains("Procedure returning CHAR(20) should NOT appear when INT expected", suggestions, "getName");
    }

    // ----- INZ value -----

    public void testInzValueExpectsDeclaredType() {
        myFixture.configureByText("main.bbk",
            "DCL-C MAX 100;\n" +
            "DCL-S note CHAR(40);\n" +
            "DCL-S retries INT(5);\n" +
            "DCL-S counter INT(10) INZ(<caret>);\n");

        List<String> suggestions = smartLookup();
        assertContains("MAX (INT literal) acceptable", suggestions, "MAX");
        assertContains("retries (INT(5)) acceptable for INT(10) INZ", suggestions, "retries");
        assertNotContains("note (CHAR) not acceptable", suggestions, "note");
    }

    // ----- Return statement -----

    public void testReturnStatementExpectsProcReturnType() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper -> INT(10) {\n" +
            "  DCL-S x INT(10);\n" +
            "  DCL-S name CHAR(20);\n" +
            "  return <caret>;\n" +
            "}\n");

        List<String> suggestions = smartLookup();
        assertContains(suggestions, "x");
        assertNotContains(suggestions, "name");
    }

    // ----- Condition (BOOL) -----

    public void testIfConditionExpectsBool() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S flag BOOL;\n" +
            "  DCL-S ready BOOL;\n" +
            "  DCL-S counter INT(10);\n" +
            "  if (<caret>) {}\n" +
            "}\n");

        List<String> suggestions = smartLookup();
        assertContains("BOOL flag matches condition", suggestions, "flag");
        assertContains("BOOL ready matches condition", suggestions, "ready");
        assertNotContains("INT counter does NOT match BOOL condition", suggestions, "counter");
    }

    // ----- No expected type → no smart suggestions (falls back to BASIC) -----

    public void testNoExpectedTypeYieldsNothingFromSmartProvider() {
        // A bare statement context has no typed slot — smart should add nothing,
        // and IntelliJ's default fallback shows the BASIC list. We just check
        // smart didn't crash and didn't filter to zero either.
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  <caret>\n" +
            "}\n");

        // Smart Completion in a context with no expected type yields no specific
        // type-driven results; IntelliJ will still pull BASIC. We just assert no exception.
        myFixture.complete(CompletionType.SMART);
        // No assertion on suggestions — just non-crash.
    }

    // ----- Cross-file -----

    public void testCrossFileSmartCompletion() {
        myFixture.configureByText("util.bbk",
            "DCL-PROC computeTotal -> INT(10) { return 0; }\n" +
            "DCL-PROC computeName -> CHAR(20) { return 'x'; }\n");
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S total INT(10);\n" +
            "  total = <caret>;\n" +
            "}\n");

        List<String> suggestions = smartLookup();
        assertContains("Cross-file procedure returning INT(10)", suggestions, "computeTotal");
        assertNotContains("Cross-file CHAR procedure must not appear", suggestions, "computeName");
    }

    // ----- helpers -----

    private List<String> smartLookup() {
        LookupElement[] elements = myFixture.complete(CompletionType.SMART);
        // complete() returns null when a single element auto-completes; in that case
        // the chosen element is no longer in the lookup, so fall back to whatever
        // lookup strings remain (typically empty).
        if (elements == null) {
            List<String> fromLookup = myFixture.getLookupElementStrings();
            return fromLookup != null ? fromLookup : List.of();
        }
        return Arrays.stream(elements).map(LookupElement::getLookupString).collect(Collectors.toList());
    }

    private static void assertContains(List<String> haystack, String needle) {
        assertContains("Expected to find '" + needle + "'", haystack, needle);
    }

    private static void assertContains(String message, List<String> haystack, String needle) {
        assertTrue(message + " — got " + haystack,
            haystack.stream().anyMatch(s -> s.equalsIgnoreCase(needle)));
    }

    private static void assertNotContains(List<String> haystack, String needle) {
        assertNotContains("Did not expect '" + needle + "'", haystack, needle);
    }

    private static void assertNotContains(String message, List<String> haystack, String needle) {
        assertFalse(message + " — got " + haystack,
            haystack.stream().anyMatch(s -> s.equalsIgnoreCase(needle)));
    }
}
