package com.larena.boxbreaker.jvmdebug;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Traduce entre las líneas BBK del fuente y las ubicaciones ({@link Location}) del
 * bytecode, en los dos sentidos:
 *
 * <ul>
 *   <li><b>forward</b> (BBK &rarr; bytecode): para instalar un breakpoint en una línea
 *       BBK hace falta el {@link Location} de esa línea &mdash; {@link #locationsOfLine}.</li>
 *   <li><b>reverse</b> (bytecode &rarr; BBK): cuando un evento JDI frena en un
 *       {@link Location}, hay que ubicarlo en el fuente &mdash; {@link #positionOf}.</li>
 * </ul>
 *
 * <p>Funciona porque {@code JvmCompiler} ya emite el {@code LineNumberTable} con las
 * líneas <em>de BBK</em> y el atributo {@code SourceFile}. Cubre todos los métodos de
 * la clase (mainline + procedimientos), ya que {@code locationsOfLine} busca en toda
 * la {@link ReferenceType}.
 */
public final class BbkLineMap {

    private final ReferenceType type;

    public BbkLineMap(ReferenceType type) {
        this.type = type;
    }

    /** Líneas BBK que tienen código: donde se puede frenar o poner un breakpoint. */
    public Set<Integer> executableLines() {
        Set<Integer> lines = new TreeSet<>();
        try {
            for (Location loc : type.allLineLocations()) lines.add(loc.lineNumber());
        } catch (AbsentInformationException e) {
            // sin LineNumberTable no hay líneas mapeables
        }
        return lines;
    }

    /** Forward: los {@link Location} de la clase para una línea BBK (vacío si la línea no tiene código). */
    public List<Location> locationsOfLine(int bbkLine) {
        try {
            return type.locationsOfLine(bbkLine);
        } catch (AbsentInformationException e) {
            return List.of();
        }
    }

    /** ¿Se puede frenar en esta línea BBK? (es decir, ¿tiene código?) */
    public boolean hasCodeAt(int bbkLine) {
        return !locationsOfLine(bbkLine).isEmpty();
    }

    /** Reverse: la posición BBK (archivo, línea) de un {@link Location} de un evento JDI. */
    public static BbkPosition positionOf(Location location) {
        String file = null;
        try {
            file = location.sourceName();       // el SourceFile de la clase, p.ej. "Main.bbk"
        } catch (AbsentInformationException ignored) {
            // clase sin SourceFile: dejamos el archivo en null
        }
        return new BbkPosition(file, location.lineNumber());
    }
}
