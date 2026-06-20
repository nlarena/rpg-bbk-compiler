package com.larena.boxbreaker.runtime.web;

import com.larena.boxbreaker.runtime.file.RecordService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos a nivel registro de un archivo (los verbos de archivo del RPG,
 * keyed por RRN): READ secuencial, CHAIN (por RRN), WRITE, UPDATE, DELETE. Sólo
 * exige estar autenticado.
 */
@RestController
@RequestMapping("/api/files/{library}/{name}/records")
public class RecordController {

    private final RecordService records;

    public RecordController(RecordService records) {
        this.records = records;
    }

    /** READ secuencial: todos los registros (orden de RRN). */
    @GetMapping
    public List<Map<String, Object>> list(@PathVariable String library, @PathVariable String name,
                                          @RequestParam(required = false) Integer limit) {
        return records.readAll(library, name, limit);
    }

    /** CHAIN por clave: el primer registro que matchea la clave dada (404 si no hay). */
    @GetMapping("/chain")
    public Map<String, Object> chain(@PathVariable String library, @PathVariable String name,
                                     @RequestParam Map<String, String> key) {
        return records.chain(library, name, new LinkedHashMap<>(key));
    }

    /** READE / lectura por clave: todos los registros de la clave (parcial) dada, en orden de clave. */
    @GetMapping("/key")
    public List<Map<String, Object>> readByKey(@PathVariable String library, @PathVariable String name,
                                               @RequestParam Map<String, String> key) {
        return records.readByKey(library, name, new LinkedHashMap<>(key));
    }

    /** CHAIN por RRN: lectura aleatoria por número de registro. */
    @GetMapping("/{rrn}")
    public Map<String, Object> read(@PathVariable String library, @PathVariable String name,
                                    @PathVariable long rrn) {
        return records.read(library, name, rrn);
    }

    /** WRITE: inserta un registro. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> write(@PathVariable String library, @PathVariable String name,
                                     @RequestBody Map<String, Object> record) {
        return records.write(library, name, record);
    }

    /** UPDATE: modifica el registro de RRN dado. */
    @PutMapping("/{rrn}")
    public Map<String, Object> update(@PathVariable String library, @PathVariable String name,
                                      @PathVariable long rrn, @RequestBody Map<String, Object> record) {
        return records.update(library, name, rrn, record);
    }

    /** DELETE: elimina el registro de RRN dado. */
    @DeleteMapping("/{rrn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String library, @PathVariable String name, @PathVariable long rrn) {
        records.delete(library, name, rrn);
    }
}
