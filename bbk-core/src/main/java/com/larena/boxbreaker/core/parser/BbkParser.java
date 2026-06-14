package com.larena.boxbreaker.core.parser;

import com.larena.boxbreaker.core.ast.*;
import com.larena.boxbreaker.core.lexer.BbkLexer;
import com.larena.boxbreaker.core.lexer.BbkToken;
import com.larena.boxbreaker.core.lexer.BbkTokenType;

import java.util.ArrayList;
import java.util.List;

import static com.larena.boxbreaker.core.lexer.BbkTokenType.*;

/**
 * Recursive-descent parser for BBK, covering the full grammar
 * ({@code BBK.bnf}): all declarations, statements and the complete expression
 * precedence chain. Headless (no IntelliJ dependency).
 *
 * <p>Unlike RPG, BBK has no {@code =} ambiguity: equality is {@code ==} and
 * assignment is {@code =} (distinct tokens), so a statement is parsed as an
 * expression and an assignment iff a {@code =}-family operator follows.
 */
public final class BbkParser {

    private final List<BbkToken> tokens;
    private int pos = 0;

    public BbkParser(List<BbkToken> tokens) {
        this.tokens = tokens;
    }

    public static BbkProgram parse(String source) {
        return new BbkParser(BbkLexer.tokenize(source)).parseProgram();
    }

    // =======================================================================
    // Program
    // =======================================================================

    public BbkProgram parseProgram() {
        List<BbkItem> items = new ArrayList<>();
        while (!at(EOF)) items.add(parseItem());
        return new BbkProgram(items);
    }

    private BbkItem parseItem() {
        if (at(KEYWORD)) {
            String kw = peek().text();
            switch (kw) {
                case "DCL-S":    return parseVariable();
                case "DCL-C":    return parseConstant();
                case "DCL-DS":   return parseDataStructure();
                case "DCL-F":    return parseFile();
                case "DCL-PR":   return parsePrototype();
                case "DCL-PROC": return parseProcedure();
                case "CTL-OPT":  return parseCtlOpt();
                case "BEGSR":    return parseSubroutine();
                case "if":       return parseIf();
                case "select":   return parseSelect();
                case "while":    return parseWhile();
                case "do":       return parseDoWhile();
                case "for":      return parseFor();
                case "break":    return bare(new BbkStatement.Break());
                case "continue": return bare(new BbkStatement.Continue());
                case "return":   return parseReturn();
                case "monitor":  return parseMonitor();
                case "EXSR":     return parseExsr();
                case "LEAVESR":  return bare(new BbkStatement.Leavesr());
                case "CALLP":    return parseCallp();
                default:
                    if (isFileOp(kw)) return parseFileOp();
                    if (kw.startsWith("PRE-")) return parseDirective();
                    // fall through: a keyword that starts an expression (true/false/null)
            }
        }
        return parseExpressionStatement();
    }

    // =======================================================================
    // Declarations
    // =======================================================================

    private BbkDeclaration parseVariable() {
        expectKw("DCL-S");
        String name = expect(IDENT, "variable name").text();
        BbkType type = parseType();
        List<BbkModifier> mods = parseModifiers();
        expect(SEMI, "';'");
        return new BbkDeclaration.Variable(name, type, mods);
    }

    private BbkDeclaration parseConstant() {
        expectKw("DCL-C");
        String name = expect(IDENT, "constant name").text();
        BbkExpr value;
        if (atKw("CONST")) {
            advance();
            expect(LPAREN, "'('");
            value = parseExpression();
            expect(RPAREN, "')'");
        } else {
            value = parseExpression();
        }
        expect(SEMI, "';'");
        return new BbkDeclaration.Constant(name, value);
    }

    private BbkDeclaration parseDataStructure() {
        expectKw("DCL-DS");
        String name = expect(IDENT, "DS name").text();
        List<BbkModifier> mods = parseModifiers();
        List<BbkDeclaration.Subfield> subs = new ArrayList<>();
        if (match(LBRACE)) {
            while (!at(RBRACE) && !at(EOF)) subs.add(parseSubfield());
            expect(RBRACE, "'}'");
        } else {
            expect(SEMI, "';' or '{'");
        }
        return new BbkDeclaration.DataStructure(name, mods, subs);
    }

