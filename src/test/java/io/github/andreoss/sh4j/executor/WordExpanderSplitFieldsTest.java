/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.Sh4j;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WordExpander}: split fields.
 *
 * @since 1.0
 */
final class WordExpanderSplitFieldsTest {

    /** ABC constant. */
    private static final String ABC = "abc";

    @Test
    void splitFieldsCustomIfs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a:b:c", ":")
        );
    }

    @Test
    void splitFieldsConsecutiveIfsCreatesEmptyFieldFirst() {
        Assertions.assertEquals(3, WordExpander.splitFields("a::b", ":").size());
    }

    @Test
    void splitFieldsConsecutiveIfsCreatesEmptyFieldSecond() {
        Assertions.assertEquals("a", WordExpander.splitFields("a::b", ":").get(0));
    }

    @Test
    void splitFieldsConsecutiveIfsCreatesEmptyFieldThird() {
        Assertions.assertEquals("", WordExpander.splitFields("a::b", ":").get(1));
    }

    @Test
    void splitFieldsConsecutiveIfsCreatesEmptyFieldFourth() {
        Assertions.assertEquals("b", WordExpander.splitFields("a::b", ":").get(2));
    }

    @Test
    void substitutionBoundaryFormsFirst() {
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("echo $( )"));
    }

    @Test
    void substitutionBoundaryFormsSecond() {
        Sh4j.INSTANCE.out("echo $( )");
        Assertions.assertEquals(String.format("0%n"), Sh4j.INSTANCE.out("echo $(( ))"));
    }

    @Test
    void substitutionBoundaryFormsThird() {
        Sh4j.INSTANCE.out("echo $( )");
        Sh4j.INSTANCE.out("echo $(( ))");
        Assertions.assertEquals(String.format("$((1%n"), Sh4j.INSTANCE.out("echo $((1"));
    }

    @Test
    void substitutionBoundaryFormsFourth() {
        Sh4j.INSTANCE.out("echo $( )");
        Sh4j.INSTANCE.out("echo $(( ))");
        Sh4j.INSTANCE.out("echo $((1");
        Assertions.assertEquals(String.format("$(x%n"), Sh4j.INSTANCE.out("echo $(x"));
    }

    @Test
    void tildeHome() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/user");
        Assertions.assertEquals("/home/user/x", WordExpander.expandTilde("~/x", env));
    }

    @Test
    void tildePlusUsesPwd() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/cur");
        Assertions.assertEquals("/cur/y", WordExpander.expandTilde("~+/y", env));
    }

    @Test
    void tildeNoOpWhenNoLeadingTilde() {
        Assertions.assertEquals(
            WordExpanderSplitFieldsTest.ABC,
            WordExpander.expandTilde(WordExpanderSplitFieldsTest.ABC, new HashMap<>())
        );
    }

    @Test
    void tildeBoundariesFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/u");
        env.put("PWD", "/cur");
        Assertions.assertEquals("/home/u", WordExpander.expandTilde("~", env));
    }

    @Test
    void tildeBoundariesSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/u");
        env.put("PWD", "/cur");
        WordExpander.expandTilde("~", env);
        Assertions.assertEquals("/cur", WordExpander.expandTilde("~+", env));
    }

    @Test
    void tildeBoundariesThird() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/u");
        env.put("PWD", "/cur");
        WordExpander.expandTilde("~", env);
        WordExpander.expandTilde("~+", env);
        Assertions.assertEquals("", WordExpander.expandTilde("", env));
    }

    @Test
    void tildeUserAndRootBoundariesFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/u");
        env.put("PWD", "/cur");
        Assertions.assertEquals("~user/x", WordExpander.expandTilde("~user/x", env));
    }

    @Test
    void tildeUserAndRootBoundariesSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/u");
        env.put("PWD", "/cur");
        WordExpander.expandTilde("~user/x", env);
        Assertions.assertEquals("/cur/", WordExpander.expandTilde("~+/", env));
    }

    @Test
    void trailingBackslashAndUnclosedExpansionsFirst() {
        Assertions.assertEquals(String.format("a\\%n"), Sh4j.INSTANCE.out("echo a\\"));
    }

    @Test
    void trailingBackslashAndUnclosedExpansionsSecond() {
        Sh4j.INSTANCE.out("echo a\\");
        Assertions.assertEquals(String.format("${%n"), Sh4j.INSTANCE.out("echo ${"));
    }

    @Test
    void trailingBackslashAndUnclosedExpansionsThird() {
        Sh4j.INSTANCE.out("echo a\\");
        Sh4j.INSTANCE.out("echo ${");
        Assertions.assertEquals(String.format("$%n"), Sh4j.INSTANCE.out("echo $"));
    }

    @Test
    void unclosedDollarParenAndNestedBraceStayLiteralFirst() {
        Assertions.assertEquals(String.format("$(abc%n"), Sh4j.INSTANCE.out("echo $(abc"));
    }

    @Test
    void unclosedDollarParenAndNestedBraceStayLiteralSecond() {
        Sh4j.INSTANCE.out("echo $(abc");
        Assertions.assertEquals(String.format("x%n"), Sh4j.INSTANCE.out("echo ${a${b}}x"));
    }

    @Test
    void unclosedDollarParenAndNestedBraceStayLiteralThird() {
        Sh4j.INSTANCE.out("echo $(abc");
        Sh4j.INSTANCE.out("echo ${a${b}}x");
        Assertions.assertEquals(String.format("${a%n"), Sh4j.INSTANCE.out("echo ${a"));
    }
}
