/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for WordExpander.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class WordExpanderTest {

    /** ABC constant. */
    private static final String ABC = "abc";

    @Test public void tildeHome() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/user");
        Assert.assertEquals("/home/user/x", WordExpander.expandTilde("~/x", env));
    }

    @Test public void tildePlusUsesPwd() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/cur");
        Assert.assertEquals("/cur/y", WordExpander.expandTilde("~+/y", env));
    }

    @Test public void tildeNoOpWhenNoLeadingTilde() {
        Assert.assertEquals(
            WordExpanderTest.ABC,
            WordExpander.expandTilde(WordExpanderTest.ABC, new HashMap<>())
        );
    }

    @Test public void normaliseBackticksBasic() {
        Assert.assertEquals("$(echo x)", WordExpander.normaliseBackticks("`echo x`"));
    }

    @Test public void normaliseBackticksDollarParenPassthrough() {
        Assert.assertEquals("$(cmd)", WordExpander.normaliseBackticks("$(cmd)"));
    }

    @Test public void splitFieldsDefaultIfs() {
        Assert.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a b c", String.format(" \t%n"))
        );
    }

    @Test public void splitFieldsCustomIfs() {
        Assert.assertEquals(Arrays.asList("a", "b", "c"), WordExpander.splitFields("a:b:c", ":"));
    }

    @Test
    public void isQuotedDouble() {
        Assert.assertTrue(WordExpander.isQuoted("\"x\""));
    }

    @Test
    public void isQuotedSingle() {
        Assert.assertTrue(WordExpander.isQuoted("'x'"));
    }

    @Test
    public void isQuotedFalseMixed() {
        Assert.assertFalse(WordExpander.isQuoted("\"x'"));
    }

    @Test
    public void isQuotedFalseShort() {
        Assert.assertFalse(WordExpander.isQuoted("x"));
    }

    @Test
    public void removeQuotesDouble() {
        Assert.assertEquals("hello", WordExpander.removeQuotes("\"hello\""));
    }

    @Test
    public void removeQuotesSingle() {
        Assert.assertEquals("hello", WordExpander.removeQuotes("'hello'"));
    }

    @Test
    public void removeQuotesUnquoted() {
        Assert.assertEquals(
            WordExpanderTest.ABC,
            WordExpander.removeQuotes(WordExpanderTest.ABC)
        );
    }

    @Test
    public void containsGlobCharsStar() {
        Assert.assertTrue(WordExpander.containsGlobChars("a*b"));
    }

    @Test
    public void containsGlobCharsQuestion() {
        Assert.assertTrue(WordExpander.containsGlobChars("a?b"));
    }

    @Test
    public void containsGlobCharsBracket() {
        Assert.assertTrue(WordExpander.containsGlobChars("a[b"));
    }

    @Test
    public void containsGlobCharsNone() {
        Assert.assertFalse(WordExpander.containsGlobChars(WordExpanderTest.ABC));
    }

    @Test public void pathnameExpandNoGlob() {
        Assert.assertEquals(
            Arrays.asList(WordExpanderTest.ABC),
            WordExpander.pathnameExpand(Arrays.asList(WordExpanderTest.ABC))
        );
    }

    @Test public void pathnameExpandNoMatchKeepsPattern() {
        Assert.assertEquals(
            Arrays.asList("__no_match_xyz__"),
            WordExpander.pathnameExpand(Arrays.asList("__no_match_xyz__"))
        );
    }

    @Test public void expandVariablesParityPlainText() {
        final WordExpander.ExpansionResult result = WordExpander.expandVariables(
            "hello world",
            new HashMap<>(),
            new Executor(new HashMap<>())
        );
        Assert.assertEquals("hello world", result.value);
        Assert.assertFalse(result.error);
    }

    @Test public void expandVariablesParityVarSubstitution() {
        final Map<String, String> env = new HashMap<>();
        env.put("MSG", "world");
        final WordExpander.ExpansionResult result = WordExpander.expandVariables(
            "hello $MSG",
            env,
            new Executor(new HashMap<>())
        );
        Assert.assertEquals("hello world", result.value);
        Assert.assertFalse(result.error);
    }

    @Test public void expandVariablesParityArithmetic() {
        final WordExpander.ExpansionResult result = WordExpander.expandVariables(
            "$((3 + 4))",
            new HashMap<>(),
            new Executor(new HashMap<>())
        );
        Assert.assertEquals("7", result.value);
        Assert.assertFalse(result.error);
    }

    @Test public void expandVariablesParityColonQuestionUnsetSetsError() {
        Assert.assertTrue(
            "expected error flag set on :? for unset var",
            WordExpander.expandVariables(
                "${UNSET_VAR_FOR_PARITY:?required}",
                new HashMap<>(),
                new Executor(new HashMap<>())
            ).error
        );
    }

    @Test public void expandVariablesUnclosedArithmetic() {
        Assert.assertEquals(
            "$((1+2",
            WordExpander.expandVariables(
                "$((1+2",
                new HashMap<>(),
                new Executor(new HashMap<>())
            ).value
        );
    }

    @Test public void expandVariablesUnclosedCmdSub() {
        Assert.assertEquals(
            "$(unclosed",
            WordExpander.expandVariables(
                "$(unclosed",
                new HashMap<>(),
                new Executor(new HashMap<>())
            ).value
        );
    }

    @Test public void expandVariablesDollarAtEnd() {
        Assert.assertEquals(
            "trailing$",
            WordExpander.expandVariables(
                "trailing$",
                new HashMap<>(),
                new Executor(new HashMap<>())
            ).value
        );
    }

    @Test public void expandVariablesDollarBraceWithoutClose() {
        Assert.assertEquals(
            "",
            WordExpander.expandVariables(
                "${unclosed",
                new HashMap<>(),
                new Executor(new HashMap<>())
            ).value
        );
    }

    @Test public void expandVariablesDollarLetterPlainVar() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "hi");
        Assert.assertEquals(
            "hi-suffix",
            WordExpander.expandVariables("$X-suffix", env, new Executor(new HashMap<>())).value
        );
    }

    @Test public void expandVariablesParityErrorFlagConsumedAfterRead() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        Assert.assertTrue(
            WordExpander.expandVariables("${UNSET_AGAIN:?msg}", env, excp).error
        );
        Assert.assertFalse(
            "flag should be consumed by previous wrapper call",
            WordExpander.expandVariables("plain", env, excp).error
        );
    }

    @Test public void expandBracedColonDashUnsetReturnsDefault() {
        final WordExpander.ExpansionResult result = WordExpander.expandBraced(
            "x:-fallback",
            new HashMap<>(),
            new Executor(new HashMap<>())
        );
        Assert.assertEquals("fallback", result.value);
        Assert.assertFalse(result.error);
    }

    @Test public void expandBracedColonEqUnsetAssigns() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals(
            "42",
            WordExpander.expandBraced("x:=42", env, new Executor(new HashMap<>())).value
        );
        Assert.assertEquals("42", env.get("x"));
    }

    @Test public void expandBracedColonQuestionUnsetSetsError() {
        Assert.assertTrue(
            WordExpander.expandBraced(
                "x:?required",
                new HashMap<>(),
                new Executor(new HashMap<>())
            ).error
        );
    }

    @Test public void expandBracedHashLength() {
        final Map<String, String> env = new HashMap<>();
        env.put("v", "hello");
        Assert.assertEquals(
            "5",
            WordExpander.expandBraced("#v", env, new Executor(new HashMap<>())).value
        );
    }

    @Test public void expandBracedDashWithoutColonUnset() {
        Assert.assertEquals(
            "fallback",
            WordExpander.expandBraced(
                "x-fallback",
                new HashMap<>(),
                new Executor(new HashMap<>())
            ).value
        );
    }

    @Test public void expandBracedDashWithoutColonSetEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        Assert.assertEquals(
            "set-but-empty: no fallback",
            "",
            WordExpander.expandBraced("x-fallback", env, new Executor(new HashMap<>())).value
        );
    }

    @Test public void expandBracedPlusSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        Assert.assertEquals(
            "word",
            WordExpander.expandBraced("x+word", env, new Executor(new HashMap<>())).value
        );
    }

    @Test public void expandBracedColonPlusSetNonEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "v");
        Assert.assertEquals(
            "word",
            WordExpander.expandBraced("x:+word", env, new Executor(new HashMap<>())).value
        );
    }

    @Test public void expandBracedQuestionWithoutColonUnsetSetsError() {
        Assert.assertTrue(
            WordExpander.expandBraced(
                "x?required",
                new HashMap<>(),
                new Executor(new HashMap<>())
            ).error
        );
    }

    @Test public void expandBracedEqualsWithoutColonUnsetAssigns() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals(
            "42",
            WordExpander.expandBraced("x=42", env, new Executor(new HashMap<>())).value
        );
        Assert.assertEquals("42", env.get("x"));
    }

    @Test public void expandBracedHashHashLongestPrefix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a/b/c/d");
        Assert.assertEquals(
            "d",
            WordExpander.expandBraced("f##*/", env, new Executor(new HashMap<>())).value
        );
    }

    @Test public void expandBracedPercentPercentLongestSuffix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.b.c");
        Assert.assertEquals(
            "a",
            WordExpander.expandBraced("f%%.*", env, new Executor(new HashMap<>())).value
        );
    }

    @Test public void splitFieldsConsecutiveIfsCreatesEmptyField() {
        final java.util.List<String> result = WordExpander.splitFields("a::b", ":");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("a", result.get(0));
        Assert.assertEquals("", result.get(1));
        Assert.assertEquals("b", result.get(2));
    }

    @Test public void globMatchesEmptyPattern() {
        Assert.assertTrue(WordExpander.globMatches("", ""));
        Assert.assertFalse(WordExpander.globMatches("", "anything"));
    }

    @Test public void expandBracedPercentSuffix() {
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.txt");
        Assert.assertEquals(
            "a",
            WordExpander.expandBraced("f%.txt", env, new Executor(new HashMap<>())).value
        );
    }

    @Test
    public void globMatchesStar() {
        Assert.assertTrue(WordExpander.globMatches("a*", WordExpanderTest.ABC));
    }

    @Test
    public void globMatchesQuestion() {
        Assert.assertTrue(WordExpander.globMatches("a?c", WordExpanderTest.ABC));
    }

    @Test
    public void globMatchesCharClass() {
        Assert.assertTrue(WordExpander.globMatches("[abc]", "b"));
    }

    @Test
    public void globMatchesNoMatch() {
        Assert.assertFalse(WordExpander.globMatches("a*", "xyz"));
    }
}
