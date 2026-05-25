/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.BitSet;

/**
 * One logical input line of {@code read}, with the characters that a
 * backslash quoted (POSIX read: without {@code -r} a backslash quotes the
 * next character and a backslash-newline continues the line).
 *
 * @since 1.0
 */
final class ReadText {

    /** The text with escaping backslashes removed. */
    private final String text;

    /** Positions of the characters quoted by a backslash. */
    private final BitSet quoted;

    /** Whether the line ended with a newline. */
    private final boolean ended;

    /**
     * Ctor.
     *
     * @param text The text with escaping backslashes removed
     * @param quoted Positions of the quoted characters
     * @param ended Whether the line ended with a newline
     */
    ReadText(final String text, final BitSet quoted, final boolean ended) {
        this.text = text;
        this.quoted = quoted;
        this.ended = ended;
    }

    /**
     * Reads one logical line.
     *
     * @param self The context to read from
     * @param raw Whether backslashes are kept as they are
     * @return The line, or {@code null} at end of input
     */
    static ReadText read(final BuiltinContext self, final boolean raw) {
        final String input = self.readInputLine();
        final ReadText result;
        if (input == null) {
            result = null;
        } else if (raw) {
            result = new ReadText(
                ReadText.chomp(input), new BitSet(0), ReadText.newline(input)
            );
        } else {
            result = ReadText.escaped(self, input);
        }
        return result;
    }

    /**
     * The text.
     *
     * @return The text with escaping backslashes removed
     */
    String text() {
        return this.text;
    }

    /**
     * Tells whether a character was quoted by a backslash.
     *
     * @param pos The position of the character
     * @return True when the character was quoted
     */
    boolean quoted(final int pos) {
        return this.quoted.get(pos);
    }

    /**
     * The exit status of the read.
     *
     * @return Zero when the line ended with a newline, one otherwise
     */
    int status() {
        int code = 1;
        if (this.ended) {
            code = 0;
        }
        return code;
    }

    private static ReadText escaped(final BuiltinContext self, final String first) {
        final StringBuilder out = new StringBuilder(first.length());
        final BitSet marks = new BitSet(first.length());
        String input = first;
        boolean more = true;
        boolean done = false;
        while (more) {
            done = ReadText.newline(input);
            more = ReadText.unescape(ReadText.chomp(input), out, marks) && done;
            if (more) {
                input = self.readInputLine();
                more = input != null;
                done = more;
            }
        }
        return new ReadText(out.toString(), marks, done);
    }

    private static boolean unescape(
        final String line, final StringBuilder out, final BitSet marks
    ) {
        boolean cont = false;
        int idx = 0;
        while (idx < line.length()) {
            final char chr = line.charAt(idx);
            if (chr == '\\' && idx + 1 < line.length()) {
                marks.set(out.length());
                idx = idx + 1;
                out.append(line.charAt(idx));
            } else if (chr == '\\') {
                cont = true;
            } else {
                out.append(chr);
            }
            idx = idx + 1;
        }
        return cont;
    }

    private static String chomp(final String input) {
        String line = input;
        if (ReadText.newline(input)) {
            line = input.substring(0, input.length() - 1);
        }
        return line;
    }

    private static boolean newline(final String input) {
        return input.charAt(input.length() - 1) == '\n';
    }
}
