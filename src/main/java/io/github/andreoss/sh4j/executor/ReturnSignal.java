/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Throwable used to unwind a {@code return N} from a function body.
 *
 * @since 1.0
 */
final class ReturnSignal extends RuntimeException {

    /** Serial version. */
    private static final long serialVersionUID = 1L;

    /** The exit status that the function should yield. */
    private final int code;

    /**
     * Creates a return signal.
     *
     * @param code The exit status to yield
     */
    ReturnSignal(final int code) {
        super();
        this.code = code;
    }

    /**
     * Returns the exit status to yield.
     *
     * @return The exit status to yield
     */
    int getCode() {
        return this.code;
    }
}
