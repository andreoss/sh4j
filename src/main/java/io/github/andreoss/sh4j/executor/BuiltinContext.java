/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Narrow façade that built-ins use to reach back into the executor for
 * shared resources (environment, I/O streams, expansion helpers, ...)
 * without having to depend on the full {@code Executor} class.
 * @since 1.0
 */
interface BuiltinContext extends BuiltinExpansion, BuiltinVariables, BuiltinIo, BuiltinJobs {
}
