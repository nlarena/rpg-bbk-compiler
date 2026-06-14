package com.larena.boxbreaker.rpg;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * End-to-end tests of the Slice 1 pipeline: RPG source in, BBK source text out.
 */
public class RpgToBbkTest {

    private String bbk(String rpg) {
        return RpgToBbk.translate(rpg).strip();
    }

    @Test
    public void declarationUppercasesType() {
        assertEquals("DCL-S counter INT(10);", bbk("dcl-s counter int(10);"));
        assertEquals("DCL-S amount PACKED(11:2);", bbk("dcl-s amount packed(11:2);"));
        assertEquals("DCL-S name CHAR(50);", bbk("dcl-s name char(50);"));
    }

    @Test
    public void declarationWithInzAndLikeds() {
        assertEquals("DCL-S counter INT(10) INZ(0);", bbk("dcl-s counter int(10) inz(0);"));
        assertEquals("DCL-S ord LIKEDS(order);", bbk("dcl-s ord likeds(order);"));
    }

    @Test
    public void indicatorTypeMapsToBool() {
        assertEquals("DCL-S valid BOOL;", bbk("dcl-s valid ind;"));
    }

    @Test
    public void assignmentAndDecimalLiteral() {
        assertEquals("counter = 0;", bbk("counter = 0;"));
        assertEquals("total = 199.95d;", bbk("total = 199.95;"));
    }

    @Test
    public void characterLiteralBecomesDoubleQuoted() {
        assertEquals("name = \"Alice\";", bbk("name = 'Alice';"));
        assertEquals("msg = \"it's ok\";", bbk("msg = 'it''s ok';"));
    }

    @Test
    public void comparisonAndLogicalOperatorsTranslate() {
        // '=' (equality) -> '==', '<>' -> '!=', and/or -> && ||
        assertEquals("flag = a == b;", bbk("flag = a = b;"));
        assertEquals("flag = a != b;", bbk("flag = a <> b;"));
        assertEquals("flag = a == 1 && b == 2;", bbk("flag = a = 1 and b = 2;"));
        assertEquals("flag = !ready;", bbk("flag = not ready;"));
    }

    @Test
    public void figurativesTranslate() {
        assertEquals("flag = true;", bbk("flag = *on;"));
        assertEquals("flag = false;", bbk("flag = *off;"));
        assertEquals("p = null;", bbk("p = *null;"));
    }

    @Test
    public void precedenceProducesMinimalParens() {
        assertEquals("x = a + b * c;", bbk("x = a + b * c;"));
        assertEquals("x = (a + b) * c;", bbk("x = (a + b) * c;"));
        assertEquals("x = a - (b + c);", bbk("x = a - (b + c);"));
        assertEquals("x = a ** b ** c;", bbk("x = a ** b ** c;"));
        assertEquals("x = (a ** b) ** c;", bbk("x = (a ** b) ** c;"));
    }

    @Test
    public void memberAccessAndCallsWithCommaArgs() {
        assertEquals("currentOrder.total = computeTax(amount);",
            bbk("currentOrder.total = computeTax(amount);"));
        // RPG ':' arg separator -> BBK ','; RPG %SUBST -> BBK substr
        assertEquals("x = substr(s, 1, 3);", bbk("x = %subst(s : 1 : 3);"));
    }

    @Test
    public void ifElseifElseBecomesBraces() {
        String rpg =
            "if a = 1;\n" +
            "  return;\n" +
            "elseif a = 2;\n" +
            "  x = 1;\n" +
            "else;\n" +
            "  x = 2;\n" +
            "endif;\n";
        String expected =
            "if (a == 1) {\n" +
            "  return;\n" +
            "} else if (a == 2) {\n" +
            "  x = 1;\n" +
            "} else {\n" +
            "  x = 2;\n" +
            "}";
        assertEquals(expected, bbk(rpg));
    }

    @Test
    public void returnAndDirective() {
        assertEquals("return;", bbk("return;"));
        assertEquals("return n - 1;", bbk("return n - 1;"));
        assertEquals("// /COPY MYLIB/MYSRC", bbk("/COPY MYLIB/MYSRC"));
    }

