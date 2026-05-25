/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Tracks the shell's background jobs: submission, lookup by identifier or
 * pseudo pid, waiting, and cancellation. All state transitions are guarded
 * by a single reentrant lock.
 *
 * @since 1.0
 */
final class JobManager {

    /** Base of the pseudo pids reported for background jobs. */
    static final long JOB_PID_BASE = 1_000_000_000L;

    /** Thread pool that runs the jobs. */
    private final ExecutorService pool = Executors.newCachedThreadPool(
        runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    );

    /** Jobs by identifier. */
    private final Map<Integer, Job> jobs = new LinkedHashMap<>(0);

    /** Source of job identifiers. */
    private final AtomicInteger counter = new AtomicInteger(1);

    /** Guards the job map. */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Checks whether a pid belongs to a shell background job.
     *
     * @param pid A process identifier
     * @return True when the pid was allocated by this manager
     */
    static boolean isJobPid(final long pid) {
        return pid >= JobManager.JOB_PID_BASE;
    }

    /**
     * Submits a runnable task as a background job.
     *
     * @param task The task to run
     * @param command Command text reported by the job built-ins
     * @return The job identifier
     */
    int submit(final Runnable task, final String command) {
        return this.submit(
            () -> {
                task.run();
                return 0;
            },
            command
        );
    }

    /**
     * Submits a task as a background job.
     *
     * @param task The task to run
     * @param command Command text reported by the job built-ins
     * @return The job identifier
     */
    int submit(final Supplier<Integer> task, final String command) {
        final int id = this.counter.getAndIncrement();
        final JobControl control = new JobControl();
        final Job job = new Job(id, command, control);
        job.watch(
            CompletableFuture.supplyAsync(
                () -> {
                    Thread.currentThread().setName(String.format("sh-job-%d", id));
                    control.attach();
                    try {
                        return task.get();
                    } finally {
                        control.detach();
                    }
                },
                this.pool
            )
        );
        this.lock.lock();
        try {
            this.jobs.put(id, job);
        } finally {
            this.lock.unlock();
        }
        return id;
    }

    /**
     * Returns the job with the given identifier.
     *
     * @param id Job identifier
     * @return The job, or null when unknown
     */
    Job getJob(final int id) {
        this.lock.lock();
        try {
            final Job job = this.jobs.get(id);
            if (job != null && job.getState() == JobManager.State.RUNNING
                && job.done()) {
                job.await();
            }
            return job;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Returns the job with the given pseudo pid.
     *
     * @param pid Pseudo pid
     * @return The job, or null when unknown
     */
    Job getJobByPid(final long pid) {
        Job result = null;
        this.lock.lock();
        try {
            for (final Job job : this.jobs.values()) {
                if (job.getPid() == pid) {
                    result = job;
                    break;
                }
            }
        } finally {
            this.lock.unlock();
        }
        return result;
    }

    /**
     * Returns every known job in submission order.
     *
     * @return The jobs
     */
    List<Job> listJobs() {
        this.lock.lock();
        try {
            for (final Job job : this.jobs.values()) {
                if (job.getState() == JobManager.State.RUNNING
                    && job.done()) {
                    job.await();
                }
            }
            return new ArrayList<>(this.jobs.values());
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Waits for the job with the given identifier.
     *
     * @param id Job identifier
     * @return The job's exit status, or 127 when unknown
     */
    int waitForJob(final int id) {
        final Job job = this.getJob(id);
        final int code;
        if (job == null) {
            code = 127;
        } else {
            code = job.await();
        }
        return code;
    }

    /**
     * Waits for the job with the given pseudo pid.
     *
     * @param pid Pseudo pid
     * @return The job's exit status, or 127 when unknown
     */
    int waitForPid(final long pid) {
        final Job job = this.getJobByPid(pid);
        final int code;
        if (job == null) {
            code = 127;
        } else {
            code = job.await();
        }
        return code;
    }

    /**
     * Waits for every running job and forgets all of them.
     */
    void waitForAll() {
        final List<Job> all = this.listJobs();
        for (final Job job : all) {
            if (job.getState() == JobManager.State.RUNNING) {
                job.await();
            }
        }
        this.lock.lock();
        try {
            this.jobs.clear();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Stops the job thread pool.
     */
    void shutdown() {
        this.pool.shutdown();
    }

    /**
     * Lifecycle state of a background job.
     *
     * @since 1.0
     */
    enum State {

        /** Still running. */
        RUNNING,

        /** Finished normally. */
        DONE,

        /** Stopped by a signal or a cancellation. */
        STOPPED
    }
}
