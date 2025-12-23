/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Throwable used to unwind a {@code break N} from N nested loops.
 *
 * @since 1.0
 */
final class BreakSignal extends RuntimeException {

    /** Serial version. */
    private static final long serialVersionUID = 1L;

    /** Remaining loop levels to break out of. */
    private final int levels;

    /**
     * Creates a break signal.
     *
     * @param levels Number of loop levels to break out of
     */
    BreakSignal(final int levels) {
        this(levels, null);
    }

    /**
     * Creates a break signal preserving the original cause.
     *
     * @param levels Number of loop levels to break out of
     * @param cause The original exception
     */
    BreakSignal(final int levels, final Throwable cause) {
        super(cause);
        this.levels = levels;
    }

    /**
     * Returns the remaining loop levels to break out of.
     *
     * @return Remaining loop levels to break out of
     */
    int getLevels() {
        return this.levels;
    }
}
