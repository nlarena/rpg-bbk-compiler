package com.larena.boxbreaker.core;

import com.larena.boxbreaker.core.ast.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Builds representative BBK AST trees by hand to confirm the node API is usable
 * and exhaustive. (The parser will produce these; here we exercise the shapes.)
 */
public class BbkAstTest {

    @Test
    public void variableWithInzAndAssignment() {
        // DCL-S counter INT(10) INZ(0);  counter = counter + 1;
        BbkDeclaration.Variable decl = new BbkDeclaration.Variable(
            "counter", BbkType.prim("INT", 10),
            List.of(new BbkModifier("INZ", List.of(new BbkExpr.Literal(BbkExpr.LitKind.INT, "0")))));

        BbkStatement.Assignment asg = new BbkStatement.Assignment(
            new BbkExpr.Identifier("counter"),
            BbkStatement.AssignOp.ASSIGN,
            new BbkExpr.Binary(new BbkExpr.Identifier("counter"), BbkExpr.BinOp.ADD,
                new BbkExpr.Literal(BbkExpr.LitKind.INT, "1")),
            BbkStatement.AttrMod.NONE);

        BbkProgram p = new BbkProgram(List.of(decl, asg));
        assertEquals(2, p.items().size());
        assertEquals(Integer.valueOf(10), ((BbkType.Primitive) decl.type()).length());
        assertEquals(BbkStatement.AssignOp.ASSIGN, asg.op());
    }

    @Test
    public void cStyleForWithCompoundUpdate() {
        // for (i = 1; i <= 10; i += 1) { total = total + i; }
        BbkStatement.Assignment init = new BbkStatement.Assignment(
            new BbkExpr.Identifier("i"), BbkStatement.AssignOp.ASSIGN,
            new BbkExpr.Literal(BbkExpr.LitKind.INT, "1"), BbkStatement.AttrMod.NONE);
        BbkExpr cond = new BbkExpr.Binary(new BbkExpr.Identifier("i"), BbkExpr.BinOp.LE,
            new BbkExpr.Literal(BbkExpr.LitKind.INT, "10"));
        BbkStatement.Assignment update = new BbkStatement.Assignment(
            new BbkExpr.Identifier("i"), BbkStatement.AssignOp.ADD,
            new BbkExpr.Literal(BbkExpr.LitKind.INT, "1"), BbkStatement.AttrMod.NONE);

        BbkStatement.For loop = new BbkStatement.For(init, cond, update, List.of());
        assertTrue(loop.init() instanceof BbkStatement.Assignment);
        assertTrue(loop.update() instanceof BbkStatement.Assignment u && u.op() == BbkStatement.AssignOp.ADD);
    }

    @Test
    public void dataStructureWithSubfields() {
        BbkDeclaration.DataStructure ds = new BbkDeclaration.DataStructure(
            "customer", List.of(BbkModifier.bare("QUALIFIED")),
            List.of(
                new BbkDeclaration.Subfield("id", BbkType.prim("INT", 10), List.of()),
                new BbkDeclaration.Subfield("active", BbkType.prim("BOOL"), List.of())));
        assertEquals(2, ds.subfields().size());
        assertEquals("QUALIFIED", ds.modifiers().get(0).name());
    }

    @Test
    public void procedureWithParamsAndReturn() {
        // DCL-PROC computeTax(amount PACKED(11:2) VALUE) -> PACKED(11:2) EXPORT { return amount; }
        BbkDeclaration.Procedure proc = new BbkDeclaration.Procedure(
            "computeTax",
            List.of(new BbkDeclaration.Parameter("amount", BbkType.prim("PACKED", 11, 2),
                List.of(BbkModifier.bare("VALUE")))),
            BbkType.prim("PACKED", 11, 2),
            List.of(BbkModifier.bare("EXPORT")),
            List.of(new BbkStatement.Return(new BbkExpr.Identifier("amount"))));
        assertEquals(1, proc.params().size());
        assertEquals(Integer.valueOf(2), ((BbkType.Primitive) proc.returnType()).decimals());
        assertTrue(proc.body().get(0) instanceof BbkStatement.Return);
    }

    @Test
    public void ternaryAndMemberAndCall() {
        // ok ? customer.id : compute(x)
        BbkExpr e = new BbkExpr.Ternary(
            new BbkExpr.Identifier("ok"),
            new BbkExpr.Member(new BbkExpr.Identifier("customer"), "id", false),
            new BbkExpr.Call(new BbkExpr.Identifier("compute"),
                List.of(new BbkExpr.Identifier("x"))));
        assertTrue(e instanceof BbkExpr.Ternary t
            && t.then() instanceof BbkExpr.Member m && m.field().equals("id")
            && t.otherwise() instanceof BbkExpr.Call);
    }

    @Test
    public void ifWithElseIfChain() {
        // if (a) {} else if (b) {} — elseBody holds a nested If
        BbkStatement.If inner = new BbkStatement.If(
            new BbkExpr.Identifier("b"), List.of(), List.of());
        BbkStatement.If outer = new BbkStatement.If(
            new BbkExpr.Identifier("a"), List.of(), List.of(inner));
        assertTrue(outer.elseBody().get(0) instanceof BbkStatement.If);
    }
}
