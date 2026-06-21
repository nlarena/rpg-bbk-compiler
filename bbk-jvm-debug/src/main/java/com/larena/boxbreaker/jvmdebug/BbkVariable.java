package com.larena.boxbreaker.jvmdebug;

import java.util.List;

/**
 * Una variable BBK en el panel de debug. Es un escalar (con {@code value} y sin hijos)
 * o una estructura de datos (con {@code value} null y sus subcampos en {@code children}).
 *
 * @param name     nombre BBK (ya sin el mangling {@code $} del bytecode)
 * @param value    valor formateado estilo BBK, o {@code null} si es una DS (compuesta)
 * @param children subcampos, si es una DS; vacío para un escalar
 */
public record BbkVariable(String name, String value, List<BbkVariable> children) {

    /** ¿Es una estructura (tiene subcampos) y no un escalar? */
    public boolean isComposite() {
        return value == null;
    }
}
