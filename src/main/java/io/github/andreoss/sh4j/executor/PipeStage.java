/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.Node;
import java.io.IOException;
import java.io.OutputStream;

/**
 * One command of a multi-command pipeline, run on its own thread.
 *
 * @since 1.0
 */
final class PipeStage implements Runnable {

    /** The subshell that runs the command. */
    private final Executor child;

    /** The command. */
    private final Node node;

    /** The pipe the command writes to, closed once it finishes. */
    private final OutputStream pipe;

    /** Exit status of the command. */
    private int code;

    /**
     * Ctor.
     *
     * @param child The subshell that runs the command
     * @param node The command
     * @param pipe The pipe the command writes to, closed once it finishes
     */
    PipeStage(final Executor child, final Node node, final OutputStream pipe) {
        this.child = child;
        this.node = node;
        this.pipe = pipe;
    }

    @Override
    public void run() {
        try (this.pipe) {
            if (Attempt.failure(this::execute).isPresent()) {
                this.code = 1;
            }
        } catch (final IOException ex) {
            this.code = 1;
        }
    }

    /**
     * Returns the exit status of the command.
     *
     * @return The exit status, valid once the stage thread finished
     */
    int code() {
        return this.code;
    }

    private void execute() {
        try {
            this.code = this.child.executeNode(this.node);
        } catch (final ExitSignal ex) {
            this.code = ex.getCode();
        }
    }
}
