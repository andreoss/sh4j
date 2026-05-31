/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.function.IntSupplier;

/**
 * The loops, functions and dot scripts a shell is running in, so that
 * {@code break}, {@code continue} and {@code return} decide at call time,
 * while the redirections of the command are still in effect, whether they
 * have a scope to leave (POSIX 2.7, 2.14).
 *
 * @since 1.0
 */
final class Scopes {

    /** Runner of compound commands, tracking the loop depth. */
    private final Compound compound;

    /** Nesting depth of the running functions and dot scripts. */
    private int depth;

    /** Whether this shell is a subshell of a function or dot script. */
    private boolean nested;

    /**
     * Ctor.
     *
     * @param compound Runner of compound commands, tracking the loop depth
     */
    Scopes(final Compound compound) {
        this.compound = compound;
    }

    /**
     * Runs a function body or dot script as a frame that {@code return}
     * leaves.
     *
     * @param body The frame body
     * @return The exit status of the body
     */
    int frame(final IntSupplier body) {
        this.depth = this.depth + 1;
        try {
            return body.getAsInt();
        } finally {
            this.depth = this.depth - 1;
        }
    }

    /**
     * Builds the signal of {@code return}: it leaves the running frame;
     * outside any frame it ends the shell, reporting the misuse on the
     * current stderr unless the shell is a subshell of a frame.
     *
     * @param code The status to return
     * @param shell The shell whose current stderr receives the diagnostic
     * @return The signal to throw
     */
    RuntimeException leave(final int code, final BuiltinIo shell) {
        final RuntimeException signal;
        if (this.depth > 0) {
            signal = new ReturnSignal(code);
        } else {
            if (!this.nested) {
                shell.errStream().println("sh: return: not in a function");
            }
            signal = new ExitSignal(code);
        }
        return signal;
    }

    /**
     * Raises a {@code break} or {@code continue} signal inside a loop;
     * outside one, reports the misuse on the current stderr instead.
     *
     * @param signal The loop control signal
     * @param name The built-in name
     * @param shell The shell whose current stderr receives the diagnostic
     */
    void loop(final RuntimeException signal, final String name, final BuiltinIo shell) {
        if (this.compound.inLoop()) {
            throw signal;
        }
        shell.errStream().println(String.format("sh: %s: not in a loop", name));
    }

    /**
     * Records that a subshell runs inside the scopes of its parent shell.
     *
     * @param parent The parent shell's scopes
     */
    void inherit(final Scopes parent) {
        this.nested = parent.depth > 0 || parent.nested;
    }
}
