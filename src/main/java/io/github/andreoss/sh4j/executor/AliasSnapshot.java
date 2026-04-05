/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Alias table captured when the current source line started, so that an
 * alias defined on a line takes effect from the next line.
 *
 * @since 1.0
 */
final class AliasSnapshot {

    /** Alias table captured for the current line. */
    private Optional<Map<String, String>> table;

    /** Source line the captured table belongs to. */
    private int line;

    /**
     * Ctor.
     */
    AliasSnapshot() {
        this(Optional.empty(), 0);
    }

    private AliasSnapshot(final Optional<Map<String, String>> table, final int line) {
        this.table = table;
        this.line = line;
    }

    /**
     * Forgets the captured table.
     */
    void reset() {
        this.table = Optional.empty();
        this.line = -1;
    }

    /**
     * Captures the alias table when a new source line starts.
     *
     * @param current The line of the command about to run
     * @param aliases The live alias table
     */
    void refresh(final int current, final Map<String, String> aliases) {
        if (this.table.isEmpty() || current != this.line) {
            this.table = Optional.of(new HashMap<>(aliases));
            this.line = current;
        }
    }

    /**
     * Returns the captured alias table.
     *
     * @return The alias table captured for the current line
     */
    Map<String, String> table() {
        return this.table.orElseThrow();
    }
}
