/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Delivers one signal to the operands of the {@code kill} built-in: a job
 * id such as {@code %1}, the pseudo pid of a background job, the shell's
 * own pid, or the pid of another process.
 *
 * @since 1.0
 */
final class Kill {

    /** Shell that runs the built-in. */
    private final BuiltinContext shell;

    /** Signal number, 0 to only probe the target. */
    private final int signal;

    /**
     * Ctor.
     *
     * @param shell Shell that runs the built-in
     * @param signal Signal number, 0 to only probe the target
     */
    Kill(final BuiltinContext shell, final int signal) {
        this.shell = shell;
        this.signal = signal;
    }

    /**
     * Delivers the signal to one operand.
     *
     * @param target Job id or pid operand
     * @return True when the target exists and received the signal
     */
    boolean send(final String target) {
        boolean sent;
        try {
            if (target.startsWith("%")) {
                sent = this.job(
                    this.shell.jobManager().getJob(Integer.parseInt(target.substring(1)))
                );
            } else {
                sent = this.pid(Long.parseLong(target));
            }
        } catch (final NumberFormatException ex) {
            sent = false;
        }
        return sent;
    }

    private boolean pid(final long pid) {
        final boolean sent;
        if (JobManager.isJobPid(pid)) {
            sent = this.job(this.shell.jobManager().getJobByPid(pid));
        } else if (pid == ProcessHandle.current().pid()) {
            sent = this.self();
        } else {
            sent = ProcessHandle.of(pid).map(this::process).orElse(false);
        }
        return sent;
    }

    private boolean job(final Job job) {
        return job != null && job.signal(this.signal);
    }

    private boolean self() {
        if (this.signal != 0) {
            final String action = this.shell.traps().get(KillSignal.name(this.signal));
            if (action == null) {
                throw new ExitSignal(128 + this.signal);
            }
            ExitTrap.run(this.shell, action);
        }
        return true;
    }

    private boolean process(final ProcessHandle proc) {
        final boolean sent;
        if (this.signal == 0) {
            sent = proc.isAlive();
        } else if (this.signal == 9) {
            sent = proc.destroyForcibly();
        } else {
            sent = proc.destroy();
        }
        return sent;
    }
}
