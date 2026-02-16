/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashSet;
import java.util.Map;

/**
 * The names a child process inherits.
 *
 * <p>The inherited names arrive through the {@link HashSet#HashSet(Collection)}
 * constructor and the POSIX defaults are added by the instance initialiser, so
 * {@link Executor} keeps its own constructor free of method calls.</p>
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
     * @param seed The inherited variables
     */
    Exports(final Map<String, String> seed) {
        super(seed.keySet());
    }

    {
        for (final String name : Exports.GUARDED) {
            this.add(name);
        }
    }
}
