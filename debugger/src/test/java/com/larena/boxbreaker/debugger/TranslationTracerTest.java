package com.larena.boxbreaker.debugger;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TranslationTracerTest {

    @Test
    public void mapsEachTopLevelConstructToItsBbk() {
        String rpg =
            "**FREE\n" +
            "dcl-s counter int(10) inz(0);\n" +    // line 2
            "counter = counter + 1;\n" +            // line 3
            "if counter > 0;\n" +                   // line 4
            "  counter = counter - 1;\n" +          // line 5
            "endif;\n";                             // line 6

        List<TranslationStep> steps = TranslationTracer.trace(rpg);

        // **FREE produces no step; three constructs remain.
        assertEquals(3, steps.size());

        TranslationStep decl = steps.get(0);
        assertEquals(2, decl.rpgStartLine());
        assertEquals(2, decl.rpgEndLine());
        assertEquals(List.of("DCL-S counter INT(10) INZ(0);"), decl.bbkLines());

        TranslationStep asg = steps.get(1);
        assertEquals(3, asg.rpgStartLine());
        assertEquals(List.of("counter = counter + 1;"), asg.bbkLines());

        TranslationStep ifStep = steps.get(2);
        assertEquals(4, ifStep.rpgStartLine());
        assertEquals(6, ifStep.rpgEndLine());
        assertEquals(3, ifStep.rpgLines().size());   // if; body; endif
        assertEquals(List.of("if (counter > 0) {", "  counter = counter - 1;", "}"),
            ifStep.bbkLines());
    }

    @Test
    public void multiLineConstructKeepsSourceLines() {
        String rpg =
            "for i = 1 to 10;\n" +
            "  total = total + i;\n" +
            "endfor;\n";
        TranslationStep step = TranslationTracer.trace(rpg).get(0);
        assertEquals(1, step.rpgStartLine());
        assertEquals(3, step.rpgEndLine());
        assertEquals(3, step.rpgLines().size());
        assertEquals("for (i = 1; i <= 10; i += 1) {", step.bbkLines().get(0));
    }

    @Test
    public void renderProducesSideBySideWithLineNumbers() {
        String rpg =
            "dcl-s counter int(10);\n" +
            "if counter > 0;\n" +
            "  counter = counter - 1;\n" +
            "endif;\n";
        String view = TranslationView.render(rpg);

        // header + the RPG and BBK appearing on the same rows
        assertTrue(view.contains("RPG"));
        assertTrue(view.contains("BBK"));
        assertTrue(view.contains("| DCL-S counter INT(10);"));
        assertTrue(view.contains("| if (counter > 0) {"));
        assertTrue(view.contains("| }"));
        // line number of the first construct appears
        assertTrue(view.contains("1  dcl-s counter int(10);"));
    }

    @Test
    public void demoRenderForReview() {
        String rpg =
            "**FREE\n" +
            "dcl-s counter int(10) inz(0);\n" +
            "dcl-s name char(50);\n" +
            "counter = counter + 1;\n" +
            "if counter > 0;\n" +
            "  name = 'positive';\n" +
            "  for i = 1 to counter;\n" +
            "    counter = counter - 1;\n" +
            "  endfor;\n" +
            "endif;\n";
        String view = TranslationView.render(rpg);
        System.out.println("\n" + view);
        assertFalse(view.isBlank());
    }

    @Test
    public void directiveShowsAsStep() {
        List<TranslationStep> steps = TranslationTracer.trace("/COPY MYLIB/SRC\nreturn;\n");
        assertEquals(2, steps.size());
        assertEquals(List.of("// /COPY MYLIB/SRC"), steps.get(0).bbkLines());
        assertEquals(List.of("return;"), steps.get(1).bbkLines());
    }
}
