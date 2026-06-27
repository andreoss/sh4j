/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Name that {@code export}, {@code readonly} or {@code unset} changes: a
 * readonly name is a special built-in utility error (POSIX 2.8.1, 2.14),
 * reported as {@code readonly variable} with status 1, which exits a
 * non-interactive shell unless the utility runs through {@code command}.
 *
 * @since 1.0
 */
final class WritableName {

    /** Built-in context for readonly state and the error stream. */
    private final BuiltinContext shell;

    /**
     * Creates the readonly checker.
     *
     * @param shell Built-in context
     */
    WritableName(final BuiltinContext shell) {
        this.shell = shell;
    }

    /**
     * Returns the name, or fails when it is readonly.
     *
     * @param name Variable name
     * @return The same name
     */
    String writable(final String name) {
        if (this.shell.isReadonly(name)) {
            this.shell.errStream().println(String.format("sh: %s: readonly variable", name));
            throw new SpecialError(1);
        }
        return name;
    }
}
