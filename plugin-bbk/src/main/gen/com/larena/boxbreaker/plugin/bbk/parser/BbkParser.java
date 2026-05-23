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
  // multiplicative_expression ((PLUS | MINUS) multiplicative_expression)*
  public static boolean additive_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additive_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ADDITIVE_EXPRESSION, "<additive expression>");
    result_ = multiplicative_expression(builder_, level_ + 1);
    result_ = result_ && additive_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // ((PLUS | MINUS) multiplicative_expression)*
  private static boolean additive_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additive_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!additive_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "additive_expression_1", pos_)) break;
    }
    return true;
  }

  // (PLUS | MINUS) multiplicative_expression
  private static boolean additive_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additive_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = additive_expression_1_0_0(builder_, level_ + 1);
    result_ = result_ && multiplicative_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // PLUS | MINUS
  private static boolean additive_expression_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additive_expression_1_0_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, PLUS);
    if (!result_) result_ = consumeToken(builder_, MINUS);
    return result_;
  }

  /* ********************************************************** */
  // KW_ALIGN
  public static boolean align_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "align_modifier")) return false;
    if (!nextTokenIs(builder_, KW_ALIGN)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KW_ALIGN);
    exit_section_(builder_, marker_, ALIGN_MODIFIER, result_);
    return result_;
  }

  /* ********************************************************** */
  // expression (COMMA expression)*
  public static boolean argument_list(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argument_list")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ARGUMENT_LIST, "<argument list>");
    result_ = expression(builder_, level_ + 1);
    result_ = result_ && argument_list_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (COMMA expression)*
  private static boolean argument_list_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argument_list_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!argument_list_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "argument_list_1", pos_)) break;
    }
    return true;
  }

  // COMMA expression
  private static boolean argument_list_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argument_list_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // EQ | PLUS_EQ | MINUS_EQ | STAR_EQ | SLASH_EQ | PERCENT_EQ
  //                | AMP_EQ | PIPE_EQ | CARET_EQ | LT_LT_EQ | GT_GT_EQ
  public static boolean assignment_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignment_op")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ASSIGNMENT_OP, "<assignment op>");
    result_ = consumeToken(builder_, EQ);
    if (!result_) result_ = consumeToken(builder_, PLUS_EQ);
    if (!result_) result_ = consumeToken(builder_, MINUS_EQ);
    if (!result_) result_ = consumeToken(builder_, STAR_EQ);
    if (!result_) result_ = consumeToken(builder_, SLASH_EQ);
    if (!result_) result_ = consumeToken(builder_, PERCENT_EQ);
    if (!result_) result_ = consumeToken(builder_, AMP_EQ);
    if (!result_) result_ = consumeToken(builder_, PIPE_EQ);
    if (!result_) result_ = consumeToken(builder_, CARET_EQ);
    if (!result_) result_ = consumeToken(builder_, LT_LT_EQ);
    if (!result_) result_ = consumeToken(builder_, GT_GT_EQ);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // assignment_op expression attribute_modifier?
  static boolean assignment_tail(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignment_tail")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = assignment_op(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && assignment_tail_2(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // attribute_modifier?
  private static boolean assignment_tail_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignment_tail_2")) return false;
    attribute_modifier(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // ATTR_HALFUP | ATTR_HALFDOWN | ATTR_TRUNC
  public static boolean attribute_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "attribute_modifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ATTRIBUTE_MODIFIER, "<attribute modifier>");
    result_ = consumeToken(builder_, ATTR_HALFUP);
    if (!result_) result_ = consumeToken(builder_, ATTR_HALFDOWN);
    if (!result_) result_ = consumeToken(builder_, ATTR_TRUNC);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
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
  // equality_expression (AMP equality_expression)*
  public static boolean bitwise_and_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwise_and_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BITWISE_AND_EXPRESSION, "<bitwise and expression>");
    result_ = equality_expression(builder_, level_ + 1);
    result_ = result_ && bitwise_and_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (AMP equality_expression)*
  private static boolean bitwise_and_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwise_and_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!bitwise_and_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "bitwise_and_expression_1", pos_)) break;
    }
    return true;
  }

  // AMP equality_expression
  private static boolean bitwise_and_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwise_and_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, AMP);
    result_ = result_ && equality_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // bitwise_xor_expression (PIPE bitwise_xor_expression)*
  public static boolean bitwise_or_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwise_or_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BITWISE_OR_EXPRESSION, "<bitwise or expression>");
    result_ = bitwise_xor_expression(builder_, level_ + 1);
    result_ = result_ && bitwise_or_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (PIPE bitwise_xor_expression)*
  private static boolean bitwise_or_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwise_or_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!bitwise_or_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "bitwise_or_expression_1", pos_)) break;
    }
    return true;
  }

  // PIPE bitwise_xor_expression
  private static boolean bitwise_or_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwise_or_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PIPE);
    result_ = result_ && bitwise_xor_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // bitwise_and_expression (CARET bitwise_and_expression)*
  public static boolean bitwise_xor_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwise_xor_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BITWISE_XOR_EXPRESSION, "<bitwise xor expression>");
    result_ = bitwise_and_expression(builder_, level_ + 1);
    result_ = result_ && bitwise_xor_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (CARET bitwise_and_expression)*
  private static boolean bitwise_xor_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwise_xor_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!bitwise_xor_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "bitwise_xor_expression_1", pos_)) break;
    }
    return true;
  }

  // CARET bitwise_and_expression
  private static boolean bitwise_xor_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwise_xor_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, CARET);
    result_ = result_ && bitwise_and_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // variable_declaration
  //              | constant_declaration
  //              | data_structure_declaration
  //              | subroutine_definition
  //              | directive
  //              | statement
  //              | unknown_block_item
  public static boolean block_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block_item")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BLOCK_ITEM, "<block item>");
    result_ = variable_declaration(builder_, level_ + 1);
    if (!result_) result_ = constant_declaration(builder_, level_ + 1);
    if (!result_) result_ = data_structure_declaration(builder_, level_ + 1);
    if (!result_) result_ = subroutine_definition(builder_, level_ + 1);
    if (!result_) result_ = directive(builder_, level_ + 1);
    if (!result_) result_ = statement(builder_, level_ + 1);
    if (!result_) result_ = unknown_block_item(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // LBRACE block_item* RBRACE
  public static boolean block_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block_statement")) return false;
    if (!nextTokenIs(builder_, LBRACE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BLOCK_STATEMENT, null);
    result_ = consumeToken(builder_, LBRACE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, block_statement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RBRACE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // block_item*
  private static boolean block_statement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block_statement_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!block_item(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "block_statement_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // KW_BREAK SEMI
  public static boolean break_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "break_statement")) return false;
    if (!nextTokenIs(builder_, KW_BREAK)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BREAK_STATEMENT, null);
    result_ = consumeTokens(builder_, 1, KW_BREAK, SEMI);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_CALLP postfix_expression SEMI
  public static boolean callp_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callp_statement")) return false;
    if (!nextTokenIs(builder_, KW_CALLP)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CALLP_STATEMENT, null);
    result_ = consumeToken(builder_, KW_CALLP);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, postfix_expression(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_CHAIN   expression IDENT IDENT? SEMI
  public static boolean chain_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "chain_op")) return false;
    if (!nextTokenIs(builder_, KW_CHAIN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CHAIN_OP, null);
    result_ = consumeToken(builder_, KW_CHAIN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, IDENT)) && result_;
    result_ = pinned_ && report_error_(builder_, chain_op_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // IDENT?
  private static boolean chain_op_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "chain_op_3")) return false;
    consumeToken(builder_, IDENT);
    return true;
  }

  /* ********************************************************** */
  // KW_CLOSE   IDENT SEMI
  public static boolean close_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "close_op")) return false;
    if (!nextTokenIs(builder_, KW_CLOSE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CLOSE_OP, null);
    result_ = consumeTokens(builder_, 1, KW_CLOSE, IDENT, SEMI);
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
  // KW_CONTINUE SEMI
  public static boolean continue_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "continue_statement")) return false;
    if (!nextTokenIs(builder_, KW_CONTINUE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CONTINUE_STATEMENT, null);
    result_ = consumeTokens(builder_, 1, KW_CONTINUE, SEMI);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // literal | KW_TRUE | KW_FALSE | KW_NULL | STAR_IDENT | IDENT
  static boolean ctl_opt_arg(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ctl_opt_arg")) return false;
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
  // LPAREN ctl_opt_arg (COLON ctl_opt_arg)* RPAREN
  public static boolean ctl_opt_args(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ctl_opt_args")) return false;
    if (!nextTokenIs(builder_, LPAREN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CTL_OPT_ARGS, null);
    result_ = consumeToken(builder_, LPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, ctl_opt_arg(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, ctl_opt_args_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (COLON ctl_opt_arg)*
  private static boolean ctl_opt_args_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ctl_opt_args_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!ctl_opt_args_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ctl_opt_args_2", pos_)) break;
    }
    return true;
  }

  // COLON ctl_opt_arg
  private static boolean ctl_opt_args_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ctl_opt_args_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COLON);
    result_ = result_ && ctl_opt_arg(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // IDENT ctl_opt_args?
  public static boolean ctl_opt_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ctl_opt_keyword")) return false;
    if (!nextTokenIs(builder_, IDENT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IDENT);
    result_ = result_ && ctl_opt_keyword_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, CTL_OPT_KEYWORD, result_);
    return result_;
  }

  // ctl_opt_args?
  private static boolean ctl_opt_keyword_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ctl_opt_keyword_1")) return false;
    ctl_opt_args(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // KW_CTL_OPT ctl_opt_keyword* SEMI
  public static boolean ctl_opt_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ctl_opt_statement")) return false;
    if (!nextTokenIs(builder_, KW_CTL_OPT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CTL_OPT_STATEMENT, null);
    result_ = consumeToken(builder_, KW_CTL_OPT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, ctl_opt_statement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ctl_opt_keyword*
  private static boolean ctl_opt_statement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ctl_opt_statement_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!ctl_opt_keyword(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ctl_opt_statement_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // KW_DCL_DS IDENT ds_modifier* ds_tail
  public static boolean data_structure_declaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "data_structure_declaration")) return false;
    if (!nextTokenIs(builder_, KW_DCL_DS)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DATA_STRUCTURE_DECLARATION, null);
    result_ = consumeTokens(builder_, 1, KW_DCL_DS, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, data_structure_declaration_2(builder_, level_ + 1));
    result_ = pinned_ && ds_tail(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ds_modifier*
  private static boolean data_structure_declaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "data_structure_declaration_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!ds_modifier(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "data_structure_declaration_2", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // KW_DELETE  expression? IDENT SEMI
  public static boolean delete_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "delete_op")) return false;
    if (!nextTokenIs(builder_, KW_DELETE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DELETE_OP, null);
    result_ = consumeToken(builder_, KW_DELETE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, delete_op_1(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeTokens(builder_, -1, IDENT, SEMI)) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // expression?
  private static boolean delete_op_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "delete_op_1")) return false;
    expression(builder_, level_ + 1);
    return true;
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
  // pre_if_directive
  //             | pre_elseif_directive
  //             | pre_else_directive
  //             | pre_endif_directive
  //             | pre_define_directive
  //             | pre_undefine_directive
  //             | pre_include_directive
  //             | pre_eof_directive
  public static boolean directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "directive")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DIRECTIVE, "<directive>");
    result_ = pre_if_directive(builder_, level_ + 1);
    if (!result_) result_ = pre_elseif_directive(builder_, level_ + 1);
    if (!result_) result_ = pre_else_directive(builder_, level_ + 1);
    if (!result_) result_ = pre_endif_directive(builder_, level_ + 1);
    if (!result_) result_ = pre_define_directive(builder_, level_ + 1);
    if (!result_) result_ = pre_undefine_directive(builder_, level_ + 1);
    if (!result_) result_ = pre_include_directive(builder_, level_ + 1);
    if (!result_) result_ = pre_eof_directive(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_DO block_statement KW_WHILE LPAREN expression RPAREN SEMI
  public static boolean do_while_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "do_while_statement")) return false;
    if (!nextTokenIs(builder_, KW_DO)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DO_WHILE_STATEMENT, null);
    result_ = consumeToken(builder_, KW_DO);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, block_statement(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeTokens(builder_, -1, KW_WHILE, LPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeTokens(builder_, -1, RPAREN, SEMI)) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // LBRACE ds_subfield* RBRACE
  public static boolean ds_body(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ds_body")) return false;
    if (!nextTokenIs(builder_, LBRACE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DS_BODY, null);
    result_ = consumeToken(builder_, LBRACE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, ds_body_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RBRACE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ds_subfield*
  private static boolean ds_body_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ds_body_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!ds_subfield(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ds_body_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // qualified_modifier
  //               | template_modifier
  //               | align_modifier
  //               | dim_modifier
  //               | based_modifier
  //               | inz_modifier
  //               | extname_ds_modifier
  //               | likeds_ds_modifier
  //               | likerec_ds_modifier
  //               | infds_ds_modifier
  public static boolean ds_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ds_modifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DS_MODIFIER, "<ds modifier>");
    result_ = qualified_modifier(builder_, level_ + 1);
    if (!result_) result_ = template_modifier(builder_, level_ + 1);
    if (!result_) result_ = align_modifier(builder_, level_ + 1);
    if (!result_) result_ = dim_modifier(builder_, level_ + 1);
    if (!result_) result_ = based_modifier(builder_, level_ + 1);
    if (!result_) result_ = inz_modifier(builder_, level_ + 1);
    if (!result_) result_ = extname_ds_modifier(builder_, level_ + 1);
    if (!result_) result_ = likeds_ds_modifier(builder_, level_ + 1);
    if (!result_) result_ = likerec_ds_modifier(builder_, level_ + 1);
    if (!result_) result_ = infds_ds_modifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // IDENT type_specification var_modifier* SEMI
  public static boolean ds_subfield(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ds_subfield")) return false;
    if (!nextTokenIs(builder_, IDENT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DS_SUBFIELD, null);
    result_ = consumeToken(builder_, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, type_specification(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, ds_subfield_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // var_modifier*
  private static boolean ds_subfield_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ds_subfield_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!var_modifier(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ds_subfield_2", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // ds_body | SEMI
  static boolean ds_tail(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ds_tail")) return false;
    if (!nextTokenIs(builder_, "", LBRACE, SEMI)) return false;
    boolean result_;
    result_ = ds_body(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, SEMI);
    return result_;
  }

  /* ********************************************************** */
  // relational_expression ((EQ_EQ | BANG_EQ) relational_expression)*
  public static boolean equality_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equality_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EQUALITY_EXPRESSION, "<equality expression>");
    result_ = relational_expression(builder_, level_ + 1);
    result_ = result_ && equality_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // ((EQ_EQ | BANG_EQ) relational_expression)*
  private static boolean equality_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equality_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!equality_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "equality_expression_1", pos_)) break;
    }
    return true;
  }

  // (EQ_EQ | BANG_EQ) relational_expression
  private static boolean equality_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equality_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = equality_expression_1_0_0(builder_, level_ + 1);
    result_ = result_ && relational_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // EQ_EQ | BANG_EQ
  private static boolean equality_expression_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equality_expression_1_0_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, EQ_EQ);
    if (!result_) result_ = consumeToken(builder_, BANG_EQ);
    return result_;
  }

  /* ********************************************************** */
  // KW_EXFMT   IDENT IDENT? SEMI
  public static boolean exfmt_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "exfmt_op")) return false;
    if (!nextTokenIs(builder_, KW_EXFMT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXFMT_OP, null);
    result_ = consumeTokens(builder_, 1, KW_EXFMT, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, exfmt_op_2(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // IDENT?
  private static boolean exfmt_op_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "exfmt_op_2")) return false;
    consumeToken(builder_, IDENT);
    return true;
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
  // ternary_expression
  public static boolean expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXPRESSION, "<expression>");
    result_ = ternary_expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // expression assignment_tail? SEMI
  public static boolean expression_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_statement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXPRESSION_STATEMENT, "<expression statement>");
    result_ = expression(builder_, level_ + 1);
    result_ = result_ && expression_statement_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, SEMI);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // assignment_tail?
  private static boolean expression_statement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_statement_1")) return false;
    assignment_tail(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // KW_EXSR IDENT SEMI
  public static boolean exsr_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "exsr_statement")) return false;
    if (!nextTokenIs(builder_, KW_EXSR)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXSR_STATEMENT, null);
    result_ = consumeTokens(builder_, 1, KW_EXSR, IDENT, SEMI);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_EXTFILE LPAREN STR_LIT RPAREN
  public static boolean extfile_f_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extfile_f_keyword")) return false;
    if (!nextTokenIs(builder_, KW_EXTFILE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXTFILE_F_KEYWORD, null);
    result_ = consumeTokens(builder_, 1, KW_EXTFILE, LPAREN, STR_LIT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_EXTNAME LPAREN STR_LIT RPAREN
  public static boolean extname_ds_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extname_ds_modifier")) return false;
    if (!nextTokenIs(builder_, KW_EXTNAME)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXTNAME_DS_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_EXTNAME, LPAREN, STR_LIT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_EXTNAME LPAREN STR_LIT RPAREN
  public static boolean extname_f_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extname_f_keyword")) return false;
    if (!nextTokenIs(builder_, KW_EXTNAME)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXTNAME_F_KEYWORD, null);
    result_ = consumeTokens(builder_, 1, KW_EXTNAME, LPAREN, STR_LIT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_EXTPGM  LPAREN STR_LIT RPAREN
  public static boolean extpgm_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extpgm_modifier")) return false;
    if (!nextTokenIs(builder_, KW_EXTPGM)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXTPGM_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_EXTPGM, LPAREN, STR_LIT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_EXTPROC LPAREN STR_LIT RPAREN
  public static boolean extproc_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extproc_modifier")) return false;
    if (!nextTokenIs(builder_, KW_EXTPROC)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXTPROC_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_EXTPROC, LPAREN, STR_LIT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // simple_f_keyword
  //             | usage_f_keyword
  //             | extname_f_keyword
  //             | extfile_f_keyword
  //             | prefix_f_keyword
  //             | rename_f_keyword
  //             | indds_f_keyword
  //             | infds_f_keyword
  public static boolean f_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "f_keyword")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, F_KEYWORD, "<f keyword>");
    result_ = simple_f_keyword(builder_, level_ + 1);
    if (!result_) result_ = usage_f_keyword(builder_, level_ + 1);
    if (!result_) result_ = extname_f_keyword(builder_, level_ + 1);
    if (!result_) result_ = extfile_f_keyword(builder_, level_ + 1);
    if (!result_) result_ = prefix_f_keyword(builder_, level_ + 1);
    if (!result_) result_ = rename_f_keyword(builder_, level_ + 1);
    if (!result_) result_ = indds_f_keyword(builder_, level_ + 1);
    if (!result_) result_ = infds_f_keyword(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_DCL_F IDENT f_keyword+ SEMI
  public static boolean file_declaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "file_declaration")) return false;
    if (!nextTokenIs(builder_, KW_DCL_F)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, FILE_DECLARATION, null);
    result_ = consumeTokens(builder_, 1, KW_DCL_F, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, file_declaration_2(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // f_keyword+
  private static boolean file_declaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "file_declaration_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = f_keyword(builder_, level_ + 1);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!f_keyword(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "file_declaration_2", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // read_op
  //                     | reade_op
  //                     | readp_op
  //                     | readpe_op
  //                     | chain_op
  //                     | setll_op
  //                     | setgt_op
  //                     | write_op
  //                     | update_op
  //                     | delete_op
  //                     | unlock_op
  //                     | open_op
  //                     | close_op
  //                     | exfmt_op
  public static boolean file_op_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "file_op_statement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, FILE_OP_STATEMENT, "<file op statement>");
    result_ = read_op(builder_, level_ + 1);
    if (!result_) result_ = reade_op(builder_, level_ + 1);
    if (!result_) result_ = readp_op(builder_, level_ + 1);
    if (!result_) result_ = readpe_op(builder_, level_ + 1);
    if (!result_) result_ = chain_op(builder_, level_ + 1);
    if (!result_) result_ = setll_op(builder_, level_ + 1);
    if (!result_) result_ = setgt_op(builder_, level_ + 1);
    if (!result_) result_ = write_op(builder_, level_ + 1);
    if (!result_) result_ = update_op(builder_, level_ + 1);
    if (!result_) result_ = delete_op(builder_, level_ + 1);
    if (!result_) result_ = unlock_op(builder_, level_ + 1);
    if (!result_) result_ = open_op(builder_, level_ + 1);
    if (!result_) result_ = close_op(builder_, level_ + 1);
    if (!result_) result_ = exfmt_op(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // lvalue assignment_op expression
  public static boolean for_assignment(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "for_assignment")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, FOR_ASSIGNMENT, "<for assignment>");
    result_ = lvalue(builder_, level_ + 1);
    result_ = result_ && assignment_op(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // for_inline_decl | for_assignment | expression
  public static boolean for_init(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "for_init")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, FOR_INIT, "<for init>");
    result_ = for_inline_decl(builder_, level_ + 1);
    if (!result_) result_ = for_assignment(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_DCL_S IDENT type_specification EQ expression
  public static boolean for_inline_decl(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "for_inline_decl")) return false;
    if (!nextTokenIs(builder_, KW_DCL_S)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, FOR_INLINE_DECL, null);
    result_ = consumeTokens(builder_, 1, KW_DCL_S, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, type_specification(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, EQ)) && result_;
    result_ = pinned_ && expression(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_FOR LPAREN for_init? SEMI expression? SEMI for_update? RPAREN block_statement
  public static boolean for_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "for_statement")) return false;
    if (!nextTokenIs(builder_, KW_FOR)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, FOR_STATEMENT, null);
    result_ = consumeTokens(builder_, 1, KW_FOR, LPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, for_statement_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, SEMI)) && result_;
    result_ = pinned_ && report_error_(builder_, for_statement_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, SEMI)) && result_;
    result_ = pinned_ && report_error_(builder_, for_statement_6(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, RPAREN)) && result_;
    result_ = pinned_ && block_statement(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // for_init?
  private static boolean for_statement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "for_statement_2")) return false;
    for_init(builder_, level_ + 1);
    return true;
  }

  // expression?
  private static boolean for_statement_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "for_statement_4")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  // for_update?
  private static boolean for_statement_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "for_statement_6")) return false;
    for_update(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // for_assignment | expression
  public static boolean for_update(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "for_update")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, FOR_UPDATE, "<for update>");
    result_ = for_assignment(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_IF LPAREN expression RPAREN block_statement
  //                  (KW_ELSE (if_statement | block_statement))?
  public static boolean if_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "if_statement")) return false;
    if (!nextTokenIs(builder_, KW_IF)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, IF_STATEMENT, null);
    result_ = consumeTokens(builder_, 1, KW_IF, LPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, RPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, block_statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && if_statement_5(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (KW_ELSE (if_statement | block_statement))?
  private static boolean if_statement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "if_statement_5")) return false;
    if_statement_5_0(builder_, level_ + 1);
    return true;
  }

  // KW_ELSE (if_statement | block_statement)
  private static boolean if_statement_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "if_statement_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KW_ELSE);
    result_ = result_ && if_statement_5_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // if_statement | block_statement
  private static boolean if_statement_5_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "if_statement_5_0_1")) return false;
    boolean result_;
    result_ = if_statement(builder_, level_ + 1);
    if (!result_) result_ = block_statement(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // KW_INDDS   LPAREN IDENT RPAREN
  public static boolean indds_f_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indds_f_keyword")) return false;
    if (!nextTokenIs(builder_, KW_INDDS)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INDDS_F_KEYWORD, null);
    result_ = consumeTokens(builder_, 1, KW_INDDS, LPAREN, IDENT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_INFDS   LPAREN IDENT RPAREN
  public static boolean infds_ds_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "infds_ds_modifier")) return false;
    if (!nextTokenIs(builder_, KW_INFDS)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INFDS_DS_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_INFDS, LPAREN, IDENT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_INFDS   LPAREN IDENT RPAREN
  public static boolean infds_f_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "infds_f_keyword")) return false;
    if (!nextTokenIs(builder_, KW_INFDS)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INFDS_F_KEYWORD, null);
    result_ = consumeTokens(builder_, 1, KW_INFDS, LPAREN, IDENT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // IDENT type_specification param_modifier*
  public static boolean inline_param(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inline_param")) return false;
    if (!nextTokenIs(builder_, IDENT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INLINE_PARAM, null);
    result_ = consumeToken(builder_, IDENT);
    result_ = result_ && type_specification(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && inline_param_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // param_modifier*
  private static boolean inline_param_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inline_param_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!param_modifier(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "inline_param_2", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // LPAREN inline_params? RPAREN
  public static boolean inline_param_list(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inline_param_list")) return false;
    if (!nextTokenIs(builder_, LPAREN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INLINE_PARAM_LIST, null);
    result_ = consumeToken(builder_, LPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, inline_param_list_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // inline_params?
  private static boolean inline_param_list_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inline_param_list_1")) return false;
    inline_params(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // inline_param (COMMA inline_param)*
  static boolean inline_params(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inline_params")) return false;
    if (!nextTokenIs(builder_, IDENT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = inline_param(builder_, level_ + 1);
    result_ = result_ && inline_params_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA inline_param)*
  private static boolean inline_params_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inline_params_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!inline_params_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "inline_params_1", pos_)) break;
    }
    return true;
  }

  // COMMA inline_param
  private static boolean inline_params_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inline_params_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && inline_param(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
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
  // KW_LEAVESR SEMI
  public static boolean leavesr_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "leavesr_statement")) return false;
    if (!nextTokenIs(builder_, KW_LEAVESR)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LEAVESR_STATEMENT, null);
    result_ = consumeTokens(builder_, 1, KW_LEAVESR, SEMI);
    pinned_ = result_; // pin = 1
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
  // KW_LIKEDS  LPAREN IDENT RPAREN
  public static boolean likeds_ds_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "likeds_ds_modifier")) return false;
    if (!nextTokenIs(builder_, KW_LIKEDS)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LIKEDS_DS_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_LIKEDS, LPAREN, IDENT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_LIKEREC LPAREN IDENT (COLON IDENT)? RPAREN
  public static boolean likerec_ds_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "likerec_ds_modifier")) return false;
    if (!nextTokenIs(builder_, KW_LIKEREC)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LIKEREC_DS_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_LIKEREC, LPAREN, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, likerec_ds_modifier_3(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (COLON IDENT)?
  private static boolean likerec_ds_modifier_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "likerec_ds_modifier_3")) return false;
    likerec_ds_modifier_3_0(builder_, level_ + 1);
    return true;
  }

  // COLON IDENT
  private static boolean likerec_ds_modifier_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "likerec_ds_modifier_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, COLON, IDENT);
    exit_section_(builder_, marker_, null, result_);
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
  // bitwise_or_expression (AMP_AMP bitwise_or_expression)*
  public static boolean logical_and_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logical_and_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LOGICAL_AND_EXPRESSION, "<logical and expression>");
    result_ = bitwise_or_expression(builder_, level_ + 1);
    result_ = result_ && logical_and_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (AMP_AMP bitwise_or_expression)*
  private static boolean logical_and_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logical_and_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!logical_and_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "logical_and_expression_1", pos_)) break;
    }
    return true;
  }

  // AMP_AMP bitwise_or_expression
  private static boolean logical_and_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logical_and_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, AMP_AMP);
    result_ = result_ && bitwise_or_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // logical_and_expression (PIPE_PIPE logical_and_expression)*
  public static boolean logical_or_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logical_or_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LOGICAL_OR_EXPRESSION, "<logical or expression>");
    result_ = logical_and_expression(builder_, level_ + 1);
    result_ = result_ && logical_or_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (PIPE_PIPE logical_and_expression)*
  private static boolean logical_or_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logical_or_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!logical_or_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "logical_or_expression_1", pos_)) break;
    }
    return true;
  }

  // PIPE_PIPE logical_and_expression
  private static boolean logical_or_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logical_or_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PIPE_PIPE);
    result_ = result_ && logical_and_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // postfix_expression
  public static boolean lvalue(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "lvalue")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LVALUE, "<lvalue>");
    result_ = postfix_expression(builder_, level_ + 1);
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
  // KW_MONITOR block_statement on_error_clause* on_exit_clause?
  public static boolean monitor_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "monitor_statement")) return false;
    if (!nextTokenIs(builder_, KW_MONITOR)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, MONITOR_STATEMENT, null);
    result_ = consumeToken(builder_, KW_MONITOR);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, block_statement(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, monitor_statement_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && monitor_statement_3(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // on_error_clause*
  private static boolean monitor_statement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "monitor_statement_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!on_error_clause(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "monitor_statement_2", pos_)) break;
    }
    return true;
  }

  // on_exit_clause?
  private static boolean monitor_statement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "monitor_statement_3")) return false;
    on_exit_clause(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // power_expression ((STAR | SLASH | PERCENT) power_expression)*
  public static boolean multiplicative_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicative_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, MULTIPLICATIVE_EXPRESSION, "<multiplicative expression>");
    result_ = power_expression(builder_, level_ + 1);
    result_ = result_ && multiplicative_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // ((STAR | SLASH | PERCENT) power_expression)*
  private static boolean multiplicative_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicative_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!multiplicative_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "multiplicative_expression_1", pos_)) break;
    }
    return true;
  }

  // (STAR | SLASH | PERCENT) power_expression
  private static boolean multiplicative_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicative_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = multiplicative_expression_1_0_0(builder_, level_ + 1);
    result_ = result_ && power_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // STAR | SLASH | PERCENT
  private static boolean multiplicative_expression_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicative_expression_1_0_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, STAR);
    if (!result_) result_ = consumeToken(builder_, SLASH);
    if (!result_) result_ = consumeToken(builder_, PERCENT);
    return result_;
  }

  /* ********************************************************** */
  // KW_ON_ERROR status_list? block_statement
  public static boolean on_error_clause(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "on_error_clause")) return false;
    if (!nextTokenIs(builder_, KW_ON_ERROR)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ON_ERROR_CLAUSE, null);
    result_ = consumeToken(builder_, KW_ON_ERROR);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, on_error_clause_1(builder_, level_ + 1));
    result_ = pinned_ && block_statement(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // status_list?
  private static boolean on_error_clause_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "on_error_clause_1")) return false;
    status_list(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // KW_ON_EXIT block_statement
  public static boolean on_exit_clause(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "on_exit_clause")) return false;
    if (!nextTokenIs(builder_, KW_ON_EXIT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ON_EXIT_CLAUSE, null);
    result_ = consumeToken(builder_, KW_ON_EXIT);
    pinned_ = result_; // pin = 1
    result_ = result_ && block_statement(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_OPEN    IDENT SEMI
  public static boolean open_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "open_op")) return false;
    if (!nextTokenIs(builder_, KW_OPEN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, OPEN_OP, null);
    result_ = consumeTokens(builder_, 1, KW_OPEN, IDENT, SEMI);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_OPTIONS LPAREN STAR_IDENT (COLON STAR_IDENT)* RPAREN
  public static boolean options_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "options_modifier")) return false;
    if (!nextTokenIs(builder_, KW_OPTIONS)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, OPTIONS_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_OPTIONS, LPAREN, STAR_IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, options_modifier_3(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (COLON STAR_IDENT)*
  private static boolean options_modifier_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "options_modifier_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!options_modifier_3_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "options_modifier_3", pos_)) break;
    }
    return true;
  }

  // COLON STAR_IDENT
  private static boolean options_modifier_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "options_modifier_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, COLON, STAR_IDENT);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_OTHER block_statement
  public static boolean other_clause(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "other_clause")) return false;
    if (!nextTokenIs(builder_, KW_OTHER)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, OTHER_CLAUSE, null);
    result_ = consumeToken(builder_, KW_OTHER);
    pinned_ = result_; // pin = 1
    result_ = result_ && block_statement(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_OVERLAY LPAREN IDENT (COLON INT_LIT)? RPAREN
  public static boolean overlay_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "overlay_modifier")) return false;
    if (!nextTokenIs(builder_, KW_OVERLAY)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, OVERLAY_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_OVERLAY, LPAREN, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, overlay_modifier_3(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (COLON INT_LIT)?
  private static boolean overlay_modifier_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "overlay_modifier_3")) return false;
    overlay_modifier_3_0(builder_, level_ + 1);
    return true;
  }

  // COLON INT_LIT
  private static boolean overlay_modifier_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "overlay_modifier_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, COLON, INT_LIT);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_VALUE
  //                  | KW_CONST
  //                  | KW_OPDESC
  //                  | options_modifier
  public static boolean param_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "param_modifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PARAM_MODIFIER, "<param modifier>");
    result_ = consumeToken(builder_, KW_VALUE);
    if (!result_) result_ = consumeToken(builder_, KW_CONST);
    if (!result_) result_ = consumeToken(builder_, KW_OPDESC);
    if (!result_) result_ = options_modifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_POS LPAREN INT_LIT RPAREN
  public static boolean pos_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pos_modifier")) return false;
    if (!nextTokenIs(builder_, KW_POS)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, POS_MODIFIER, null);
    result_ = consumeTokens(builder_, 1, KW_POS, LPAREN, INT_LIT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // primary postfix_suffix*
  public static boolean postfix_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfix_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, POSTFIX_EXPRESSION, "<postfix expression>");
    result_ = primary(builder_, level_ + 1);
    result_ = result_ && postfix_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // postfix_suffix*
  private static boolean postfix_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfix_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!postfix_suffix(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "postfix_expression_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // LPAREN argument_list? RPAREN              // function call
  //                  | LBRACKET subscript_list RBRACKET           // array subscript
  //                  | DOT IDENT                                   // member access
  //                  | ARROW IDENT
  public static boolean postfix_suffix(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfix_suffix")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, POSTFIX_SUFFIX, "<postfix suffix>");
    result_ = postfix_suffix_0(builder_, level_ + 1);
    if (!result_) result_ = postfix_suffix_1(builder_, level_ + 1);
    if (!result_) result_ = parseTokens(builder_, 0, DOT, IDENT);
    if (!result_) result_ = parseTokens(builder_, 0, ARROW, IDENT);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // LPAREN argument_list? RPAREN
  private static boolean postfix_suffix_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfix_suffix_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LPAREN);
    result_ = result_ && postfix_suffix_0_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RPAREN);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // argument_list?
  private static boolean postfix_suffix_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfix_suffix_0_1")) return false;
    argument_list(builder_, level_ + 1);
    return true;
  }

  // LBRACKET subscript_list RBRACKET
  private static boolean postfix_suffix_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfix_suffix_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LBRACKET);
    result_ = result_ && subscript_list(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RBRACKET);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // unary_expression (STAR_STAR power_expression)?
  public static boolean power_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "power_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, POWER_EXPRESSION, "<power expression>");
    result_ = unary_expression(builder_, level_ + 1);
    result_ = result_ && power_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (STAR_STAR power_expression)?
  private static boolean power_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "power_expression_1")) return false;
    power_expression_1_0(builder_, level_ + 1);
    return true;
  }

  // STAR_STAR power_expression
  private static boolean power_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "power_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, STAR_STAR);
    result_ = result_ && power_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // extpgm_modifier
  //               | extproc_modifier
  //               | KW_OPDESC
  //               | KW_RTNPARM
  public static boolean pr_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pr_modifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PR_MODIFIER, "<pr modifier>");
    result_ = extpgm_modifier(builder_, level_ + 1);
    if (!result_) result_ = extproc_modifier(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KW_OPDESC);
    if (!result_) result_ = consumeToken(builder_, KW_RTNPARM);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_PRE_DEFINE IDENT pre_define_value?
  public static boolean pre_define_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_define_directive")) return false;
    if (!nextTokenIs(builder_, KW_PRE_DEFINE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PRE_DEFINE_DIRECTIVE, null);
    result_ = consumeTokens(builder_, 1, KW_PRE_DEFINE, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && pre_define_directive_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // pre_define_value?
  private static boolean pre_define_directive_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_define_directive_2")) return false;
    pre_define_value(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // literal | STAR_IDENT
  static boolean pre_define_value(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_define_value")) return false;
    boolean result_;
    result_ = literal(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, STAR_IDENT);
    return result_;
  }

  /* ********************************************************** */
  // KW_PRE_ELSE
  public static boolean pre_else_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_else_directive")) return false;
    if (!nextTokenIs(builder_, KW_PRE_ELSE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KW_PRE_ELSE);
    exit_section_(builder_, marker_, PRE_ELSE_DIRECTIVE, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_PRE_ELSEIF expression
  public static boolean pre_elseif_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_elseif_directive")) return false;
    if (!nextTokenIs(builder_, KW_PRE_ELSEIF)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PRE_ELSEIF_DIRECTIVE, null);
    result_ = consumeToken(builder_, KW_PRE_ELSEIF);
    pinned_ = result_; // pin = 1
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_PRE_ENDIF
  public static boolean pre_endif_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_endif_directive")) return false;
    if (!nextTokenIs(builder_, KW_PRE_ENDIF)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KW_PRE_ENDIF);
    exit_section_(builder_, marker_, PRE_ENDIF_DIRECTIVE, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_PRE_EOF
  public static boolean pre_eof_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_eof_directive")) return false;
    if (!nextTokenIs(builder_, KW_PRE_EOF)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KW_PRE_EOF);
    exit_section_(builder_, marker_, PRE_EOF_DIRECTIVE, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_PRE_IF expression
  public static boolean pre_if_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_if_directive")) return false;
    if (!nextTokenIs(builder_, KW_PRE_IF)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PRE_IF_DIRECTIVE, null);
    result_ = consumeToken(builder_, KW_PRE_IF);
    pinned_ = result_; // pin = 1
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_PRE_INCLUDE (STR_LIT | IDENT)
  public static boolean pre_include_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_include_directive")) return false;
    if (!nextTokenIs(builder_, KW_PRE_INCLUDE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PRE_INCLUDE_DIRECTIVE, null);
    result_ = consumeToken(builder_, KW_PRE_INCLUDE);
    pinned_ = result_; // pin = 1
    result_ = result_ && pre_include_directive_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // STR_LIT | IDENT
  private static boolean pre_include_directive_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_include_directive_1")) return false;
    boolean result_;
    result_ = consumeToken(builder_, STR_LIT);
    if (!result_) result_ = consumeToken(builder_, IDENT);
    return result_;
  }

  /* ********************************************************** */
  // KW_PRE_UNDEFINE IDENT
  public static boolean pre_undefine_directive(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pre_undefine_directive")) return false;
    if (!nextTokenIs(builder_, KW_PRE_UNDEFINE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PRE_UNDEFINE_DIRECTIVE, null);
    result_ = consumeTokens(builder_, 1, KW_PRE_UNDEFINE, IDENT);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_PREFIX  LPAREN IDENT (COLON INT_LIT)? RPAREN
  public static boolean prefix_f_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefix_f_keyword")) return false;
    if (!nextTokenIs(builder_, KW_PREFIX)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PREFIX_F_KEYWORD, null);
    result_ = consumeTokens(builder_, 1, KW_PREFIX, LPAREN, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, prefix_f_keyword_3(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (COLON INT_LIT)?
  private static boolean prefix_f_keyword_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefix_f_keyword_3")) return false;
    prefix_f_keyword_3_0(builder_, level_ + 1);
    return true;
  }

  // COLON INT_LIT
  private static boolean prefix_f_keyword_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefix_f_keyword_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, COLON, INT_LIT);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // literal
  //           | KW_TRUE | KW_FALSE | KW_NULL
  //           | STAR_IDENT
  //           | IDENT
  //           | LPAREN expression RPAREN
  public static boolean primary(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primary")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PRIMARY, "<primary>");
    result_ = literal(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KW_TRUE);
    if (!result_) result_ = consumeToken(builder_, KW_FALSE);
    if (!result_) result_ = consumeToken(builder_, KW_NULL);
    if (!result_) result_ = consumeToken(builder_, STAR_IDENT);
    if (!result_) result_ = consumeToken(builder_, IDENT);
    if (!result_) result_ = primary_6(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // LPAREN expression RPAREN
  private static boolean primary_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primary_6")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LPAREN);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RPAREN);
    exit_section_(builder_, marker_, null, result_);
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
  // KW_EXPORT | extproc_modifier
  public static boolean proc_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "proc_modifier")) return false;
    if (!nextTokenIs(builder_, "<proc modifier>", KW_EXPORT, KW_EXTPROC)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PROC_MODIFIER, "<proc modifier>");
    result_ = consumeToken(builder_, KW_EXPORT);
    if (!result_) result_ = extproc_modifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_DCL_PROC IDENT inline_param_list? return_type? proc_modifier* block_statement
  public static boolean procedure_declaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "procedure_declaration")) return false;
    if (!nextTokenIs(builder_, KW_DCL_PROC)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PROCEDURE_DECLARATION, null);
    result_ = consumeTokens(builder_, 1, KW_DCL_PROC, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, procedure_declaration_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, procedure_declaration_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, procedure_declaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && block_statement(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // inline_param_list?
  private static boolean procedure_declaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "procedure_declaration_2")) return false;
    inline_param_list(builder_, level_ + 1);
    return true;
  }

  // return_type?
  private static boolean procedure_declaration_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "procedure_declaration_3")) return false;
    return_type(builder_, level_ + 1);
    return true;
  }

  // proc_modifier*
  private static boolean procedure_declaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "procedure_declaration_4")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!proc_modifier(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "procedure_declaration_4", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // KW_DCL_PR IDENT inline_param_list? return_type? pr_modifier* SEMI
  public static boolean prototype_declaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prototype_declaration")) return false;
    if (!nextTokenIs(builder_, KW_DCL_PR)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PROTOTYPE_DECLARATION, null);
    result_ = consumeTokens(builder_, 1, KW_DCL_PR, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, prototype_declaration_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, prototype_declaration_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, prototype_declaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // inline_param_list?
  private static boolean prototype_declaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prototype_declaration_2")) return false;
    inline_param_list(builder_, level_ + 1);
    return true;
  }

  // return_type?
  private static boolean prototype_declaration_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prototype_declaration_3")) return false;
    return_type(builder_, level_ + 1);
    return true;
  }

  // pr_modifier*
  private static boolean prototype_declaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prototype_declaration_4")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!pr_modifier(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "prototype_declaration_4", pos_)) break;
    }
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
  // KW_READ    IDENT IDENT? SEMI
  public static boolean read_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "read_op")) return false;
    if (!nextTokenIs(builder_, KW_READ)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, READ_OP, null);
    result_ = consumeTokens(builder_, 1, KW_READ, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, read_op_2(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // IDENT?
  private static boolean read_op_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "read_op_2")) return false;
    consumeToken(builder_, IDENT);
    return true;
  }

  /* ********************************************************** */
  // KW_READE   expression IDENT IDENT? SEMI
  public static boolean reade_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "reade_op")) return false;
    if (!nextTokenIs(builder_, KW_READE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, READE_OP, null);
    result_ = consumeToken(builder_, KW_READE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, IDENT)) && result_;
    result_ = pinned_ && report_error_(builder_, reade_op_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // IDENT?
  private static boolean reade_op_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "reade_op_3")) return false;
    consumeToken(builder_, IDENT);
    return true;
  }

  /* ********************************************************** */
  // KW_READP   IDENT IDENT? SEMI
  public static boolean readp_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "readp_op")) return false;
    if (!nextTokenIs(builder_, KW_READP)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, READP_OP, null);
    result_ = consumeTokens(builder_, 1, KW_READP, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, readp_op_2(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // IDENT?
  private static boolean readp_op_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "readp_op_2")) return false;
    consumeToken(builder_, IDENT);
    return true;
  }

  /* ********************************************************** */
  // KW_READPE  expression IDENT IDENT? SEMI
  public static boolean readpe_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "readpe_op")) return false;
    if (!nextTokenIs(builder_, KW_READPE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, READPE_OP, null);
    result_ = consumeToken(builder_, KW_READPE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, IDENT)) && result_;
    result_ = pinned_ && report_error_(builder_, readpe_op_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // IDENT?
  private static boolean readpe_op_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "readpe_op_3")) return false;
    consumeToken(builder_, IDENT);
    return true;
  }

  /* ********************************************************** */
  // shift_expression ((LT | GT | LT_EQ | GT_EQ) shift_expression)*
  public static boolean relational_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relational_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, RELATIONAL_EXPRESSION, "<relational expression>");
    result_ = shift_expression(builder_, level_ + 1);
    result_ = result_ && relational_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // ((LT | GT | LT_EQ | GT_EQ) shift_expression)*
  private static boolean relational_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relational_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!relational_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "relational_expression_1", pos_)) break;
    }
    return true;
  }

  // (LT | GT | LT_EQ | GT_EQ) shift_expression
  private static boolean relational_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relational_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = relational_expression_1_0_0(builder_, level_ + 1);
    result_ = result_ && shift_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // LT | GT | LT_EQ | GT_EQ
  private static boolean relational_expression_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relational_expression_1_0_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, LT);
    if (!result_) result_ = consumeToken(builder_, GT);
    if (!result_) result_ = consumeToken(builder_, LT_EQ);
    if (!result_) result_ = consumeToken(builder_, GT_EQ);
    return result_;
  }

  /* ********************************************************** */
  // KW_RENAME  LPAREN IDENT COLON IDENT RPAREN
  public static boolean rename_f_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rename_f_keyword")) return false;
    if (!nextTokenIs(builder_, KW_RENAME)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, RENAME_F_KEYWORD, null);
    result_ = consumeTokens(builder_, 1, KW_RENAME, LPAREN, IDENT, COLON, IDENT, RPAREN);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_RETURN expression? SEMI
  public static boolean return_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "return_statement")) return false;
    if (!nextTokenIs(builder_, KW_RETURN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, RETURN_STATEMENT, null);
    result_ = consumeToken(builder_, KW_RETURN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, return_statement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // expression?
  private static boolean return_statement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "return_statement_1")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // ARROW type_specification
  public static boolean return_type(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "return_type")) return false;
    if (!nextTokenIs(builder_, ARROW)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, RETURN_TYPE, null);
    result_ = consumeToken(builder_, ARROW);
    pinned_ = result_; // pin = 1
    result_ = result_ && type_specification(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_SELECT LBRACE when_clause+ other_clause? RBRACE
  public static boolean select_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "select_statement")) return false;
    if (!nextTokenIs(builder_, KW_SELECT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SELECT_STATEMENT, null);
    result_ = consumeTokens(builder_, 1, KW_SELECT, LBRACE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, select_statement_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, select_statement_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, RBRACE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // when_clause+
  private static boolean select_statement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "select_statement_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = when_clause(builder_, level_ + 1);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!when_clause(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "select_statement_2", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // other_clause?
  private static boolean select_statement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "select_statement_3")) return false;
    other_clause(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // KW_SETGT   expression IDENT SEMI
  public static boolean setgt_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setgt_op")) return false;
    if (!nextTokenIs(builder_, KW_SETGT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SETGT_OP, null);
    result_ = consumeToken(builder_, KW_SETGT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeTokens(builder_, -1, IDENT, SEMI)) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_SETLL   expression IDENT SEMI
  public static boolean setll_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setll_op")) return false;
    if (!nextTokenIs(builder_, KW_SETLL)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SETLL_OP, null);
    result_ = consumeToken(builder_, KW_SETLL);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeTokens(builder_, -1, IDENT, SEMI)) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // additive_expression ((LT_LT | GT_GT) additive_expression)*
  public static boolean shift_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shift_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SHIFT_EXPRESSION, "<shift expression>");
    result_ = additive_expression(builder_, level_ + 1);
    result_ = result_ && shift_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // ((LT_LT | GT_GT) additive_expression)*
  private static boolean shift_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shift_expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!shift_expression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "shift_expression_1", pos_)) break;
    }
    return true;
  }

  // (LT_LT | GT_GT) additive_expression
  private static boolean shift_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shift_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = shift_expression_1_0_0(builder_, level_ + 1);
    result_ = result_ && additive_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // LT_LT | GT_GT
  private static boolean shift_expression_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shift_expression_1_0_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, LT_LT);
    if (!result_) result_ = consumeToken(builder_, GT_GT);
    return result_;
  }

  /* ********************************************************** */
  // KW_KEYED | KW_USROPN | KW_DISK | KW_PRINTER | KW_WORKSTN | KW_SEQ
  public static boolean simple_f_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simple_f_keyword")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SIMPLE_F_KEYWORD, "<simple f keyword>");
    result_ = consumeToken(builder_, KW_KEYED);
    if (!result_) result_ = consumeToken(builder_, KW_USROPN);
    if (!result_) result_ = consumeToken(builder_, KW_DISK);
    if (!result_) result_ = consumeToken(builder_, KW_PRINTER);
    if (!result_) result_ = consumeToken(builder_, KW_WORKSTN);
    if (!result_) result_ = consumeToken(builder_, KW_SEQ);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // variable_declaration
  //           | constant_declaration
  //           | data_structure_declaration
  //           | statement
  //           | unknown_sr_item
  public static boolean sr_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "sr_item")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SR_ITEM, "<sr item>");
    result_ = variable_declaration(builder_, level_ + 1);
    if (!result_) result_ = constant_declaration(builder_, level_ + 1);
    if (!result_) result_ = data_structure_declaration(builder_, level_ + 1);
    if (!result_) result_ = statement(builder_, level_ + 1);
    if (!result_) result_ = unknown_sr_item(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // if_statement
  //             | select_statement
  //             | while_statement
  //             | do_while_statement
  //             | for_statement
  //             | break_statement
  //             | continue_statement
  //             | return_statement
  //             | monitor_statement
  //             | file_op_statement       // L5
  //             | exsr_statement          // L5
  //             | leavesr_statement       // L5
  //             | callp_statement         // L5
  //             | expression_statement
  public static boolean statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, STATEMENT, "<statement>");
    result_ = if_statement(builder_, level_ + 1);
    if (!result_) result_ = select_statement(builder_, level_ + 1);
    if (!result_) result_ = while_statement(builder_, level_ + 1);
    if (!result_) result_ = do_while_statement(builder_, level_ + 1);
    if (!result_) result_ = for_statement(builder_, level_ + 1);
    if (!result_) result_ = break_statement(builder_, level_ + 1);
    if (!result_) result_ = continue_statement(builder_, level_ + 1);
    if (!result_) result_ = return_statement(builder_, level_ + 1);
    if (!result_) result_ = monitor_statement(builder_, level_ + 1);
    if (!result_) result_ = file_op_statement(builder_, level_ + 1);
    if (!result_) result_ = exsr_statement(builder_, level_ + 1);
    if (!result_) result_ = leavesr_statement(builder_, level_ + 1);
    if (!result_) result_ = callp_statement(builder_, level_ + 1);
    if (!result_) result_ = expression_statement(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
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
  // LPAREN expression (COMMA expression)* RPAREN
  public static boolean status_list(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "status_list")) return false;
    if (!nextTokenIs(builder_, LPAREN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, STATUS_LIST, null);
    result_ = consumeToken(builder_, LPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, status_list_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (COMMA expression)*
  private static boolean status_list_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "status_list_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!status_list_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "status_list_2", pos_)) break;
    }
    return true;
  }

  // COMMA expression
  private static boolean status_list_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "status_list_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_BEGSR IDENT SEMI sr_item* KW_ENDSR IDENT? SEMI
  public static boolean subroutine_definition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "subroutine_definition")) return false;
    if (!nextTokenIs(builder_, KW_BEGSR)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SUBROUTINE_DEFINITION, null);
    result_ = consumeTokens(builder_, 1, KW_BEGSR, IDENT, SEMI);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, subroutine_definition_3(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, KW_ENDSR)) && result_;
    result_ = pinned_ && report_error_(builder_, subroutine_definition_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // sr_item*
  private static boolean subroutine_definition_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "subroutine_definition_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!sr_item(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "subroutine_definition_3", pos_)) break;
    }
    return true;
  }

  // IDENT?
  private static boolean subroutine_definition_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "subroutine_definition_5")) return false;
    consumeToken(builder_, IDENT);
    return true;
  }

  /* ********************************************************** */
  // expression (COMMA expression)*
  public static boolean subscript_list(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "subscript_list")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SUBSCRIPT_LIST, "<subscript list>");
    result_ = expression(builder_, level_ + 1);
    result_ = result_ && subscript_list_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (COMMA expression)*
  private static boolean subscript_list_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "subscript_list_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!subscript_list_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "subscript_list_1", pos_)) break;
    }
    return true;
  }

  // COMMA expression
  private static boolean subscript_list_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "subscript_list_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // KW_TEMPLATE
  public static boolean template_modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "template_modifier")) return false;
    if (!nextTokenIs(builder_, KW_TEMPLATE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KW_TEMPLATE);
    exit_section_(builder_, marker_, TEMPLATE_MODIFIER, result_);
    return result_;
  }

  /* ********************************************************** */
  // logical_or_expression (QUESTION expression COLON ternary_expression)?
  public static boolean ternary_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternary_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, TERNARY_EXPRESSION, "<ternary expression>");
    result_ = logical_or_expression(builder_, level_ + 1);
    result_ = result_ && ternary_expression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (QUESTION expression COLON ternary_expression)?
  private static boolean ternary_expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternary_expression_1")) return false;
    ternary_expression_1_0(builder_, level_ + 1);
    return true;
  }

  // QUESTION expression COLON ternary_expression
  private static boolean ternary_expression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternary_expression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, QUESTION);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && ternary_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // variable_declaration
  //                  | constant_declaration
  //                  | ctl_opt_statement
  //                  | data_structure_declaration
  //                  | file_declaration
  //                  | prototype_declaration
  //                  | procedure_declaration
  //                  | directive
  //                  | unknown_item
  public static boolean top_level_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_item")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, TOP_LEVEL_ITEM, "<top level item>");
    result_ = variable_declaration(builder_, level_ + 1);
    if (!result_) result_ = constant_declaration(builder_, level_ + 1);
    if (!result_) result_ = ctl_opt_statement(builder_, level_ + 1);
    if (!result_) result_ = data_structure_declaration(builder_, level_ + 1);
    if (!result_) result_ = file_declaration(builder_, level_ + 1);
    if (!result_) result_ = prototype_declaration(builder_, level_ + 1);
    if (!result_) result_ = procedure_declaration(builder_, level_ + 1);
    if (!result_) result_ = directive(builder_, level_ + 1);
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
  // (PLUS | MINUS | BANG | TILDE) unary_expression
  //                    | postfix_expression
  public static boolean unary_expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unary_expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, UNARY_EXPRESSION, "<unary expression>");
    result_ = unary_expression_0(builder_, level_ + 1);
    if (!result_) result_ = postfix_expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (PLUS | MINUS | BANG | TILDE) unary_expression
  private static boolean unary_expression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unary_expression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = unary_expression_0_0(builder_, level_ + 1);
    result_ = result_ && unary_expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // PLUS | MINUS | BANG | TILDE
  private static boolean unary_expression_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unary_expression_0_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, PLUS);
    if (!result_) result_ = consumeToken(builder_, MINUS);
    if (!result_) result_ = consumeToken(builder_, BANG);
    if (!result_) result_ = consumeToken(builder_, TILDE);
    return result_;
  }

  /* ********************************************************** */
  // !RBRACE !<<eof>> any_token
  static boolean unknown_block_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unknown_block_item")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = unknown_block_item_0(builder_, level_ + 1);
    result_ = result_ && unknown_block_item_1(builder_, level_ + 1);
    result_ = result_ && consumeAnyToken(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !RBRACE
  private static boolean unknown_block_item_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unknown_block_item_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !consumeToken(builder_, RBRACE);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // !<<eof>>
  private static boolean unknown_block_item_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unknown_block_item_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !eof(builder_, level_ + 1);
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
  // !KW_ENDSR !RBRACE !<<eof>> any_token
  static boolean unknown_sr_item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unknown_sr_item")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = unknown_sr_item_0(builder_, level_ + 1);
    result_ = result_ && unknown_sr_item_1(builder_, level_ + 1);
    result_ = result_ && unknown_sr_item_2(builder_, level_ + 1);
    result_ = result_ && consumeAnyToken(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !KW_ENDSR
  private static boolean unknown_sr_item_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unknown_sr_item_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !consumeToken(builder_, KW_ENDSR);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // !RBRACE
  private static boolean unknown_sr_item_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unknown_sr_item_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !consumeToken(builder_, RBRACE);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // !<<eof>>
  private static boolean unknown_sr_item_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unknown_sr_item_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !eof(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // KW_UNLOCK  IDENT SEMI
  public static boolean unlock_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unlock_op")) return false;
    if (!nextTokenIs(builder_, KW_UNLOCK)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, UNLOCK_OP, null);
    result_ = consumeTokens(builder_, 1, KW_UNLOCK, IDENT, SEMI);
    pinned_ = result_; // pin = 1
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_UPDATE  IDENT IDENT? SEMI
  public static boolean update_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "update_op")) return false;
    if (!nextTokenIs(builder_, KW_UPDATE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, UPDATE_OP, null);
    result_ = consumeTokens(builder_, 1, KW_UPDATE, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, update_op_2(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // IDENT?
  private static boolean update_op_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "update_op_2")) return false;
    consumeToken(builder_, IDENT);
    return true;
  }

  /* ********************************************************** */
  // KW_USAGE   LPAREN STAR_IDENT (COLON STAR_IDENT)* RPAREN
  public static boolean usage_f_keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usage_f_keyword")) return false;
    if (!nextTokenIs(builder_, KW_USAGE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, USAGE_F_KEYWORD, null);
    result_ = consumeTokens(builder_, 1, KW_USAGE, LPAREN, STAR_IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, usage_f_keyword_3(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RPAREN) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (COLON STAR_IDENT)*
  private static boolean usage_f_keyword_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usage_f_keyword_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!usage_f_keyword_3_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "usage_f_keyword_3", pos_)) break;
    }
    return true;
  }

  // COLON STAR_IDENT
  private static boolean usage_f_keyword_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usage_f_keyword_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, COLON, STAR_IDENT);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // inz_modifier
  //                | static_modifier
  //                | export_modifier
  //                | dim_modifier
  //                | based_modifier
  //                | qualified_modifier
  //                | overlay_modifier
  //                | pos_modifier
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
    if (!result_) result_ = overlay_modifier(builder_, level_ + 1);
    if (!result_) result_ = pos_modifier(builder_, level_ + 1);
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

  /* ********************************************************** */
  // KW_WHEN LPAREN expression RPAREN block_statement
  public static boolean when_clause(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "when_clause")) return false;
    if (!nextTokenIs(builder_, KW_WHEN)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, WHEN_CLAUSE, null);
    result_ = consumeTokens(builder_, 1, KW_WHEN, LPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, RPAREN)) && result_;
    result_ = pinned_ && block_statement(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_WHILE LPAREN expression RPAREN block_statement
  public static boolean while_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "while_statement")) return false;
    if (!nextTokenIs(builder_, KW_WHILE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, WHILE_STATEMENT, null);
    result_ = consumeTokens(builder_, 1, KW_WHILE, LPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, RPAREN)) && result_;
    result_ = pinned_ && block_statement(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // KW_WRITE   IDENT IDENT? SEMI
  public static boolean write_op(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "write_op")) return false;
    if (!nextTokenIs(builder_, KW_WRITE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, WRITE_OP, null);
    result_ = consumeTokens(builder_, 1, KW_WRITE, IDENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, write_op_2(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, SEMI) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // IDENT?
  private static boolean write_op_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "write_op_2")) return false;
    consumeToken(builder_, IDENT);
    return true;
  }

}
