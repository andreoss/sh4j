/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: a double-quoted {@code $@} inside a parameter
 * expansion expands to one field per positional parameter (POSIX 2.5.2).
 *
 * @since 1.0
 */
final class PosixAtSignWordTest {

    @Test
    void spreadsQuotedDefaultOfAtSign() {
        Assertions.assertEquals(
            String.format("2:[a][b c]%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; c \"${@:-x}\"")
        );
    }

    @Test
    void usesDefaultWordWithoutParameters() {
        Assertions.assertEquals(
            String.format("1:[x]%n"),
            PosixAtSignWordTest.out("set --; c \"${@:-x}\"")
        );
    }

    @Test
    void spreadsAtSignInAlternativeOfAtSign() {
        Assertions.assertEquals(
            String.format("2:[pa][bq]%n"),
            PosixAtSignWordTest.out("set -- a b; c \"${@+p$@q}\"")
        );
    }

    @Test
    void spreadsQuotedAtSignInQuotedAlternative() {
        Assertions.assertEquals(
            String.format("2:[a][b]%n"),
            PosixAtSignWordTest.out("set -- a b; c \"${1+\"$@\"}\"")
        );
    }

    @Test
    void keepsParameterOfUnquotedAlternative() {
        Assertions.assertEquals(
            String.format("1:[x y]%n"),
            PosixAtSignWordTest.out("set -- 'x y'; c ${1+\"$@\"}")
        );
    }

    @Test
    void spreadsQuotedAtSignInUnquotedDefault() {
        Assertions.assertEquals(
            String.format("2:[a][b c]%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; c ${u:-\"$@\"}")
        );
    }

    @Test
    void spreadsQuotedAtSignWithEmptyIfs() {
        Assertions.assertEquals(
            String.format("2:[a][b c]%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; IFS=''; c ${u:-\"$@\"}")
        );
    }

    @Test
    void joinsTextAroundAtSignInDefault() {
        Assertions.assertEquals(
            String.format("3:[prea][b cpost][end]%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; c \"${u:-pre\"$@\"post}\" end")
        );
    }

    @Test
    void spreadsAtSignInsideSingleQuotesOfWord() {
        Assertions.assertEquals(
            String.format("2:['a][b c']%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; c \"${u:-'$@'}\"")
        );
    }

    @Test
    void keepsQuotesOfParametersInAlternative() {
        Assertions.assertEquals(
            String.format("2:[it's][q\"\\]%n"),
            PosixAtSignWordTest.out("set -- \"it's\" 'q\"\\'; c \"${1+\"$@\"}\"")
        );
    }

    @Test
    void keepsQuotesOfParametersInDefault() {
        Assertions.assertEquals(
            String.format("2:[it's][q\"\\]%n"),
            PosixAtSignWordTest.out("set -- \"it's\" 'q\"\\'; c \"${@:-x}\"")
        );
    }

    @Test
    void keepsEmptyParametersOfDefault() {
        Assertions.assertEquals(
            String.format("2:[][]%n"),
            PosixAtSignWordTest.out("set -- '' ''; c \"${@:-x}\"")
        );
    }

    @Test
    void joinsAtSignDefaultInAssignment() {
        Assertions.assertEquals(
            String.format("1:[a b c]%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; v=\"${@:-z}\"; c \"$v\"")
        );
    }

    @Test
    void treatsAtSignAsSetWithoutParameters() {
        Assertions.assertEquals(
            String.format("1:[]%n"),
            PosixAtSignWordTest.out("set --; c \"${@-x}\"")
        );
    }

    @Test
    void spreadsNestedDefaultOfAtSign() {
        Assertions.assertEquals(
            String.format("2:[a][b c]%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; c \"${1+\"${@:-z}\"}\"")
        );
    }

    @Test
    void joinsTextAroundQuotedDefaultOfAtSign() {
        Assertions.assertEquals(
            String.format("2:[<a][b c>]%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; c \"<${@:-x}>\"")
        );
    }

    @Test
    void removesPrefixFromEachParameter() {
        Assertions.assertEquals(
            String.format("2:[][b c]%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; c \"${@#a}\"")
        );
    }

    @Test
    void keepsEscapedAtSignInDefault() {
        Assertions.assertEquals(
            String.format("1:[$@]%n"),
            PosixAtSignWordTest.out("set -- a 'b c'; c \"${u:-\\$@}\"")
        );
    }

    @Test
    void keepsParameterEndingWithBackslash() {
        Assertions.assertEquals(
            String.format("2:[a\\][b]%n"),
            PosixAtSignWordTest.out("set -- 'a\\' b; c \"${@:-x}\"")
        );
    }

    private static String out(final String script) {
        return Sh4j.INSTANCE.out(
            "c() { printf '%s:' \"$#\"; printf '[%s]' \"$@\"; echo; }; ".concat(script)
        );
    }
}
