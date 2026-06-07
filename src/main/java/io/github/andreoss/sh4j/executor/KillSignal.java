/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.Locale;
import java.util.Map;

/**
 * Signal names and numbers understood by the {@code kill} built-in.
 *
 * @since 1.0
 */
final class KillSignal {

    /** Signal numbers by name. */
    private static final Map<String, Integer> NUMBERS = Map.ofEntries(
        Map.entry("HUP", 1), Map.entry("INT", 2), Map.entry("QUIT", 3),
        Map.entry("ILL", 4), Map.entry("TRAP", 5), Map.entry("ABRT", 6),
        Map.entry("BUS", 7), Map.entry("FPE", 8), Map.entry("KILL", 9),
        Map.entry("USR1", 10), Map.entry("SEGV", 11), Map.entry("USR2", 12),
        Map.entry("PIPE", 13), Map.entry("ALRM", 14), Map.entry("TERM", 15),
        Map.entry("CHLD", 17), Map.entry("CONT", 18), Map.entry("STOP", 19),
        Map.entry("TSTP", 20), Map.entry("TTIN", 21), Map.entry("TTOU", 22)
    );

    @GeneratedDefensive
    private KillSignal() {
    }

    /**
     * Returns the number of a signal given by name or number.
     *
     * @param spec Signal name in any case or number, empty for the default
     * @return Signal number, TERM for an empty spec, -1 for an invalid one
     */
    static int number(final String spec) {
        final int num;
        if (spec.isEmpty()) {
            num = 15;
        } else if (spec.matches("[0-9]{1,2}")) {
            num = Integer.parseInt(spec);
        } else {
            num = KillSignal.NUMBERS.getOrDefault(spec.toUpperCase(Locale.ROOT), -1);
        }
        return num;
    }

    /**
     * Returns the name of a signal number.
     *
     * @param num Signal number
     * @return Signal name, or the number as text when it has no name
     */
    static String name(final int num) {
        String name = Integer.toString(num);
        for (final Map.Entry<String, Integer> entry : KillSignal.NUMBERS.entrySet()) {
            if (entry.getValue() == num) {
                name = entry.getKey();
            }
        }
        return name;
    }
}
