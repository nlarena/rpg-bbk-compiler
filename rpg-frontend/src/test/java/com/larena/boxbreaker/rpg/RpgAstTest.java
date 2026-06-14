package com.larena.boxbreaker.rpg;

import com.larena.boxbreaker.rpg.ast.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Builds representative AST trees by hand to confirm the node API is usable and
 * exhaustive. (The parser will produce these; here we just exercise the shapes.)
 */
public class RpgAstTest {

    @Test
    public void buildsADeclarationAndAssignment() {
        // dcl-s counter int(10) inz(0);
        RpgDeclaration.Variable decl = new RpgDeclaration.Variable(
            "counter",
            RpgType.scalar("int", 10),
            List.of(new RpgKeyword("inz", List.of(new RpgExpr.Literal(RpgExpr.LiteralKind.INT, "0")))));

        // counter = counter + 1;
        RpgStatement.Assignment asg = new RpgStatement.Assignment(
            new RpgExpr.Identifier("counter"),
            new RpgExpr.Binary(
                new RpgExpr.Identifier("counter"),
                RpgExpr.BinOp.ADD,
                new RpgExpr.Literal(RpgExpr.LiteralKind.INT, "1")),
            RpgStatement.EvalMode.PLAIN);

        RpgProgram program = new RpgProgram(true, List.of(decl, asg));

        assertEquals(2, program.items().size());
        assertTrue(program.free());
        assertTrue(program.items().get(0) instanceof RpgDeclaration.Variable);
        assertTrue(program.items().get(1) instanceof RpgStatement.Assignment);
        assertEquals("counter", decl.name());
        assertEquals(Integer.valueOf(10), ((RpgType.Scalar) decl.type()).length());
    }

    @Test
    public void buildsAQualifiedMemberAndBifCall() {
        // currentOrder.total = %dec(amount : 11 : 2);
        RpgExpr lhs = new RpgExpr.Member(new RpgExpr.Identifier("currentOrder"), "total");
        RpgExpr rhs = new RpgExpr.BifCall("%dec", List.of(
            new RpgExpr.Identifier("amount"),
            new RpgExpr.Literal(RpgExpr.LiteralKind.INT, "11"),
            new RpgExpr.Literal(RpgExpr.LiteralKind.INT, "2")));

        RpgStatement.Assignment asg =
            new RpgStatement.Assignment(lhs, rhs, RpgStatement.EvalMode.PLAIN);

        assertTrue(asg.target() instanceof RpgExpr.Member m && m.field().equals("total"));
        assertTrue(asg.value() instanceof RpgExpr.BifCall b && b.args().size() == 3);
    }

    @Test
    public void buildsAnIfWithElseifAndFigurative() {
        // if valid = *on; ... elseif valid = *off; ... else; ... endif;
        RpgExpr cond = new RpgExpr.Binary(
            new RpgExpr.Identifier("valid"), RpgExpr.BinOp.EQ, new RpgExpr.Figurative("*on"));
        RpgStatement.ElseIf elif = new RpgStatement.ElseIf(
            new RpgExpr.Binary(new RpgExpr.Identifier("valid"), RpgExpr.BinOp.EQ,
                new RpgExpr.Figurative("*off")),
            List.of(new RpgStatement.Leave()));

        RpgStatement.If ifs = new RpgStatement.If(
            cond,
            List.of(new RpgStatement.Return(null)),
            List.of(elif),
            List.of(new RpgStatement.Iter()));

        assertEquals(1, ifs.elseIfs().size());
        assertTrue(ifs.thenBody().get(0) instanceof RpgStatement.Return r && r.value() == null);
        assertTrue(ifs.elseBody().get(0) instanceof RpgStatement.Iter);
    }

    @Test
    public void buildsAProcedureWithInterfaceAndBody() {
        // dcl-proc computeTax; dcl-pi ... end-pi; <body> end-proc;
        RpgDeclaration.ProcInterface pi = new RpgDeclaration.ProcInterface(
            "computeTax",
            RpgType.scalar("packed", 11, 2),
            List.of(),
            List.of(new RpgDeclaration.Parameter("amount",
                RpgType.scalar("packed", 11, 2),
                List.of(RpgKeyword.bare("value")))));

        RpgDeclaration.Procedure proc = new RpgDeclaration.Procedure(
            "computeTax",
            List.of(RpgKeyword.bare("export")),
            pi,
            List.of(new RpgStatement.Return(
                new RpgExpr.Binary(new RpgExpr.Identifier("amount"), RpgExpr.BinOp.MUL,
                    new RpgExpr.Literal(RpgExpr.LiteralKind.DEC, "0.21")))));

        assertEquals("computeTax", proc.name());
        assertEquals(1, proc.pi().params().size());
        assertEquals(1, proc.body().size());
        assertTrue(proc.body().get(0) instanceof RpgStatement.Return);
    }

    @Test
    public void likeDsTypeIsRepresentable() {
        // dcl-s currentOrder likeds(order);
        RpgDeclaration.Variable v = new RpgDeclaration.Variable(
            "currentOrder", new RpgType.LikeDs("order"), List.of());
        assertTrue(v.type() instanceof RpgType.LikeDs ld && ld.name().equals("order"));
    }
}