    private BbkDeclaration.Subfield parseSubfield() {
        String name = expect(IDENT, "subfield name").text();
        BbkType type = parseType();
        List<BbkModifier> mods = parseModifiers();
        expect(SEMI, "';'");
        return new BbkDeclaration.Subfield(name, type, mods);
    }

    private BbkDeclaration parseFile() {
        expectKw("DCL-F");
        String name = expect(IDENT, "file name").text();
        List<BbkModifier> kws = parseModifiers();
        expect(SEMI, "';'");
        return new BbkDeclaration.File(name, kws);
    }

    private BbkDeclaration parsePrototype() {
        expectKw("DCL-PR");
        String name = expect(IDENT, "prototype name").text();
        List<BbkDeclaration.Parameter> params = at(LPAREN) ? parseParams() : List.of();
        BbkType ret = match(ARROW) ? parseType() : null;
        List<BbkModifier> mods = parseModifiers();
        expect(SEMI, "';'");
        return new BbkDeclaration.Prototype(name, params, ret, mods);
    }

    private BbkDeclaration parseProcedure() {
        expectKw("DCL-PROC");
        String name = expect(IDENT, "procedure name").text();
        List<BbkDeclaration.Parameter> params = at(LPAREN) ? parseParams() : List.of();
        BbkType ret = match(ARROW) ? parseType() : null;
        List<BbkModifier> mods = parseModifiers();
        List<BbkItem> body = parseBlock();
        return new BbkDeclaration.Procedure(name, params, ret, mods, body);
    }

    private List<BbkDeclaration.Parameter> parseParams() {
        expect(LPAREN, "'('");
        List<BbkDeclaration.Parameter> out = new ArrayList<>();
        if (!at(RPAREN)) {
            out.add(parseParam());
            while (match(COMMA)) out.add(parseParam());
        }
        expect(RPAREN, "')'");
        return out;
    }

    private BbkDeclaration.Parameter parseParam() {
        String name = expect(IDENT, "parameter name").text();
        BbkType type = parseType();
        List<BbkModifier> mods = parseModifiers();
        return new BbkDeclaration.Parameter(name, type, mods);
    }

    private BbkDeclaration parseCtlOpt() {
        expectKw("CTL-OPT");
        List<BbkModifier> kws = parseModifiers();
        expect(SEMI, "';'");
        return new BbkDeclaration.CtlOpt(kws);
    }

    // ----- types and modifiers -----

    private BbkType parseType() {
        if (atKw("LIKE") || atKw("LIKEDS") || atKw("LIKEREC")) {
            BbkType.LikeKind kind = switch (advance().text()) {
                case "LIKE" -> BbkType.LikeKind.LIKE;
                case "LIKEDS" -> BbkType.LikeKind.LIKEDS;
                default -> BbkType.LikeKind.LIKEREC;
            };
            expect(LPAREN, "'('");
            String n = expect(IDENT, "name").text();
            expect(RPAREN, "')'");
            return new BbkType.Like(kind, n);
        }
        String name = expect(KEYWORD, "type name").text();
        Integer length = null, decimals = null;
        if (match(LPAREN)) {
            length = intOf(expect(INT_LIT, "length"));
            if (match(COLON)) decimals = intOf(expect(INT_LIT, "decimals"));
            expect(RPAREN, "')'");
        }
        return new BbkType.Primitive(name, length, decimals);
    }

