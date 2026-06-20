package com.larena.boxbreaker.runtime.file;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Acceso a datos a nivel registro sobre la tabla física de un {@link FileObject}
 * &mdash; la implementación, del lado del runtime, de los verbos de archivo del
 * RPG (WRITE / READ / CHAIN / UPDATE / DELETE), todos keyed por {@code RRN}
 * (relative record number, la clave de la tabla).
 *
 * <p>Es la capa que terminará llamando el programa BBK a través de la ABI de
 * acceso a datos. Hoy se expone por REST para poder ejercitarla. Los nombres de
 * columna se restringen al record format declarado (los {@link FileField}), que
 * ya son nombres de sistema validados &rarr; el SQL es seguro; los valores van
 * siempre como parámetros JDBC.
 */
@Service
public class RecordService {

    private static final int MAX_ROWS = 500;
    private static final String RRN = "RRN";

    private final FileObjectRepository files;
    private final JdbcTemplate jdbc;

    public RecordService(FileObjectRepository files, JdbcTemplate jdbc) {
        this.files = files;
        this.jdbc = jdbc;
    }

    /** READ secuencial: todos los registros, en orden de RRN (tope {@value #MAX_ROWS}). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> readAll(String library, String name, Integer limit) {
        FileObject file = require(library, name);
        int max = (limit == null || limit <= 0 || limit > MAX_ROWS) ? MAX_ROWS : limit;
        String sql = "SELECT " + selectColumns(file) + " FROM " + q(file.getTableName())
            + " ORDER BY " + q(RRN) + " LIMIT " + max;
        return jdbc.queryForList(sql);
    }

    /** CHAIN por RRN: lectura aleatoria por número de registro. */
    @Transactional(readOnly = true)
    public Map<String, Object> read(String library, String name, long rrn) {
        return readRow(require(library, name), rrn);
    }

