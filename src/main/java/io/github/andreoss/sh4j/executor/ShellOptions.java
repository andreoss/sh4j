/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import java.util.List;

/**
 * Option names of the {@code set} built-in and their letters
 * (POSIX 2.14 set).
 *
 * @since 1.0
 */
final class ShellOptions {

    /** Option names, in listing order. */
    private static final List<String> NAMES = List.of(
        "allexport", "errexit", "monitor", "noclobber", "noexec",
        "noglob", "notify", "nounset", "verbose", "xtrace"
    );

    /** Option letters, one per name. */
    private static final String LETTERS = "aemCnfbuvx";

    /** Current shell option letters. */
    private final String flags;

    /**
     * Ctor.
     *
     * @param flags Current shell option letters
     */
    ShellOptions(final String flags) {
        this.flags = flags;
    }

    /**
     * Returns the letter of an option name.
     *
     * @param name Option name
     * @return The letter, or zero for an unknown name
     */
    static char letter(final String name) {
        final int idx = ShellOptions.NAMES.indexOf(name);
        char letter = 0;
        if (idx >= 0) {
            letter = ShellOptions.LETTERS.charAt(idx);
        }
        return letter;
    }

    /**
     * Prints the options: a table for {@code set -o}, or commands that
     * restore them for {@code set +o}.
     *
     * @param out Output stream
     * @param table True for the {@code set -o} table
     */
    void print(final PrintSink out, final boolean table) {
        if (table) {
            out.println("Current option settings");
        }
        for (int idx = 0; idx < ShellOptions.NAMES.size(); idx = idx + 1) {
            final String name = ShellOptions.NAMES.get(idx);
            final boolean set = this.flags.indexOf(ShellOptions.LETTERS.charAt(idx)) >= 0;
            if (table) {
                out.println(String.format("%-16s%s", name, ShellOptions.state(set, "on", "off")));
            } else {
                out.println(String.format("set %so %s", ShellOptions.state(set, "-", "+"), name));
            }
        }
    }

    private static String state(final boolean set, final String yes, final String not) {
        final String text;
        if (set) {
            text = yes;
        } else {
            text = not;
        }
        return text;
    }
}
