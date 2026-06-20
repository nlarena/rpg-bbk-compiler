package com.larena.boxbreaker.runtime.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Persistencia de las definiciones de {@link FileObject} (y sus campos via cascade). */
public interface FileObjectRepository extends JpaRepository<FileObject, Long> {

    boolean existsByLibraryAndName(String library, String name);

    Optional<FileObject> findByLibraryAndName(String library, String name);

    /** Todos los archivos declarados, m&aacute;s recientes primero. */
    List<FileObject> findAllByOrderByCreatedAtDesc();
}
