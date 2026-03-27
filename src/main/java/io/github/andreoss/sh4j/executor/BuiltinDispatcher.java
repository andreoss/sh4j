/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;

/**
 * Holds the executor's table of built-in commands and routes calls to the
 * matching {@link Builtin}. Every built-in registered here keeps its own
 * lambda body; the dispatcher itself stays generic.
 *
 * @since 1.0
 */
final class BuiltinDispatcher {

    /** Map from built-in name to its implementation. */
    private final Map<String, Builtin> registry;

    /**
     * Builds the dispatcher and registers every shell built-in.
     */
    BuiltinDispatcher() {
        this(BuiltinRegistry.build());
    }

    /**
     * Builds the dispatcher over a given table.
     *
     * @param registry Map from built-in name to its implementation
     */
    private BuiltinDispatcher(final Map<String, Builtin> registry) {
        this.registry = registry;
    }

    /**
     * Registers a built-in by name.
     *
     * @param name    The command name (as written in source)
     * @param builtin The implementation
     */
    void register(final String name, final Builtin builtin) {
        this.registry.put(name, builtin);
    }

    /**
     * Checks whether a built-in is registered.
     *
     * @param name A command name
     * @return True when a built-in is registered under {@code name}
     */
    boolean isBuiltin(final String name) {
        return this.registry.containsKey(name);
    }

    /**
     * Dispatches to the matching built-in, or returns null when no entry
     * exists (the executor then resolves an external command).
     *
     * @param name        The command name
     * @param self        Narrow façade over the executor
     * @param call        Arguments, environment and inline assignments
     * @return The built-in's exit code, or null when not a built-in
     */
    Integer dispatch(
        final String name,
        final BuiltinContext self,
        final Invocation call
    ) {
        final Builtin builtin = this.registry.get(name);
        final Integer code;
        if (builtin == null) {
            code = null;
        } else {
            code = builtin.invoke(self, call);
        }
        return code;
    }
}
