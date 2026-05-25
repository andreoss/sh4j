/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * One background job.
 *
 * @since 1.0
 */
public final class Job {

    /** Job identifier. */
    private final int id;

    /** Pseudo pid reported for the job. */
    private final long pid;

    /** Command text. */
    private final String command;

    /** Signal delivery to the running job. */
    private final JobControl control;

    /** Completion state. */
    private volatile JobManager.State state;

    /** Exit status once the job is done. */
    private volatile int exitcode;

    /** Completion future of the submitted task. */
    private CompletableFuture<Integer> source;

    /** Completion future that callers wait on. */
    private CompletableFuture<Integer> future;

    /**
     * Creates a job.
     *
     * @param id Job identifier
     * @param command Command text
     * @param control Signal delivery to the running job
     */
    Job(final int id, final String command, final JobControl control) {
        this.id = id;
        this.pid = JobManager.JOB_PID_BASE + id;
        this.command = command;
        this.control = control;
        this.state = JobManager.State.RUNNING;
        this.exitcode = -1;
    }

    /**
     * Returns the job identifier.
     *
     * @return Job identifier
     */
    int getId() {
        return this.id;
    }

    /**
     * Returns the pseudo pid.
     *
     * @return Pseudo pid
     */
    long getPid() {
        return this.pid;
    }

    /**
     * Returns the command text.
     *
     * @return Command text
     */
    String getCommand() {
        return this.command;
    }

    /**
     * Returns the current state.
     *
     * @return Current state
     */
    JobManager.State getState() {
        return this.state;
    }

    /**
     * Returns the exit status.
     *
     * @return Exit status, or -1 while the job runs
     */
    int getExitCode() {
        return this.exitcode;
    }

    /**
     * Checks whether the completion future is done.
     *
     * @return True when the future has completed
     */
    boolean done() {
        return this.future.isDone();
    }

    /**
     * Marks the job as running again.
     */
    void resume() {
        this.state = JobManager.State.RUNNING;
    }

    /**
     * Delivers a signal to the job.
     *
     * @param sig Signal number, 0 to only probe the job
     * @return True when the job has not ended yet
     */
    boolean signal(final int sig) {
        return this.control.send(sig);
    }

    /**
     * Attaches the completion callback that records the final state.
     *
     * @param src Completion future of the submitted task
     */
    void watch(final CompletableFuture<Integer> src) {
        this.source = src;
        this.future = src.whenComplete(
            (result, ex) -> {
                if (ex instanceof CancellationException) {
                    this.state = JobManager.State.STOPPED;
                } else {
                    this.state = JobManager.State.DONE;
                    int code = 1;
                    if (result != null) {
                        code = result;
                    }
                    this.exitcode = this.control.status(code);
                }
            }
        );
    }

    /**
     * Waits for the job to finish.
     *
     * @return The job's exit status, or 1 when it was interrupted
     */
    int await() {
        int code;
        try {
            code = this.control.status(this.future.get());
            this.state = JobManager.State.DONE;
            this.exitcode = code;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            code = 1;
        } catch (final CancellationException ex) {
            code = 1;
        } catch (final ExecutionException ex) {
            code = this.control.status(1);
        }
        return code;
    }

    /**
     * Cancels the job.
     */
    void cancel() {
        this.source.cancel(true);
        this.future.cancel(true);
    }
}
