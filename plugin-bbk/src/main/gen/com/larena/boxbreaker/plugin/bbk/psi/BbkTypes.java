// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.larena.boxbreaker.plugin.bbk.BbkElementType;
import com.larena.boxbreaker.plugin.bbk.BbkTokenType;
import com.larena.boxbreaker.plugin.bbk.psi.impl.*;

public interface BbkTypes {

  IElementType BASED_MODIFIER = new BbkElementType("BASED_MODIFIER");
  IElementType CONSTANT_DECLARATION = new BbkElementType("CONSTANT_DECLARATION");
  IElementType CONSTANT_VALUE = new BbkElementType("CONSTANT_VALUE");
  IElementType CONST_WRAPPER = new BbkElementType("CONST_WRAPPER");
  IElementType DIM_MODIFIER = new BbkElementType("DIM_MODIFIER");
  IElementType EXPORT_MODIFIER = new BbkElementType("EXPORT_MODIFIER");
  IElementType INZ_MODIFIER = new BbkElementType("INZ_MODIFIER");
  IElementType LIKE_REFERENCE = new BbkElementType("LIKE_REFERENCE");
  IElementType LITERAL = new BbkElementType("LITERAL");
  IElementType PRIMITIVE_TYPE = new BbkElementType("PRIMITIVE_TYPE");
  IElementType PRIMITIVE_TYPE_SPEC = new BbkElementType("PRIMITIVE_TYPE_SPEC");
  IElementType QUALIFIED_MODIFIER = new BbkElementType("QUALIFIED_MODIFIER");
  IElementType STATIC_MODIFIER = new BbkElementType("STATIC_MODIFIER");
  IElementType TOP_LEVEL_ITEM = new BbkElementType("TOP_LEVEL_ITEM");
  IElementType TYPE_ARGS = new BbkElementType("TYPE_ARGS");
  IElementType TYPE_SPECIFICATION = new BbkElementType("TYPE_SPECIFICATION");
  IElementType VARIABLE_DECLARATION = new BbkElementType("VARIABLE_DECLARATION");
  IElementType VAR_MODIFIER = new BbkElementType("VAR_MODIFIER");

