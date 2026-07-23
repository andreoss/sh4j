/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arguments of the {@code printf} built-in, taken in order by the
 * conversions; a missing one reads as empty or zero, and a number that does
 * not convert is reported and marks the run as failed.
 *
 * @since 1.0
 */
final class PrintfArguments {

    /** Integer prefix: sign, then hexadecimal, octal or decimal digits. */
    private static final Pattern INTEGER = Pattern.compile(
        "\\s*([+-]?)(0[xX][0-9a-fA-F]+|0[0-7]*|[1-9][0-9]*)"
    );

    /** Decimal floating point prefix. */
    private static final Pattern REAL = Pattern.compile(
        "\\s*[+-]?([0-9]+\\.?[0-9]*|\\.[0-9]+)([eE][+-]?[0-9]+)?"
    );

    /** Hexadecimal prefix, read as an integer. */
    private static final Pattern HEX = Pattern.compile("\\s*[+-]?0[xX].*");

    /** Infinity or not-a-number. */
    private static final Pattern SPECIAL = Pattern.compile(
        "\\s*([+-]?)(inf|infinity|nan)", Pattern.CASE_INSENSITIVE
    );

    /** Smallest integer a conversion accepts. */
    private static final BigInteger LOW = BigInteger.valueOf(Long.MIN_VALUE);

    /** Largest integer a conversion accepts. */
    private static final BigInteger HIGH = BigInteger.valueOf(Long.MAX_VALUE);

    /** The arguments. */
    private final List<String> values;

    /** Where conversion errors are reported. */
    private final PrintSink errors;

    /** Index of the next argument. */
    private int position;

    /** True once an error has been reported. */
    private boolean failed;

    /**
     * Ctor.
     *
     * @param values The arguments
     * @param errors Where conversion errors are reported
     */
    PrintfArguments(final List<String> values, final PrintSink errors) {
        this.values = values;
        this.errors = errors;
    }

    /**
     * Tells whether arguments are left.
     *
     * @return True when an argument is left
     */
    boolean remain() {
        return this.position < this.values.size();
    }

    /**
     * Counts the arguments taken so far.
     *
     * @return Index of the next argument
     */
    int position() {
        return this.position;
    }

    /**
     * Tells whether an error has been reported.
     *
     * @return True after an error
     */
    boolean failed() {
        return this.failed;
    }

    /**
     * Takes the next argument as text.
     *
     * @return The argument, empty when none is left
     */
    String text() {
        final String value;
        if (this.remain()) {
            value = this.values.get(this.position);
            this.position += 1;
        } else {
            value = "";
        }
        return value;
    }

    /**
     * Takes the next argument as an integer.
     *
     * @return The value, zero when none is left
     */
    long integer() {
        return this.integer(this.text());
    }

    /**
     * Takes the next argument as a floating point number.
     *
     * @return The value, zero when none is left
     */
    double real() {
        final String value = this.text();
        final Matcher special = PrintfArguments.SPECIAL.matcher(value);
        final Matcher real = PrintfArguments.REAL.matcher(value);
        final double result;
        if (special.matches()) {
            result = PrintfArguments.special(special);
        } else if (!PrintfArguments.HEX.matcher(value).matches() && real.lookingAt()) {
            result = Double.parseDouble(real.group().strip());
            this.rest(value, real.end());
        } else {
            result = this.integer(value);
        }
        return result;
    }

    /**
     * Reports an error and marks the run as failed.
     *
     * @param subject What the error is about
     * @param reason What went wrong
     */
    void fail(final String subject, final String reason) {
        this.errors.println(String.format("sh: printf: %s: %s", subject, reason));
        this.failed = true;
    }

    private long integer(final String value) {
        final long result;
        if (value.isEmpty()) {
            result = 0L;
        } else if (value.charAt(0) == '\'' || value.charAt(0) == '"') {
            result = value.codePoints().skip(1L).findFirst().orElse(0);
        } else {
            result = this.parse(value);
        }
        return result;
    }

    private long parse(final String value) {
        final Matcher matcher = PrintfArguments.INTEGER.matcher(value);
        long result = 0L;
        if (matcher.lookingAt()) {
            BigInteger number = PrintfArguments.digits(matcher.group(2));
            if ("-".equals(matcher.group(1))) {
                number = number.negate();
            }
            if (number.compareTo(PrintfArguments.HIGH) > 0) {
                this.fail(value, "result too large");
                number = PrintfArguments.HIGH;
            } else if (number.compareTo(PrintfArguments.LOW) < 0) {
                this.fail(value, "result too large");
                number = PrintfArguments.LOW;
            }
            result = number.longValue();
            this.rest(value, matcher.end());
        } else {
            this.fail(value, "invalid number");
        }
        return result;
    }

    private void rest(final String value, final int end) {
        if (end < value.length()) {
            this.fail(value, "not completely converted");
        }
    }

    private static BigInteger digits(final String digits) {
        final BigInteger number;
        if (digits.length() > 1 && Character.toLowerCase(digits.charAt(1)) == 'x') {
            number = new BigInteger(digits.substring(2), 16);
        } else if (digits.charAt(0) == '0') {
            number = new BigInteger(digits, 8);
        } else {
            number = new BigInteger(digits);
        }
        return number;
    }

    private static double special(final Matcher matcher) {
        double result = Double.NaN;
        if (!"nan".equals(matcher.group(2).toLowerCase(Locale.ROOT))) {
            result = Double.POSITIVE_INFINITY;
        }
        if ("-".equals(matcher.group(1))) {
            result = -result;
        }
        return result;
    }
}
