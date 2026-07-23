/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Backslash escapes of the {@code printf} built-in: in the format, and in
 * an argument of the {@code %b} conversion, where {@code \c} ends output.
 *
 * @since 1.0
 */
final class PrintfEscapes {

    /** Bytes of the escapes of backslash, a, b, f, n, r, t and v, in order. */
    private static final byte[] CODES = {'\\', 7, '\b', '\f', '\n', '\r', '\t', 11};

    @GeneratedDefensive
    private PrintfEscapes() {
    }

    /**
     * Decodes the escape at a backslash of the format.
     *
     * @param text The format
     * @param start Index of the backslash
     * @param out Destination of the decoded bytes
     * @return Index just past the escape
     */
    static int format(final String text, final int start, final ByteArrayOutputStream out) {
        final int next = start + 1;
        final int end;
        if (next < text.length() && PrintfEscapes.isOctal(text.charAt(next))) {
            end = PrintfEscapes.octal(text, next, out);
        } else {
            end = PrintfEscapes.simple(text, next, out);
        }
        return end;
    }

    /**
     * Decodes an argument of {@code %b}.
     *
     * @param text The argument
     * @param out Destination of the decoded bytes
     * @return False when {@code \c} asks to end all output
     */
    static boolean argument(final String text, final ByteArrayOutputStream out) {
        final StringBuilder plain = new StringBuilder(text.length());
        boolean going = true;
        int idx = 0;
        while (going && idx < text.length()) {
            final char chr = text.charAt(idx);
            if (chr == '\\' && text.startsWith("c", idx + 1)) {
                going = false;
            } else if (chr == '\\') {
                PrintfEscapes.flush(plain, out);
                idx = PrintfEscapes.inArgument(text, idx + 1, out);
            } else {
                plain.append(chr);
                idx += 1;
            }
        }
        PrintfEscapes.flush(plain, out);
        return going;
    }

    /**
     * Writes text as UTF-8 and empties it.
     *
     * @param plain The text
     * @param out Destination of the bytes
     */
    static void flush(final StringBuilder plain, final ByteArrayOutputStream out) {
        out.writeBytes(plain.toString().getBytes(StandardCharsets.UTF_8));
        plain.setLength(0);
    }

    private static int inArgument(
        final String text, final int next, final ByteArrayOutputStream out
    ) {
        final int end;
        if (next < text.length() && text.charAt(next) == '0') {
            end = PrintfEscapes.octal(text, next + 1, out);
        } else if (next < text.length() && PrintfEscapes.isOctal(text.charAt(next))) {
            end = PrintfEscapes.octal(text, next, out);
        } else {
            end = PrintfEscapes.simple(text, next, out);
        }
        return end;
    }

    private static int octal(final String text, final int from, final ByteArrayOutputStream out) {
        int value = 0;
        int idx = from;
        final int limit = Math.min(text.length(), from + 3);
        while (idx < limit && PrintfEscapes.isOctal(text.charAt(idx))) {
            value = value * 8 + text.charAt(idx) - '0';
            idx += 1;
        }
        out.write(value);
        return idx;
    }

    private static int simple(final String text, final int next, final ByteArrayOutputStream out) {
        final int end;
        if (next < text.length()) {
            final int pos = "\\abfnrtv".indexOf(text.charAt(next));
            if (pos < 0) {
                out.write('\\');
                end = next;
            } else {
                out.write(PrintfEscapes.CODES[pos]);
                end = next + 1;
            }
        } else {
            out.write('\\');
            end = next;
        }
        return end;
    }

    private static boolean isOctal(final char chr) {
        return chr >= '0' && chr <= '7';
    }
}
