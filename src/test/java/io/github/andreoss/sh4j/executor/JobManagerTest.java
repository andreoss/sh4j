/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

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
    void runnableJobCompletesWithZeroFirst() {
        final JobManager manager = new JobManager();
        MatcherAssert.assertThat(
            "job should complete with zero",
            manager.waitForJob(manager.submit(() -> { }, "work")),
            CoreMatchers.equalTo(0)
        );
        manager.shutdown();
    }

    @Test
    void runnableJobCompletesWithZeroSecond() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> { }, "work");
        manager.waitForJob(id);
        MatcherAssert.assertThat(
            "job should exist",
            manager.getJob(id),
            CoreMatchers.notNullValue()
        );
        manager.shutdown();
    }

    @Test
    void runnableJobCompletesWithZeroThird() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> { }, "work");
        manager.waitForJob(id);
        MatcherAssert.assertThat(
            "state should be DONE",
            manager.getJob(id).getState(),
            CoreMatchers.equalTo(JobManager.State.DONE)
        );
        manager.shutdown();
    }

    @Test
    void runnableJobCompletesWithZeroFourth() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> { }, "work");
        manager.waitForJob(id);
        MatcherAssert.assertThat(
            "exit code should be zero",
            manager.getJob(id).getExitCode(),
            CoreMatchers.equalTo(0)
        );
        manager.shutdown();
    }

    @Test
    void runnableJobCompletesWithZeroFifth() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> { }, "work");
        manager.waitForJob(id);
        MatcherAssert.assertThat(
            "command should round-trip",
            manager.getJob(id).getCommand(),
            CoreMatchers.equalTo("work")
        );
        manager.shutdown();
    }

    @Test
    void supplierJobReturnsStatusAndPidFirst() {
        final JobManager manager = new JobManager();
        MatcherAssert.assertThat(
            "pid should map to status",
            manager.waitForPid(JobManager.JOB_PID_BASE + manager.submit(() -> 7, "status")),
            CoreMatchers.equalTo(7)
        );
        manager.shutdown();
    }

    @Test
    void supplierJobReturnsStatusAndPidSecond() {
        final JobManager manager = new JobManager();
        final long pid = JobManager.JOB_PID_BASE + manager.submit(() -> 7, "status");
        manager.waitForPid(pid);
        MatcherAssert.assertThat(
            "pid should be registered",
            manager.getJobByPid(pid),
            CoreMatchers.notNullValue()
        );
        manager.shutdown();
    }

    @Test
    void supplierJobReturnsStatusAndPidThird() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> 7, "status");
        manager.waitForPid(JobManager.JOB_PID_BASE + id);
        MatcherAssert.assertThat(
            "job should return status",
            manager.waitForJob(id),
            CoreMatchers.equalTo(7)
        );
        manager.shutdown();
    }

    @Test
    void supplierJobReturnsStatusAndPidFourth() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> 7, "status");
        manager.waitForPid(JobManager.JOB_PID_BASE + id);
        manager.waitForJob(id);
        MatcherAssert.assertThat(
            "unknown job should return 127",
            manager.waitForJob(999),
            CoreMatchers.equalTo(127)
        );
        manager.shutdown();
    }

    @Test
    void supplierJobReturnsStatusAndPidFifth() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> 7, "status");
        manager.waitForPid(JobManager.JOB_PID_BASE + id);
        manager.waitForJob(id);
        manager.waitForJob(999);
        MatcherAssert.assertThat(
            "unknown pid should return 127",
            manager.waitForPid(999),
            CoreMatchers.equalTo(127)
        );
        manager.shutdown();
    }

    @Test
    void supplierJobReturnsStatusAndPidSixth() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> 7, "status");
        manager.waitForPid(JobManager.JOB_PID_BASE + id);
        manager.waitForJob(id);
        manager.waitForJob(999);
        manager.waitForPid(999);
        MatcherAssert.assertThat(
            "unknown job should be absent",
            manager.getJob(999),
            CoreMatchers.nullValue()
        );
        manager.shutdown();
    }

    @Test
    void listJobsAndWaitForAllFirst() throws Exception {
        final JobManager manager = new JobManager();
        manager.submit(() -> 1, "first");
        manager.submit(() -> 2, "second");
        MatcherAssert.assertThat(
            "two jobs should be listed",
            manager.listJobs().size(),
            CoreMatchers.equalTo(2)
        );
        manager.shutdown();
    }

    @Test
    void listJobsAndWaitForAllSecond() throws Exception {
        final JobManager manager = new JobManager();
        manager.submit(() -> 1, "first");
        manager.submit(() -> 2, "second");
        MatcherAssert.assertThat(
            "first job command",
            manager.listJobs().get(0).getCommand(),
            CoreMatchers.equalTo("first")
        );
        manager.shutdown();
    }

    @Test
    void listJobsAndWaitForAllThird() throws Exception {
        final JobManager manager = new JobManager();
        manager.submit(() -> 1, "first");
        manager.submit(() -> 2, "second");
        MatcherAssert.assertThat(
            "second job command",
            manager.listJobs().get(1).getCommand(),
            CoreMatchers.equalTo("second")
        );
        manager.shutdown();
    }

    @Test
    void listJobsAndWaitForAllFourth() throws Exception {
        final JobManager manager = new JobManager();
        manager.submit(() -> 1, "first");
        manager.submit(() -> 2, "second");
        manager.listJobs();
        manager.waitForAll();
        MatcherAssert.assertThat(
            "jobs should be cleared",
            manager.listJobs().isEmpty(),
            CoreMatchers.equalTo(true)
        );
        manager.shutdown();
    }

    @Test
    void failedSupplierBecomesStatusOneFirst() {
        final JobManager manager = new JobManager();
        MatcherAssert.assertThat(
            "failed job should return 1",
            manager.waitForJob(
                manager.submit(
                    () -> {
                        throw new IllegalStateException("boom");
                    },
                    "fail"
                )
            ),
            CoreMatchers.equalTo(1)
        );
        manager.shutdown();
    }

    @Test
    void failedSupplierBecomesStatusOneSecond() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(
            () -> {
                throw new IllegalStateException("boom");
            },
            "fail"
        );
        manager.waitForJob(id);
        MatcherAssert.assertThat(
            "failed job should be DONE",
            manager.getJob(id).getState(),
            CoreMatchers.equalTo(JobManager.State.DONE)
        );
        manager.shutdown();
    }

    @Test
    void jobPidBoundariesFirst() {
        MatcherAssert.assertThat(
            "base should be a job pid",
            JobManager.isJobPid(JobManager.JOB_PID_BASE),
            CoreMatchers.equalTo(true)
        );
    }

    @Test
    void jobPidBoundariesSecond() {
        MatcherAssert.assertThat(
            "base minus one should not be a job pid",
            JobManager.isJobPid(JobManager.JOB_PID_BASE - 1),
            CoreMatchers.equalTo(false)
        );
    }
}