    /** Modifier names may be keywords (INZ, QUALIFIED, ...) or identifiers (CTL-OPT options). */
    private List<BbkModifier> parseModifiers() {
        List<BbkModifier> out = new ArrayList<>();
        while (at(KEYWORD) || at(IDENT)) {
            String name = advance().text();
            List<BbkExpr> args = new ArrayList<>();
            if (match(LPAREN)) {
                if (!at(RPAREN)) {
                    args.add(parseExpression());
                    while (match(COLON)) args.add(parseExpression());   // BBK modifier args use ':'
                }
                expect(RPAREN, "')'");
            }
            out.add(new BbkModifier(name, args));
        }
        return out;
    }

    // =======================================================================
    // Statements
    // =======================================================================

    private List<BbkItem> parseBlock() {
        expect(LBRACE, "'{'");
        List<BbkItem> items = new ArrayList<>();
        while (!at(RBRACE) && !at(EOF)) items.add(parseItem());
        expect(RBRACE, "'}'");
        return items;
    }

    private BbkStatement parseIf() {
        expectKw("if");
        BbkExpr cond = parenExpr();
        List<BbkItem> then = parseBlock();
        List<BbkItem> elseBody = List.of();
        if (atKw("else")) {
            advance();
            // else if ...  OR  else { ... }
            elseBody = atKw("if") ? List.of(parseIf()) : parseBlock();
        }
        return new BbkStatement.If(cond, then, elseBody);
    }

    private BbkStatement parseSelect() {
        expectKw("select");
        expect(LBRACE, "'{'");
        List<BbkStatement.When> whens = new ArrayList<>();
        while (atKw("when")) {
            advance();
            BbkExpr c = parenExpr();
            whens.add(new BbkStatement.When(c, parseBlock()));
        }
        List<BbkItem> other = List.of();
        if (atKw("other")) {
            advance();
            other = parseBlock();
        }
        expect(RBRACE, "'}'");
        return new BbkStatement.Select(whens, other);
    }

    private BbkStatement parseWhile() {
        expectKw("while");
        BbkExpr cond = parenExpr();
        return new BbkStatement.While(cond, parseBlock());
    }

    private BbkStatement parseDoWhile() {
        expectKw("do");
        List<BbkItem> body = parseBlock();
        expectKw("while");
        BbkExpr cond = parenExpr();
        expect(SEMI, "';'");
        return new BbkStatement.DoWhile(body, cond);
    }

    private BbkStatement parseFor() {
        expectKw("for");
        expect(LPAREN, "'('");
        BbkItem init = at(SEMI) ? null : parseForInit();
        expect(SEMI, "';'");
        BbkExpr cond = at(SEMI) ? null : parseExpression();
        expect(SEMI, "';'");
        BbkStatement update = at(RPAREN) ? null : parseForUpdate();
        expect(RPAREN, "')'");
        return new BbkStatement.For(init, cond, update, parseBlock());
    }

    private BbkItem parseForInit() {
        if (atKw("DCL-S")) {
            advance();
            String name = expect(IDENT, "name").text();
            BbkType type = parseType();
            expect(EQ, "'='");
            BbkExpr value = parseExpression();
            return new BbkDeclaration.Variable(name, type,
                List.of(new BbkModifier("INZ", List.of(value))));
        }
        return assignOrExpr();
    }

    private BbkStatement parseForUpdate() {
        return assignOrExpr();
    }

    private BbkStatement parseReturn() {
        expectKw("return");
        BbkExpr v = at(SEMI) ? null : parseExpression();
        expect(SEMI, "';'");
        return new BbkStatement.Return(v);
    }

    private BbkStatement parseMonitor() {
        expectKw("monitor");
        List<BbkItem> body = parseBlock();
        List<BbkStatement.OnError> onErrors = new ArrayList<>();
        while (atKw("on-error")) {
            advance();
            List<BbkExpr> statuses = new ArrayList<>();
            if (match(LPAREN)) {
                if (!at(RPAREN)) {
                    statuses.add(parseExpression());
                    while (match(COMMA)) statuses.add(parseExpression());
                }
                expect(RPAREN, "')'");
            }
            onErrors.add(new BbkStatement.OnError(statuses, parseBlock()));
        }
        List<BbkItem> onExit = List.of();
        if (atKw("on-exit")) {
            advance();
            onExit = parseBlock();
        }
        return new BbkStatement.Monitor(body, onErrors, onExit);
    }

