/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Functional contract for a shell built-in (e.g. {@code echo}, {@code cd}).
 * Implementations are registered in {@link BuiltinDispatcher} and invoked by
 * the executor when the dispatcher recognises a command name.
 *
 * @since 1.0
 */
@FunctionalInterface
interface Builtin {

    /**
     * Runs the built-in.
     *
     * @param self Narrow façade over the executor for env and I/O access
     * @param call Arguments, environment view, and inline assignments
     * @return The POSIX exit status the built-in produced
     */
    int invoke(BuiltinContext self, Invocation call);
}
