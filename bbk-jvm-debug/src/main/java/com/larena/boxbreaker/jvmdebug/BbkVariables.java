package com.larena.boxbreaker.jvmdebug;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee las variables visibles en un frame y las mapea a la vista BBK: locales del
 * frame (por el LocalVariableTable) + campos estáticos de la clase (globales y
 * subcampos de DS). Revierte el mangling {@code ds$sub} a un árbol &mdash; la DS como
 * nodo padre con sus subcampos &mdash; y formatea cada valor estilo BBK.
 */
final class BbkVariables {

    private BbkVariables() {}

    static List<BbkVariable> read(ThreadReference thread, StackFrame frame) {
        return group(thread, rawValues(frame));
    }

    /** Valores crudos visibles en el frame: locales (LocalVariableTable) + campos estáticos de la clase. */
    static Map<String, Value> rawValues(StackFrame frame) {
        Map<String, Value> raw = new LinkedHashMap<>();
        try {
            for (LocalVariable lv : frame.visibleVariables()) raw.put(lv.name(), frame.getValue(lv));
        } catch (AbsentInformationException e) {
            // sin LocalVariableTable: no hay locales nombradas
        }
        ReferenceType type = frame.location().declaringType();
        for (Field f : type.fields()) {
            if (f.isStatic() && !raw.containsKey(f.name())) raw.put(f.name(), type.getValue(f));
        }
        return raw;
    }

    /** Agrupa los nombres mangleados {@code ds$sub} bajo un nodo DS y formatea los valores. */
    private static List<BbkVariable> group(ThreadReference thread, Map<String, Value> raw) {
        List<BbkVariable> out = new ArrayList<>();
        Map<String, List<BbkVariable>> dsChildren = new LinkedHashMap<>();
        for (Map.Entry<String, Value> e : raw.entrySet()) {
            String name = e.getKey();
            String value = BbkValues.format(thread, e.getValue());
            int dollar = name.indexOf('$');
            if (dollar > 0) {
                String ds = name.substring(0, dollar);
                String member = name.substring(dollar + 1);
                dsChildren.computeIfAbsent(ds, k -> {
                    List<BbkVariable> children = new ArrayList<>();
                    out.add(new BbkVariable(ds, null, children));   // nodo DS, sus hijos se van agregando
                    return children;
                }).add(new BbkVariable(member, value, List.of()));
            } else {
                out.add(new BbkVariable(name, value, List.of()));
            }
        }
        return out;
    }
}