    private BbkStatement parseSubroutine() {
        expectKw("BEGSR");
        String name = expect(IDENT, "subroutine name").text();
        expect(SEMI, "';'");
        List<BbkItem> body = new ArrayList<>();
        while (!atKw("ENDSR") && !at(EOF)) body.add(parseItem());
        expectKw("ENDSR");
        if (at(IDENT)) advance();   // optional repeated name
        expect(SEMI, "';'");
        return new BbkStatement.Subroutine(name, body);
    }

    private BbkStatement parseExsr() {
        expectKw("EXSR");
        String name = expect(IDENT, "subroutine name").text();
        expect(SEMI, "';'");
        return new BbkStatement.Exsr(name);
    }

    private BbkStatement parseCallp() {
        expectKw("CALLP");
        BbkExpr e = parsePostfix();
        expect(SEMI, "';'");
        return new BbkStatement.Callp(e);
    }

    private BbkStatement parseFileOp() {
        String opcode = advance().text();
        List<BbkExpr> operands = new ArrayList<>();
        while (!at(SEMI) && !at(EOF)) operands.add(parseExpression());
        expect(SEMI, "';'");
        return new BbkStatement.FileOp(opcode, operands);
    }

    private BbkStatement parseDirective() {
        String kw = advance().text();
        List<BbkExpr> args = new ArrayList<>();
        switch (kw) {
            case "PRE-IF", "PRE-ELSEIF" -> args.add(parseExpression());
            case "PRE-INCLUDE" -> args.add(parsePrimary());
            case "PRE-UNDEFINE" -> args.add(new BbkExpr.Identifier(expect(IDENT, "name").text()));
            case "PRE-DEFINE" -> {
                args.add(new BbkExpr.Identifier(expect(IDENT, "name").text()));
                if (at(INT_LIT) || at(DEC_LIT) || at(STR_LIT) || at(STAR_IDENT)) {
                    args.add(parsePrimary());
                }
            }
            default -> { /* PRE-ELSE, PRE-ENDIF, PRE-EOF: bare */ }
        }
        return new BbkStatement.Directive(kw, args);
    }

    /** A bare expression statement or an assignment ({@code target op value [@attr];}). */
    private BbkStatement parseExpressionStatement() {
        BbkStatement s = assignOrExpr();
        expect(SEMI, "';'");
        return s;
    }

    /** Shared by expression-statements and for-init/update (no trailing ';'). */
    private BbkStatement assignOrExpr() {
        BbkExpr lhs = parseExpression();
        BbkStatement.AssignOp op = assignOp(peek().type());
        if (op != null) {
            advance();
            BbkExpr value = parseExpression();
            BbkStatement.AttrMod attr = BbkStatement.AttrMod.NONE;
            if (at(ATTR)) attr = attrMod(advance().text());
            return new BbkStatement.Assignment(lhs, op, value, attr);
        }
        return new BbkStatement.ExpressionStatement(lhs);
    }

    private BbkStatement bare(BbkStatement stmt) {
        advance();
        expect(SEMI, "';'");
        return stmt;
    }

    // =======================================================================
    // Expressions (full precedence chain)
    // =======================================================================

    public BbkExpr parseExpression() { return parseTernary(); }

    private BbkExpr parseTernary() {
        BbkExpr cond = parseBinary(0);
        if (match(QUESTION)) {
            BbkExpr then = parseExpression();
            expect(COLON, "':'");
            BbkExpr otherwise = parseTernary();
            return new BbkExpr.Ternary(cond, then, otherwise);
        }
        return cond;
    }

    /** Precedence-climbing for the left-associative binary levels (|| down to <<). */
    private BbkExpr parseBinary(int level) {
        if (level >= LEVELS.length) return parsePower();
        BbkExpr left = parseBinary(level + 1);
        while (true) {
            BbkExpr.BinOp op = LEVELS[level].match(peek().type());
            if (op == null) return left;
            advance();
            left = new BbkExpr.Binary(left, op, parseBinary(level + 1));
        }
    }

