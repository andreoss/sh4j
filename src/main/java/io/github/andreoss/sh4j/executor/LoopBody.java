/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.ListNode;

/**
 * Runs the body of a loop and tracks the status and the break request.
 *
 * @since 1.0
 */
final class LoopBody {

    /** The shell that runs the body. */
    private final BuiltinContext shell;

    /** The loop body. */
    private final ListNode body;

    /** Exit status of the most recent body run. */
    private int status;

    /** False once the body requested a break. */
    private boolean running;

    /**
     * Ctor.
     *
     * @param shell The shell that runs the body
     * @param body The loop body
     */
    LoopBody(final BuiltinContext shell, final ListNode body) {
        this(shell, body, true);
    }

    private LoopBody(
        final BuiltinContext shell, final ListNode body, final boolean running
    ) {
        this.shell = shell;
        this.body = body;
        this.running = running;
    }

    /**
     * Tells whether the loop may run another iteration.
     *
     * @return False once the body requested a break
     */
    boolean running() {
        return this.running;
    }

    /**
     * Returns the exit status of the most recent body run.
     *
     * @return The exit status, 0 when the body never ran
     */
    int status() {
        return this.status;
    }

    /**
     * Runs the body once, handling single-level break and continue.
     */
    void run() {
        try {
            this.status = this.shell.execute(this.body);
        } catch (final BreakSignal ex) {
            if (ex.getLevels() > 1) {
                throw new BreakSignal(ex.getLevels() - 1, ex);
            }
            this.running = false;
        } catch (final ContinueSignal ex) {
            if (ex.getLevels() > 1) {
                throw new ContinueSignal(ex.getLevels() - 1, ex);
            }
            this.status = 0;
        }
    }
}
