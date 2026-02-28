/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds the set of names a child process inherits.
 *
 * @since 1.0
 */
final class Exports {

    /** Names the shell always exports. */
    private static final String[] GUARDED = {"HOME", "PATH", "PWD"};

    private Exports() {
    }

    /**
     * Creates the export set from an inherited environment view.
     *
     * @param seed The inherited variables
     * @return The names to export
     */
    static Set<String> of(final Map<String, String> seed) {
        final Set<String> names = new HashSet<>(seed.keySet());
        for (final String guarded : Exports.GUARDED) {
            names.add(guarded);
        }
        return names;
    }
}
