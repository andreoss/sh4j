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
        assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a b c", " \t\n"));
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
        final List<String> result = WordExpander.pathnameExpand(List.of("__no_match_xyz__"));
        assertEquals(List.of("__no_match_xyz__"), result);
    }

    @Test void expandVariablesParityPlainText() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("hello world", env, excp);
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
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("$((3 + 4))", env, excp);
        assertEquals("7", result.value());
        assertFalse(result.error());
    }

    @Test void expandVariablesParityColonQuestionUnsetSetsError() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("${UNSET_VAR_FOR_PARITY:?required}", env, excp);
        assertTrue(result.error(), "expected error flag set on :? for unset var");
    }

    @Test void expandVariablesUnclosedArithmetic() {
        final Executor exe = new Executor(new HashMap<>());
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("$((1+2", new HashMap<>(), exe);
        // unclosed -> literal text preserved
        assertEquals("$((1+2", result.value());
    }

    @Test void expandVariablesUnclosedCmdSub() {
        final Executor exe = new Executor(new HashMap<>());
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("$(unclosed", new HashMap<>(), exe);
        assertEquals("$(unclosed", result.value());
    }

    @Test void expandVariablesDollarAtEnd() {
        final Executor exe = new Executor(new HashMap<>());
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("trailing$", new HashMap<>(), exe);
        assertEquals("trailing$", result.value());
    }

    @Test void expandVariablesDollarBraceWithoutClose() {
        // ${unclosed: expandBraced sees no closing `}`, falls through to VAR_PATTERN
        // which then matches ${unclosed (optional close) → resolves to unset var → "".
        final Executor exe = new Executor(new HashMap<>());
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("${unclosed", new HashMap<>(), exe);
        assertEquals("", result.value());
    }

    @Test void expandVariablesDollarLetterPlainVar() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String,String> env = new HashMap<>();
        env.put("X", "hi");
        final WordExpander.ExpansionResult result = WordExpander.expandVariables("$X-suffix", env, exe);
        assertEquals("hi-suffix", result.value());
    }

    @Test void expandVariablesParityErrorFlagConsumedAfterRead() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        assertTrue(WordExpander.expandVariables("${UNSET_AGAIN:?msg}", env, excp).error());
        assertFalse(WordExpander.expandVariables("plain", env, excp).error(), "flag should be consumed by previous wrapper call");
    }

    @Test void expandBracedColonDashUnsetReturnsDefault() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x:-fallback", env, excp);
        assertEquals("fallback", result.value());
        assertFalse(result.error());
    }

    @Test void expandBracedColonEqUnsetAssigns() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x:=42", env, excp);
        assertEquals("42", result.value());
        assertEquals("42", env.get("x"));
    }

    @Test void expandBracedColonQuestionUnsetSetsError() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x:?required", env, excp);
        assertTrue(result.error());
    }

    @Test void expandBracedHashLength() {
        final Executor excp = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>();
        env.put("v", "hello");
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("#v", env, excp);
        assertEquals("5", result.value());
    }

    @Test void expandBracedDashWithoutColonUnset() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String,String> env = new HashMap<>();
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x-fallback", env, exe);
        assertEquals("fallback", result.value());
    }

    @Test void expandBracedDashWithoutColonSetEmpty() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String,String> env = new HashMap<>();
        env.put("x", "");
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x-fallback", env, exe);
        assertEquals("", result.value(), "set-but-empty: no fallback");
    }

    @Test void expandBracedPlusSet() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String,String> env = new HashMap<>();
        env.put("x", "");
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x+word", env, exe);
        assertEquals("word", result.value());
    }

    @Test void expandBracedColonPlusSetNonEmpty() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String,String> env = new HashMap<>();
        env.put("x", "v");
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x:+word", env, exe);
        assertEquals("word", result.value());
    }

    @Test void expandBracedQuestionWithoutColonUnsetSetsError() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String,String> env = new HashMap<>();
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x?required", env, exe);
        assertTrue(result.error());
    }

    @Test void expandBracedEqualsWithoutColonUnsetAssigns() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String,String> env = new HashMap<>();
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("x=42", env, exe);
        assertEquals("42", result.value());
        assertEquals("42", env.get("x"));
    }

    @Test void expandBracedHashHashLongestPrefix() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String,String> env = new HashMap<>();
        env.put("f", "a/b/c/d");
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("f##*/", env, exe);
        assertEquals("d", result.value());
    }

    @Test void expandBracedPercentPercentLongestSuffix() {
        final Executor exe = new Executor(new HashMap<>());
        final Map<String,String> env = new HashMap<>();
        env.put("f", "a.b.c");
        final WordExpander.ExpansionResult result = WordExpander.expandBraced("f%%.*", env, exe);
        assertEquals("a", result.value());
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
