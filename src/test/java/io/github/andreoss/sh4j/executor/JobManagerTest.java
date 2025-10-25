/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals(0, manager.waitForJob(id));
        final JobManager.Job job = manager.getJob(id);
        Assert.assertNotNull(job);
        Assert.assertEquals(JobManager.State.DONE, job.state);
        Assert.assertEquals(0, job.exitCode);
        Assert.assertEquals("work", job.command);
        manager.shutdown();
    }

    @Test
    public void supplierJobReturnsStatusAndPid() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> 7, "status");
        final long pid = JobManager.JOB_PID_BASE + id;
        Assert.assertEquals(7, manager.waitForPid(pid));
        Assert.assertNotNull(manager.getJobByPid(pid));
        Assert.assertEquals(7, manager.waitForJob(id));
        Assert.assertEquals(127, manager.waitForJob(999));
        Assert.assertEquals(127, manager.waitForPid(999));
        Assert.assertNull(manager.getJob(999));
        manager.shutdown();
    }

    @Test
    public void listJobsAndWaitForAll() throws Exception {
        final JobManager manager = new JobManager();
        final int first = manager.submit(() -> 1, "first");
        final int second = manager.submit(() -> 2, "second");
        final List<JobManager.Job> listed = manager.listJobs();
        Assert.assertEquals(2, listed.size());
        Assert.assertEquals(first, listed.get(0).id);
        Assert.assertEquals(second, listed.get(1).id);
        manager.waitForAll();
        Assert.assertTrue(manager.listJobs().isEmpty());
        manager.shutdown();
    }

    @Test
    public void failedSupplierBecomesStatusOne() {
        final JobManager manager = new JobManager();
        final int id = manager.submit(() -> {
            throw new IllegalStateException("boom");
        }, "fail");
        Assert.assertEquals(1, manager.waitForJob(id));
        Assert.assertEquals(JobManager.State.DONE, manager.getJob(id).state);
        manager.shutdown();
    }

    @Test
    public void jobPidBoundaries() {
        Assert.assertTrue(JobManager.isJobPid(JobManager.JOB_PID_BASE));
        Assert.assertFalse(JobManager.isJobPid(JobManager.JOB_PID_BASE - 1));
    }
}
