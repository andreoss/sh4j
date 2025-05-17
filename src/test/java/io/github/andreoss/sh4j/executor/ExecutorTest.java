/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import io.github.andreoss.sh4j.parser.Subshell;
import io.github.andreoss.sh4j.parser.IfNode;
import io.github.andreoss.sh4j.parser.ForNode;
import io.github.andreoss.sh4j.parser.WhileNode;
import io.github.andreoss.sh4j.parser.CaseNode;
import io.github.andreoss.sh4j.parser.FunctionNode;

class ExecutorTest {
    private Parser parser(String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    @Test
    void executeSimpleEcho() {
        final ListNode list = this.parser("echo hello").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executePwd() {
        final ListNode list = this.parser("pwd").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeTrue() {
        final ListNode list = this.parser("true").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFalse() {
        final ListNode list = this.parser("false").parse();
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeWithArguments() {
        final ListNode list = this.parser("echo a b c").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSemicolonSeparator() {
        final ListNode list = this.parser("echo a; echo b").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAndIf() {
        final ListNode list = this.parser("echo a && echo b").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeOrIf() {
        final ListNode list = this.parser("false || echo b").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAndIfStopsOnFailure() {
        final ListNode list = this.parser("false && echo not reached").parse();
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeOrIfStopsOnSuccess() {
        final ListNode list = this.parser("echo success || echo not reached").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeEmptyList() {
        final ListNode list = this.parser("").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAssignmentOnly() {
        final ListNode list = this.parser("FOO=bar").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void executeAssignmentWithCommand() {
        final ListNode list = this.parser("FOO=hello echo $FOO").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeMultipleAssignments() {
        final ListNode list = this.parser("A=1 B=2").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        assertEquals("1", executor.getVariable("A"));
        assertEquals("2", executor.getVariable("B"));
    }

    @Test
    void executeVariableExpansion() {
        final ListNode list = this.parser("echo $HOME").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeOutputRedirection() throws Exception {
        final ListNode list = this.parser("echo test > /tmp/sh4j_test_output").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeInputRedirection() throws Exception {
        final ListNode list = this.parser("cat < /dev/null").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executePipeline() {
        final ListNode list = this.parser("echo hello | cat").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBackground() {
        final ListNode list = this.parser("echo hello &").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBackgroundSetsLastBgPid() {
        final ListNode list = this.parser("echo hello &").parse();
        final Executor executor = new Executor();
        executor.execute(list);
        assertTrue(executor.getLastBgPid() > 0, "lastBgPid should be > 0 after & command");
    }

    @Test
    void executeBackgroundAndSequential() {
        final ListNode list = this.parser("echo bg1 & echo seq1").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeClobberRedirection() throws Exception {
        final ListNode list = this.parser("echo test >| /tmp/sh4j_clobber_test").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrRedirect() throws Exception {
        final ListNode list = this.parser("echo hello 2>/dev/null").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrToStdout() throws Exception {
        final ListNode list = this.parser("echo hello 2>&1").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrRedirectToFile() throws Exception {
        final String file = "/tmp/sh4j_stderr_test";
        final ListNode list = this.parser("echo hello 2>" + file).parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrAppendToFile() throws Exception {
        final String file = "/tmp/sh4j_stderr_append_" + System.nanoTime();
        final ListNode list = this.parser("echo hello 2>>" + file).parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutAppendToFile() throws Exception {
        final String file = "/tmp/sh4j_stdout_append_" + System.nanoTime();
        final ListNode list = this.parser("echo hello >>" + file).parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutRedirectIOException() throws Exception {
        final ListNode list = this.parser("echo hello >/dev/null/no_such_path_xyz").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrRedirectIOException() throws Exception {
        final ListNode list = this.parser("echo hello 2>/dev/null/no_such_stderr_xyz").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStdoutAppendIOException() throws Exception {
        final ListNode list = this.parser("echo hello >>/dev/null/no_such_append_xyz").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrAppendIOException() throws Exception {
        final ListNode list = this.parser("echo hello 2>>/dev/null/no_such_eappend_xyz").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinCloseStdout() {
        final ListNode list = this.parser("echo hello >&-").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeMultiplePipelines() {
        final ListNode list = this.parser("echo a; echo b; echo c").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBangPipeline() {
        final ListNode list = this.parser("! false").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeVariableWithEmptyValue() {
        final ListNode list = this.parser("echo $EMPTY_VAR").parse();
        final Map<String, String> env = new HashMap<>();
        env.put("EMPTY_VAR", "");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCommandNotFound() {
        final ListNode list = this.parser("nonexistent_command_xyz").parse();
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void executeCommandNotFoundReturns127() {
        final ListNode list = this.parser("nonexistent_cmd_xyz_127").parse();
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void executeBangWithTrue() {
        final ListNode list = this.parser("! true").parse();
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeExitZero() {
        final ListNode list = this.parser("exit 0").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeExitOne() {
        final ListNode list = this.parser("exit 1").parse();
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeEchoAbc() {
        final ListNode list = this.parser("echo abc").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeEmptyListNode() {
        final ListNode list = new ListNode();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executePipelineWithNoCommands() {
        final ListNode list = new ListNode();
        list.addPipeline(new Pipeline(false), ListNode.Separator.NONE);
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAppendRedirection() throws Exception {
        final ListNode list = this.parser("echo test >> /tmp/sh4j_append_test").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeStderrRedirect() throws Exception {
        final ListNode list = this.parser("echo test 2>/tmp/sh4j_test_stderr").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeStderrToStdout() throws Exception {
        final ListNode list = this.parser("echo test 2>&1").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCloseStdin() throws Exception {
        final ListNode list = this.parser("cat <&-").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCloseStdout() throws Exception {
        final ListNode list = this.parser("echo test >&-").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeReadWriteRedirect() throws Exception {
        final ListNode list = this.parser("cat <> /dev/null").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeExitNoArgs() {
        final ListNode list = this.parser("exit").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeExitWithNonNumericArg() {
        final ListNode list = this.parser("exit xyz").parse();
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executePipelineWithNullCommand() {
        final Pipeline pipeline = new Pipeline(false);
        pipeline.getCommands().add(null);
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCommandWithNullName() {
        final SimpleCommand cmd = new SimpleCommand(null);
        final Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(cmd);
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAssignmentWithNoEquals() {
        final SimpleCommand cmd = new SimpleCommand("echo");
        cmd.getAssignments().add("INVALID");
        final Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(cmd);
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executePipelineWithFailingCommand() {
        final ListNode list = this.parser("echo hello | nonexistent_cmd_xyz_fail | echo done").parse();
        final Executor executor = new Executor();
        final int code = executor.execute(list);
        assertTrue(code == 127 || code == 126,
                "Pipeline with missing middle stage must exit with POSIX "
                        + "command-not-found(127) or not-executable(126); got " + code);
    }

    @Test
    void executeSubshell() {
        final ListNode list = this.parser("(echo hello)").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSubshellWithMultiplePipelines() {
        final ListNode list = this.parser("(echo a; echo b)").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSubshellInPipeline() {
        final ListNode list = this.parser("echo foo | (cat) | wc").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSubshellWithExit() {
        final ListNode list = this.parser("(exit 42)").parse();
        final Executor executor = new Executor();
        assertEquals(42, executor.execute(list));
    }

    @Test
    void executeGrouping() {
        final ListNode list = this.parser("{ echo hello; }").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeGroupingInPipeline() {
        final ListNode list = this.parser("echo foo | { cat; } | wc").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeGroupingSharesEnvironment() {
        final ListNode list = this.parser("FOO=bar; { echo $FOO; }").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeIfTrueThen() {
        final ListNode list = this.parser("if true; then echo hi; fi").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeIfFalseElse() {
        final ListNode list = this.parser("if false; then echo a; else echo b; fi").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeIfTrueThenElseNotExecuted() {
        final ListNode list = this.parser("if true; then echo hi; else false; fi").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeForDoDone() {
        final ListNode list = this.parser("for var in a b c; do echo $var; done").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeWhileFalse() {
        final ListNode list = this.parser("while false; do echo hi; done").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeUntilTrue() {
        final ListNode list = this.parser("until true; do echo hi; done").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseMatch() {
        final ListNode list = this.parser("case a in a) echo match;; esac").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseNoMatch() {
        final ListNode list = this.parser("case a in b) echo nomatch;; esac").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseDefaultStar() {
        final ListNode list = this.parser("case a in *) echo default;; esac").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCasePipePattern() {
        final ListNode list = this.parser("case a in b|c|a) echo match;; esac").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseFirstMatchOnly() {
        final ListNode list = this.parser("case a in a) echo first;; b) echo second;; esac").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFunctionDefinitionStored() {
        final ListNode list = this.parser("foo() { echo hello; }").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFunctionCall() {
        final ListNode list = this.parser("foo() { echo hello; }; foo").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFunctionSubshellBody() {
        final ListNode list = this.parser("foo() (echo hello); foo").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeWhileLoop() {
        final ListNode list = this.parser("while false; do echo body; done").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeUntilLoop() {
        final ListNode list = this.parser("until true; do echo body; done").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeWhileBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parser("while exit $?; do echo ran; exit 1; done").parse();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(1, executor.execute(list));
        assertEquals("ran\n", bos.toString());
    }

    @Test
    void executeUntilBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parser("exit 1; until exit $?; do echo ran; exit 0; done").parse();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("ran\n", bos.toString());
    }

    @Test
    void executeWhileBodyExitCodePropagated() {
        final ListNode list = this.parser("while exit $?; do exit 42; done").parse();
        final Executor executor = new Executor();
        assertEquals(42, executor.execute(list));
    }

    @Test
    void testCommandInterruptedException() {
        final ListNode list = this.parser("pwd").parse();
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void testPipelineInterruptedException() {
        final ListNode list = this.parser("pwd | cat").parse();
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void waitForBackgroundJobsInterrupted() {
        final ListNode list = this.parser("sleep 1 &").parse();
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        executor.execute(list);
        assertTrue(Thread.interrupted(), "InterruptedException catch should re-set interrupt flag");
    }

    @Test
    void testExportNoArgs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_EXPORT_VAR", "hello");
        final Executor executor = new Executor(env);
        executor.setOutputStream(new PrintStream(bos, true));
        final ListNode list = this.parser("export").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().contains("export TEST_EXPORT_VAR=hello"));
    }

    @Test
    void executeIfFalseNoElse() {
        final ListNode list = this.parser("if false; then echo yes; fi").parse();
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeElifWhenIfFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parser("if false; then echo no; elif true; then echo yes; fi").parse();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("yes\n", bos.toString());
    }

    @Test
    void executeElifChainFalseFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parser("if false; then echo a; elif false; then echo b; else echo c; fi").parse();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("c\n", bos.toString());
    }

    @Test
    void executeElifChainSecondElifTrue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parser("if false; then echo a; elif false; then echo b; elif true; then echo c; fi").parse();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("c\n", bos.toString());
    }

    @Test
    void executeRedirectStdoutToStderr() {
        final ListNode list = this.parser("echo test 1>&2").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeHereDoc() {
        final ListNode list = this.parser("cat << EOF\nhello\nEOF\n").parse();
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void testHeredocExpansion() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        final ListNode list = this.parser("cat << EOF\nhello $TEST_HVAR\nEOF\n").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("hello expanded", bos.toString().trim());
    }

    @Test
    void testRedirectTargetExpansionOutput() throws Exception {
        final java.io.File f = new java.io.File("/tmp/sh4j_redir_expand_test.txt");
        f.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        final Executor executor = new Executor(env);
        final ListNode list = this.parser("echo hello > $TEST_FILE").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(f.exists());
        assertEquals("hello\n", new String(java.nio.file.Files.readAllBytes(f.toPath())));
        f.delete();
    }

    @Test
    void testRedirectTargetExpansionAppend() throws Exception {
        final java.io.File f = new java.io.File("/tmp/sh4j_redir_append_expand.txt");
        f.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        final Executor executor = new Executor(env);
        final ListNode list = this.parser("echo hello >> $TEST_FILE").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(f.exists());
        assertEquals("hello\n", new String(java.nio.file.Files.readAllBytes(f.toPath())));
        f.delete();
    }

    @Test
    void testRedirectTargetExpansionInput() throws Exception {
        final java.io.File f = new java.io.File("/tmp/sh4j_redir_input_expand.txt");
        f.delete();
        try (java.io.FileWriter w = new java.io.FileWriter(f)) {
            w.write("hello from file");
        }
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_input_expand.txt");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        final ListNode list = this.parser("cat < $TEST_FILE").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("hello from file", bos.toString().trim());
        f.delete();
    }

    @Test
    void executeNonExecutableCommand() throws Exception {
        final java.io.File f = new java.io.File("/tmp/sh4j_nonexec_test.sh");
        f.createNewFile();
        f.setExecutable(false);
        final ListNode list = this.parser(f.getAbsolutePath()).parse();
        final Executor executor = new Executor();
        assertEquals(126, executor.execute(list));
    }

    @Test
    void expandDollarQuestion() {
        final Executor executor = new Executor();
        final ListNode list = this.parser("false").parse();
        executor.execute(list);
        assertEquals("1", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionAfterSuccess() {
        final Executor executor = new Executor();
        final ListNode list = this.parser("true").parse();
        executor.execute(list);
        assertEquals("0", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarHyphen() {
        final Executor executor = new Executor();
        assertEquals("", executor.expandVariables("$-", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionBraced() {
        final Executor executor = new Executor();
        final ListNode list = this.parser("false").parse();
        executor.execute(list);
        assertEquals("1", executor.expandVariables("${?}", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionInString() {
        final Executor executor = new Executor();
        final ListNode list = this.parser("false").parse();
        executor.execute(list);
        assertEquals("exit:1", executor.expandVariables("exit:$?", new HashMap<>()));
    }

    @Test
    void expandDollarHash() {
        final Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c"));
        assertEquals("3", executor.expandVariables("$#", new HashMap<>()));
    }

    @Test
    void expandDollarAt() {
        final Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c"));
        assertEquals("a b c", executor.expandVariables("$@", new HashMap<>()));
    }

    @Test
    void expandDollarStar() {
        final Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c"));
        assertEquals("a b c", executor.expandVariables("$*", new HashMap<>()));
    }

    @Test
    void expandDollarZero() {
        final Executor executor = new Executor(new HashMap<>(), List.of("my_script"));
        assertEquals("my_script", executor.expandVariables("$0", new HashMap<>()));
    }

    @Test
    void expandDollarOne() {
        final Executor executor = new Executor(new HashMap<>(), List.of("sh", "first", "second"));
        assertEquals("first", executor.expandVariables("$1", new HashMap<>()));
    }

    @Test
    void expandDollarNine() {
        final Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i"));
        assertEquals("i", executor.expandVariables("$9", new HashMap<>()));
    }

    @Test
    void expandBracedMultiDigit() {
        final Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
        assertEquals("j", executor.expandVariables("${10}", new HashMap<>()));
    }

    @Test
    void expandDollarHashNone() {
        final Executor executor = new Executor();
        assertEquals("0", executor.expandVariables("$#", new HashMap<>()));
    }

    @Test
    void expandDollarAtEmpty() {
        final Executor executor = new Executor();
        assertEquals("", executor.expandVariables("$@", new HashMap<>()));
    }

    @Test
    void expandDollarDollar() {
        final Executor executor = new Executor();
        final String pid = executor.expandVariables("$$", new HashMap<>());
        assertNotNull(pid);
        assertFalse(pid.isEmpty());
        assertEquals(String.valueOf(ProcessHandle.current().pid()), pid);
    }

    @Test
    void expandDollarBang() {
        final Executor executor = new Executor();
        assertEquals("0", executor.expandVariables("$!", new HashMap<>()));
    }

    @Test
    void expandDollarBangBraced() {
        final Executor executor = new Executor();
        assertEquals("0", executor.expandVariables("${!}", new HashMap<>()));
    }

    @Test
    void expandTildeToHome() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/testuser");
        assertEquals("/home/testuser", WordExpander.expandTilde("~", env));
    }

    @Test
    void expandTildeWithPath() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/testuser");
        assertEquals("/home/testuser/file.txt", WordExpander.expandTilde("~/file.txt", env));
    }

    @Test
    void expandTildePlus() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/current/dir");
        assertEquals("/current/dir", WordExpander.expandTilde("~+", env));
    }

    @Test
    void expandTildeNone() {
        assertEquals("/foo/bar", WordExpander.expandTilde("/foo/bar", new HashMap<>()));
    }

    @Test
    void expandVarDefault() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("default", executor.expandVariables("${var:-default}", env));
    }

    @Test
    void expandVarDefaultSet() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "actual");
        assertEquals("actual", executor.expandVariables("${var:-default}", env));
    }

    @Test
    void expandVarDefaultEmpty() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "");
        assertEquals("default", executor.expandVariables("${var:-default}", env));
    }

    @Test
    void expandVarDefaultNullOnly() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("default", executor.expandVariables("${var-default}", env));
    }

    @Test
    void expandVarDefaultNullOnlySetSkips() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "value");
        assertEquals("value", executor.expandVariables("${var-default}", env));
    }

    @Test
    void expandVarDefaultNullOnlyEmptySkips() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "");
        assertEquals("", executor.expandVariables("${var-default}", env));
    }

    @Test
    void expandVarAssignDefault() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("assigned", executor.expandVariables("${var:=assigned}", env));
        assertEquals("assigned", env.get("var"));
    }

    @Test
    void expandVarAlternate() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "x");
        assertEquals("alt", executor.expandVariables("${var:+alt}", env));
    }

    @Test
    void expandVarAlternateUnset() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("", executor.expandVariables("${var:+alt}", env));
    }

    @Test
    void expandVarAssignNullOnly() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("assigned", executor.expandVariables("${var=assigned}", env));
        assertEquals("assigned", env.get("var"));
    }

    @Test
    void expandVarAssignNullOnlySetSkips() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "existing");
        assertEquals("existing", executor.expandVariables("${var=assigned}", env));
        assertEquals("existing", env.get("var"));
    }

    @Test
    void expandVarAlternateNullOnly() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "x");
        assertEquals("alt", executor.expandVariables("${var+alt}", env));
    }

    @Test
    void expandVarAlternateNullOnlyUnset() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("", executor.expandVariables("${var+alt}", env));
    }

    @Test
    void expandVarLength() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "hello");
        assertEquals("5", executor.expandVariables("${#var}", env));
    }

    @Test
    void expandVarLengthUnset() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("0", executor.expandVariables("${#var}", env));
    }

    @Test
    void expandVarSuffixShort() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "file.txt");
        assertEquals("file", executor.expandVariables("${var%.*}", env));
    }

    @Test
    void expandVarSuffixLong() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "a.b.c.txt");
        assertEquals("a", executor.expandVariables("${var%%.*}", env));
    }

    @Test
    void expandVarPrefixShort() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "foo/bar/baz");
        assertEquals("bar/baz", executor.expandVariables("${var#*/}", env));
    }

    @Test
    void expandVarPrefixLong() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "foo/bar/baz");
        assertEquals("baz", executor.expandVariables("${var##*/}", env));
    }

    @Test
    void expandVarPatternNoMatch() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("var", "hello");
        assertEquals("hello", executor.expandVariables("${var%x}", env));
    }

    @Test
    void commandSubstitutionEcho() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("hello", executor.expandVariables("$(echo hello)", env));
    }

    @Test
    void commandSubstitutionInString() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("msg=hi", executor.expandVariables("msg=$(echo hi)", env));
    }

    @Test
    void backtickSubstitution() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("hello", executor.expandVariables("`echo hello`", env));
    }

    @Test
    void backtickSubstitutionInString() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("msg=hi", executor.expandVariables("msg=`echo hi`", env));
    }

    @Test
    void normaliseBackticksSimple() {
        assertEquals("$(echo hello)", WordExpander.normaliseBackticks("`echo hello`"));
    }

    @Test
    void normaliseBackticksNoChange() {
        assertEquals("hello world", WordExpander.normaliseBackticks("hello world"));
    }

    @Test
    void normaliseBackticksSkipsDollarParen() {
        assertEquals("$(echo hi)", WordExpander.normaliseBackticks("$(echo hi)"));
    }

    @Test
    void normaliseBackticksEscaped() {
        assertEquals("$(echo `hi`)", WordExpander.normaliseBackticks("`echo \\`hi\\``"));
    }

    @Test
    void normaliseBackticksDollarEscape() {
        assertEquals("$(echo $PATH)", WordExpander.normaliseBackticks("`echo \\$PATH`"));
    }

    @Test
    void normaliseBackticksBackslashEscape() {
        assertEquals("$(echo a\\b)", WordExpander.normaliseBackticks("`echo a\\\\b`"));
    }

    @Test
    void normaliseBackticksNonSpecialEscape() {
        assertEquals("$(echo \\n)", WordExpander.normaliseBackticks("`echo \\\\n`"));
    }

    @Test
    void normaliseBackticksUnclosed() {
        assertEquals("`echo hello", WordExpander.normaliseBackticks("`echo hello"));
    }

    @Test
    void normaliseBackticksSkipsDollarBrace() {
        assertEquals("${var:-default}", WordExpander.normaliseBackticks("${var:-default}"));
    }

    @Test
    void normaliseBackticksSkipsNestedDollarBrace() {
        assertEquals("${var:-${other}}", WordExpander.normaliseBackticks("${var:-${other}}"));
    }

    @Test
    void normaliseBackticksSkipsTripleNestedDollarBrace() {
        assertEquals("${a:-${b:-${c}}}", WordExpander.normaliseBackticks("${a:-${b:-${c}}}"));
    }

    @Test
    void normaliseBackticksDollarAtEnd() {
        assertEquals("foo$", WordExpander.normaliseBackticks("foo$"));
    }

    @Test
    void normaliseBackticksUnmatchedDollarBrace() {
        assertEquals("${unclosed", WordExpander.normaliseBackticks("${unclosed"));
    }

    @Test
    void normaliseBackticksDollarFollowedByLetter() {
        assertEquals("$VAR", WordExpander.normaliseBackticks("$VAR"));
    }

    @Test
    void normaliseBackticksBackslashAtEndOfInput() {
        assertEquals("`abc\\", WordExpander.normaliseBackticks("`abc\\"));
    }

    @Test
    void globMatchesQuestionMarkInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parser("case ab in a?) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("MATCHED"));
    }

    @Test
    void globMatchesQuestionMarkNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parser("case abc in a?) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parser("case b in [abc]) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("MATCHED"));
    }

    @Test
    void globMatchesCharClassNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parser("case z in [abc]) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassRangeInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parser("case m in [a-z]) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("MATCHED"));
    }

    @Test
    void globMatchesUnclosedBracketInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parser("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("MATCHED"));
    }

    @Test
    void backtickNestedNormalise() {
        assertEquals("$(echo `echo hi`)", WordExpander.normaliseBackticks("`echo \\`echo hi\\``"));
    }

    @Test
    void echoBuiltin() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("echo hello").parse()));
    }

    @Test
    void echoBuiltinArgs() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("echo hello world").parse()));
    }

    @Test
    void echoBuiltinNoNewline() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("echo -n hello").parse()));
    }

    @Test
    void echoBuiltinEmpty() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("echo").parse()));
    }

    @Test
    void arithmeticSimple() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("7", executor.expandVariables("$((3 + 4))", env));
    }

    @Test
    void arithmeticMul() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("15", executor.expandVariables("$((3 * 5))", env));
    }

    @Test
    void arithmeticVar() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        assertEquals("15", executor.expandVariables("$((x + 5))", env));
    }

    @Test
    void arithmeticNested() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("14", executor.expandVariables("$((2 * (3 + 4)))", env));
    }

    @Test
    void arithmeticShiftLeft() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("32", executor.expandVariables("$((4 << 3))", env));
    }

    @Test
    void arithmeticShiftRight() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("4", executor.expandVariables("$((32 >> 3))", env));
    }

    @Test
    void arithmeticLessThan() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((3 < 5))", env));
        assertEquals("0", executor.expandVariables("$((5 < 3))", env));
    }

    @Test
    void arithmeticLessEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((5 <= 5))", env));
        assertEquals("0", executor.expandVariables("$((6 <= 5))", env));
    }

    @Test
    void arithmeticGreaterThan() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((5 > 3))", env));
        assertEquals("0", executor.expandVariables("$((3 > 5))", env));
    }

    @Test
    void arithmeticGreaterEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((5 >= 5))", env));
        assertEquals("0", executor.expandVariables("$((4 >= 5))", env));
    }

    @Test
    void arithmeticEqualEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((7 == 7))", env));
        assertEquals("0", executor.expandVariables("$((7 == 8))", env));
    }

    @Test
    void arithmeticNotEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((7 != 8))", env));
        assertEquals("0", executor.expandVariables("$((7 != 7))", env));
    }

    @Test
    void arithmeticBitAnd() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("4", executor.expandVariables("$((12 & 6))", env));
    }

    @Test
    void arithmeticBitXor() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("10", executor.expandVariables("$((12 ^ 6))", env));
    }

    @Test
    void arithmeticBitOr() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("14", executor.expandVariables("$((12 | 6))", env));
    }

    @Test
    void arithmeticLogicalAnd() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((1 && 2))", env));
        assertEquals("0", executor.expandVariables("$((0 && 2))", env));
    }

    @Test
    void arithmeticLogicalOr() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((0 || 5))", env));
        assertEquals("0", executor.expandVariables("$((0 || 0))", env));
    }

    @Test
    void arithmeticUnaryNot() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((!0))", env));
        assertEquals("0", executor.expandVariables("$((!5))", env));
    }

    @Test
    void arithmeticUnaryBitNot() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("-1", executor.expandVariables("$((~0))", env));
        assertEquals("-6", executor.expandVariables("$((~5))", env));
    }

    @Test
    void arithmeticTernaryTrue() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("10", executor.expandVariables("$((1 ? 10 : 20))", env));
    }

    @Test
    void arithmeticTernaryFalse() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("20", executor.expandVariables("$((0 ? 10 : 20))", env));
    }

    @Test
    void arithmeticTernaryNested() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("30", executor.expandVariables("$((1 ? 2 ? 30 : 40 : 50))", env));
        assertEquals("40", executor.expandVariables("$((1 ? 0 ? 30 : 40 : 50))", env));
        assertEquals("50", executor.expandVariables("$((0 ? 30 : 40 ? 50 : 60))", env));
    }

    @Test
    void arithmeticTernaryWithComparison() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "5");
        assertEquals("1", executor.expandVariables("$((x > 3 ? 1 : 0))", env));
        assertEquals("0", executor.expandVariables("$((x > 10 ? 1 : 0))", env));
    }

    @Test
    void arithmeticTernaryArithmeticBranches() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("7", executor.expandVariables("$((1 ? 3 + 4 : 99))", env));
        assertEquals("99", executor.expandVariables("$((0 ? 3 + 4 : 99))", env));
    }

    @Test
    void arithmeticAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("5", executor.expandVariables("$((x = 5))", env));
        assertEquals("5", env.get("x"));
    }

    @Test
    void arithmeticPlusAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        assertEquals("13", executor.expandVariables("$((x += 3))", env));
        assertEquals("13", env.get("x"));
    }

