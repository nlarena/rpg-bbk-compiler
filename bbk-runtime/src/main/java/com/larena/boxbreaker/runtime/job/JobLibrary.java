package com.larena.boxbreaker.runtime.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One entry of a job's library list (the {@code *LIBL}). The {@code position}
 * is the search order (first match wins), so the list is an <em>ordered</em>
 * one-to-many — that is why it lives in its own table rather than a column.
 * A library appears at most once per job.
 */
@Entity
@Table(name = "job_library", uniqueConstraints = {
    @UniqueConstraint(name = "uq_job_library_position", columnNames = {"job_id", "position"}),
    @UniqueConstraint(name = "uq_job_library_name", columnNames = {"job_id", "library"})
})
public class JobLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 10)
    private String library;

    protected JobLibrary() {}   // for JPA

    public JobLibrary(Job job, int position, String library) {
        this.job = job;
        this.position = position;
        this.library = library;
    }

    public Long getId() { return id; }
    public int getPosition() { return position; }
    public String getLibrary() { return library; }
}
