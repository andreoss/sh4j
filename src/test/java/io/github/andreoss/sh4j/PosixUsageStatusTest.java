/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: exit status of getopts and hash usage errors.
 *
 * @since 1.0
 */
final class PosixUsageStatusTest {

    @Test
    void getoptsWithoutOperandsReturnsTwo() {
        Assertions.assertEquals(
            String.format("2%n"),
            Sh4j.INSTANCE.out("getopts 2>/dev/null; echo \"$?\"")
        );
    }

    @Test
    void getoptsWithoutNameReturnsTwo() {
        Assertions.assertEquals(
            String.format("2%n"),
            Sh4j.INSTANCE.out("getopts ab 2>/dev/null; echo \"$?\"")
        );
    }

    @Test
    void getoptsWithoutNamePrintsUsage() {
        Assertions.assertEquals(
            String.format("sh: getopts: Usage: getopts optstring var [arg...]%n"),
            Sh4j.INSTANCE.out("getopts ab 2>&1")
        );
    }

    @Test
    void getoptsWithNameStillParses() {
        Assertions.assertEquals(
            String.format("0 a%n"),
            Sh4j.INSTANCE.out("set -- -a; getopts ab opt; echo \"$? $opt\"")
        );
    }

    @Test
    void hashMissingCommandReturnsOne() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("hash nosuchcmd_q 2>/dev/null; echo \"$?\"")
        );
    }

    @Test
    void hashMissingCommandPrintsMessage() {
        Assertions.assertEquals(
            String.format("sh: hash: nosuchcmd_q: not found%n"),
            Sh4j.INSTANCE.out("hash nosuchcmd_q 2>&1")
        );
    }

    @Test
    void hashMissingAmongFoundReturnsOne() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("hash nosuchcmd_q sh 2>/dev/null; echo \"$?\"")
        );
    }

    @Test
    void hashFoundCommandReturnsZero() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("hash sh"));
    }

    @Test
    void hashResetReturnsZero() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("hash -r"));
    }

    @Test
    void hashListReturnsZero() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("hash"));
    }
}
