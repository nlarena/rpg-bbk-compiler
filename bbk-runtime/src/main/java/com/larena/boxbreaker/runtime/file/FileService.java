package com.larena.boxbreaker.runtime.file;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Declaración y modificación de archivos de base de datos (physical files).
 * Persiste la definición ({@link FileObject} + {@link FileField}) y mantiene en
 * sincronía la tabla física real en MySQL: {@code CREATE TABLE} al declarar,
 * {@code ALTER TABLE} al cambiar el formato de registro.
 *
 * <p>Los nombres se validan con la convención de nombres del sistema IBM&nbsp;i
 * (&le;10 chars, empieza con letra) &mdash; eso además los hace <b>seguros para
 * construir el DDL</b> (no hay forma de inyectar SQL).
 */
@Service
public class FileService {

    /** Nombre de sistema IBM i: letra inicial + hasta 9 de [A-Z0-9_]. */
    private static final Pattern SYSTEM_NAME = Pattern.compile("^[A-Z][A-Z0-9_]{0,9}$");

    private final FileObjectRepository repo;
    private final JdbcTemplate jdbc;

    public FileService(FileObjectRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    /** Nombre del índice que materializa la clave de acceso (el access path). */
    private static final String KEY_INDEX = "k_access";

    /** Especificación de un campo a declarar (entrada del servicio). */
    public record FieldSpec(String name, FieldType type, int length, int decimals, int keyPosition) {}

    @Transactional
    public FileObject declare(String rawName, String rawLibrary, String description, List<FieldSpec> fields) {
        String name = requireSystemName(rawName, "archivo");
        String library = requireSystemName(rawLibrary, "biblioteca");
        List<FieldSpec> norm = normalize(fields);

        if (repo.existsByLibraryAndName(library, name)) {
            throw conflict("el archivo " + library + "/" + name + " ya existe");
        }

        String tableName = library + "_" + name;   // ambos ya validados -> identificador seguro
        if (tableExists(tableName)) {
            throw conflict("la tabla física " + tableName + " ya existe");
        }

        // 1) Crear la tabla física real (DDL: en MySQL hace commit implícito).
        jdbc.execute(buildCreateTable(tableName, norm));

        // 2) Crear el índice de la clave de acceso (si hay campos clave).
        createKeyIndex(tableName, norm);

        // 3) Persistir la definición.
        FileObject file = new FileObject(name, library, blankToNull(description), tableName, Instant.now());
        for (FieldSpec f : norm) file.addField(f.name(), f.type(), f.length(), f.decimals(), f.keyPosition());
        return repo.save(file);
    }

    /**
     * Modifica el formato de registro de un archivo existente (CHGPF): difiere los
     * campos actuales contra los nuevos y aplica un único {@code ALTER TABLE} con
     * los DROP/ADD/MODIFY necesarios. El archivo y la biblioteca (la identidad) no
     * se cambian acá.
     */
    @Transactional
    public FileObject modify(String rawLibrary, String rawName, String description, List<FieldSpec> fields) {
        String library = requireSystemName(rawLibrary, "biblioteca");
        String name = requireSystemName(rawName, "archivo");
        FileObject file = repo.findByLibraryAndName(library, name)
            .orElseThrow(() -> notFound("no existe el archivo " + library + "/" + name));

        List<FieldSpec> norm = normalize(fields);
        String table = file.getTableName();

        Map<String, FileField> oldByName = new LinkedHashMap<>();
        for (FileField f : file.getFields()) oldByName.put(f.getName(), f);
        Set<String> newNames = new LinkedHashSet<>();
        for (FieldSpec f : norm) newNames.add(f.name());

        // Un solo ALTER con todas las acciones -> atómico (si falla, no aplica nada).
        List<String> actions = new ArrayList<>();
        for (FileField old : file.getFields()) {
            if (!newNames.contains(old.getName())) {
                actions.add("DROP COLUMN " + quoteIdent(old.getName()));
            }
        }
        for (FieldSpec f : norm) {
            String colType = f.type().toSqlColumnType(f.length(), f.decimals());
            FileField old = oldByName.get(f.name());
            if (old == null) {
                actions.add("ADD COLUMN " + quoteIdent(f.name()) + " " + colType);
            } else if (!old.getType().toSqlColumnType(old.getLength(), old.getDecimals()).equals(colType)) {
                actions.add("MODIFY COLUMN " + quoteIdent(f.name()) + " " + colType);
            }
        }
        if (!actions.isEmpty()) {
            jdbc.execute("ALTER TABLE " + quoteIdent(table) + " " + String.join(", ", actions));
        }

        // Refrescar el índice de la clave de acceso (drop + recrear según la nueva clave).
        dropKeyIndexIfExists(table);
        createKeyIndex(table, norm);

        // Reemplazar la definición de campos y el texto.
        file.getFields().clear();
        for (FieldSpec f : norm) file.addField(f.name(), f.type(), f.length(), f.decimals(), f.keyPosition());
        file.setDescription(blankToNull(description));
        return repo.save(file);
    }

    @Transactional(readOnly = true)
    public List<FileObject> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    // ----- helpers -----

    /** Valida y normaliza (a mayúsculas) los campos; nombres únicos, largos/decimales coherentes. */
    private List<FieldSpec> normalize(List<FieldSpec> fields) {
        if (fields == null || fields.isEmpty()) {
            throw badRequest("declará al menos un campo");
        }
        Set<String> seen = new LinkedHashSet<>();
        List<FieldSpec> result = new ArrayList<>();
        for (FieldSpec f : fields) {
            String fn = requireSystemName(f.name(), "campo");
            if (!seen.add(fn)) throw badRequest("campo duplicado: " + fn);
            if (f.type() == null) throw badRequest("campo " + fn + ": falta el tipo");
            if (f.type().usesLength() && f.length() < 1) {
                throw badRequest("campo " + fn + ": el largo debe ser > 0");
            }
            if (f.type() == FieldType.DECIMAL && (f.decimals() < 0 || f.decimals() > f.length())) {
                throw badRequest("campo " + fn + ": decimales inválidos (0.." + f.length() + ")");
            }
            if (f.keyPosition() < 0) {
                throw badRequest("campo " + fn + ": posición de clave inválida");
            }
            result.add(new FieldSpec(fn, f.type(), f.length(), f.decimals(), f.keyPosition()));
        }
        return result;
    }

    /** Campos clave (keyPosition > 0), ordenados por su posición en la clave. */
    private static List<FieldSpec> keyFields(List<FieldSpec> fields) {
        return fields.stream()
            .filter(f -> f.keyPosition() > 0)
            .sorted(java.util.Comparator.comparingInt(FieldSpec::keyPosition))
            .toList();
    }

    /** Crea el índice de la clave de acceso (no-único) sobre los campos clave, si hay. */
    private void createKeyIndex(String tableName, List<FieldSpec> fields) {
        List<FieldSpec> keys = keyFields(fields);
        if (keys.isEmpty()) return;
        String cols = keys.stream().map(f -> quoteIdent(f.name())).collect(java.util.stream.Collectors.joining(", "));
        jdbc.execute("CREATE INDEX " + quoteIdent(KEY_INDEX) + " ON " + quoteIdent(tableName) + " (" + cols + ")");
    }

    private void dropKeyIndexIfExists(String tableName) {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
            Integer.class, tableName, KEY_INDEX);
        if (n != null && n > 0) {
            jdbc.execute("DROP INDEX " + quoteIdent(KEY_INDEX) + " ON " + quoteIdent(tableName));
        }
    }

    private boolean tableExists(String tableName) {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
            Integer.class, tableName);
        return n != null && n > 0;
    }

    private static String buildCreateTable(String tableName, List<FieldSpec> fields) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ").append(quoteIdent(tableName)).append(" (\n");
        // RRN: relative record number, como la clave física del registro.
        sb.append("  ").append(quoteIdent("RRN")).append(" BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY");
        for (FieldSpec f : fields) {
            sb.append(",\n  ").append(quoteIdent(f.name())).append(" ")
              .append(f.type().toSqlColumnType(f.length(), f.decimals()));
        }
        sb.append("\n)");
        return sb.toString();
    }

    /** Comilla un identificador. Los nombres ya pasaron por {@link #SYSTEM_NAME}, así que es seguro. */
    private static String quoteIdent(String ident) {
        return "`" + ident + "`";
    }

    private static String requireSystemName(String raw, String what) {
        String value = raw == null ? "" : raw.trim().toUpperCase();
        if (!SYSTEM_NAME.matcher(value).matches()) {
            throw badRequest("nombre de " + what + " inválido: '" + raw
                + "' (1-10 chars, empieza con letra, solo A-Z 0-9 _)");
        }
        return value;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }
}
