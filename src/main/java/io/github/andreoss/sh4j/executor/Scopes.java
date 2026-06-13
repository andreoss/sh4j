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

    /** Status the shell exits with while its EXIT trap runs, else -1. */
    private int ending;

    /** Status of the last {@code exit} that ended the shell. */
    private int exited;

    /**
     * Ctor.
     *
     * @param compound Runner of compound commands, tracking the loop depth
     */
    Scopes(final Compound compound) {
        this.compound = compound;
        this.ending = -1;
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
     * Runs a function body as a frame that {@code return} leaves and that
     * no loop of the caller encloses.
     *
     * @param body The function body
     * @return The exit status of the body
     */
    int call(final IntSupplier body) {
        return this.compound.isolated(() -> this.frame(body));
    }

    /**
     * Returns the number of loops lexically enclosing the running command.
     *
     * @return The loop depth, 0 outside any loop
     */
    int loops() {
        return this.compound.depth();
    }

    /**
     * Raises a {@code break} or {@code continue} signal inside a loop;
     * outside one it does nothing, reporting the misuse on the current
     * stderr only at the top level of the shell.
     *
     * @param signal The loop control signal
     * @param name The built-in name
     * @param shell The shell whose current stderr receives the diagnostic
     */
    void loop(final RuntimeException signal, final String name, final BuiltinIo shell) {
        if (this.compound.depth() > 0) {
            throw signal;
        }
        if (this.depth == 0 && !this.nested) {
            shell.errStream().println(String.format("sh: %s: not in a loop", name));
        }
    }

    /**
     * Enters the action of the {@code EXIT} trap; an {@code exit} in it sets
     * the status the shell exits with (POSIX exit).
     *
     * @param status The status the shell exits with before the trap
     */
    void enter(final int status) {
        this.ending = status;
        this.exited = status;
    }

    /**
     * Leaves the action of the {@code EXIT} trap.
     *
     * @return The status the shell exits with after the trap
     */
    int ended() {
        this.ending = -1;
        return this.exited;
    }

    /**
     * Records the status of an {@code exit} that ended the shell.
     *
     * @param code The exit status
     */
    void exit(final int code) {
        this.exited = code;
    }

    /**
     * Returns the status of {@code exit} without an operand: inside the
     * {@code EXIT} trap, the status the shell was exiting with.
     *
     * @param last The value of {@code $?}
     * @return The exit status
     */
    int status(final int last) {
        int result = last;
        if (this.ending >= 0) {
            result = this.ending;
        }
        return result;
    }

    /**
     * Records that a subshell runs inside the scopes of its parent shell,
     * including the loops that enclose it.
     *
     * @param parent The parent shell's scopes
     */
    void inherit(final Scopes parent) {
        this.nested = parent.depth > 0 || parent.nested;
        this.compound.inherit(parent.compound);
    }
}
