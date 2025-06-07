/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.CaseNode;
import io.github.andreoss.sh4j.parser.ForNode;
import io.github.andreoss.sh4j.parser.FunctionNode;
import io.github.andreoss.sh4j.parser.IfNode;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import io.github.andreoss.sh4j.parser.Subshell;
import io.github.andreoss.sh4j.parser.WhileNode;
import static org.junit.jupiter.api.Assertions.*;

final class ExecutorTest {

    @Test
    void executeSimpleEcho() {
        final ListNode list = this.parse("echo hello");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executePwd() {
        final ListNode list = this.parse("pwd");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeTrue() {
        final ListNode list = this.parse("true");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFalse() {
        final ListNode list = this.parse("false");
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeWithArguments() {
        final ListNode list = this.parse("echo a b c");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSemicolonSeparator() {
        final ListNode list = this.parse("echo a; echo b");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAndIf() {
        final ListNode list = this.parse("echo a && echo b");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeOrIf() {
        final ListNode list = this.parse("false || echo b");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAndIfStopsOnFailure() {
        final ListNode list = this.parse("false && echo not reached");
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeOrIfStopsOnSuccess() {
        final ListNode list = this.parse("echo success || echo not reached");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeEmptyList() {
        final ListNode list = this.parse("");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeAssignmentOnly() {
        final ListNode list = this.parse("FOO=bar");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void executeAssignmentWithCommand() {
        final ListNode list = this.parse("FOO=hello echo $FOO");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeMultipleAssignments() {
        final ListNode list = this.parse("A=1 B=2");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        assertEquals("1", executor.getVariable("A"));
        assertEquals("2", executor.getVariable("B"));
    }

    @Test
    void executeVariableExpansion() {
        final ListNode list = this.parse("echo $HOME");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeOutputRedirection() throws Exception {
        final ListNode list = this.parse("echo test > /tmp/sh4j_test_output");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeInputRedirection() throws Exception {
        final ListNode list = this.parse("cat < /dev/null");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executePipeline() {
        final ListNode list = this.parse("echo hello | cat");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBackground() {
        final ListNode list = this.parse("echo hello &");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBackgroundSetsLastBgPid() {
        final ListNode list = this.parse("echo hello &");
        final Executor executor = new Executor();
        executor.execute(list);
        assertTrue(executor.getLastBgPid() > 0, "lastBgPid should be > 0 after & command");
    }

    @Test
    void executeBackgroundAndSequential() {
        final ListNode list = this.parse("echo bg1 & echo seq1");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeClobberRedirection() throws Exception {
        final ListNode list = this.parse("echo test >| /tmp/sh4j_clobber_test");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrRedirect() throws Exception {
        final ListNode list = this.parse("echo hello 2>/dev/null");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrToStdout() throws Exception {
        final ListNode list = this.parse("echo hello 2>&1");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrRedirectToFile() throws Exception {
        final String file = "/tmp/sh4j_stderr_test";
        final ListNode list = this.parse(String.format("echo hello 2>%s", file));
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stderr_append_%s", System.nanoTime());
        final ListNode list = this.parse(String.format("echo hello 2>>%s", file));
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stdout_append_%s", System.nanoTime());
        final ListNode list = this.parse(String.format("echo hello >>%s", file));
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutRedirectIOException() throws Exception {
        final ListNode list = this.parse("echo hello >/dev/null/no_such_path_xyz");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrRedirectIOException() throws Exception {
        final ListNode list = this.parse("echo hello 2>/dev/null/no_such_stderr_xyz");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStdoutAppendIOException() throws Exception {
        final ListNode list = this.parse("echo hello >>/dev/null/no_such_append_xyz");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinStderrAppendIOException() throws Exception {
        final ListNode list = this.parse("echo hello 2>>/dev/null/no_such_eappend_xyz");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBuiltinCloseStdout() {
        final ListNode list = this.parse("echo hello >&-");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeMultiplePipelines() {
        final ListNode list = this.parse("echo a; echo b; echo c");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeBangPipeline() {
        final ListNode list = this.parse("! false");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeVariableWithEmptyValue() {
        final ListNode list = this.parse("echo $EMPTY_VAR");
        final Map<String, String> env = new HashMap<>();
        env.put("EMPTY_VAR", "");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCommandNotFound() {
        final ListNode list = this.parse("nonexistent_command_xyz");
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void executeCommandNotFoundReturnsNotFound() {
        final ListNode list = this.parse("nonexistent_cmd_xyz_127");
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(list));
    }

    @Test
    void executeBangWithTrue() {
        final ListNode list = this.parse("! true");
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeExitZero() {
        final ListNode list = this.parse("exit 0");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeExitOne() {
        final ListNode list = this.parse("exit 1");
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeEchoAbc() {
        final ListNode list = this.parse("echo abc");
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
        final ListNode list = this.parse("echo test >> /tmp/sh4j_append_test");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeStderrRedirect() throws Exception {
        final ListNode list = this.parse("echo test 2>/tmp/sh4j_test_stderr");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeStderrToStdout() throws Exception {
        final ListNode list = this.parse("echo test 2>&1");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCloseStdin() throws Exception {
        final ListNode list = this.parse("cat <&-");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCloseStdout() throws Exception {
        final ListNode list = this.parse("echo test >&-");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeReadWriteRedirect() throws Exception {
        final ListNode list = this.parse("cat <> /dev/null");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeExitNoArgs() {
        final ListNode list = this.parse("exit");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeExitWithNonNumericArg() {
        final ListNode list = this.parse("exit xyz");
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
        final ListNode list = this.parse("echo hello | nonexistent_cmd_xyz_fail | echo done");
        final Executor executor = new Executor();
        final int code = executor.execute(list);
        assertTrue(code == 127 || code == 126,
                "Pipeline with missing middle stage must exit with POSIX "
                        + String.format("command-not-found(127) or not-executable(126); got %s", code));
    }

    @Test
    void executeSubshell() {
        final ListNode list = this.parse("(echo hello)");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSubshellWithMultiplePipelines() {
        final ListNode list = this.parse("(echo a; echo b)");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSubshellInPipeline() {
        final ListNode list = this.parse("echo foo | (cat) | wc");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeSubshellWithExit() {
        final ListNode list = this.parse("(exit 42)");
        final Executor executor = new Executor();
        assertEquals(42, executor.execute(list));
    }

    @Test
    void executeGrouping() {
        final ListNode list = this.parse("{ echo hello; }");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeGroupingInPipeline() {
        final ListNode list = this.parse("echo foo | { cat; } | wc");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeGroupingSharesEnvironment() {
        final ListNode list = this.parse("FOO=bar; { echo $FOO; }");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeIfTrueThen() {
        final ListNode list = this.parse("if true; then echo hi; fi");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeIfFalseElse() {
        final ListNode list = this.parse("if false; then echo a; else echo b; fi");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeIfTrueThenElseNotExecuted() {
        final ListNode list = this.parse("if true; then echo hi; else false; fi");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeForDoDone() {
        final ListNode list = this.parse("for var in a b c; do echo $var; done");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeWhileFalse() {
        final ListNode list = this.parse("while false; do echo hi; done");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeUntilTrue() {
        final ListNode list = this.parse("until true; do echo hi; done");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseMatch() {
        final ListNode list = this.parse("case a in a) echo match;; esac");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseNoMatch() {
        final ListNode list = this.parse("case a in b) echo nomatch;; esac");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseDefaultStar() {
        final ListNode list = this.parse("case a in *) echo default;; esac");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCasePipePattern() {
        final ListNode list = this.parse("case a in b|c|a) echo match;; esac");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeCaseFirstMatchOnly() {
        final ListNode list = this.parse("case a in a) echo first;; b) echo second;; esac");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFunctionDefinitionStored() {
        final ListNode list = this.parse("foo() { echo hello; }");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFunctionCall() {
        final ListNode list = this.parse("foo() { echo hello; }; foo");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeFunctionSubshellBody() {
        final ListNode list = this.parse("foo() (echo hello); foo");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeWhileLoop() {
        final ListNode list = this.parse("while false; do echo body; done");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeUntilLoop() {
        final ListNode list = this.parse("until true; do echo body; done");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeWhileBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("while exit $?; do echo ran; exit 1; done");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(1, executor.execute(list));
        assertEquals("ran\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeUntilBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("exit 1; until exit $?; do echo ran; exit 0; done");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("ran\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeWhileBodyExitCodePropagated() {
        final ListNode list = this.parse("while exit $?; do exit 42; done");
        final Executor executor = new Executor();
        assertEquals(42, executor.execute(list));
    }

    @Test
    void commandInterruptedException() {
        final ListNode list = this.parse("pwd");
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void pipelineInterruptedException() {
        final ListNode list = this.parse("pwd | cat");
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void waitForBackgroundJobsInterrupted() {
        final ListNode list = this.parse("sleep 1 &");
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        executor.execute(list);
        assertTrue(Thread.interrupted(), "InterruptedException catch should re-set interrupt flag");
    }

    @Test
    void exportNoArgs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_EXPORT_VAR", "hello");
        final Executor executor = new Executor(env);
        executor.setOutputStream(new PrintStream(bos, true));
        final ListNode list = this.parse("export");
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString(StandardCharsets.UTF_8).contains("export TEST_EXPORT_VAR=hello"));
    }

    @Test
    void executeIfFalseNoElse() {
        final ListNode list = this.parse("if false; then echo yes; fi");
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
    }

    @Test
    void executeElifWhenIfFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("if false; then echo no; elif true; then echo yes; fi");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("yes\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeElifChainFalseFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("if false; then echo a; elif false; then echo b; else echo c; fi");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("c\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeElifChainSecondElifTrue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("if false; then echo a; elif false; then echo b; elif true; then echo c; fi");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals("c\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeRedirectStdoutToStderr() {
        final ListNode list = this.parse("echo test 1>&2");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void executeHereDoc() {
        final ListNode list = this.parse("cat << EOF\nhello\nEOF\n");
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(list));
    }

    @Test
    void heredocExpansion() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        final ListNode list = this.parse("cat << EOF\nhello $TEST_HVAR\nEOF\n");
        assertEquals(0, executor.execute(list));
        assertEquals("hello expanded", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void redirectTargetExpansionOutput() throws Exception {
        final java.io.File f = new java.io.File("/tmp/sh4j_redir_expand_test.txt");
        f.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        final Executor executor = new Executor(env);
        final ListNode list = this.parse("echo hello > $TEST_FILE");
        assertEquals(0, executor.execute(list));
        assertTrue(f.exists());
        assertEquals("hello\n", new String(java.nio.file.Files.readAllBytes(f.toPath())));
        f.delete();
    }

    @Test
    void redirectTargetExpansionAppend() throws Exception {
        final java.io.File f = new java.io.File("/tmp/sh4j_redir_append_expand.txt");
        f.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        final Executor executor = new Executor(env);
        final ListNode list = this.parse("echo hello >> $TEST_FILE");
        assertEquals(0, executor.execute(list));
        assertTrue(f.exists());
        assertEquals("hello\n", new String(java.nio.file.Files.readAllBytes(f.toPath())));
        f.delete();
    }

    @Test
    void redirectTargetExpansionInput() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_redir_input_expand.txt");
        file.delete();
        try (java.io.FileWriter w = new java.io.FileWriter(file, StandardCharsets.UTF_8)) {
            w.write("hello from file");
        }
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_input_expand.txt");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        final ListNode list = this.parse("cat < $TEST_FILE");
        assertEquals(0, executor.execute(list));
        assertEquals("hello from file", bos.toString(StandardCharsets.UTF_8).trim());
        file.delete();
    }

    @Test
    void executeNonExecutableCommand() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_nonexec_test.sh");
        file.createNewFile();
        file.setExecutable(false);
        final ListNode list = this.parse(file.getAbsolutePath());
        final Executor executor = new Executor();
        assertEquals(126, executor.execute(list));
    }

    @Test
    void expandDollarQuestion() {
        final Executor executor = new Executor();
        final ListNode list = this.parse("false");
        executor.execute(list);
        assertEquals("1", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionAfterSuccess() {
        final Executor executor = new Executor();
        final ListNode list = this.parse("true");
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
        final ListNode list = this.parse("false");
        executor.execute(list);
        assertEquals("1", executor.expandVariables("${?}", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionInString() {
        final Executor executor = new Executor();
        final ListNode list = this.parse("false");
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
        final ListNode list = this.parse("case ab in a?) echo MATCHED;; *) echo NO;; esac");
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void globMatchesQuestionMarkNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parse("case abc in a?) echo MATCHED;; *) echo NO;; esac");
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parse("case b in [abc]) echo MATCHED;; *) echo NO;; esac");
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void globMatchesCharClassNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parse("case z in [abc]) echo MATCHED;; *) echo NO;; esac");
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassRangeInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parse("case m in [a-z]) echo MATCHED;; *) echo NO;; esac");
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void globMatchesUnclosedBracketInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        final ListNode list = this.parse("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac");
        assertEquals(0, executor.execute(list));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void backtickNestedNormalise() {
        assertEquals("$(echo `echo hi`)", WordExpander.normaliseBackticks("`echo \\`echo hi\\``"));
    }

    @Test
    void echoBuiltin() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("echo hello")));
    }

    @Test
    void echoBuiltinArgs() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("echo hello world")));
    }

    @Test
    void echoBuiltinNoNewline() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("echo -n hello")));
    }

    @Test
    void echoBuiltinEmpty() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("echo")));
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
        executor.execute(this.parse("echo capture-me | cat"));
        assertTrue(bos.toString(StandardCharsets.UTF_8).contains("capture-me"), String.format("captureStream: %s", bos));
    }

    @Test
    void resolveCommandWithCustomPathDirInPath() throws Exception {
        final java.io.File dir = java.nio.file.Files.createTempDirectory("sh4j_custompath_").toFile();
        dir.deleteOnExit();
        final java.io.File bin = new java.io.File(dir, "myhello");
        try (java.io.FileWriter w = new java.io.FileWriter(bin, StandardCharsets.UTF_8)) {
            w.write("#!/bin/sh\necho ok\n");
        }
        bin.setExecutable(true);
        bin.deleteOnExit();
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", dir.getAbsolutePath());
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parse("myhello")));
    }

    @Test
    void externalCloseStdin() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("cat <&-")));
    }

    @Test
    void externalStdoutToStderr() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("true 1>&2")));
    }

    @Test
    void externalReadWriteFdZero() throws Exception {
        final java.io.File file = java.io.File.createTempFile("sh4j_rw_0_", ".dat");
        file.deleteOnExit();
        try (java.io.FileWriter w = new java.io.FileWriter(file, StandardCharsets.UTF_8)) {
            w.write("seed\n");
        }
        final Executor executor = new Executor();
        executor.execute(this.parser(String.format("cat <>%s", file.getAbsolutePath())).parse());
    }

    @Test
    void externalHeredocDashStripsTabs() throws Exception {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("cat <<-EOF\n\t\thello\nEOF\n")));
    }

    @Test
    void exportArgWithEqualsNonStandardName() throws Exception {
        final Executor executor = new Executor(new HashMap<>());
        final int code = executor.execute(this.parse("export 1foo=bar"));
        assertEquals(0, code);
        assertEquals("bar", executor.getVariable("1foo"));
    }

    @Test
    void execNonExecutableFileTriggersIoException() throws Exception {
        final java.io.File nonexec = java.io.File.createTempFile("sh4j_nonexec_", ".dat");
        nonexec.deleteOnExit();
        try (java.io.FileWriter w = new java.io.FileWriter(nonexec, StandardCharsets.UTF_8)) {
            w.write("not a script");
        }
        nonexec.setExecutable(false);
        final Executor executor = new Executor();
        final int code = executor.execute(this.parser(String.format("exec %s", nonexec.getAbsolutePath())).parse());
        assertTrue(code != 0, String.format("non-executable should fail; got %s", code));
    }

    @Test
    void execInterruptedException() {
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        final int code = executor.execute(this.parse("exec sleep 1"));
        Thread.interrupted();
        assertEquals(1, code);
    }

    @Test
    void externalNonExecutableFileTriggersIoException() throws Exception {
        final java.io.File nonexec = java.io.File.createTempFile("sh4j_ext_nonexec_", ".dat");
        nonexec.deleteOnExit();
        try (java.io.FileWriter w = new java.io.FileWriter(nonexec, StandardCharsets.UTF_8)) {
            w.write("nope");
        }
        nonexec.setExecutable(false);
        final Executor executor = new Executor();
        final int code = executor.execute(this.parse(nonexec.getAbsolutePath()));
        assertTrue(code != 0, String.format("non-executable external should fail; got %s", code));
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
    void externalReadWriteRedirectFdTwo() throws Exception {
        final String path = String.format("/tmp/sh4j_rw_fd2_%s", System.nanoTime());
        new java.io.File(path).delete();
        final Executor executor = new Executor();
        executor.execute(this.parse(String.format("cat /nonexistent_xyz 2<>%s", path)));
        new java.io.File(path).delete();
    }

    @Test
    void externalHereDocRedirect() throws Exception {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("cat <<EOF\nhello world\nEOF\n")));
    }

    @Test
    void externalHereDocDashRedirect() throws Exception {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("cat <<-EOF\n\thello dashed\nEOF\n")));
    }

    @Test
    void shiftLargerThanPositionalCount() {
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b")));
        assertEquals(0, executor.execute(this.parse("shift 100")));
    }

    @Test
    void execWithMultipleArgs() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("exec true arg1 arg2 arg3")));
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
        executor.execute(this.parse("c=true; while $c; do c=false; continue; echo unreached; done"));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().isEmpty(), String.format("continue should skip body, got: %s", bos));
    }

    @Test
    void typeForExternalNonBuiltinCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("type ls")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).contains("ls is"));
    }

    @Test
    void exitWithValidIntegerAndWordExpansionError() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(1, executor.execute(this.parse("exit 5${UNSET_FOR_EXIT_999:?msg}")));
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
        final Map<String, String> empty = new HashMap<>();
        empty.put("PATH", "");
        final Executor executor = new Executor(empty);
        executor.expandVariables("", empty);
        assertEquals(127, executor.execute(this.parse("nonexistent_xyz_zzz_cmd_abc")));
    }

    @Test
    void resolveCommandWithEmptyDirInPath() {
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", ":/usr/bin");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parse("true")));
    }

    @Test
    void createProcessBuilderWithQuotedArg() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("true \"quoted arg\"")));
    }

    @Test
    void forLoopWithContinue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("for i in 1 2 3; do continue; echo $i; done")));
        assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void typeForExternalCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("type true")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).contains("true is"));
    }

    @Test
    void exitWithWordExpansionError() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(1, executor.execute(this.parse("exit ${UNSET_FOR_EXIT_8x9:?errmsg}")));
    }

    @Test
    void exportArgWithEquals() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(0, executor.execute(this.parse("export FOO=hello")));
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
        assertEquals(0, executor.execute(this.parse("true")));
    }

    @Test
    void commandNotFoundReturnsNotFound() {
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(this.parse("nonexistent_cmd_xyz_127")));
    }

    @Test
    void commandWithSlashFound() {
        final Map<String, String> env = new HashMap<>();
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parse("/user/.nix-profile/bin/ls")));
    }

    @Test
    void commandFoundInCustomPath() throws IOException {
        final File dir = new File(String.format("/tmp/sh4j_path_test_%s", System.nanoTime()));
        dir.mkdirs();
        dir.deleteOnExit();
        final File script = new File(dir, "mycmd");
        try (FileWriter w = new FileWriter(script, StandardCharsets.UTF_8)) {
            w.write("#!/bin/sh\nexit 42\n");
        }
        script.setExecutable(true);
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", dir.getAbsolutePath());
        final Executor executor = new Executor(env);
        dir.delete();
        assertEquals(42, executor.execute(this.parse("mycmd")));
    }

    @Test
    void commandNotFoundEmptyPath() {
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", "");
        final Executor executor = new Executor(env);
        assertEquals(127, executor.execute(this.parse("somecmd")));
    }

    @Test
    void commandFoundInSecondPathDir() throws IOException {
        final File dir = new File(String.format("/tmp/sh4j_path_test2_%s", System.nanoTime()));
        dir.mkdirs();
        dir.deleteOnExit();
        final File script = new File(dir, "mycmd2");
        try (FileWriter w = new FileWriter(script, StandardCharsets.UTF_8)) {
            w.write("#!/bin/sh\nexit 7\n");
        }
        script.setExecutable(true);
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", String.format("/nonexistent_test_dir_xyz:%s", dir.getAbsolutePath()));
        final Executor executor = new Executor(env);
        dir.delete();
        assertEquals(7, executor.execute(this.parse("mycmd2")));
    }

    @Test
    void commandWithSlashNotFound() {
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(this.parse("/nonexistent_path_xyz_abc/cmd")));
    }

    @Test
    void exportWithValue() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("export FOO=bar")));
        assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void exportExistingVariable() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("export FOO")));
    }

    @Test
    void exportThenVariableExpands() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("export MSG=hello; echo $MSG")));
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
        assertEquals(0, executor.execute(this.parse("MSG=\"hello  world\"; echo \"$MSG\"")));
    }

    @Test
    void echoUnquotedVarSplit() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(0, executor.execute(this.parse("MSG=\"hello  world\"; echo $MSG")));
    }

    @Test
    void echoQuotedLiteralNoSplit() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("echo \"hello   world\"")));
    }

    @Test
    void echoCustomIfsColon() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", ":");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parse("echo a:b:c")));
    }

    @Test
    void echoEmptyIfsNoSplit() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", "");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parse("echo hello   world")));
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
        assertEquals(0, executor.execute(this.parse("echo \"*.java\"")));
    }

    @Test
    void pathnameExpandGlobUnquoted() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("echo *.java")));
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
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().contains("my error"), "error message should go to injected stream");
    }

    @Test
    void cdToDirectory() throws Exception {
        final String saved = System.getProperty("user.dir");
        try {
            final Executor executor = new Executor();
            assertEquals(0, executor.execute(this.parse("cd /tmp")));
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
            assertEquals(0, executor.execute(this.parse("cd")));
            assertEquals("/tmp", executor.getVariable("PWD"));
        } finally {
            System.setProperty("user.dir", saved);
        }
    }

    @Test
    void cdToNonexistentReturnsOne() {
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(this.parse("cd /sh4j_nonexistent_dir_xyz")));
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
            assertEquals(0, executor.execute(this.parse("cd -")));
            assertEquals("/", executor.getVariable("PWD"));
            assertEquals("/tmp", executor.getVariable("OLDPWD"));
            assertEquals("/", bos.toString(StandardCharsets.UTF_8).trim());
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
            assertEquals(0, executor.execute(this.parse("cd $MYDIR")));
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
        assertEquals(0, executor.execute(this.parse("type echo cd exit export type true false shift unset")));
        final String out = bos.toString(StandardCharsets.UTF_8);
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
        assertEquals(0, executor.execute(this.parse("type true")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).contains("true is "));
    }

    @Test
    void typeNotFound() {
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(this.parse("type nonexistent_cmd_xyz")));
    }

    @Test
    void execBuiltinRunsCommand() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("exec true")));
    }

