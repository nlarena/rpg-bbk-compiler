package com.larena.boxbreaker.runtime.file;

/**
 * Tipo de un campo del formato de registro, modelado sobre los tipos de datos
 * DDS del IBM&nbsp;i (simplificado). Cada uno sabe a qu&eacute; columna SQL baja
 * cuando se crea la tabla f&iacute;sica.
 */
public enum FieldType {
    CHAR,      // A en DDS  -> VARCHAR(n)
    DECIMAL,   // P/S en DDS -> DECIMAL(n,d)  (empaquetado / zonado)
    INTEGER,   // B en DDS  -> INT
    DATE;      // L en DDS  -> DATE

    /** Columna SQL para este tipo, dados el largo y los decimales declarados. */
    public String toSqlColumnType(int length, int decimals) {
        switch (this) {
            case CHAR:    return "VARCHAR(" + Math.max(1, length) + ")";
            case DECIMAL: return "DECIMAL(" + Math.max(1, length) + "," + Math.max(0, decimals) + ")";
            case INTEGER: return "INT";
            case DATE:    return "DATE";
            default:      throw new IllegalStateException("tipo no soportado: " + this);
        }
    }

    /** True si el tipo usa el atributo de largo (CHAR / DECIMAL). */
    public boolean usesLength() {
        return this == CHAR || this == DECIMAL;
    }
}
