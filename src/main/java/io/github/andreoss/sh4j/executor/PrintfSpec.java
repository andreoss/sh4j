/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * One conversion specification of the {@code printf} built-in: flags,
 * field width, precision and conversion character, read from the format at
 * a percent sign.
 *
 * @since 1.0
 */
final class PrintfSpec {

    /** Largest field width or precision honoured. */
    private static final int LIMIT = 1 << 20;

    /** Flags given. */
    private final String flags;

    /** Minimum field width. */
    private final int width;

    /** Precision, negative when none is given. */
    private final int precision;

    /** Conversion character, zero when the format ends first. */
    private final char conversion;

    /** Index just past the specification. */
    private final int end;

    /**
     * Ctor.
     *
     * @param flags Flags given
     * @param width Minimum field width
     * @param precision Precision, negative when none is given
     * @param conversion Conversion character, zero when the format ends first
     * @param end Index just past the specification
     */
    private PrintfSpec(
        final String flags, final int width, final int precision,
        final char conversion, final int end
    ) {
        this.flags = flags;
        this.width = width;
        this.precision = precision;
        this.conversion = conversion;
        this.end = end;
    }

    /**
     * Reads a specification; a {@code *} width or precision takes the next
     * argument.
     *
     * @param format The format
     * @param start Index of the percent sign
     * @param args The arguments
     * @return The specification
     */
    static PrintfSpec read(final String format, final int start, final PrintfArguments args) {
        int pos = start + 1;
        while (pos < format.length() && "-+ #0".indexOf(format.charAt(pos)) >= 0) {
            pos += 1;
        }
        String flags = format.substring(start + 1, pos);
        long width;
        if (format.startsWith("*", pos)) {
            width = args.integer();
            pos += 1;
            if (width < 0L) {
                flags = flags.concat("-");
                width = -width;
            }
        } else {
            final int stop = PrintfSpec.skipDigits(format, pos);
            width = PrintfSpec.number(format.substring(pos, stop));
            pos = stop;
        }
        long precision = -1L;
        if (format.startsWith(".", pos)) {
            pos += 1;
            if (format.startsWith("*", pos)) {
                precision = Math.max(-1L, args.integer());
                pos += 1;
            } else {
                final int stop = PrintfSpec.skipDigits(format, pos);
                precision = PrintfSpec.number(format.substring(pos, stop));
                pos = stop;
            }
        }
        char conversion = 0;
        if (pos < format.length()) {
            conversion = format.charAt(pos);
            pos += 1;
        }
        return new PrintfSpec(
            flags,
            (int) Math.min(width, PrintfSpec.LIMIT),
            (int) Math.min(precision, PrintfSpec.LIMIT),
            conversion,
            pos
        );
    }

    /**
     * Gives the conversion character.
     *
     * @return The character, zero when the format ended first
     */
    char conversion() {
        return this.conversion;
    }

    /**
     * Gives the index just past the specification.
     *
     * @return The index
     */
    int end() {
        return this.end;
    }

    /**
     * Formats an integer for {@code d}, {@code i}, {@code o}, {@code u},
     * {@code x} or {@code X}.
     *
     * @param value The value
     * @return The field
     */
    byte[] integer(final long value) {
        String body = this.digits(value);
        if (this.precision == 0 && value == 0L) {
            body = "";
        } else if (body.length() < this.precision) {
            body = "0".repeat(this.precision - body.length()).concat(body);
        }
        if (this.conversion == 'o' && this.has('#') && !body.startsWith("0")) {
            body = "0".concat(body);
        }
        return this.field(this.head(value), body, this.precision < 0);
    }

    /**
     * Formats a floating point number for {@code f}, {@code F}, {@code e},
     * {@code E}, {@code g} or {@code G}.
     *
     * @param value The value
     * @return The field
     */
    byte[] real(final double value) {
        final boolean finite = Double.isFinite(value);
        String body;
        if (Double.isNaN(value)) {
            body = "nan";
        } else if (finite) {
            body = new PrintfReal(
                Character.toLowerCase(this.conversion), this.precision, this.has('#')
            ).render(Math.abs(value));
        } else {
            body = "inf";
        }
        if (Character.isUpperCase(this.conversion)) {
            body = body.toUpperCase(Locale.ROOT);
        }
        return this.field(
            this.sign(!Double.isNaN(value) && Double.doubleToRawLongBits(value) < 0L),
            body,
            finite
        );
    }

    /**
     * Formats text for {@code s} or {@code b}: the precision cuts it to
     * that many bytes.
     *
     * @param text The text as bytes
     * @return The field
     */
    byte[] text(final byte[] text) {
        byte[] body = text;
        if (this.precision >= 0 && this.precision < text.length) {
            body = Arrays.copyOf(text, this.precision);
        }
        return this.pad(body);
    }

    /**
     * Pads a field with spaces to the width, on the left unless the
     * {@code -} flag is given.
     *
     * @param body The field
     * @return The padded field
     */
    byte[] pad(final byte[] body) {
        byte[] result = body;
        final int gap = this.width - body.length;
        if (gap > 0) {
            result = new byte[this.width];
            Arrays.fill(result, (byte) ' ');
            if (this.has('-')) {
                System.arraycopy(body, 0, result, 0, body.length);
            } else {
                System.arraycopy(body, 0, result, gap, body.length);
            }
        }
        return result;
    }

    private byte[] field(final String head, final String body, final boolean zeros) {
        String field = head.concat(body);
        final int gap = this.width - field.length();
        if (gap > 0 && zeros && this.has('0') && !this.has('-')) {
            field = head.concat("0".repeat(gap)).concat(body);
        }
        return this.pad(field.getBytes(StandardCharsets.US_ASCII));
    }

    private String digits(final long value) {
        return switch (this.conversion) {
            case 'o' -> Long.toOctalString(value);
            case 'u' -> Long.toUnsignedString(value);
            case 'x' -> Long.toHexString(value);
            case 'X' -> Long.toHexString(value).toUpperCase(Locale.ROOT);
            default -> Long.toString(value).replace("-", "");
        };
    }

    private String head(final long value) {
        final String head;
        if ((this.conversion == 'x' || this.conversion == 'X') && this.has('#')
            && value != 0L) {
            head = "0".concat(String.valueOf(this.conversion));
        } else if (this.conversion == 'd' || this.conversion == 'i') {
            head = this.sign(value < 0L);
        } else {
            head = "";
        }
        return head;
    }

    private String sign(final boolean negative) {
        final String sign;
        if (negative) {
            sign = "-";
        } else if (this.has('+')) {
            sign = "+";
        } else if (this.has(' ')) {
            sign = " ";
        } else {
            sign = "";
        }
        return sign;
    }

    private boolean has(final char flag) {
        return this.flags.indexOf(flag) >= 0;
    }

    private static int skipDigits(final String format, final int from) {
        int pos = from;
        while (pos < format.length() && format.charAt(pos) >= '0' && format.charAt(pos) <= '9') {
            pos += 1;
        }
        return pos;
    }

    private static long number(final String digits) {
        long value = 0L;
        for (int idx = 0; idx < digits.length(); idx += 1) {
            value = Math.min(value * 10L + digits.charAt(idx) - '0', PrintfSpec.LIMIT);
        }
        return value;
    }
}
