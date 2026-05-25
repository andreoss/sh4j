/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.Map;

/**
 * Signal names and numbers understood by the {@code kill} built-in.
 *
 * @since 1.0
 */
final class KillSignal {

    /** Signal numbers by name. */
    private static final Map<String, Integer> NUMBERS = Map.of(
        "HUP", 1, "INT", 2, "QUIT", 3, "KILL", 9, "TERM", 15
    );

    @GeneratedDefensive
    private KillSignal() {
    }

    /**
     * Returns the number of a signal given by name or number.
     *
     * @param spec Signal name or number, empty for the default
     * @return Signal number, TERM for an unknown name
     */
    static int number(final String spec) {
        int num;
        try {
            num = Integer.parseInt(spec);
        } catch (final NumberFormatException ex) {
            num = KillSignal.NUMBERS.getOrDefault(spec, 15);
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
