package com.larena.boxbreaker.debugger;

import com.larena.boxbreaker.core.ast.BbkItem;

import java.util.IdentityHashMap;

/** Archivo + línea de cada item, combinando las posiciones de todos los fuentes. */
final class Locations {

    private final IdentityHashMap<BbkItem, String> files;
    private final IdentityHashMap<BbkItem, Integer> lines;

    Locations(IdentityHashMap<BbkItem, String> files, IdentityHashMap<BbkItem, Integer> lines) {
        this.files = files;
        this.lines = lines;
    }

    String fileOf(BbkItem item) {
        String f = files.get(item);
        return f == null ? "" : f;
    }

    int lineOf(BbkItem item) {
        Integer l = lines.get(item);
        return l == null ? 0 : l;
    }
}