    // ----- full grammar end-to-end -----

    @Test
    public void loopsTranslateToBraces() {
        assertEquals("while (x < 10) {\n  x = x + 1;\n}",
            bbk("dow x < 10;\n  x = x + 1;\nenddo;"));
        assertEquals("do {\n  x = x + 1;\n} while (!(x >= 10));",
            bbk("dou x >= 10;\n  x = x + 1;\nenddo;"));
        assertEquals("for (i = 1; i <= 10; i += 1) {\n  total = total + i;\n}",
            bbk("for i = 1 to 10;\n  total = total + i;\nendfor;"));
        assertEquals("for (i = 10; i >= 1; i -= 2) {\n  x = i;\n}",
            bbk("for i = 10 downto 1 by 2;\n  x = i;\nendfor;"));
    }

    @Test
    public void selectTranslates() {
        String rpg = "select;\nwhen x = 1;\n  y = 1;\nother;\n  y = 0;\nendsl;";
        String expected =
            "select {\n" +
            "  when (x == 1) {\n" +
            "    y = 1;\n" +
            "  }\n" +
            "  other {\n" +
            "    y = 0;\n" +
            "  }\n" +
            "}";
        assertEquals(expected, bbk(rpg));
    }

    @Test
    public void leaveIterBecomeBreakContinue() {
        assertEquals("while (a) {\n  break;\n}", bbk("dow a;\n  leave;\nenddo;"));
        assertEquals("while (a) {\n  continue;\n}", bbk("dow a;\n  iter;\nenddo;"));
    }

    @Test
    public void dataStructureTranslates() {
        String rpg = "dcl-ds customer qualified;\n  id int(10);\n  name char(50);\nend-ds;";
        String expected =
            "DCL-DS customer QUALIFIED {\n" +
            "  id INT(10);\n" +
            "  name CHAR(50);\n" +
            "}";
        assertEquals(expected, bbk(rpg));
    }

    @Test
    public void procedureFoldsInterfaceIntoInlineParams() {
        String rpg =
            "dcl-proc computeTax export;\n" +
            "  dcl-pi computeTax packed(11:2);\n" +
            "    amount packed(11:2) value;\n" +
            "  end-pi;\n" +
            "  return amount;\n" +
            "end-proc;";
        String expected =
            "DCL-PROC computeTax(amount PACKED(11:2) VALUE) -> PACKED(11:2) EXPORT {\n" +
            "  return amount;\n" +
            "}";
        assertEquals(expected, bbk(rpg));
    }

    @Test
    public void prototypeAndCtlOptAndFile() {
        assertEquals("DCL-PR computeTax(amount PACKED(11:2) VALUE) -> PACKED(11:2);",
            bbk("dcl-pr computeTax packed(11:2);\n  amount packed(11:2) value;\nend-pr;"));
        assertEquals("CTL-OPT MAIN(run) DFTACTGRP(*no);",
            bbk("ctl-opt main(run) dftactgrp(*no);"));
        assertEquals("DCL-F orders USAGE(*input) KEYED;",
            bbk("dcl-f orders usage(*input) keyed;"));
    }

    @Test
    public void fileOpAndExsr() {
        assertEquals("read custFile data;", bbk("read custFile data;"));
        assertEquals("exsr init;", bbk("exsr init;"));
    }

    @Test
    public void smallProgramRoundTrip() {
        String rpg =
            "**FREE\n" +
            "dcl-s counter int(10) inz(0);\n" +
            "dcl-s name char(50);\n" +
            "counter = counter + 1;\n" +
            "if counter > 0;\n" +
            "  name = 'positive';\n" +
            "endif;\n";
        String expected =
            "DCL-S counter INT(10) INZ(0);\n" +
            "DCL-S name CHAR(50);\n" +
            "counter = counter + 1;\n" +
            "if (counter > 0) {\n" +
            "  name = \"positive\";\n" +
            "}";
        assertEquals(expected, bbk(rpg));
    }
}
