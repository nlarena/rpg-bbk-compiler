package com.larena.boxbreaker.rpg.lexer;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Catalog of free-format RPGLE reserved words, opcodes and figurative
 * constants. Grounded in {@code docs/theory/rpgle-grammar.md} §2.3 (reserved
 * words), §2.4 (free-form opcodes) and §2.6 (figurative constants).
 *
 * <p>The grammar doc states its lists are non-exhaustive (IBM adds keywords per
 * release); this set follows the documented entries and can grow. Legacy
 * fixed-form-only opcodes ({@code Z-ADD}, {@code MOVE}, {@code CASxx},
 * {@code GOTO}, ...) are intentionally excluded — we target free-form.
 *
 * <p>All entries are lowercase; lookups must lowercase first (RPG is
 * case-insensitive).
 */
public final class RpgKeywords {

    private RpgKeywords() {}

    /** Reserved words + free-form opcodes + word operators. */
    public static final Set<String> KEYWORDS = Set.of(
        // §2.3 reserved words — declarations and specs
        "ctl-opt", "dcl-s", "dcl-c", "dcl-ds", "dcl-pr", "dcl-pi", "dcl-f", "dcl-proc",
        "end-ds", "end-pr", "end-pi", "end-proc",
        // subroutines
        "begsr", "endsr", "exsr", "leavesr",
        // control flow
        "if", "else", "elseif", "endif",
        "select", "when", "other", "endsl",
        "dow", "enddo", "dou", "doweq",
        "for", "endfor", "to", "downto", "by",
        "iter", "leave", "return",
        "monitor", "on-error", "on-exit", "endmon",
        // declaration keywords / modifiers
        "inz", "based", "pos", "overlay",
        "like", "likeds", "likerec", "template",
        "export", "import", "static", "auto",
        "opdesc", "options", "rtnparm", "const", "value",
        "global", "qualified", "align", "extpgm", "extproc",
        "psds", "infds", "indara",
        "usropn", "disk", "printer", "workstn", "seq",
        "usage", "rename", "prefix", "extname", "extfile",
        // §2.4 free-form opcodes (legacy fixed-form C-spec opcodes excluded)
        "eval", "evalr", "eval-corr", "clear", "reset",
        "callp", "dsply", "dump", "sorta", "lookup", "define",
        "read", "reade", "readp", "readpe", "chain",
        "write", "update", "delete", "unlock",
        "open", "close", "feod", "setll", "setgt", "exfmt", "except",
        // §2.7 word operators
        "and", "or", "not", "xor"
    );

    /**
     * Figurative constants (§2.6). Indicator references ({@code *IN01}..
     * {@code *IN99}, {@code *INLR}, ...) are matched by {@link #INDICATOR}
     * instead of being enumerated.
     */
    public static final Set<String> FIGURATIVE = Set.of(
        "*on", "*off", "*blank", "*blanks", "*zero", "*zeros",
        "*hival", "*loval", "*null", "*omit", "*all",
        "*start", "*end", "*loopcount", "*date", "*time", "*timestamp"
    );

    /** Indicator references: {@code *IN}, {@code *INLR}, {@code *INRT}, {@code *IN01}.. */
    public static final Pattern INDICATOR =
        Pattern.compile("\\*in([a-z0-9]{1,4})?", Pattern.CASE_INSENSITIVE);

    /**
     * Compiler directives (preprocessor). Written {@code /name} at the start of
     * a line; the whole line is the directive. Distinct from the {@code /}
     * division operator. Names stored without the leading slash, lowercase.
     */
    public static final Set<String> DIRECTIVES = Set.of(
        "copy", "include",
        "if", "elseif", "else", "endif",
        "define", "undefine",
        "eof", "free", "end-free", "set", "restore", "space", "title", "eject"
    );

    public static boolean isKeyword(String word) {
        return KEYWORDS.contains(word.toLowerCase());
    }

    /** A directive name (without the leading {@code /}), e.g. {@code "copy"}, {@code "if"}. */
    public static boolean isDirective(String word) {
        return DIRECTIVES.contains(word.toLowerCase());
    }

    /** A {@code *}-prefixed word that is a figurative constant or an indicator. */
    public static boolean isStarName(String starWord) {
        String lower = starWord.toLowerCase();
        return FIGURATIVE.contains(lower) || INDICATOR.matcher(lower).matches();
    }
}
