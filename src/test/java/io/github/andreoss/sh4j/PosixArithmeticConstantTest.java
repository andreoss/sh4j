/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: arithmetic constants follow ISO C, so a leading
 * {@code 0} means octal and {@code 0x} or {@code 0X} means hexadecimal
 * (POSIX 2.6.4).
 *
 * @since 1.0
 */
final class PosixArithmeticConstantTest {

    @Test
    void octalEightStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((08))\"; echo after")
        );
    }

    @Test
    void octalEightFailsScript() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("echo \"$((08))\"; echo after")
        );
    }

    @Test
    void octalEightIsReported() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo \"$((08))\"").stderr().contains("08")
        );
    }

    @Test
    void octalNineStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((09))\"; echo after")
        );
    }

    @Test
    void octalNineteenStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((019))\"; echo after")
        );
    }

    @Test
    void octalNineteenFailsScript() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("echo \"$((019))\"; echo after")
        );
    }

    @Test
    void octalInSumStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((09 + 1))\"; echo after")
        );
    }

    @Test
    void bareHexPrefixStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((0x))\"; echo after")
        );
    }

    @Test
    void bareHexPrefixFailsScript() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("echo \"$((0x))\"; echo after")
        );
    }

    @Test
    void bareUpperHexPrefixStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((0X))\"; echo after")
        );
    }

    @Test
    void hexPrefixBeforeSpaceStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((0x 1))\"; echo after")
        );
    }

    @Test
    void invalidHexDigitStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("echo \"$((0xg))\"; echo after")
        );
    }

    @Test
    void invalidHexDigitFailsScript() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("echo \"$((0xg))\"; echo after")
        );
    }

    @Test
    void validConstantsExpand() {
        Assertions.assertEquals(
            String.format("8 31 0 0%n"),
            Sh4j.INSTANCE.out("echo \"$((010))\" \"$((0x1F))\" \"$((0))\" \"$((00))\"")
        );
    }

    @Test
    void wideOctalConstantExpands() {
        Assertions.assertEquals(
            String.format("511 7%n"),
            Sh4j.INSTANCE.out("echo \"$((0777))\" \"$((007))\"")
        );
    }

    @Test
    void largestHexConstantExpands() {
        Assertions.assertEquals(
            String.format("2147483647%n"),
            Sh4j.INSTANCE.out("echo \"$((0x7fffffff))\"")
        );
    }

    @Test
    void negatedOctalConstantExpands() {
        Assertions.assertEquals(
            String.format("-8%n"),
            Sh4j.INSTANCE.out("echo \"$((-010))\"")
        );
    }

    @Test
    void octalVariableIsOctal() {
        Assertions.assertEquals(
            String.format("8%n"),
            Sh4j.INSTANCE.out("x=010; echo \"$((x))\"")
        );
    }

    @Test
    void negativeOctalVariableIsOctal() {
        Assertions.assertEquals(
            String.format("-8%n"),
            Sh4j.INSTANCE.out("x=-010; echo \"$((x))\"")
        );
    }

    @Test
    void hexVariableIsHex() {
        Assertions.assertEquals(
            String.format("31%n"),
            Sh4j.INSTANCE.out("x=0x1F; echo \"$((x))\"")
        );
    }

    @Test
    void invalidOctalVariableStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("x=08; echo \"$((x))\"; echo after")
        );
    }

    @Test
    void invalidOctalVariableFailsScript() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("x=08; echo \"$((x + 1))\"; echo after")
        );
    }

    @Test
    void bareHexPrefixVariableStopsScript() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("x=0x; echo \"$((x))\"; echo after")
        );
    }
}
