/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.Sh4j;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WordExpander}: expand braced.
 *
 * @since 1.0
 */
final class WordExpanderExpandBracedTest {

    /** ABC constant. */
    private static final String ABC = "abc";

    @Test
    void expandBracedHashHashLongestPrefix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a/b/c/d");
        Assertions.assertEquals(
            "d",
            WordExpander.expandBracedImpl("f##*/", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void expandBracedPercentPercentLongestSuffix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.b.c");
        Assertions.assertEquals(
            "a",
            WordExpander.expandBracedImpl("f%%.*", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void expandBracedPercentSuffix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.txt");
        Assertions.assertEquals(
            "a",
            WordExpander.expandBracedImpl("f%.txt", env, new Executor(new HashMap<>()))
        );
    }

    @Test
    void globMatchesEmptyPatternFirst() {
        Assertions.assertTrue(WordExpander.globMatches("", ""));
    }

    @Test
    void globMatchesEmptyPatternSecond() {
        WordExpander.globMatches("", "");
        Assertions.assertFalse(WordExpander.globMatches("", "anything"));
    }

    @Test
    void globMatchesStar() {
        Assertions.assertTrue(WordExpander.globMatches("a*", WordExpanderExpandBracedTest.ABC));
    }

    @Test
    void globMatchesQuestion() {
        Assertions.assertTrue(WordExpander.globMatches("a?c", WordExpanderExpandBracedTest.ABC));
    }

    @Test
    void globMatchesCharClass() {
        Assertions.assertTrue(WordExpander.globMatches("[abc]", "b"));
    }

    @Test
    void globMatchesNoMatch() {
        Assertions.assertFalse(WordExpander.globMatches("a*", "xyz"));
    }

    @Test
    void globMatchesBoundariesFirst() {
        Assertions.assertTrue(WordExpander.globMatches("", ""));
    }

    @Test
    void globMatchesBoundariesSecond() {
        WordExpander.globMatches("", "");
        Assertions.assertFalse(WordExpander.globMatches("", "x"));
    }

    @Test
    void globMatchesBoundariesThird() {
        WordExpander.globMatches("", "");
        WordExpander.globMatches("", "x");
        Assertions.assertTrue(WordExpander.globMatches("[abc", "[abc"));
    }

    @Test
    void globMatchesBoundariesFourth() {
        WordExpander.globMatches("", "");
        WordExpander.globMatches("", "x");
        WordExpander.globMatches("[abc", "[abc");
        Assertions.assertTrue(WordExpander.globMatches("a?c", "abc"));
    }

    @Test
    void globMatchesBoundariesFifth() {
        WordExpander.globMatches("", "");
        WordExpander.globMatches("", "x");
        WordExpander.globMatches("[abc", "[abc");
        WordExpander.globMatches("a?c", "abc");
        Assertions.assertFalse(WordExpander.globMatches("a?c", "ac"));
    }

    @Test
    void globMatchesBoundariesSixth() {
        WordExpander.globMatches("", "");
        WordExpander.globMatches("", "x");
        WordExpander.globMatches("[abc", "[abc");
        WordExpander.globMatches("a?c", "abc");
        WordExpander.globMatches("a?c", "ac");
        Assertions.assertTrue(WordExpander.globMatches("a.b", "a.b"));
    }

    @Test
    void globMatchesBoundariesSeventh() {
        WordExpander.globMatches("", "");
        WordExpander.globMatches("", "x");
        WordExpander.globMatches("[abc", "[abc");
        WordExpander.globMatches("a?c", "abc");
        WordExpander.globMatches("a?c", "ac");
        WordExpander.globMatches("a.b", "a.b");
        Assertions.assertFalse(WordExpander.globMatches("a.b", "axb"));
    }

    @Test
    void globCharacterBoundariesFirst() {
        Assertions.assertFalse(WordExpander.containsGlobChars(""));
    }

    @Test
    void globCharacterBoundariesSecond() {
        WordExpander.containsGlobChars("");
        Assertions.assertTrue(WordExpander.globMatches("[", "["));
    }

    @Test
    void globCharacterBoundariesThird() {
        WordExpander.containsGlobChars("");
        WordExpander.globMatches("[", "[");
        Assertions.assertFalse(WordExpander.globMatches("[", "x"));
    }

    @Test
    void globCharacterBoundariesFourth() {
        WordExpander.containsGlobChars("");
        WordExpander.globMatches("[", "[");
        WordExpander.globMatches("[", "x");
        Assertions.assertTrue(WordExpander.containsGlobChars("*"));
    }

    @Test
    void globCharacterBoundariesFifth() {
        WordExpander.containsGlobChars("");
        WordExpander.globMatches("[", "[");
        WordExpander.globMatches("[", "x");
        WordExpander.containsGlobChars("*");
        Assertions.assertTrue(WordExpander.containsGlobChars("?"));
    }

    @Test
    void globCharacterBoundariesSixth() {
        WordExpander.containsGlobChars("");
        WordExpander.globMatches("[", "[");
        WordExpander.globMatches("[", "x");
        WordExpander.containsGlobChars("*");
        WordExpander.containsGlobChars("?");
        Assertions.assertTrue(WordExpander.containsGlobChars("["));
    }

    @Test
    void globCharacterBoundariesSeventh() {
        WordExpander.containsGlobChars("");
        WordExpander.globMatches("[", "[");
        WordExpander.globMatches("[", "x");
        WordExpander.containsGlobChars("*");
        WordExpander.containsGlobChars("?");
        WordExpander.containsGlobChars("[");
        Assertions.assertTrue(WordExpander.globMatches("[ab]", "a"));
    }

    @Test
    void globCharacterBoundariesEighth() {
        WordExpander.containsGlobChars("");
        WordExpander.globMatches("[", "[");
        WordExpander.globMatches("[", "x");
        WordExpander.containsGlobChars("*");
        WordExpander.containsGlobChars("?");
        WordExpander.containsGlobChars("[");
        WordExpander.globMatches("[ab]", "a");
        Assertions.assertFalse(WordExpander.globMatches("[ab]", "c"));
    }

    @Test
    void identifierBoundariesAndEmptyBraceFirst() {
        Assertions.assertEquals(
            String.format("zu a1 a_%n"),
            Sh4j.INSTANCE.out("Z=z; _=u; A1=a1; A_=a_; echo $Z$_ $A1 $A_")
        );
    }

    @Test
    void identifierBoundariesAndEmptyBraceSecond() {
        Sh4j.INSTANCE.out("Z=z; _=u; A1=a1; A_=a_; echo $Z$_ $A1 $A_");
        Assertions.assertEquals(String.format("${}x%n"), Sh4j.INSTANCE.out("echo ${}x"));
    }

    @Test
    void identifierAndSubstitutionBoundariesFirst() {
        Assertions.assertEquals(
            String.format("zu n%n"),
            Sh4j.INSTANCE.out("Z=z; _=u; Z9=n; echo $Z$_ $Z9")
        );
    }

    @Test
    void identifierAndSubstitutionBoundariesSecond() {
        Sh4j.INSTANCE.out("Z=z; _=u; Z9=n; echo $Z$_ $Z9");
        Assertions.assertEquals(
            String.format("1 true false%n"),
            Sh4j.INSTANCE.out("echo $((1)) $(echo true) $(echo false)")
        );
    }

    @Test
    void identifierAndSubstitutionBoundariesThird() {
        Sh4j.INSTANCE.out("Z=z; _=u; Z9=n; echo $Z$_ $Z9");
        Sh4j.INSTANCE.out("echo $((1)) $(echo true) $(echo false)");
        Assertions.assertEquals(
            String.format("x 2%n"),
            Sh4j.INSTANCE.out("set -- x y; echo ${1} ${#}")
        );
    }

    @Test
    void isQuotedDouble() {
        Assertions.assertTrue(WordExpander.isQuoted("\"x\""));
    }

    @Test
    void isQuotedSingle() {
        Assertions.assertTrue(WordExpander.isQuoted("'x'"));
    }

    @Test
    void isQuotedFalseMixed() {
        Assertions.assertFalse(WordExpander.isQuoted("\"x'"));
    }

    @Test
    void isQuotedFalseShort() {
        Assertions.assertFalse(WordExpander.isQuoted("x"));
    }

    @Test
    void nestedBraceMatchingFirst() {
        Assertions.assertEquals(String.format("x%n"), Sh4j.INSTANCE.out("A=x; echo ${A:-${B:-d}}"));
    }

    @Test
    void nestedBraceMatchingSecond() {
        Sh4j.INSTANCE.out("A=x; echo ${A:-${B:-d}}");
        Assertions.assertEquals(String.format("y%n"), Sh4j.INSTANCE.out("B=y; echo ${A:-${B:-d}}"));
    }

    @Test
    void nestedBraceMatchingThird() {
        Sh4j.INSTANCE.out("A=x; echo ${A:-${B:-d}}");
        Sh4j.INSTANCE.out("B=y; echo ${A:-${B:-d}}");
        Assertions.assertEquals(String.format("x%n"), Sh4j.INSTANCE.out("echo ${a{b}c}x"));
    }

    @Test
    void normaliseBackticksBasic() {
        Assertions.assertEquals("$(echo x)", WordExpander.normaliseBackticks("`echo x`"));
    }

    @Test
    void normaliseBackticksDollarParenPassthrough() {
        Assertions.assertEquals("$(cmd)", WordExpander.normaliseBackticks("$(cmd)"));
    }

    @Test
    void normaliseBacktickNestedAndUnclosedFirst() {
        Assertions.assertEquals("$(echo ${x})", WordExpander.normaliseBackticks("`echo ${x}`"));
    }

    @Test
    void normaliseBacktickNestedAndUnclosedSecond() {
        WordExpander.normaliseBackticks("`echo ${x}`");
        Assertions.assertEquals("`unclosed", WordExpander.normaliseBackticks("`unclosed"));
    }
}
