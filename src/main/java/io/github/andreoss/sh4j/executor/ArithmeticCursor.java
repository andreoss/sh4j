/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Mutable cursor over the text of an arithmetic expression.
 *
 * @since 1.0
 */
final class ArithmeticCursor {

    /** The input text. */
    private final String text;

    /** Current position in the text. */
    private int pos;

    /**
     * Creates a cursor over the given text.
     *
     * @param text The input text
     */
    ArithmeticCursor(final String text) {
        this.text = text;
        this.pos = 0;
    }

    /**
     * Returns the input text.
     *
     * @return The input text
     */
    String text() {
        return this.text;
    }

    /**
     * Returns the current position.
     *
     * @return The current position
     */
    int pos() {
        return this.pos;
    }

    /**
     * Moves the cursor to the given position.
     *
     * @param value The new position
     */
    void pos(final int value) {
        this.pos = value;
    }

    /**
     * Advances the cursor by one character.
     */
    void advance() {
        this.pos = this.pos + 1;
    }

    /**
     * Advances the cursor by the given number of characters.
     *
     * @param count Number of characters to skip
     */
    void advance(final int count) {
        this.pos = this.pos + count;
    }

    /**
     * Checks whether the cursor is at end of input.
     *
     * @return True at end of input
     */
    boolean eof() {
        return this.pos >= this.text.length();
    }

    /**
     * Returns the character at the given offset.
     *
     * @param offset Offset from the cursor
     * @return The character, or zero when out of range
     */
    char charAt(final int offset) {
        char result = 0;
        final int idx = this.pos + offset;
        if (idx < this.text.length()) {
            result = this.text.charAt(idx);
        }
        return result;
    }

    /**
     * Checks whether the cursor points at the given character.
     *
     * @param chr The expected character
     * @return True when the cursor points at {@code chr}
     */
    boolean currentIs(final char chr) {
        return !this.eof() && this.text.charAt(this.pos) == chr;
    }

    /**
     * Checks whether the text starts at the cursor with the given value.
     *
     * @param value The expected prefix
     * @return True when the prefix matches
     */
    boolean hasPrefix(final String value) {
        return this.text.startsWith(value, this.pos);
    }

    /**
     * Skips whitespace at the cursor.
     */
    void skipWhitespace() {
        while (!this.eof()
            && Character.isWhitespace(this.text.charAt(this.pos))) {
            this.pos = this.pos + 1;
        }
    }
}
