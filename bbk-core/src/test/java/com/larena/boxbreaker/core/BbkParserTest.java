package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.ast.*;
import com.larena.boxbreaker.core.parser.BbkParser;
import org.junit.Test;

import static org.junit.Assert.*;

public class BbkParserTest {

    private BbkProgram parse(String src) { return BbkParser.parse(src); }

    // ----- declarations -----

    @Test
    public void variableWithModifier() {
        BbkDeclaration.Variable v = (BbkDeclaration.Variable)
            parse("DCL-S counter INT(10) INZ(0);").items().get(0);
        assertEquals("counter", v.name());
        assertEquals(Integer.valueOf(10), ((BbkType.Primitive) v.type()).length());
        assertEquals("INZ", v.modifiers().get(0).name());
    }

    @Test
    public void dataStructureWithSubfields() {
        BbkDeclaration.DataStructure ds = (BbkDeclaration.DataStructure)
            parse("DCL-DS customer QUALIFIED { id INT(10); active BOOL; }").items().get(0);
        assertEquals(2, ds.subfields().size());
        assertEquals("QUALIFIED", ds.modifiers().get(0).name());
        assertTrue(ds.subfields().get(1).type() instanceof BbkType.Primitive p && p.name().equals("BOOL"));
    }

    @Test
    public void procedureWithParamsAndReturn() {
        BbkDeclaration.Procedure proc = (BbkDeclaration.Procedure) parse(
            "DCL-PROC computeTax(amount PACKED(11:2) VALUE) -> PACKED(11:2) EXPORT { return amount; }")
            .items().get(0);
        assertEquals(1, proc.params().size());
        assertEquals("amount", proc.params().get(0).name());
        assertEquals(Integer.valueOf(2), ((BbkType.Primitive) proc.returnType()).decimals());
        assertEquals("EXPORT", proc.modifiers().get(0).name());
        assertTrue(proc.body().get(0) instanceof BbkStatement.Return);
    }

    @Test
    public void likedsType() {
        BbkDeclaration.Variable v = (BbkDeclaration.Variable)
            parse("DCL-S ord LIKEDS(order);").items().get(0);
        assertTrue(v.type() instanceof BbkType.Like l
            && l.kind() == BbkType.LikeKind.LIKEDS && l.name().equals("order"));
    }

    // ----- assignment (no '=' ambiguity in BBK) -----

    @Test
    public void assignmentVsEquality() {
        BbkStatement.Assignment a = (BbkStatement.Assignment) parse("counter = counter + 1;").items().get(0);
        assertEquals(BbkStatement.AssignOp.ASSIGN, a.op());
        assertTrue(a.value() instanceof BbkExpr.Binary b && b.op() == BbkExpr.BinOp.ADD);

        // '==' inside an if is equality, not assignment
        BbkStatement.If f = (BbkStatement.If) parse("if (a == 1) { x = 2; }").items().get(0);
        assertTrue(f.condition() instanceof BbkExpr.Binary b && b.op() == BbkExpr.BinOp.EQ);
    }

    @Test
    public void compoundAssignmentAndAttribute() {
        BbkStatement.Assignment a = (BbkStatement.Assignment) parse("x += 1;").items().get(0);
        assertEquals(BbkStatement.AssignOp.ADD, a.op());

        BbkStatement.Assignment h = (BbkStatement.Assignment) parse("x = a / b @halfup;").items().get(0);
        assertEquals(BbkStatement.AttrMod.HALFUP, h.attr());
    }

    // ----- expressions: precedence, ternary, bitwise, postfix -----

    @Test
    public void precedenceAndTernary() {
        BbkExpr.Binary add = (BbkExpr.Binary)
            ((BbkStatement.Assignment) parse("x = a + b * c;").items().get(0)).value();
        assertEquals(BbkExpr.BinOp.ADD, add.op());
        assertTrue(add.right() instanceof BbkExpr.Binary m && m.op() == BbkExpr.BinOp.MUL);

        BbkExpr e = ((BbkStatement.Assignment) parse("x = ok ? 1 : 2;").items().get(0)).value();
        assertTrue(e instanceof BbkExpr.Ternary);
    }

    @Test
    public void bitwiseAndShift() {
        BbkExpr e = ((BbkStatement.Assignment) parse("x = a & b | c ^ d << 2;").items().get(0)).value();
        // top operator is | (bit-or), lower than ^ and &
        assertTrue(e instanceof BbkExpr.Binary b && b.op() == BbkExpr.BinOp.BIT_OR);
    }

    @Test
    public void postfixCallIndexMember() {
        BbkExpr e = ((BbkStatement.ExpressionStatement) parse("f(a, b)[i].field;").items().get(0)).expr();
        assertTrue(e instanceof BbkExpr.Member m && m.field().equals("field")
            && m.target() instanceof BbkExpr.Index idx
            && idx.target() instanceof BbkExpr.Call c && c.args().size() == 2);
    }

    // ----- control flow -----

    @Test
    public void controlFlowForms() {
        assertTrue(parse("if (a) { } else if (b) { } else { }").items().get(0)
            instanceof BbkStatement.If f && f.elseBody().get(0) instanceof BbkStatement.If);
        assertTrue(parse("while (c) { x = 1; }").items().get(0) instanceof BbkStatement.While);
        assertTrue(parse("do { x = 1; } while (c);").items().get(0) instanceof BbkStatement.DoWhile);
        assertTrue(parse("for (i = 1; i <= 10; i += 1) { s = s + i; }").items().get(0)
            instanceof BbkStatement.For);
        assertTrue(parse("select { when (a) { x = 1; } other { x = 0; } }").items().get(0)
            instanceof BbkStatement.Select);
        assertTrue(parse("monitor { x = 1; } on-error { x = 0; }").items().get(0)
            instanceof BbkStatement.Monitor);
        assertTrue(parse("BEGSR init; x = 0; ENDSR;").items().get(0) instanceof BbkStatement.Subroutine);
    }

    @Test
    public void fileOpsAndCallpAndExsr() {
        assertTrue(parse("read custFile data;").items().get(0) instanceof BbkStatement.FileOp);
        assertTrue(parse("CALLP doThing(x);").items().get(0) instanceof BbkStatement.Callp);
        assertTrue(parse("EXSR init;").items().get(0) instanceof BbkStatement.Exsr);
    }

    // ----- the closing loop: parse the exact BBK the frontend emits -----

    @Test
    public void parsesFrontendOutput() {
        String bbk =
            "DCL-S counter INT(10) INZ(0);\n" +
            "DCL-DS customer QUALIFIED {\n" +
            "  id INT(10);\n" +
            "  active BOOL;\n" +
            "}\n" +
            "counter = counter + 1;\n" +
            "if (counter > 0 && customer.active == true) {\n" +
            "  name = \"active\";\n" +
            "  for (i = 1; i <= counter; i += 1) {\n" +
            "    counter = counter - 1;\n" +
            "  }\n" +
            "}\n";
        BbkProgram p = parse(bbk);
        assertEquals(4, p.items().size());   // var, DS, assignment, if
        assertTrue(p.items().get(1) instanceof BbkDeclaration.DataStructure);
        assertTrue(p.items().get(3) instanceof BbkStatement.If);
    }
}
