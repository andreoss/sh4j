/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Throwable used to unwind {@code exit} to the shell boundary.
 *
 * @since 1.0
 */
public final class ExitSignal extends RuntimeException {

    /** Serial version. */
    private static final long serialVersionUID = 1L;

    /** Requested exit status. */
    private final int code;

    /**
     * Creates an exit signal.
     *
     * @param code Requested exit status
     */
    ExitSignal(final int code) {
        super();
        this.code = code;
    }

    /**
     * Returns the requested exit status.
     *
     * @return Requested exit status
     */
    int getCode() {
        return this.code;
    }
}
