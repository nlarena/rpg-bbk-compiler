package com.larena.boxbreaker.runtime.web;

import com.larena.boxbreaker.runtime.file.FieldType;
import com.larena.boxbreaker.runtime.file.FileField;
import com.larena.boxbreaker.runtime.file.FileObject;
import com.larena.boxbreaker.runtime.file.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Declaraci&oacute;n de archivos de base de datos (physical files) &mdash; crear
 * (CRTPF-style) y listar. Requiere {@code *ALLOBJ}: crear objetos es privilegiado.
 */
@RestController
@RequestMapping("/api/files")
@PreAuthorize("hasAuthority('ALLOBJ')")
public class FileController {

    private final FileService files;

    public FileController(FileService files) {
        this.files = files;
    }

    @GetMapping
    public List<FileResponse> list() {
        return files.list().stream().map(FileResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse declare(@RequestBody DeclareFileRequest request) {
        FileObject file = files.declare(request.name(), request.library(), request.text(), toSpecs(request.fields()));
        return FileResponse.of(file);
    }

    @PutMapping("/{library}/{name}")
    public FileResponse modify(@PathVariable String library, @PathVariable String name,
                               @RequestBody ModifyFileRequest request) {
        FileObject file = files.modify(library, name, request.text(), toSpecs(request.fields()));
        return FileResponse.of(file);
    }

    private static List<FileService.FieldSpec> toSpecs(List<FieldDef> fields) {
        return (fields == null ? List.<FieldDef>of() : fields).stream()
            .map(f -> new FileService.FieldSpec(f.name(), parseType(f.type()), nz(f.length()), nz(f.decimals())))
            .toList();
    }

    private static FieldType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "falta el tipo de campo");
        }
        try {
            return FieldType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo de campo desconocido: " + raw);
        }
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }

    // ----- DTOs -----

    public record DeclareFileRequest(String name, String library, String text, List<FieldDef> fields) {}

    public record ModifyFileRequest(String text, List<FieldDef> fields) {}

    public record FieldDef(String name, String type, Integer length, Integer decimals) {}

    public record FileResponse(String name, String library, String text, String tableName,
                               String createdAt, List<FieldResponse> fields) {
        static FileResponse of(FileObject f) {
            return new FileResponse(
                f.getName(), f.getLibrary(), f.getDescription(), f.getTableName(),
                iso(f.getCreatedAt()),
                f.getFields().stream().map(FieldResponse::of).toList());
        }

        private static String iso(Instant when) { return when == null ? null : when.toString(); }
    }

    public record FieldResponse(String name, String type, int length, int decimals) {
        static FieldResponse of(FileField f) {
            return new FieldResponse(f.getName(), f.getType().name(), f.getLength(), f.getDecimals());
        }
    }
}
