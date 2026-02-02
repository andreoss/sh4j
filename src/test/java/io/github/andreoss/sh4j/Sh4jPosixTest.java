/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * POSIX compliance integration tests using the Sh4j helper API.
 * Tests are organised by POSIX section:
 *   - 2.5 Shell Command Language (expansion, quoting, etc.)
 *   - 2.6 Shell Commands (control flow, pipelines, etc.)
 *   - 2.14 Special Built-ins
 *   - 2.5.3 Shell Variables
 *
 * @since 1.0
 */
final class Sh4jPosixTest {

    // ============================================================
    // POSIX 2.5.1 Token Recognition
    // ============================================================

    @Test
    void simpleCommand() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("true"));
    }

    @Test
    void commandWithArguments() {
        Assertions.assertEquals(String.format("a b c%n"), Sh4j.INSTANCE.out("echo a b c"));
    }

    @Test
    void commandWithInlineAssignment() {
        Assertions.assertEquals(String.format("hello%n"), Sh4j.INSTANCE.out("X=hello echo $X"));
    }

    // ============================================================
    // POSIX 2.5.2 Quoting
    // ============================================================

    @Test
    void singleQuotesPreserveLiterals() {
        Assertions.assertEquals(
            String.format("hello * ?%n"),
            Sh4j.INSTANCE.out("echo 'hello * ?'")
        );
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

    // ============================================================
    // POSIX 2.6 Shell Commands - Pipelines
    // ============================================================

    @Test
    void pipelineWithTrue() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("true | true"));
    }

    @Test
    void pipelineWithFalse() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("false | true"));
    }

    @Test
    void pipelineExitCodeIsLast() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("false | true"));
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("true | false"));
    }

    // ============================================================
    // POSIX 2.9.3 Lists - AND/OR
    // ============================================================

    @Test
    void andListShortCircuitsOnFailure() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("false && echo 1"));
        Assertions.assertEquals("", Sh4j.INSTANCE.out("false && echo 1"));
    }

    @Test
    void andListContinuesOnSuccess() {
        Assertions.assertEquals(String.format("yes%n"), Sh4j.INSTANCE.out("true && echo yes"));
    }

    @Test
    void orListShortCircuitsOnSuccess() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("true || echo 1"));
        Assertions.assertEquals("", Sh4j.INSTANCE.out("true || echo 1"));
    }

    @Test
    void orListContinuesOnFailure() {
        Assertions.assertEquals(
            String.format("fallback%n"),
            Sh4j.INSTANCE.out("false || echo fallback")
        );
    }

    // ============================================================
    // POSIX 2.9.4 Compound Commands - if
    // ============================================================

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

    // ============================================================
    // POSIX 2.9.4 Compound Commands - for
    // ============================================================

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

    // ============================================================
    // POSIX 2.9.4 Compound Commands - while/until
    // ============================================================

    @Test
    void whileLoop() {
        Assertions.assertEquals(
            String.format("1%n2%n"),
            Sh4j.INSTANCE.out("i=1; while [ $i -le 2 ]; do echo $i; i=$((i+1)); done")
        );
    }

    @Test
    void untilLoop() {
        Assertions.assertEquals(
            String.format("1%n2%n"),
            Sh4j.INSTANCE.out("i=1; until [ $i -gt 2 ]; do echo $i; i=$((i+1)); done")
        );
    }

    // ============================================================
    // POSIX 2.9.4 Compound Commands - case
    // ============================================================

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
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("case bar in foo) echo matched;; esac")
        );
    }

    // ============================================================
    // POSIX 2.9.5 Function Definition
    // ============================================================

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

    // ============================================================
    // POSIX 2.6.2 Exit Status - set -e
    // ============================================================

    @Test
    void setEExitsOnFailure() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e; false; echo 1"));
    }

    @Test
    void setEDoesNotExitOnOrList() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -e; false || true"));
    }

    @Test
    void andListShortCircuitsNoErrexit() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e; false && true"));
    }

    @Test
    void setEDoesNotExitInIfCondition() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("set -e; if false; then echo yes; fi; echo after")
        );
    }

    @Test
    void setEStopsAcrossSeparateCalls() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e", "false", "echo 1"));
    }

    @Test
    void setEStopsWithoutEchoAcrossSeparateCalls() {
        Assertions.assertEquals("", Sh4j.INSTANCE.out("set -e", "false", "echo 1"));
    }

    @Test
    void setEWithPipelineLastFails() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.run("set -e; true | false"));
    }

    @Test
    void setEWithNonLastPipeStageFailsOk() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -e; false | true"));
    }

    @Test
    void setEWithBangInverts() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -e; ! false"));
    }

    @Test
    void setEWithBangTrueNoExit() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("set -e; ! false; echo after")
        );
    }

    @Test
    void setEWithCompoundAndOrNoExit() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("set -e; false || echo ok"));
    }

    @Test
    void setEWithTrueAndThenOk() {
        Assertions.assertEquals(
            String.format("ok%n"),
            Sh4j.INSTANCE.out("set -e; true && echo ok")
        );
    }

    // ============================================================
    // POSIX 2.6.2 Exit Status - $? special parameter
    // ============================================================

    @Test
    void questionMarkIsLastExitCode() {
        Assertions.assertEquals(String.format("42%n"), Sh4j.INSTANCE.out("(exit 42)", "echo $?"));
    }

    @Test
    void questionMarkUpdatedAfterSuccess() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("false", "true", "echo $?")
        );
    }

    // ============================================================
    // POSIX 2.5.2 Parameter Expansion
    // ============================================================

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

    @Test
    void expandUnsetVariableIsEmpty() {
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("echo $UNSET_VAR_XYZ"));
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
    void plusUseAltIfSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "value");
        Assertions.assertEquals(String.format("alt%n"), Sh4j.INSTANCE.out(env, "echo ${X+alt}"));
    }

    @Test
    void plusEmptyIfUnset() {
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out("echo ${UNSET_XYZ+alt}"));
    }

    @Test
    void equalsAssignDefaultIfUnset() {
        Assertions.assertEquals(
            String.format("default%n"),
            Sh4j.INSTANCE.out("echo ${UNSET_XYZ=default}")
        );
        Assertions.assertEquals(
            "default",
            Sh4j.INSTANCE.getVar("UNSET_XYZ", "echo ${UNSET_XYZ=default}")
        );
    }

    // ============================================================
    // POSIX 2.6.4 Command Substitution
    // ============================================================

    @Test
    void commandSubstitution() {
        Assertions.assertEquals(String.format("hello%n"), Sh4j.INSTANCE.out("echo $(echo hello)"));
    }

    @Test
    void nestedCommandSubstitution() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.out("echo $(echo $(echo hello))")
        );
    }

    @Test
    void backtickCommandSubstitution() {
        Assertions.assertEquals(String.format("hello%n"), Sh4j.INSTANCE.out("echo `echo hello`"));
    }

    // ============================================================
    // POSIX 2.6.5 Arithmetic Expansion
    // ============================================================

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

    // ============================================================
    // POSIX 2.5.3 Special Parameters
    // ============================================================

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

    // ============================================================
    // POSIX 2.14 Special Built-ins - export
    // ============================================================

    @Test
    void exportMakesVariableInEnvironment() {
        Assertions.assertEquals("yes", Sh4j.INSTANCE.getVar("EXPORTED", "export EXPORTED=yes"));
    }

    @Test
    void exportWithValue() {
        final Sh4j.Result result = Sh4j.INSTANCE.runFull("export X=hello", "echo $X");
        Assertions.assertEquals(0, result.exitCode());
        Assertions.assertEquals(String.format("hello%n"), result.stdout());
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - readonly
    // ============================================================

    @Test
    void readonlyPreventsReassignment() {
        Assertions.assertEquals(1, Sh4j.INSTANCE.runFull("readonly R=hello", "R=world").exitCode());
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - unset
    // ============================================================

    @Test
    void unsetRemovesVariable() {
        final Map<String, String> env = new HashMap<>();
        env.put("X", "value");
        Assertions.assertEquals(String.format("%n"), Sh4j.INSTANCE.out(env, "unset X", "echo $X"));
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - cd
    // ============================================================

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

    // ============================================================
    // POSIX 2.14 Special Built-ins - shift
    // ============================================================

    @Test
    void shiftRemovesFirstParam() {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out("set -- a b c; shift; echo $1")
        );
    }

    @Test
    void shiftByN() {
        Assertions.assertEquals(
            String.format("c%n"),
            Sh4j.INSTANCE.out("set -- a b c; shift 2; echo $1")
        );
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - read
    // ============================================================

    @Test
    void readSingleVar() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(String.format("hello%n").getBytes(StandardCharsets.UTF_8)),
                "read VAR",
                "echo $VAR"
            )
        );
    }

    @Test
    void readMultipleVars() {
        Assertions.assertEquals(
            String.format("a b c%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(String.format("a b c%n").getBytes(StandardCharsets.UTF_8)),
                "read X Y Z",
                "echo $X $Y $Z"
            )
        );
    }

    @Test
    void readRawMode() {
        Assertions.assertEquals(
            String.format("a\\b%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(String.format("a\\b%n").getBytes(StandardCharsets.UTF_8)),
                "read -r VAR",
                "echo $VAR"
            )
        );
    }

    @Test
    void readWithReplay() {
        Assertions.assertEquals(
            String.format("hello world%n"),
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(
                    String.format("hello world%n").getBytes(StandardCharsets.UTF_8)
                ),
                "read",
                "echo $REPLY"
            )
        );
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - alias/unalias
    // ============================================================

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
    void unaliasRemovesAlias() {
        Assertions.assertNotEquals(
            0,
            Sh4j.INSTANCE.runFull(
                "alias ll='echo hello'",
                "unalias ll",
                "ll"
            ).exitCode()
        );
    }

    @Test
    void unaliasAll() {
        Assertions.assertNotEquals(
            0,
            Sh4j.INSTANCE.runFull(
                "alias ll='echo hello'",
                "alias la='echo world'",
                "unalias -a",
                "ll"
            ).exitCode()
        );
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - getopts
    // ============================================================

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

    // ============================================================
    // POSIX 2.14 Special Built-ins - jobs/bg/fg
    // ============================================================

    @Test
    void jobsListsBackground() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("true & jobs").contains("[1]"));
    }

    @Test
    void bgConfirmsJob() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("true & bg").contains("[1]"));
    }

    @Test
    void fgWaitsForJob() {
        Assertions.assertTrue(Sh4j.INSTANCE.out("true & fg").contains("true"));
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - fc
    // ============================================================

    @Test
    void fcListsHistory() {
        final String out = Sh4j.INSTANCE.out("echo one", "echo two", "fc -l 1 2");
        Assertions.assertTrue(out.contains("1"));
        Assertions.assertTrue(out.contains("echo one"));
        Assertions.assertTrue(out.contains("2"));
        Assertions.assertTrue(out.contains("echo two"));
    }

    @Test
    void fcSubstitute() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.out(
                "echo hello",
                "fc -s hello=world"
            ).contains("echo world")
        );
    }

    // ============================================================
    // POSIX 2.5.3 Shell Variables - LINENO
    // ============================================================

    @Test
    void linenoReflectsSourceLine() {
        Assertions.assertEquals("1", Sh4j.INSTANCE.getVar("LINENO", "echo $LINENO"));
    }

    // ============================================================
    // POSIX 2.5.3 Shell Variables - LANG/LC_*
    // ============================================================

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
        Assertions.assertEquals(
            "C",
            Sh4j.INSTANCE.getVar("LC_MESSAGES", "LC_MESSAGES=C")
        );
    }

    // ============================================================
    // POSIX 2.6.3 Redirection
    // ============================================================

    @Test
    void outputRedirection() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.out(
                "echo hello > /tmp/sh4j_test.txt",
                "cat /tmp/sh4j_test.txt"
            )
        );
    }

    @Test
    void inputRedirection() {
        Assertions.assertEquals(
            String.format("hello%n"),
            Sh4j.INSTANCE.out(
                "echo hello > /tmp/sh4j_test_in.txt",
                "cat < /tmp/sh4j_test_in.txt"
            )
        );
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

    // ============================================================
    // POSIX 2.9.1 Compound Commands - subshell
    // ============================================================

    @Test
    void subshellDoesNotAffectParent() {
        Assertions.assertEquals(
            String.format("outer%n"),
            Sh4j.INSTANCE.out("X=outer; (X=inner); echo $X")
        );
    }

    @Test
    void subshellCanReadParentVars() {
        Assertions.assertEquals(String.format("outer%n"), Sh4j.INSTANCE.out("X=outer; (echo $X)"));
    }

    // ============================================================
    // POSIX 2.9.1 Compound Commands - grouping
    // ============================================================

    @Test
    void groupingSharesEnvironment() {
        Assertions.assertEquals(
            String.format("inner%n"),
            Sh4j.INSTANCE.out("{ X=inner; }; echo $X")
        );
    }

    // ============================================================
    // POSIX 2.8.1 Word Splitting - IFS
    // ============================================================

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

    // ============================================================
    // POSIX 2.6.2 Exit Status - break/continue
    // ============================================================

    @Test
    void breakExitsLoop() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("for i in 1 2 3; do echo $i; break; echo after; done")
        );
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
}
