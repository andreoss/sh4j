/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: an unquoted {@code $@} with IFS set to the empty
 * string expands to one field per positional parameter (POSIX 2.5.2).
 *
 * @since 1.0
 */
final class PosixAtSignNullIfsTest {

    @Test
    void loopsOverEachParameter() {
        Assertions.assertEquals(
            String.format("[a b]%n[c]%n"),
            Sh4j.INSTANCE.out(
                "set -- 'a b' c; IFS=; for i in $@; do echo \"[$i]\"; done"
            )
        );
    }

    @Test
    void passesEachParameterAsArgument() {
        Assertions.assertEquals(
            String.format("2:[a b][c]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; IFS=; c $@")
        );
    }

    @Test
    void joinsPrefixAndSuffixToOuterParameters() {
        Assertions.assertEquals(
            String.format("2:[xa b][cy]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; IFS=; c x$@y")
        );
    }

    @Test
    void spreadsBracedAtSign() {
        Assertions.assertEquals(
            String.format("2:[a b][c]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; IFS=; c ${@}")
        );
    }

    @Test
    void spreadsBracedAtSignWithDefault() {
        Assertions.assertEquals(
            String.format("2:[a b][c]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; IFS=; c ${@:-z}")
        );
    }

    @Test
    void spreadsAtSignInDefaultWord() {
        Assertions.assertEquals(
            String.format("2:[a b][c]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; IFS=; c ${x:-$@}")
        );
    }

    @Test
    void spreadsQuotedAtSignInDefaultWord() {
        Assertions.assertEquals(
            String.format("2:[a b][c]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; IFS=; c ${x:-\"$@\"}")
        );
    }

    @Test
    void keepsQuotedAtSign() {
        Assertions.assertEquals(
            String.format("2:[a b][c]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; IFS=; c \"$@\"")
        );
    }

    @Test
    void keepsQuotedStarJoined() {
        Assertions.assertEquals(
            String.format("1:[a bc]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; IFS=; c \"$*\"")
        );
    }

    @Test
    void splitsParametersWithUnsetIfs() {
        Assertions.assertEquals(
            String.format("3:[a][b][c]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; unset IFS; c $@")
        );
    }

    @Test
    void splitsParametersWithDefaultIfs() {
        Assertions.assertEquals(
            String.format("4:[xa][b][cy][z]%n"),
            PosixAtSignNullIfsTest.out("set -- 'a b' c; c x$@y z")
        );
    }

    @Test
    void splitsParametersWithOtherIfs() {
        Assertions.assertEquals(
            String.format("3:[a][b][c]%n"),
            PosixAtSignNullIfsTest.out("set -- a:b c; IFS=:; c $@")
        );
    }

    private static String out(final String script) {
        return Sh4j.INSTANCE.out(
            "c() { printf '%s:' \"$#\"; printf '[%s]' \"$@\"; echo; }; ".concat(script)
        );
    }
}
