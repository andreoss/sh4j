/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Runs an action on the calling thread and reports the unchecked exception
 * it threw, letting errors propagate unchanged.
 *
 * @since 1.0
 */
final class Attempt {

    @GeneratedDefensive
    private Attempt() {
    }

    /**
     * Runs the action on the calling thread.
     *
     * @param action The action
     * @return The unchecked exception the action threw, empty when it
     *  completed normally
     */
    static Optional<RuntimeException> failure(final Runnable action) {
        final Optional<Throwable> thrown = CompletableFuture
            .runAsync(action, Runnable::run)
            .handle((done, error) -> Optional.ofNullable(error).map(Throwable::getCause))
            .join();
        return thrown.map(Attempt::unchecked);
    }

    private static RuntimeException unchecked(final Throwable thrown) {
        if (thrown instanceof Error) {
            throw (Error) thrown;
        }
        return (RuntimeException) thrown;
    }
}
