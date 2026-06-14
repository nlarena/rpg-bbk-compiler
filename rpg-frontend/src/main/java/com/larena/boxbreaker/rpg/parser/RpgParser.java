package com.larena.boxbreaker.rpg.parser;

import com.larena.boxbreaker.rpg.ast.*;
import com.larena.boxbreaker.rpg.lexer.RpgLexer;
import com.larena.boxbreaker.rpg.lexer.RpgToken;
import com.larena.boxbreaker.rpg.lexer.RpgTokenType;

import java.util.ArrayList;
import java.util.List;

import static com.larena.boxbreaker.rpg.lexer.RpgTokenType.*;

/**
 * Recursive-descent parser for free-format RPGLE. Covers the full free-form
 * declaration (§4.2), statement (§4.3) and expression (§4.4) grammar.
 *
 * <p><b>The {@code =} ambiguity:</b> at statement level {@code lvalue = expr}
 * is an assignment; inside an expression (an {@code if} condition, an RHS) the
 * {@code =} is equality. Resolved by parsing the statement head at the
 * <em>postfix</em> level (which stops before {@code =}); a following {@code =}
 * then means assignment.
 */
public final class RpgParser {

    private final List<RpgToken> tokens;
    private int pos = 0;

    public RpgParser(List<RpgToken> tokens) {
        this.tokens = tokens;
    }

    /** Convenience: lex + parse a source string into a program. */
    public static RpgProgram parse(String source) {
        return new RpgParser(RpgLexer.tokenize(source)).parseProgram();
    }

    // =======================================================================
    // Program
    // =======================================================================

    public RpgProgram parseProgram() {
        boolean free = match(FREE_DIRECTIVE);
        List<RpgItem> items = new ArrayList<>();
        while (!at(EOF)) {
            items.add(parseItem());
        }
        return new RpgProgram(free, items);
    }

    private RpgItem parseItem() {
        if (at(DIRECTIVE)) return new RpgStatement.Directive(advance().text());
        if (at(KEYWORD)) {
            String kw = peek().text().toLowerCase();
            return switch (kw) {
                // declarations
                case "ctl-opt"  -> parseCtlOpt();
                case "dcl-s"    -> parseDclS();
                case "dcl-c"    -> parseDclC();
                case "dcl-ds"   -> parseDclDs();
                case "dcl-pr"   -> parseDclPr();
                case "dcl-pi"   -> parseDclPi();
                case "dcl-f"    -> parseDclF();
                case "dcl-proc" -> parseDclProc();
                // structured statements
                case "if"       -> parseIf();
                case "select"   -> parseSelect();
                case "dow"      -> parseDow();
                case "dou"      -> parseDou();
                case "for"      -> parseFor();
                case "monitor"  -> parseMonitor();
                case "begsr"    -> parseSubroutine();
                case "return"   -> parseReturn();
                case "leave"    -> bare(new RpgStatement.Leave());
                case "iter"     -> bare(new RpgStatement.Iter());
                case "leavesr"  -> bare(new RpgStatement.Leavesr());
                case "eval", "evalr" -> parseEval();
                // any other opcode (exsr, callp, read, chain, write, dsply, ...)
                default         -> parseOp();
            };
        }
        return parseAssignmentOrExpr();
    }

    // =======================================================================
    // Declarations (§4.2)
    // =======================================================================

    private RpgDeclaration parseCtlOpt() {
        expectKeyword("ctl-opt");
        List<RpgKeyword> kws = parseModifiers();
        expect(SEMI, "';' after ctl-opt");
        return new RpgDeclaration.CtlOpt(kws);
    }

    private RpgDeclaration parseDclS() {
        expectKeyword("dcl-s");
        String name = expect(IDENT, "variable name").text();
        RpgType type = parseType();
        List<RpgKeyword> kws = parseModifiers();
        expect(SEMI, "';' after dcl-s");
        return new RpgDeclaration.Variable(name, type, kws);
    }

