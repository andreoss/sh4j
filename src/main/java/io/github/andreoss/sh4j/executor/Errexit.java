/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.function.IntSupplier;

/**
 * The {@code errexit} option (POSIX 2.14 set -e): a failing command exits
 * the shell, except while the option is ignored in an {@code if},
 * {@code while} or {@code until} condition, in an and-or list before its
 * last pipeline, and in a pipeline beginning with {@code !}.
 *
 * @since 1.0
 */
final class Errexit {

    /** Nesting depth of the contexts that ignore the option. */
    private int ignored;

    /**
     * Runs a command in a context that ignores the option.
     *
     * @param body The command
     * @return The exit status of the command
     */
    int ignoring(final IntSupplier body) {
        this.ignored = this.ignored + 1;
        try {
            return body.getAsInt();
        } finally {
            this.ignored = this.ignored - 1;
        }
    }

    /**
     * Exits the shell when the option applies to a failed command.
     *
     * @param flags The current shell flags
     * @param status The exit status of the command
     */
    void check(final String flags, final int status) {
        if (status != 0 && this.ignored == 0 && flags.indexOf('e') >= 0) {
            throw new ExitSignal(status);
        }
    }

    /**
     * Copies the ignoring context of the parent shell into a subshell.
     *
     * @param parent The parent shell's option state
     */
    void inherit(final Errexit parent) {
        this.ignored = parent.ignored;
    }
}
