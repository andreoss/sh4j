/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Tells when the body of a {@code $(...)} or {@code ${...}} expansion
 * read so far is complete.
 *
 * @since 1.0
 */
final class Closer {

    /** The closing character. */
    private final int close;

    /** Whether the expansion sits inside double quotes. */
    private final boolean quoted;

    /**
     * Ctor.
     *
     * @param close The closing character
     * @param quoted Whether the expansion sits inside double quotes
     */
    Closer(final int close, final boolean quoted) {
        this.close = close;
        this.quoted = quoted;
    }

    /**
     * Checks whether the body ends the expansion.
     *
     * @param body The body read so far
     * @param last The character just read
     * @return True when the last character closes the expansion
     */
    boolean closes(final CharSequence body, final int last) {
        final boolean result;
        if (last == ')' && this.close == ')') {
            result = new Nesting(body.toString()).paren(0) > 0;
        } else if (last == '}' && this.close == '}') {
            result = new Nesting(body.toString()).brace(0, this.quoted) > 0;
        } else {
            result = false;
        }
        return result;
    }
}
