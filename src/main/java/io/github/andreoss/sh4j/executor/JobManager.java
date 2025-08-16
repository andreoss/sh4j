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

final class JobManager {

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
        int exitCode;
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
    }

    private final ExecutorService pool;
    private final Map<Integer, Job> jobs;
    private final AtomicInteger nextId;

    JobManager() {
        this.pool = Executors.newCachedThreadPool(runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        this.jobs = new LinkedHashMap<>();
        this.nextId = new AtomicInteger(1);
    }

    int submit(final Runnable task, final String command) {
        final int id = this.nextId.getAndIncrement();
        final CompletableFuture<Integer> future = CompletableFuture.supplyAsync(
            () -> {
                Thread.currentThread().setName("sh-job-" + id);
                task.run();
                return 0;
            },
            this.pool
        );
        final Job job = new Job(id, id, command, future);
        future.whenComplete((result, ex) -> {
            if (ex instanceof CancellationException) {
                job.state = State.STOPPED;
            } else {
                job.state = State.DONE;
                job.exitCode = result != null ? result : 1;
            }
        });
        synchronized (this.jobs) {
            this.jobs.put(id, job);
        }
        return id;
    }

    Job getJob(final int id) {
        synchronized (this.jobs) {
            final Job job = this.jobs.get(id);
            if (job != null && job.state == State.RUNNING && job.future.isDone()) {
                job.state = State.DONE;
                try {
                    job.exitCode = job.future.get();
                } catch (final Exception ex) {
                    job.exitCode = 1;
                }
            }
            return job;
        }
    }

    List<Job> listJobs() {
        synchronized (this.jobs) {
            for (final Job job : this.jobs.values()) {
                if (job.state == State.RUNNING && job.future.isDone()) {
                    job.state = State.DONE;
                    try {
                        job.exitCode = job.future.get();
                    } catch (final Exception ex) {
                        job.exitCode = 1;
                    }
                }
            }
            return new ArrayList<>(this.jobs.values());
        }
    }

    int waitForJob(final int id) {
        final Job job = this.getJob(id);
        if (job == null) {
            return 127;
        }
        try {
            final int code = job.future.get();
            job.state = State.DONE;
            job.exitCode = code;
            return code;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            return 1;
        } catch (final ExecutionException ex) {
            return 1;
        }
    }

    void waitForAll() {
        final List<Job> all = this.listJobs();
        for (final Job job : all) {
            if (job.state == State.RUNNING) {
                try {
                    job.future.get();
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (final ExecutionException ex) {
                    // ignore
                }
            }
        }
        synchronized (this.jobs) {
            this.jobs.clear();
        }
    }

    void shutdown() {
        this.pool.shutdown();
    }
}