  IElementType AMP = new BbkTokenType("&");
  IElementType AMP_AMP = new BbkTokenType("&&");
  IElementType AMP_EQ = new BbkTokenType("&=");
  IElementType ARROW = new BbkTokenType("->");
  IElementType AT = new BbkTokenType("@");
  IElementType ATTR_HALFDOWN = new BbkTokenType("@halfdown");
  IElementType ATTR_HALFUP = new BbkTokenType("@halfup");
  IElementType ATTR_TRUNC = new BbkTokenType("@trunc");
  IElementType BANG = new BbkTokenType("!");
  IElementType BANG_EQ = new BbkTokenType("!=");
  IElementType BLOCK_COMMENT = new BbkTokenType("BLOCK_COMMENT");
  IElementType CARET = new BbkTokenType("^");
  IElementType CARET_EQ = new BbkTokenType("^=");
  IElementType COLON = new BbkTokenType(":");
  IElementType COMMA = new BbkTokenType(",");
  IElementType DEC_LIT = new BbkTokenType("DEC_LIT");
  IElementType DOT = new BbkTokenType(".");
  IElementType EQ = new BbkTokenType("=");
  IElementType EQ_EQ = new BbkTokenType("==");
  IElementType FLOAT_LIT = new BbkTokenType("FLOAT_LIT");
  IElementType GT = new BbkTokenType(">");
  IElementType GT_EQ = new BbkTokenType(">=");
  IElementType GT_GT = new BbkTokenType(">>");
  IElementType GT_GT_EQ = new BbkTokenType(">>=");
  IElementType IDENT = new BbkTokenType("IDENT");
  IElementType INT_LIT = new BbkTokenType("INT_LIT");
  IElementType INT_LIT_HEX = new BbkTokenType("INT_LIT_HEX");
  IElementType INT_LIT_OCT = new BbkTokenType("INT_LIT_OCT");
  IElementType KW_ALIGN = new BbkTokenType("ALIGN");
  IElementType KW_BASED = new BbkTokenType("BASED");
  IElementType KW_BINDEC = new BbkTokenType("BINDEC");
  IElementType KW_BOOL = new BbkTokenType("BOOL");
  IElementType KW_BREAK = new BbkTokenType("break");
  IElementType KW_CHAR = new BbkTokenType("CHAR");
  IElementType KW_CONST = new BbkTokenType("CONST");
  IElementType KW_CONTINUE = new BbkTokenType("continue");
  IElementType KW_CTL_OPT = new BbkTokenType("CTL-OPT");
  IElementType KW_DATE = new BbkTokenType("DATE");
  IElementType KW_DCL_C = new BbkTokenType("DCL-C");
  IElementType KW_DCL_DS = new BbkTokenType("DCL-DS");
  IElementType KW_DCL_F = new BbkTokenType("DCL-F");
  IElementType KW_DCL_PARM = new BbkTokenType("DCL-PARM");
  IElementType KW_DCL_PR = new BbkTokenType("DCL-PR");
  IElementType KW_DCL_PROC = new BbkTokenType("DCL-PROC");
  IElementType KW_DCL_S = new BbkTokenType("DCL-S");
  IElementType KW_DCL_SUBF = new BbkTokenType("DCL-SUBF");
  IElementType KW_DIM = new BbkTokenType("DIM");
  IElementType KW_DISK = new BbkTokenType("DISK");
  IElementType KW_DO = new BbkTokenType("do");
  IElementType KW_ELSE = new BbkTokenType("else");
  IElementType KW_EXPORT = new BbkTokenType("EXPORT");
  IElementType KW_EXTFILE = new BbkTokenType("EXTFILE");
  IElementType KW_EXTNAME = new BbkTokenType("EXTNAME");
  IElementType KW_EXTPGM = new BbkTokenType("EXTPGM");
  IElementType KW_EXTPROC = new BbkTokenType("EXTPROC");
  IElementType KW_FALSE = new BbkTokenType("false");
  IElementType KW_FLOAT = new BbkTokenType("FLOAT");
  IElementType KW_FOR = new BbkTokenType("for");
  IElementType KW_IF = new BbkTokenType("if");
  IElementType KW_IMPORT = new BbkTokenType("IMPORT");
  IElementType KW_INDDS = new BbkTokenType("INDDS");
  IElementType KW_INFDS = new BbkTokenType("INFDS");
  IElementType KW_INT = new BbkTokenType("INT");
  IElementType KW_INZ = new BbkTokenType("INZ");
  IElementType KW_KEYED = new BbkTokenType("KEYED");
  IElementType KW_LIKE = new BbkTokenType("LIKE");
  IElementType KW_LIKEDS = new BbkTokenType("LIKEDS");
  IElementType KW_LIKEREC = new BbkTokenType("LIKEREC");
  IElementType KW_MONITOR = new BbkTokenType("monitor");
  IElementType KW_NULL = new BbkTokenType("null");
  IElementType KW_ON_ERROR = new BbkTokenType("on-error");
  IElementType KW_ON_EXIT = new BbkTokenType("on-exit");
  IElementType KW_OPDESC = new BbkTokenType("OPDESC");
  IElementType KW_OPTIONS = new BbkTokenType("OPTIONS");
  IElementType KW_OTHER = new BbkTokenType("other");
  IElementType KW_OVERLAY = new BbkTokenType("OVERLAY");
  IElementType KW_PACKED = new BbkTokenType("PACKED");
  IElementType KW_POINTER = new BbkTokenType("POINTER");
  IElementType KW_POS = new BbkTokenType("POS");
  IElementType KW_PREFIX = new BbkTokenType("PREFIX");
  IElementType KW_PRE_DEFINE = new BbkTokenType("PRE-DEFINE");
  IElementType KW_PRE_ELSE = new BbkTokenType("PRE-ELSE");
  IElementType KW_PRE_ELSEIF = new BbkTokenType("PRE-ELSEIF");
  IElementType KW_PRE_ENDIF = new BbkTokenType("PRE-ENDIF");
  IElementType KW_PRE_EOF = new BbkTokenType("PRE-EOF");
  IElementType KW_PRE_IF = new BbkTokenType("PRE-IF");
  IElementType KW_PRE_INCLUDE = new BbkTokenType("PRE-INCLUDE");
  IElementType KW_PRE_UNDEFINE = new BbkTokenType("PRE-UNDEFINE");
  IElementType KW_PRINTER = new BbkTokenType("PRINTER");
  IElementType KW_QUALIFIED = new BbkTokenType("QUALIFIED");
  IElementType KW_RENAME = new BbkTokenType("RENAME");
  IElementType KW_RETURN = new BbkTokenType("return");
  IElementType KW_RTNPARM = new BbkTokenType("RTNPARM");
  IElementType KW_SELECT = new BbkTokenType("select");
  IElementType KW_SEQ = new BbkTokenType("SEQ");
  IElementType KW_STATIC = new BbkTokenType("STATIC");
  IElementType KW_TEMPLATE = new BbkTokenType("TEMPLATE");
  IElementType KW_TIME = new BbkTokenType("TIME");
  IElementType KW_TIMESTAMP = new BbkTokenType("TIMESTAMP");
  IElementType KW_TRUE = new BbkTokenType("true");
  IElementType KW_UNS = new BbkTokenType("UNS");
  IElementType KW_USAGE = new BbkTokenType("USAGE");
  IElementType KW_USROPN = new BbkTokenType("USROPN");
  IElementType KW_VALUE = new BbkTokenType("VALUE");
  IElementType KW_VARCHAR = new BbkTokenType("VARCHAR");
  IElementType KW_VOID = new BbkTokenType("VOID");
  IElementType KW_WHEN = new BbkTokenType("when");
  IElementType KW_WHILE = new BbkTokenType("while");
  IElementType KW_WORKSTN = new BbkTokenType("WORKSTN");
  IElementType KW_ZONED = new BbkTokenType("ZONED");
  IElementType LBRACE = new BbkTokenType("{");
  IElementType LBRACKET = new BbkTokenType("[");
  IElementType LINE_COMMENT = new BbkTokenType("LINE_COMMENT");
  IElementType LPAREN = new BbkTokenType("(");
  IElementType LT = new BbkTokenType("<");
  IElementType LT_EQ = new BbkTokenType("<=");
  IElementType LT_LT = new BbkTokenType("<<");
  IElementType LT_LT_EQ = new BbkTokenType("<<=");
  IElementType MINUS = new BbkTokenType("-");
  IElementType MINUS_EQ = new BbkTokenType("-=");
  IElementType PERCENT = new BbkTokenType("%");
  IElementType PERCENT_EQ = new BbkTokenType("%=");
  IElementType PIPE = new BbkTokenType("|");
  IElementType PIPE_EQ = new BbkTokenType("|=");
  IElementType PIPE_PIPE = new BbkTokenType("||");
  IElementType PLUS = new BbkTokenType("+");
  IElementType PLUS_EQ = new BbkTokenType("+=");
  IElementType QUESTION = new BbkTokenType("?");
  IElementType RBRACE = new BbkTokenType("}");
  IElementType RBRACKET = new BbkTokenType("]");
  IElementType RPAREN = new BbkTokenType(")");
  IElementType SEMI = new BbkTokenType(";");
  IElementType SLASH = new BbkTokenType("/");
  IElementType SLASH_EQ = new BbkTokenType("/=");
  IElementType STAR = new BbkTokenType("*");
  IElementType STAR_EQ = new BbkTokenType("*=");
  IElementType STAR_IDENT = new BbkTokenType("STAR_IDENT");
  IElementType STAR_STAR = new BbkTokenType("**");
  IElementType STR_LIT = new BbkTokenType("STR_LIT");
  IElementType TILDE = new BbkTokenType("~");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == BASED_MODIFIER) {
        return new BbkBasedModifierImpl(node);
      }
      else if (type == CONSTANT_DECLARATION) {
        return new BbkConstantDeclarationImpl(node);
      }
      else if (type == CONSTANT_VALUE) {
        return new BbkConstantValueImpl(node);
      }
      else if (type == CONST_WRAPPER) {
        return new BbkConstWrapperImpl(node);
      }
      else if (type == DIM_MODIFIER) {
        return new BbkDimModifierImpl(node);
      }
      else if (type == EXPORT_MODIFIER) {
        return new BbkExportModifierImpl(node);
      }
      else if (type == INZ_MODIFIER) {
        return new BbkInzModifierImpl(node);
      }
      else if (type == LIKE_REFERENCE) {
        return new BbkLikeReferenceImpl(node);
      }
      else if (type == LITERAL) {
        return new BbkLiteralImpl(node);
      }
      else if (type == PRIMITIVE_TYPE) {
        return new BbkPrimitiveTypeImpl(node);
      }
      else if (type == PRIMITIVE_TYPE_SPEC) {
        return new BbkPrimitiveTypeSpecImpl(node);
      }
      else if (type == QUALIFIED_MODIFIER) {
        return new BbkQualifiedModifierImpl(node);
      }
      else if (type == STATIC_MODIFIER) {
        return new BbkStaticModifierImpl(node);
      }
      else if (type == TOP_LEVEL_ITEM) {
        return new BbkTopLevelItemImpl(node);
      }
      else if (type == TYPE_ARGS) {
        return new BbkTypeArgsImpl(node);
      }
      else if (type == TYPE_SPECIFICATION) {
        return new BbkTypeSpecificationImpl(node);
      }
      else if (type == VARIABLE_DECLARATION) {
        return new BbkVariableDeclarationImpl(node);
      }
      else if (type == VAR_MODIFIER) {
        return new BbkVarModifierImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
