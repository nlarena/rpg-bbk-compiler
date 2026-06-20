package com.larena.boxbreaker.runtime.job;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs a unit of work as a job's <em>thread of control</em>: the job is the
 * process-like owner of the context (persisted, with its library list), and the
 * work runs on its own thread inside that job's lifecycle.
 *
 * <pre>
 *   start (ACTIVE, STARTED event)  ─▶  run work on a thread  ─▶  end (ENDED / FAILED event)
 * </pre>
 *
 * <p>This is the simplest possible execution: start the job, run the work on a
 * thread, wait for it, end the job. Concurrency, scheduling and the real
 * client-side execution come later.
 */
@Service
public class JobRunner {

    private final JobService jobs;

    public JobRunner(JobService jobs) {
        this.jobs = jobs;
    }

    /**
     * Like {@link #run}, but does <em>not</em> wait: starts the job (so it is
     * persisted {@code ACTIVE}), runs {@code work} on a background daemon thread,
     * and ends the job when the work finishes. Returns the just-started job
     * immediately, so a caller (e.g. an HTTP request) can return while the job is
     * still active — and an inquiry like WRKACTJOB sees it {@code ACTIVE}.
     */
    public Job runAsync(String name, String user, List<String> libraries, Runnable work) {
        Job job = jobs.start(name, user, libraries);

        Thread thread = new Thread(() -> {
            String terminalEvent = "ENDED";
            try {
                work.run();
            } catch (Throwable t) {
                terminalEvent = "FAILED";
            }
            jobs.end(job.getJobNumber(), terminalEvent);
        }, "job-" + job.getJobNumber());
        thread.setDaemon(true);
        thread.start();

        return job;
    }

    /** Start a job, run {@code work} on its thread, and end the job. Returns the ended job. */
    public Job run(String name, String user, List<String> libraries, Runnable work) throws InterruptedException {
        Job job = jobs.start(name, user, libraries);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                work.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "job-" + job.getJobNumber());

        thread.start();
        thread.join();

        return jobs.end(job.getJobNumber(), failure.get() == null ? "ENDED" : "FAILED");
    }
}
