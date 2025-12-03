/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for JobManager.
 * @since 1.0
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
public final class JobManagerTest {

    @Test
    public void runnableJobCompletesWithZero() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> { }, "work");
        assertThat(
            "job should complete with zero",
            manager.waitForJob(id),
            equalTo(0)
        );
        final JobManager.Job job = manager.getJob(id);
        assertThat("job should exist", job, notNullValue());
        assertThat(
            "state should be DONE",
            job.state,
            equalTo(JobManager.State.DONE)
        );
        assertThat("exit code should be zero", job.exitCode, equalTo(0));
        assertThat("command should round-trip", job.command, equalTo("work"));
        manager.shutdown();
    }

    @Test
    public void supplierJobReturnsStatusAndPid() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> 7, "status");
        final long pid = JobManager.JOB_PID_BASE + id;
        assertThat("pid should map to status", manager.waitForPid(pid), equalTo(7));
        assertThat(
            "pid should be registered",
            manager.getJobByPid(pid),
            notNullValue()
        );
        assertThat("job should return status", manager.waitForJob(id), equalTo(7));
        assertThat(
            "unknown job should return 127",
            manager.waitForJob(999),
            equalTo(127)
        );
        assertThat(
            "unknown pid should return 127",
            manager.waitForPid(999),
            equalTo(127)
        );
        assertThat("unknown job should be absent", manager.getJob(999), nullValue());
        manager.shutdown();
    }

    @Test
    public void listJobsAndWaitForAll() throws Exception {
        final JobManager manager = new JobManager();
        manager.submit(() -> 1, "first");
        manager.submit(() -> 2, "second");
        final List<JobManager.Job> listed = manager.listJobs();
        assertThat("two jobs should be listed", listed.size(), equalTo(2));
        assertThat("first job command", listed.get(0).command, equalTo("first"));
        assertThat("second job command", listed.get(1).command, equalTo("second"));
        manager.waitForAll();
        assertThat("jobs should be cleared", manager.listJobs().isEmpty(), equalTo(true));
        manager.shutdown();
    }

    @Test
    public void failedSupplierBecomesStatusOne() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> {
            throw new IllegalStateException("boom");
        }, "fail");
        assertThat("failed job should return 1", manager.waitForJob(id), equalTo(1));
        assertThat(
            "failed job should be DONE",
            manager.getJob(id).state,
            equalTo(JobManager.State.DONE)
        );
        manager.shutdown();
    }

    @Test
    public void jobPidBoundaries() {
        assertThat(
            "base should be a job pid",
            JobManager.isJobPid(JobManager.JOB_PID_BASE), equalTo(true)
        );
        assertThat(
            "base minus one should not be a job pid",
            JobManager.isJobPid(JobManager.JOB_PID_BASE - 1), equalTo(false)
        );
    }
}
