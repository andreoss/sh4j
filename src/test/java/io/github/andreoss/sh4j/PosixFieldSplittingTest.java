/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: POSIX 2.5.2 special parameters and 2.6.5 field
 * splitting.
 *
 * @since 1.0
 */
final class PosixFieldSplittingTest {

    @Test
    void atSignJoinsPrefixAndSuffix() {
        Assertions.assertEquals(
            String.format("[xa]%n[by]%n"),
            Sh4j.INSTANCE.out(
                "set -- a b; for x in \"x$@y\"; do echo \"[$x]\"; done"
            )
        );
    }

    @Test
    void atSignWithPrefixAndNoParameters() {
        Assertions.assertEquals(
            String.format("[x]%n"),
            Sh4j.INSTANCE.out(
                "set --; for x in \"x$@\"; do echo \"[$x]\"; done"
            )
        );
    }

    @Test
    void atSignAloneWithNoParameters() {
        Assertions.assertEquals(
            String.format("e%n"),
            Sh4j.INSTANCE.out(
                "set --; for x in \"$@\"; do echo \"[$x]\"; done; echo e"
            )
        );
    }

    @Test
    void atSignInCommandArguments() {
        Assertions.assertEquals(
            String.format("[aa b][cb]"),
            Sh4j.INSTANCE.out(
                "set -- \"a b\" c; printf '[%s]' \"a$@b\""
            )
        );
    }

    @Test
    void atSignBetweenUnquotedText() {
        Assertions.assertEquals(
            String.format("[preaa b][cbpost]"),
            Sh4j.INSTANCE.out(
                "set -- \"a b\" c; printf '[%s]' pre\"a$@b\"post"
            )
        );
    }

    @Test
    void bracedAtSignWithSuffix() {
        Assertions.assertEquals(
            String.format("[a b][cz]"),
            Sh4j.INSTANCE.out(
                "set -- \"a b\" c; printf '[%s]' \"${@}z\""
            )
        );
    }

    @Test
    void twoAtSignsInOneWord() {
        Assertions.assertEquals(
            String.format("[a b][ca b][c]"),
            Sh4j.INSTANCE.out(
                "set -- \"a b\" c; printf '[%s]' \"$@$@\""
            )
        );
    }

    @Test
    void atSignKeepsSpecialCharacters() {
        Assertions.assertEquals(
            String.format("[<q\"\\$x`>]"),
            Sh4j.INSTANCE.out(
                "set -- 'q\"\\$x`'; printf '[%s]' \"<$@>\""
            )
        );
    }

    @Test
    void atSignEmptyInsideLargerWord() {
        Assertions.assertEquals(
            String.format("[ab]"),
            Sh4j.INSTANCE.out(
                "set --; printf '[%s]' \"a$@b\" \"${@}\""
            )
        );
    }

    @Test
    void atSignWithOneParameter() {
        Assertions.assertEquals(
            String.format("[<one>]"),
            Sh4j.INSTANCE.out(
                "set -- one; printf '[%s]' \"<$@>\""
            )
        );
    }

    @Test
    void atSignInsideSubstitutionStaysJoined() {
        Assertions.assertEquals(
            String.format("[a b]"),
            Sh4j.INSTANCE.out(
                "set -- a b; printf '[%s]' \"$(echo \"$@\")\""
            )
        );
    }

    @Test
    void atSignSingleQuotedOrEscaped() {
        Assertions.assertEquals(
            String.format("[$@][$@][a b]"),
            Sh4j.INSTANCE.out(
                "set -- a b; printf '[%s]' '$@' \"\\$@\" \"`echo $@`\""
            )
        );
    }

    @Test
    void atSignInCaseWord() {
        Assertions.assertEquals(
            String.format("m%n"),
            Sh4j.INSTANCE.out(
                "set -- a b; case \"x$@\" in \"xa b\") echo m;; esac"
            )
        );
    }

    @Test
    void atSignInAssignment() {
        Assertions.assertEquals(
            String.format("[pa b cq]%n"),
            Sh4j.INSTANCE.out(
                "set -- \"a b\" c; x=\"p$@q\"; echo \"[$x]\""
            )
        );
    }

    @Test
    void splitWhiteSpaceJoinsDelimiter() {
        Assertions.assertEquals(
            String.format("[a]%n[b]%n[c]%n"),
            Sh4j.INSTANCE.out(
                "IFS=\" :\"; v=\" a : b  c:\"; for f in $v; do echo \"[$f]\"; done"
            )
        );
    }

    @Test
    void splitAdjacentDelimitersGiveEmptyField() {
        Assertions.assertEquals(
            String.format("[a]%n[]%n[b]%n"),
            Sh4j.INSTANCE.out(
                "IFS=:; v=\"a::b\"; for f in $v; do echo \"[$f]\"; done"
            )
        );
    }

    @Test
    void splitSpacedDelimitersGiveEmptyField() {
        Assertions.assertEquals(
            String.format("[a][][b]"),
            Sh4j.INSTANCE.out(
                "IFS=\" :\"; v=\"a :: b\"; printf '[%s]' $v"
            )
        );
    }

    @Test
    void splitLeadingDelimiterGivesEmptyField() {
        Assertions.assertEquals(
            String.format("[][a]"),
            Sh4j.INSTANCE.out(
                "IFS=\" :\"; v=\" : a\"; printf '[%s]' $v"
            )
        );
    }

    @Test
    void splitTrailingSpacedDelimiter() {
        Assertions.assertEquals(
            String.format("[a]"),
            Sh4j.INSTANCE.out(
                "IFS=\" :\"; v=\"a : \"; printf '[%s]' $v"
            )
        );
    }

    @Test
    void splitLoneDelimiter() {
        Assertions.assertEquals(
            String.format("[]"),
            Sh4j.INSTANCE.out(
                "IFS=\" :\"; v=\":\"; printf '[%s]' $v"
            )
        );
    }

    @Test
    void splitTwoTrailingDelimiters() {
        Assertions.assertEquals(
            String.format("[a][]"),
            Sh4j.INSTANCE.out(
                "IFS=\" :\"; v=\"a::\"; printf '[%s]' $v"
            )
        );
    }

    @Test
    void splitKeepsQuotedPartOfWord() {
        Assertions.assertEquals(
            String.format("[ab cx][y]"),
            Sh4j.INSTANCE.out(
                "v=\"x y\"; printf '[%s]' a\"b c\"$v"
            )
        );
    }

    @Test
    void splitKeepsEscapedDelimiter() {
        Assertions.assertEquals(
            String.format("[a:p][q]"),
            Sh4j.INSTANCE.out(
                "IFS=:; v=\"p:q\"; printf '[%s]' a\\:$v"
            )
        );
    }

    @Test
    void assignDefaultStillWorks() {
        Assertions.assertEquals(
            String.format("z%n"),
            Sh4j.INSTANCE.out(
                ": ${u:=z}; echo \"$u\""
            )
        );
    }
}
