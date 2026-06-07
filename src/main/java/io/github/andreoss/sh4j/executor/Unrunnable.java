/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.IOException;

/**
 * Reports a command that cannot be run: not found or not executable
 * (POSIX 2.8.2, 2.9.1.1).
 *
 * @since 1.0
 */
final class Unrunnable {

    /** Exit code for a command found but not executable. */
    static final int NOT_EXECUTABLE = 126;

    /** The shell whose standard error receives the diagnostic. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell whose standard error receives the diagnostic
     */
    Unrunnable(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Reports a command name that resolved to no executable file.
     *
     * @param name The command name
     * @return The exit status: 126 for a directory, 127 otherwise
     */
    int missing(final String name) {
        final int code;
        if (name.contains("/")
            && RedirectionHandler.resolve(name, this.shell.currentDirectory()).isDirectory()) {
            code = this.report(name, "is a directory", Unrunnable.NOT_EXECUTABLE);
        } else {
            code = this.report(name, "not found", ExternalCommand.NOT_FOUND);
        }
        return code;
    }

    /**
     * Reports a command whose process could not be started.
     *
     * @param name The command name
     * @param exc The failure to start the process
     * @return The exit status: 127 when the file is missing, 126 otherwise
     */
    int failed(final String name, final IOException exc) {
        final String msg = exc.getMessage();
        final int code;
        if (msg != null && (msg.contains("error=2") || msg.contains("No such file"))) {
            code = this.report(name, "not found", ExternalCommand.NOT_FOUND);
        } else {
            code = this.report(name, "cannot execute", Unrunnable.NOT_EXECUTABLE);
        }
        return code;
    }

    private int report(final String name, final String reason, final int code) {
        this.shell.errStream().println(String.format("sh: %s: %s", name, reason));
        return code;
    }
}
