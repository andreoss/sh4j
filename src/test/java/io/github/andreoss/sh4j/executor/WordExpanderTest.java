/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WordExpanderTest {
    @Test void tildeHome() {
        Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/user");
        assertEquals("/home/user/x", WordExpander.expandTilde("~/x", env));
    }

    @Test void tildePlusUsesPwd() {
        Map<String, String> env = new HashMap<>();
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

    @Test void isQuotedDouble() { assertTrue(WordExpander.isQuoted("\"x\"")); }
    @Test void isQuotedSingle() { assertTrue(WordExpander.isQuoted("'x'")); }
    @Test void isQuotedFalseMixed() { assertFalse(WordExpander.isQuoted("\"x'")); }
    @Test void isQuotedFalseShort() { assertFalse(WordExpander.isQuoted("x")); }

    @Test void removeQuotesDouble() { assertEquals("hello", WordExpander.removeQuotes("\"hello\"")); }
    @Test void removeQuotesSingle() { assertEquals("hello", WordExpander.removeQuotes("'hello'")); }
    @Test void removeQuotesUnquoted() { assertEquals("abc", WordExpander.removeQuotes("abc")); }

    @Test void containsGlobCharsStar() { assertTrue(WordExpander.containsGlobChars("a*b")); }
    @Test void containsGlobCharsQuestion() { assertTrue(WordExpander.containsGlobChars("a?b")); }
    @Test void containsGlobCharsBracket() { assertTrue(WordExpander.containsGlobChars("a[b")); }
    @Test void containsGlobCharsNone() { assertFalse(WordExpander.containsGlobChars("abc")); }

    @Test void pathnameExpandNoGlob() {
        assertEquals(List.of("abc"), WordExpander.pathnameExpand(List.of("abc")));
    }

    @Test void pathnameExpandNoMatchKeepsPattern() {
        List<String> r = WordExpander.pathnameExpand(List.of("__no_match_xyz__"));
        assertEquals(List.of("__no_match_xyz__"), r);
    }

    @Test void expandVariablesParityPlainText() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        WordExpander.ExpansionResult r = WordExpander.expandVariables("hello world", env, e);
        assertEquals("hello world", r.value());
        assertFalse(r.error());
    }

    @Test void expandVariablesParityVarSubstitution() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        env.put("MSG", "world");
        WordExpander.ExpansionResult r = WordExpander.expandVariables("hello $MSG", env, e);
        assertEquals("hello world", r.value());
        assertFalse(r.error());
    }

    @Test void expandVariablesParityArithmetic() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        WordExpander.ExpansionResult r = WordExpander.expandVariables("$((3 + 4))", env, e);
        assertEquals("7", r.value());
        assertFalse(r.error());
    }

    @Test void expandVariablesParityColonQuestionUnsetSetsError() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        WordExpander.ExpansionResult r = WordExpander.expandVariables("${UNSET_VAR_FOR_PARITY:?required}", env, e);
        assertTrue(r.error(), "expected error flag set on :? for unset var");
    }

    @Test void expandVariablesParityErrorFlagConsumedAfterRead() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        WordExpander.ExpansionResult r1 = WordExpander.expandVariables("${UNSET_AGAIN:?msg}", env, e);
        assertTrue(r1.error());
        WordExpander.ExpansionResult r2 = WordExpander.expandVariables("plain", env, e);
        assertFalse(r2.error(), "flag should be consumed by previous wrapper call");
    }

    @Test void expandBracedColonDashUnsetReturnsDefault() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        WordExpander.ExpansionResult r = WordExpander.expandBraced("x:-fallback", env, e);
        assertEquals("fallback", r.value());
        assertFalse(r.error());
    }

    @Test void expandBracedColonEqUnsetAssigns() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        WordExpander.ExpansionResult r = WordExpander.expandBraced("x:=42", env, e);
        assertEquals("42", r.value());
        assertEquals("42", env.get("x"));
    }

    @Test void expandBracedColonQuestionUnsetSetsError() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        WordExpander.ExpansionResult r = WordExpander.expandBraced("x:?required", env, e);
        assertTrue(r.error());
    }

    @Test void expandBracedHashLength() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        env.put("v", "hello");
        WordExpander.ExpansionResult r = WordExpander.expandBraced("#v", env, e);
        assertEquals("5", r.value());
    }

    @Test void expandBracedPercentSuffix() {
        Executor e = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>();
        env.put("f", "a.txt");
        WordExpander.ExpansionResult r = WordExpander.expandBraced("f%.txt", env, e);
        assertEquals("a", r.value());
    }

    @Test void globMatchesStar() { assertTrue(WordExpander.globMatches("a*", "abc")); }
    @Test void globMatchesQuestion() { assertTrue(WordExpander.globMatches("a?c", "abc")); }
    @Test void globMatchesCharClass() { assertTrue(WordExpander.globMatches("[abc]", "b")); }
    @Test void globMatchesNoMatch() { assertFalse(WordExpander.globMatches("a*", "xyz")); }
}
