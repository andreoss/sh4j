/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WordExpanderTest {

    @Test void tildeHome() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/user");
        assertEquals("/home/user/x", WordExpander.expandTilde("~/x", env));
    }

    @Test void tildePlusUsesPwd() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/cur");
        assertEquals("/cur/y", WordExpander.expandTilde("~+/y", env));
    }

    @Test void tildeNoOpWhenNoLeadingTilde() {
        assertEquals("abc", WordExpander.expandTilde("abc", new HashMap<>()));
    }

    @Test void normaliseBackticksBasic() {
        assertEquals("$(echo x)", WordExpander.normaliseBackticks("`echo x`"));
    }

    @Test void normaliseBackticksDollarParenPassthrough() {
        assertEquals("$(cmd)", WordExpander.normaliseBackticks("$(cmd)"));
    }

    @Test void splitFieldsDefaultIfs() {
        assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a b c", String.format(" \t%n")));
    }

    @Test void splitFieldsCustomIfs() {
        assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a:b:c", ":"));
    }

    @Test
    void isQuotedDouble() {
        assertTrue(WordExpander.isQuoted("\"x\""));
    }

    @Test
    void isQuotedSingle() {
        assertTrue(WordExpander.isQuoted("'x'"));
    }

    @Test
    void isQuotedFalseMixed() {
        assertFalse(WordExpander.isQuoted("\"x'"));
    }

    @Test
    void isQuotedFalseShort() {
        assertFalse(WordExpander.isQuoted("x"));
    }

    @Test
    void removeQuotesDouble() {
        assertEquals("hello", WordExpander.removeQuotes("\"hello\""));
    }

    @Test
    void removeQuotesSingle() {
        assertEquals("hello", WordExpander.removeQuotes("'hello'"));
    }

    @Test
    void removeQuotesUnquoted() {
        assertEquals("abc", WordExpander.removeQuotes("abc"));
    }

    @Test
    void containsGlobCharsStar() {
        assertTrue(WordExpander.containsGlobChars("a*b"));
    }

    @Test
    void containsGlobCharsQuestion() {
        assertTrue(WordExpander.containsGlobChars("a?b"));
    }

    @Test
    void containsGlobCharsBracket() {
        assertTrue(WordExpander.containsGlobChars("a[b"));
    }

    @Test
    void containsGlobCharsNone() {
        assertFalse(WordExpander.containsGlobChars("abc"));
    }

    @Test void pathnameExpandNoGlob() {
        assertEquals(List.of("abc"), WordExpander.pathnameExpand(List.of("abc")));
    }

    @Test void pathnameExpandNoMatchKeepsPattern() {
        assertEquals(List.of("__no_match_xyz__"), WordExpander.pathnameExpand(List.of("__no_match_xyz__")));
    }

    @Test void expandVariablesParityPlainText() {
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("hello world", new HashMap<>(), new Executor(new HashMap<>()));
        assertEquals("hello world", result.value());
        assertFalse(result.error());
    }

    @Test void expandVariablesParityVarSubstitution() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("MSG", "world");
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("hello $MSG", env, excp);
        assertEquals("hello world", result.value());
        assertFalse(result.error());
    }

    @Test void expandVariablesParityArithmetic() {
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("$((3 + 4))", new HashMap<>(), new Executor(new HashMap<>()));
        assertEquals("7", result.value());
        assertFalse(result.error());
    }

    @Test void expandVariablesParityColonQuestionUnsetSetsError() {
        assertTrue(WordExpander.expandVariables("${UNSET_VAR_FOR_PARITY:?required}", new HashMap<>(), new Executor(new HashMap<>())).error(), "expected error flag set on :? for unset var");
    }

    @Test void expandVariablesUnclosedArithmetic() {
        // unclosed -> literal text preserved
        assertEquals("$((1+2", WordExpander.expandVariables("$((1+2", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandVariablesUnclosedCmdSub() {
        assertEquals("$(unclosed", WordExpander.expandVariables("$(unclosed", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandVariablesDollarAtEnd() {
        assertEquals("trailing$", WordExpander.expandVariables("trailing$", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandVariablesDollarBraceWithoutClose() {
        // ${unclosed: expandBraced sees no closing `}`, falls through to VAR_PATTERN
        // which then matches ${unclosed (optional close) → resolves to unset var → "".
        assertEquals("", WordExpander.expandVariables("${unclosed", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandVariablesDollarLetterPlainVar() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("X", "hi");
        assertEquals("hi-suffix", WordExpander.expandVariables("$X-suffix", env, exe).value());
    }

    @Test void expandVariablesParityErrorFlagConsumedAfterRead() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        assertTrue(WordExpander.expandVariables("${UNSET_AGAIN:?msg}", env, excp).error());
        assertFalse(WordExpander.expandVariables("plain", env, excp).error(), "flag should be consumed by previous wrapper call");
    }

    @Test void expandBracedColonDashUnsetReturnsDefault() {
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x:-fallback", new HashMap<>(), new Executor(new HashMap<>()));
        assertEquals("fallback", result.value());
        assertFalse(result.error());
    }

    @Test void expandBracedColonEqUnsetAssigns() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        assertEquals("42", WordExpander.expandBraced("x:=42", env, excp).value());
        assertEquals("42", env.get("x"));
    }

    @Test void expandBracedColonQuestionUnsetSetsError() {
        assertTrue(WordExpander.expandBraced("x:?required", new HashMap<>(), new Executor(new HashMap<>())).error());
    }

    @Test void expandBracedHashLength() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("v", "hello");
        assertEquals("5", WordExpander.expandBraced("#v", env, excp).value());
    }

    @Test void expandBracedDashWithoutColonUnset() {
        assertEquals("fallback", WordExpander.expandBraced("x-fallback", new HashMap<>(), new Executor(new HashMap<>())).value());
    }

    @Test void expandBracedDashWithoutColonSetEmpty() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        assertEquals("", WordExpander.expandBraced("x-fallback", env, exe).value(), "set-but-empty: no fallback");
    }

    @Test void expandBracedPlusSet() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("x", "");
        assertEquals("word", WordExpander.expandBraced("x+word", env, exe).value());
    }

    @Test void expandBracedColonPlusSetNonEmpty() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("x", "v");
        assertEquals("word", WordExpander.expandBraced("x:+word", env, exe).value());
    }

    @Test void expandBracedQuestionWithoutColonUnsetSetsError() {
        assertTrue(WordExpander.expandBraced("x?required", new HashMap<>(), new Executor(new HashMap<>())).error());
    }

    @Test void expandBracedEqualsWithoutColonUnsetAssigns() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        assertEquals("42", WordExpander.expandBraced("x=42", env, exe).value());
        assertEquals("42", env.get("x"));
    }

    @Test void expandBracedHashHashLongestPrefix() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a/b/c/d");
        assertEquals("d", WordExpander.expandBraced("f##*/", env, exe).value());
    }

    @Test void expandBracedPercentPercentLongestSuffix() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.b.c");
        assertEquals("a", WordExpander.expandBraced("f%%.*", env, exe).value());
    }

    @Test void splitFieldsConsecutiveIfsCreatesEmptyField() {
        // non-whitespace IFS: consecutive separators produce empty fields per POSIX.
        final java.util.List<String> result = WordExpander.splitFields("a::b", ":");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("", result.get(1));
        assertEquals("b", result.get(2));
    }

    @Test void globMatchesEmptyPattern() {
        assertTrue(WordExpander.globMatches("", ""));
        assertFalse(WordExpander.globMatches("", "anything"));
    }

    @Test void expandBracedPercentSuffix() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("f", "a.txt");
        assertEquals("a", WordExpander.expandBraced("f%.txt", env, excp).value());
    }

    @Test
    void globMatchesStar() {
        assertTrue(WordExpander.globMatches("a*", "abc"));
    }

    @Test
    void globMatchesQuestion() {
        assertTrue(WordExpander.globMatches("a?c", "abc"));
    }

    @Test
    void globMatchesCharClass() {
        assertTrue(WordExpander.globMatches("[abc]", "b"));
    }

    @Test
    void globMatchesNoMatch() {
        assertFalse(WordExpander.globMatches("a*", "xyz"));
    }
}