    private BbkExpr parsePower() {
        BbkExpr left = parseUnary();
        if (match(STAR_STAR)) {                       // right-associative
            return new BbkExpr.Binary(left, BbkExpr.BinOp.POW, parsePower());
        }
        return left;
    }

    private BbkExpr parseUnary() {
        BbkExpr.UnOp op = switch (peek().type()) {
            case PLUS -> BbkExpr.UnOp.POS;
            case MINUS -> BbkExpr.UnOp.NEG;
            case BANG -> BbkExpr.UnOp.NOT;
            case TILDE -> BbkExpr.UnOp.BIT_NOT;
            default -> null;
        };
        if (op != null) { advance(); return new BbkExpr.Unary(op, parseUnary()); }
        return parsePostfix();
    }

    private BbkExpr parsePostfix() {
        BbkExpr e = parsePrimary();
        while (true) {
            if (at(LPAREN)) {
                e = new BbkExpr.Call(e, argList(LPAREN, RPAREN));
            } else if (at(LBRACKET)) {
                e = new BbkExpr.Index(e, argList(LBRACKET, RBRACKET));
            } else if (match(DOT)) {
                e = new BbkExpr.Member(e, expect(IDENT, "field").text(), false);
            } else if (match(ARROW)) {
                e = new BbkExpr.Member(e, expect(IDENT, "field").text(), true);
            } else {
                return e;
            }
        }
    }

    private BbkExpr parsePrimary() {
        BbkToken t = peek();
        switch (t.type()) {
            case LPAREN -> { advance(); BbkExpr e = parseExpression(); expect(RPAREN, "')'"); return e; }
            case IDENT -> { advance(); return new BbkExpr.Identifier(t.text()); }
            case STAR_IDENT -> { advance(); return new BbkExpr.StarIdent(t.text()); }
            case INT_LIT -> { advance(); return lit(BbkExpr.LitKind.INT, t); }
            case INT_LIT_HEX -> { advance(); return lit(BbkExpr.LitKind.HEX, t); }
            case INT_LIT_OCT -> { advance(); return lit(BbkExpr.LitKind.OCT, t); }
            case FLOAT_LIT -> { advance(); return lit(BbkExpr.LitKind.FLOAT, t); }
            case DEC_LIT -> { advance(); return lit(BbkExpr.LitKind.DEC, t); }
            case STR_LIT -> { advance(); return lit(BbkExpr.LitKind.STRING, t); }
            case KEYWORD -> {
                switch (t.text()) {
                    case "true": advance(); return new BbkExpr.BoolLit(true);
                    case "false": advance(); return new BbkExpr.BoolLit(false);
                    case "null": advance(); return new BbkExpr.NullLit();
                    default: throw new BbkParseException("Expected an expression", t);
                }
            }
            default -> throw new BbkParseException("Expected an expression", t);
        }
    }

    private List<BbkExpr> argList(BbkTokenType open, BbkTokenType close) {
        expect(open, "'" + open + "'");
        List<BbkExpr> args = new ArrayList<>();
        if (!at(close)) {
            args.add(parseExpression());
            while (match(COMMA)) args.add(parseExpression());   // call/subscript args use ','
        }
        expect(close, "')'/']'");
        return args;
    }

    private static BbkExpr lit(BbkExpr.LitKind kind, BbkToken t) {
        return new BbkExpr.Literal(kind, t.text());
    }

    // ----- binary operator levels, highest-numbered = tightest binding -----

    @FunctionalInterface private interface OpMatch { BbkExpr.BinOp match(BbkTokenType t); }

