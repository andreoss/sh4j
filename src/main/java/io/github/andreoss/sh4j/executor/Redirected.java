/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.Redirection;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * Runs an action with the redirections of a command applied to the live
 * streams of a shell, restoring them afterwards.
 *
 * @since 1.0
 */
final class Redirected {

    /** The shell whose streams are redirected. */
    private final Executor shell;

    /** True when a redirection error exits the shell (special built-in). */
    private final boolean fatal;

    /**
     * Ctor.
     *
     * @param shell The shell whose streams are redirected
     */
    Redirected(final Executor shell) {
        this(shell, false);
    }

    /**
     * Ctor.
     *
     * @param shell The shell whose streams are redirected
     * @param fatal True when a redirection error exits the shell
     */
    Redirected(final Executor shell, final boolean fatal) {
        this.shell = shell;
        this.fatal = fatal;
    }

    /**
     * Runs the action with the redirections applied.
     *
     * @param redirs The redirections
     * @param action The action
     * @return The exit status of the action, or 1 when a redirection fails
     */
    int run(final List<Redirection> redirs, final IntSupplier action) {
        final int code;
        if (new Noclobber(this.shell).refuses(redirs)) {
            code = this.failure();
        } else {
            code = this.swapped(redirs, action);
        }
        return code;
    }

    private int swapped(final List<Redirection> redirs, final IntSupplier action) {
        final RedirectionHandler.StreamSwap swap = OutputSwap.apply(this.shell, redirs);
        final InputSwap input = new InputSwap(this.shell);
        final Optional<InputStream> kept = input.install(redirs);
        final int code;
        try {
            if (swap.failed() || input.failed()) {
                code = this.failure();
            } else {
                code = action.getAsInt();
            }
        } finally {
            input.restore(kept);
            OutputSwap.restore(this.shell, swap);
        }
        return code;
    }

    private int failure() {
        if (this.fatal) {
            throw new ExitSignal(1);
        }
        return 1;
    }
}
