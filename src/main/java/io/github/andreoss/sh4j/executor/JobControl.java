/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Delivers a signal to one background job: interrupts the thread that
 * runs it and destroys the child processes it started.
 *
 * @since 1.0
 */
final class JobControl {

    /** Control of the job the current thread runs, inherited by its threads. */
    private static final InheritableThreadLocal<JobControl> CURRENT =
        new InheritableThreadLocal<>();

    /** Guards the mutable state. */
    private final ReentrantLock lock = new ReentrantLock();

    /** Child processes started by the job. */
    private final List<Process> procs = new ArrayList<>(0);

    /** Thread that runs the job, null before it starts. */
    private Thread thread;

    /** Whether the job has ended. */
    private boolean ended;

    /** Signal delivered to the job, 0 when none. */
    private int signal;

    /**
     * Records a child process started by the job of the current thread.
     *
     * @param process The child process
     */
    static void track(final Process process) {
        final JobControl control = JobControl.CURRENT.get();
        if (control != null) {
            control.add(process);
        }
    }

    /**
     * Binds the job to the current thread.
     */
    void attach() {
        this.lock.lock();
        try {
            JobControl.CURRENT.set(this);
            this.thread = Thread.currentThread();
            if (this.signal != 0) {
                this.thread.interrupt();
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Unbinds the job from the current thread and clears its interrupt.
     */
    void detach() {
        this.lock.lock();
        try {
            JobControl.CURRENT.remove();
            this.ended = true;
            this.procs.clear();
            Thread.interrupted();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Delivers a signal to the job.
     *
     * @param sig Signal number, 0 to only probe the job
     * @return True when the job has not ended yet
     */
    boolean send(final int sig) {
        this.lock.lock();
        try {
            final boolean alive = !this.ended;
            if (alive && sig != 0) {
                this.signal = sig;
                if (this.thread != null) {
                    this.thread.interrupt();
                }
                this.procs.forEach(this::destroy);
            }
            return alive;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Returns the exit status of the job.
     *
     * @param code Status the job's commands returned
     * @return Sum of 128 and the delivered signal, or the given status
     */
    int status(final int code) {
        this.lock.lock();
        try {
            int result = code;
            if (this.signal != 0) {
                result = 128 + this.signal;
            }
            return result;
        } finally {
            this.lock.unlock();
        }
    }

    private void add(final Process process) {
        this.lock.lock();
        try {
            this.procs.removeIf(proc -> !proc.isAlive());
            this.procs.add(process);
            if (this.signal != 0) {
                this.destroy(process);
            }
        } finally {
            this.lock.unlock();
        }
    }

    private void destroy(final Process process) {
        if (this.signal == 9) {
            process.destroyForcibly();
        } else {
            process.destroy();
        }
    }
}
