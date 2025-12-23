/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.List;

/**
 * POSIX command-history ring buffer for the {@code fc} built-in.
 * Stores up to {@code capacity} commands with monotonically increasing
 * history numbers starting at 1. When the buffer wraps, old entries
 * are overwritten but history numbers continue to increase.
 *
 * @since 1.0
 */
final class CommandHistory {

    /** Default capacity matching POSIX HISTSIZE. */
    static final int DEFAULT_CAPACITY = 128;

    /** Ring buffer entries. */
    private final String[] buffer;

    /** Capacity of the ring buffer. */
    private final int capacity;

    /** Next history number to assign (starts at 1). */
    private int counter;

    /** Index in {@code buffer} for the oldest entry (when full). */
    private int head;

    /** Number of entries currently stored. */
    private int count;

    /**
     * Creates a history with the default capacity.
     */
    CommandHistory() {
        this(CommandHistory.DEFAULT_CAPACITY);
    }

    /**
     * Creates a history with the given capacity.
     *
     * @param capacity Maximum number of commands to retain
     */
    CommandHistory(final int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
        this.buffer = new String[capacity];
        this.counter = 1;
        this.head = 0;
        this.count = 0;
    }

    /**
     * Adds a command to the history.
     *
     * @param command The command text (empty strings are ignored)
     */
    void add(final String command) {
        if (command == null || command.isEmpty()) {
            return;
        }
        final int idx;
        if (this.count < this.capacity) {
            idx = this.count;
            this.count = this.count + 1;
        } else {
            idx = this.head;
            this.head = (this.head + 1) % this.capacity;
        }
        this.buffer[idx] = command;
        this.counter = this.counter + 1;
    }

    /**
     * Returns the number of stored commands.
     *
     * @return The number of commands currently stored
     */
    int size() {
        return this.count;
    }

    /**
     * Returns the next history number.
     *
     * @return The history number the next added command will receive
     */
    int counter() {
        return this.counter;
    }

    /**
     * Returns the most recent history number.
     *
     * @return The history number of the most recently added command, or 0
     */
    int lastNumber() {
        int result = 0;
        if (this.count != 0) {
            result = this.counter - 1;
        }
        return result;
    }

    /**
     * Looks up the command with the given history number.
     *
     * @param number The history number (1-based)
     * @return The command text, or null when not found
     */
    String get(final int number) {
        String result = null;
        if (number >= 1 && number < this.counter) {
            final int idx = this.indexForNumber(number);
            if (idx >= 0) {
                result = this.buffer[idx];
            }
        }
        return result;
    }

    /**
     * Lists commands in the inclusive range {@code [first, last]}.
     * Negative numbers are relative to the most recent entry
     * (e.g. -1 = most recent, -2 = second most recent).
     *
     * @param first Start of range (negative for relative)
     * @param last  End of range (negative for relative, 0 = last)
     * @return List of history entries in order
     */
    List<HistoryEntry> list(final int first, final int last) {
        final List<HistoryEntry> result = new ArrayList<>(0);
        final int start = this.resolveNumber(first);
        final int finish;
        if (last == 0) {
            finish = this.lastNumber();
        } else {
            finish = this.resolveNumber(last);
        }
        if (start >= 1 && finish >= start) {
            for (int num = start; num <= finish; num = num + 1) {
                final String cmd = this.get(num);
                if (cmd != null) {
                    result.add(new HistoryEntry(num, cmd));
                }
            }
        }
        return result;
    }

    private int resolveNumber(final int number) {
        int result = -1;
        if (this.count != 0) {
            if (number >= 0) {
                result = number;
            } else {
                result = this.lastNumber() + number + 1;
            }
        }
        return result;
    }

    private int indexForNumber(final int number) {
        int result = -1;
        if (this.count < this.capacity) {
            result = number - 1;
        } else {
            final int offset = number - (this.counter - this.count);
            if (offset >= 0 && offset < this.count) {
                result = (this.head + offset) % this.capacity;
            }
        }
        return result;
    }
}
