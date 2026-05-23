// This is a generated file. Not intended for manual editing.
package com.larena.boxbreaker.plugin.bbk.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.larena.boxbreaker.plugin.bbk.psi.BbkTypes.*;
import static com.larena.boxbreaker.plugin.bbk.parser.BbkParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class BbkParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parseLight(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parseLight(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, null);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    result_ = parse_root_(root_, builder_);
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_) {
    return parse_root_(root_, builder_, 0);
  }

  static boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return translation_unit(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // KW_BASED LPAREN IDENT RPAREN
  public static boolean based_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "based_modifier")) return false;
    if (!nextTokenIs(builder_, KW_BASED)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BASED_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_BASED, LPAREN, IDENT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_CONST LPAREN (literal | IDENT) RPAREN
  public static boolean const_wrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "const_wrapper")) return false;
    if (!nextTokenIs(builder_, KW_CONST)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CONST_WRAPPER, null);
    result_ = consumeTokens(builder_, 1, KW_CONST, LPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, const_wrapper_2(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // literal | IDENT
  private static boolean const_wrapper_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "const_wrapper_2")) return false;
    boolean result_;
    result_ = literal(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, IDENT);
    return result_;
  }

  /* ********************************************************** */
  // KW_DCL_C IDENT constant_value SEMI
  public static boolean constant_declaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "constant_declaration")) return false;
    if (!nextTokenIs(builder_, KW_DCL_C)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CONSTANT_DECLARATION, null);
    result_ = consumeTokens(builder_, 1, KW_DCL_C, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, constant_value(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // const_wrapper | literal | KW_TRUE | KW_FALSE | KW_NULL
  public static boolean constant_value(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "constant_value")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CONSTANT_VALUE, "<constant value>");
    result_ = const_wrapper(builder_, level_ + 1);
    if (!result_) result_ = literal(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KW_TRUE);
    if (!result_) result_ = consumeToken(builder_, KW_FALSE);
    if (!result_) result_ = consumeToken(builder_, KW_NULL);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_DIM LPAREN INT_LIT (COLON INT_LIT)? RPAREN
  public static boolean dim_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dim_modifier")) return false;
    if (!nextTokenIs(builder_, KW_DIM)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DIM_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_DIM, LPAREN, INT_LIT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, dim_modifier_3(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (COLON INT_LIT)?
  private static boolean dim_modifier_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dim_modifier_3")) return false;
    dim_modifier_3_0(builder_, level_ + 1);
    return true;
  }

  // COLON INT_LIT
  private static boolean dim_modifier_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dim_modifier_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, COLON, INT_LIT);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_EXPORT
  public static boolean export_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "export_modifier")) return false;
    if (!nextTokenIs(builder_, KW_EXPORT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KW_EXPORT);
    exit_section_(builder_, marker_, EXPORT_MODIFIER, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_INZ LPAREN modifier_value RPAREN
  public static boolean inz_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inz_modifier")) return false;
    if (!nextTokenIs(builder_, KW_INZ)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INZ_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_INZ, LPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, modifier_value(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // (KW_LIKE | KW_LIKEDS | KW_LIKEREC) LPAREN IDENT RPAREN
  public static boolean like_reference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "like_reference")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LIKE_REFERENCE, "<like reference>");
    result_ = like_reference_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeTokens(builder_, -1, LPAREN, IDENT, RPAREN));
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // KW_LIKE | KW_LIKEDS | KW_LIKEREC
  private static boolean like_reference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "like_reference_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, KW_LIKE);
    if (!result_) result_ = consumeToken(builder_, KW_LIKEDS);
    if (!result_) result_ = consumeToken(builder_, KW_LIKEREC);
    return result_;
  }

  /* ********************************************************** */
  // INT_LIT | INT_LIT_HEX | INT_LIT_OCT | FLOAT_LIT | DEC_LIT | STR_LIT
  public static boolean literal(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "literal")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LITERAL, "<literal>");
    result_ = consumeToken(builder_, INT_LIT);
    if (!result_) result_ = consumeToken(builder_, INT_LIT_HEX);
    if (!result_) result_ = consumeToken(builder_, INT_LIT_OCT);
    if (!result_) result_ = consumeToken(builder_, FLOAT_LIT);
    if (!result_) result_ = consumeToken(builder_, DEC_LIT);
    if (!result_) result_ = consumeToken(builder_, STR_LIT);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // literal | KW_TRUE | KW_FALSE | KW_NULL | STAR_IDENT | IDENT
  static boolean modifier_value(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifier_value")) return false;
    boolean result_;
    result_ = literal(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KW_TRUE);
    if (!result_) result_ = consumeToken(builder_, KW_FALSE);
    if (!result_) result_ = consumeToken(builder_, KW_NULL);
    if (!result_) result_ = consumeToken(builder_, STAR_IDENT);
    if (!result_) result_ = consumeToken(builder_, IDENT);
    return result_;
  }

  /* ********************************************************** */
  // KW_CHAR | KW_VARCHAR
  //                  | KW_PACKED | KW_ZONED | KW_BINDEC
  //                  | KW_INT | KW_UNS | KW_FLOAT
  //                  | KW_DATE | KW_TIME | KW_TIMESTAMP
  //                  | KW_BOOL | KW_POINTER | KW_VOID
  public static boolean primitive_type(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primitive_type")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PRIMITIVE_TYPE, "<primitive type>");
    result_ = consumeToken(builder_, KW_CHAR);
    if (!result_) result_ = consumeToken(builder_, KW_VARCHAR);
    if (!result_) result_ = consumeToken(builder_, KW_PACKED);
    if (!result_) result_ = consumeToken(builder_, KW_ZONED);
    if (!result_) result_ = consumeToken(builder_, KW_BINDEC);
    if (!result_) result_ = consumeToken(builder_, KW_INT);
    if (!result_) result_ = consumeToken(builder_, KW_UNS);
    if (!result_) result_ = consumeToken(builder_, KW_FLOAT);
    if (!result_) result_ = consumeToken(builder_, KW_DATE);
    if (!result_) result_ = consumeToken(builder_, KW_TIME);
    if (!result_) result_ = consumeToken(builder_, KW_TIMESTAMP);
    if (!result_) result_ = consumeToken(builder_, KW_BOOL);
    if (!result_) result_ = consumeToken(builder_, KW_POINTER);
    if (!result_) result_ = consumeToken(builder_, KW_VOID);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // primitive_type type_args?
  public static boolean primitive_type_spec(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primitive_type_spec")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PRIMITIVE_TYPE_SPEC, "<primitive type spec>");
    result_ = primitive_type(builder_, level_ + 1);
    result_ = result_ && primitive_type_spec_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // type_args?
  private static boolean primitive_type_spec_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primitive_type_spec_1")) return false;
    type_args(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // KW_QUALIFIED
  public static boolean qualified_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualified_modifier")) return false;
    if (!nextTokenIs(builder_, KW_QUALIFIED)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KW_QUALIFIED);
    exit_section_(builder_, marker_, QUALIFIED_MODIFIER, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_STATIC
  public static boolean static_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "static_modifier")) return false;
    if (!nextTokenIs(builder_, KW_STATIC)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KW_STATIC);
    exit_section_(builder_, marker_, STATIC_MODIFIER, result_);
    return result_;
  }

  /* ********************************************************** */
  // variable_declaration
  //                  | constant_declaration
  //                  | unknown_item
  public static boolean top_level_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_item")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, TOP_LEVEL_ITEM, "<top level item>");
    result_ = variable_declaration(builder_, level_ + 1);
    if (!result_) result_ = constant_declaration(builder_, level_ + 1);
    if (!result_) result_ = unknown_item(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // top_level_item*
  static boolean translation_unit(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "translation_unit")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!top_level_item(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "translation_unit", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // LPAREN INT_LIT type_args_tail? RPAREN
  public static boolean type_args(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type_args")) return false;
    if (!nextTokenIs(builder_, LPAREN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, TYPE_ARGS, null);
    result_ = consumeTokens(builder_, 1, LPAREN, INT_LIT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, type_args_2(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // type_args_tail?
  private static boolean type_args_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type_args_2")) return false;
    type_args_tail(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // COLON INT_LIT
  static boolean type_args_tail(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type_args_tail")) return false;
    if (!nextTokenIs(builder_, COLON)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = consumeTokens(builder_, 1, COLON, INT_LIT);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // like_reference | primitive_type_spec
  public static boolean type_specification(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type_specification")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, TYPE_SPECIFICATION, "<type specification>");
    result_ = like_reference(builder_, level_ + 1);
    if (!result_) result_ = primitive_type_spec(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // !<<eof>> any_token
  static boolean unknown_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unknown_item")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = unknown_item_0(builder_, level_ + 1);
    result_ = result_ && consumeAnyToken(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !<<eof>>
  private static boolean unknown_item_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unknown_item_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !eof(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // inz_modifier
  //                | static_modifier
  //                | export_modifier
  //                | dim_modifier
  //                | based_modifier
  //                | qualified_modifier
  public static boolean var_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "var_modifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, VAR_MODIFIER, "<var modifier>");
    result_ = inz_modifier(builder_, level_ + 1);
    if (!result_) result_ = static_modifier(builder_, level_ + 1);
    if (!result_) result_ = export_modifier(builder_, level_ + 1);
    if (!result_) result_ = dim_modifier(builder_, level_ + 1);
    if (!result_) result_ = based_modifier(builder_, level_ + 1);
    if (!result_) result_ = qualified_modifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_DCL_S IDENT type_specification var_modifier* SEMI
  public static boolean variable_declaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variable_declaration")) return false;
    if (!nextTokenIs(builder_, KW_DCL_S)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, VARIABLE_DECLARATION, null);
    result_ = consumeTokens(builder_, 1, KW_DCL_S, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, type_specification(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, variable_declaration_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // var_modifier*
  private static boolean variable_declaration_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variable_declaration_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!var_modifier(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "variable_declaration_3", pos_)) break;
    }
    return true;
  }

}
