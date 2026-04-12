/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: alias.
 *
 * @since 1.0
 */
final class Sh4jPosixAliasTest {

    @Test
    void aliasExpands() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.out("alias ll='echo hello'", "ll")
        );
    }

    @Test
    void aliasWithArguments() {
        Assertions.assertEquals(
            String.format("hello world%n"),
            Sh4j.INSTANCE.out("alias ll='echo'", "ll hello world")
        );
    }

    @Test
    void andListShortCircuitsOnFailureFirst() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("false && echo 1"));
    }

    @Test
    void andListShortCircuitsOnFailureSecond() {
        Sh4j.INSTANCE.run("false && echo 1");
        Assertions.assertEquals("", Sh4j.INSTANCE.out("false && echo 1"));
    }

    @Test
    void andListContinuesOnSuccess() {
        Assertions.assertEquals(String.format("yes%n"), Sh4j.INSTANCE.out("true && echo yes"));
    }

    @Test
    void andListShortCircuitsNoErrexit() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e; false && true"));
    }

    @Test
    void appendRedirection() {
        Assertions.assertEquals(
            String.format("hello%nworld%n"),
            Sh4j.INSTANCE.out(
                "echo hello > /tmp/sh4j_test_app.txt",
                "echo world >> /tmp/sh4j_test_app.txt",
                "cat /tmp/sh4j_test_app.txt"
            )
        );
    }

    @Test
    void arithmeticAddition() {
        Assertions.assertEquals(String.format("7%n"), Sh4j.INSTANCE.out("echo $((3 + 4))"));
    }

    @Test
    void arithmeticMultiplication() {
        Assertions.assertEquals(String.format("12%n"), Sh4j.INSTANCE.out("echo $((3 * 4))"));
    }

    @Test
    void arithmeticWithVariables() {
        Assertions.assertEquals(
            String.format("10%n"),
            Sh4j.INSTANCE.out("x=3; y=7; echo $((x + y))")
        );
    }

    @Test
    void arithmeticNested() {
        Assertions.assertEquals(String.format("14%n"), Sh4j.INSTANCE.out("echo $((2 * (3 + 4)))"));
    }

    @Test
    void backslashEscapesNextChar() {
        Assertions.assertEquals(String.format("$%n"), Sh4j.INSTANCE.out("echo \\$"));
    }

    @Test
    void backslashNewlineContinuation() {
        Assertions.assertEquals(
            String.format("hello world%n"),
            Sh4j.INSTANCE.out(String.format("echo hello \\%nworld"))
        );
    }

    @Test
    void backtickCommandSubstitution() {
        Assertions.assertEquals(String.format("hello%n"), Sh4j.INSTANCE.out("echo `echo hello`"));
    }

    @Test
    void bgConfirmsJob() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("true & bg").contains("[1]"));
    }

    @Test
    void breakExitsLoop() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("for i in 1 2 3; do echo $i; break; echo after; done")
        );
    }

    @Test
    void casePatternMatch() {
        Assertions.assertEquals(
            String.format("matched%n"),
            Sh4j.INSTANCE.out("case foo in foo) echo matched;; esac")
        );
    }

    @Test
    void caseWildcardMatch() {
        Assertions.assertEquals(
            String.format("wild%n"),
            Sh4j.INSTANCE.out("case bar in f*) echo foo;; *) echo wild;; esac")
        );
    }

    @Test
    void caseNoMatch() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("case bar in foo) echo matched;; esac"));
    }

    @Test
    void cdChangesDirectory() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("cd /tmp", "pwd").trim().startsWith("/tmp"));
    }

    @Test
    void cdDashReturnsToOldPwd() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/tmp");
        env.put("OLDPWD", "/home");
        Assertions.assertTrue(Sh4j.INSTANCE.out(env, "cd -").contains("/home"));
    }

    @Test
    void colonDashUseDefaultIfUnsetOrNull() {
        Assertions.assertEquals(
            String.format("default%n"),
            Sh4j.INSTANCE.out("echo ${UNSET_XYZ:-default}")
        );
    }

    @Test
    void colonDashUseDefaultIfEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "");
        Assertions.assertEquals(
            String.format("default%n"),
            Sh4j.INSTANCE.out(env, "echo ${X:-default}")
        );
    }

    @Test
    void commandWithArguments() {
        Assertions.assertEquals(String.format("a b c%n"), Sh4j.INSTANCE.out("echo a b c"));
    }

    @Test
    void commandWithInlineAssignment() {
        Assertions.assertEquals(String.format("hello%n"), Sh4j.INSTANCE.out("X=hello echo $X"));
    }

    @Test
    void commandSubstitution() {
        Assertions.assertEquals(String.format("hello%n"), Sh4j.INSTANCE.out("echo $(echo hello)"));
    }

    @Test
    void continueSkipsRestOfLoop() {
        Assertions.assertEquals(
            String.format("1%n3%n"),
            Sh4j.INSTANCE.out(
                "for i in 1 2 3; do if [ $i -eq 2 ]; then continue; fi; echo $i; done"
            )
        );
    }

    @Test
    void dashUseDefaultIfUnset() {
        Assertions.assertEquals(
            String.format("default%n"),
            Sh4j.INSTANCE.out("echo ${UNSET_XYZ-default}")
        );
    }

    @Test
    void dashUseDefaultIfSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "value");
        Assertions.assertEquals(
            String.format("value%n"),
            Sh4j.INSTANCE.out(env, "echo ${X-default}")
        );
    }

    @Test
    void dollarZeroIsScriptName() {
        Assertions.assertEquals(String.format("sh%n"), Sh4j.INSTANCE.out("echo $0"));
    }

    @Test
    void dollarHashIsParamCount() {
        Assertions.assertEquals(String.format("3%n"), Sh4j.INSTANCE.out("set -- a b c; echo $#"));
    }

    @Test
    void dollarAtIsAllParams() {
        Assertions.assertEquals(
            String.format("a b c%n"),
            Sh4j.INSTANCE.out("set -- a b c; echo $@")
        );
    }

    @Test
    void dollarStarIsAllParams() {
        Assertions.assertEquals(
            String.format("a b c%n"),
            Sh4j.INSTANCE.out("set -- a b c; echo $*")
        );
    }

    @Test
    void dollarDollarIsCurrentPid() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("echo $$").trim().matches("\\d+"));
    }

    @Test
    void dollarBangIsLastBgPid() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("sleep 0 & echo $!").trim().matches("\\d+"));
    }

    @Test
    void doubleQuotesAllowExpansion() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/test");
        Assertions.assertEquals(
            String.format("/home/test%n"),
            Sh4j.INSTANCE.out(env, "echo \"$HOME\"")
        );
    }

    @Test
    void equalsAssignDefaultIfUnsetFirst() {
        Assertions.assertEquals(
            String.format("default%n"),
            Sh4j.INSTANCE.out("echo ${UNSET_XYZ=default}")
        );
    }

    @Test
    void equalsAssignDefaultIfUnsetSecond() {
        Sh4j.INSTANCE.out("echo ${UNSET_XYZ=default}");
        Assertions.assertEquals(
            "default",
            Sh4j.INSTANCE.getVar("UNSET_XYZ", "echo ${UNSET_XYZ=default}")
        );
    }

    @Test
    void expandVariable() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "hello");
        Assertions.assertEquals(String.format("hello%n"), Sh4j.INSTANCE.out(env, "echo $X"));
    }

    @Test
    void expandBracedVariable() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "hello");
        Assertions.assertEquals(
            String.format("hello world%n"),
            Sh4j.INSTANCE.out(env, "echo ${X} world")
        );
    }
}
