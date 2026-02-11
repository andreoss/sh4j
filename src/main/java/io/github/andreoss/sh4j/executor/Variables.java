/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.Map;

/**
 * The executor's live variable table, seeded with the POSIX defaults.
 *
 * <p>Seeding happens while the map is constructed so that an {@link Executor}
 * can install it with a single assignment and keep its constructor free of
 * method calls.</p>
 *
 * @since 1.0
 */
final class Variables extends HashMap<String, String> {

    /** Serial id. */
    private static final long serialVersionUID = 1L;

    /** Default IFS value (space, tab, newline). */
    static final String DEFAULT_IFS =
        new StringBuilder().append(' ').append('\t').append((char) 10).toString();

    /** Default fallback PATH when none is set. */
    private static final String DEFAULT_PATH = "/usr/local/bin:/usr/bin:/bin";

    /** Names the shell always exports. */
    static final String[] GUARDED = {"HOME", "PATH", "PWD"};

    /**
     * Creates the table from a caller-supplied environment view.
     *
     * @param seed The inherited variables, or null for none
     * @param directory The working directory backing {@code PWD}
     */
    Variables(final Map<String, String> seed, final String directory) {
        super();
        if (seed != null) {
            this.putAll(seed);
        }
        this.putIfAbsent("IFS", Variables.DEFAULT_IFS);
        this.putIfAbsent(
            "HOME", System.getProperty("user.home", "/root")
        );
        this.putIfAbsent("PATH", System.getenv("PATH"));
        final String path = this.get("PATH");
        if (path == null || path.isEmpty()) {
            this.put("PATH", Variables.DEFAULT_PATH);
        }
        this.putIfAbsent("PWD", directory);
        this.putIfAbsent(
            "PPID",
            Long.toString(
                ProcessHandle.current().parent()
                    .orElse(ProcessHandle.current()).pid()
            )
        );
    }
}
