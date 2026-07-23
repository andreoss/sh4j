/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Digits of a finite, non-negative floating point number in the
 * {@code f}, {@code e} or {@code g} style of {@code printf}, rounded half to
 * even from the exact binary value.
 *
 * @since 1.0
 */
final class PrintfReal {

    /** Style: {@code f}, {@code e} or {@code g}. */
    private final char style;

    /** Precision, negative when none is given. */
    private final int precision;

    /** True when the {@code #} flag keeps the decimal point and zeros. */
    private final boolean alternate;

    /**
     * Ctor.
     *
     * @param style Style: {@code f}, {@code e} or {@code g}
     * @param precision Precision, negative when none is given
     * @param alternate True when the {@code #} flag is given
     */
    PrintfReal(final char style, final int precision, final boolean alternate) {
        this.style = style;
        this.precision = precision;
        this.alternate = alternate;
    }

    /**
     * Renders the digits.
     *
     * @param value Finite, non-negative value
     * @return The digits, without sign
     */
    String render(final double value) {
        final BigDecimal exact = PrintfReal.exact(value);
        int digits = this.precision;
        if (digits < 0) {
            digits = 6;
        }
        final String result;
        if (this.style == 'e') {
            result = this.scientific(exact, digits);
        } else if (this.style == 'g') {
            result = this.general(exact, Math.max(digits, 1));
        } else {
            result = this.fixed(exact, digits);
        }
        return result;
    }

    private String general(final BigDecimal exact, final int digits) {
        final int exponent = PrintfReal.exponent(exact, digits);
        String result;
        if (exponent < digits && exponent >= -4) {
            result = this.fixed(exact, digits - 1 - exponent);
            if (!this.alternate) {
                result = PrintfReal.trim(result);
            }
        } else {
            result = this.scientific(exact, digits - 1);
            if (!this.alternate) {
                final int mark = result.indexOf('e');
                result = PrintfReal.trim(result.substring(0, mark))
                    .concat(result.substring(mark));
            }
        }
        return result;
    }

    private String fixed(final BigDecimal exact, final int digits) {
        String result = exact.setScale(digits, RoundingMode.HALF_EVEN).toPlainString();
        if (this.alternate && digits == 0) {
            result = result.concat(".");
        }
        return result;
    }

    private String scientific(final BigDecimal exact, final int digits) {
        final int exponent = PrintfReal.exponent(exact, digits + 1);
        final String mantissa = this.fixed(exact.movePointLeft(exponent), digits);
        final String sign;
        if (exponent < 0) {
            sign = "e-";
        } else {
            sign = "e+";
        }
        final String power = String.valueOf(Math.abs(exponent));
        final String pad;
        if (power.length() < 2) {
            pad = "0";
        } else {
            pad = "";
        }
        return mantissa.concat(sign).concat(pad).concat(power);
    }

    private static BigDecimal exact(final double value) {
        final int power = Math.getExponent(value) - 52;
        final BigDecimal whole = new BigDecimal(
            BigInteger.valueOf((long) Math.scalb(value, -power))
        );
        final BigDecimal scale = new BigDecimal(BigInteger.TWO.pow(Math.abs(power)));
        final BigDecimal result;
        if (power < 0) {
            result = whole.divide(scale);
        } else {
            result = whole.multiply(scale);
        }
        return result;
    }

    private static int exponent(final BigDecimal exact, final int digits) {
        final BigDecimal rounded = exact.round(new MathContext(digits, RoundingMode.HALF_EVEN));
        return rounded.precision() - rounded.scale() - 1;
    }

    private static String trim(final String digits) {
        String result = digits;
        if (result.indexOf('.') >= 0) {
            int end = result.length();
            while (result.charAt(end - 1) == '0') {
                end -= 1;
            }
            if (result.charAt(end - 1) == '.') {
                end -= 1;
            }
            result = result.substring(0, end);
        }
        return result;
    }
}