    @Test
    void execBuiltinNotFoundReturnsNotFound() {
        final Executor executor = new Executor();
        assertEquals(127, executor.execute(this.parse("exec nonexistent_cmd_xyz")));
    }

    @Test
    void execBuiltinNoArgsIsNoop() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("exec")));
    }

    @Test
    void trueBuiltinReturnsZero() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("true")));
    }

    @Test
    void falseBuiltinReturnsOne() {
        final Executor executor = new Executor();
        assertEquals(1, executor.execute(this.parse("false")));
    }

    @Test
    void unsetRemovesVariable() {
        final Map<String, String> env = new HashMap<>();
        env.put("MYVAR", "value");
        final Executor executor = new Executor(env);
        assertEquals(0, executor.execute(this.parse("unset MYVAR")));
        assertNull(executor.getVariable("MYVAR"));
    }

    @Test
    void shiftRemovesPositionalParam() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(List.of("sh", "a", "b", "c")));
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("shift; echo $#")));
        assertEquals("2", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void shiftWithCount() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(new HashMap<>(), new ArrayList<>(List.of("sh", "a", "b", "c")));
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("shift 2; echo $#")));
        assertEquals("1", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void breakStopsWhileLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("while exit $?; do echo ran; break; exit 1; done")));
        assertEquals("ran", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void breakStopsForLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("for i in a b c; do echo $i; break; done")));
        assertEquals("a", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void continueInForLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("for i in a b; do echo $i; continue; echo after; done")));
        assertEquals("a\nb", bos.toString(StandardCharsets.UTF_8).trim());
        assertFalse(bos.toString(StandardCharsets.UTF_8).contains("after"));
    }

    @Test
    void colonBuiltinReturnsZero() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse(":")));
    }

    @Test
    void evalBuiltinRunsCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("eval echo hello world")));
        assertEquals("hello world", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void evalBuiltinNoArgsReturnsZero() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("eval")));
    }

    @Test
    void externalOutputRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new java.io.File(outpath).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(inpath, StandardCharsets.UTF_8)) {
            w.write("hello world");
        }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse(String.format("cat < %s", inpath) + String.format(" > %s", outpath))));
        assertTrue(new java.io.File(outpath).exists());
        assertEquals("hello world", java.nio.file.Files.readString(java.nio.file.Path.of(outpath)).trim());
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    void externalAppendRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_append_in.txt";
        final String outpath = "/tmp/sh4j_ext_append_out.txt";
        new java.io.File(outpath).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(inpath, StandardCharsets.UTF_8)) {
            w.write("second");
        }
        try (java.io.FileWriter w = new java.io.FileWriter(outpath, StandardCharsets.UTF_8)) {
            w.write("first\n");
        }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse(String.format("cat < %s", inpath) + String.format(" >> %s", outpath))));
        final String content = java.nio.file.Files.readString(java.nio.file.Path.of(outpath)).trim();
        assertTrue(content.startsWith("first"));
        assertTrue(content.contains("second"));
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    void externalClobberRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outpath = "/tmp/sh4j_ext_clobber_out.txt";
        new java.io.File(outpath).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(inpath, StandardCharsets.UTF_8)) {
            w.write("hello clobber");
        }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse(String.format("cat < %s", inpath) + String.format(" >| %s", outpath))));
        assertTrue(new java.io.File(outpath).exists());
        assertEquals("hello clobber", java.nio.file.Files.readString(java.nio.file.Path.of(outpath)).trim());
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    void externalRedirectStdoutToStderr() throws Exception {
        final String inpath = "/tmp/sh4j_ext_12_in.txt";
        try (java.io.FileWriter w = new java.io.FileWriter(inpath, StandardCharsets.UTF_8)) {
            w.write("hello");
        }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse(String.format("cat < %s", inpath) + " 1>&2")));
        new java.io.File(inpath).delete();
    }

    @Test
    void externalRedirectStderrToStdout() throws Exception {
        final String inpath = "/tmp/sh4j_ext_21_in.txt";
        try (java.io.FileWriter w = new java.io.FileWriter(inpath, StandardCharsets.UTF_8)) {
            w.write("hello");
        }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse(String.format("cat < %s", inpath) + " 2>&1")));
        new java.io.File(inpath).delete();
    }

    @Test
    void externalCloseStdout() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("true 1>&-")));
    }

    @Test
    void externalCloseStderr() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("true 2>&-")));
    }

    @Test
    void externalStderrOutputRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_out.txt";
        new java.io.File(path).delete();
        final Executor executor = new Executor();
        executor.execute(this.parse(String.format("cat /nonexistent_path_xyz_123 2> %s", path)));
        assertTrue(new java.io.File(path).exists());
        new java.io.File(path).delete();
    }

    @Test
    void externalStderrAppendRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_append.txt";
        new java.io.File(path).delete();
        try (java.io.FileWriter w = new java.io.FileWriter(path, StandardCharsets.UTF_8)) {
            w.write("initial\n");
        }
        final Executor executor = new Executor();
        executor.execute(this.parse(String.format("cat /nonexistent_path_xyz_456 2>> %s", path)));
        assertTrue(java.nio.file.Files.readString(java.nio.file.Path.of(path)).contains("initial"));
        new java.io.File(path).delete();
    }

    @Test
    void externalReadWriteRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_rw.txt";
        try (java.io.FileWriter w = new java.io.FileWriter(path, StandardCharsets.UTF_8)) {
            w.write("hello");
        }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse(String.format("cat <> %s", path))));
        new java.io.File(path).delete();
    }

    @Test
    void heredocStripTabsBasic() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("cat <<- EOF\n\thello\nEOF\n")));
        assertEquals("hello", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMultiple() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("cat <<- EOF\n\t\thello\nEOF\n")));
        assertEquals("hello", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsDelimiterWithTabs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("cat <<- EOF\n\thello\n\tEOF\n")));
        assertEquals("hello", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMixed() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("cat <<- MARKER\n\tfirst\nsecond\n\tthird\nMARKER\n")));
        assertEquals("first\nsecond\nthird", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void typeFunction() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("myfunc() { true; }; type myfunc")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).contains("myfunc is a function"));
    }

    @Test
    void shiftNonNumericReturnsOne() {
        assertEquals(1, new Executor().execute(this.parse("shift abc")));
    }

    @Test
    void shiftNegativeReturnsOne() {
        assertEquals(1, new Executor().execute(this.parse("shift -1")));
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }

}
