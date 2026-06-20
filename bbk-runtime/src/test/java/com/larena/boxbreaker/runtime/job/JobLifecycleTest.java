package com.larena.boxbreaker.runtime.job;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end against MySQL: running a simple thread takes a job through its full
 * lifecycle (ACTIVE → ENDED), persists it with its library list, and records the
 * STARTED/ENDED transition events.
 */
@SpringBootTest
class JobLifecycleTest {

    @Autowired JobRunner runner;
    @Autowired JobRepository repo;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void runsASimpleThreadThroughTheJobLifecycle() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);

        Job job = runner.run("payroll", "nico", List.of("QGPL", "MYAPP"), () -> ran.set(true));

        // the thread actually executed, and the returned job is ended
        assertTrue(ran.get(), "the job's thread of control should have run");
        assertEquals(JobStatus.ENDED, job.getStatus());
        assertNotNull(job.getEndedAt());
        assertEquals(job.getJobNumber() + "/NICO/PAYROLL", job.qualifiedName());

        // re-read from MySQL inside a transaction (lazy collections) and verify persistence
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(s -> {
            Job stored = repo.findByJobNumber(job.getJobNumber()).orElseThrow();
            assertEquals(JobStatus.ENDED, stored.getStatus());
            assertEquals(List.of("QGPL", "MYAPP"),
                stored.getLibraryList().stream().map(JobLibrary::getLibrary).toList());
            List<String> events = stored.getEvents().stream().map(JobEvent::getEventType).toList();
            assertTrue(events.contains("STARTED"), "should record STARTED");
            assertTrue(events.contains("ENDED"), "should record ENDED");
        });

        // tidy up the dev DB (cascade removes library list + events)
        repo.deleteById(job.getId());
    }
}