    private static final OpMatch[] LEVELS = {
        t -> t == PIPE_PIPE ? BbkExpr.BinOp.OR : null,                               // ||
        t -> t == AMP_AMP ? BbkExpr.BinOp.AND : null,                               // &&
        t -> t == PIPE ? BbkExpr.BinOp.BIT_OR : null,                               // |
        t -> t == CARET ? BbkExpr.BinOp.BIT_XOR : null,                             // ^
        t -> t == AMP ? BbkExpr.BinOp.BIT_AND : null,                               // &
        t -> t == EQ_EQ ? BbkExpr.BinOp.EQ : t == BANG_EQ ? BbkExpr.BinOp.NE : null,// == !=
        t -> switch (t) { case LT -> BbkExpr.BinOp.LT; case GT -> BbkExpr.BinOp.GT;
                          case LT_EQ -> BbkExpr.BinOp.LE; case GT_EQ -> BbkExpr.BinOp.GE;
                          default -> null; },                                       // < > <= >=
        t -> t == LT_LT ? BbkExpr.BinOp.SHL : t == GT_GT ? BbkExpr.BinOp.SHR : null,// << >>
        t -> t == PLUS ? BbkExpr.BinOp.ADD : t == MINUS ? BbkExpr.BinOp.SUB : null, // + -
        t -> switch (t) { case STAR -> BbkExpr.BinOp.MUL; case SLASH -> BbkExpr.BinOp.DIV;
                          case PERCENT -> BbkExpr.BinOp.MOD; default -> null; },    // * / %
    };

    private static BbkStatement.AssignOp assignOp(BbkTokenType t) {
        return switch (t) {
            case EQ -> BbkStatement.AssignOp.ASSIGN;
            case PLUS_EQ -> BbkStatement.AssignOp.ADD;
            case MINUS_EQ -> BbkStatement.AssignOp.SUB;
            case STAR_EQ -> BbkStatement.AssignOp.MUL;
            case SLASH_EQ -> BbkStatement.AssignOp.DIV;
            case PERCENT_EQ -> BbkStatement.AssignOp.MOD;
            case AMP_EQ -> BbkStatement.AssignOp.AND;
            case PIPE_EQ -> BbkStatement.AssignOp.OR;
            case CARET_EQ -> BbkStatement.AssignOp.XOR;
            case LT_LT_EQ -> BbkStatement.AssignOp.SHL;
            case GT_GT_EQ -> BbkStatement.AssignOp.SHR;
            default -> null;
        };
    }

    private static BbkStatement.AttrMod attrMod(String text) {
        return switch (text) {
            case "@halfup" -> BbkStatement.AttrMod.HALFUP;
            case "@halfdown" -> BbkStatement.AttrMod.HALFDOWN;
            case "@trunc" -> BbkStatement.AttrMod.TRUNC;
            default -> BbkStatement.AttrMod.NONE;
        };
    }

    private static boolean isFileOp(String kw) {
        return switch (kw) {
            case "read", "reade", "readp", "readpe", "chain", "setll", "setgt",
                 "write", "update", "delete", "unlock", "open", "close", "exfmt" -> true;
            default -> false;
        };
    }

    // =======================================================================
    // Cursor helpers
    // =======================================================================

    private BbkToken peek() { return tokens.get(pos); }
    private boolean at(BbkTokenType t) { return peek().type() == t; }
    private boolean atKw(String kw) { return at(KEYWORD) && peek().text().equals(kw); }

    private BbkToken advance() {
        BbkToken t = peek();
        if (t.type() != EOF) pos++;
        return t;
    }

    private boolean match(BbkTokenType t) {
        if (at(t)) { advance(); return true; }
        return false;
    }

    private BbkToken expect(BbkTokenType t, String what) {
        if (at(t)) return advance();
        throw new BbkParseException("Expected " + what, peek());
    }

    private void expectKw(String kw) {
        if (atKw(kw)) { advance(); return; }
        throw new BbkParseException("Expected '" + kw + "'", peek());
    }

    private BbkExpr parenExpr() {
        expect(LPAREN, "'('");
        BbkExpr e = parseExpression();
        expect(RPAREN, "')'");
        return e;
    }

    private static int intOf(BbkToken t) { return Integer.parseInt(t.text()); }
}
