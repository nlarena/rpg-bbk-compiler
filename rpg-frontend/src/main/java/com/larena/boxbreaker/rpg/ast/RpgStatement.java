package com.larena.boxbreaker.rpg.ast;

import java.util.List;

/**
 * An RPG statement (grammar §4.3). Sealed; mirrors {@link RpgExpr} and
 * {@link RpgDeclaration} so the emitter handles every kind exhaustively.
 *
 * <p>Bodies of structured statements hold {@code List<RpgStatement>}; module
 * and procedure bodies (which may also contain declarations) hold
 * {@code List<RpgItem>}.
 */
public sealed interface RpgStatement extends RpgItem
        permits RpgStatement.Assignment, RpgStatement.If, RpgStatement.Select,
                RpgStatement.Dow, RpgStatement.Dou, RpgStatement.For,
                RpgStatement.Monitor, RpgStatement.Subroutine, RpgStatement.Return,
                RpgStatement.Leave, RpgStatement.Iter, RpgStatement.Leavesr,
                RpgStatement.Op, RpgStatement.ExprStatement, RpgStatement.Directive {

    /** {@code target = value;} with an optional EVAL mode (half-adjust / truncate / corr). */
    record Assignment(RpgExpr target, RpgExpr value, EvalMode mode) implements RpgStatement {}

    /** {@code if cond; ... [elseif cond; ...]* [else; ...] endif;} */
    record If(RpgExpr condition, List<RpgStatement> thenBody,
              List<ElseIf> elseIfs, List<RpgStatement> elseBody) implements RpgStatement {
        public If {
            thenBody = List.copyOf(thenBody);
            elseIfs = List.copyOf(elseIfs);
            elseBody = List.copyOf(elseBody);
        }
    }

    /** {@code select; [when cond; ...]* [other; ...] endsl;} */
    record Select(List<WhenClause> whens, List<RpgStatement> otherBody) implements RpgStatement {
        public Select {
            whens = List.copyOf(whens);
            otherBody = List.copyOf(otherBody);
        }
    }

    /** {@code dow cond; ... enddo;} */
    record Dow(RpgExpr condition, List<RpgStatement> body) implements RpgStatement {
        public Dow { body = List.copyOf(body); }
    }

    /** {@code dou cond; ... enddo;} */
    record Dou(RpgExpr condition, List<RpgStatement> body) implements RpgStatement {
        public Dou { body = List.copyOf(body); }
    }

    /** {@code for v = from (to|downto) to [by step]; ... endfor;} */
    record For(String var, RpgExpr from, ForDir dir, RpgExpr to, RpgExpr by,
               List<RpgStatement> body) implements RpgStatement {
        public For { body = List.copyOf(body); }
    }

    /** {@code monitor; ... [on-error status; ...]* [on-exit; ...] endmon;} */
    record Monitor(List<RpgStatement> body, List<OnError> onErrors,
                   List<RpgStatement> onExit) implements RpgStatement {
        public Monitor {
            body = List.copyOf(body);
            onErrors = List.copyOf(onErrors);
            onExit = List.copyOf(onExit);
        }
    }

    /** {@code begsr name; ... endsr;} */
    record Subroutine(String name, List<RpgStatement> body) implements RpgStatement {
        public Subroutine { body = List.copyOf(body); }
    }

    /** {@code return [value];} — {@code value} null for a bare return. */
    record Return(RpgExpr value) implements RpgStatement {}

    /** {@code leave;} (break out of a loop). */
    record Leave() implements RpgStatement {}

    /** {@code iter;} (continue a loop). */
    record Iter() implements RpgStatement {}

    /** {@code leavesr;} (return from a subroutine). */
    record Leavesr() implements RpgStatement {}

    /**
     * A generic opcode / file-operation statement: {@code read file ds;},
     * {@code chain key file;}, {@code callp proc(args);}, {@code dsply msg;}.
     * The opcode is the leading keyword; operands are the following expressions.
     */
    record Op(String opcode, List<RpgExpr> operands) implements RpgStatement {
        public Op { operands = List.copyOf(operands); }
    }

    /** A bare expression used as a statement (e.g. a procedure call). */
    record ExprStatement(RpgExpr expr) implements RpgStatement {}

    /** A compiler directive line surfaced into the tree: {@code /COPY ...}, {@code /IF ...}. */
    record Directive(String text) implements RpgStatement {}

    // ----- helper records -----

    /** One {@code elseif cond; body} arm of an {@link If}. */
    record ElseIf(RpgExpr condition, List<RpgStatement> body) {
        public ElseIf { body = List.copyOf(body); }
    }

    /** One {@code when cond; body} arm of a {@link Select}. */
    record WhenClause(RpgExpr condition, List<RpgStatement> body) {
        public WhenClause { body = List.copyOf(body); }
    }

    /** One {@code on-error status...; body} arm of a {@link Monitor}. */
    record OnError(List<RpgExpr> statusList, List<RpgStatement> body) {
        public OnError {
            statusList = List.copyOf(statusList);
            body = List.copyOf(body);
        }
    }

    // ----- enums -----

    /** Direction of a {@code for} loop. */
    enum ForDir { TO, DOWNTO }

    /** EVAL rounding mode on an assignment. */
    enum EvalMode { PLAIN, HALF_ADJUST, TRUNCATE, CORR }
}
