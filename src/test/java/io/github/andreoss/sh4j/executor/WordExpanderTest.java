/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Tests for WordExpander.
 * @since 1.0
 */
final class WordExpanderTest {

    @Test void tildeHome() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/user");
        Assertions.assertEquals("/home/user/x", WordExpander.expandTilde("~/x", env));
    }

    @Test void tildePlusUsesPwd() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/cur");
        Assertions.assertEquals("/cur/y", WordExpander.expandTilde("~+/y", env));
    }

    @Test void tildeNoOpWhenNoLeadingTilde() {
        Assertions.assertEquals("abc", WordExpander.expandTilde("abc", new HashMap<>()));
    }

    @Test void normaliseBackticksBasic() {
        Assertions.assertEquals("$(echo x)", WordExpander.normaliseBackticks("`echo x`"));
    }

    @Test void normaliseBackticksDollarParenPassthrough() {
        Assertions.assertEquals("$(cmd)", WordExpander.normaliseBackticks("$(cmd)"));
    }

    @Test void splitFieldsDefaultIfs() {
        Assertions.assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a b c", String.format(" \t%n")));
    }

    @Test void splitFieldsCustomIfs() {
        Assertions.assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a:b:c", ":"));
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
    void removeQuotesDouble() {
        Assertions.assertEquals("hello", WordExpander.removeQuotes("\"hello\""));
    }

    @Test
    void removeQuotesSingle() {
        Assertions.assertEquals("hello", WordExpander.removeQuotes("'hello'"));
    }

    @Test
    void removeQuotesUnquoted() {
        Assertions.assertEquals("abc", WordExpander.removeQuotes("abc"));
    }

    @Test
    void containsGlobCharsStar() {
        Assertions.assertTrue(WordExpander.containsGlobChars("a*b"));
    }

    @Test
    void containsGlobCharsQuestion() {
        Assertions.assertTrue(WordExpander.containsGlobChars("a?b"));
    }

    @Test
    void containsGlobCharsBracket() {
        Assertions.assertTrue(WordExpander.containsGlobChars("a[b"));
    }

    @Test
    void containsGlobCharsNone() {
        Assertions.assertFalse(WordExpander.containsGlobChars("abc"));
    }

    @Test void pathnameExpandNoGlob() {
        Assertions.assertEquals(List.of("abc"), WordExpander.pathnameExpand(List.of("abc")));
    }

    @Test void pathnameExpandNoMatchKeepsPattern() {
        Assertions.assertEquals(List.of("__no_match_xyz__"), WordExpander.pathnameExpand(List.of("__no_match_xyz__")));
    }

    @Test void expandVariablesParityPlainText() {
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("hello world", new HashMap<>(), new Executor(new HashMap<>()));
        Assertions.assertEquals("hello world", result.value());
        Assertions.assertFalse(result.error());
    }

    @Test void expandVariablesParityVarSubstitution() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("MSG", "world");
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("hello $MSG", env, excp);
        Assertions.assertEquals("hello world", result.value());
        Assertions.assertFalse(result.error());
    }

    @Test void expandVariablesParityArithmetic() {
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("$((3 + 4))", new HashMap<>(), new Executor(new HashMap<>()));
        Assertions.assertEquals("7", result.value());
        Assertions.assertFalse(result.error());
    }

    @Test void expandVariablesParityColonQuestionUnsetSetsError() {
        Assertions.assertTrue(WordExpander.expandVariables("${UNSET_VAR_FOR_PARITY:?required}", new HashMap<>(), new Executor(new HashMap<>())).error(), "expected error flag set on :? for unset var");
    }

    @Test void expandVariablesUnclosedArithmetic() {
        Assertions.assertEquals("$((1+2", WordExpander.expandVariables("$((1+2", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandVariablesUnclosedCmdSub() {
        Assertions.assertEquals("$(unclosed", WordExpander.expandVariables("$(unclosed", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandVariablesDollarAtEnd() {
        Assertions.assertEquals("trailing$", WordExpander.expandVariables("trailing$", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandVariablesDollarBraceWithoutClose() {
        Assertions.assertEquals("", WordExpander.expandVariables("${unclosed", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandVariablesDollarLetterPlainVar() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("X", "hi");
        Assertions.assertEquals("hi-suffix", WordExpander.expandVariables("$X-suffix", env, exe).value());
    }

    @Test void expandVariablesParityErrorFlagConsumedAfterRead() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        Assertions.assertTrue(WordExpander.expandVariables("${UNSET_AGAIN:?msg}", env, excp).error());
        Assertions.assertFalse(WordExpander.expandVariables("plain", env, excp).error(), "flag should be consumed by previous wrapper call");
    }

    @Test void expandBracedColonDashUnsetReturnsDefault() {
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x:-fallback", new HashMap<>(), new Executor(new HashMap<>()));
        Assertions.assertEquals("fallback", result.value());
        Assertions.assertFalse(result.error());
    }

    @Test void expandBracedColonEqUnsetAssigns() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("42", WordExpander.expandBraced("x:=42", env, excp).value());
        Assertions.assertEquals("42", env.get("x"));
    }

    @Test void expandBracedColonQuestionUnsetSetsError() {
        Assertions.assertTrue(WordExpander.expandBraced("x:?required", new HashMap<>(), new Executor(new HashMap<>())).error());
    }

    @Test void expandBracedHashLength() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("v", "hello");
        Assertions.assertEquals("5", WordExpander.expandBraced("#v", env, excp).value());
    }

    @Test void expandBracedDashWithoutColonUnset() {
        Assertions.assertEquals("fallback", WordExpander.expandBraced("x-fallback", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandBracedDashWithoutColonSetEmpty() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        Assertions.assertEquals("", WordExpander.expandBraced("x-fallback", env, exe).value(), "set-but-empty: no fallback");
    }

    @Test void expandBracedPlusSet() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        Assertions.assertEquals("word", WordExpander.expandBraced("x+word", env, exe).value());
    }

    @Test void expandBracedColonPlusSetNonEmpty() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("x", "v");
        Assertions.assertEquals("word", WordExpander.expandBraced("x:+word", env, exe).value());
    }

    @Test void expandBracedQuestionWithoutColonUnsetSetsError() {
        Assertions.assertTrue(WordExpander.expandBraced("x?required", new HashMap<>(), new Executor(new HashMap<>())).error());
    }

    @Test void expandBracedEqualsWithoutColonUnsetAssigns() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("42", WordExpander.expandBraced("x=42", env, exe).value());
        Assertions.assertEquals("42", env.get("x"));
    }

    @Test void expandBracedHashHashLongestPrefix() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a/b/c/d");
        Assertions.assertEquals("d", WordExpander.expandBraced("f##*/", env, exe).value());
    }

    @Test void expandBracedPercentPercentLongestSuffix() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.b.c");
        Assertions.assertEquals("a", WordExpander.expandBraced("f%%.*", env, exe).value());
    }

    @Test void splitFieldsConsecutiveIfsCreatesEmptyField() {
        final java.util.List<String> result = WordExpander.splitFields("a::b", ":");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("", result.get(1));
        Assertions.assertEquals("b", result.get(2));
    }

    @Test void globMatchesEmptyPattern() {
        Assertions.assertTrue(WordExpander.globMatches("", ""));
        Assertions.assertFalse(WordExpander.globMatches("", "anything"));
    }

    @Test void expandBracedPercentSuffix() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.txt");
        Assertions.assertEquals("a", WordExpander.expandBraced("f%.txt", env, excp).value());
    }

    @Test
    void globMatchesStar() {
        Assertions.assertTrue(WordExpander.globMatches("a*", "abc"));
    }

    @Test
    void globMatchesQuestion() {
        Assertions.assertTrue(WordExpander.globMatches("a?c", "abc"));
    }

    @Test
    void globMatchesCharClass() {
        Assertions.assertTrue(WordExpander.globMatches("[abc]", "b"));
    }

    @Test
    void globMatchesNoMatch() {
        Assertions.assertFalse(WordExpander.globMatches("a*", "xyz"));
    }
}
