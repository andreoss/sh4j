/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.regex.Pattern;

/**
 * Name operand of {@code export}, {@code readonly} and {@code unset}: a
 * name that is not a POSIX name (3.216) is a special built-in utility
 * error (POSIX 2.8.1), reported as {@code bad variable name} with status 2.
 *
 * @since 1.0
 */
final class VariableName {

    /** A POSIX name. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** Built-in context for the error stream. */
    private final BuiltinIo shell;

    /** Name of the built-in that owns the operand. */
    private final String utility;

    /**
     * Creates the name checker of one built-in.
     *
     * @param shell Built-in context
     * @param utility Name of the built-in
     */
    VariableName(final BuiltinIo shell, final String utility) {
        this.shell = shell;
        this.utility = utility;
    }

    /**
     * Returns the name, or fails when it is not a POSIX name.
     *
     * @param name Name operand
     * @return The same name
     */
    String valid(final String name) {
        if (!VariableName.NAME.matcher(name).matches()) {
            this.shell.errStream().println(
                String.format("sh: %s: %s: bad variable name", this.utility, name)
            );
            throw new SpecialError(2);
        }
        return name;
    }
}
