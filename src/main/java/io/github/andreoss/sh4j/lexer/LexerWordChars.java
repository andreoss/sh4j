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
     */
    static void consumeDollar(
        final CharReader reader, final StringBuilder buf
    ) {
        reader.consume();
        buf.append('$');
        if (!reader.isEof() && reader.peek() == '(') {
            LexerWordChars.consumeBalancedParens(reader, buf);
        } else if (!reader.isEof() && reader.peek() == '{') {
            LexerWordChars.consumeBalancedBraces(reader, buf);
        }
    }

    /**
     * Handles a {@code $(...)} or {@code $((...))} expansion.
     *
     * @param reader Input cursor
     * @param buf Accumulator
     */
    static void consumeBalancedParens(
        final CharReader reader, final StringBuilder buf
    ) {
        reader.consume();
        buf.append('(');
        final boolean arith = !reader.isEof() && reader.peek() == '(';
        int depth = 1;
        if (arith) {
            reader.consume();
            buf.append('(');
            depth = 2;
        }
        while (!reader.isEof() && depth > 0) {
            final int next = reader.consume();
            buf.append((char) next);
            if (next == '(') {
                depth = depth + 1;
            } else if (next == ')') {
                depth = depth - 1;
            }
        }
    }

    /**
     * Handles a {@code ${...}} expansion.
     *
     * @param reader Input cursor
     * @param buf Accumulator
     */
    static void consumeBalancedBraces(
        final CharReader reader, final StringBuilder buf
    ) {
        reader.consume();
        buf.append('{');
        int depth = 1;
        while (!reader.isEof() && depth > 0) {
            final int next = reader.consume();
            buf.append((char) next);
            if (next == '{') {
                depth = depth + 1;
            } else if (next == '}') {
                depth = depth - 1;
            }
        }
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
