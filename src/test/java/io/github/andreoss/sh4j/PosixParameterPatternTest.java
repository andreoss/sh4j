/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: the pattern word of the remove prefix and suffix
 * parameter expansions (POSIX 2.6.2, 2.13).
 *
 * @since 1.0
 */
final class PosixParameterPatternTest {

    @Test
    void matchesEscapedBackslashInQuotedExpansion() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("x='a\\b'; echo \"${x#*\\\\}\"")
        );
    }

    @Test
    void matchesEscapedBackslashInUnquotedExpansion() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("x='a\\b'; echo ${x#*\\\\}")
        );
    }

    @Test
    void expandsParameterInPattern() {
        Assertions.assertEquals(
            String.format("bc%n"),
            Sh4j.INSTANCE.out("p=a; x=abc; echo \"${x#$p}\"")
        );
    }

    @Test
    void matchesDoubleQuotedPatternLiterally() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("x='a*b'; echo \"${x#\"a*\"}\"")
        );
    }

    @Test
    void matchesBackslashEscapedStarLiterally() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("x='a*b'; echo ${x#a\\*}")
        );
    }

    @Test
    void matchesSingleQuotedPatternLiterally() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("x='a*b'; echo ${x#'a*'}")
        );
    }

    @Test
    void keepsUnquotedParameterAsPattern() {
        Assertions.assertEquals(
            String.format("[]%n"),
            Sh4j.INSTANCE.out("p='*'; x=abc; echo \"[${x##$p}]\"")
        );
    }

    @Test
    void matchesQuotedParameterLiterally() {
        Assertions.assertEquals(
            String.format("abc%n"),
            Sh4j.INSTANCE.out("p='*'; x=abc; echo \"${x##\"$p\"}\"")
        );
    }

    @Test
    void removesShortestEmptyPrefixForStar() {
        Assertions.assertEquals(
            String.format("[abc]%n"),
            Sh4j.INSTANCE.out("p='*'; x=abc; echo \"[${x#$p}]\"")
        );
    }

    @Test
    void expandsCommandSubstitutionInPattern() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("x=a.b.c; echo ${x%.$(echo b).c}")
        );
    }

    @Test
    void expandsBackquotedSubstitutionInPattern() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("x=a.b.c; echo \"${x%.`echo b`.c}\"")
        );
    }

    @Test
    void expandsArithmeticInPattern() {
        Assertions.assertEquals(
            String.format("abc%n"),
            Sh4j.INSTANCE.out("x=abc1; echo \"${x%$((0+1))}\"")
        );
    }

    @Test
    void expandsTildeInPattern() {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out("HOME=/h; x=/h/a; echo \"${x#~/}\"")
        );
    }

    @Test
    void removesLiteralPathPrefix() {
        Assertions.assertEquals(
            String.format("/lib%n"),
            Sh4j.INSTANCE.out("x=/usr/lib; echo \"${x#/usr}\"")
        );
    }

    @Test
    void removesLongestPathPrefix() {
        Assertions.assertEquals(
            String.format("lib%n"),
            Sh4j.INSTANCE.out("x=/usr/lib; echo \"${x##*/}\"")
        );
    }

    @Test
    void removesShortestPathSuffix() {
        Assertions.assertEquals(
            String.format("/usr%n"),
            Sh4j.INSTANCE.out("x=/usr/lib; echo \"${x%/*}\"")
        );
    }

    @Test
    void matchesNamedClassInSuffix() {
        Assertions.assertEquals(
            String.format("abc%n"),
            Sh4j.INSTANCE.out("x=abc123; echo \"${x%%[[:digit:]]*}\"")
        );
    }

    @Test
    void matchesNegatedBracketInPrefix() {
        Assertions.assertEquals(
            String.format("bc123%n"),
            Sh4j.INSTANCE.out("x=abc123; echo \"${x#[!b]}\"")
        );
    }

    @Test
    void matchesRangeBracketsInPrefix() {
        Assertions.assertEquals(
            String.format("[]%n"),
            Sh4j.INSTANCE.out("x=abc123; echo \"[${x##[a-b]*[0-9]}]\"")
        );
    }

    @Test
    void matchesQuotedBracketLiterally() {
        Assertions.assertEquals(
            String.format("abc123%n"),
            Sh4j.INSTANCE.out("x=abc123; echo \"${x#\"[a]\"}\"")
        );
    }

    @Test
    void matchesClosingBracketInClass() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("x='a]b'; echo \"${x#a[]]}\"")
        );
    }

    @Test
    void matchesQuoteCharacterFromParameter() {
        Assertions.assertEquals(
            String.format("y%n"),
            Sh4j.INSTANCE.out("x='x\"y'; q='\"'; echo \"${x#*$q}\"")
        );
    }

    @Test
    void matchesEscapedQuestionMarkLiterally() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("x='a?b'; echo \"${x#'a?'}\"")
        );
    }

    @Test
    void keepsLengthOperator() {
        Assertions.assertEquals(
            String.format("3 abc e%n"),
            Sh4j.INSTANCE.out("x=abc; echo ${#x} ${x:-d} ${u:-e}")
        );
    }

    @Test
    void removesLongestSuffixWithStar() {
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out("x=aaa; echo ${x%%a*}x")
        );
    }
}
