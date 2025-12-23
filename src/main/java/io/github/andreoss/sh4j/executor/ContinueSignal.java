/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Throwable used to unwind a {@code continue N} from N nested loops.
 *
 * @since 1.0
 */
final class ContinueSignal extends RuntimeException {

    /** Serial version. */
    private static final long serialVersionUID = 1L;

    /** Remaining loop levels to continue at. */
    private final int levels;

    /**
     * Creates a continue signal.
     *
     * @param levels Number of loop levels to skip over
     */
    ContinueSignal(final int levels) {
        this(levels, null);
    }

    /**
     * Creates a continue signal preserving the original cause.
     *
     * @param levels Number of loop levels to skip over
     * @param cause   The original exception
     */
    ContinueSignal(final int levels, final Throwable cause) {
        super(cause);
        this.levels = levels;
    }

    /**
     * Returns the remaining loop levels to skip over.
     *
     * @return Remaining loop levels to skip over
     */
    int getLevels() {
        return this.levels;
    }
}
