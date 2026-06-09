/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;

/**
 * Leading options of one built-in (POSIX 12.2 Utility Syntax Guidelines):
 * single letters, possibly grouped, ended by {@code --}, by {@code -} or by
 * the first operand. An unknown letter is reported as {@code Illegal option}.
 *
 * @since 1.0
 */
final class Options {

    /** Built-in context for the error stream. */
    private final BuiltinIo shell;

    /** Name of the built-in that owns the options. */
    private final String name;

    /** Option letters the built-in accepts. */
    private final String allowed;

    /** Option letters given so far. */
    private String seen;

    /** Index of the first operand. */
    private int next;

    /**
     * Creates the option reader of one built-in.
     *
     * @param shell Built-in context
     * @param name Name of the built-in
     * @param allowed Option letters the built-in accepts
     */
    Options(final BuiltinIo shell, final String name, final String allowed) {
        this.shell = shell;
        this.name = name;
        this.allowed = allowed;
        this.seen = "";
        this.next = 0;
    }

    /**
     * Reads the leading options, reporting the first unknown letter.
     *
     * @param args Arguments of the built-in
     * @return True when every option letter is accepted
     */
    boolean read(final List<String> args) {
        boolean valid = true;
        boolean more = true;
        while (valid && more && this.next < args.size()) {
            final String arg = args.get(this.next);
            if ("--".equals(arg)) {
                this.next += 1;
                more = false;
            } else if (arg.length() > 1 && arg.charAt(0) == '-') {
                valid = this.letters(arg);
                this.next += 1;
            } else {
                more = false;
            }
        }
        return valid;
    }

    /**
     * Reads the leading options of a special built-in: an unknown letter is
     * a special built-in utility error with status 2.
     *
     * @param args Arguments of the built-in
     * @return Index of the first operand
     */
    int operands(final List<String> args) {
        if (!this.read(args)) {
            throw new SpecialError(2);
        }
        return this.next;
    }

    /**
     * Returns the index of the first operand after {@link #read}.
     *
     * @return Index of the first operand
     */
    int first() {
        return this.next;
    }

    /**
     * Tells whether an option letter was given.
     *
     * @param letter Option letter
     * @return True when the letter was given
     */
    boolean has(final char letter) {
        return this.seen.indexOf(letter) >= 0;
    }

    /**
     * Tells whether one option letter was given after another one.
     *
     * @param letter Option letter
     * @param other Other option letter
     * @return True when the last {@code letter} follows the last {@code other}
     */
    boolean after(final char letter, final char other) {
        return this.seen.lastIndexOf(letter) > this.seen.lastIndexOf(other);
    }

    private boolean letters(final String arg) {
        boolean valid = true;
        int idx = 1;
        while (valid && idx < arg.length()) {
            final char letter = arg.charAt(idx);
            valid = this.allowed.indexOf(letter) >= 0;
            if (valid) {
                this.seen = this.seen.concat(String.valueOf(letter));
            } else {
                this.shell.errStream().println(
                    String.format("sh: %s: Illegal option -%c", this.name, letter)
                );
            }
            idx += 1;
        }
        return valid;
    }
}
