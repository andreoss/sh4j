/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import java.util.Map;

/**
 * Functional contract for a shell built-in (e.g. {@code echo}, {@code cd}).
 * Implementations are registered in {@link BuiltinDispatcher} and invoked by
 * the executor when the dispatcher recognises a command name.
 * @since 1.0
 */
@FunctionalInterface
interface Builtin {

    /**
     * Runs the built-in.
     * @param self        Narrow façade over the executor for env / I/O access
     * @param arguments   The command words excluding the built-in name
     * @param env         The current environment view (may be mutated)
     * @param assignments Inline {@code KEY=VALUE} pairs attached to this call
     * @return The POSIX exit status the built-in produced
     */
    int invoke(
        BuiltinContext self,
        List<String> arguments,
        Map<String, String> env,
        List<String> assignments
    );
}