    private RpgDeclaration parseDclC() {
        expectKeyword("dcl-c");
        String name = expect(IDENT, "constant name").text();
        RpgExpr value;
        if (atKeyword("const")) {            // dcl-c name const(value);
            advance();
            expect(LPAREN, "'(' after CONST");
            value = parseExpression();
            expect(RPAREN, "')' after constant value");
        } else {                              // dcl-c name value;
            value = parseExpression();
        }
        expect(SEMI, "';' after dcl-c");
        return new RpgDeclaration.Constant(name, value);
    }

    private RpgDeclaration parseDclDs() {
        expectKeyword("dcl-ds");
        String name = expect(IDENT, "data structure name").text();
        List<RpgKeyword> kws = parseModifiers();
        List<RpgDeclaration.Subfield> subfields = new ArrayList<>();
        if (match(SEMI)) {                    // subfields follow until END-DS
            while (!atKeyword("end-ds") && !at(EOF)) {
                subfields.add(parseSubfield());
            }
        }
        expectKeyword("end-ds");
        match(IDENT);                         // optional repeated name
        expect(SEMI, "';' after END-DS");
        return new RpgDeclaration.DataStructure(name, kws, subfields);
    }

    private RpgDeclaration.Subfield parseSubfield() {
        String name = expect(IDENT, "subfield name").text();
        RpgType type = parseType();
        List<RpgKeyword> kws = parseModifiers();
        expect(SEMI, "';' after subfield");
        return new RpgDeclaration.Subfield(name, type, kws);
    }

    private RpgDeclaration parseDclPr() {
        expectKeyword("dcl-pr");
        String name = expect(IDENT, "prototype name").text();
        RpgType ret = parseOptionalReturnType();
        List<RpgKeyword> kws = parseModifiers();
        List<RpgDeclaration.Parameter> params = new ArrayList<>();
        if (match(SEMI)) {
            while (!atKeyword("end-pr") && !at(EOF)) params.add(parseDclParm());
        }
        expectKeyword("end-pr");
        match(IDENT);
        expect(SEMI, "';' after END-PR");
        return new RpgDeclaration.Prototype(name, ret, kws, params);
    }

    private RpgDeclaration.ProcInterface parseDclPi() {
        expectKeyword("dcl-pi");
        String name = expect(IDENT, "interface name").text();
        RpgType ret = parseOptionalReturnType();
        List<RpgKeyword> kws = parseModifiers();
        List<RpgDeclaration.Parameter> params = new ArrayList<>();
        if (match(SEMI)) {
            while (!atKeyword("end-pi") && !at(EOF)) params.add(parseDclParm());
        }
        expectKeyword("end-pi");
        match(IDENT);
        expect(SEMI, "';' after END-PI");
        return new RpgDeclaration.ProcInterface(name, ret, kws, params);
    }

    private RpgDeclaration.Parameter parseDclParm() {
        // free-form allows the DCL-PARM keyword to be omitted; accept both.
        if (atKeyword("dcl-parm")) advance();
        String name = expect(IDENT, "parameter name").text();
        RpgType type = parseType();
        List<RpgKeyword> kws = parseModifiers();
        expect(SEMI, "';' after parameter");
        return new RpgDeclaration.Parameter(name, type, kws);
    }

    private RpgDeclaration parseDclF() {
        expectKeyword("dcl-f");
        String name = expect(IDENT, "file name").text();
        List<RpgKeyword> kws = parseModifiers();
        expect(SEMI, "';' after dcl-f");
        return new RpgDeclaration.File(name, kws);
    }

    private RpgDeclaration parseDclProc() {
        expectKeyword("dcl-proc");
        String name = expect(IDENT, "procedure name").text();
        List<RpgKeyword> kws = parseModifiers();
        expect(SEMI, "';' after dcl-proc header");
        RpgDeclaration.ProcInterface pi = atKeyword("dcl-pi") ? parseDclPi() : null;
        List<RpgItem> body = new ArrayList<>();
        while (!atKeyword("end-proc") && !at(EOF)) {
            body.add(parseItem());
        }
        expectKeyword("end-proc");
        match(IDENT);
        expect(SEMI, "';' after END-PROC");
        return new RpgDeclaration.Procedure(name, kws, pi, body);
    }

    /** A return type follows the name when it is a type name or LIKE/LIKEDS/LIKEREC. */
    private RpgType parseOptionalReturnType() {
        if (at(IDENT) || atAnyKeyword("like", "likeds", "likerec")) {
            return parseType();
        }
        return null;
    }

