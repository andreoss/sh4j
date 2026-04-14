/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.Sh4j;
import io.github.andreoss.sh4j.TempDirs;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link WordExpander}: normalise backtick.
 *
 * @since 1.0
 */
final class WordExpanderNormaliseBacktickTest {

    /** ABC constant. */
    private static final String ABC = "abc";

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void normaliseBacktickNestedAndUnclosedThird() {
        WordExpander.normaliseBackticks("`echo ${x}`");
        WordExpander.normaliseBackticks("`unclosed");
        Assertions.assertEquals("${x}", WordExpander.normaliseBackticks("${x}"));
    }

    @Test
    void normaliseDollarAndUnclosedFormsFirst() {
        Assertions.assertEquals("$x", WordExpander.normaliseBackticks("$x"));
    }

    @Test
    void normaliseDollarAndUnclosedFormsSecond() {
        WordExpander.normaliseBackticks("$x");
        Assertions.assertEquals("$", WordExpander.normaliseBackticks("$"));
    }

    @Test
    void normaliseDollarAndUnclosedFormsThird() {
        WordExpander.normaliseBackticks("$x");
        WordExpander.normaliseBackticks("$");
        Assertions.assertEquals("$(x", WordExpander.normaliseBackticks("$(x"));
    }

    @Test
    void normaliseDollarAndUnclosedFormsFourth() {
        WordExpander.normaliseBackticks("$x");
        WordExpander.normaliseBackticks("$");
        WordExpander.normaliseBackticks("$(x");
        Assertions.assertEquals("${x", WordExpander.normaliseBackticks("${x"));
    }

    @Test
    void pathnameExpandNoGlob() {
        Assertions.assertEquals(
            Collections.singletonList(WordExpanderNormaliseBacktickTest.ABC),
            WordExpander.pathnameExpand(
                Collections.singletonList(WordExpanderNormaliseBacktickTest.ABC)
            )
        );
    }

    @Test
    void pathnameExpandNoMatchKeepsPattern() {
        Assertions.assertEquals(
            Collections.singletonList("__no_match_xyz__"),
            WordExpander.pathnameExpand(Collections.singletonList("__no_match_xyz__"))
        );
    }

