package com.larena.boxbreaker.runtime.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Un campo del formato de registro de un {@link FileObject} (la columna de la
 * tabla f&iacute;sica). Conserva el orden declarado v&iacute;a {@code position}.
 */
@Entity
@Table(name = "file_field")
public class FileField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileObject file;

    @Column(nullable = false, length = 10)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FieldType type;

    @Column(name = "field_length", nullable = false)
    private int length;

    @Column(name = "decimal_positions", nullable = false)
    private int decimals;

    @Column(nullable = false)
    private int position;

    /** Posición en la clave de acceso (0 = no es campo clave; 1..n = orden en la clave). */
    @Column(name = "key_position", nullable = false)
    private int keyPosition;

    protected FileField() {}   // for JPA

    FileField(FileObject file, int position, String name, FieldType type, int length, int decimals, int keyPosition) {
        this.file = file;
        this.position = position;
        this.name = name;
        this.type = type;
        this.length = length;
        this.decimals = decimals;
        this.keyPosition = keyPosition;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public FieldType getType() { return type; }
    public int getLength() { return length; }
    public int getDecimals() { return decimals; }
    public int getPosition() { return position; }
    public int getKeyPosition() { return keyPosition; }
    public boolean isKey() { return keyPosition > 0; }
}
