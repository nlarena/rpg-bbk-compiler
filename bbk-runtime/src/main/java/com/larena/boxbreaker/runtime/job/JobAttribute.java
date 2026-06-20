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
 * A flexible job attribute (EAV — entity/attribute/value), the BPMS "variables"
 * pattern: the long tail of job settings (decimal format, date format, CCSID, …)
 * lives here as name/value rows, so new attributes are added <em>without</em>
 * schema changes. One value per (job, name).
 */
@Entity
@Table(name = "job_attribute", uniqueConstraints =
    @UniqueConstraint(name = "uq_job_attribute_name", columnNames = {"job_id", "name"}))
public class JobAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "value", length = 512)
    private String value;

    protected JobAttribute() {}   // for JPA

    public JobAttribute(Job job, String name, String value) {
        this.job = job;
        this.name = name;
        this.value = value;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getValue() { return value; }
}
