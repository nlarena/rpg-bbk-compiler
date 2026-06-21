package com.larena.boxbreaker.jvmdebug;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * Convierte un valor de la JVM a la vista BBK: como texto para el panel
 * ({@link #format}) o como valor tipado de Java para el evaluador ({@link #toJava}).
 * Los enteros BBK son {@code long}, los decimales {@code BigDecimal}, los
 * indicadores/booleanos {@code boolean} ({@code *ON}/{@code *OFF}), los char/varchar
 * {@code String}.
 */
final class BbkValues {

    private BbkValues() {}

    /** Texto estilo BBK para el panel de variables. */
    static String format(ThreadReference thread, Value v) {
        if (v == null) return "*NULL";
        if (v instanceof LongValue l) return Long.toString(l.value());
        if (v instanceof IntegerValue i) return Integer.toString(i.value());
        if (v instanceof BooleanValue b) return b.value() ? "*ON" : "*OFF";
        if (v instanceof DoubleValue d) return Double.toString(d.value());
        if (v instanceof StringReference s) return s.value();
        if (v instanceof ArrayReference a) return "[" + a.length() + "]";
        if (v instanceof ObjectReference o) {
            String dec = bigDecimal(thread, o);
            return dec != null ? dec : o.referenceType().name();
        }
        return v.toString();
    }

    /** Valor tipado de Java (Long/Double/BigDecimal/Boolean/String) para evaluar expresiones. */
    static Object toJava(ThreadReference thread, Value v) {
        if (v == null) return null;
        if (v instanceof LongValue l) return l.value();
        if (v instanceof IntegerValue i) return (long) i.value();
        if (v instanceof BooleanValue b) return b.value();
        if (v instanceof DoubleValue d) return d.value();
        if (v instanceof StringReference s) return s.value();
        if (v instanceof ObjectReference o) {
            String dec = bigDecimal(thread, o);
            if (dec != null) return new BigDecimal(dec);
        }
        return null;   // arrays/otros objetos: no evaluables como escalar
    }

    /** Si {@code o} es un BigDecimal, su {@code toPlainString} (con escala), invocado por JDI. */
    private static String bigDecimal(ThreadReference thread, ObjectReference o) {
        if (!"java.math.BigDecimal".equals(o.referenceType().name()) || !(o.referenceType() instanceof ClassType ct)) {
            return null;
        }
        Method m = ct.concreteMethodByName("toPlainString", "()Ljava/lang/String;");
        if (m == null) return null;
        try {
            Value r = o.invokeMethod(thread, m, List.of(), ObjectReference.INVOKE_SINGLE_THREADED);
            return r instanceof StringReference s ? s.value() : null;
        } catch (Exception e) {
            return null;   // si la invocación falla, no formateamos el decimal
        }
    }
}
