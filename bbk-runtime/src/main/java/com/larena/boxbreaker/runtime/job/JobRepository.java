package com.larena.boxbreaker.runtime.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Persistence for {@link Job}s (and their library list / events / attributes via cascade). */
public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByJobNumber(String jobNumber);

    /** All jobs, newest first — for the WRKACTJOB-style listing. */
    List<Job> findAllByOrderByCreatedAtDesc();

    /** The job with the highest number — used to seed the next job number. */
    Optional<Job> findTopByOrderByJobNumberDesc();
}
