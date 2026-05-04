/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: POSIX 2.13 pattern matching notation, 2.6.6
 * pathname expansion and case patterns.
 *
 * @since 1.0
 */
final class PosixPatternMatchingTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void upperClassMatchesUppercaseNames() throws IOException {
        Assertions.assertEquals(
            String.format("A1%n"),
            Sh4j.INSTANCE.out(
                PosixPatternMatchingTest.within("echo [[:upper:]]*", "A1", "b2")
            )
        );
    }

    @Test
    void negatedBracketExcludesCharacter() throws IOException {
        Assertions.assertEquals(
            String.format("b2%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("echo [!A]*", "A1", "b2"))
        );
    }

    @Test
    void caretBracketExcludesCharacter() throws IOException {
        Assertions.assertEquals(
            String.format("b2%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("echo [^A]*", "A1", "b2"))
        );
    }

    @Test
    void rangeBracketSelectsNames() throws IOException {
        Assertions.assertEquals(
            String.format("b2 c3%n"),
            Sh4j.INSTANCE.out(
                PosixPatternMatchingTest.within("echo [b-c]*", "A1", "b2", "c3", "d4")
            )
        );
    }

    @Test
    void closingBracketFirstIsLiteral() throws IOException {
        Assertions.assertEquals(
            String.format("]x%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("echo []]*", "]x", "b2"))
        );
    }

    @Test
    void trailingDashInBracketIsLiteral() throws IOException {
        Assertions.assertEquals(
            String.format("c-%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("echo *[x-]", "c-", "b2"))
        );
    }

    @Test
    void unmatchedClassKeepsWord() throws IOException {
        Assertions.assertEquals(
            String.format("[[:digit:]]*%n"),
            Sh4j.INSTANCE.out(
                PosixPatternMatchingTest.within("echo [[:digit:]]*", "A1", "b2")
            )
        );
    }

    @Test
    void quotedAndEscapedStarsAreLiteral() throws IOException {
        Assertions.assertEquals(
            String.format("x* x*%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("echo \"x*\" x\\*", "x1"))
        );
    }

    @Test
    void partlyQuotedWordGlobsUnquotedStar() throws IOException {
        Assertions.assertEquals(
            String.format("x1 x*%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("echo \"x\"* x\"*\"", "x1"))
        );
    }

    @Test
    void quotedBracketIsLiteral() throws IOException {
        Assertions.assertEquals(
            String.format("[x]1 x1%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("echo '[x]'1 [x]1", "x1"))
        );
    }

    @Test
    void escapedBackslashSurvivesUnmatchedPattern() throws IOException {
        Assertions.assertEquals(
            String.format("q\\*%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("echo q\\\\*", "x1"))
        );
    }

    @Test
    void unquotedVariableGlobsAndQuotedDoesNot() throws IOException {
        Assertions.assertEquals(
            String.format("x1 x*%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("p='x*'; echo $p \"$p\"", "x1"))
        );
    }

    @Test
    void forListUndergoesPathnameExpansion() throws IOException {
        Assertions.assertEquals(
            String.format("x1%nx2%n"),
            Sh4j.INSTANCE.out(
                PosixPatternMatchingTest.within("for f in x*; do echo \"$f\"; done", "x1", "x2")
            )
        );
    }

    @Test
    void externalCommandArgumentsKeepQuotedStar() throws IOException {
        Assertions.assertEquals(
            String.format("x1%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("ls \"x\"*", "x1"))
        );
    }

    @Test
    void directoryPrefixIsUnquoted() throws IOException {
        final File dir = new TempDirs(PosixPatternMatchingTest.root).folder();
        new TempDirs(dir.toPath()).file("x1");
        Assertions.assertEquals(
            String.format("%s/x1%n", dir.getAbsolutePath()),
            Sh4j.INSTANCE.out(String.format("echo \"%s\"/x*", dir.getAbsolutePath()))
        );
    }

    @Test
    void noglobRemovesEscapes() throws IOException {
        Assertions.assertEquals(
            String.format("x* x*%n"),
            Sh4j.INSTANCE.out(PosixPatternMatchingTest.within("set -f; echo x* \"x\"\\*", "x1"))
        );
    }

    @Test
    void escapedStarCasePatternIsLiteral() {
        Assertions.assertEquals(
            String.format("ok%n"),
            Sh4j.INSTANCE.out("case a in \\*) echo bad;; *) echo ok;; esac")
        );
    }

    @Test
    void quotedStarCasePatternMatchesStar() {
        Assertions.assertEquals(
            String.format("star%n"),
            Sh4j.INSTANCE.out("case '*' in \"*\") echo star;; *) echo other;; esac")
        );
    }

    @Test
    void quotedStarCasePatternRejectsOther() {
        Assertions.assertEquals(
            String.format("other%n"),
            Sh4j.INSTANCE.out("case a in \"*\") echo star;; *) echo other;; esac")
        );
    }

    @Test
    void variableCasePatternGlobsOnlyUnquoted() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("p='*'; case a in \"$p\") echo no;; $p) echo yes;; esac")
        );
    }

    @Test
    void partlyQuotedCasePatternGlobs() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("case ab in \"a\"*) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void lowerClassCasePattern() {
        Assertions.assertEquals(
            String.format("low%n"),
            Sh4j.INSTANCE.out("case b in [[:lower:]]) echo low;; *) echo no;; esac")
        );
    }

    @Test
    void classesCombineInOneBracket() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("case 7 in [[:alpha:][:digit:]]) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void hexDigitClassCasePattern() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("case F in [[:xdigit:]]) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void punctClassRejectsLetter() {
        Assertions.assertEquals(
            String.format("no%n"),
            Sh4j.INSTANCE.out("case a in [[:punct:][:space:]]) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void unknownClassMatchesNothing() {
        Assertions.assertEquals(
            String.format("no%n"),
            Sh4j.INSTANCE.out("case b in [[:nope:]]) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void unterminatedClassIsLiteralBracket() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("case '[' in [[:a]) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void reversedRangeMatchesNothing() {
        Assertions.assertEquals(
            String.format("no%n"),
            Sh4j.INSTANCE.out("case b in [z-a]) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void escapedCharacterInsideBracket() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("case '!' in [\\!a]) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void quotedRangeDashIsLiteral() {
        Assertions.assertEquals(
            String.format("no%n"),
            Sh4j.INSTANCE.out("case b in [a\"-\"c]) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void questionMarkMatchesOneCharacter() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("case ab in a?) echo yes;; *) echo no;; esac")
        );
    }

    @Test
    void backslashSubjectMatchesQuotedBackslash() {
        Assertions.assertEquals(
            String.format("bs%n"),
            Sh4j.INSTANCE.out("case 'a\\b' in *'\\'*) echo bs;; *) echo no;; esac")
        );
    }

    @Test
    void suffixRemovalStillMatchesPattern() {
        Assertions.assertEquals(
            String.format("foo.tar gz%n"),
            Sh4j.INSTANCE.out("x=foo.tar.gz; echo \"${x%.*}\" \"${x##*.}\"")
        );
    }

    private static String within(final String script, final String... names)
        throws IOException {
        final File dir = new TempDirs(PosixPatternMatchingTest.root).folder();
        for (final String name : names) {
            new TempDirs(dir.toPath()).file(name);
        }
        return String.format("cd %s && %s", dir.getAbsolutePath(), script);
    }
}