    @Test
    void arithmeticMinusAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        assertEquals("7", executor.expandVariables("$((x -= 3))", env));
        assertEquals("7", env.get("x"));
    }

    @Test
    void arithmeticMulDivModAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        assertEquals("30", executor.expandVariables("$((x *= 3))", env));
        env.put("y", "20");
        assertEquals("4", executor.expandVariables("$((y /= 5))", env));
        env.put("z", "23");
        assertEquals("3", executor.expandVariables("$((z %= 5))", env));
    }

    @Test
    void arithmeticShiftAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "4");
        assertEquals("32", executor.expandVariables("$((x <<= 3))", env));
        env.put("y", "32");
        assertEquals("4", executor.expandVariables("$((y >>= 3))", env));
    }

    @Test
    void arithmeticBitAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "12");
        assertEquals("4", executor.expandVariables("$((x &= 6))", env));
        env.put("y", "12");
        assertEquals("10", executor.expandVariables("$((y ^= 6))", env));
        env.put("z", "12");
        assertEquals("14", executor.expandVariables("$((z |= 6))", env));
    }

    @Test
    void arithmeticAssignRightAssoc() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("7", executor.expandVariables("$((a = b = 7))", env));
        assertEquals("7", env.get("a"));
        assertEquals("7", env.get("b"));
    }

    @Test
    void arithmeticShortCircuitAndSuppressesAssignment() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((0 && (x = 99)))", env);
        assertEquals(null, env.get("x"));
    }

    @Test
    void arithmeticShortCircuitOrSuppressesAssignment() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 || (x = 99)))", env);
        assertEquals(null, env.get("x"));
    }

    @Test
    void arithmeticTernaryShortCircuit() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        assertEquals("1", env.get("a"));
        assertEquals(null, env.get("b"));
        executor.expandVariables("$((0 ? (c = 3) : (d = 4)))", env);
        assertEquals(null, env.get("c"));
        assertEquals("4", env.get("d"));
    }

    @Test
    void captureStreamFromPipeline() throws Exception {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setCaptureStream(bos);
        executor.execute(this.parser("echo capture-me | cat").parse());
        assertTrue(bos.toString().contains("capture-me"), "captureStream: " + bos);
    }

    @Test
    void resolveCommandWithCustomPathDirInPath() throws Exception {
        final java.io.File dir = java.nio.file.Files.createTempDirectory("sh4j_custompath_").toFile();
        dir.deleteOnExit();
        final java.io.File bin = new java.io.File(dir, "myhello");
        try (java.io.FileWriter w = new java.io.FileWriter(bin)) { w.write("#!/bin/sh\necho ok\n"); }
        bin.setExecutable(true);
        bin.deleteOnExit();
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", dir.getAbsolutePath());
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parser("myhello").parse()));
    }

    @Test
    void externalCloseStdin() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat <&-").parse()));
    }

    @Test
    void externalStdoutToStderr() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("true 1>&2").parse()));
    }

    @Test
    void externalReadWriteFd0() throws Exception {
        final java.io.File f = java.io.File.createTempFile("sh4j_rw_0_", ".dat");
        f.deleteOnExit();
        try (java.io.FileWriter w = new java.io.FileWriter(f)) { w.write("seed\n"); }
        final Executor executor = new Executor();
        executor.execute(this.parser("cat <>" + f.getAbsolutePath()).parse());
    }

    @Test
    void externalHeredocDashStripsTabs() throws Exception {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat <<-EOF\n\t\thello\nEOF\n").parse()));
    }

    @Test
    void exportArgWithEqualsNonStandardName() throws Exception {
        final Executor executor = new Executor(new HashMap<>());
        final int code = executor.execute(this.parser("export 1foo=bar").parse());
        assertEquals(0, code);
        assertEquals("bar", executor.getVariable("1foo"));
    }

    @Test
    void execNonExecutableFileTriggersIoException() throws Exception {
        final java.io.File nonExec = java.io.File.createTempFile("sh4j_nonexec_", ".dat");
        nonExec.deleteOnExit();
        try (java.io.FileWriter w = new java.io.FileWriter(nonExec)) { w.write("not a script"); }
        nonExec.setExecutable(false);
        final Executor executor = new Executor();
        final int code = executor.execute(this.parser("exec " + nonExec.getAbsolutePath()).parse());
        assertTrue(code != 0, "non-executable should fail; got " + code);
    }

    @Test
    void execInterruptedException() {
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        final int code = executor.execute(this.parser("exec sleep 1").parse());
        Thread.interrupted();
        assertEquals(1, code);
    }

    @Test
    void externalNonExecutableFileTriggersIoException() throws Exception {
        final java.io.File nonExec = java.io.File.createTempFile("sh4j_ext_nonexec_", ".dat");
        nonExec.deleteOnExit();
        try (java.io.FileWriter w = new java.io.FileWriter(nonExec)) { w.write("nope"); }
        nonExec.setExecutable(false);
        final Executor executor = new Executor();
        final int code = executor.execute(this.parser(nonExec.getAbsolutePath()).parse());
        assertTrue(code != 0, "non-executable external should fail; got " + code);
    }

    @Test
    void arithmeticAssignDivByZero() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        assertEquals("0", executor.expandVariables("$((x /= 0))", env));
        assertEquals("0", env.get("x"));
    }

    @Test
    void arithmeticAssignModByZero() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        assertEquals("0", executor.expandVariables("$((x %= 0))", env));
    }

    @Test
    void arithmeticAssignNonNumericEnvTreatedAsZero() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        assertEquals("5", executor.expandVariables("$((x += 5))", env));
        assertEquals("5", env.get("x"));
    }

    @Test
    void arithmeticAssignWithUnderscoreName() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("42", executor.expandVariables("$((_foo = 42))", env));
        assertEquals("42", env.get("_foo"));
    }

    @Test
    void externalReadWriteRedirectFd2() throws Exception {
        final String path = "/tmp/sh4j_rw_fd2_" + System.nanoTime();
        new java.io.File(path).delete();
        final Executor executor = new Executor();
        executor.execute(this.parser("cat /nonexistent_xyz 2<>" + path).parse());
        new java.io.File(path).delete();
    }

    @Test
    void externalHereDocRedirect() throws Exception {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat <<EOF\nhello world\nEOF\n").parse()));
    }

    @Test
    void externalHereDocDashRedirect() throws Exception {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat <<-EOF\n\thello dashed\nEOF\n").parse()));
    }

    @Test
    void shiftLargerThanPositionalCount() {
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b")));
        assertEquals(0, executor.execute(this.parser("shift 100").parse()));
    }

    @Test
    void execWithMultipleArgs() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("exec true arg1 arg2 arg3").parse()));
    }

    @Test
    void positionalParamIndexOutOfRangeIsEmpty() {
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b")));
        final Map<String, String> env = new HashMap<>();
        assertEquals("", executor.expandVariables("$9", env));
    }

    @Test
    void normaliseBackticksBackslashNonSpecialChar() {
        assertEquals("$(echo \\n)", WordExpander.normaliseBackticks("`echo \\n`"));
    }

    @Test
    void whileLoopWithContinue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parser("c=true; while $c; do c=false; continue; echo unreached; done").parse());
        assertTrue(bos.toString().trim().isEmpty(), "continue should skip body, got: " + bos);
    }

    @Test
    void typeForExternalNonBuiltinCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("type ls").parse()));
        assertTrue(bos.toString().contains("ls is"));
    }

    @Test
    void exitWithValidIntegerAndWordExpansionError() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(1, executor.execute(this.parser("exit 5${UNSET_FOR_EXIT_999:?msg}").parse()));
    }

    @Test
    void expandBracedMultiDigitPositionalOutOfRange() {
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b", "c")));
        final Map<String, String> env = new HashMap<>();
        assertEquals("", executor.expandVariables("${10}", env));
    }

    @Test
    void expandBracedMultiDigitPositionalInRange() {
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b", "c")));
        final Map<String, String> env = new HashMap<>();
        assertEquals("b", executor.expandVariables("${2}", env));
    }

    @Test
    void expandBracedLeadingHashVar() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "hello");
        assertEquals("5", executor.expandVariables("${#x}", env));
    }

    @Test
    void expandBracedLeadingHashUnsetVar() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("0", executor.expandVariables("${#UNSET_HASH_TEST_xyz}", env));
    }

    @Test
    void specialParamShellFlags() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("", executor.expandVariables("$-", env));
    }

    @Test
    void specialParamBackgroundPidBeforeAnyJob() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("0", executor.expandVariables("$!", env));
    }

    @Test
    void specialParamAtStarWithPositionals() {
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b", "c")));
        final Map<String, String> env = new HashMap<>();
        assertEquals("a b c", executor.expandVariables("$@", env));
        assertEquals("a b c", executor.expandVariables("$*", env));
    }

    @Test
    void specialParamAtStarEmpty() {
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script")));
        final Map<String, String> env = new HashMap<>();
        assertEquals("", executor.expandVariables("$@", env));
    }

    @Test
    void arithmeticFactorNonNumericEnvAsZero() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        assertEquals("1", executor.expandVariables("$((x + 1))", env));
    }

    @Test
    void resolveCommandPathNullReturnsNull() {
        final Map<String, String> emptyEnv = new HashMap<>();
        emptyEnv.put("PATH", "");
        final Executor executor = new Executor(emptyEnv);
        executor.expandVariables("", emptyEnv);
        assertEquals(127, executor.execute(this.parser("nonexistent_xyz_zzz_cmd_abc").parse()));
    }

    @Test
    void resolveCommandWithEmptyDirInPath() {
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", ":/usr/bin");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parser("true").parse()));
    }

    @Test
    void createProcessBuilderWithQuotedArg() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("true \"quoted arg\"").parse()));
    }

    @Test
    void forLoopWithContinue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("for i in 1 2 3; do continue; echo $i; done").parse()));
        assertEquals("", bos.toString().trim());
    }

    @Test
    void typeForExternalCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("type true").parse()));
        assertTrue(bos.toString().contains("true is"));
    }

    @Test
    void exitWithWordExpansionError() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(1, executor.execute(this.parser("exit ${UNSET_FOR_EXIT_8x9:?errmsg}").parse()));
    }

    @Test
    void exportArgWithEquals() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(0, executor.execute(this.parser("export FOO=hello").parse()));
        assertEquals("hello", executor.getVariable("FOO"));
    }

    @Test
    void expandBracedColonEqWhenSet() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "set");
        assertEquals("set", executor.expandVariables("${x:=default}", env));
    }

    @Test
    void expandBracedQuestionWhenSet() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "value");
        assertEquals("value", executor.expandVariables("${x?msg}", env));
    }

    @Test
    void expandBracedPrefixHashNoMatch() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abcdef");
        assertEquals("abcdef", executor.expandVariables("${x#zzz}", env));
    }

    @Test
    void expandBracedUnknownOperatorFallsThrough() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "value");
        assertEquals("value@weird}", executor.expandVariables("${x@weird}", env));
    }

    @Test
    void tildeUserNameFallthrough() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("~nonexistentuser123", executor.expandVariables("~nonexistentuser123", env));
    }

    @Test
    void arithmeticPrecedenceMixed() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((1 + 2 == 3))", env));
        assertEquals("1", executor.expandVariables("$((1 < 2 && 3 < 4))", env));
        assertEquals("7", executor.expandVariables("$((1 | 2 | 4 & 6))", env));
    }

    @Test
    void defaultIfsSet() {
        final Executor executor = new Executor();
        assertEquals(" \t\n", executor.getVariable("IFS"));
    }

    @Test
    void defaultHomeSet() {
        final Executor executor = new Executor();
        assertEquals(System.getProperty("user.home"), executor.getVariable("HOME"));
    }

    @Test
    void defaultPathSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", "");
        final Executor executor = new Executor(env);
        assertEquals("/usr/local/bin:/usr/bin:/bin", executor.getVariable("PATH"));
    }

    @Test
    void defaultsDoNotOverrideExistingEnv() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/custom/home");
        env.put("PATH", "/custom/path");
        final Executor executor = new Executor(env);
        assertEquals("/custom/home", executor.getVariable("HOME"));
        assertEquals("/custom/path", executor.getVariable("PATH"));
        assertEquals(" \t\n", executor.getVariable("IFS"));
    }

    @Test
    void commandFoundInPath() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("true").parse()));
    }

    @Test
    void commandNotFoundReturns127() {
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(this.parser("nonexistent_cmd_xyz_127").parse()));
    }

    @Test
    void commandWithSlashFound() {
        final Map<String, String> env = new HashMap<>();
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parser("/user/.nix-profile/bin/ls").parse()));
    }

    @Test
    void commandFoundInCustomPath() throws IOException {
        final File dir = new File("/tmp/sh4j_path_test_" + System.nanoTime());
        dir.mkdirs();
        dir.deleteOnExit();
        final File script = new File(dir, "mycmd");
        try (FileWriter w = new FileWriter(script)) {
            w.write("#!/bin/sh\nexit 42\n");
        }
        script.setExecutable(true);
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", dir.getAbsolutePath());
        final Executor executor = new Executor(env);
        dir.delete();
        assertEquals(42, executor.execute(this.parser("mycmd").parse()));
    }

    @Test
    void commandNotFoundEmptyPath() {
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", "");
        final Executor executor = new Executor(env);
        assertEquals(127, executor.execute(this.parser("somecmd").parse()));
    }

    @Test
    void commandFoundInSecondPathDir() throws IOException {
        final File dir = new File("/tmp/sh4j_path_test2_" + System.nanoTime());
        dir.mkdirs();
        dir.deleteOnExit();
        final File script = new File(dir, "mycmd2");
        try (FileWriter w = new FileWriter(script)) {
            w.write("#!/bin/sh\nexit 7\n");
        }
        script.setExecutable(true);
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", "/nonexistent_test_dir_xyz:" + dir.getAbsolutePath());
        final Executor executor = new Executor(env);
        dir.delete();
        assertEquals(7, executor.execute(this.parser("mycmd2").parse()));
    }

    @Test
    void commandWithSlashNotFound() {
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(this.parser("/nonexistent_path_xyz_abc/cmd").parse()));
    }

    @Test
    void exportWithValue() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("export FOO=bar").parse()));
        assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void exportExistingVariable() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("export FOO").parse()));
    }

    @Test
    void exportThenVariableExpands() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("export MSG=hello; echo $MSG").parse()));
        assertEquals("hello", executor.getVariable("MSG"));
    }

    @Test
    void splitFieldsDefaultIfs() {
        assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a b c", " \t\n"));
    }

    @Test
    void splitFieldsMultipleWs() {
        assertEquals(List.of("a", "b"), WordExpander.splitFields("a   b", " \t\n"));
    }

    @Test
    void splitFieldsLeadingWs() {
        assertEquals(List.of("a", "b"), WordExpander.splitFields("  a b", " \t\n"));
    }

    @Test
    void splitFieldsTrailingWs() {
        assertEquals(List.of("a", "b"), WordExpander.splitFields("a b  ", " \t\n"));
    }

    @Test
    void splitFieldsColon() {
        assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a:b:c", ":"));
    }

    @Test
    void splitFieldsEmptyBetweenColons() {
        assertEquals(List.of("a", "", "b"), WordExpander.splitFields("a::b", ":"));
    }

    @Test
    void splitFieldsOnlyColons() {
        assertEquals(List.of("", ""), WordExpander.splitFields(":", ":"));
    }

    @Test
    void splitFieldsDoubleColon() {
        assertEquals(List.of("", "", ""), WordExpander.splitFields("::", ":"));
    }

    @Test
    void splitFieldsNoDelim() {
        assertEquals(List.of("hello"), WordExpander.splitFields("hello", ":"));
    }

    @Test
    void splitFieldsEmptyIfs() {
        assertEquals(List.of("hello world"), WordExpander.splitFields("hello world", ""));
    }

    @Test
    void splitFieldsNullIfs() {
        assertEquals(List.of("hello"), WordExpander.splitFields("hello", null));
    }

    @Test
    void splitFieldsEmptyValue() {
        assertEquals(List.of(), WordExpander.splitFields("", " \t\n"));
    }

    @Test
    void splitFieldsMixedWsNws() {
        assertEquals(List.of("a", "", "b"), WordExpander.splitFields("a :b", " :"));
    }

    @Test
    void splitFieldsTrailingColon() {
        assertEquals(List.of("a", ""), WordExpander.splitFields("a:", ":"));
    }

    @Test
    void splitFieldsCustomNws() {
        assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a,b,c", ","));
    }

    @Test
    void isQuotedDoubleQuotes() {
        assertTrue(WordExpander.isQuoted("\"hello\""));
    }

    @Test
    void isQuotedSingleQuotes() {
        assertTrue(WordExpander.isQuoted("'hello'"));
    }

    @Test
    void isQuotedNoQuotes() {
        assertFalse(WordExpander.isQuoted("hello"));
    }

    @Test
    void isQuotedPartial() {
        assertFalse(WordExpander.isQuoted("a\"b\"c"));
    }

    @Test
    void removeQuotesNoop() {
        assertEquals("hello", WordExpander.removeQuotes("hello"));
    }

    @Test
    void removeQuotesDouble() {
        assertEquals("hello world", WordExpander.removeQuotes("\"hello world\""));
    }

    @Test
    void removeQuotesSingle() {
        assertEquals("hello", WordExpander.removeQuotes("'hello'"));
    }

    @Test
    void removeQuotesPartialDouble() {
        assertEquals("foovaluebar", WordExpander.removeQuotes("foo\"value\"bar"));
    }

    @Test
    void removeQuotesPartialSingle() {
        assertEquals("helloworld", WordExpander.removeQuotes("hello'world'"));
    }

    @Test
    void removeQuotesApostropheInsideDouble() {
        assertEquals("it's", WordExpander.removeQuotes("\"it's\""));
    }

    @Test
    void removeQuotesNestedQuotes() {
        assertEquals("hello \"there\" world", WordExpander.removeQuotes("\"hello \\\"there\\\" world\""));
    }

    @Test
    void removeQuotesBackslashOutside() {
        assertEquals("hello world", WordExpander.removeQuotes("hello\\ world"));
    }

    @Test
    void removeQuotesBackslashInDouble() {
        assertEquals("hello\"world", WordExpander.removeQuotes("\"hello\\\"world\""));
    }

    @Test
    void removeQuotesEmptyQuotes() {
        assertEquals("", WordExpander.removeQuotes("\"\""));
        assertEquals("", WordExpander.removeQuotes("''"));
    }

    @Test
    void echoQuotedVarNoSplit() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(0, executor.execute(this.parser("MSG=\"hello  world\"; echo \"$MSG\"").parse()));
    }

    @Test
    void echoUnquotedVarSplit() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(0, executor.execute(this.parser("MSG=\"hello  world\"; echo $MSG").parse()));
    }

    @Test
    void echoQuotedLiteralNoSplit() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("echo \"hello   world\"").parse()));
    }

    @Test
    void echoCustomIfsColon() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", ":");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parser("echo a:b:c").parse()));
    }

    @Test
    void echoEmptyIfsNoSplit() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", "");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parser("echo hello   world").parse()));
    }

    @Test
    void splitFieldsTabIfs() {
        assertEquals(List.of("a", "b"), WordExpander.splitFields("a\tb", "\t"));
    }

    @Test
    void splitFieldsMixedIfs() {
        assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a:b c", ": "));
    }

    @Test
    void containsGlobCharsStar() {
        assertTrue(WordExpander.containsGlobChars("*.txt"));
    }

    @Test
    void containsGlobCharsQuestion() {
        assertTrue(WordExpander.containsGlobChars("file?"));
    }

    @Test
    void containsGlobCharsBracket() {
        assertTrue(WordExpander.containsGlobChars("[ab]"));
    }

    @Test
    void containsGlobCharsNone() {
        assertFalse(WordExpander.containsGlobChars("hello.txt"));
    }

    @Test
    void pathnameExpandNoGlob() {
        assertEquals(List.of("hello"), WordExpander.pathnameExpand(List.of("hello")));
    }

    @Test
    void pathnameExpandGlobNoMatchKeepsPattern() {
        assertEquals(List.of("__no_match__xyz__"), WordExpander.pathnameExpand(List.of("__no_match__xyz__")));
    }

    @Test
    void pathnameExpandGlobWithMatch() {
        assertEquals(List.of("pom.xml"), WordExpander.pathnameExpand(List.of("pom.xml")));
    }

    @Test
    void pathnameExpandWildcardMatch() {
        assertTrue(WordExpander.pathnameExpand(List.of("*.xml")).contains("pom.xml"));
    }

    @Test
    void pathnameExpandGlobQuoted() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("echo \"*.java\"").parse()));
    }

    @Test
    void pathnameExpandGlobUnquoted() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("echo *.java").parse()));
    }

    @Test
    void expandBracedColonQuestionUnset() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals("", executor.expandVariables("${x:?my error}", new HashMap<>()));
    }

    @Test
    void expandBracedQuestionUnset() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals("", executor.expandVariables("${x?my error}", new HashMap<>()));
    }

    @Test
    void expandBracedColonQuestionSet() {
        final Executor executor = new Executor(new HashMap<>());
        final Map<String, String> env = new HashMap<>(Map.of("x", "value"));
        assertEquals("value", executor.expandVariables("${x:?not used}", env));
    }

    @Test
    void expandBracedColonQuestionDefaultMsg() {
        final Executor executor = new Executor(new HashMap<>());
        executor.expandVariables("${x:?}", new HashMap<>());
    }

    @Test
    void expandBracedColonQuestionErrorStream() {
        final Executor executor = new Executor(new HashMap<>());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setErrorStream(new PrintStream(bos, true));
        executor.expandVariables("${x:?my error}", new HashMap<>());
        assertTrue(bos.toString().trim().contains("my error"), "error message should go to injected stream");
    }

    @Test
    void cdToDirectory() throws Exception {
        final String saved = System.getProperty("user.dir");
        try {
            final Executor executor = new Executor();
            assertEquals(0, executor.execute(this.parser("cd /tmp").parse()));
            assertEquals("/tmp", executor.getVariable("PWD"));
        } finally {
            System.setProperty("user.dir", saved);
        }
    }

    @Test
    void cdToHome() throws Exception {
        final String saved = System.getProperty("user.dir");
        try {
            final Map<String, String> env = new HashMap<>();
            env.put("HOME", "/tmp");
            final Executor executor = new Executor(env);
            assertEquals(0, executor.execute(this.parser("cd").parse()));
            assertEquals("/tmp", executor.getVariable("PWD"));
        } finally {
            System.setProperty("user.dir", saved);
        }
    }

    @Test
    void cdToNonexistentReturns1() {
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(this.parser("cd /sh4j_nonexistent_dir_xyz").parse()));
    }

    @Test
    void cdMinus() throws Exception {
        final String saved = System.getProperty("user.dir");
        try {
            final Map<String, String> env = new HashMap<>();
            env.put("PWD", "/tmp");
            env.put("OLDPWD", "/");
            final Executor executor = new Executor(env);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            executor.setCaptureStream(bos);
            assertEquals(0, executor.execute(this.parser("cd -").parse()));
            assertEquals("/", executor.getVariable("PWD"));
            assertEquals("/tmp", executor.getVariable("OLDPWD"));
            assertEquals("/", bos.toString().trim());
        } finally {
            System.setProperty("user.dir", saved);
        }
    }

    @Test
    void cdWithExpandedVar() throws Exception {
        final String saved = System.getProperty("user.dir");
        try {
            final Map<String, String> env = new HashMap<>();
            env.put("MYDIR", "/tmp");
            final Executor executor = new Executor(env);
            assertEquals(0, executor.execute(this.parser("cd $MYDIR").parse()));
            assertEquals("/tmp", executor.getVariable("PWD"));
        } finally {
            System.setProperty("user.dir", saved);
        }
    }

    @Test
    void typeBuiltin() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("type echo cd exit export type true false shift unset").parse()));
        final String out = bos.toString();
        assertTrue(out.contains("echo is a shell built-in"));
        assertTrue(out.contains("cd is a shell built-in"));
        assertTrue(out.contains("exit is a shell built-in"));
        assertTrue(out.contains("export is a shell built-in"));
        assertTrue(out.contains("type is a shell built-in"));
        assertTrue(out.contains("true is a shell built-in"));
        assertTrue(out.contains("false is a shell built-in"));
    }

    @Test
    void typeExternal() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("type true").parse()));
        assertTrue(bos.toString().contains("true is "));
    }

    @Test
    void typeNotFound() {
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(this.parser("type nonexistent_cmd_xyz").parse()));
    }

    @Test
    void execBuiltinRunsCommand() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("exec true").parse()));
    }

    @Test
    void execBuiltinNotFoundReturns127() {
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(this.parser("exec nonexistent_cmd_xyz").parse()));
    }

    @Test
    void execBuiltinNoArgsIsNoop() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("exec").parse()));
    }

    @Test
    void trueBuiltinReturns0() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("true").parse()));
    }

    @Test
    void falseBuiltinReturns1() {
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(this.parser("false").parse()));
    }

    @Test
    void unsetRemovesVariable() {
        final Map<String, String> env = new HashMap<>();
        env.put("MYVAR", "value");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parser("unset MYVAR").parse()));
        assertNull(executor.getVariable("MYVAR"));
    }

    @Test
    void shiftRemovesPositionalParam() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(List.of("sh", "a", "b", "c")));
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("shift; echo $#").parse()));
        assertEquals("2", bos.toString().trim());
    }

    @Test
    void shiftWithCount() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(List.of("sh", "a", "b", "c")));
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("shift 2; echo $#").parse()));
        assertEquals("1", bos.toString().trim());
    }

    @Test
    void breakStopsWhileLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("while exit $?; do echo ran; break; exit 1; done").parse()));
        assertEquals("ran", bos.toString().trim());
    }

    @Test
    void breakStopsForLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("for i in a b c; do echo $i; break; done").parse()));
        assertEquals("a", bos.toString().trim());
    }

    @Test
    void continueInForLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("for i in a b; do echo $i; continue; echo after; done").parse()));
        assertEquals("a\nb", bos.toString().trim());
        assertFalse(bos.toString().contains("after"));
    }

    @Test
    void colonBuiltinReturns0() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser(":").parse()));
    }

    @Test
    void evalBuiltinRunsCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("eval echo hello world").parse()));
        assertEquals("hello world", bos.toString().trim());
    }

    @Test
    void evalBuiltinNoArgsReturns0() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("eval").parse()));
    }

    @Test
    void externalOutputRedirect() throws Exception {
        final String inputPath = "/tmp/sh4j_ext_in.txt";
        final String outputPath = "/tmp/sh4j_ext_out.txt";
        new java.io.File(outputPath).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("hello world"); }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat < " + inputPath + " > " + outputPath).parse()));
        assertTrue(new java.io.File(outputPath).exists());
        assertEquals("hello world", java.nio.file.Files.readString(java.nio.file.Path.of(outputPath)).trim());
        new java.io.File(inputPath).delete();
        new java.io.File(outputPath).delete();
    }

    @Test
    void externalAppendRedirect() throws Exception {
        final String inputPath = "/tmp/sh4j_ext_append_in.txt";
        final String outputPath = "/tmp/sh4j_ext_append_out.txt";
        new java.io.File(outputPath).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("second"); }
        try (java.io.FileWriter w = new java.io.FileWriter(outputPath)) { w.write("first\n"); }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat < " + inputPath + " >> " + outputPath).parse()));
        final String content = java.nio.file.Files.readString(java.nio.file.Path.of(outputPath)).trim();
        assertTrue(content.startsWith("first"));
        assertTrue(content.contains("second"));
        new java.io.File(inputPath).delete();
        new java.io.File(outputPath).delete();
    }

    @Test
    void externalClobberRedirect() throws Exception {
        final String inputPath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outputPath = "/tmp/sh4j_ext_clobber_out.txt";
        new java.io.File(outputPath).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("hello clobber"); }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat < " + inputPath + " >| " + outputPath).parse()));
        assertTrue(new java.io.File(outputPath).exists());
        assertEquals("hello clobber", java.nio.file.Files.readString(java.nio.file.Path.of(outputPath)).trim());
        new java.io.File(inputPath).delete();
        new java.io.File(outputPath).delete();
    }

    @Test
    void externalRedirectStdoutToStderr() throws Exception {
        final String inputPath = "/tmp/sh4j_ext_12_in.txt";
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("hello"); }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat < " + inputPath + " 1>&2").parse()));
        new java.io.File(inputPath).delete();
    }

    @Test
    void externalRedirectStderrToStdout() throws Exception {
        final String inputPath = "/tmp/sh4j_ext_21_in.txt";
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("hello"); }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat < " + inputPath + " 2>&1").parse()));
        new java.io.File(inputPath).delete();
    }

    @Test
    void externalCloseStdout() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("true 1>&-").parse()));
    }

    @Test
    void externalCloseStderr() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("true 2>&-").parse()));
    }

    @Test
    void externalStderrOutputRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_out.txt";
        new java.io.File(path).delete();
        final Executor executor = new Executor();
        executor.execute(this.parser("cat /nonexistent_path_xyz_123 2> " + path).parse());
        assertTrue(new java.io.File(path).exists());
        new java.io.File(path).delete();
    }

    @Test
    void externalStderrAppendRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_append.txt";
        new java.io.File(path).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(path)) { w.write("initial\n"); }
        final Executor executor = new Executor();
        executor.execute(this.parser("cat /nonexistent_path_xyz_456 2>> " + path).parse());
        assertTrue(java.nio.file.Files.readString(java.nio.file.Path.of(path)).contains("initial"));
        new java.io.File(path).delete();
    }

    @Test
    void externalReadWriteRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_rw.txt";
        try (java.io.FileWriter w = new java.io.FileWriter(path)) { w.write("hello"); }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser("cat <> " + path).parse()));
        new java.io.File(path).delete();
    }

    @Test
    void heredocStripTabsBasic() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("cat <<- EOF\n\thello\nEOF\n").parse()));
        assertEquals("hello", bos.toString().trim());
    }

    @Test
    void heredocStripTabsMultiple() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("cat <<- EOF\n\t\thello\nEOF\n").parse()));
        assertEquals("hello", bos.toString().trim());
    }

    @Test
    void heredocStripTabsDelimiterWithTabs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("cat <<- EOF\n\thello\n\tEOF\n").parse()));
        assertEquals("hello", bos.toString().trim());
    }

    @Test
    void heredocStripTabsMixed() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("cat <<- MARKER\n\tfirst\nsecond\n\tthird\nMARKER\n").parse()));
        assertEquals("first\nsecond\nthird", bos.toString().trim());
    }

    @Test
    void typeFunction() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parser("myfunc() { true; }; type myfunc").parse()));
        assertTrue(bos.toString().contains("myfunc is a function"));
    }

    @Test
    void shiftNonNumericReturns1() {
        assertEquals(1, new Executor().execute(this.parser("shift abc").parse()));
    }

    @Test
    void shiftNegativeReturns1() {
        assertEquals(1, new Executor().execute(this.parser("shift -1").parse()));
    }
}
