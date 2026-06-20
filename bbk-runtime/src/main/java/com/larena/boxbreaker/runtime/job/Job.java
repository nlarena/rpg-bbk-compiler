package com.larena.boxbreaker.runtime.job;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A unit of work in the runtime — the analogue of an IBM&nbsp;i job, and the
 * persisted "process instance" of the BPMS-inspired model. It is the session
 * context a BBK program runs within: it owns the library list, the user profile,
 * the lifecycle status, an event history (state transitions) and a flexible
 * attribute bag.
 *
 * <p>Carries a surrogate {@code id} (technical key) plus a {@code jobNumber}
 * (the IBM&nbsp;i-style business key).
 */
@Entity
@Table(name = "job")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_number", unique = true, nullable = false, length = 6)
    private String jobNumber;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_profile", nullable = false)
    private String userProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /** The {@code *LIBL}: ordered object-search path. */
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<JobLibrary> libraryList = new ArrayList<>();

    /** State-transition history (BPMS-style audit trail). */
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC")
    private List<JobEvent> events = new ArrayList<>();

    /** Flexible attribute bag (EAV) — add attributes without schema changes. */
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobAttribute> attributes = new ArrayList<>();

    protected Job() {}   // for JPA

    public Job(String jobNumber, String name, String userProfile, Instant createdAt) {
        this.jobNumber = jobNumber;
        this.name = name;
        this.userProfile = userProfile;
        this.createdAt = createdAt;
    }

    // ----- domain behaviour -----

    /** Qualified name in IBM&nbsp;i form {@code number/user/name}. */
    public String qualifiedName() { return jobNumber + "/" + userProfile + "/" + name; }

    public JobLibrary addLibrary(String library) {
        JobLibrary entry = new JobLibrary(this, libraryList.size(), library);
        libraryList.add(entry);
        return entry;
    }

    public JobEvent recordEvent(String eventType, Instant when) {
        JobEvent event = new JobEvent(this, eventType, when);
        events.add(event);
        return event;
    }

    public JobAttribute putAttribute(String name, String value) {
        JobAttribute attr = new JobAttribute(this, name, value);
        attributes.add(attr);
        return attr;
    }

    public void end(Instant when) {
        this.status = JobStatus.ENDED;
        this.endedAt = when;
    }

    // ----- accessors -----

    public Long getId() { return id; }
    public String getJobNumber() { return jobNumber; }
    public String getName() { return name; }
    public String getUserProfile() { return userProfile; }
    public JobStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEndedAt() { return endedAt; }
    public List<JobLibrary> getLibraryList() { return libraryList; }
    public List<JobEvent> getEvents() { return events; }
    public List<JobAttribute> getAttributes() { return attributes; }
}
