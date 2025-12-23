/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * A single history entry (number + command text).
 *
 * @since 1.0
 */
final class HistoryEntry {

    /** History number of the command. */
    private final int number;

    /** Command text. */
    private final String command;

    /**
     * Creates an entry.
     *
     * @param number History number
     * @param command Command text
     */
    HistoryEntry(final int number, final String command) {
        this.number = number;
        this.command = command;
    }

    /**
     * Returns the history number.
     *
     * @return History number
     */
    int number() {
        return this.number;
    }

    /**
     * Returns the command text.
     *
     * @return Command text
     */
    String command() {
        return this.command;
    }
}
