/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
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
    private final Map<String, Builtin> registry = BuiltinRegistry.build();

    /**
     * Builds the dispatcher and registers every shell built-in.
     */
    BuiltinDispatcher() {
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
     * @param arguments   Command arguments (excluding the name)
     * @param env         The live environment view
     * @param assignments Inline assignments attached to this call
     * @return The built-in's exit code, or null when not a built-in
     */
    Integer dispatch(
        final String name,
        final BuiltinContext self,
        final List<String> arguments,
        final Map<String, String> env,
        final List<String> assignments
    ) {
        final Builtin builtin = this.registry.get(name);
        final Integer code;
        if (builtin == null) {
            code = null;
        } else {
            code = builtin.invoke(self, new Invocation(arguments, env, assignments));
        }
        return code;
    }
}