    @Test
    void pathnameExpansionSkipsDotfilesAndSortsFirst() throws IOException {
        final File dir = new TempDirs(WordExpanderNormaliseBacktickTest.root).folder("we-glob");
        Files.write(new File(dir, "b.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        Files.write(new File(dir, ".hidden").toPath(), new byte[0]);
        Assertions.assertEquals(
            Arrays.asList("a.txt", "b.txt"),
            WordExpander.pathnameExpand(Collections.singletonList("*.txt"), dir)
        );
    }

    @Test
    void pathnameExpansionSkipsDotfilesAndSortsSecond() throws IOException {
        final File dir = new TempDirs(WordExpanderNormaliseBacktickTest.root).folder("we-glob");
        Files.write(new File(dir, "b.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        Files.write(new File(dir, ".hidden").toPath(), new byte[0]);
        WordExpander.pathnameExpand(Collections.singletonList("*.txt"), dir);
        Assertions.assertEquals(
            Collections.singletonList(".hidden"),
            WordExpander.pathnameExpand(Collections.singletonList(".*"), dir)
        );
    }

    @Test
    void pathnameExpansionSkipsDotfilesAndSortsThird() throws IOException {
        final File dir = new TempDirs(WordExpanderNormaliseBacktickTest.root).folder("we-glob");
        Files.write(new File(dir, "b.txt").toPath(), new byte[0]);
        Files.write(new File(dir, "a.txt").toPath(), new byte[0]);
        Files.write(new File(dir, ".hidden").toPath(), new byte[0]);
        WordExpander.pathnameExpand(Collections.singletonList("*.txt"), dir);
        WordExpander.pathnameExpand(Collections.singletonList(".*"), dir);
        Assertions.assertEquals(
            Collections.singletonList("no-match*"),
            WordExpander.pathnameExpand(Collections.singletonList("no-match*"), dir)
        );
    }

    @Test
    void prefixAndSuffixOperatorsAtBoundaries() {
        Assertions.assertEquals(
            String.format("abc||abc||bc|ab|%n"),
            Sh4j.INSTANCE.out(
                "X=abc; echo \"${X#}|${X##*}|${X%}|${X%%*}|${X#a*}|${X%c}|${X##a*}\""
            )
        );
    }

    @Test
    void prefixAndSuffixFullMatchBoundaries() {
        Assertions.assertEquals(
            String.format("||||abc%n"),
            Sh4j.INSTANCE.out("X=abc; echo \"${X#abc}|${X##abc}|${X%abc}|${X%%abc}|${X#z}\"")
        );
    }

    @Test
    void prefixAndSuffixNoMatchForms() {
        Assertions.assertEquals(
            String.format("abc|abc|abc|abc%n"),
            Sh4j.INSTANCE.out("X=abc; echo \"${X#z}|${X##z}|${X%z}|${X%%z}\"")
        );
    }

    @Test
    void quoteAndGlobBoundariesFirst() {
        Assertions.assertFalse(WordExpander.isQuoted(null));
    }

    @Test
    void quoteAndGlobBoundariesSecond() {
        Assertions.assertFalse(WordExpander.isQuoted(""));
    }

    @Test
    void quoteAndGlobBoundariesThird() {
        Assertions.assertFalse(WordExpander.isQuoted("\""));
    }

    @Test
    void quoteAndGlobBoundariesFourth() {
        Assertions.assertFalse(WordExpander.containsGlobChars("abc"));
    }

    @Test
    void quoteAndGlobBoundariesFifth() {
        WordExpander.containsGlobChars("abc");
        Assertions.assertTrue(WordExpander.containsGlobChars("a?c"));
    }

    @Test
    void quoteAndGlobBoundariesSixth() {
        WordExpander.containsGlobChars("abc");
        WordExpander.containsGlobChars("a?c");
        Assertions.assertTrue(WordExpander.containsGlobChars("a[c"));
    }

    @Test
    void quoteBoundaryAndEmptyEscapeFirst() {
        Assertions.assertTrue(WordExpander.isQuoted("\"\""));
    }

    @Test
    void quoteBoundaryAndEmptyEscapeSecond() {
        Assertions.assertTrue(WordExpander.isQuoted("''"));
    }

    @Test
    void quoteBoundaryAndEmptyEscapeThird() {
        Assertions.assertFalse(WordExpander.isQuoted("\"x"));
    }

    @Test
    void quoteBoundaryAndEmptyEscapeFourth() {
        Assertions.assertNull(WordExpander.escapeForQuoteRemoval(null));
    }

    @Test
    void quoteBoundaryAndEmptyEscapeFifth() {
        WordExpander.escapeForQuoteRemoval(null);
        Assertions.assertEquals("", WordExpander.escapeForQuoteRemoval(""));
    }

    @Test
    void removeQuotesDouble() {
        Assertions.assertEquals("hello", WordExpander.removeQuotes("\"hello\""));
    }

    @Test
    void removeQuotesSingle() {
        Assertions.assertEquals("hello", WordExpander.removeQuotes("'hello'"));
    }

    @Test
    void removeQuotesUnquoted() {
        Assertions.assertEquals(
            WordExpanderNormaliseBacktickTest.ABC,
            WordExpander.removeQuotes(WordExpanderNormaliseBacktickTest.ABC)
        );
    }

    @Test
    void removeQuotesBoundaryFormsFirst() {
        Assertions.assertEquals("", WordExpander.removeQuotes(""));
    }

    @Test
    void removeQuotesBoundaryFormsSecond() {
        WordExpander.removeQuotes("");
        Assertions.assertEquals("a", WordExpander.removeQuotes("'a"));
    }

    @Test
    void removeQuotesBoundaryFormsThird() {
        WordExpander.removeQuotes("");
        WordExpander.removeQuotes("'a");
        Assertions.assertEquals("a", WordExpander.removeQuotes("\"a"));
    }

    @Test
    void removeQuotesBoundaryFormsFourth() {
        WordExpander.removeQuotes("");
        WordExpander.removeQuotes("'a");
        WordExpander.removeQuotes("\"a");
        Assertions.assertEquals("a\\", WordExpander.removeQuotes("a\\"));
    }

    @Test
    void removeQuotesBoundaryFormsFifth() {
        WordExpander.removeQuotes("");
        WordExpander.removeQuotes("'a");
        WordExpander.removeQuotes("\"a");
        WordExpander.removeQuotes("a\\");
        Assertions.assertEquals("a\\qb", WordExpander.removeQuotes("\"a\\qb\""));
    }

    @Test
    void removeQuotesEscapedDoubleAndEmptyFirst() {
        Assertions.assertEquals("", WordExpander.removeQuotes("\"\""));
    }

    @Test
    void removeQuotesEscapedDoubleAndEmptySecond() {
        WordExpander.removeQuotes("\"\"");
        Assertions.assertEquals("a\"b", WordExpander.removeQuotes("\"a\\\"b\""));
    }

    @Test
    void removeQuotesEscapedDoubleAndEmptyThird() {
        WordExpander.removeQuotes("\"\"");
        WordExpander.removeQuotes("\"a\\\"b\"");
        Assertions.assertEquals("a\\", WordExpander.removeQuotes("\"a\\\\\""));
    }

    @Test
    void removeQuotesUnclosedAndEscapedFormsFirst() {
        Assertions.assertEquals("a", WordExpander.removeQuotes("'a"));
    }

    @Test
    void removeQuotesUnclosedAndEscapedFormsSecond() {
        WordExpander.removeQuotes("'a");
        Assertions.assertEquals("a", WordExpander.removeQuotes("\"a"));
    }

    @Test
    void removeQuotesUnclosedAndEscapedFormsThird() {
        WordExpander.removeQuotes("'a");
        WordExpander.removeQuotes("\"a");
        Assertions.assertEquals("ab", WordExpander.removeQuotes("a\\b"));
    }

    @Test
    void removeQuotesUnclosedAndEscapedFormsFourth() {
        WordExpander.removeQuotes("'a");
        WordExpander.removeQuotes("\"a");
        WordExpander.removeQuotes("a\\b");
        Assertions.assertEquals("a\\", WordExpander.removeQuotes("a\\"));
    }

    @Test
    void splitFieldsDefaultIfs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a b c", String.format(" \t%n"))
        );
    }
}
