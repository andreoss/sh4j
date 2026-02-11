/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The names a child process inherits.
 *
 * <p>The POSIX default variables are always exported, so the set is completed
 * while it is built rather than by a follow-up call from the constructor.</p>
 *
 * @since 1.0
 */
final class Exports extends HashSet<String> {

    /** Serial id. */
    private static final long serialVersionUID = 1L;

    /** Names the shell always exports. */
    private static final String[] GUARDED = {"HOME", "PATH", "PWD"};

    /**
     * Creates the export set.
     *
     * @param seed The inherited variable names
     */
    Exports(final Map<String, String> seed) {
        super();
        if (seed != null) {
            this.addAll(seed.keySet());
        }
        for (final String name : Exports.GUARDED) {
            this.add(name);
        }
    }
}
