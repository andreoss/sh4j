/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;

/**
 * Variables seen by an arithmetic expansion: the command environment it
 * reads from and the shell variables its assignments persist to.
 *
 * @since 1.0
 */
final class ArithmeticScope {

    /** Command environment used for lookup; nullable. */
    private final Map<String, String> local;

    /** Shell variables that assignments persist to; nullable. */
    private final Map<String, String> shared;

    /**
     * Ctor for a scope whose assignments stay in the environment.
     *
     * @param env The environment, nullable
     */
    ArithmeticScope(final Map<String, String> env) {
        this(env, null);
    }

    /**
     * Ctor.
     *
     * @param local The command environment used for lookup, nullable
     * @param shared The shell variables assignments persist to, nullable
     */
    ArithmeticScope(final Map<String, String> local, final Map<String, String> shared) {
        this.local = local;
        this.shared = shared;
    }

    /**
     * Returns the value of a variable, or {@code "0"} when unset.
     *
     * @param name The variable name
     * @return The value
     */
    String value(final String name) {
        final String result;
        if (this.local == null) {
            result = "0";
        } else {
            result = this.local.getOrDefault(name, "0");
        }
        return result;
    }

    /**
     * Assigns a variable in the command environment and the shell.
     *
     * @param name The variable name
     * @param value The new value
     */
    void assign(final String name, final int value) {
        final String text = String.valueOf(value);
        if (this.local != null) {
            this.local.put(name, text);
        }
        if (this.shared != null) {
            this.shared.put(name, text);
        }
    }
}
