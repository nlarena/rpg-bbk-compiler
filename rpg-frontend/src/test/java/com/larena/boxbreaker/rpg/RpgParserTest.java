package com.larena.boxbreaker.rpg;

import com.larena.boxbreaker.rpg.ast.*;
import com.larena.boxbreaker.rpg.parser.RpgParseException;
import com.larena.boxbreaker.rpg.parser.RpgParser;
import org.junit.Test;

import static org.junit.Assert.*;

public class RpgParserTest {

    private RpgProgram parse(String src) {
        return RpgParser.parse(src);
    }

    // ----- declarations -----

    @Test
    public void parsesDclSWithSizeAndInz() {
        RpgProgram p = parse("dcl-s counter int(10) inz(0);");
        assertEquals(1, p.items().size());
        RpgDeclaration.Variable v = (RpgDeclaration.Variable) p.items().get(0);
        assertEquals("counter", v.name());
        RpgType.Scalar t = (RpgType.Scalar) v.type();
        assertEquals("int", t.name());
        assertEquals(Integer.valueOf(10), t.length());
        assertEquals(1, v.keywords().size());
        assertEquals("inz", v.keywords().get(0).name());
    }

    @Test
    public void parsesPackedAndLikeds() {
        RpgType.Scalar t = (RpgType.Scalar)
            ((RpgDeclaration.Variable) parse("dcl-s amount packed(11:2);").items().get(0)).type();
        assertEquals(Integer.valueOf(11), t.length());
        assertEquals(Integer.valueOf(2), t.decimals());

        RpgType ld = ((RpgDeclaration.Variable) parse("dcl-s ord likeds(order);").items().get(0)).type();
        assertTrue(ld instanceof RpgType.LikeDs l && l.name().equals("order"));
    }

    // ----- assignment + the '=' ambiguity -----

    @Test
    public void parsesAssignment() {
        RpgStatement.Assignment a = (RpgStatement.Assignment) parse("counter = 0;").items().get(0);
        assertTrue(a.target() instanceof RpgExpr.Identifier id && id.name().equals("counter"));
        assertTrue(a.value() instanceof RpgExpr.Literal lit && lit.text().equals("0"));
    }

    @Test
    public void assignmentEqualsVsEqualityInCondition() {
        // statement-level '=' is assignment; '=' inside if(...) is equality
        RpgStatement.Assignment a = (RpgStatement.Assignment)
            parse("counter = counter + 1;").items().get(0);
        assertTrue(a.value() instanceof RpgExpr.Binary b && b.op() == RpgExpr.BinOp.ADD);

        RpgStatement.If f = (RpgStatement.If) parse("if counter = 0;\nreturn;\nendif;").items().get(0);
        assertTrue(f.condition() instanceof RpgExpr.Binary b && b.op() == RpgExpr.BinOp.EQ);
    }

    @Test
    public void parsesMemberAssignment() {
        RpgStatement.Assignment a = (RpgStatement.Assignment)
            parse("currentOrder.total = 100;").items().get(0);
        assertTrue(a.target() instanceof RpgExpr.Member m
            && m.field().equals("total")
            && m.target() instanceof RpgExpr.Identifier id && id.name().equals("currentOrder"));
    }

    // ----- expressions: precedence -----

    @Test
    public void precedenceMultiplyBeforeAdd() {
        // a + b * c  ->  a + (b * c)
        RpgExpr.Binary add = (RpgExpr.Binary)
            ((RpgStatement.Assignment) parse("x = a + b * c;").items().get(0)).value();
        assertEquals(RpgExpr.BinOp.ADD, add.op());
        assertTrue(add.right() instanceof RpgExpr.Binary mul && mul.op() == RpgExpr.BinOp.MUL);
    }

    @Test
    public void powerIsRightAssociative() {
        // a ** b ** c  ->  a ** (b ** c)
        RpgExpr.Binary pow = (RpgExpr.Binary)
            ((RpgStatement.Assignment) parse("x = a ** b ** c;").items().get(0)).value();
        assertEquals(RpgExpr.BinOp.POW, pow.op());
        assertTrue(pow.right() instanceof RpgExpr.Binary inner && inner.op() == RpgExpr.BinOp.POW);
    }

    @Test
    public void parensOverridePrecedence() {
        // (a + b) * c  ->  ( ... ) * c
        RpgExpr.Binary mul = (RpgExpr.Binary)
            ((RpgStatement.Assignment) parse("x = (a + b) * c;").items().get(0)).value();
        assertEquals(RpgExpr.BinOp.MUL, mul.op());
        assertTrue(mul.left() instanceof RpgExpr.Binary add && add.op() == RpgExpr.BinOp.ADD);
    }

    @Test
    public void booleanOperators() {
        // a = 1 and b = 2 or c = 3  ->  (a=1 and b=2) or (c=3)
        RpgExpr.Binary or = (RpgExpr.Binary)
            ((RpgStatement.Assignment) parse("x = a = 1 and b = 2 or c = 3;").items().get(0)).value();
        assertEquals(RpgExpr.BinOp.OR, or.op());
        assertTrue(or.left() instanceof RpgExpr.Binary and && and.op() == RpgExpr.BinOp.AND);
    }

    // ----- expressions: leaves -----

