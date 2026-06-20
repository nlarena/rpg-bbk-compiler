package com.larena.boxbreaker.runtime.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One state-transition event in a job's life (BPMS-style audit trail):
 * {@code STARTED}, {@code ENDED}, and whatever lifecycle events we add later.
 * {@code eventType} is a free string so new event kinds need no schema change.
 */
@Entity
@Table(name = "job_event", indexes = @Index(name = "ix_job_event_job", columnList = "job_id"))
public class JobEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected JobEvent() {}   // for JPA

    public JobEvent(Job job, String eventType, Instant occurredAt) {
        this.job = job;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
}
