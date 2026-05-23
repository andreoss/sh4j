/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.regex.Pattern;

/**
 * Integer constant of an arithmetic expression, read as in ISO C: a
 * leading {@code 0x} or {@code 0X} means hexadecimal, a leading {@code 0}
 * means octal, anything else decimal (POSIX 2.6.4).
 *
 * @since 1.0
 */
final class ArithmeticNumber {

    /** Hexadecimal constant. */
    private static final Pattern HEX = Pattern.compile("0[xX][0-9a-fA-F]+");

    /** Octal constant, including a lone zero. */
    private static final Pattern OCTAL = Pattern.compile("0[0-7]*");

    /** Decimal constant. */
    private static final Pattern DECIMAL = Pattern.compile("[1-9][0-9]*");

    /** Cursor positioned at the constant. */
    private final ArithmeticCursor cursor;

    /**
     * Ctor.
     *
     * @param cursor Cursor positioned at the constant
     */
    ArithmeticNumber(final ArithmeticCursor cursor) {
        this.cursor = cursor;
    }

    /**
     * Consumes the constant and returns its value.
     *
     * @return The value, wrapped to a 32-bit integer
     */
    int value() {
        final int start = this.cursor.pos();
        while (!this.cursor.eof() && ArithmeticNumber.isWord(this.cursor.charAt(0))) {
            this.cursor.advance();
        }
        final String token = this.cursor.text().substring(start, this.cursor.pos());
        if (token.isEmpty()) {
            throw new ArithmeticException(
                String.format(
                    "syntax error: unexpected \"%s\"", this.cursor.text().substring(start)
                )
            );
        }
        return ArithmeticNumber.parse(token);
    }

    private static int parse(final String token) {
        final int result;
        if (ArithmeticNumber.HEX.matcher(token).matches()) {
            result = ArithmeticNumber.digits(token.substring(2), 16);
        } else if (ArithmeticNumber.OCTAL.matcher(token).matches()) {
            result = ArithmeticNumber.digits(token, 8);
        } else if (ArithmeticNumber.DECIMAL.matcher(token).matches()) {
            result = ArithmeticNumber.digits(token, 10);
        } else {
            throw new ArithmeticException(
                String.format("invalid number \"%s\"", token)
            );
        }
        return result;
    }

    private static int digits(final String text, final int base) {
        int num = 0;
        for (int idx = 0; idx < text.length(); idx += 1) {
            num = num * base + Character.digit(text.charAt(idx), base);
        }
        return num;
    }

    private static boolean isWord(final char chr) {
        return Character.isLetterOrDigit(chr) || chr == '_';
    }
}
