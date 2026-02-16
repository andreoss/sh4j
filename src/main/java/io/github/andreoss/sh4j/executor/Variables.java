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
 * <p>The caller's environment is copied by the {@link HashMap#HashMap(Map)}
 * constructor and the defaults are then applied by the instance initialiser, so
 * {@link Executor} can install the table with one assignment and keep its own
 * constructor free of method calls.</p>
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

    /** Fallback working directory when the property is unset. */
    static final String FALLBACK_DIR = "/";

    /** Home-directory fallback when the property is unset. */
    private static final String FALLBACK_HOME = "/root";

    /** Working directory backing {@code PWD}. */
    private final String directory = System.getProperty(
        "user.dir", Variables.FALLBACK_DIR
    );

    /**
     * Creates the table from a caller-supplied environment view.
     *
     * @param seed The inherited variables, or null for none
     */
    Variables(final Map<String, String> seed) {
        super(seed == null ? Variables.empty() : seed);
    }

    private static Map<String, String> empty() {
        return new HashMap<>();
    }

    {
        this.putIfAbsent("IFS", Variables.DEFAULT_IFS);
        this.putIfAbsent(
            "HOME", System.getProperty("user.home", Variables.FALLBACK_HOME)
        );
        this.putIfAbsent("PATH", System.getenv("PATH"));
        final String path = this.get("PATH");
        if (path == null || path.isEmpty()) {
            this.put("PATH", Variables.DEFAULT_PATH);
        }
        this.putIfAbsent("PWD", this.directory);
        this.putIfAbsent(
            "PPID",
            Long.toString(
                ProcessHandle.current().parent()
                    .orElse(ProcessHandle.current()).pid()
            )
        );
    }
}
