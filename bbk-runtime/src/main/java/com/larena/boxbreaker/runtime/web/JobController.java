package com.larena.boxbreaker.runtime.web;

import com.larena.boxbreaker.runtime.job.Job;
import com.larena.boxbreaker.runtime.job.JobRunner;
import com.larena.boxbreaker.runtime.job.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Job inquiry — the analogue of IBM&nbsp;i's {@code WRKACTJOB}. Read-only listing
 * of the jobs the runtime knows about, newest first. Requires the {@code *JOBCTL}
 * (job control) special authority, or {@code *ALLOBJ}.
 */
@RestController
@RequestMapping("/api/jobs")
@PreAuthorize("hasAuthority('JOBCTL') or hasAuthority('ALLOBJ')")
public class JobController {

    /** A submitted job sleeps this long by default, and never longer than the cap. */
    private static final long DEFAULT_SECONDS = 30L;
    private static final long MAX_SECONDS = 600L;

    private final JobService jobs;
    private final JobRunner runner;

    public JobController(JobService jobs, JobRunner runner) {
        this.jobs = jobs;
        this.runner = runner;
    }

    @GetMapping
    public List<JobResponse> list() {
        return jobs.list().stream().map(JobResponse::of).toList();
    }

    /**
     * Submit a job that simply waits, so it stays {@code ACTIVE} for a while (a
     * stand-in for real work). Returns immediately with the just-started job; the
     * job ends on its own after {@code seconds}. Lets you watch a live active job
     * in the WRKACTJOB-style listing.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse submit(@RequestBody(required = false) SubmitJobRequest request) {
        SubmitJobRequest r = request == null ? new SubmitJobRequest(null, null, null) : request;
        String name = (r.name() == null || r.name().isBlank()) ? "BBKWAIT" : r.name();
        long seconds = r.seconds() == null ? DEFAULT_SECONDS : Math.max(1, Math.min(r.seconds(), MAX_SECONDS));
        long millis = seconds * 1000L;

        Job job = runner.runAsync(name, r.user(), List.of(), () -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return JobResponse.of(job);
    }

    public record SubmitJobRequest(String name, String user, Long seconds) {}

    /** Flat view of a job for the viewer's grid. Timestamps are ISO-8601 UTC (or null). */
    public record JobResponse(String jobNumber, String name, String user,
                              String status, String createdAt, String endedAt) {
        static JobResponse of(Job j) {
            return new JobResponse(
                j.getJobNumber(), j.getName(), j.getUserProfile(),
                j.getStatus().name(), iso(j.getCreatedAt()), iso(j.getEndedAt()));
        }

        private static String iso(Instant when) {
            return when == null ? null : when.toString();
        }
    }
}
