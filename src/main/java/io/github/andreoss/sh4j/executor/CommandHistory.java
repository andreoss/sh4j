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
    private int nextNumber;
    /** Index in {@code buffer} for the oldest entry (when full). */
    private int head;
    /** Number of entries currently stored. */
    private int count;

    /**
     * Creates a history with the default capacity.
     */
    CommandHistory() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a history with the given capacity.
     * @param capacity Maximum number of commands to retain
     */
    CommandHistory(final int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
        this.buffer = new String[capacity];
        this.nextNumber = 1;
        this.head = 0;
        this.count = 0;
    }

    /**
     * Adds a command to the history.
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
        this.nextNumber = this.nextNumber + 1;
    }

    /**
     * @return The number of commands currently stored
     */
    int size() {
        return this.count;
    }

    /**
     * @return The history number that will be assigned to the next
     *     added command (1 when empty, N+1 after N commands)
     */
    int nextNumber() {
        return this.nextNumber;
    }

    /**
     * @return The history number of the most recently added command,
     *     or 0 when the history is empty
     */
    int lastNumber() {
        if (this.count == 0) {
            return 0;
        }
        return this.nextNumber - 1;
    }

    /**
     * Looks up the command with the given history number.
     * @param number The history number (1-based)
     * @return The command text, or null when not found
     */
    String get(final int number) {
        if (number < 1 || number >= this.nextNumber) {
            return null;
        }
        final int idx = this.indexForNumber(number);
        if (idx < 0) {
            return null;
        }
        return this.buffer[idx];
    }

    /**
     * Lists commands in the inclusive range {@code [first, last]}.
     * Negative numbers are relative to the most recent entry
     * (e.g. -1 = most recent, -2 = second most recent).
     * @param first Start of range (negative for relative)
     * @param last  End of range (negative for relative, 0 = last)
     * @return List of history entries in order
     */
    List<Entry> list(final int first, final int last) {
        final List<Entry> result = new ArrayList<>();
        final int actualFirst = this.resolveNumber(first);
        int actualLast = last == 0 ? this.lastNumber() : this.resolveNumber(last);
        if (actualFirst < 1 || actualLast < actualFirst) {
            return result;
        }
        for (int num = actualFirst; num <= actualLast; num = num + 1) {
            final String cmd = this.get(num);
            if (cmd != null) {
                result.add(new Entry(num, cmd));
            }
        }
        return result;
    }

    /**
     * Resolves a possibly-negative history number to an absolute number.
     * @param number The number to resolve (negative = relative to last)
     * @return Absolute history number, or -1 when the history is empty
     */
    private int resolveNumber(final int number) {
        if (this.count == 0) {
            return -1;
        }
        if (number >= 0) {
            return number;
        }
        return this.lastNumber() + number + 1;
    }

    /**
     * Computes the buffer index for a given history number.
     * @param number The history number
     * @return Buffer index, or -1 when the number is out of range
     */
    private int indexForNumber(final int number) {
        if (this.count < this.capacity) {
            return number - 1;
        }
        final int offset = number - (this.nextNumber - this.count);
        if (offset < 0 || offset >= this.count) {
            return -1;
        }
        return (this.head + offset) % this.capacity;
    }

    /**
     * A single history entry (number + command text).
     * @since 1.0
     */
    static final class Entry {
        private final int number;
        private final String command;

        Entry(final int number, final String command) {
            this.number = number;
            this.command = command;
        }

        int number() {
            return this.number;
        }

        String command() {
            return this.command;
        }
    }
}