    /**
     * CHAIN por clave: lectura aleatoria por valor de clave. Devuelve el primer
     * registro que matchea, en orden de clave (con clave no-única puede haber
     * varios). Sin coincidencia &rarr; 404 (el equivalente de {@code %found = off}).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> chain(String library, String name, Map<String, Object> keyValues) {
        FileObject file = require(library, name);
        List<KeyColumn> where = keyPrefix(file, keyValues);
        String sql = "SELECT " + selectColumns(file) + " FROM " + q(file.getTableName())
            + " WHERE " + whereClause(where) + " ORDER BY " + keyOrder(file) + " LIMIT 1";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, values(where));
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CHAIN sin coincidencia (%found = off) en " + file.qualifiedName());
        }
        return rows.get(0);
    }

    /**
     * READE / lectura por clave: todos los registros que matchean la clave (parcial)
     * dada, en orden de clave. La clave parcial debe usar los primeros campos de la
     * clave, en orden.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> readByKey(String library, String name, Map<String, Object> keyValues) {
        FileObject file = require(library, name);
        List<KeyColumn> where = keyPrefix(file, keyValues);
        String sql = "SELECT " + selectColumns(file) + " FROM " + q(file.getTableName())
            + " WHERE " + whereClause(where) + " ORDER BY " + keyOrder(file) + " LIMIT " + MAX_ROWS;
        return jdbc.queryForList(sql, values(where));
    }

    /** WRITE: inserta un registro y devuelve el registro creado (con su RRN). */
    @Transactional
    public Map<String, Object> write(String library, String name, Map<String, Object> values) {
        FileObject file = require(library, name);
        Map<String, Object> rec = onlyDeclared(values, fieldNames(file));
        if (rec.isEmpty()) throw badRequest("declará al menos un valor de campo");

        List<String> cols = new ArrayList<>(rec.keySet());
        String columnList = cols.stream().map(RecordService::q).collect(Collectors.joining(", "));
        String placeholders = cols.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + q(file.getTableName()) + " (" + columnList + ") VALUES (" + placeholders + ")";
        Object[] params = cols.stream().map(rec::get).toArray();

        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps;
        }, keys);

        long rrn = keys.getKey() == null ? 0L : keys.getKey().longValue();
        return readRow(file, rrn);
    }

    /** UPDATE: modifica el registro de RRN dado y devuelve el resultado. */
    @Transactional
    public Map<String, Object> update(String library, String name, long rrn, Map<String, Object> values) {
        FileObject file = require(library, name);
        Map<String, Object> rec = onlyDeclared(values, fieldNames(file));
        if (rec.isEmpty()) throw badRequest("declará al menos un campo a actualizar");

        List<String> cols = new ArrayList<>(rec.keySet());
        String setClause = cols.stream().map(c -> q(c) + " = ?").collect(Collectors.joining(", "));
        Object[] params = new Object[cols.size() + 1];
        for (int i = 0; i < cols.size(); i++) params[i] = rec.get(cols.get(i));
        params[cols.size()] = rrn;

        int affected = jdbc.update("UPDATE " + q(file.getTableName()) + " SET " + setClause
            + " WHERE " + q(RRN) + " = ?", params);
        if (affected == 0) throw noRecord(file, rrn);
        return readRow(file, rrn);
    }

    /** DELETE: elimina el registro de RRN dado. */
    @Transactional
    public void delete(String library, String name, long rrn) {
        FileObject file = require(library, name);
        int affected = jdbc.update("DELETE FROM " + q(file.getTableName()) + " WHERE " + q(RRN) + " = ?", rrn);
        if (affected == 0) throw noRecord(file, rrn);
    }

    // ----- helpers -----

    private Map<String, Object> readRow(FileObject file, long rrn) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT " + selectColumns(file) + " FROM " + q(file.getTableName()) + " WHERE " + q(RRN) + " = ?", rrn);
        if (rows.isEmpty()) throw noRecord(file, rrn);
        return rows.get(0);
    }

    private FileObject require(String library, String name) {
        String lib = library == null ? "" : library.trim().toUpperCase();
        String nm = name == null ? "" : name.trim().toUpperCase();
        return files.findByLibraryAndName(lib, nm)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "no existe el archivo " + lib + "/" + nm));
    }

    /** Una columna clave del WHERE, con su valor. */
    private record KeyColumn(String column, Object value) {}

    /** Campos clave del archivo, en orden de keyPosition. */
    private static List<FileField> orderedKeys(FileObject file) {
        return file.getFields().stream()
            .filter(FileField::isKey)
            .sorted(Comparator.comparingInt(FileField::getKeyPosition))
            .toList();
    }

    /** Lista de columnas de la clave para el ORDER BY (orden de acceso). */
    private static String keyOrder(FileObject file) {
        return orderedKeys(file).stream().map(f -> q(f.getName())).collect(Collectors.joining(", "));
    }

    /**
     * Valida que los campos provistos sean un <b>prefijo de la clave</b> (los
     * primeros, en orden) y arma las columnas del WHERE. Es la regla de RPG: el
     * acceso por clave usa la clave completa o una parcial por la izquierda.
     */
    private static List<KeyColumn> keyPrefix(FileObject file, Map<String, Object> keyValues) {
        List<FileField> keys = orderedKeys(file);
        if (keys.isEmpty()) throw badRequest("el archivo " + file.qualifiedName() + " no tiene clave");

        Map<String, Object> provided = new LinkedHashMap<>();
        if (keyValues != null) {
            for (Map.Entry<String, Object> e : keyValues.entrySet()) {
                provided.put(e.getKey().trim().toUpperCase(), e.getValue());
            }
        }
        if (provided.isEmpty()) throw badRequest("indicá al menos el primer campo de la clave");
        if (provided.size() > keys.size()) throw badRequest("la clave tiene " + keys.size() + " campo(s)");

        List<KeyColumn> where = new ArrayList<>();
        for (int i = 0; i < provided.size(); i++) {
            FileField kf = keys.get(i);
            if (!provided.containsKey(kf.getName())) {
                throw badRequest("la clave parcial debe usar los primeros campos clave en orden; falta " + kf.getName());
            }
            where.add(new KeyColumn(kf.getName(), provided.get(kf.getName())));
        }
        return where;
    }

    private static String whereClause(List<KeyColumn> cols) {
        return cols.stream().map(c -> q(c.column()) + " = ?").collect(Collectors.joining(" AND "));
    }

    private static Object[] values(List<KeyColumn> cols) {
        return cols.stream().map(KeyColumn::value).toArray();
    }

    /** Nombres de campo del record format (las columnas válidas, sin RRN). */
    private static Set<String> fieldNames(FileObject file) {
        Set<String> names = new LinkedHashSet<>();
        for (FileField f : file.getFields()) names.add(f.getName());
        return names;
    }

    /** Filtra el body a sólo los campos declarados (ignora RRN); rechaza campos desconocidos. */
    private static Map<String, Object> onlyDeclared(Map<String, Object> values, Set<String> allowed) {
        Map<String, Object> rec = new LinkedHashMap<>();
        if (values != null) {
            for (Map.Entry<String, Object> e : values.entrySet()) {
                String col = e.getKey() == null ? "" : e.getKey().trim().toUpperCase();
                if (col.equals(RRN)) continue;   // el RRN lo asigna el sistema
                if (!allowed.contains(col)) throw badRequest("campo desconocido: " + col);
                rec.put(col, e.getValue());
            }
        }
        return rec;
    }

    /** Lista de columnas para el SELECT: RRN + los campos declarados. */
    private static String selectColumns(FileObject file) {
        StringBuilder sb = new StringBuilder(q(RRN));
        for (FileField f : file.getFields()) sb.append(", ").append(q(f.getName()));
        return sb.toString();
    }

    private static String q(String ident) {
        return "`" + ident + "`";
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static ResponseStatusException noRecord(FileObject file, long rrn) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
            "no hay registro con RRN " + rrn + " en " + file.qualifiedName());
    }
}