    // =======================================================================
    // Types and modifiers
    // =======================================================================

    private RpgType parseType() {
        if (at(KEYWORD)) {
            switch (peek().text().toLowerCase()) {
                case "like":    advance(); return new RpgType.Like(parenIdent());
                case "likeds":  advance(); return new RpgType.LikeDs(parenIdent());
                case "likerec": advance(); return parseLikeRec();
                default: break;
            }
        }
        String name = expect(IDENT, "type name").text();
        Integer length = null, decimals = null;
        if (match(LPAREN)) {
            length = intValue(expect(INT_LITERAL, "type length"));
            if (match(COLON)) decimals = intValue(expect(INT_LITERAL, "decimals"));
            expect(RPAREN, "')' after type size");
        }
        return new RpgType.Scalar(name, length, decimals);
    }

    private RpgType parseLikeRec() {
        expect(LPAREN, "'(' after LIKEREC");
        List<String> parts = new ArrayList<>();
        parts.add(expect(IDENT, "record name").text());
        while (match(COLON)) parts.add(expect(IDENT, "record part").text());
        expect(RPAREN, "')' after LIKEREC");
        return new RpgType.LikeRec(parts.remove(0), parts);
    }

    private List<RpgKeyword> parseModifiers() {
        List<RpgKeyword> out = new ArrayList<>();
        // A modifier name is a keyword (INZ, QUALIFIED, ...) or a plain identifier
        // (CTL-OPT options like MAIN, DCL-F keywords like KEYED) — name (args)?.
        while ((at(KEYWORD) || at(IDENT)) && !isDeclEndKeyword(peek().text())) {
            String name = advance().text();
            List<RpgExpr> args = new ArrayList<>();
            if (match(LPAREN)) {
                if (!at(RPAREN)) {
                    args.add(parseArg());
                    while (match(COLON)) args.add(parseArg());
                }
                expect(RPAREN, "')' after modifier arguments");
            }
            out.add(new RpgKeyword(name, args));
        }
        return out;
    }

    /** A modifier argument may be a {@code *}-name (e.g. USAGE(*INPUT)) or an expression. */
    private RpgExpr parseArg() {
        if (at(STAR_NAME)) return starName(advance().text());
        return parseExpression();
    }

    private static boolean isDeclEndKeyword(String kw) {
        return switch (kw.toLowerCase()) {
            case "end-ds", "end-pr", "end-pi", "end-proc" -> true;
            default -> false;
        };
    }

    // =======================================================================
    // Statements (§4.3)
    // =======================================================================

    private RpgStatement parseIf() {
        expectKeyword("if");
        RpgExpr cond = parseExpression();
        expect(SEMI, "';' after if condition");
        List<RpgStatement> thenBody = parseStatementsUntil("elseif", "else", "endif");
        List<RpgStatement.ElseIf> elseIfs = new ArrayList<>();
        while (atKeyword("elseif")) {
            advance();
            RpgExpr c = parseExpression();
            expect(SEMI, "';' after elseif condition");
            elseIfs.add(new RpgStatement.ElseIf(c, parseStatementsUntil("elseif", "else", "endif")));
        }
        List<RpgStatement> elseBody = List.of();
        if (atKeyword("else")) {
            advance();
            expect(SEMI, "';' after else");
            elseBody = parseStatementsUntil("endif");
        }
        expectKeyword("endif");
        expect(SEMI, "';' after endif");
        return new RpgStatement.If(cond, thenBody, elseIfs, elseBody);
    }

    private RpgStatement parseSelect() {
        expectKeyword("select");
        expect(SEMI, "';' after select");
        List<RpgStatement.WhenClause> whens = new ArrayList<>();
        while (atKeyword("when")) {
            advance();
            RpgExpr c = parseExpression();
            expect(SEMI, "';' after when condition");
            whens.add(new RpgStatement.WhenClause(c, parseStatementsUntil("when", "other", "endsl")));
        }
        List<RpgStatement> otherBody = List.of();
        if (atKeyword("other")) {
            advance();
            expect(SEMI, "';' after other");
            otherBody = parseStatementsUntil("endsl");
        }
        expectKeyword("endsl");
        expect(SEMI, "';' after endsl");
        return new RpgStatement.Select(whens, otherBody);
    }

