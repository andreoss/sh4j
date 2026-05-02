/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Quoting and expansion consumers for {@link LexerWords}.
 *
 * @since 1.0
 */
final class LexerWordChars {

    private LexerWordChars() {
    }

    /**
     * Handles a backslash outside quotes.
     *
     * @param reader Input cursor
     * @param buf Accumulator
     */
    static void consumeBackslash(
        final CharReader reader, final StringBuilder buf
    ) {
        reader.consume();
        if (reader.peek() == '\n') {
            reader.consume();
        } else {
            buf.append('\\');
            if (!reader.isEof()) {
                buf.append((char) reader.consume());
            }
        }
    }

    /**
     * Handles a character inside double quotes.
     *
     * @param reader Input cursor
     * @param buf Accumulator
     * @param chr Current character
     */
    static void consumeDoubleQuoted(
        final CharReader reader, final StringBuilder buf, final int chr
    ) {
        if (chr == '\\') {
            reader.consume();
            buf.append('\\');
            final int next = reader.peek();
            if (next == '\n') {
                reader.consume();
                buf.deleteCharAt(buf.length() - 1);
            } else if (next == '$' || next == '`' || next == '"' || next == '\\') {
                buf.append((char) reader.consume());
            }
        } else {
            buf.append((char) reader.consume());
        }
    }

    /**
     * Handles a dollar-introduced expansion.
     *
     * @param reader Input cursor
     * @param buf Accumulator
     * @param quoted Whether the expansion sits inside double quotes
     */
    static void consumeDollar(
        final CharReader reader, final StringBuilder buf, final boolean quoted
    ) {
        reader.consume();
        buf.append('$');
        if (!reader.isEof() && reader.peek() == '(') {
            LexerWordChars.consumeBalanced(reader, buf, new Closer(')', quoted));
        } else if (!reader.isEof() && reader.peek() == '{') {
            LexerWordChars.consumeBalanced(reader, buf, new Closer('}', quoted));
        }
    }

    /**
     * Handles a {@code $(...)}, {@code $((...))} or {@code ${...}}
     * expansion: reads up to the closing character that is not quoted
     * or nested.
     *
     * @param reader Input cursor, at the opening character
     * @param buf Accumulator
     * @param closer The closing character and quoting context
     */
    static void consumeBalanced(
        final CharReader reader, final StringBuilder buf, final Closer closer
    ) {
        buf.append((char) reader.consume());
        final StringBuilder body = new StringBuilder();
        boolean done = false;
        while (!done && !reader.isEof()) {
            final int next = reader.consume();
            body.append((char) next);
            done = closer.closes(body, next);
        }
        buf.append(body);
    }

    /**
     * Handles a backtick-delimited command substitution.
     *
     * @param reader Input cursor
     * @param buf Accumulator
     */
    static void consumeBackticks(
        final CharReader reader, final StringBuilder buf
    ) {
        reader.consume();
        buf.append('`');
        boolean done = false;
        while (!done && !reader.isEof()) {
            final int next = reader.consume();
            buf.append((char) next);
            if (next == '`') {
                done = true;
            } else if (next == '\\' && !reader.isEof()) {
                buf.append((char) reader.consume());
            }
        }
    }
}
