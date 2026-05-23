package com.larena.boxbreaker.plugin.bbk;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import static com.larena.boxbreaker.plugin.bbk.psi.BbkTypes.*;

%%

%public
%class _BbkLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode
%ignorecase

%{
  public _BbkLexer() {
    this((java.io.Reader) null);
  }
%}

// ----- Macros -----

DIGIT       = [0-9]
HEXDIGIT    = [0-9a-fA-F]
OCTDIGIT    = [0-7]
LETTER      = [a-zA-Z]
IDENT_START = {LETTER} | "_"
IDENT_CONT  = {IDENT_START} | {DIGIT}

NEWLINE       = \r\n | \r | \n
WHITESPACE    = [ \t\f]+
LINE_TERM     = {NEWLINE}+
LINE_COMMENT  = "//" [^\r\n]*
BLOCK_COMMENT = "/*" ~"*/"

IDENT      = {IDENT_START} {IDENT_CONT}*
STAR_IDENT = "*" {IDENT}

INT_LIT      = {DIGIT}+
INT_LIT_HEX  = "0x" {HEXDIGIT}+
INT_LIT_OCT  = "0o" {OCTDIGIT}+
FLOAT_LIT    = {DIGIT}+ "." {DIGIT}+ [eE] [+-]? {DIGIT}+
DEC_LIT      = {DIGIT}+ "." {DIGIT}+ [dD]?

STR_DOUBLE = \" ( [^\"\\\r\n] | \\ . )* \"
STR_SINGLE = \' ( [^\'\\\r\n] | \\ . )* \'

%%

