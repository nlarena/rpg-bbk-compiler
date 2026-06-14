package com.larena.boxbreaker.plugin.bbk;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.larena.boxbreaker.plugin.bbk.builtins.BbkBuiltinFunction;
import com.larena.boxbreaker.plugin.bbk.builtins.BbkBuiltinRegistry;
import com.larena.boxbreaker.plugin.bbk.documentation.BbkDocumentationProvider;
import com.larena.boxbreaker.plugin.bbk.psi.BbkConstantDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkProcedureDeclaration;
import com.larena.boxbreaker.plugin.bbk.psi.BbkVariableDeclaration;

/**
 * Tests Quick Documentation ({@code Ctrl+Q}): {@link BbkDocumentationProvider}
 * produces the right HTML for user declarations and for built-in functions.
 */
public class BbkDocumentationTest extends BasePlatformTestCase {

    private final BbkDocumentationProvider provider = new BbkDocumentationProvider();

    // ----- Registry sanity -----

    public void testRegistryHasCommonBifs() {
        assertTrue(BbkBuiltinRegistry.isBuiltin("trim"));
        assertTrue(BbkBuiltinRegistry.isBuiltin("TRIM"));   // case-insensitive
        assertTrue(BbkBuiltinRegistry.isBuiltin("substr"));
        assertFalse(BbkBuiltinRegistry.isBuiltin("notABif"));
        BbkBuiltinFunction substr = BbkBuiltinRegistry.find("substr");
        assertNotNull(substr);
        assertEquals("substr(s, start [, len])", substr.signature());
        assertEquals(3, substr.parameters().size());
    }

    // ----- Declarations -----

    public void testDocForVariable() {
        myFixture.configureByText("main.bbk", "DCL-S counter INT(10);\n");
        BbkVariableDeclaration decl = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkVariableDeclaration.class);
        assertNotNull(decl);
        String doc = provider.generateDoc(decl, null);
        assertNotNull(doc);
        assertTrue("Should label kind: " + doc, doc.contains("Variable"));
        assertTrue("Should mention name: " + doc, doc.contains("counter"));
        assertTrue("Should mention type: " + doc, doc.contains("INT(10)"));
    }

    public void testDocForConstant() {
        myFixture.configureByText("main.bbk", "DCL-C MAX 100;\n");
        BbkConstantDeclaration decl = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkConstantDeclaration.class);
        assertNotNull(decl);
        String doc = provider.generateDoc(decl, null);
        assertNotNull(doc);
        assertTrue(doc.contains("Constant"));
        assertTrue(doc.contains("MAX"));
    }

    public void testDocForProcedureShowsSignature() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC helper(n INT(10) VALUE) -> INT(20) {\n" +
            "  return n;\n" +
            "}\n");
        BbkProcedureDeclaration decl = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkProcedureDeclaration.class);
        assertNotNull(decl);
        String doc = provider.generateDoc(decl, null);
        assertNotNull(doc);
        assertTrue("Should label as Procedure: " + doc, doc.contains("Procedure"));
        assertTrue("Should show signature with params and return: " + doc,
            doc.contains("INT(10)") && doc.contains("INT(20)"));
    }

    public void testQuickNavigateForVariable() {
        myFixture.configureByText("main.bbk", "DCL-S amount PACKED(7:2);\n");
        BbkVariableDeclaration decl = PsiTreeUtil.findChildOfType(myFixture.getFile(), BbkVariableDeclaration.class);
        String info = provider.getQuickNavigateInfo(decl, null);
        assertNotNull(info);
        assertTrue(info.contains("Variable"));
        assertTrue(info.contains("amount"));
        assertTrue(info.contains("PACKED(7:2)"));
    }

    // ----- Builtins -----

    public void testDocForBuiltinViaCustomElement() {
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S s CHAR(20);\n" +
            "  s = tr<caret>im(s);\n" +
            "}\n");

        PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(context);
        assertEquals("trim", context.getText());

        PsiElement docElement = provider.getCustomDocumentationElement(
            myFixture.getEditor(), myFixture.getFile(), context, myFixture.getCaretOffset());
        assertNotNull("Builtin IDENT should be claimed for documentation", docElement);

        String doc = provider.generateDoc(docElement, context);
        assertNotNull(doc);
        assertTrue("Should show signature: " + doc, doc.contains("trim(s)"));
        assertTrue("Should describe trimming: " + doc.toLowerCase(),
            doc.toLowerCase().contains("blank"));
    }

    public void testBuiltinNotClaimedWhenNameResolvesToDeclaration() {
        // A user-declared variable named 'trim' should win over the BIF.
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S trim INT(10);\n" +
            "  trim = tr<caret>im + 1;\n" +
            "}\n");

        PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(context);
        PsiElement docElement = provider.getCustomDocumentationElement(
            myFixture.getEditor(), myFixture.getFile(), context, myFixture.getCaretOffset());
        assertNull("Should NOT claim 'trim' as a builtin when it resolves to a variable", docElement);
    }
}
