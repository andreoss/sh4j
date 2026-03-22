/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;

/**
 * Bundles the environment, builtin context, and output buffer used during
 * variable expansion so helper methods take fewer parameters.
 *
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidStringBufferField")
final class ExpansionContext {

    /** Environment variable bindings. */
    private final Map<String, String> env;

    /** Builtin execution context. */
    private final BuiltinContext ctx;

    /** Accumulator for expanded output. */
    private final StringBuilder expanded;

    /**
     * Creates an expansion context.
     *
     * @param env Environment variable bindings
     * @param ctx Builtin execution context
     * @param expanded Accumulator for expanded output
     */
    ExpansionContext(
        final Map<String, String> env,
        final BuiltinContext ctx,
        final StringBuilder expanded
    ) {
        this.env = env;
        this.ctx = ctx;
        this.expanded = expanded;
    }

    /**
     * Returns the environment bindings.
     *
     * @return Environment variable bindings
     */
    Map<String, String> env() {
        return this.env;
    }

    /**
     * Returns the builtin execution context.
     *
     * @return Builtin execution context
     */
    BuiltinContext ctx() {
        return this.ctx;
    }

    /**
     * Returns the output accumulator.
     *
     * @return Output accumulator
     */
    StringBuilder expanded() {
        return this.expanded;
    }
}
