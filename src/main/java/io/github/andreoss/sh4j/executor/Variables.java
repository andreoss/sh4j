/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the executor's live variable table with the POSIX defaults applied.
 *
 * @since 1.0
 */
final class Variables {

    /** Default IFS value (space, tab, newline). */
    static final String DEFAULT_IFS = " \t\n";

    /** Fallback working directory when the property is unset. */
    static final String FALLBACK_DIR = "/";

    private Variables() {
    }

    /**
     * Creates the table from a caller-supplied environment view.
     *
     * @param seed The inherited variables
     * @return The seeded table
     */
    static Map<String, String> of(final Map<String, String> seed) {
        final Map<String, String> table = new HashMap<>(seed);
        table.putIfAbsent("IFS", Variables.DEFAULT_IFS);
        table.putIfAbsent(
            "HOME", System.getProperty("user.home", "/root")
        );
        table.putIfAbsent("PATH", System.getenv("PATH"));
        final String path = table.get("PATH");
        if (path == null || path.isEmpty()) {
            table.put("PATH", "/usr/local/bin:/usr/bin:/bin");
        }
        table.putIfAbsent(
            "PWD", System.getProperty("user.dir", Variables.FALLBACK_DIR)
        );
        table.putIfAbsent(
            "PPID",
            Long.toString(
                ProcessHandle.current().parent()
                    .orElse(ProcessHandle.current()).pid()
            )
        );
        return table;
    }
}
