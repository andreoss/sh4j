/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * POSIX compliance integration tests using the Sh4j helper API.
 * Tests are organised by POSIX section:
 *   - 2.5 Shell Command Language (expansion, quoting, etc.)
 *   - 2.6 Shell Commands (control flow, pipelines, etc.)
 *   - 2.14 Special Built-ins
 *   - 2.5.3 Shell Variables
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public final class Sh4jPosixTest {

    // ============================================================
    // POSIX 2.5.1 Token Recognition
    // ============================================================

    @Test
    public void simpleCommand() {
        Assert.assertEquals(0, Sh4j.run("true"));
    }

    @Test
    public void commandWithArguments() {
        Assert.assertEquals("a b c\n", Sh4j.out("echo a b c"));
    }

    @Test
    public void commandWithInlineAssignment() {
        Assert.assertEquals("hello\n", Sh4j.out("X=hello echo $X"));
    }

    // ============================================================
    // POSIX 2.5.2 Quoting
    // ============================================================

    @Test
    public void singleQuotesPreserveLiterals() {
        Assert.assertEquals("hello * ?\n", Sh4j.out("echo 'hello * ?'"));
    }

    @Test
    public void doubleQuotesAllowExpansion() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("HOME", "/home/test");
        Assert.assertEquals("/home/test\n", Sh4j.out(env, "echo \"$HOME\""));
    }

    @Test
    public void backslashEscapesNextChar() {
        Assert.assertEquals("$\n", Sh4j.out("echo \\$"));
    }

    @Test
    public void backslashNewlineContinuation() {
        Assert.assertEquals("hello world\n", Sh4j.out("echo hello \\\nworld"));
    }

    // ============================================================
    // POSIX 2.6 Shell Commands - Pipelines
    // ============================================================

    @Test
    public void pipelineWithTrue() {
        Assert.assertEquals(0, Sh4j.run("true | true"));
    }

    @Test
    public void pipelineWithFalse() {
        Assert.assertEquals(1, Sh4j.run("false | true"));
    }

    @Test
    public void pipelineExitCodeIsLast() {
        Assert.assertEquals(0, Sh4j.run("false | true"));
        Assert.assertEquals(1, Sh4j.run("true | false"));
    }

    // ============================================================
    // POSIX 2.9.3 Lists - AND/OR
    // ============================================================

    @Test
    public void andListShortCircuitsOnFailure() {
        Assert.assertEquals(1, Sh4j.run("false && echo 1"));
        Assert.assertEquals("", Sh4j.out("false && echo 1"));
    }

    @Test
    public void andListContinuesOnSuccess() {
        Assert.assertEquals("yes\n", Sh4j.out("true && echo yes"));
    }

    @Test
    public void orListShortCircuitsOnSuccess() {
        Assert.assertEquals(0, Sh4j.run("true || echo 1"));
        Assert.assertEquals("", Sh4j.out("true || echo 1"));
    }

    @Test
    public void orListContinuesOnFailure() {
        Assert.assertEquals("fallback\n", Sh4j.out("false || echo fallback"));
    }

    // ============================================================
    // POSIX 2.9.4 Compound Commands - if
    // ============================================================

    @Test
    public void ifTrueBranch() {
        Assert.assertEquals("yes\n", Sh4j.out("if true; then echo yes; fi"));
    }

    @Test
    public void ifElseBranch() {
        Assert.assertEquals("no\n", Sh4j.out("if false; then echo yes; else echo no; fi"));
    }

    @Test
    public void ifElifBranch() {
        Assert.assertEquals(
            "elif\n",
            Sh4j.out("if false; then echo if; elif true; then echo elif; fi")
        );
    }

    // ============================================================
    // POSIX 2.9.4 Compound Commands - for
    // ============================================================

    @Test
    public void forLoopWithList() {
        Assert.assertEquals("1\n2\n3\n", Sh4j.out("for i in 1 2 3; do echo $i; done"));
    }

    @Test
    public void forLoopWithPositionalParams() {
        final Map<String, String> env = new HashMap<String, String>();
        Assert.assertEquals(
            "a\nb\nc\n",
            Sh4j.out(env, "set -- a b c; for i; do echo $i; done")
        );
    }

    // ============================================================
    // POSIX 2.9.4 Compound Commands - while/until
    // ============================================================

    @Test
    public void whileLoop() {
        Assert.assertEquals(
            "1\n2\n",
            Sh4j.out("i=1; while [ $i -le 2 ]; do echo $i; i=$((i+1)); done")
        );
    }

    @Test
    public void untilLoop() {
        Assert.assertEquals(
            "1\n2\n",
            Sh4j.out("i=1; until [ $i -gt 2 ]; do echo $i; i=$((i+1)); done")
        );
    }

    // ============================================================
    // POSIX 2.9.4 Compound Commands - case
    // ============================================================

    @Test
    public void casePatternMatch() {
        Assert.assertEquals(
            "matched\n",
            Sh4j.out("case foo in foo) echo matched;; esac")
        );
    }

    @Test
    public void caseWildcardMatch() {
        Assert.assertEquals(
            "wild\n",
            Sh4j.out("case bar in f*) echo foo;; *) echo wild;; esac")
        );
    }

    @Test
    public void caseNoMatch() {
        Assert.assertEquals(
            "",
            Sh4j.out("case bar in foo) echo matched;; esac")
        );
    }

    // ============================================================
    // POSIX 2.9.5 Function Definition
    // ============================================================

    @Test
    public void functionDefinitionAndCall() {
        Assert.assertEquals("hello\n", Sh4j.out("greet() { echo hello; }; greet"));
    }

    @Test
    public void functionWithArguments() {
        Assert.assertEquals(
            "arg1\n",
            Sh4j.out("f() { echo $1; }; f arg1")
        );
    }

    // ============================================================
    // POSIX 2.6.2 Exit Status - set -e
    // ============================================================

    @Test
    public void setEExitsOnFailure() {
        Assert.assertEquals(1, Sh4j.run("set -e; false; echo 1"));
    }

    @Test
    public void setEDoesNotExitOnOrList() {
        Assert.assertEquals(0, Sh4j.run("set -e; false || true"));
    }

    @Test
    public void setEDoesNotExitOnAndList() {
        Assert.assertEquals(0, Sh4j.run("set -e; false && true"));
    }

    @Test
    public void setEDoesNotExitInIfCondition() {
        Assert.assertEquals(
            "after\n",
            Sh4j.out("set -e; if false; then echo yes; fi; echo after")
        );
    }

    // ============================================================
    // POSIX 2.6.2 Exit Status - $? special parameter
    // ============================================================

    @Test
    public void questionMarkIsLastExitCode() {
        Assert.assertEquals("42\n", Sh4j.out("exit 42", "echo $?"));
    }

    @Test
    public void questionMarkUpdatedAfterSuccess() {
        Assert.assertEquals("0\n", Sh4j.out("false", "true", "echo $?"));
    }

    // ============================================================
    // POSIX 2.5.2 Parameter Expansion
    // ============================================================

    @Test
    public void expandVariable() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("X", "hello");
        Assert.assertEquals("hello\n", Sh4j.out(env, "echo $X"));
    }

    @Test
    public void expandBracedVariable() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("X", "hello");
        Assert.assertEquals("hello world\n", Sh4j.out(env, "echo ${X} world"));
    }

    @Test
    public void expandUnsetVariableIsEmpty() {
        Assert.assertEquals("\n", Sh4j.out("echo $UNSET_VAR_XYZ"));
    }

    @Test
    public void dashUseDefaultIfUnset() {
        Assert.assertEquals("default\n", Sh4j.out("echo ${UNSET_XYZ-default}"));
    }

    @Test
    public void dashUseDefaultIfSet() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("X", "value");
        Assert.assertEquals("value\n", Sh4j.out(env, "echo ${X-default}"));
    }

    @Test
    public void colonDashUseDefaultIfUnsetOrNull() {
        Assert.assertEquals("default\n", Sh4j.out("echo ${UNSET_XYZ:-default}"));
    }

    @Test
    public void colonDashUseDefaultIfEmpty() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("X", "");
        Assert.assertEquals("default\n", Sh4j.out(env, "echo ${X:-default}"));
    }

    @Test
    public void plusUseAltIfSet() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("X", "value");
        Assert.assertEquals("alt\n", Sh4j.out(env, "echo ${X+alt}"));
    }

    @Test
    public void plusEmptyIfUnset() {
        Assert.assertEquals("\n", Sh4j.out("echo ${UNSET_XYZ+alt}"));
    }

    @Test
    public void equalsAssignDefaultIfUnset() {
        Assert.assertEquals("default\n", Sh4j.out("echo ${UNSET_XYZ=default}"));
        Assert.assertEquals("default", Sh4j.getVar("UNSET_XYZ", "echo ${UNSET_XYZ=default}"));
    }

    // ============================================================
    // POSIX 2.6.4 Command Substitution
    // ============================================================

    @Test
    public void commandSubstitution() {
        Assert.assertEquals("hello\n", Sh4j.out("echo $(echo hello)"));
    }

    @Test
    public void nestedCommandSubstitution() {
        Assert.assertEquals("hello\n", Sh4j.out("echo $(echo $(echo hello))"));
    }

    @Test
    public void backtickCommandSubstitution() {
        Assert.assertEquals("hello\n", Sh4j.out("echo `echo hello`"));
    }

    // ============================================================
    // POSIX 2.6.5 Arithmetic Expansion
    // ============================================================

    @Test
    public void arithmeticAddition() {
        Assert.assertEquals("7\n", Sh4j.out("echo $((3 + 4))"));
    }

    @Test
    public void arithmeticMultiplication() {
        Assert.assertEquals("12\n", Sh4j.out("echo $((3 * 4))"));
    }

    @Test
    public void arithmeticWithVariables() {
        Assert.assertEquals("10\n", Sh4j.out("x=3; y=7; echo $((x + y))"));
    }

    @Test
    public void arithmeticNested() {
        Assert.assertEquals("14\n", Sh4j.out("echo $((2 * (3 + 4)))"));
    }

    // ============================================================
    // POSIX 2.5.3 Special Parameters
    // ============================================================

    @Test
    public void dollarZeroIsScriptName() {
        Assert.assertEquals("sh\n", Sh4j.out("echo $0"));
    }

    @Test
    public void dollarHashIsParamCount() {
        Assert.assertEquals("3\n", Sh4j.out("set -- a b c; echo $#"));
    }

    @Test
    public void dollarAtIsAllParams() {
        Assert.assertEquals("a b c\n", Sh4j.out("set -- a b c; echo $@"));
    }

    @Test
    public void dollarStarIsAllParams() {
        Assert.assertEquals("a b c\n", Sh4j.out("set -- a b c; echo $*"));
    }

    @Test
    public void dollarDollarIsCurrentPid() {
        final String out = Sh4j.out("echo $$");
        Assert.assertTrue(out.trim().matches("\\d+"));
    }

    @Test
    public void dollarBangIsLastBgPid() {
        final String out = Sh4j.out("sleep 0 & echo $!");
        Assert.assertTrue(out.trim().matches("\\d+"));
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - export
    // ============================================================

    @Test
    public void exportMakesVariableInEnvironment() {
        Assert.assertEquals("yes", Sh4j.getVar("EXPORTED", "export EXPORTED=yes"));
    }

    @Test
    public void exportWithValue() {
        final Sh4j.Result result = Sh4j.runFull("export X=hello", "echo $X");
        Assert.assertEquals(0, result.exitCode());
        Assert.assertEquals("hello\n", result.stdout());
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - readonly
    // ============================================================

    @Test
    public void readonlyPreventsReassignment() {
        final Sh4j.Result result = Sh4j.runFull("readonly R=hello", "R=world");
        Assert.assertEquals(1, result.exitCode());
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - unset
    // ============================================================

    @Test
    public void unsetRemovesVariable() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("X", "value");
        Assert.assertEquals("", Sh4j.out(env, "unset X", "echo $X"));
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - cd
    // ============================================================

    @Test
    public void cdChangesDirectory() {
        final String out = Sh4j.out("cd /tmp", "pwd");
        Assert.assertTrue(out.trim().startsWith("/tmp"));
    }

    @Test
    public void cdDashReturnsToOldPwd() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("PWD", "/tmp");
        env.put("OLDPWD", "/home");
        final String out = Sh4j.out(env, "cd -");
        Assert.assertTrue(out.contains("/home"));
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - shift
    // ============================================================

    @Test
    public void shiftRemovesFirstParam() {
        Assert.assertEquals("b\n", Sh4j.out("set -- a b c; shift; echo $1"));
    }

    @Test
    public void shiftByN() {
        Assert.assertEquals("c\n", Sh4j.out("set -- a b c; shift 2; echo $1"));
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - read
    // ============================================================

    @Test
    public void readSingleVar() {
        final String input = "hello\n";
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            "read VAR",
            "echo $VAR"
        );
        Assert.assertEquals("hello\n", result.stdout());
    }

    @Test
    public void readMultipleVars() {
        final String input = "a b c\n";
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            "read X Y Z",
            "echo $X $Y $Z"
        );
        Assert.assertEquals("a b c\n", result.stdout());
    }

    @Test
    public void readRawMode() {
        final String input = "a\\b\n";
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            "read -r VAR",
            "echo $VAR"
        );
        Assert.assertEquals("a\\b\n", result.stdout());
    }

    @Test
    public void readWithReplay() {
        final String input = "hello world\n";
        final Sh4j.Result result = Sh4j.runFull(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            "read",
            "echo $REPLY"
        );
        Assert.assertEquals("hello world\n", result.stdout());
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - alias/unalias
    // ============================================================

    @Test
    public void aliasExpands() {
        Assert.assertEquals("hello\n", Sh4j.out("alias ll='echo hello'", "ll"));
    }

    @Test
    public void aliasWithArguments() {
        Assert.assertEquals("hello world\n", Sh4j.out("alias ll='echo'", "ll hello world"));
    }

    @Test
    public void unaliasRemovesAlias() {
        final Sh4j.Result result = Sh4j.runFull(
            "alias ll='echo hello'",
            "unalias ll",
            "ll"
        );
        Assert.assertNotEquals(0, result.exitCode());
    }

    @Test
    public void unaliasAll() {
        final Sh4j.Result result = Sh4j.runFull(
            "alias ll='echo hello'",
            "alias la='echo world'",
            "unalias -a",
            "ll"
        );
        Assert.assertNotEquals(0, result.exitCode());
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - getopts
    // ============================================================

    @Test
    public void getoptsParsesFlags() {
        final Sh4j.Result result = Sh4j.runFull(
            "set -- -a -b val",
            "getopts ab: opt; echo $opt",
            "getopts ab: opt; echo $opt $OPTARG"
        );
        Assert.assertEquals("a\nb val\n", result.stdout());
    }

    @Test
    public void getoptsEndsAtNonOption() {
        final Sh4j.Result result = Sh4j.runFull(
            "set -- -a file",
            "getopts a opt; echo $opt",
            "getopts a opt; echo $?"
        );
        Assert.assertEquals("a\n1\n", result.stdout());
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - jobs/bg/fg
    // ============================================================

    @Test
    public void jobsListsBackground() {
        final String out = Sh4j.out("true & jobs");
        Assert.assertTrue(out.contains("[1]"));
    }

    @Test
    public void bgConfirmsJob() {
        final String out = Sh4j.out("true & bg");
        Assert.assertTrue(out.contains("[1]"));
    }

    @Test
    public void fgWaitsForJob() {
        final String out = Sh4j.out("true & fg");
        Assert.assertTrue(out.contains("true"));
    }

    // ============================================================
    // POSIX 2.14 Special Built-ins - fc
    // ============================================================

    @Test
    public void fcListsHistory() {
        final String out = Sh4j.out("echo one", "echo two", "fc -l 1 2");
        Assert.assertTrue(out.contains("1"));
        Assert.assertTrue(out.contains("echo one"));
        Assert.assertTrue(out.contains("2"));
        Assert.assertTrue(out.contains("echo two"));
    }

    @Test
    public void fcSubstitute() {
        final String out = Sh4j.out("echo hello", "fc -s hello=world");
        Assert.assertTrue(out.contains("echo world"));
    }

    // ============================================================
    // POSIX 2.5.3 Shell Variables - LINENO
    // ============================================================

    @Test
    public void linenoReflectsSourceLine() {
        Assert.assertEquals("1", Sh4j.getVar("LINENO", "echo $LINENO"));
    }

    // ============================================================
    // POSIX 2.5.3 Shell Variables - LANG/LC_*
    // ============================================================

    @Test
    public void langCanBeSet() {
        Assert.assertEquals("en_US.UTF-8", Sh4j.getVar("LANG", "LANG=en_US.UTF-8"));
    }

    @Test
    public void lcAllCanBeSet() {
        Assert.assertEquals("C", Sh4j.getVar("LC_ALL", "LC_ALL=C"));
    }

    @Test
    public void lcCollateCanBeSet() {
        Assert.assertEquals(
            "en_US.UTF-8",
            Sh4j.getVar("LC_COLLATE", "LC_COLLATE=en_US.UTF-8")
        );
    }

    @Test
    public void lcCtypeCanBeSet() {
        Assert.assertEquals("UTF-8", Sh4j.getVar("LC_CTYPE", "LC_CTYPE=UTF-8"));
    }

    @Test
    public void lcMessagesCanBeSet() {
        Assert.assertEquals(
            "C",
            Sh4j.getVar("LC_MESSAGES", "LC_MESSAGES=C")
        );
    }

    // ============================================================
    // POSIX 2.6.3 Redirection
    // ============================================================

    @Test
    public void outputRedirection() {
        final Sh4j.Result result = Sh4j.runFull("echo hello > /tmp/sh4j_test.txt", "cat /tmp/sh4j_test.txt");
        Assert.assertEquals("hello\n", result.stdout());
    }

    @Test
    public void inputRedirection() {
        final Sh4j.Result result = Sh4j.runFull(
            "echo hello > /tmp/sh4j_test_in.txt",
            "cat < /tmp/sh4j_test_in.txt"
        );
        Assert.assertEquals("hello\n", result.stdout());
    }

    @Test
    public void appendRedirection() {
        final Sh4j.Result result = Sh4j.runFull(
            "echo hello > /tmp/sh4j_test_app.txt",
            "echo world >> /tmp/sh4j_test_app.txt",
            "cat /tmp/sh4j_test_app.txt"
        );
        Assert.assertEquals("hello\nworld\n", result.stdout());
    }

    // ============================================================
    // POSIX 2.9.1 Compound Commands - subshell
    // ============================================================

    @Test
    public void subshellDoesNotAffectParent() {
        final String out = Sh4j.out("X=outer; (X=inner); echo $X");
        Assert.assertEquals("outer\n", out);
    }

    @Test
    public void subshellCanReadParentVars() {
        final String out = Sh4j.out("X=outer; (echo $X)");
        Assert.assertEquals("outer\n", out);
    }

    // ============================================================
    // POSIX 2.9.1 Compound Commands - grouping
    // ============================================================

    @Test
    public void groupingSharesEnvironment() {
        final String out = Sh4j.out("{ X=inner; }; echo $X");
        Assert.assertEquals("inner\n", out);
    }

    // ============================================================
    // POSIX 2.8.1 Word Splitting - IFS
    // ============================================================

    @Test
    public void ifsDefaultSplitsOnSpace() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("X", "a b c");
        Assert.assertEquals("3\n", Sh4j.out(env, "set -- $X; echo $#"));
    }

    @Test
    public void ifsCustomDelimiter() {
        final Map<String, String> env = new HashMap<String, String>();
        env.put("IFS", ":");
        env.put("X", "a:b:c");
        Assert.assertEquals("3\n", Sh4j.out(env, "set -- $X; echo $#"));
    }

    // ============================================================
    // POSIX 2.6.2 Exit Status - break/continue
    // ============================================================

    @Test
    public void breakExitsLoop() {
        final String out = Sh4j.out("for i in 1 2 3; do echo $i; break; echo after; done");
        Assert.assertEquals("1\n", out);
    }

    @Test
    public void continueSkipsRestOfLoop() {
        final String out = Sh4j.out("for i in 1 2 3; do if [ $i -eq 2 ]; then continue; fi; echo $i; done");
        Assert.assertEquals("1\n3\n", out);
    }
}
