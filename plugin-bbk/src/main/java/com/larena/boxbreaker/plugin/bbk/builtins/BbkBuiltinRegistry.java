package com.larena.boxbreaker.plugin.bbk.builtins;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalog of BBK built-in functions. Single source of truth, consumed by
 * completion (signature templates) and quick documentation (descriptions).
 *
 * <p>Lookup is case-insensitive — BBK identifiers are case-insensitive and the
 * BIF names commonly appear as {@code %trim} / {@code TRIM} / {@code trim}.
 */
public final class BbkBuiltinRegistry {

    private BbkBuiltinRegistry() {}

    private static final Map<String, BbkBuiltinFunction> BY_NAME = new LinkedHashMap<>();

    private static void add(@NotNull String name, @NotNull String signature, @NotNull String returnType,
                            @NotNull String summary, @NotNull List<BbkBuiltinFunction.Parameter> params,
                            @NotNull String description) {
        BY_NAME.put(name.toLowerCase(),
            new BbkBuiltinFunction(name, signature, returnType, summary, params, description));
    }

    private static BbkBuiltinFunction.Parameter p(@NotNull String name, @NotNull String doc) {
        return new BbkBuiltinFunction.Parameter(name, doc);
    }

    static {
        // ----- Core / output -----
        add("print", "print(x)", "(void)",
            "Prints a value to standard output.",
            List.of(p("x", "The value to print.")),
            "Writes <code>x</code> followed by a newline to standard output. Core intrinsic of the BBK runtime.");

        // ----- String functions -----
        add("lower", "lower(s)", "CHAR/VARCHAR",
            "Lowercases a string.",
            List.of(p("s", "The string to convert.")),
            "Returns <code>s</code> with every letter in lower case.");
        add("upper", "upper(s)", "CHAR/VARCHAR",
            "Uppercases a string.",
            List.of(p("s", "The string to convert.")),
            "Returns <code>s</code> with every letter in upper case.");
        add("replace", "replace(s, old, new)", "CHAR/VARCHAR",
            "Replaces occurrences of a substring.",
            List.of(p("s", "The source string."),
                p("old", "The substring to find."),
                p("new", "The replacement substring.")),
            "Returns <code>s</code> with every occurrence of <code>old</code> replaced by <code>new</code>.");
        add("trim", "trim(s)", "CHAR/VARCHAR",
            "Removes leading and trailing blanks.",
            List.of(p("s", "The string to trim.")),
            "Returns <code>s</code> with both leading and trailing blanks removed.");
        add("triml", "triml(s)", "CHAR/VARCHAR",
            "Removes leading blanks.",
            List.of(p("s", "The string to trim on the left.")),
            "Returns <code>s</code> with leading blanks removed.");
        add("trimr", "trimr(s)", "CHAR/VARCHAR",
            "Removes trailing blanks.",
            List.of(p("s", "The string to trim on the right.")),
            "Returns <code>s</code> with trailing blanks removed.");
        add("substr", "substr(s, start [, len])", "CHAR/VARCHAR",
            "Extracts a substring.",
            List.of(p("s", "The source string."),
                p("start", "1-based start position."),
                p("len", "Optional length; defaults to the rest of the string.")),
            "Returns the substring of <code>s</code> starting at <code>start</code> for <code>len</code> characters.");
        add("scan", "scan(needle, hay [, start])", "INT",
            "Finds a substring position.",
            List.of(p("needle", "The string to search for."),
                p("hay", "The string to search in."),
                p("start", "Optional 1-based search start position.")),
            "Returns the 1-based position of <code>needle</code> within <code>hay</code>, or 0 if not found.");
        add("len", "len(s)", "INT",
            "Returns the current length.",
            List.of(p("s", "A string or variable-length field.")),
            "Returns the current length of <code>s</code> (for VARCHAR, the active length; for CHAR, the declared length).");
        add("xlate", "xlate(from, to, s)", "CHAR/VARCHAR",
            "Translates characters.",
            List.of(p("from", "Set of source characters."),
                p("to", "Set of replacement characters."),
                p("s", "The string to translate.")),
            "Returns <code>s</code> with every character in <code>from</code> replaced by the character at the same position in <code>to</code>.");

        // ----- Numeric / conversion functions -----
        add("char", "char(n)", "CHAR",
            "Converts to character.",
            List.of(p("n", "A numeric, date, time or timestamp value.")),
            "Returns the character representation of <code>n</code>.");
        add("dec", "dec(value [, p, d])", "PACKED",
            "Converts to packed decimal.",
            List.of(p("value", "The value to convert."),
                p("p", "Optional total precision."),
                p("d", "Optional decimal places.")),
            "Returns <code>value</code> as a packed decimal, optionally with the given precision and scale.");
        add("int", "int(n)", "INT",
            "Truncates to integer.",
            List.of(p("n", "A numeric value.")),
            "Returns the integer part of <code>n</code>, truncating any fractional part.");
        add("float", "float(n)", "FLOAT",
            "Converts to floating point.",
            List.of(p("n", "A numeric value.")),
            "Returns <code>n</code> as a floating-point number.");
        add("sqrt", "sqrt(n)", "FLOAT",
            "Square root.",
            List.of(p("n", "A non-negative numeric value.")),
            "Returns the square root of <code>n</code>.");
        add("inth", "inth(n)", "INT",
            "Rounds to integer.",
            List.of(p("n", "A numeric value.")),
            "Returns <code>n</code> rounded half-up to the nearest integer.");
        add("abs", "abs(n)", "numeric",
            "Absolute value.",
            List.of(p("n", "A numeric value.")),
            "Returns the absolute value of <code>n</code>.");

        // ----- Array / structure functions -----
        add("elem", "elem(array)", "INT",
            "Number of elements.",
            List.of(p("array", "An array (DIM) field.")),
            "Returns the number of elements declared for <code>array</code>.");
        add("size", "size(v)", "INT",
            "Size in bytes.",
            List.of(p("v", "A variable, data structure or field.")),
            "Returns the storage size of <code>v</code> in bytes.");
        add("addr", "addr(v)", "POINTER",
            "Address of a variable.",
            List.of(p("v", "A variable or field.")),
            "Returns a pointer to the storage of <code>v</code>.");
        add("lookup", "lookup(key, array)", "INT",
            "Searches an array.",
            List.of(p("key", "The value to find."),
                p("array", "The array to search.")),
            "Returns the 1-based index of <code>key</code> in <code>array</code>, or 0 if not found.");

        // ----- Date / time functions -----
        add("date", "date(value)", "DATE",
            "Converts to a date.",
            List.of(p("value", "A string or timestamp to interpret as a date.")),
            "Returns <code>value</code> converted to a DATE.");
        add("time", "time(value)", "TIME",
            "Converts to a time.",
            List.of(p("value", "A string or timestamp to interpret as a time.")),
            "Returns <code>value</code> converted to a TIME.");
        add("timestamp", "timestamp(value)", "TIMESTAMP",
            "Converts to a timestamp.",
            List.of(p("value", "A string to interpret as a timestamp.")),
            "Returns <code>value</code> converted to a TIMESTAMP.");
        add("today", "today()", "DATE",
            "Today's date.",
            List.of(),
            "Returns the current date.");
        add("now", "now()", "TIMESTAMP",
            "Current date and time.",
            List.of(),
            "Returns the current date and time.");
        add("year", "year(d)", "INT",
            "Year component.",
            List.of(p("d", "A DATE or TIMESTAMP.")),
            "Returns the year of <code>d</code>.");
        add("month", "month(d)", "INT",
            "Month component (1-12).",
            List.of(p("d", "A DATE or TIMESTAMP.")),
            "Returns the month of <code>d</code> (1-12).");
        add("day", "day(d)", "INT",
            "Day-of-month component.",
            List.of(p("d", "A DATE or TIMESTAMP.")),
            "Returns the day of the month of <code>d</code>.");
        add("hour", "hour(t)", "INT",
            "Hour component (0-23).",
            List.of(p("t", "A TIME or TIMESTAMP.")),
            "Returns the hour of <code>t</code>.");
        add("minute", "minute(t)", "INT",
            "Minute component (0-59).",
            List.of(p("t", "A TIME or TIMESTAMP.")),
            "Returns the minute of <code>t</code>.");
        add("second", "second(t)", "INT",
            "Second component (0-59).",
            List.of(p("t", "A TIME or TIMESTAMP.")),
            "Returns the second of <code>t</code>.");
        add("adddays", "adddays(d, n)", "DATE/TIMESTAMP",
            "Adds days.",
            List.of(p("d", "A DATE or TIMESTAMP."), p("n", "Number of days (negative subtracts).")),
            "Returns <code>d</code> plus <code>n</code> days.");
        add("addmonths", "addmonths(d, n)", "DATE/TIMESTAMP",
            "Adds months.",
            List.of(p("d", "A DATE or TIMESTAMP."), p("n", "Number of months (negative subtracts).")),
            "Returns <code>d</code> plus <code>n</code> months; the day is clamped to the last valid day.");
        add("addyears", "addyears(d, n)", "DATE/TIMESTAMP",
            "Adds years.",
            List.of(p("d", "A DATE or TIMESTAMP."), p("n", "Number of years (negative subtracts).")),
            "Returns <code>d</code> plus <code>n</code> years; Feb 29 is clamped to Feb 28 on non-leap years.");
        add("addhours", "addhours(t, n)", "TIME/TIMESTAMP",
            "Adds hours.",
            List.of(p("t", "A TIME or TIMESTAMP."), p("n", "Number of hours (negative subtracts).")),
            "Returns <code>t</code> plus <code>n</code> hours (TIME wraps within the day).");
        add("addminutes", "addminutes(t, n)", "TIME/TIMESTAMP",
            "Adds minutes.",
            List.of(p("t", "A TIME or TIMESTAMP."), p("n", "Number of minutes (negative subtracts).")),
            "Returns <code>t</code> plus <code>n</code> minutes (TIME wraps within the day).");
        add("addseconds", "addseconds(t, n)", "TIME/TIMESTAMP",
            "Adds seconds.",
            List.of(p("t", "A TIME or TIMESTAMP."), p("n", "Number of seconds (negative subtracts).")),
            "Returns <code>t</code> plus <code>n</code> seconds (TIME wraps within the day).");
        add("diffdays", "diffdays(a, b)", "INT",
            "Whole days between two dates.",
            List.of(p("a", "A DATE or TIMESTAMP."), p("b", "A DATE or TIMESTAMP.")),
            "Returns the number of whole days from <code>b</code> to <code>a</code>.");
        add("diffseconds", "diffseconds(a, b)", "INT",
            "Seconds between two times.",
            List.of(p("a", "A TIME or TIMESTAMP."), p("b", "A TIME or TIMESTAMP.")),
            "Returns the number of seconds from <code>b</code> to <code>a</code>.");
    }

    /** Case-insensitive lookup. Returns {@code null} if no BIF matches. */
    public static @Nullable BbkBuiltinFunction find(@NotNull String name) {
        return BY_NAME.get(name.toLowerCase());
    }

    public static boolean isBuiltin(@NotNull String name) {
        return BY_NAME.containsKey(name.toLowerCase());
    }

    /** All BIFs in declaration order (string → numeric → array → date). */
    public static @NotNull List<BbkBuiltinFunction> all() {
        return List.copyOf(BY_NAME.values());
    }
}
