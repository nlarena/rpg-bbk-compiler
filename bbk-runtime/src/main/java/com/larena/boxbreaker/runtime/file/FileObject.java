package com.larena.boxbreaker.runtime.file;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Un archivo de base de datos &mdash; el an&aacute;logo de un <em>physical
 * file</em> (PF) del IBM&nbsp;i: un objeto en una biblioteca, con un formato de
 * registro (la lista de {@link FileField campos}). Al declararlo, el runtime crea
 * adem&aacute;s la tabla f&iacute;sica real ({@code tableName}).
 *
 * <p>Clave de negocio: {@code (library, name)} &mdash; el nombre calificado
 * estilo IBM&nbsp;i {@code biblioteca/archivo}.
 */
@Entity
@Table(name = "file_object",
       uniqueConstraints = @UniqueConstraint(name = "uk_file_lib_name", columnNames = {"library", "name"}))
public class FileObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false, length = 10)
    private String library;

    @Column(name = "description", length = 50)
    private String description;

    /** Nombre de la tabla f&iacute;sica real creada para este archivo. */
    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<FileField> fields = new ArrayList<>();

    protected FileObject() {}   // for JPA

    public FileObject(String name, String library, String description, String tableName, Instant createdAt) {
        this.name = name;
        this.library = library;
        this.description = description;
        this.tableName = tableName;
        this.createdAt = createdAt;
    }

    /** Nombre calificado en forma IBM&nbsp;i {@code biblioteca/archivo}. */
    public String qualifiedName() { return library + "/" + name; }

    public FileField addField(String name, FieldType type, int length, int decimals) {
        FileField field = new FileField(this, fields.size(), name, type, length, decimals);
        fields.add(field);
        return field;
    }

    public void setDescription(String description) { this.description = description; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLibrary() { return library; }
    public String getDescription() { return description; }
    public String getTableName() { return tableName; }
    public Instant getCreatedAt() { return createdAt; }
    public List<FileField> getFields() { return fields; }
}
