/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for JobManager.
 *
 * @since 1.0
 */
final class JobManagerTest {

    @Test
    void runnableJobCompletesWithZero() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> { }, "work");
        MatcherAssert.assertThat(
            "job should complete with zero",
            manager.waitForJob(id),
            CoreMatchers.equalTo(0)
        );
        final Job job = manager.getJob(id);
        MatcherAssert.assertThat("job should exist", job, CoreMatchers.notNullValue());
        MatcherAssert.assertThat(
            "state should be DONE",
            job.getState(),
            CoreMatchers.equalTo(JobManager.State.DONE)
        );
        MatcherAssert.assertThat(
            "exit code should be zero",
            job.getExitCode(),
            CoreMatchers.equalTo(0)
        );
        MatcherAssert.assertThat(
            "command should round-trip",
            job.getCommand(),
            CoreMatchers.equalTo("work")
        );
        manager.shutdown();
    }

    @Test
    void supplierJobReturnsStatusAndPid() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> 7, "status");
        final long pid = JobManager.JOB_PID_BASE + id;
        MatcherAssert.assertThat(
            "pid should map to status",
            manager.waitForPid(pid),
            CoreMatchers.equalTo(7)
        );
        MatcherAssert.assertThat(
            "pid should be registered",
            manager.getJobByPid(pid),
            CoreMatchers.notNullValue()
        );
        MatcherAssert.assertThat(
            "job should return status",
            manager.waitForJob(id),
            CoreMatchers.equalTo(7)
        );
        MatcherAssert.assertThat(
            "unknown job should return 127",
            manager.waitForJob(999),
            CoreMatchers.equalTo(127)
        );
        MatcherAssert.assertThat(
            "unknown pid should return 127",
            manager.waitForPid(999),
            CoreMatchers.equalTo(127)
        );
        MatcherAssert.assertThat(
            "unknown job should be absent",
            manager.getJob(999),
            CoreMatchers.nullValue()
        );
        manager.shutdown();
    }

    @Test
    void listJobsAndWaitForAll() throws Exception {
        final JobManager manager = new JobManager();
        manager.submit(() -> 1, "first");
        manager.submit(() -> 2, "second");
        final List<Job> listed = manager.listJobs();
        MatcherAssert.assertThat(
            "two jobs should be listed",
            listed.size(),
            CoreMatchers.equalTo(2)
        );
        MatcherAssert.assertThat(
            "first job command",
            listed.get(0).getCommand(),
            CoreMatchers.equalTo("first")
        );
        MatcherAssert.assertThat(
            "second job command",
            listed.get(1).getCommand(),
            CoreMatchers.equalTo("second")
        );
        manager.waitForAll();
        MatcherAssert.assertThat(
            "jobs should be cleared",
            manager.listJobs().isEmpty(),
            CoreMatchers.equalTo(true)
        );
        manager.shutdown();
    }

    @Test
    void failedSupplierBecomesStatusOne() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(
            () -> {
                throw new IllegalStateException("boom");
            },
            "fail"
        );
        MatcherAssert.assertThat(
            "failed job should return 1",
            manager.waitForJob(id),
            CoreMatchers.equalTo(1)
        );
        MatcherAssert.assertThat(
            "failed job should be DONE",
            manager.getJob(id).getState(),
            CoreMatchers.equalTo(JobManager.State.DONE)
        );
        manager.shutdown();
    }

    @Test
    void jobPidBoundaries() {
        MatcherAssert.assertThat(
            "base should be a job pid",
            JobManager.isJobPid(JobManager.JOB_PID_BASE), CoreMatchers.equalTo(true)
        );
        MatcherAssert.assertThat(
            "base minus one should not be a job pid",
            JobManager.isJobPid(JobManager.JOB_PID_BASE - 1), CoreMatchers.equalTo(false)
        );
    }
}
