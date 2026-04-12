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
 * Tests for {@link Sh4j}: expand unset.
 *
 * @since 1.0
 */
final class Sh4jPosixExpandUnsetTest {

    @Test
    void expandUnsetVariableIsEmpty() {
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("echo $UNSET_VAR_XYZ"));
    }

    @Test
    void exportMakesVariableInEnvironment() {
        Assertions.assertEquals("yes", Sh4j.INSTANCE.getVar("EXPORTED", "export EXPORTED=yes"));
    }

    @Test
    void exportWithValueFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.runFull("export X=hello", "echo $X").exitCode());
    }

    @Test
    void exportWithValueSecond() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.runFull("export X=hello", "echo $X").stdout()
        );
    }

    @Test
    void fcListsHistoryFirst() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("echo one", "echo two", "fc -l 1 2").contains("1"));
    }

    @Test
    void fcListsHistorySecond() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.out("echo one", "echo two", "fc -l 1 2").contains("echo one")
        );
    }

    @Test
    void fcListsHistoryThird() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("echo one", "echo two", "fc -l 1 2").contains("2"));
    }

    @Test
    void fcListsHistoryFourth() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.out("echo one", "echo two", "fc -l 1 2").contains("echo two")
        );
    }

    @Test
    void fcSubstitute() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.out("echo hello", "fc -s hello=world").contains("echo world")
        );
    }

    @Test
    void fgWaitsForJob() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("true & fg").contains("true"));
    }

    @Test
    void forLoopWithList() {
        Assertions.assertEquals(
            String.format("1%n2%n3%n"),
            Sh4j.INSTANCE.out("for i in 1 2 3; do echo $i; done")
        );
    }

    @Test
    void forLoopWithPositionalParams() {
        Assertions.assertEquals(
            String.format("a%nb%nc%n"),
            Sh4j.INSTANCE.out(new HashMap<>(), "set -- a b c; for i; do echo $i; done")
        );
    }

    @Test
    void functionDefinitionAndCall() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.out("greet() { echo hello; }; greet")
        );
    }

    @Test
    void functionWithArguments() {
        Assertions.assertEquals(
            String.format("arg1%n"),
            Sh4j.INSTANCE.out("f() { echo $1; }; f arg1")
        );
    }

    @Test
    void getoptsParsesFlags() {
        Assertions.assertEquals(
            String.format("a%nb val%n"),
            Sh4j.INSTANCE.out(
                "set -- -a -b val",
                "getopts ab: opt; echo $opt",
                "getopts ab: opt; echo $opt $OPTARG"
            )
        );
    }

    @Test
    void getoptsEndsAtNonOption() {
        Assertions.assertEquals(
            String.format("a%n1%n"),
            Sh4j.INSTANCE.out(
                "set -- -a file",
                "getopts a opt; echo $opt",
                "getopts a opt; echo $?"
            )
        );
    }

    @Test
    void groupingSharesEnvironment() {
        Assertions.assertEquals(
            String.format("inner%n"),
            Sh4j.INSTANCE.out("{ X=inner; }; echo $X")
        );
    }

    @Test
    void ifTrueBranch() {
        Assertions.assertEquals(
            String.format("yes%n"),
            Sh4j.INSTANCE.out("if true; then echo yes; fi")
        );
    }

    @Test
    void ifElseBranch() {
        Assertions.assertEquals(
            String.format("no%n"),
            Sh4j.INSTANCE.out("if false; then echo yes; else echo no; fi")
        );
    }

    @Test
    void ifElifBranch() {
        Assertions.assertEquals(
            String.format("elif%n"),
            Sh4j.INSTANCE.out("if false; then echo if; elif true; then echo elif; fi")
        );
    }

    @Test
    void ifsDefaultSplitsOnSpace() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "a b c");
        Assertions.assertEquals(String.format("3%n"), Sh4j.INSTANCE.out(env, "set -- $X; echo $#"));
    }

    @Test
    void ifsCustomDelimiter() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", ":");
        env.put("X", "a:b:c");
        Assertions.assertEquals(String.format("3%n"), Sh4j.INSTANCE.out(env, "set -- $X; echo $#"));
    }

    @Test
    void inputRedirection() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.out("echo hello > /tmp/sh4j_test_in.txt", "cat < /tmp/sh4j_test_in.txt")
        );
    }

    @Test
    void jobsListsBackground() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("true & jobs").contains("[1]"));
    }

    @Test
    void langCanBeSet() {
        Assertions.assertEquals("en_US.UTF-8", Sh4j.INSTANCE.getVar("LANG", "LANG=en_US.UTF-8"));
    }

    @Test
    void lcAllCanBeSet() {
        Assertions.assertEquals("C", Sh4j.INSTANCE.getVar("LC_ALL", "LC_ALL=C"));
    }

    @Test
    void lcCollateCanBeSet() {
        Assertions.assertEquals(
            "en_US.UTF-8",
            Sh4j.INSTANCE.getVar("LC_COLLATE", "LC_COLLATE=en_US.UTF-8")
        );
    }

    @Test
    void lcCtypeCanBeSet() {
        Assertions.assertEquals("UTF-8", Sh4j.INSTANCE.getVar("LC_CTYPE", "LC_CTYPE=UTF-8"));
    }

    @Test
    void lcMessagesCanBeSet() {
        Assertions.assertEquals("C", Sh4j.INSTANCE.getVar("LC_MESSAGES", "LC_MESSAGES=C"));
    }

    @Test
    void linenoReflectsSourceLine() {
        Assertions.assertEquals("1", Sh4j.INSTANCE.getVar("LINENO", "echo $LINENO"));
    }

    @Test
    void nestedCommandSubstitution() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.out("echo $(echo $(echo hello))")
        );
    }

    @Test
    void orListShortCircuitsOnSuccessFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("true || echo 1"));
    }

    @Test
    void orListShortCircuitsOnSuccessSecond() {
        Sh4j.INSTANCE.run("true || echo 1");
        Assertions.assertEquals("", Sh4j.INSTANCE.out("true || echo 1"));
    }

    @Test
    void orListContinuesOnFailure() {
        Assertions.assertEquals(
            String.format("fallback%n"),
            Sh4j.INSTANCE.out("false || echo fallback")
        );
    }

    @Test
    void outputRedirection() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.out("echo hello > /tmp/sh4j_test.txt", "cat /tmp/sh4j_test.txt")
        );
    }

    @Test
    void pipelineWithTrue() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("true | true"));
    }

    @Test
    void pipelineWithFalse() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("false | true"));
    }

    @Test
    void pipelineExitCodeIsLastFirst() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("false | true"));
    }

    @Test
    void pipelineExitCodeIsLastSecond() {
        Sh4j.INSTANCE.run("false | true");
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("true | false"));
    }

    @Test
    void plusUseAltIfSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "value");
        Assertions.assertEquals(String.format("alt%n"), Sh4j.INSTANCE.out(env, "echo ${X+alt}"));
    }
}
