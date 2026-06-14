package com.larena.boxbreaker.core.lexer;

import java.util.Set;

/**
 * BBK reserved words and attribute modifiers, taken verbatim from
 * {@code BBK.bnf}. BBK is <b>case-sensitive</b>, so these are matched exactly
 * (no lower-casing).
 */
public final class BbkKeywords {

    private BbkKeywords() {}

    /** Every reserved word, exact spelling. */
    public static final Set<String> KEYWORDS = Set.of(
        // control flow (C-style)
        "if", "else", "while", "do", "for", "break", "continue", "return",
        // control flow (RPG-style retained)
        "select", "when", "other", "monitor", "on-error", "on-exit",
        // boolean / null literals
        "true", "false", "null",
        // declaration keywords
        "DCL-S", "DCL-C", "DCL-DS", "DCL-PR", "DCL-PROC", "DCL-F", "DCL-PARM", "DCL-SUBF",
        // primitive types
        "CHAR", "VARCHAR", "PACKED", "ZONED", "BINDEC", "INT", "UNS", "FLOAT",
        "DATE", "TIME", "TIMESTAMP", "BOOL", "POINTER", "VOID",
        // declaration modifiers
        "INZ", "BASED", "DIM", "OVERLAY", "POS", "LIKE", "LIKEDS", "LIKEREC",
        "TEMPLATE", "QUALIFIED", "ALIGN", "VALUE", "CONST", "OPTIONS", "RTNPARM",
        "OPDESC", "STATIC", "EXPORT", "IMPORT", "EXTPGM", "EXTPROC",
        // file-spec keywords
        "USAGE", "KEYED", "EXTNAME", "EXTFILE", "PREFIX", "RENAME", "DISK",
        "PRINTER", "WORKSTN", "SEQ", "USROPN", "INFDS", "INDDS",
        // directives
        "CTL-OPT", "PRE-IF", "PRE-ELSEIF", "PRE-ELSE", "PRE-ENDIF",
        "PRE-DEFINE", "PRE-UNDEFINE", "PRE-INCLUDE", "PRE-EOF",
        // file operations
        "read", "reade", "readp", "readpe", "chain", "setll", "setgt",
        "write", "update", "delete", "unlock", "open", "close", "exfmt",
        // subroutines + callp
        "BEGSR", "ENDSR", "EXSR", "LEAVESR", "CALLP"
    );

    /** Attribute modifiers: {@code @halfup}, {@code @halfdown}, {@code @trunc}. */
    public static final Set<String> ATTRIBUTES = Set.of("@halfup", "@halfdown", "@trunc");

    public static boolean isKeyword(String s) {
        return KEYWORDS.contains(s);
    }

    public static boolean isAttribute(String s) {
        return ATTRIBUTES.contains(s);
    }
}
