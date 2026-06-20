package com.larena.boxbreaker.runtime.job;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Job lifecycle on top of {@link JobRepository}: start a job (persisted, {@code ACTIVE},
 * with a {@code STARTED} event) and end it ({@code ENDED}, with a terminal event).
 * Every state transition is recorded as a {@link JobEvent} (the BPMS-style audit trail).
 */
@Service
public class JobService {

    private static final long FIRST_NUMBER = 100_001L;

    private final JobRepository repo;
    private final AtomicLong nextNumber = new AtomicLong(0);   // 0 = not yet seeded from the DB

    public JobService(JobRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Job start(String name, String user, List<String> libraries) {
        Job job = new Job(nextJobNumber(), requireText(name, "job name"), userOrDefault(user), Instant.now());
        if (libraries != null) {
            for (String lib : libraries) job.addLibrary(lib.trim().toUpperCase());
        }
        job.recordEvent("STARTED", Instant.now());
        return repo.save(job);
    }

    @Transactional
    public Job end(String jobNumber, String terminalEvent) {
        Job job = require(jobNumber);
        Instant now = Instant.now();
        job.end(now);
        job.recordEvent(terminalEvent, now);
        return repo.save(job);
    }

    /** All jobs, newest first (WRKACTJOB-style listing). */
    @Transactional(readOnly = true)
    public List<Job> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Job require(String jobNumber) {
        return repo.findByJobNumber(jobNumber)
            .orElseThrow(() -> new IllegalArgumentException("no job with number '" + jobNumber + "'"));
    }

    /** Next 6-digit job number, continuing from whatever is already in the DB. */
    private synchronized String nextJobNumber() {
        if (nextNumber.get() == 0) {
            long base = repo.findTopByOrderByJobNumberDesc()
                .map(j -> Long.parseLong(j.getJobNumber()))
                .orElse(FIRST_NUMBER - 1);
            nextNumber.set(base);
        }
        return String.format("%06d", nextNumber.incrementAndGet());
    }

    private static String userOrDefault(String user) {
        return (user == null || user.isBlank()) ? "QUSER" : user.trim().toUpperCase();
    }

    private static String requireText(String value, String what) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(what + " must not be blank");
        return value.trim().toUpperCase();
    }
}