    private RpgStatement parseDow() {
        expectKeyword("dow");
        RpgExpr cond = parseExpression();
        expect(SEMI, "';' after dow condition");
        List<RpgStatement> body = parseStatementsUntil("enddo");
        expectKeyword("enddo");
        expect(SEMI, "';' after enddo");
        return new RpgStatement.Dow(cond, body);
    }

    private RpgStatement parseDou() {
        expectKeyword("dou");
        RpgExpr cond = parseExpression();
        expect(SEMI, "';' after dou condition");
        List<RpgStatement> body = parseStatementsUntil("enddo");
        expectKeyword("enddo");
        expect(SEMI, "';' after enddo");
        return new RpgStatement.Dou(cond, body);
    }

    private RpgStatement parseFor() {
        expectKeyword("for");
        String var = expect(IDENT, "for variable").text();
        expect(EQ, "'=' in for");
        RpgExpr from = parseExpression();
        RpgStatement.ForDir dir;
        if (atKeyword("to")) { advance(); dir = RpgStatement.ForDir.TO; }
        else if (atKeyword("downto")) { advance(); dir = RpgStatement.ForDir.DOWNTO; }
        else throw new RpgParseException("Expected TO or DOWNTO", peek());
        RpgExpr to = parseExpression();
        RpgExpr by = null;
        if (atKeyword("by")) { advance(); by = parseExpression(); }
        expect(SEMI, "';' after for header");
        List<RpgStatement> body = parseStatementsUntil("endfor");
        expectKeyword("endfor");
        expect(SEMI, "';' after endfor");
        return new RpgStatement.For(var, from, dir, to, by, body);
    }

    private RpgStatement parseMonitor() {
        expectKeyword("monitor");
        expect(SEMI, "';' after monitor");
        List<RpgStatement> body = parseStatementsUntil("on-error", "on-exit", "endmon");
        List<RpgStatement.OnError> onErrors = new ArrayList<>();
        while (atKeyword("on-error")) {
            advance();
            List<RpgExpr> statuses = new ArrayList<>();
            if (!at(SEMI)) {
                statuses.add(parseExpression());
                while (match(COLON)) statuses.add(parseExpression());
            }
            expect(SEMI, "';' after on-error");
            onErrors.add(new RpgStatement.OnError(statuses, parseStatementsUntil("on-error", "on-exit", "endmon")));
        }
        List<RpgStatement> onExit = List.of();
        if (atKeyword("on-exit")) {
            advance();
            expect(SEMI, "';' after on-exit");
            onExit = parseStatementsUntil("endmon");
        }
        expectKeyword("endmon");
        expect(SEMI, "';' after endmon");
        return new RpgStatement.Monitor(body, onErrors, onExit);
    }

    private RpgStatement parseSubroutine() {
        expectKeyword("begsr");
        String name = expect(IDENT, "subroutine name").text();
        expect(SEMI, "';' after begsr");
        List<RpgStatement> body = parseStatementsUntil("endsr");
        expectKeyword("endsr");
        match(IDENT);
        expect(SEMI, "';' after endsr");
        return new RpgStatement.Subroutine(name, body);
    }

    private RpgStatement parseReturn() {
        expectKeyword("return");
        RpgExpr value = at(SEMI) ? null : parseExpression();
        expect(SEMI, "';' after return");
        return new RpgStatement.Return(value);
    }

    private RpgStatement parseEval() {
        String op = advance().text().toLowerCase();   // eval | evalr
        RpgStatement.EvalMode mode = RpgStatement.EvalMode.PLAIN;
        if (match(LPAREN)) {
            String flag = expect(IDENT, "EVAL flag").text();
            if (flag.equalsIgnoreCase("h")) mode = RpgStatement.EvalMode.HALF_ADJUST;
            else if (flag.equalsIgnoreCase("r")) mode = RpgStatement.EvalMode.TRUNCATE;
            expect(RPAREN, "')' after EVAL flag");
        }
        RpgExpr target = parsePostfix();
        expect(EQ, "'=' in EVAL");
        RpgExpr value = parseExpression();
        expect(SEMI, "';' after EVAL");
        return new RpgStatement.Assignment(target, value, mode);
    }

