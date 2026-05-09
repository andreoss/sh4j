/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Throwable for a special built-in utility error (POSIX 2.8.1): it exits a
 * non-interactive shell, unless the utility runs through {@code command}.
 *
 * @since 1.0
 */
final class SpecialError extends RuntimeException {

    /** Serial version. */
    private static final long serialVersionUID = 1L;

    /** Exit status of the failed utility. */
    private final int code;

    /**
     * Creates a special built-in error.
     *
     * @param code Exit status of the failed utility
     */
    SpecialError(final int code) {
        super();
        this.code = code;
    }

    /**
     * Returns the exit status of the failed utility.
     *
     * @return Exit status
     */
    int getCode() {
        return this.code;
    }
}
