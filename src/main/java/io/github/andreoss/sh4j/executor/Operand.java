/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;

/**
 * Numeric operand of {@code exit}, {@code return}, {@code shift},
 * {@code break} and {@code continue}: a malformed or negative operand,
 * or a zero loop count, is a special built-in utility error (POSIX 2.8.1),
 * reported as {@code Illegal number} with status 2.
 *
 * @since 1.0
 */
final class Operand {

    /** Built-in context for the error stream and {@code $?}. */
    private final BuiltinContext shell;

    /** Name of the built-in that owns the operand. */
    private final String name;

    /**
     * Creates the operand parser of one built-in.
     *
     * @param shell Built-in context
     * @param name Name of the built-in
     */
    Operand(final BuiltinContext shell, final String name) {
        this.shell = shell;
        this.name = name;
    }

    /**
     * Parses the first operand, or returns the fallback without one.
     *
     * @param args Operands of the built-in
     * @param fallback Value without an operand
     * @return The non-negative operand value
     */
    int value(final List<String> args, final int fallback) {
        final int result;
        if (args.isEmpty()) {
            result = fallback;
        } else {
            result = this.parse(args.get(0));
        }
        return result;
    }

    /**
     * Parses the first operand, or returns {@code $?} without one.
     *
     * @param args Operands of the built-in
     * @return The non-negative operand value
     */
    int status(final List<String> args) {
        return this.value(args, Integer.parseInt(this.shell.getSpecialParam('?')));
    }

    /**
     * Parses the loop count of {@code break} and {@code continue}: a
     * positive operand, 1 without one.
     *
     * @param args Operands of the built-in
     * @return The positive loop count
     */
    int count(final List<String> args) {
        final int result = this.value(args, 1);
        if (result == 0) {
            throw this.illegal(args.get(0));
        }
        return result;
    }

    private int parse(final String text) {
        int result;
        try {
            result = Integer.parseInt(text);
        } catch (final NumberFormatException ex) {
            result = -1;
        }
        if (result < 0) {
            throw this.illegal(text);
        }
        return result;
    }

    private SpecialError illegal(final String text) {
        this.shell.errStream().println(
            String.format("sh: %s: Illegal number: %s", this.name, text)
        );
        return new SpecialError(2);
    }
}