    private RpgStatement parseOp() {
        String opcode = advance().text();
        List<RpgExpr> operands = new ArrayList<>();
        while (!at(SEMI) && !at(EOF)) operands.add(parseArg());
        expect(SEMI, "';' after " + opcode);
        return new RpgStatement.Op(opcode, operands);
    }

    private RpgStatement parseAssignmentOrExpr() {
        RpgExpr head = parsePostfix();          // lvalue (stops before '=')
        if (match(EQ)) {
            RpgExpr value = parseExpression();
            expect(SEMI, "';' after assignment");
            return new RpgStatement.Assignment(head, value, RpgStatement.EvalMode.PLAIN);
        }
        expect(SEMI, "';' after expression statement");
        return new RpgStatement.ExprStatement(head);
    }

    private List<RpgStatement> parseStatementsUntil(String... stops) {
        List<RpgStatement> body = new ArrayList<>();
        while (!at(EOF) && !atAnyKeyword(stops)) {
            RpgItem item = parseItem();
            if (!(item instanceof RpgStatement s)) {
                throw new RpgParseException("Declarations are not allowed in this block", peek());
            }
            body.add(s);
        }
        return body;
    }

    private RpgStatement bare(RpgStatement stmt) {
        advance(); // the keyword
        expect(SEMI, "';'");
        return stmt;
    }

    // =======================================================================
    // Expressions (§4.4 precedence chain)
    // =======================================================================

    public RpgExpr parseExpression() { return parseOr(); }

    private RpgExpr parseOr() {
        RpgExpr left = parseAnd();
        while (atKeyword("or") || atKeyword("xor")) {
            RpgExpr.BinOp op = advance().text().equalsIgnoreCase("xor") ? RpgExpr.BinOp.XOR : RpgExpr.BinOp.OR;
            left = new RpgExpr.Binary(left, op, parseAnd());
        }
        return left;
    }

    private RpgExpr parseAnd() {
        RpgExpr left = parseNot();
        while (atKeyword("and")) {
            advance();
            left = new RpgExpr.Binary(left, RpgExpr.BinOp.AND, parseNot());
        }
        return left;
    }

    private RpgExpr parseNot() {
        if (atKeyword("not")) {
            advance();
            return new RpgExpr.Unary(RpgExpr.UnOp.NOT, parseNot());
        }
        return parseComparison();
    }

    private RpgExpr parseComparison() {
        RpgExpr left = parseAdditive();
        RpgExpr.BinOp op = switch (peek().type()) {
            case EQ -> RpgExpr.BinOp.EQ;
            case NE -> RpgExpr.BinOp.NE;
            case LT -> RpgExpr.BinOp.LT;
            case GT -> RpgExpr.BinOp.GT;
            case LE -> RpgExpr.BinOp.LE;
            case GE -> RpgExpr.BinOp.GE;
            default -> null;
        };
        if (op == null) return left;
        advance();
        return new RpgExpr.Binary(left, op, parseAdditive());
    }

    private RpgExpr parseAdditive() {
        RpgExpr left = parseMultiplicative();
        while (at(PLUS) || at(MINUS)) {
            RpgExpr.BinOp op = advance().type() == PLUS ? RpgExpr.BinOp.ADD : RpgExpr.BinOp.SUB;
            left = new RpgExpr.Binary(left, op, parseMultiplicative());
        }
        return left;
    }

    private RpgExpr parseMultiplicative() {
        RpgExpr left = parsePower();
        while (at(STAR) || at(SLASH)) {
            RpgExpr.BinOp op = advance().type() == STAR ? RpgExpr.BinOp.MUL : RpgExpr.BinOp.DIV;
            left = new RpgExpr.Binary(left, op, parsePower());
        }
        return left;
    }

    private RpgExpr parsePower() {
        RpgExpr left = parseUnary();
        if (match(STARSTAR)) {
            return new RpgExpr.Binary(left, RpgExpr.BinOp.POW, parsePower());
        }
        return left;
    }