<YYINITIAL> {

  // ----- Whitespace and comments -----
  {WHITESPACE}         { return TokenType.WHITE_SPACE; }
  {LINE_TERM}          { return TokenType.WHITE_SPACE; }
  {LINE_COMMENT}       { return LINE_COMMENT; }
  {BLOCK_COMMENT}      { return BLOCK_COMMENT; }

  // ----- Multi-char operators (longest first within section) -----
  "<<="                { return LT_LT_EQ; }
  ">>="                { return GT_GT_EQ; }
  "**"                 { return STAR_STAR; }
  "=="                 { return EQ_EQ; }
  "!="                 { return BANG_EQ; }
  "<="                 { return LT_EQ; }
  ">="                 { return GT_EQ; }
  "<<"                 { return LT_LT; }
  ">>"                 { return GT_GT; }
  "&&"                 { return AMP_AMP; }
  "||"                 { return PIPE_PIPE; }
  "+="                 { return PLUS_EQ; }
  "-="                 { return MINUS_EQ; }
  "*="                 { return STAR_EQ; }
  "/="                 { return SLASH_EQ; }
  "%="                 { return PERCENT_EQ; }
  "&="                 { return AMP_EQ; }
  "|="                 { return PIPE_EQ; }
  "^="                 { return CARET_EQ; }
  "->"                 { return ARROW; }

  // ----- Single-char operators -----
  "+"                  { return PLUS; }
  "-"                  { return MINUS; }
  "/"                  { return SLASH; }
  "%"                  { return PERCENT; }
  "<"                  { return LT; }
  ">"                  { return GT; }
  "="                  { return EQ; }
  "!"                  { return BANG; }
  "&"                  { return AMP; }
  "|"                  { return PIPE; }
  "^"                  { return CARET; }
  "~"                  { return TILDE; }
  "?"                  { return QUESTION; }

  // ----- Punctuators -----
  ";"                  { return SEMI; }
  ","                  { return COMMA; }
  "."                  { return DOT; }
  "("                  { return LPAREN; }
  ")"                  { return RPAREN; }
  "{"                  { return LBRACE; }
  "}"                  { return RBRACE; }
  "["                  { return LBRACKET; }
  "]"                  { return RBRACKET; }
  ":"                  { return COLON; }
  "@"                  { return AT; }

  // ----- Attribute modifiers (matched before identifiers and @) -----
  "@halfup"            { return ATTR_HALFUP; }
  "@halfdown"          { return ATTR_HALFDOWN; }
  "@trunc"             { return ATTR_TRUNC; }

  // ----- Directives (PRE-*) -----
  "PRE-IF"             { return KW_PRE_IF; }
  "PRE-ELSEIF"         { return KW_PRE_ELSEIF; }
  "PRE-ELSE"           { return KW_PRE_ELSE; }
  "PRE-ENDIF"          { return KW_PRE_ENDIF; }
  "PRE-DEFINE"         { return KW_PRE_DEFINE; }
  "PRE-UNDEFINE"       { return KW_PRE_UNDEFINE; }
  "PRE-INCLUDE"        { return KW_PRE_INCLUDE; }
  "PRE-EOF"            { return KW_PRE_EOF; }
  "CTL-OPT"            { return KW_CTL_OPT; }

  // ----- Declaration keywords (DCL-*) -----
  "DCL-S"              { return KW_DCL_S; }
  "DCL-C"              { return KW_DCL_C; }
  "DCL-DS"             { return KW_DCL_DS; }
  "DCL-PR"             { return KW_DCL_PR; }
  "DCL-PROC"           { return KW_DCL_PROC; }
  "DCL-F"              { return KW_DCL_F; }
  "DCL-PARM"           { return KW_DCL_PARM; }
  "DCL-SUBF"           { return KW_DCL_SUBF; }

  // ----- Error handling clauses (hyphenated) -----
  "on-error"           { return KW_ON_ERROR; }
  "on-exit"            { return KW_ON_EXIT; }

  // ----- Control flow keywords (C-style) -----
  "if"                 { return KW_IF; }
  "else"               { return KW_ELSE; }
  "while"              { return KW_WHILE; }
  "do"                 { return KW_DO; }
  "for"                { return KW_FOR; }
  "break"              { return KW_BREAK; }
  "continue"           { return KW_CONTINUE; }
  "return"             { return KW_RETURN; }

  // ----- Control flow keywords (RPG-style retained) -----
  "select"             { return KW_SELECT; }
  "when"               { return KW_WHEN; }
  "other"              { return KW_OTHER; }
  "monitor"            { return KW_MONITOR; }

  // ----- Boolean and null literals -----
  "true"               { return KW_TRUE; }
  "false"              { return KW_FALSE; }
  "null"               { return KW_NULL; }

  // ----- Primitive types -----
  "CHAR"               { return KW_CHAR; }
  "VARCHAR"            { return KW_VARCHAR; }
  "PACKED"             { return KW_PACKED; }
  "ZONED"              { return KW_ZONED; }
  "BINDEC"             { return KW_BINDEC; }
  "INT"                { return KW_INT; }
  "UNS"                { return KW_UNS; }
  "FLOAT"              { return KW_FLOAT; }
  "DATE"               { return KW_DATE; }
  "TIME"               { return KW_TIME; }
  "TIMESTAMP"          { return KW_TIMESTAMP; }
  "BOOL"               { return KW_BOOL; }
  "POINTER"            { return KW_POINTER; }
  "VOID"               { return KW_VOID; }

  // ----- Declaration modifiers -----
  "INZ"                { return KW_INZ; }
  "BASED"              { return KW_BASED; }
  "DIM"                { return KW_DIM; }
  "OVERLAY"            { return KW_OVERLAY; }
  "POS"                { return KW_POS; }
  "LIKE"               { return KW_LIKE; }
  "LIKEDS"             { return KW_LIKEDS; }
  "LIKEREC"            { return KW_LIKEREC; }
  "TEMPLATE"           { return KW_TEMPLATE; }
  "QUALIFIED"          { return KW_QUALIFIED; }
  "ALIGN"              { return KW_ALIGN; }
  "VALUE"              { return KW_VALUE; }
  "CONST"              { return KW_CONST; }
  "OPTIONS"            { return KW_OPTIONS; }
  "RTNPARM"            { return KW_RTNPARM; }
  "OPDESC"             { return KW_OPDESC; }
  "STATIC"             { return KW_STATIC; }
  "EXPORT"             { return KW_EXPORT; }
  "IMPORT"             { return KW_IMPORT; }
  "EXTPGM"             { return KW_EXTPGM; }
  "EXTPROC"            { return KW_EXTPROC; }

  // ----- File-spec keywords -----
  "USAGE"              { return KW_USAGE; }
  "KEYED"              { return KW_KEYED; }
  "EXTNAME"            { return KW_EXTNAME; }
  "EXTFILE"            { return KW_EXTFILE; }
  "PREFIX"             { return KW_PREFIX; }
  "RENAME"             { return KW_RENAME; }
  "DISK"               { return KW_DISK; }
  "PRINTER"            { return KW_PRINTER; }
  "WORKSTN"            { return KW_WORKSTN; }
  "SEQ"                { return KW_SEQ; }
  "USROPN"             { return KW_USROPN; }
  "INFDS"              { return KW_INFDS; }
  "INDDS"              { return KW_INDDS; }

  // ----- Literals (numeric and string; STAR_IDENT before STAR) -----
  // STAR_IDENT must come before single "*" rule above (longest match handles it).
  {STAR_IDENT}         { return STAR_IDENT; }
  "*"                  { return STAR; }

  {INT_LIT_HEX}        { return INT_LIT_HEX; }
  {INT_LIT_OCT}        { return INT_LIT_OCT; }
  {FLOAT_LIT}          { return FLOAT_LIT; }
  {DEC_LIT}            { return DEC_LIT; }
  {INT_LIT}            { return INT_LIT; }
  {STR_DOUBLE}         { return STR_LIT; }
  {STR_SINGLE}         { return STR_LIT; }

  // ----- Identifiers (after all keywords) -----
  {IDENT}              { return IDENT; }
}

// ----- Anything else is a bad character -----
[^]                    { return TokenType.BAD_CHARACTER; }
