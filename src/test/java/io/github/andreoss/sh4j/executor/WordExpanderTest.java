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

    @Test void globMatchesStar() { assertTrue(WordExpander.globMatches("a*", "abc")); }
    @Test void globMatchesQuestion() { assertTrue(WordExpander.globMatches("a?c", "abc")); }
    @Test void globMatchesCharClass() { assertTrue(WordExpander.globMatches("[abc]", "b")); }
    @Test void globMatchesNoMatch() { assertFalse(WordExpander.globMatches("a*", "xyz")); }
}
