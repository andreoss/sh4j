/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Single-character lookahead cursor over the lexer's input stream. The
 * cursor caches the next character so {@link #peek()} is O(1) and
 * {@link #consume()} fetches the following character lazily.
 * @since 1.0
 */
public final class CharReader {

    /** Underlying character source. */
    private final Reader reader;
    /** Cached next character (or -1 at EOF). */
    private int peeked;
    /** 1-based line number of the cached character. */
    private int line;
    /** 0-based column number of the cached character. */
    private int column;

    /**
     * Reads from the given string.
     * @param input The script source
     */
    public CharReader(final String input) {
        this(new StringReader(input));
    }

    /**
     * Reads from the given reader.
     * @param reader The source to consume
     */
    public CharReader(final Reader reader) {
        this.reader = reader;
        this.line = 1;
        this.column = 0;
        this.read();
    }

    /**
     * Looks at the next character without consuming it.
     * @return The next character, or -1 at end-of-input
     */
    public int peek() {
        return this.peeked;
    }

    /**
     * Advances the cursor.
     * @return The character that was previously at the cursor
     */
    public int consume() {
        final int cur = this.peeked;
        if (cur == '\n') {
            this.line = this.line + 1;
            this.column = 0;
        } else if (cur != -1) {
            this.column = this.column + 1;
        }
        this.read();
        return cur;
    }

    /**
     * @return True when the cursor has reached end-of-input
     */
    public boolean isEof() {
        return this.peeked == -1;
    }

    /**
     * @return 1-based line number of the next character
     */
    public int getLine() {
        return this.line;
    }

    /**
     * @return 0-based column number of the next character
     */
    public int getColumn() {
        return this.column;
    }

    /**
     * Fills the lookahead slot from the underlying reader, swallowing
     * IO exceptions as end-of-input (consistent with the contract that
     * {@link #peek()} can never fail).
     */
    private void read() {
        try {
            this.peeked = this.reader.read();
        } catch (final IOException ex) {
            this.peeked = -1;
        }
    }
}