    private RpgExpr parseUnary() {
        if (at(PLUS)) { advance(); return new RpgExpr.Unary(RpgExpr.UnOp.POS, parseUnary()); }
        if (at(MINUS)) { advance(); return new RpgExpr.Unary(RpgExpr.UnOp.NEG, parseUnary()); }
        return parsePostfix();
    }

    private RpgExpr parsePostfix() {
        RpgExpr e = parsePrimary();
        while (true) {
            if (at(LPAREN)) {
                e = new RpgExpr.Call(e, parseArgList());
            } else if (match(DOT)) {
                e = new RpgExpr.Member(e, expect(IDENT, "field name after '.'").text());
            } else {
                return e;
            }
        }
    }

    private RpgExpr parsePrimary() {
        RpgToken t = peek();
        switch (t.type()) {
            case LPAREN -> {
                advance();
                RpgExpr inner = parseExpression();
                expect(RPAREN, "')'");
                return inner;
            }
            case IDENT -> { advance(); return new RpgExpr.Identifier(t.text()); }
            case BIF -> { advance(); return new RpgExpr.BifCall(t.text(), at(LPAREN) ? parseArgList() : List.of()); }
            case STAR_NAME -> { advance(); return starName(t.text()); }
            case INT_LITERAL -> { advance(); return lit(RpgExpr.LiteralKind.INT, t); }
            case DEC_LITERAL -> { advance(); return lit(RpgExpr.LiteralKind.DEC, t); }
            case STRING_LITERAL -> { advance(); return lit(RpgExpr.LiteralKind.STRING, t); }
            case HEX_LITERAL -> { advance(); return lit(RpgExpr.LiteralKind.HEX, t); }
            case DATE_LITERAL -> { advance(); return lit(RpgExpr.LiteralKind.DATE, t); }
            case TIME_LITERAL -> { advance(); return lit(RpgExpr.LiteralKind.TIME, t); }
            case TIMESTAMP_LITERAL -> { advance(); return lit(RpgExpr.LiteralKind.TIMESTAMP, t); }
            case UCS2_LITERAL -> { advance(); return lit(RpgExpr.LiteralKind.UCS2, t); }
            case GRAPHIC_LITERAL -> { advance(); return lit(RpgExpr.LiteralKind.GRAPHIC, t); }
            default -> throw new RpgParseException("Expected an expression", t);
        }
    }

    private List<RpgExpr> parseArgList() {
        expect(LPAREN, "'('");
        List<RpgExpr> args = new ArrayList<>();
        if (!at(RPAREN)) {
            args.add(parseArg());
            while (match(COLON)) args.add(parseArg());
        }
        expect(RPAREN, "')'");
        return args;
    }

    private static RpgExpr lit(RpgExpr.LiteralKind kind, RpgToken t) {
        return new RpgExpr.Literal(kind, t.text());
    }

    private static RpgExpr starName(String text) {
        return text.toLowerCase().startsWith("*in")
            ? new RpgExpr.IndicatorRef(text)
            : new RpgExpr.Figurative(text);
    }

    // =======================================================================
    // Token cursor helpers
    // =======================================================================

    private RpgToken peek() { return tokens.get(pos); }

    private boolean at(RpgTokenType type) { return peek().type() == type; }

    private boolean atKeyword(String kw) {
        return at(KEYWORD) && peek().text().equalsIgnoreCase(kw);
    }

    private boolean atAnyKeyword(String... kws) {
        for (String kw : kws) if (atKeyword(kw)) return true;
        return false;
    }

    private RpgToken advance() {
        RpgToken t = peek();
        if (t.type() != EOF) pos++;
        return t;
    }

    private boolean match(RpgTokenType type) {
        if (at(type)) { advance(); return true; }
        return false;
    }

    private RpgToken expect(RpgTokenType type, String what) {
        if (at(type)) return advance();
        throw new RpgParseException("Expected " + what, peek());
    }

    private void expectKeyword(String kw) {
        if (atKeyword(kw)) { advance(); return; }
        throw new RpgParseException("Expected '" + kw + "'", peek());
    }

    private String parenIdent() {
        expect(LPAREN, "'('");
        String id = expect(IDENT, "name").text();
        expect(RPAREN, "')'");
        return id;
    }

    private static int intValue(RpgToken t) {
        return Integer.parseInt(t.text());
    }
}
