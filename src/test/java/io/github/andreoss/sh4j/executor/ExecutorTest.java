/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Executor.
 * @since 1.0
 */
final class ExecutorTest {

    @Test
    void executeSimpleEcho() {
        assertEquals(0, new Executor().execute(this.parse("echo hello")));
    }

    @Test
    void executePwd() {
        assertEquals(0, new Executor().execute(this.parse("pwd")));
    }

    @Test
    void executeTrue() {
        assertEquals(0, new Executor().execute(this.parse("true")));
    }

    @Test
    void executeFalse() {
        assertEquals(1, new Executor().execute(this.parse("false")));
    }

    @Test
    void executeWithArguments() {
        assertEquals(0, new Executor().execute(this.parse("echo a b c")));
    }

    @Test
    void executeSemicolonSeparator() {
        assertEquals(0, new Executor().execute(this.parse("echo a; echo b")));
    }

    @Test
    void executeAndIf() {
        assertEquals(0, new Executor().execute(this.parse("echo a && echo b")));
    }

    @Test
    void executeOrIf() {
        assertEquals(0, new Executor().execute(this.parse("false || echo b")));
    }

    @Test
    void executeAndIfStopsOnFailure() {
        assertEquals(1, new Executor().execute(this.parse("false && echo not reached")));
    }

    @Test
    void executeOrIfStopsOnSuccess() {
        assertEquals(0, new Executor().execute(this.parse("echo success || echo not reached")));
    }

    @Test
    void executeEmptyList() {
        assertEquals(0, new Executor().execute(this.parse("")));
    }

