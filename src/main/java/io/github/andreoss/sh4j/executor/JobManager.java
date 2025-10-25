/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

final class JobManager {

    static final long JOB_PID_BASE = 1_000_000_000L;

    private final ExecutorService pool;
    private final Map<Integer, Job> jobs;
    private final AtomicInteger nextId;
    private final ReentrantLock lock;

    enum State {
        RUNNING,
        DONE,
        STOPPED
    }

    static final class Job {
        final int id;
        final long pid;
        final String command;
        volatile State state;
        volatile int exitCode;
        final CompletableFuture<Integer> future;

        Job(final int id, final long pid, final String command,
            final CompletableFuture<Integer> future) {
            this.id = id;
            this.pid = pid;
            this.command = command;
            this.future = future;
            this.state = State.RUNNING;
            this.exitCode = -1;
        }

        int await() {
            int code;
            try {
                code = this.future.get();
                this.state = State.DONE;
                this.exitCode = code;
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                code = 1;
            } catch (final ExecutionException ex) {
                code = 1;
            }
            return code;
        }
    }

    JobManager() {
        this.pool = Executors.newCachedThreadPool(runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        this.jobs = new LinkedHashMap<>();
        this.nextId = new AtomicInteger(1);
        this.lock = new ReentrantLock();
    }

    int submit(final Runnable task, final String command) {
        return this.submit(() -> {
            task.run();
            return 0;
        }, command);
    }

    int submit(final Supplier<Integer> task, final String command) {
        final int id = this.nextId.getAndIncrement();
        final CompletableFuture<Integer> future = CompletableFuture.supplyAsync(
            () -> {
                Thread.currentThread().setName("sh-job-" + id);
                return task.get();
            },
            this.pool
        );
        final Job job = new Job(id, JobManager.JOB_PID_BASE + id, command, future);
        future.whenComplete((result, ex) -> {
            if (ex instanceof CancellationException) {
                job.state = State.STOPPED;
            } else {
                job.state = State.DONE;
                job.exitCode = result != null ? result : 1;
            }
        });
        this.lock.lock();
        try {
            this.jobs.put(id, job);
        } finally {
            this.lock.unlock();
        }
        return id;
    }

    static boolean isJobPid(final long pid) {
        return pid >= JobManager.JOB_PID_BASE;
    }

    Job getJob(final int id) {
        this.lock.lock();
        try {
            final Job job = this.jobs.get(id);
            if (job != null && job.state == State.RUNNING && job.future.isDone()) {
                job.await();
            }
            return job;
        } finally {
            this.lock.unlock();
        }
    }

    Job getJobByPid(final long pid) {
        Job result = null;
        this.lock.lock();
        try {
            for (final Job job : this.jobs.values()) {
                if (job.pid == pid) {
                    result = job;
                    break;
                }
            }
        } finally {
            this.lock.unlock();
        }
        return result;
    }

    List<Job> listJobs() {
        this.lock.lock();
        try {
            for (final Job job : this.jobs.values()) {
                if (job.state == State.RUNNING && job.future.isDone()) {
                    job.await();
                }
            }
            return new ArrayList<>(this.jobs.values());
        } finally {
            this.lock.unlock();
        }
    }

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

    void waitForAll() {
        final List<Job> all = this.listJobs();
        for (final Job job : all) {
            if (job.state == State.RUNNING) {
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

    void shutdown() {
        this.pool.shutdown();
    }
}
