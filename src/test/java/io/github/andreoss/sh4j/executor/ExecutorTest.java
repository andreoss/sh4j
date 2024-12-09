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
        ListNode list = parser("echo hello").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executePwd() {
        ListNode list = parser("pwd").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeTrue() {
        ListNode list = parser("true").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeFalse() {
        ListNode list = parser("false").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(1, exitCode);
    }

    @Test
    void executeWithArguments() {
        ListNode list = parser("echo a b c").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeSemicolonSeparator() {
        ListNode list = parser("echo a; echo b").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeAndIf() {
        ListNode list = parser("echo a && echo b").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeOrIf() {
        ListNode list = parser("false || echo b").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeAndIfStopsOnFailure() {
        ListNode list = parser("false && echo not reached").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(1, exitCode);
    }

    @Test
    void executeOrIfStopsOnSuccess() {
        ListNode list = parser("echo success || echo not reached").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeEmptyList() {
        ListNode list = parser("").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeAssignmentOnly() {
        ListNode list = parser("FOO=bar").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
        assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void executeAssignmentWithCommand() {
        ListNode list = parser("FOO=hello echo $FOO").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeMultipleAssignments() {
        ListNode list = parser("A=1 B=2").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
        assertEquals("1", executor.getVariable("A"));
        assertEquals("2", executor.getVariable("B"));
    }

    @Test
    void executeVariableExpansion() {
        ListNode list = parser("echo $HOME").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeOutputRedirection() throws Exception {
        ListNode list = parser("echo test > /tmp/sh4j_test_output").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeInputRedirection() throws Exception {
        ListNode list = parser("cat < /dev/null").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executePipeline() {
        ListNode list = parser("echo hello | cat").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeBackground() {
        ListNode list = parser("echo hello &").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeBackgroundSetsLastBgPid() {
        ListNode list = parser("echo hello &").parse();
        Executor executor = new Executor();
        executor.execute(list);
        assertTrue(executor.getLastBgPid() > 0, "lastBgPid should be > 0 after & command");
    }

    @Test
    void executeBackgroundAndSequential() {
        ListNode list = parser("echo bg1 & echo seq1").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeClobberRedirection() throws Exception {
        ListNode list = parser("echo test >| /tmp/sh4j_clobber_test").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeBuiltinStderrRedirect() throws Exception {
        ListNode list = parser("echo hello 2>/dev/null").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrToStdout() throws Exception {
        ListNode list = parser("echo hello 2>&1").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrRedirectToFile() throws Exception {
        String file = "/tmp/sh4j_stderr_test";
        ListNode list = parser("echo hello 2>" + file).parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrAppendToFile() throws Exception {
        String file = "/tmp/sh4j_stderr_append_" + System.nanoTime();
        ListNode list = parser("echo hello 2>>" + file).parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutAppendToFile() throws Exception {
        String file = "/tmp/sh4j_stdout_append_" + System.nanoTime();
        ListNode list = parser("echo hello >>" + file).parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutRedirectIOException() throws Exception {
        ListNode list = parser("echo hello >/dev/null/no_such_path_xyz").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrRedirectIOException() throws Exception {
        ListNode list = parser("echo hello 2>/dev/null/no_such_stderr_xyz").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStdoutAppendIOException() throws Exception {
        ListNode list = parser("echo hello >>/dev/null/no_such_append_xyz").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrAppendIOException() throws Exception {
        ListNode list = parser("echo hello 2>>/dev/null/no_such_eappend_xyz").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinCloseStdout() {
        ListNode list = parser("echo hello >&-").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeMultiplePipelines() {
        ListNode list = parser("echo a; echo b; echo c").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeBangPipeline() {
        ListNode list = parser("! false").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeVariableWithEmptyValue() {
        ListNode list = parser("echo $EMPTY_VAR").parse();
        Map<String, String> env = new HashMap<>();
        env.put("EMPTY_VAR", "");
        Executor executor = new Executor(env);
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeCommandNotFound() {
        ListNode list = parser("nonexistent_command_xyz").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(127, exitCode);
    }

    @Test
    void executeCommandNotFoundReturns127() {
        ListNode list = parser("nonexistent_cmd_xyz_127").parse();
        Executor executor = new Executor();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void executeBangWithTrue() {
        ListNode list = parser("! true").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(1, exitCode);
    }

    @Test
    void executeExitZero() {
        ListNode list = parser("exit 0").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeExitOne() {
        ListNode list = parser("exit 1").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(1, exitCode);
    }

    @Test
    void executeEchoAbc() {
        ListNode list = parser("echo abc").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeEmptyListNode() {
        ListNode list = new ListNode();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executePipelineWithNoCommands() {
        ListNode list = new ListNode();
        list.addPipeline(new Pipeline(false), ListNode.Separator.NONE);
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAppendRedirection() throws Exception {
        ListNode list = parser("echo test >> /tmp/sh4j_append_test").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeStderrRedirect() throws Exception {
        ListNode list = parser("echo test 2>/tmp/sh4j_test_stderr").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeStderrToStdout() throws Exception {
        ListNode list = parser("echo test 2>&1").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeCloseStdin() throws Exception {
        ListNode list = parser("cat <&-").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeCloseStdout() throws Exception {
        ListNode list = parser("echo test >&-").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeReadWriteRedirect() throws Exception {
        ListNode list = parser("cat <> /dev/null").parse();
        Executor executor = new Executor();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
    }

    @Test
    void executeExitNoArgs() {
        ListNode list = parser("exit").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeExitWithNonNumericArg() {
        ListNode list = parser("exit xyz").parse();
        Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executePipelineWithNullCommand() {
        Pipeline pipeline = new Pipeline(false);
        pipeline.getCommands().add(null);
        ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCommandWithNullName() {
        SimpleCommand cmd = new SimpleCommand(null);
        Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(cmd);
        ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAssignmentWithNoEquals() {
        SimpleCommand cmd = new SimpleCommand("echo");
        cmd.getAssignments().add("INVALID");
        Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(cmd);
        ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executePipelineWithFailingCommand() {
        ListNode list = parser("echo hello | nonexistent_cmd_xyz_fail | echo done").parse();
        Executor executor = new Executor();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void executeSubshell() {
        ListNode list = parser("(echo hello)").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSubshellWithMultiplePipelines() {
        ListNode list = parser("(echo a; echo b)").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSubshellInPipeline() {
        ListNode list = parser("echo foo | (cat) | wc").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSubshellWithExit() {
        ListNode list = parser("(exit 42)").parse();
        Executor executor = new Executor();
        assertEquals(42, executor.execute(list));
    }

    @Test
    void executeGrouping() {
        ListNode list = parser("{ echo hello; }").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeGroupingInPipeline() {
        ListNode list = parser("echo foo | { cat; } | wc").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeGroupingSharesEnvironment() {
        ListNode list = parser("FOO=bar; { echo $FOO; }").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeIfTrueThen() {
        ListNode list = parser("if true; then echo hi; fi").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeIfFalseElse() {
        ListNode list = parser("if false; then echo a; else echo b; fi").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeIfTrueThenElseNotExecuted() {
        ListNode list = parser("if true; then echo hi; else false; fi").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeForDoDone() {
        ListNode list = parser("for var in a b c; do echo $var; done").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeWhileFalse() {
        ListNode list = parser("while false; do echo hi; done").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeUntilTrue() {
        ListNode list = parser("until true; do echo hi; done").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseMatch() {
        ListNode list = parser("case a in a) echo match;; esac").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseNoMatch() {
        ListNode list = parser("case a in b) echo nomatch;; esac").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseDefaultStar() {
        ListNode list = parser("case a in *) echo default;; esac").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCasePipePattern() {
        ListNode list = parser("case a in b|c|a) echo match;; esac").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseFirstMatchOnly() {
        ListNode list = parser("case a in a) echo first;; b) echo second;; esac").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFunctionDefinitionStored() {
        ListNode list = parser("foo() { echo hello; }").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFunctionCall() {
        ListNode list = parser("foo() { echo hello; }; foo").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFunctionSubshellBody() {
        ListNode list = parser("foo() (echo hello); foo").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeWhileLoop() {
        ListNode list = parser("while false; do echo body; done").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeUntilLoop() {
        ListNode list = parser("until true; do echo body; done").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeWhileBodyRunsOnce() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ListNode list = parser("while exit $?; do echo ran; exit 1; done").parse();
        Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(1, executor.execute(list));
        assertEquals("ran\n", bos.toString());
    }

    @Test
    void executeUntilBodyRunsOnce() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ListNode list = parser("exit 1; until exit $?; do echo ran; exit 0; done").parse();
        Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("ran\n", bos.toString());
    }

    @Test
    void executeWhileBodyExitCodePropagated() {
        ListNode list = parser("while exit $?; do exit 42; done").parse();
        Executor executor = new Executor();
        assertEquals(42, executor.execute(list));
    }

    @Test
    void testCommandInterruptedException() {
        ListNode list = parser("pwd").parse();
        Executor executor = new Executor();
        Thread.currentThread().interrupt();
        int result = executor.execute(list);
        assertEquals(1, result);
    }

    @Test
    void testPipelineInterruptedException() {
        ListNode list = parser("pwd | cat").parse();
        Executor executor = new Executor();
        Thread.currentThread().interrupt();
        int result = executor.execute(list);
        assertEquals(1, result);
    }

    @Test
    void waitForBackgroundJobsInterrupted() {
        ListNode list = parser("sleep 1 &").parse();
        Executor executor = new Executor();
        Thread.currentThread().interrupt();
        executor.execute(list);
        boolean reInterrupted = Thread.interrupted();
        assertTrue(reInterrupted, "InterruptedException catch should re-set interrupt flag");
    }

    @Test
    void testExportNoArgs() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Map<String, String> env = new HashMap<>();
        env.put("TEST_EXPORT_VAR", "hello");
        Executor executor = new Executor(env);
        executor.setOutputStream(new PrintStream(bos, true));
        ListNode list = parser("export").parse();
        int exitCode = executor.execute(list);
        assertEquals(0, exitCode);
        String output = bos.toString();
        assertTrue(output.contains("export TEST_EXPORT_VAR=hello"));
    }

    @Test
    void executeIfFalseNoElse() {
        ListNode list = parser("if false; then echo yes; fi").parse();
        Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeElifWhenIfFalse() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ListNode list = parser("if false; then echo no; elif true; then echo yes; fi").parse();
        Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("yes\n", bos.toString());
    }

    @Test
    void executeElifChainFalseFalse() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ListNode list = parser("if false; then echo a; elif false; then echo b; else echo c; fi").parse();
        Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("c\n", bos.toString());
    }

    @Test
    void executeElifChainSecondElifTrue() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ListNode list = parser("if false; then echo a; elif false; then echo b; elif true; then echo c; fi").parse();
        Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("c\n", bos.toString());
    }

    @Test
    void executeRedirectStdoutToStderr() {
        ListNode list = parser("echo test 1>&2").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeHereDoc() {
        ListNode list = parser("cat << EOF\nhello\nEOF\n").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void testHeredocExpansion() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        ListNode list = parser("cat << EOF\nhello $TEST_HVAR\nEOF\n").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("hello expanded", bos.toString().trim());
    }

    @Test
    void testRedirectTargetExpansionOutput() throws Exception {
        java.io.File f = new java.io.File("/tmp/sh4j_redir_expand_test.txt");
        f.delete();
        Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        Executor executor = new Executor(env);
        ListNode list = parser("echo hello > $TEST_FILE").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(f.exists());
        assertEquals("hello\n", new String(java.nio.file.Files.readAllBytes(f.toPath())));
        f.delete();
    }

    @Test
    void testRedirectTargetExpansionAppend() throws Exception {
        java.io.File f = new java.io.File("/tmp/sh4j_redir_append_expand.txt");
        f.delete();
        Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        Executor executor = new Executor(env);
        ListNode list = parser("echo hello >> $TEST_FILE").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(f.exists());
        assertEquals("hello\n", new String(java.nio.file.Files.readAllBytes(f.toPath())));
        f.delete();
    }

    @Test
    void testRedirectTargetExpansionInput() throws Exception {
        java.io.File f = new java.io.File("/tmp/sh4j_redir_input_expand.txt");
        f.delete();
        try (java.io.FileWriter w = new java.io.FileWriter(f)) {
            w.write("hello from file");
        }
        Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_input_expand.txt");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        ListNode list = parser("cat < $TEST_FILE").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("hello from file", bos.toString().trim());
        f.delete();
    }

    @Test
    void executeNonExecutableCommand() throws Exception {
        java.io.File f = new java.io.File("/tmp/sh4j_nonexec_test.sh");
        f.createNewFile();
        f.setExecutable(false);
        ListNode list = parser(f.getAbsolutePath()).parse();
        Executor executor = new Executor();
        assertEquals(126, executor.execute(list));
    }

    @Test
    void expandDollarQuestion() {
        Executor executor = new Executor();
        ListNode list = parser("false").parse();
        executor.execute(list);
        assertEquals("1", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionAfterSuccess() {
        Executor executor = new Executor();
        ListNode list = parser("true").parse();
        executor.execute(list);
        assertEquals("0", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarHyphen() {
        Executor executor = new Executor();
        assertEquals("", executor.expandVariables("$-", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionBraced() {
        Executor executor = new Executor();
        ListNode list = parser("false").parse();
        executor.execute(list);
        assertEquals("1", executor.expandVariables("${?}", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionInString() {
        Executor executor = new Executor();
        ListNode list = parser("false").parse();
        executor.execute(list);
        assertEquals("exit:1", executor.expandVariables("exit:$?", new HashMap<>()));
    }

    @Test
    void expandDollarHash() {
        Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c"));
        assertEquals("3", executor.expandVariables("$#", new HashMap<>()));
    }

    @Test
    void expandDollarAt() {
        Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c"));
        assertEquals("a b c", executor.expandVariables("$@", new HashMap<>()));
    }

    @Test
    void expandDollarStar() {
        Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c"));
        assertEquals("a b c", executor.expandVariables("$*", new HashMap<>()));
    }

    @Test
    void expandDollarZero() {
        Executor executor = new Executor(new HashMap<>(), List.of("my_script"));
        assertEquals("my_script", executor.expandVariables("$0", new HashMap<>()));
    }

    @Test
    void expandDollarOne() {
        Executor executor = new Executor(new HashMap<>(), List.of("sh", "first", "second"));
        assertEquals("first", executor.expandVariables("$1", new HashMap<>()));
    }

    @Test
    void expandDollarNine() {
        Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i"));
        assertEquals("i", executor.expandVariables("$9", new HashMap<>()));
    }

    @Test
    void expandBracedMultiDigit() {
        Executor executor = new Executor(new HashMap<>(), List.of("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
        assertEquals("j", executor.expandVariables("${10}", new HashMap<>()));
    }

    @Test
    void expandDollarHashNone() {
        Executor executor = new Executor();
        assertEquals("0", executor.expandVariables("$#", new HashMap<>()));
    }

    @Test
    void expandDollarAtEmpty() {
        Executor executor = new Executor();
        assertEquals("", executor.expandVariables("$@", new HashMap<>()));
    }

    @Test
    void expandDollarDollar() {
        Executor executor = new Executor();
        String pid = executor.expandVariables("$$", new HashMap<>());
        assertNotNull(pid);
        assertFalse(pid.isEmpty());
        assertEquals(String.valueOf(ProcessHandle.current().pid()), pid);
    }

    @Test
    void expandDollarBang() {
        Executor executor = new Executor();
        assertEquals("0", executor.expandVariables("$!", new HashMap<>()));
    }

    @Test
    void expandDollarBangBraced() {
        Executor executor = new Executor();
        assertEquals("0", executor.expandVariables("${!}", new HashMap<>()));
    }

    @Test
    void expandTildeToHome() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/testuser");
        assertEquals("/home/testuser", Executor.expandTilde("~", env));
    }

    @Test
    void expandTildeWithPath() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("HOME", "/home/testuser");
        assertEquals("/home/testuser/file.txt", Executor.expandTilde("~/file.txt", env));
    }

    @Test
    void expandTildePlus() {
        Map<String, String> env = new HashMap<>();
        env.put("PWD", "/current/dir");
        assertEquals("/current/dir", Executor.expandTilde("~+", env));
    }

    @Test
    void expandTildeNone() {
        assertEquals("/foo/bar", Executor.expandTilde("/foo/bar", new HashMap<>()));
    }

    @Test
    void expandVarDefault() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("default", executor.expandVariables("${var:-default}", env));
    }

    @Test
    void expandVarDefaultSet() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "actual");
        assertEquals("actual", executor.expandVariables("${var:-default}", env));
    }

    @Test
    void expandVarDefaultEmpty() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "");
        assertEquals("default", executor.expandVariables("${var:-default}", env));
    }

    @Test
    void expandVarDefaultNullOnly() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("default", executor.expandVariables("${var-default}", env));
    }

    @Test
    void expandVarDefaultNullOnlySetSkips() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "value");
        assertEquals("value", executor.expandVariables("${var-default}", env));
    }

    @Test
    void expandVarDefaultNullOnlyEmptySkips() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "");
        assertEquals("", executor.expandVariables("${var-default}", env));
    }

    @Test
    void expandVarAssignDefault() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("assigned", executor.expandVariables("${var:=assigned}", env));
        assertEquals("assigned", env.get("var"));
    }

    @Test
    void expandVarAlternate() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "x");
        assertEquals("alt", executor.expandVariables("${var:+alt}", env));
    }

    @Test
    void expandVarAlternateUnset() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("", executor.expandVariables("${var:+alt}", env));
    }

    @Test
    void expandVarAssignNullOnly() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("assigned", executor.expandVariables("${var=assigned}", env));
        assertEquals("assigned", env.get("var"));
    }

    @Test
    void expandVarAssignNullOnlySetSkips() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "existing");
        assertEquals("existing", executor.expandVariables("${var=assigned}", env));
        assertEquals("existing", env.get("var"));
    }

    @Test
    void expandVarAlternateNullOnly() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "x");
        assertEquals("alt", executor.expandVariables("${var+alt}", env));
    }

    @Test
    void expandVarAlternateNullOnlyUnset() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("", executor.expandVariables("${var+alt}", env));
    }

    @Test
    void expandVarLength() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "hello");
        assertEquals("5", executor.expandVariables("${#var}", env));
    }

    @Test
    void expandVarLengthUnset() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("0", executor.expandVariables("${#var}", env));
    }

    @Test
    void expandVarSuffixShort() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "file.txt");
        assertEquals("file", executor.expandVariables("${var%.*}", env));
    }

    @Test
    void expandVarSuffixLong() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "a.b.c.txt");
        assertEquals("a", executor.expandVariables("${var%%.*}", env));
    }

    @Test
    void expandVarPrefixShort() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "foo/bar/baz");
        assertEquals("bar/baz", executor.expandVariables("${var#*/}", env));
    }

    @Test
    void expandVarPrefixLong() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "foo/bar/baz");
        assertEquals("baz", executor.expandVariables("${var##*/}", env));
    }

    @Test
    void expandVarPatternNoMatch() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("var", "hello");
        assertEquals("hello", executor.expandVariables("${var%x}", env));
    }

    @Test
    void commandSubstitutionEcho() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        String result = executor.expandVariables("$(echo hello)", env);
        assertEquals("hello", result);
    }

    @Test
    void commandSubstitutionInString() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        String result = executor.expandVariables("msg=$(echo hi)", env);
        assertEquals("msg=hi", result);
    }

    @Test
    void backtickSubstitution() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        String result = executor.expandVariables("`echo hello`", env);
        assertEquals("hello", result);
    }

    @Test
    void backtickSubstitutionInString() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        String result = executor.expandVariables("msg=`echo hi`", env);
        assertEquals("msg=hi", result);
    }

    @Test
    void normaliseBackticksSimple() {
        assertEquals("$(echo hello)", Executor.normaliseBackticks("`echo hello`"));
    }

    @Test
    void normaliseBackticksNoChange() {
        assertEquals("hello world", Executor.normaliseBackticks("hello world"));
    }

    @Test
    void normaliseBackticksSkipsDollarParen() {
        assertEquals("$(echo hi)", Executor.normaliseBackticks("$(echo hi)"));
    }

    @Test
    void normaliseBackticksEscaped() {
        assertEquals("$(echo `hi`)", Executor.normaliseBackticks("`echo \\`hi\\``"));
    }

    @Test
    void normaliseBackticksDollarEscape() {
        assertEquals("$(echo $PATH)", Executor.normaliseBackticks("`echo \\$PATH`"));
    }

    @Test
    void normaliseBackticksBackslashEscape() {
        assertEquals("$(echo a\\b)", Executor.normaliseBackticks("`echo a\\\\b`"));
    }

    @Test
    void normaliseBackticksNonSpecialEscape() {
        assertEquals("$(echo \\n)", Executor.normaliseBackticks("`echo \\\\n`"));
    }

    @Test
    void normaliseBackticksUnclosed() {
        assertEquals("`echo hello", Executor.normaliseBackticks("`echo hello"));
    }

    @Test
    void normaliseBackticksSkipsDollarBrace() {
        assertEquals("${var:-default}", Executor.normaliseBackticks("${var:-default}"));
    }

    @Test
    void normaliseBackticksSkipsNestedDollarBrace() {
        assertEquals("${var:-${other}}", Executor.normaliseBackticks("${var:-${other}}"));
    }

    @Test
    void normaliseBackticksSkipsTripleNestedDollarBrace() {
        assertEquals("${a:-${b:-${c}}}", Executor.normaliseBackticks("${a:-${b:-${c}}}"));
    }

    @Test
    void normaliseBackticksDollarAtEnd() {
        assertEquals("foo$", Executor.normaliseBackticks("foo$"));
    }

    @Test
    void normaliseBackticksUnmatchedDollarBrace() {
        assertEquals("${unclosed", Executor.normaliseBackticks("${unclosed"));
    }

    @Test
    void normaliseBackticksDollarFollowedByLetter() {
        assertEquals("$VAR", Executor.normaliseBackticks("$VAR"));
    }

    @Test
    void normaliseBackticksBackslashAtEndOfInput() {
        assertEquals("`abc\\", Executor.normaliseBackticks("`abc\\"));
    }

    @Test
    void globMatchesQuestionMarkInCase() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("case ab in a?) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("MATCHED"));
    }

    @Test
    void globMatchesQuestionMarkNoMatch() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("case abc in a?) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassInCase() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("case b in [abc]) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("MATCHED"));
    }

    @Test
    void globMatchesCharClassNoMatch() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("case z in [abc]) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassRangeInCase() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("case m in [a-z]) echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("MATCHED"));
    }

    @Test
    void globMatchesUnclosedBracketInCase() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().trim().equals("MATCHED"));
    }

    @Test
    void backtickNestedNormalise() {
        String result = Executor.normaliseBackticks("`echo \\`echo hi\\``");
        assertEquals("$(echo `echo hi`)", result);
    }

    @Test
    void echoBuiltin() {
        Executor executor = new Executor();
        ListNode list = parser("echo hello").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void echoBuiltinArgs() {
        Executor executor = new Executor();
        ListNode list = parser("echo hello world").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void echoBuiltinNoNewline() {
        Executor executor = new Executor();
        ListNode list = parser("echo -n hello").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void echoBuiltinEmpty() {
        Executor executor = new Executor();
        ListNode list = parser("echo").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void arithmeticSimple() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("7", executor.expandVariables("$((3 + 4))", env));
    }

    @Test
    void arithmeticMul() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("15", executor.expandVariables("$((3 * 5))", env));
    }

    @Test
    void arithmeticVar() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        assertEquals("15", executor.expandVariables("$((x + 5))", env));
    }

    @Test
    void arithmeticNested() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("14", executor.expandVariables("$((2 * (3 + 4)))", env));
    }

    @Test
    void arithmeticShiftLeft() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("32", executor.expandVariables("$((4 << 3))", env));
    }

    @Test
    void arithmeticShiftRight() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("4", executor.expandVariables("$((32 >> 3))", env));
    }

    @Test
    void arithmeticLessThan() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((3 < 5))", env));
        assertEquals("0", executor.expandVariables("$((5 < 3))", env));
    }

    @Test
    void arithmeticLessEqual() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((5 <= 5))", env));
        assertEquals("0", executor.expandVariables("$((6 <= 5))", env));
    }

    @Test
    void arithmeticGreaterThan() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((5 > 3))", env));
        assertEquals("0", executor.expandVariables("$((3 > 5))", env));
    }

    @Test
    void arithmeticGreaterEqual() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((5 >= 5))", env));
        assertEquals("0", executor.expandVariables("$((4 >= 5))", env));
    }

    @Test
    void arithmeticEqualEqual() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((7 == 7))", env));
        assertEquals("0", executor.expandVariables("$((7 == 8))", env));
    }

    @Test
    void arithmeticNotEqual() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((7 != 8))", env));
        assertEquals("0", executor.expandVariables("$((7 != 7))", env));
    }

    @Test
    void arithmeticBitAnd() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("4", executor.expandVariables("$((12 & 6))", env));
    }

    @Test
    void arithmeticBitXor() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("10", executor.expandVariables("$((12 ^ 6))", env));
    }

    @Test
    void arithmeticBitOr() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("14", executor.expandVariables("$((12 | 6))", env));
    }

    @Test
    void arithmeticLogicalAnd() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((1 && 2))", env));
        assertEquals("0", executor.expandVariables("$((0 && 2))", env));
    }

    @Test
    void arithmeticLogicalOr() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((0 || 5))", env));
        assertEquals("0", executor.expandVariables("$((0 || 0))", env));
    }

    @Test
    void arithmeticUnaryNot() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((!0))", env));
        assertEquals("0", executor.expandVariables("$((!5))", env));
    }

    @Test
    void arithmeticUnaryBitNot() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("-1", executor.expandVariables("$((~0))", env));
        assertEquals("-6", executor.expandVariables("$((~5))", env));
    }

    @Test
    void arithmeticPrecedenceMixed() {
        Executor executor = new Executor();
        Map<String, String> env = new HashMap<>();
        assertEquals("1", executor.expandVariables("$((1 + 2 == 3))", env));
        assertEquals("1", executor.expandVariables("$((1 < 2 && 3 < 4))", env));
        assertEquals("7", executor.expandVariables("$((1 | 2 | 4 & 6))", env));
    }

    @Test
    void defaultIfsSet() {
        Executor executor = new Executor();
        assertEquals(" \t\n", executor.getVariable("IFS"));
    }

    @Test
    void defaultHomeSet() {
        Executor executor = new Executor();
        assertEquals(System.getProperty("user.home"), executor.getVariable("HOME"));
    }

    @Test
    void defaultPathSet() {
        Map<String, String> env = new HashMap<>();
        env.put("PATH", "");
        Executor executor = new Executor(env);
        assertEquals("/usr/local/bin:/usr/bin:/bin", executor.getVariable("PATH"));
    }

    @Test
    void defaultsDoNotOverrideExistingEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("HOME", "/custom/home");
        env.put("PATH", "/custom/path");
        Executor executor = new Executor(env);
        assertEquals("/custom/home", executor.getVariable("HOME"));
        assertEquals("/custom/path", executor.getVariable("PATH"));
        assertEquals(" \t\n", executor.getVariable("IFS"));
    }

    @Test
    void commandFoundInPath() {
        Executor executor = new Executor();
        ListNode list = parser("true").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void commandNotFoundReturns127() {
        Executor executor = new Executor();
        ListNode list = parser("nonexistent_cmd_xyz_127").parse();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void commandWithSlashFound() {
        Map<String, String> env = new HashMap<>();
        Executor executor = new Executor(env);
        ListNode list = parser("/user/.nix-profile/bin/ls").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void commandFoundInCustomPath() throws IOException {
        File dir = new File("/tmp/sh4j_path_test_" + System.nanoTime());
        dir.mkdirs();
        dir.deleteOnExit();
        File script = new File(dir, "mycmd");
        try (FileWriter w = new FileWriter(script)) {
            w.write("#!/bin/sh\nexit 42\n");
        }
        script.setExecutable(true);
        Map<String, String> env = new HashMap<>();
        env.put("PATH", dir.getAbsolutePath());
        Executor executor = new Executor(env);
        ListNode list = parser("mycmd").parse();
        int exitCode = executor.execute(list);
        dir.delete();
        assertEquals(42, exitCode);
    }

    @Test
    void commandNotFoundEmptyPath() {
        Map<String, String> env = new HashMap<>();
        env.put("PATH", "");
        Executor executor = new Executor(env);
        ListNode list = parser("somecmd").parse();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void commandFoundInSecondPathDir() throws IOException {
        File dir = new File("/tmp/sh4j_path_test2_" + System.nanoTime());
        dir.mkdirs();
        dir.deleteOnExit();
        File script = new File(dir, "mycmd2");
        try (FileWriter w = new FileWriter(script)) {
            w.write("#!/bin/sh\nexit 7\n");
        }
        script.setExecutable(true);
        Map<String, String> env = new HashMap<>();
        env.put("PATH", "/nonexistent_test_dir_xyz:" + dir.getAbsolutePath());
        Executor executor = new Executor(env);
        ListNode list = parser("mycmd2").parse();
        int exitCode = executor.execute(list);
        dir.delete();
        assertEquals(7, exitCode);
    }

    @Test
    void commandWithSlashNotFound() {
        Executor executor = new Executor();
        ListNode list = parser("/nonexistent_path_xyz_abc/cmd").parse();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void exportWithValue() {
        ListNode list = parser("export FOO=bar").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void exportExistingVariable() {
        Executor executor = new Executor();
        ListNode list = parser("export FOO").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void exportThenVariableExpands() {
        Executor executor = new Executor();
        ListNode list = parser("export MSG=hello; echo $MSG").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("hello", executor.getVariable("MSG"));
    }

    @Test
    void splitFieldsDefaultIfs() {
        assertEquals(List.of("a", "b", "c"), Executor.splitFields("a b c", " \t\n"));
    }

    @Test
    void splitFieldsMultipleWs() {
        assertEquals(List.of("a", "b"), Executor.splitFields("a   b", " \t\n"));
    }

    @Test
    void splitFieldsLeadingWs() {
        assertEquals(List.of("a", "b"), Executor.splitFields("  a b", " \t\n"));
    }

    @Test
    void splitFieldsTrailingWs() {
        assertEquals(List.of("a", "b"), Executor.splitFields("a b  ", " \t\n"));
    }

    @Test
    void splitFieldsColon() {
        assertEquals(List.of("a", "b", "c"), Executor.splitFields("a:b:c", ":"));
    }

    @Test
    void splitFieldsEmptyBetweenColons() {
        assertEquals(List.of("a", "", "b"), Executor.splitFields("a::b", ":"));
    }

    @Test
    void splitFieldsOnlyColons() {
        assertEquals(List.of("", ""), Executor.splitFields(":", ":"));
    }

    @Test
    void splitFieldsDoubleColon() {
        assertEquals(List.of("", "", ""), Executor.splitFields("::", ":"));
    }

    @Test
    void splitFieldsNoDelim() {
        assertEquals(List.of("hello"), Executor.splitFields("hello", ":"));
    }

    @Test
    void splitFieldsEmptyIfs() {
        assertEquals(List.of("hello world"), Executor.splitFields("hello world", ""));
    }

    @Test
    void splitFieldsNullIfs() {
        assertEquals(List.of("hello"), Executor.splitFields("hello", null));
    }

    @Test
    void splitFieldsEmptyValue() {
        assertEquals(List.of(), Executor.splitFields("", " \t\n"));
    }

    @Test
    void splitFieldsMixedWsNws() {
        assertEquals(List.of("a", "", "b"), Executor.splitFields("a :b", " :"));
    }

    @Test
    void splitFieldsTrailingColon() {
        assertEquals(List.of("a", ""), Executor.splitFields("a:", ":"));
    }

    @Test
    void splitFieldsCustomNws() {
        assertEquals(List.of("a", "b", "c"), Executor.splitFields("a,b,c", ","));
    }

    @Test
    void isQuotedDoubleQuotes() {
        assertTrue(Executor.isQuoted("\"hello\""));
    }

    @Test
    void isQuotedSingleQuotes() {
        assertTrue(Executor.isQuoted("'hello'"));
    }

    @Test
    void isQuotedNoQuotes() {
        assertFalse(Executor.isQuoted("hello"));
    }

    @Test
    void isQuotedPartial() {
        assertFalse(Executor.isQuoted("a\"b\"c"));
    }

    @Test
    void stripQuotesDouble() {
        assertEquals("hello", Executor.stripQuotes("\"hello\""));
    }

    @Test
    void stripQuotesSingle() {
        assertEquals("hello", Executor.stripQuotes("'hello'"));
    }

    @Test
    void stripQuotesNone() {
        assertEquals("hello", Executor.stripQuotes("hello"));
    }

    @Test
    void removeQuotesNoop() {
        assertEquals("hello", Executor.removeQuotes("hello"));
    }

    @Test
    void removeQuotesDouble() {
        assertEquals("hello world", Executor.removeQuotes("\"hello world\""));
    }

    @Test
    void removeQuotesSingle() {
        assertEquals("hello", Executor.removeQuotes("'hello'"));
    }

    @Test
    void removeQuotesPartialDouble() {
        assertEquals("foovaluebar", Executor.removeQuotes("foo\"value\"bar"));
    }

    @Test
    void removeQuotesPartialSingle() {
        assertEquals("helloworld", Executor.removeQuotes("hello'world'"));
    }

    @Test
    void removeQuotesApostropheInsideDouble() {
        assertEquals("it's", Executor.removeQuotes("\"it's\""));
    }

    @Test
    void removeQuotesNestedQuotes() {
        assertEquals("hello \"there\" world", Executor.removeQuotes("\"hello \\\"there\\\" world\""));
    }

    @Test
    void removeQuotesBackslashOutside() {
        assertEquals("hello world", Executor.removeQuotes("hello\\ world"));
    }

    @Test
    void removeQuotesBackslashInDouble() {
        assertEquals("hello\"world", Executor.removeQuotes("\"hello\\\"world\""));
    }

    @Test
    void removeQuotesEmptyQuotes() {
        assertEquals("", Executor.removeQuotes("\"\""));
        assertEquals("", Executor.removeQuotes("''"));
    }

    @Test
    void echoQuotedVarNoSplit() {
        Executor executor = new Executor(new HashMap<>());
        ListNode list = parser("MSG=\"hello  world\"; echo \"$MSG\"").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void echoUnquotedVarSplit() {
        Executor executor = new Executor(new HashMap<>());
        ListNode list = parser("MSG=\"hello  world\"; echo $MSG").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void echoQuotedLiteralNoSplit() {
        Executor executor = new Executor();
        ListNode list = parser("echo \"hello   world\"").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void echoCustomIfsColon() {
        Map<String, String> env = new HashMap<>();
        env.put("IFS", ":");
        Executor executor = new Executor(env);
        ListNode list = parser("echo a:b:c").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void echoEmptyIfsNoSplit() {
        Map<String, String> env = new HashMap<>();
        env.put("IFS", "");
        Executor executor = new Executor(env);
        ListNode list = parser("echo hello   world").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void splitFieldsTabIfs() {
        assertEquals(List.of("a", "b"), Executor.splitFields("a\tb", "\t"));
    }

    @Test
    void splitFieldsMixedIfs() {
        assertEquals(List.of("a", "b", "c"), Executor.splitFields("a:b c", ": "));
    }

    @Test
    void containsGlobCharsStar() {
        assertTrue(Executor.containsGlobChars("*.txt"));
    }

    @Test
    void containsGlobCharsQuestion() {
        assertTrue(Executor.containsGlobChars("file?"));
    }

    @Test
    void containsGlobCharsBracket() {
        assertTrue(Executor.containsGlobChars("[ab]"));
    }

    @Test
    void containsGlobCharsNone() {
        assertFalse(Executor.containsGlobChars("hello.txt"));
    }

    @Test
    void pathnameExpandNoGlob() {
        assertEquals(List.of("hello"), Executor.pathnameExpand(List.of("hello")));
    }

    @Test
    void pathnameExpandGlobNoMatchKeepsPattern() {
        List<String> result = Executor.pathnameExpand(List.of("__no_match__xyz__"));
        assertEquals(List.of("__no_match__xyz__"), result);
    }

    @Test
    void pathnameExpandGlobWithMatch() {
        List<String> result = Executor.pathnameExpand(List.of("pom.xml"));
        assertEquals(List.of("pom.xml"), result);
    }

    @Test
    void pathnameExpandWildcardMatch() {
        List<String> result = Executor.pathnameExpand(List.of("*.xml"));
        assertTrue(result.contains("pom.xml"));
    }

    @Test
    void pathnameExpandGlobQuoted() {
        Executor executor = new Executor();
        ListNode list = parser("echo \"*.java\"").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void pathnameExpandGlobUnquoted() {
        Executor executor = new Executor();
        ListNode list = parser("echo *.java").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void expandBracedColonQuestionUnset() {
        Executor executor = new Executor(new HashMap<>());
        assertEquals("", executor.expandVariables("${x:?my error}", new HashMap<>()));
    }

    @Test
    void expandBracedQuestionUnset() {
        Executor executor = new Executor(new HashMap<>());
        assertEquals("", executor.expandVariables("${x?my error}", new HashMap<>()));
    }

    @Test
    void expandBracedColonQuestionSet() {
        Executor executor = new Executor(new HashMap<>());
        Map<String, String> env = new HashMap<>(Map.of("x", "value"));
        assertEquals("value", executor.expandVariables("${x:?not used}", env));
    }

    @Test
    void expandBracedColonQuestionDefaultMsg() {
        Executor executor = new Executor(new HashMap<>());
        executor.expandVariables("${x:?}", new HashMap<>());
    }

    @Test
    void expandBracedColonQuestionErrorStream() {
        Executor executor = new Executor(new HashMap<>());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setErrorStream(new PrintStream(bos, true));
        executor.expandVariables("${x:?my error}", new HashMap<>());
        String output = bos.toString().trim();
        assertTrue(output.contains("my error"), "error message should go to injected stream");
    }

    @Test
    void cdToDirectory() throws Exception {
        String saved = System.getProperty("user.dir");
        try {
            ListNode list = parser("cd /tmp").parse();
            Executor executor = new Executor();
            assertEquals(0, executor.execute(list));
            assertEquals("/tmp", executor.getVariable("PWD"));
        } finally {
            System.setProperty("user.dir", saved);
        }
    }

    @Test
    void cdToHome() throws Exception {
        String saved = System.getProperty("user.dir");
        try {
            Map<String, String> env = new HashMap<>();
            env.put("HOME", "/tmp");
            Executor executor = new Executor(env);
            ListNode list = parser("cd").parse();
            assertEquals(0, executor.execute(list));
            assertEquals("/tmp", executor.getVariable("PWD"));
        } finally {
            System.setProperty("user.dir", saved);
        }
    }

    @Test
    void cdToNonexistentReturns1() {
        ListNode list = parser("cd /sh4j_nonexistent_dir_xyz").parse();
        Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void cdMinus() throws Exception {
        String saved = System.getProperty("user.dir");
        try {
            Map<String, String> env = new HashMap<>();
            env.put("PWD", "/tmp");
            env.put("OLDPWD", "/");
            Executor executor = new Executor(env);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            executor.setCaptureStream(bos);
            ListNode list = parser("cd -").parse();
            assertEquals(0, executor.execute(list));
            assertEquals("/", executor.getVariable("PWD"));
            assertEquals("/tmp", executor.getVariable("OLDPWD"));
            assertEquals("/", bos.toString().trim());
        } finally {
            System.setProperty("user.dir", saved);
        }
    }

    @Test
    void cdWithExpandedVar() throws Exception {
        String saved = System.getProperty("user.dir");
        try {
            Map<String, String> env = new HashMap<>();
            env.put("MYDIR", "/tmp");
            Executor executor = new Executor(env);
            ListNode list = parser("cd $MYDIR").parse();
            assertEquals(0, executor.execute(list));
            assertEquals("/tmp", executor.getVariable("PWD"));
        } finally {
            System.setProperty("user.dir", saved);
        }
    }

    @Test
    void typeBuiltin() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("type echo cd exit export type true false shift unset").parse();
        assertEquals(0, executor.execute(list));
        String out = bos.toString();
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
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("type true").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().contains("true is "));
    }

    @Test
    void typeNotFound() {
        Executor executor = new Executor();
        ListNode list = parser("type nonexistent_cmd_xyz").parse();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void execBuiltinRunsCommand() {
        Executor executor = new Executor();
        ListNode list = parser("exec true").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void execBuiltinNotFoundReturns127() {
        Executor executor = new Executor();
        ListNode list = parser("exec nonexistent_cmd_xyz").parse();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void execBuiltinNoArgsIsNoop() {
        Executor executor = new Executor();
        ListNode list = parser("exec").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void trueBuiltinReturns0() {
        Executor executor = new Executor();
        ListNode list = parser("true").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void falseBuiltinReturns1() {
        Executor executor = new Executor();
        ListNode list = parser("false").parse();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void unsetRemovesVariable() {
        Map<String, String> env = new HashMap<>();
        env.put("MYVAR", "value");
        Executor executor = new Executor(env);
        ListNode list = parser("unset MYVAR").parse();
        assertEquals(0, executor.execute(list));
        assertNull(executor.getVariable("MYVAR"));
    }

    @Test
    void shiftRemovesPositionalParam() {
        List<String> params = new ArrayList<>(List.of("sh", "a", "b", "c"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor(new HashMap<>(), params);
        executor.setCaptureStream(bos);
        ListNode list = parser("shift; echo $#").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("2", bos.toString().trim());
    }

    @Test
    void shiftWithCount() {
        List<String> params = new ArrayList<>(List.of("sh", "a", "b", "c"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor(new HashMap<>(), params);
        executor.setCaptureStream(bos);
        ListNode list = parser("shift 2; echo $#").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("1", bos.toString().trim());
    }

    @Test
    void breakStopsWhileLoop() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("while exit $?; do echo ran; break; exit 1; done").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("ran", bos.toString().trim());
    }

    @Test
    void breakStopsForLoop() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("for i in a b c; do echo $i; break; done").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("a", bos.toString().trim());
    }

    @Test
    void continueInForLoop() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("for i in a b; do echo $i; continue; echo after; done").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("a\nb", bos.toString().trim());
        assertFalse(bos.toString().contains("after"));
    }

    @Test
    void colonBuiltinReturns0() {
        Executor executor = new Executor();
        ListNode list = parser(":").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void evalBuiltinRunsCommand() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("eval echo hello world").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("hello world", bos.toString().trim());
    }

    @Test
    void evalBuiltinNoArgsReturns0() {
        Executor executor = new Executor();
        ListNode list = parser("eval").parse();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void externalOutputRedirect() throws Exception {
        String inputPath = "/tmp/sh4j_ext_in.txt";
        String outputPath = "/tmp/sh4j_ext_out.txt";
        new java.io.File(outputPath).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("hello world"); }
        Executor executor = new Executor();
        ListNode list = parser("cat < " + inputPath + " > " + outputPath).parse();
        assertEquals(0, executor.execute(list));
        assertTrue(new java.io.File(outputPath).exists());
        assertEquals("hello world", java.nio.file.Files.readString(java.nio.file.Path.of(outputPath)).trim());
        new java.io.File(inputPath).delete();
        new java.io.File(outputPath).delete();
    }

    @Test
    void externalAppendRedirect() throws Exception {
        String inputPath = "/tmp/sh4j_ext_append_in.txt";
        String outputPath = "/tmp/sh4j_ext_append_out.txt";
        new java.io.File(outputPath).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("second"); }
        try (java.io.FileWriter w = new java.io.FileWriter(outputPath)) { w.write("first\n"); }
        Executor executor = new Executor();
        ListNode list = parser("cat < " + inputPath + " >> " + outputPath).parse();
        assertEquals(0, executor.execute(list));
        String content = java.nio.file.Files.readString(java.nio.file.Path.of(outputPath)).trim();
        assertTrue(content.startsWith("first"));
        assertTrue(content.contains("second"));
        new java.io.File(inputPath).delete();
        new java.io.File(outputPath).delete();
    }

    @Test
    void externalClobberRedirect() throws Exception {
        String inputPath = "/tmp/sh4j_ext_clobber_in.txt";
        String outputPath = "/tmp/sh4j_ext_clobber_out.txt";
        new java.io.File(outputPath).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("hello clobber"); }
        Executor executor = new Executor();
        ListNode list = parser("cat < " + inputPath + " >| " + outputPath).parse();
        assertEquals(0, executor.execute(list));
        assertTrue(new java.io.File(outputPath).exists());
        assertEquals("hello clobber", java.nio.file.Files.readString(java.nio.file.Path.of(outputPath)).trim());
        new java.io.File(inputPath).delete();
        new java.io.File(outputPath).delete();
    }

    @Test
    void externalRedirectStdoutToStderr() throws Exception {
        String inputPath = "/tmp/sh4j_ext_12_in.txt";
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("hello"); }
        Executor executor = new Executor();
        ListNode list = parser("cat < " + inputPath + " 1>&2").parse();
        assertEquals(0, executor.execute(list));
        new java.io.File(inputPath).delete();
    }

    @Test
    void externalRedirectStderrToStdout() throws Exception {
        String inputPath = "/tmp/sh4j_ext_21_in.txt";
        try (java.io.FileWriter w = new java.io.FileWriter(inputPath)) { w.write("hello"); }
        Executor executor = new Executor();
        ListNode list = parser("cat < " + inputPath + " 2>&1").parse();
        assertEquals(0, executor.execute(list));
        new java.io.File(inputPath).delete();
    }

    @Test
    void externalCloseStdout() {
        ListNode list = parser("true 1>&-").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void externalCloseStderr() {
        ListNode list = parser("true 2>&-").parse();
        Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void externalStderrOutputRedirect() throws Exception {
        String path = "/tmp/sh4j_ext_err_out.txt";
        new java.io.File(path).delete();
        Executor executor = new Executor();
        ListNode list = parser("cat /nonexistent_path_xyz_123 2> " + path).parse();
        executor.execute(list);
        assertTrue(new java.io.File(path).exists());
        new java.io.File(path).delete();
    }

    @Test
    void externalStderrAppendRedirect() throws Exception {
        String path = "/tmp/sh4j_ext_err_append.txt";
        new java.io.File(path).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(path)) { w.write("initial\n"); }
        Executor executor = new Executor();
        ListNode list = parser("cat /nonexistent_path_xyz_456 2>> " + path).parse();
        executor.execute(list);
        String content = java.nio.file.Files.readString(java.nio.file.Path.of(path));
        assertTrue(content.contains("initial"));
        new java.io.File(path).delete();
    }

    @Test
    void externalReadWriteRedirect() throws Exception {
        String path = "/tmp/sh4j_ext_rw.txt";
        try (java.io.FileWriter w = new java.io.FileWriter(path)) { w.write("hello"); }
        Executor executor = new Executor();
        ListNode list = parser("cat <> " + path).parse();
        assertEquals(0, executor.execute(list));
        new java.io.File(path).delete();
    }

    @Test
    void heredocStripTabsBasic() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("cat <<- EOF\n\thello\nEOF\n").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("hello", bos.toString().trim());
    }

    @Test
    void heredocStripTabsMultiple() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("cat <<- EOF\n\t\thello\nEOF\n").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("hello", bos.toString().trim());
    }

    @Test
    void heredocStripTabsDelimiterWithTabs() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("cat <<- EOF\n\thello\n\tEOF\n").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("hello", bos.toString().trim());
    }

    @Test
    void heredocStripTabsMixed() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("cat <<- MARKER\n\tfirst\nsecond\n\tthird\nMARKER\n").parse();
        assertEquals(0, executor.execute(list));
        assertEquals("first\nsecond\nthird", bos.toString().trim());
    }

    @Test
    void typeFunction() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Executor executor = new Executor();
        executor.setCaptureStream(bos);
        ListNode list = parser("myfunc() { true; }; type myfunc").parse();
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString().contains("myfunc is a function"));
    }

    @Test
    void shiftNonNumericReturns1() {
        Executor executor = new Executor();
        ListNode list = parser("shift abc").parse();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void shiftNegativeReturns1() {
        Executor executor = new Executor();
        ListNode list = parser("shift -1").parse();
        assertEquals(1, executor.execute(list));
    }
}