    @Test
    void executeAssignmentOnly() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("FOO=bar")));
        assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void executeAssignmentWithCommand() {
        assertEquals(0, new Executor().execute(this.parse("FOO=hello echo $FOO")));
    }

    @Test
    void executeMultipleAssignments() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("A=1 B=2")));
        assertEquals("1", executor.getVariable("A"));
        assertEquals("2", executor.getVariable("B"));
    }

    @Test
    void executeVariableExpansion() {
        assertEquals(0, new Executor().execute(this.parse("echo $HOME")));
    }

    @Test
    void executeOutputRedirection() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo test > /tmp/sh4j_test_output")));
    }

    @Test
    void executeInputRedirection() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("cat < /dev/null")));
    }

    @Test
    void executePipeline() {
        assertEquals(0, new Executor().execute(this.parse("echo hello | cat")));
    }

    @Test
    void executeBackground() {
        assertEquals(0, new Executor().execute(this.parse("echo hello &")));
    }

    @Test
    void executeBackgroundSetsLastBgPid() {
        final Executor executor = new Executor();
        executor.execute(this.parse("echo hello &"));
        assertTrue(executor.getLastBgPid() > 0, "lastBgPid should be > 0 after & command");
    }

    @Test
    void executeBackgroundAndSequential() {
        assertEquals(0, new Executor().execute(this.parse("echo bg1 & echo seq1")));
    }

    @Test
    void executeClobberRedirection() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo test >| /tmp/sh4j_clobber_test")));
    }

    @Test
    void executeBuiltinStderrRedirect() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo hello 2>/dev/null")));
    }

    @Test
    void executeBuiltinStderrToStdout() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo hello 2>&1")));
    }

    @Test
    void executeBuiltinStderrRedirectToFile() throws Exception {
        assertEquals(0, new Executor().execute(this.parse(String.format("echo hello 2>%s", "/tmp/sh4j_stderr_test"))));
    }

    @Test
    void executeBuiltinStderrAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stderr_append_%s", System.nanoTime());
        assertEquals(0, new Executor().execute(this.parse(String.format("echo hello 2>>%s", file))));
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stdout_append_%s", System.nanoTime());
        assertEquals(0, new Executor().execute(this.parse(String.format("echo hello >>%s", file))));
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutRedirectIOException() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo hello >/dev/null/no_such_path_xyz")));
    }

    @Test
    void executeBuiltinStderrRedirectIOException() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo hello 2>/dev/null/no_such_stderr_xyz")));
    }

    @Test
    void executeBuiltinStdoutAppendIOException() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo hello >>/dev/null/no_such_append_xyz")));
    }

    @Test
    void executeBuiltinStderrAppendIOException() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo hello 2>>/dev/null/no_such_eappend_xyz")));
    }

    @Test
    void executeBuiltinCloseStdout() {
        assertEquals(0, new Executor().execute(this.parse("echo hello >&-")));
    }

    @Test
    void executeMultiplePipelines() {
        assertEquals(0, new Executor().execute(this.parse("echo a; echo b; echo c")));
    }

    @Test
    void executeBangPipeline() {
        assertEquals(0, new Executor().execute(this.parse("! false")));
    }

    @Test
    void executeVariableWithEmptyValue() {
        final ListNode list = this.parse("echo $EMPTY_VAR");
        final Map<String, String> env = new HashMap<>();
        env.put("EMPTY_VAR", "");
        assertEquals(0, new Executor(env).execute(list));
    }

    @Test
    void executeCommandNotFound() {
        assertEquals(127, new Executor().execute(this.parse("nonexistent_command_xyz")));
    }

    @Test
    void executeCommandNotFoundReturnsNotFound() {
        assertEquals(127, new Executor().execute(this.parse("nonexistent_cmd_xyz_127")));
    }

    @Test
    void executeBangWithTrue() {
        assertEquals(1, new Executor().execute(this.parse("! true")));
    }

    @Test
    void executeExitZero() {
        assertEquals(0, new Executor().execute(this.parse("exit 0")));
    }

    @Test
    void executeExitOne() {
        assertEquals(1, new Executor().execute(this.parse("exit 1")));
    }

    @Test
    void executeEchoAbc() {
        assertEquals(0, new Executor().execute(this.parse("echo abc")));
    }

    @Test
    void executeEmptyListNode() {
        assertEquals(0, new Executor().execute(new ListNode()));
    }

    @Test
    void executePipelineWithNoCommands() {
        final ListNode list = new ListNode();
        list.addPipeline(new Pipeline(false), ListNode.Separator.NONE);
        assertEquals(0, new Executor().execute(list));
    }

    @Test
    void executeAppendRedirection() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo test >> /tmp/sh4j_append_test")));
    }

    @Test
    void executeStderrRedirect() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo test 2>/tmp/sh4j_test_stderr")));
    }

    @Test
    void executeStderrToStdout() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo test 2>&1")));
    }

    @Test
    void executeCloseStdin() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("cat <&-")));
    }

    @Test
    void executeCloseStdout() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("echo test >&-")));
    }

    @Test
    void executeReadWriteRedirect() throws Exception {
        assertEquals(0, new Executor().execute(this.parse("cat <> /dev/null")));
    }

    @Test
    void executeExitNoArgs() {
        assertEquals(0, new Executor().execute(this.parse("exit")));
    }

    @Test
    void executeExitWithNonNumericArg() {
        assertEquals(1, new Executor().execute(this.parse("exit xyz")));
    }

    @Test
    void executePipelineWithNullCommand() {
        final Pipeline pipeline = new Pipeline(false);
        pipeline.getCommands().add(null);
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        assertEquals(0, new Executor().execute(list));
    }

    @Test
    void executeCommandWithNullName() {
        final Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(new SimpleCommand(null));
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        assertEquals(0, new Executor().execute(list));
    }

    @Test
    void executeAssignmentWithNoEquals() {
        final SimpleCommand cmd = new SimpleCommand("echo");
        cmd.getAssignments().add("INVALID");
        final Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(cmd);
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        assertEquals(0, new Executor().execute(list));
    }

    @Test
    void executePipelineWithFailingCommand() {
        final Executor executor = new Executor();
        final int code = executor.execute(this.parse("echo hello | nonexistent_cmd_xyz_fail | echo done"));
        assertTrue(code == 127 || code == 126,
                "Pipeline with missing middle stage must exit with POSIX "
                        + String.format("command-not-found(127) or not-executable(126); got %s", code));
    }

    @Test
    void executeSubshell() {
        assertEquals(0, new Executor().execute(this.parse("(echo hello)")));
    }

    @Test
    void executeSubshellWithMultiplePipelines() {
        assertEquals(0, new Executor().execute(this.parse("(echo a; echo b)")));
    }

    @Test
    void executeSubshellInPipeline() {
        assertEquals(0, new Executor().execute(this.parse("echo foo | (cat) | wc")));
    }

    @Test
    void executeSubshellWithExit() {
        assertEquals(42, new Executor().execute(this.parse("(exit 42)")));
    }

    @Test
    void executeGrouping() {
        assertEquals(0, new Executor().execute(this.parse("{ echo hello; }")));
    }

    @Test
    void executeGroupingInPipeline() {
        assertEquals(0, new Executor().execute(this.parse("echo foo | { cat; } | wc")));
    }

    @Test
    void executeGroupingSharesEnvironment() {
        assertEquals(0, new Executor().execute(this.parse("FOO=bar; { echo $FOO; }")));
    }

    @Test
    void executeIfTrueThen() {
        assertEquals(0, new Executor().execute(this.parse("if true; then echo hi; fi")));
    }

    @Test
    void executeIfFalseElse() {
        assertEquals(0, new Executor().execute(this.parse("if false; then echo a; else echo b; fi")));
    }

    @Test
    void executeIfTrueThenElseNotExecuted() {
        assertEquals(0, new Executor().execute(this.parse("if true; then echo hi; else false; fi")));
    }

    @Test
    void executeForDoDone() {
        assertEquals(0, new Executor().execute(this.parse("for var in a b c; do echo $var; done")));
    }

    @Test
    void executeWhileFalse() {
        assertEquals(0, new Executor().execute(this.parse("while false; do echo hi; done")));
    }

    @Test
    void executeUntilTrue() {
        assertEquals(0, new Executor().execute(this.parse("until true; do echo hi; done")));
    }

    @Test
    void executeCaseMatch() {
        assertEquals(0, new Executor().execute(this.parse("case a in a) echo match;; esac")));
    }

    @Test
    void executeCaseNoMatch() {
        assertEquals(0, new Executor().execute(this.parse("case a in b) echo nomatch;; esac")));
    }

    @Test
    void executeCaseDefaultStar() {
        assertEquals(0, new Executor().execute(this.parse("case a in *) echo default;; esac")));
    }

    @Test
    void executeCasePipePattern() {
        assertEquals(0, new Executor().execute(this.parse("case a in b|c|a) echo match;; esac")));
    }

    @Test
    void executeCaseFirstMatchOnly() {
        assertEquals(0, new Executor().execute(this.parse("case a in a) echo first;; b) echo second;; esac")));
    }

    @Test
    void executeFunctionDefinitionStored() {
        assertEquals(0, new Executor().execute(this.parse("foo() { echo hello; }")));
    }

    @Test
    void executeFunctionCall() {
        assertEquals(0, new Executor().execute(this.parse("foo() { echo hello; }; foo")));
    }

    @Test
    void executeFunctionSubshellBody() {
        assertEquals(0, new Executor().execute(this.parse("foo() (echo hello); foo")));
    }

    @Test
    void executeWhileLoop() {
        assertEquals(0, new Executor().execute(this.parse("while false; do echo body; done")));
    }

    @Test
    void executeUntilLoop() {
        assertEquals(0, new Executor().execute(this.parse("until true; do echo body; done")));
    }

    @Test
    void executeWhileBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("while exit $?; do echo ran; exit 1; done");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(1, executor.execute(list));
        assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeUntilBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("exit 1; until exit $?; do echo ran; exit 0; done");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeWhileBodyExitCodePropagated() {
        assertEquals(42, new Executor().execute(this.parse("while exit $?; do exit 42; done")));
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
        assertEquals(0, executor.execute(this.parse("export")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).contains("export TEST_EXPORT_VAR=hello"));
    }

    @Test
    void executeIfFalseNoElse() {
        assertEquals(1, new Executor().execute(this.parse("if false; then echo yes; fi")));
    }

    @Test
    void executeElifWhenIfFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("if false; then echo no; elif true; then echo yes; fi");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals(String.format("yes%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeElifChainFalseFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse(
            "if false; then echo a; elif false; then echo b; else echo c; fi"
        );
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeElifChainSecondElifTrue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse(
            "if false; then echo a; elif false; then echo b; elif true; then echo c; fi"
        );
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true));
        assertEquals(0, executor.execute(list));
        assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeRedirectStdoutToStderr() {
        assertEquals(0, new Executor().execute(this.parse("echo test 1>&2")));
    }

    @Test
    void executeHereDoc() {
        assertEquals(0, new Executor().execute(this.parse(String.format("cat << EOF%nhello%nEOF%n"))));
    }

    @Test
    void heredocExpansion() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse(String.format("cat << EOF%nhello $TEST_HVAR%nEOF%n"))));
        assertEquals("hello expanded", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void redirectTargetExpansionOutput() throws Exception {
        final java.io.File f = new java.io.File("/tmp/sh4j_redir_expand_test.txt");
        f.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        assertEquals(0, new Executor(env).execute(this.parse("echo hello > $TEST_FILE")));
        assertTrue(f.exists());
        assertEquals(String.format("hello%n"), new String(java.nio.file.Files.readAllBytes(f.toPath())));
        f.delete();
    }

    @Test
    void redirectTargetExpansionAppend() throws Exception {
        final java.io.File f = new java.io.File("/tmp/sh4j_redir_append_expand.txt");
        f.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        assertEquals(0, new Executor(env).execute(this.parse("echo hello >> $TEST_FILE")));
        assertTrue(f.exists());
        assertEquals(String.format("hello%n"), new String(java.nio.file.Files.readAllBytes(f.toPath())));
        f.delete();
    }

    @Test
    void redirectTargetExpansionInput() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_redir_input_expand.txt");
        file.delete();
        try (java.io.BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write("hello from file");
        }
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_input_expand.txt");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("cat < $TEST_FILE")));
        assertEquals("hello from file", bos.toString(StandardCharsets.UTF_8).trim());
        file.delete();
    }

    @Test
    void executeNonExecutableCommand() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_nonexec_test.sh");
        file.createNewFile();
        file.setExecutable(false);
        assertEquals(126, new Executor().execute(this.parse(file.getAbsolutePath())));
    }

    @Test
    void expandDollarQuestion() {
        final Executor executor = new Executor();
        executor.execute(this.parse("false"));
        assertEquals("1", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionAfterSuccess() {
        final Executor executor = new Executor();
        executor.execute(this.parse("true"));
        assertEquals("0", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarHyphen() {
        assertEquals("", new Executor().expandVariables("$-", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionBraced() {
        final Executor executor = new Executor();
        executor.execute(this.parse("false"));
        assertEquals("1", executor.expandVariables("${?}", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionInString() {
        final Executor executor = new Executor();
        executor.execute(this.parse("false"));
        assertEquals("exit:1", executor.expandVariables("exit:$?", new HashMap<>()));
    }

    @Test
    void expandDollarHash() {
        assertEquals("3", new Executor(new HashMap<>(), List.of("sh", "a", "b", "c")).expandVariables("$#", new HashMap<>()));
    }

    @Test
    void expandDollarAt() {
        assertEquals("a b c", new Executor(new HashMap<>(), List.of("sh", "a", "b", "c")).expandVariables("$@", new HashMap<>()));
    }

    @Test
    void expandDollarStar() {
        assertEquals("a b c", new Executor(new HashMap<>(), List.of("sh", "a", "b", "c")).expandVariables("$*", new HashMap<>()));
    }

    @Test
    void expandDollarZero() {
        assertEquals("my_script", new Executor(new HashMap<>(), List.of("my_script")).expandVariables("$0", new HashMap<>()));
    }

    @Test
    void expandDollarOne() {
        assertEquals("first", new Executor(new HashMap<>(), List.of("sh", "first", "second")).expandVariables("$1", new HashMap<>()));
    }

    @Test
    void expandDollarNine() {
        assertEquals("i", new Executor(new HashMap<>(), List.of("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i")).expandVariables("$9", new HashMap<>()));
    }

    @Test
    void expandBracedMultiDigit() {
        assertEquals("j", new Executor(new HashMap<>(), List.of("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j")).expandVariables("${10}", new HashMap<>()));
    }

    @Test
    void expandDollarHashNone() {
        assertEquals("0", new Executor().expandVariables("$#", new HashMap<>()));
    }

    @Test
    void expandDollarAtEmpty() {
        assertEquals("", new Executor().expandVariables("$@", new HashMap<>()));
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
        assertEquals("0", new Executor().expandVariables("$!", new HashMap<>()));
    }

    @Test
    void expandDollarBangBraced() {
        assertEquals("0", new Executor().expandVariables("${!}", new HashMap<>()));
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
        assertEquals("default", new Executor().expandVariables("${var:-default}", new HashMap<>()));
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
        assertEquals("default", new Executor().expandVariables("${var-default}", new HashMap<>()));
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
        assertEquals("", new Executor().expandVariables("${var:+alt}", new HashMap<>()));
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
        assertEquals("", new Executor().expandVariables("${var+alt}", new HashMap<>()));
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
        assertEquals("0", new Executor().expandVariables("${#var}", new HashMap<>()));
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
        assertEquals("hello", new Executor().expandVariables("$(echo hello)", new HashMap<>()));
    }

    @Test
    void commandSubstitutionInString() {
        assertEquals("msg=hi", new Executor().expandVariables("msg=$(echo hi)", new HashMap<>()));
    }

    @Test
    void backtickSubstitution() {
        assertEquals("hello", new Executor().expandVariables("`echo hello`", new HashMap<>()));
    }

    @Test
    void backtickSubstitutionInString() {
        assertEquals("msg=hi", new Executor().expandVariables("msg=`echo hi`", new HashMap<>()));
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
        assertEquals(0, executor.execute(this.parse("case ab in a?) echo MATCHED;; *) echo NO;; esac")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void globMatchesQuestionMarkNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("case abc in a?) echo MATCHED;; *) echo NO;; esac")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("case b in [abc]) echo MATCHED;; *) echo NO;; esac")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void globMatchesCharClassNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("case z in [abc]) echo MATCHED;; *) echo NO;; esac")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassRangeInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("case m in [a-z]) echo MATCHED;; *) echo NO;; esac")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void globMatchesUnclosedBracketInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac")));
        assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void backtickNestedNormalise() {
        assertEquals("$(echo `echo hi`)", WordExpander.normaliseBackticks("`echo \\`echo hi\\``"));
    }

    @Test
    void echoBuiltin() {
        assertEquals(0, new Executor().execute(this.parse("echo hello")));
    }

    @Test
    void echoBuiltinArgs() {
        assertEquals(0, new Executor().execute(this.parse("echo hello world")));
    }

    @Test
    void echoBuiltinNoNewline() {
        assertEquals(0, new Executor().execute(this.parse("echo -n hello")));
    }

    @Test
    void echoBuiltinEmpty() {
        assertEquals(0, new Executor().execute(this.parse("echo")));
    }

    @Test
    void arithmeticSimple() {
        assertEquals("7", new Executor().expandVariables("$((3 + 4))", new HashMap<>()));
    }

    @Test
    void arithmeticMul() {
        assertEquals("15", new Executor().expandVariables("$((3 * 5))", new HashMap<>()));
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
        assertEquals("14", new Executor().expandVariables("$((2 * (3 + 4)))", new HashMap<>()));
    }

    @Test
    void arithmeticShiftLeft() {
        assertEquals("32", new Executor().expandVariables("$((4 << 3))", new HashMap<>()));
    }

    @Test
    void arithmeticShiftRight() {
        assertEquals("4", new Executor().expandVariables("$((32 >> 3))", new HashMap<>()));
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
        assertEquals("4", new Executor().expandVariables("$((12 & 6))", new HashMap<>()));
    }

    @Test
    void arithmeticBitXor() {
        assertEquals("10", new Executor().expandVariables("$((12 ^ 6))", new HashMap<>()));
    }

    @Test
    void arithmeticBitOr() {
        assertEquals("14", new Executor().expandVariables("$((12 | 6))", new HashMap<>()));
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
        assertEquals("10", new Executor().expandVariables("$((1 ? 10 : 20))", new HashMap<>()));
    }

    @Test
    void arithmeticTernaryFalse() {
        assertEquals("20", new Executor().expandVariables("$((0 ? 10 : 20))", new HashMap<>()));
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
        try (java.io.BufferedWriter w = Files.newBufferedWriter(bin.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("#!/bin/sh%necho ok%n"));
        }
        bin.setExecutable(true);
        bin.deleteOnExit();
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", dir.getAbsolutePath());
        assertEquals(0, new Executor(env).execute(this.parse("myhello")));
    }

    @Test
    void externalCloseStdin() {
        assertEquals(0, new Executor().execute(this.parse("cat <&-")));
    }

    @Test
    void externalStdoutToStderr() {
        assertEquals(0, new Executor().execute(this.parse("true 1>&2")));
    }

    @Test
    void externalReadWriteFdZero() throws Exception {
        final java.io.File file = java.io.File.createTempFile("sh4j_rw_0_", ".dat");
        file.deleteOnExit();
        try (java.io.BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("seed%n"));
        }
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parser(String.format("cat <>%s", file.getAbsolutePath())).parse()));
    }

    @Test
    void externalHeredocDashStripsTabs() throws Exception {
        assertEquals(0, new Executor().execute(this.parse(String.format("cat <<-EOF%n\t\thello%nEOF%n"))));
    }

    @Test
    void exportArgWithEqualsNonStandardName() throws Exception {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals(0, executor.execute(this.parse("export 1foo=bar")));
        assertEquals("bar", executor.getVariable("1foo"));
    }

    @Test
    void execNonExecutableFileTriggersIoException() throws Exception {
        final java.io.File nonexec = java.io.File.createTempFile("sh4j_nonexec_", ".dat");
        nonexec.deleteOnExit();
        try (java.io.BufferedWriter w = Files.newBufferedWriter(nonexec.toPath(), StandardCharsets.UTF_8)) {
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
        try (java.io.BufferedWriter w = Files.newBufferedWriter(nonexec.toPath(), StandardCharsets.UTF_8)) {
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
        assertNotEquals(0, executor.execute(this.parse(String.format("cat /nonexistent_xyz 2<>%s", path))));
        new java.io.File(path).delete();
    }

    @Test
    void externalHereDocRedirect() throws Exception {
        assertEquals(0, new Executor().execute(this.parse(String.format("cat <<EOF%nhello world%nEOF%n"))));
    }

    @Test
    void externalHereDocDashRedirect() throws Exception {
        assertEquals(0, new Executor().execute(this.parse(String.format("cat <<-EOF%n\thello dashed%nEOF%n"))));
    }

    @Test
    void shiftLargerThanPositionalCount() {
        assertEquals(0, new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b"))).execute(this.parse("shift 100")));
    }

    @Test
    void execWithMultipleArgs() {
        assertEquals(0, new Executor().execute(this.parse("exec true arg1 arg2 arg3")));
    }

    @Test
    void positionalParamIndexOutOfRangeIsEmpty() {
        assertEquals("", new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b"))).expandVariables("$9", new HashMap<>()));
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
        assertEquals(1, new Executor(new HashMap<>()).execute(this.parse("exit 5${UNSET_FOR_EXIT_999:?msg}")));
    }

    @Test
    void expandBracedMultiDigitPositionalOutOfRange() {
        assertEquals("", new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b", "c"))).expandVariables("${10}", new HashMap<>()));
    }

    @Test
    void expandBracedMultiDigitPositionalInRange() {
        assertEquals("b", new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script", "a", "b", "c"))).expandVariables("${2}", new HashMap<>()));
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
        assertEquals("0", new Executor().expandVariables("${#UNSET_HASH_TEST_xyz}", new HashMap<>()));
    }

    @Test
    void specialParamShellFlags() {
        assertEquals("", new Executor().expandVariables("$-", new HashMap<>()));
    }

    @Test
    void specialParamBackgroundPidBeforeAnyJob() {
        assertEquals("0", new Executor().expandVariables("$!", new HashMap<>()));
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
        assertEquals("", new Executor(new HashMap<>(), new ArrayList<>(java.util.Arrays.asList("script"))).expandVariables("$@", new HashMap<>()));
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
        assertEquals(0, new Executor(env).execute(this.parse("true")));
    }

    @Test
    void createProcessBuilderWithQuotedArg() {
        assertEquals(0, new Executor().execute(this.parse("true \"quoted arg\"")));
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
        assertEquals(1, new Executor(new HashMap<>()).execute(this.parse("exit ${UNSET_FOR_EXIT_8x9:?errmsg}")));
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
        assertEquals("~nonexistentuser123", new Executor().expandVariables("~nonexistentuser123", new HashMap<>()));
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
        assertEquals(String.format(" \t%n"), new Executor().getVariable("IFS"));
    }

    @Test
    void defaultHomeSet() {
        assertEquals(System.getProperty("user.home"), new Executor().getVariable("HOME"));
    }

    @Test
    void defaultPathSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", "");
        assertEquals("/usr/local/bin:/usr/bin:/bin", new Executor(env).getVariable("PATH"));
    }

    @Test
    void defaultsDoNotOverrideExistingEnv() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/custom/home");
        env.put("PATH", "/custom/path");
        final Executor executor = new Executor(env);
        assertEquals("/custom/home", executor.getVariable("HOME"));
        assertEquals("/custom/path", executor.getVariable("PATH"));
        assertEquals(String.format(" \t%n"), executor.getVariable("IFS"));
    }

    @Test
    void commandFoundInPath() {
        assertEquals(0, new Executor().execute(this.parse("true")));
    }

    @Test
    void commandNotFoundReturnsNotFound() {
        assertEquals(127, new Executor().execute(this.parse("nonexistent_cmd_xyz_127")));
    }

    @Test
    void commandWithSlashFound() {
        assertEquals(0, new Executor(new HashMap<>()).execute(this.parse("/user/.nix-profile/bin/ls")));
    }

    @Test
    void commandFoundInCustomPath() throws IOException {
        final File dir = new File(String.format("/tmp/sh4j_path_test_%s", System.nanoTime()));
        dir.mkdirs();
        dir.deleteOnExit();
        final File script = new File(dir, "mycmd");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("#!/bin/sh%nexit 42%n"));
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
        assertEquals(127, new Executor(env).execute(this.parse("somecmd")));
    }

    @Test
    void commandFoundInSecondPathDir() throws IOException {
        final File dir = new File(String.format("/tmp/sh4j_path_test2_%s", System.nanoTime()));
        dir.mkdirs();
        dir.deleteOnExit();
        final File script = new File(dir, "mycmd2");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("#!/bin/sh%nexit 7%n"));
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
        assertEquals(127, new Executor().execute(this.parse("/nonexistent_path_xyz_abc/cmd")));
    }

    @Test
    void exportWithValue() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("export FOO=bar")));
        assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void exportExistingVariable() {
        assertEquals(0, new Executor().execute(this.parse("export FOO")));
    }

    @Test
    void exportThenVariableExpands() {
        final Executor executor = new Executor();
        assertEquals(0, executor.execute(this.parse("export MSG=hello; echo $MSG")));
        assertEquals("hello", executor.getVariable("MSG"));
    }

    @Test
    void splitFieldsDefaultIfs() {
        assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a b c", String.format(" \t%n")));
    }

    @Test
    void splitFieldsMultipleWs() {
        assertEquals(List.of("a", "b"), WordExpander.splitFields("a   b", String.format(" \t%n")));
    }

    @Test
    void splitFieldsLeadingWs() {
        assertEquals(List.of("a", "b"), WordExpander.splitFields("  a b", String.format(" \t%n")));
    }

    @Test
    void splitFieldsTrailingWs() {
        assertEquals(List.of("a", "b"), WordExpander.splitFields("a b  ", String.format(" \t%n")));
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
        assertEquals(List.of(), WordExpander.splitFields("", String.format(" \t%n")));
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
        assertEquals(0, new Executor(new HashMap<>()).execute(this.parse("MSG=\"hello  world\"; echo \"$MSG\"")));
    }

    @Test
    void echoUnquotedVarSplit() {
        assertEquals(0, new Executor(new HashMap<>()).execute(this.parse("MSG=\"hello  world\"; echo $MSG")));
    }

    @Test
    void echoQuotedLiteralNoSplit() {
        assertEquals(0, new Executor().execute(this.parse("echo \"hello   world\"")));
    }

    @Test
    void echoCustomIfsColon() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", ":");
        assertEquals(0, new Executor(env).execute(this.parse("echo a:b:c")));
    }

    @Test
    void echoEmptyIfsNoSplit() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", "");
        assertEquals(0, new Executor(env).execute(this.parse("echo hello   world")));
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
        assertEquals(0, new Executor().execute(this.parse("echo \"*.java\"")));
    }

    @Test
    void pathnameExpandGlobUnquoted() {
        assertEquals(0, new Executor().execute(this.parse("echo *.java")));
    }

    @Test
    void expandBracedColonQuestionUnset() {
        assertEquals("", new Executor(new HashMap<>()).expandVariables("${x:?my error}", new HashMap<>()));
    }

    @Test
    void expandBracedQuestionUnset() {
        assertEquals("", new Executor(new HashMap<>()).expandVariables("${x?my error}", new HashMap<>()));
    }

    @Test
    void expandBracedColonQuestionSet() {
        assertEquals("value", new Executor(new HashMap<>()).expandVariables("${x:?not used}", new HashMap<>(Map.of("x", "value"))));
    }

    @Test
    void expandBracedColonQuestionDefaultMsg() {
        final Executor executor = new Executor(new HashMap<>());
        assertEquals("", executor.expandVariables("${x:?}", new HashMap<>()));
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
        assertEquals(1, new Executor().execute(this.parse("cd /sh4j_nonexistent_dir_xyz")));
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
        assertEquals(1, new Executor().execute(this.parse("type nonexistent_cmd_xyz")));
    }

    @Test
    void execBuiltinRunsCommand() {
        assertEquals(0, new Executor().execute(this.parse("exec true")));
    }

    @Test
    void execBuiltinNotFoundReturnsNotFound() {
        assertEquals(127, new Executor().execute(this.parse("exec nonexistent_cmd_xyz")));
    }

    @Test
    void execBuiltinNoArgsIsNoop() {
        assertEquals(0, new Executor().execute(this.parse("exec")));
    }

    @Test
    void trueBuiltinReturnsZero() {
        assertEquals(0, new Executor().execute(this.parse("true")));
    }

    @Test
    void falseBuiltinReturnsOne() {
        assertEquals(1, new Executor().execute(this.parse("false")));
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
        assertEquals(String.format("a%nb"), bos.toString(StandardCharsets.UTF_8).trim());
        assertFalse(bos.toString(StandardCharsets.UTF_8).contains("after"));
    }

    @Test
    void colonBuiltinReturnsZero() {
        assertEquals(0, new Executor().execute(this.parse(":")));
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
        assertEquals(0, new Executor().execute(this.parse("eval")));
    }

    @Test
    void externalOutputRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new java.io.File(outpath).delete();
        try (java.io.BufferedWriter w = Files.newBufferedWriter(java.nio.file.Path.of(inpath), StandardCharsets.UTF_8)) {
            w.write("hello world");
        }
        assertEquals(0, new Executor().execute(this.parse(String.format("cat < %s", inpath) + String.format(" > %s", outpath))));
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
        try (java.io.BufferedWriter w = Files.newBufferedWriter(java.nio.file.Path.of(inpath), StandardCharsets.UTF_8)) {
            w.write("second");
        }
        try (java.io.BufferedWriter w = Files.newBufferedWriter(java.nio.file.Path.of(outpath), StandardCharsets.UTF_8)) {
            w.write(String.format("first%n"));
        }
        assertEquals(0, new Executor().execute(this.parse(String.format("cat < %s", inpath) + String.format(" >> %s", outpath))));
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
        try (java.io.BufferedWriter w = Files.newBufferedWriter(java.nio.file.Path.of(inpath), StandardCharsets.UTF_8)) {
            w.write("hello clobber");
        }
        assertEquals(0, new Executor().execute(this.parse(String.format("cat < %s", inpath) + String.format(" >| %s", outpath))));
        assertTrue(new java.io.File(outpath).exists());
        assertEquals("hello clobber", java.nio.file.Files.readString(java.nio.file.Path.of(outpath)).trim());
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    void externalRedirectStdoutToStderr() throws Exception {
        final String inpath = "/tmp/sh4j_ext_12_in.txt";
        try (java.io.BufferedWriter w = Files.newBufferedWriter(java.nio.file.Path.of(inpath), StandardCharsets.UTF_8)) {
            w.write("hello");
        }
        assertEquals(0, new Executor().execute(this.parse(String.format("cat < %s", inpath) + " 1>&2")));
        new java.io.File(inpath).delete();
    }

    @Test
    void externalRedirectStderrToStdout() throws Exception {
        final String inpath = "/tmp/sh4j_ext_21_in.txt";
        try (java.io.BufferedWriter w = Files.newBufferedWriter(java.nio.file.Path.of(inpath), StandardCharsets.UTF_8)) {
            w.write("hello");
        }
        assertEquals(0, new Executor().execute(this.parse(String.format("cat < %s", inpath) + " 2>&1")));
        new java.io.File(inpath).delete();
    }

    @Test
    void externalCloseStdout() {
        assertEquals(0, new Executor().execute(this.parse("true 1>&-")));
    }

    @Test
    void externalCloseStderr() {
        assertEquals(0, new Executor().execute(this.parse("true 2>&-")));
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
        try (java.io.BufferedWriter w = Files.newBufferedWriter(java.nio.file.Path.of(path), StandardCharsets.UTF_8)) {
            w.write(String.format("initial%n"));
        }
        final Executor executor = new Executor();
        executor.execute(this.parse(String.format("cat /nonexistent_path_xyz_456 2>> %s", path)));
        assertTrue(java.nio.file.Files.readString(java.nio.file.Path.of(path)).contains("initial"));
        new java.io.File(path).delete();
    }

    @Test
    void externalReadWriteRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_rw.txt";
        try (java.io.BufferedWriter w = Files.newBufferedWriter(java.nio.file.Path.of(path), StandardCharsets.UTF_8)) {
            w.write("hello");
        }
        assertEquals(0, new Executor().execute(this.parse(String.format("cat <> %s", path))));
        new java.io.File(path).delete();
    }

    @Test
    void heredocStripTabsBasic() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse(String.format("cat <<- EOF%n\thello%nEOF%n"))));
        assertEquals("hello", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMultiple() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse(String.format("cat <<- EOF%n\t\thello%nEOF%n"))));
        assertEquals("hello", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsDelimiterWithTabs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse(String.format("cat <<- EOF%n\thello%n\tEOF%n"))));
        assertEquals("hello", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMixed() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        assertEquals(0, executor.execute(this.parse(String.format("cat <<- MARKER%n\tfirst%nsecond%n\tthird%nMARKER%n"))));
        assertEquals(String.format("first%nsecond%nthird"), bos.toString(StandardCharsets.UTF_8).trim());
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