    @Test
    public void bifCallAndArgs() {
        RpgExpr.BifCall bif = (RpgExpr.BifCall)
            ((RpgStatement.Assignment) parse("x = %subst(s : 1 : 3);").items().get(0)).value();
        assertEquals("%subst", bif.name());
        assertEquals(3, bif.args().size());
    }

    @Test
    public void procCallAndFigurativeAndIndicator() {
        RpgExpr.Call call = (RpgExpr.Call)
            ((RpgStatement.Assignment) parse("x = computeTax(amount);").items().get(0)).value();
        assertEquals(1, call.args().size());

        RpgExpr fig = ((RpgStatement.Assignment) parse("flag = *on;").items().get(0)).value();
        assertTrue(fig instanceof RpgExpr.Figurative f && f.name().equalsIgnoreCase("*on"));

        RpgExpr ind = ((RpgStatement.Assignment) parse("x = *inlr;").items().get(0)).value();
        assertTrue(ind instanceof RpgExpr.IndicatorRef r && r.name().equalsIgnoreCase("*inlr"));
    }

    // ----- if / elseif / else -----

    @Test
    public void ifElseifElse() {
        RpgStatement.If f = (RpgStatement.If) parse(
            "if a = 1;\n  return;\nelseif a = 2;\n  x = 1;\nelse;\n  x = 2;\nendif;").items().get(0);
        assertEquals(1, f.thenBody().size());
        assertEquals(1, f.elseIfs().size());
        assertEquals(1, f.elseBody().size());
        assertTrue(f.elseIfs().get(0).condition() instanceof RpgExpr.Binary b && b.op() == RpgExpr.BinOp.EQ);
    }

    // ----- return + directives -----

    @Test
    public void returnWithAndWithoutValue() {
        assertNull(((RpgStatement.Return) parse("return;").items().get(0)).value());
        assertNotNull(((RpgStatement.Return) parse("return x + 1;").items().get(0)).value());
    }

    @Test
    public void directiveSurvivesInTree() {
        RpgProgram p = parse("/COPY MYLIB/MYSRC\nreturn;");
        assertTrue(p.items().get(0) instanceof RpgStatement.Directive d
            && d.text().startsWith("/COPY"));
        assertTrue(p.items().get(1) instanceof RpgStatement.Return);
    }

    @Test
    public void freeDirectiveSetsFlag() {
        assertTrue(parse("**FREE\ndcl-s x int(10);").free());
        assertFalse(parse("dcl-s x int(10);").free());
    }

    // ----- full grammar: declarations and structured statements -----

    @Test
    public void parsesProcedureWithInterfaceAndBody() {
        RpgProgram p = parse(
            "dcl-proc computeTax export;\n" +
            "  dcl-pi computeTax packed(11:2);\n" +
            "    amount packed(11:2) value;\n" +
            "  end-pi;\n" +
            "  return amount * 0.21;\n" +
            "end-proc;");
        RpgDeclaration.Procedure proc = (RpgDeclaration.Procedure) p.items().get(0);
        assertEquals("computeTax", proc.name());
        assertNotNull(proc.pi());
        assertEquals(1, proc.pi().params().size());
        assertEquals("amount", proc.pi().params().get(0).name());
        assertEquals(1, proc.body().size());
    }

    @Test
    public void parsesDataStructureWithSubfields() {
        RpgProgram p = parse(
            "dcl-ds customer qualified;\n" +
            "  id int(10);\n" +
            "  name char(50);\n" +
            "end-ds;");
        RpgDeclaration.DataStructure ds = (RpgDeclaration.DataStructure) p.items().get(0);
        assertEquals("customer", ds.name());
        assertEquals(2, ds.subfields().size());
        assertEquals("id", ds.subfields().get(0).name());
    }

    @Test
    public void parsesLoopsAndSelect() {
        assertTrue(parse("for i = 1 to 10;\n  x = i;\nendfor;").items().get(0) instanceof RpgStatement.For);
        assertTrue(parse("dow x < 10;\n  x = x + 1;\nenddo;").items().get(0) instanceof RpgStatement.Dow);
        assertTrue(parse("dou x >= 10;\n  x = x + 1;\nenddo;").items().get(0) instanceof RpgStatement.Dou);
        assertTrue(parse("select;\nwhen x = 1;\n  y = 1;\nother;\n  y = 0;\nendsl;").items().get(0)
            instanceof RpgStatement.Select);
        assertTrue(parse("monitor;\n  x = 1;\non-error;\n  x = 0;\nendmon;").items().get(0)
            instanceof RpgStatement.Monitor);
        assertTrue(parse("begsr init;\n  x = 0;\nendsr;").items().get(0) instanceof RpgStatement.Subroutine);
    }

    @Test
    public void parsesOpcodeStatements() {
        RpgStatement.Op read = (RpgStatement.Op) parse("read custFile data;").items().get(0);
        assertEquals("read", read.opcode().toLowerCase());
        assertEquals(2, read.operands().size());
        assertTrue(parse("exsr init;").items().get(0) instanceof RpgStatement.Op);
    }

    @Test
    public void ctlOptAndDclF() {
        assertTrue(parse("ctl-opt main(run) dftactgrp(*no);").items().get(0) instanceof RpgDeclaration.CtlOpt);
        assertTrue(parse("dcl-f orders usage(*input) keyed;").items().get(0) instanceof RpgDeclaration.File);
    }
}
