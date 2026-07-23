/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link Sh4j}: the {@code printf} built-in writes its format with
 * escapes decoded and conversions filled from the arguments (POSIX printf).
 *
 * @since 1.0
 */
final class PosixPrintfTest {

    @Test
    void isBuiltin() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.out("type printf").contains("built-in")
        );
    }

    @Test
    void runsWithoutPath() {
        Assertions.assertEquals(
            "ok",
            Sh4j.INSTANCE.out("PATH=/nonexistent; printf '%s' ok")
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '`',
        value = {
            "printf 'a\\nb'|a^Jb",
            "printf 'a\\tb'|a^Ib",
            "printf 'a\\\\b'|a\\b",
            "printf '\\a\\b\\f\\r\\v'|^G^H^L^M^K",
            "printf '\\101\\102'|AB",
            "printf '\\0101'|^H1",
            "printf '\\303\\251'|é",
            "printf 'a\\'|a\\",
            "printf 'a\\qb'|a\\qb",
            "printf '\\u263a'|\\u263a",
            "printf '😀.'|😀.",
            "printf '%%'|%",
            "printf '%%%s' a|%a"
        }
    )
    void decodesFormat(final String script, final String expected) {
        Assertions.assertEquals(
            expected, PosixPrintfTest.visible(Sh4j.INSTANCE.out(script))
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '`',
        value = {
            "printf '[%s]' x|[x]",
            "printf '[%5s]' ab|[   ab]",
            "printf '[%-5s]' ab|[ab   ]",
            "printf '[%.2s]' abcd|[ab]",
            "printf '[%.9s]' abcd|[abcd]",
            "printf '[%.s]' abcd|[]",
            "printf '[%3s]' é|[ é]",
            "printf '[%s]'|[]",
            "printf '%s,' a b c|a,b,c,",
            "printf '%s=%s;' a 1 b|a=1;b=;",
            "printf 'x\\n' a b|x^J",
            "printf '[%c]' hello|[h]",
            "printf '[%c]' ''|[^@]",
            "printf '[%3c]' a|[  a]",
            "printf '[%-3c]' é|[é ]",
            "printf '[%*s]' 4 x|[   x]",
            "printf '[%*s]' -3 x|[x  ]",
            "printf '[%.*s]' 2 abcd|[ab]",
            "printf '[%.*s]' -1 abcd|[abcd]",
            "printf -- '[%s]' x|[x]"
        }
    )
    void formatsStrings(final String script, final String expected) {
        Assertions.assertEquals(
            expected, PosixPrintfTest.visible(Sh4j.INSTANCE.out(script))
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '`',
        value = {
            "printf '[%b]' 'a\\nb'|[a^Jb]",
            "printf '[%b]' 'x\\0101'|[xA]",
            "printf '[%b]' '\\101'|[A]",
            "printf '[%b]' '\\0'|[^@]",
            "printf '[%b]' 'a\\\\b'|[a\\b]",
            "printf '[%b]' 'a\\'|[a\\]",
            "printf '[%b]' 'a\\qb'|[a\\qb]",
            "printf '[%.2b]' abcd|[ab]",
            "printf '[%5b]' x|[    x]",
            "printf '[%b]' 'a\\cb' c|[a",
            "printf '%b-' a 'b\\c' c|a-b",
            "printf '%b-\\n' a|a-^J"
        }
    )
    void expandsEscapesInArguments(final String script, final String expected) {
        Assertions.assertEquals(
            expected, PosixPrintfTest.visible(Sh4j.INSTANCE.out(script))
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '`',
        value = {
            "printf '[%d]' 42|[42]",
            "printf '[%i]' -42|[-42]",
            "printf '[%d]'|[0]",
            "printf '[%d]' ''|[0]",
            "printf '[%5d]' 42|[   42]",
            "printf '[%-5d]' 42|[42   ]",
            "printf '[%05d]' -42|[-0042]",
            "printf '[%-05d]' 42|[42   ]",
            "printf '[%05.3d]' 7|[  007]",
            "printf '[%+d]' 5|[+5]",
            "printf '[% d]' 5|[ 5]",
            "printf '[%+d]' -5|[-5]",
            "printf '[%.3d]' 7|[007]",
            "printf '[%.0d]' 0|[]",
            "printf '[%d]' 0x1f|[31]",
            "printf '[%d]' 0X1F|[31]",
            "printf '[%d]' 017|[15]",
            "printf '[%d]' ' +12'|[12]",
            "printf '[%d]' \"'A\"|[65]",
            "printf '[%d]' '\"B'|[66]",
            "printf '[%d]' \"'\"|[0]",
            "printf '[%d]' -9223372036854775808|[-9223372036854775808]",
            "printf '[%o]' 8|[10]",
            "printf '[%#o]' 8|[010]",
            "printf '[%#o]' 0|[0]",
            "printf '[%#.0o]' 0|[0]",
            "printf '[%#.3o]' 8|[010]",
            "printf '[%u]' -1|[18446744073709551615]",
            "printf '[%+u]' 3|[3]",
            "printf '[%x]' 255|[ff]",
            "printf '[%X]' 255|[FF]",
            "printf '[%#x]' 255|[0xff]",
            "printf '[%#X]' 255|[0XFF]",
            "printf '[%#x]' 0|[0]",
            "printf '[%#06x]' 255|[0x00ff]"
        }
    )
    void formatsIntegers(final String script, final String expected) {
        Assertions.assertEquals(
            expected, PosixPrintfTest.visible(Sh4j.INSTANCE.out(script))
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '`',
        value = {
            "printf '[%f]' 3.14159|[3.141590]",
            "printf '[%f]'|[0.000000]",
            "printf '[%.2f]' 2.675|[2.67]",
            "printf '[%.0f]' 2.5|[2]",
            "printf '[%.0f]' 3.5|[4]",
            "printf '[%.f]' 2.5|[2]",
            "printf '[%#.0f]' 3|[3.]",
            "printf '[%8.3f]' 3.14159|[   3.142]",
            "printf '[%-8.3f]' 3.14159|[3.142   ]",
            "printf '[%08.3f]' -3.5|[-003.500]",
            "printf '[%+f]' 1|[+1.000000]",
            "printf '[% f]' 1|[ 1.000000]",
            "printf '[%f]' -0|[-0.000000]",
            "printf '[%f]' .5|[0.500000]",
            "printf '[%f]' 1e3|[1000.000000]",
            "printf '[%f]' 0x10|[16.000000]",
            "printf '[%F]' 1.5|[1.500000]",
            "printf '[%f]' inf|[inf]",
            "printf '[%F]' Infinity|[INF]",
            "printf '[%f]' -inf|[-inf]",
            "printf '[%+f]' inf|[+inf]",
            "printf '[%f]' nan|[nan]",
            "printf '[%05f]' inf|[  inf]",
            "printf '[%e]' 12345.678|[1.234568e+04]",
            "printf '[%E]' 12345.678|[1.234568E+04]",
            "printf '[%e]' 0|[0.000000e+00]",
            "printf '[%e]' 0.000123|[1.230000e-04]",
            "printf '[%e]' 1e100|[1.000000e+100]",
            "printf '[%e]' 9.9999999|[1.000000e+01]",
            "printf '[%.0e]' 5|[5e+00]",
            "printf '[%#.0e]' 5|[5.e+00]",
            "printf '[%.2e]' -1234|[-1.23e+03]",
            "printf '[%g]' 100000|[100000]",
            "printf '[%g]' 1000000|[1e+06]",
            "printf '[%g]' 0.0001|[0.0001]",
            "printf '[%g]' 0.00001|[1e-05]",
            "printf '[%g]' 3.14159|[3.14159]",
            "printf '[%g]' 0|[0]",
            "printf '[%g]' 2.5|[2.5]",
            "printf '[%g]' 123456789|[1.23457e+08]",
            "printf '[%G]' 1e-10|[1E-10]",
            "printf '[%G]' 1.5e-10|[1.5E-10]",
            "printf '[%#g]' 1|[1.00000]",
            "printf '[%#g]' 1e-10|[1.00000e-10]",
            "printf '[%.0g]' 15|[2e+01]",
            "printf '[%.3g]' 1234|[1.23e+03]",
            "printf '[%10.4g]' 3.14159|[     3.142]"
        }
    )
    void formatsReals(final String script, final String expected) {
        Assertions.assertEquals(
            expected, PosixPrintfTest.visible(Sh4j.INSTANCE.out(script))
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '`',
        value = {
            "printf '[%d]' abc|[0]|abc",
            "printf '[%d]' 12abc|[12]|12abc",
            "printf '[%d]' 08|[0]|08",
            "printf '[%d]' 99999999999999999999|[9223372036854775807]|99999999999999999999",
            "printf '[%d]' -99999999999999999999|[-9223372036854775808]|-99999999999999999999",
            "printf '[%f]' abc|[0.000000]|abc",
            "printf '[%f]' 1.5x|[1.500000]|1.5x",
            "printf '[%d][%d]' x 7|[0][7]|x",
            "printf 'a%zb'|a|%z",
            "printf 'a%'|a|%"
        }
    )
    void reportsBadOperands(
        final String script, final String expected, final String culprit
    ) {
        final Sh4j.Result result = Sh4j.INSTANCE.runFull(script);
        Assertions.assertAll(
            () -> Assertions.assertEquals(1, result.exitcode()),
            () -> Assertions.assertEquals(expected, result.stdout()),
            () -> Assertions.assertTrue(
                result.stderr().contains(String.format("printf: %s:", culprit)),
                result.stderr()
            )
        );
    }

    @Test
    void stopsAfterBadDirective() {
        Assertions.assertEquals(
            "[a]",
            Sh4j.INSTANCE.out("printf '[%s]%y' a b")
        );
    }

    @Test
    void failsWithoutFormat() {
        final Sh4j.Result result = Sh4j.INSTANCE.runFull("printf");
        Assertions.assertAll(
            () -> Assertions.assertEquals(2, result.exitcode()),
            () -> Assertions.assertTrue(result.stderr().contains("usage"))
        );
    }

    @Test
    void failsWithOnlyEndOfOptions() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("printf --"));
    }

    @Test
    void succeedsOnCleanRun() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("printf '%s %d\\n' a 1 >/dev/null"));
    }

    @Test
    void writesNothingToClosedOutputWithoutFailing() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("printf '' >&-"));
    }

    @Test
    void feedsPipeline() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("printf 'a\\nb\\n' | { read x; read y; echo \"$y\"; }")
        );
    }

    private static String visible(final String text) {
        final StringBuilder shown = new StringBuilder(text.length());
        for (int idx = 0; idx < text.length(); idx += 1) {
            final char chr = text.charAt(idx);
            if (chr < ' ') {
                shown.append('^').append((char) (chr + '@'));
            } else {
                shown.append(chr);
            }
        }
        return shown.toString();
    }
}
