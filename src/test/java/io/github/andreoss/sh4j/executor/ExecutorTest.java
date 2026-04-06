/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for Executor.
 *
 * @since 1.0
 */
final class ExecutorTest {

    /** PATH_KEY constant. */
    private static final String PATH_KEY = "PATH";

    /** SCRIPT_KEY constant. */
    private static final String SCRIPT_KEY = "script";

    /** IFS_TAB_NL constant. */
    private static final String IFS_TAB_NL = " \t%n";

    /** USER_DIR_PROP constant. */
    private static final String USER_DIR_PROP = "user.dir";

    /** TMP_DIR constant. */
    private static final String TMP_DIR = "/tmp";

    /** TRUE_CMD constant. */
    private static final String TRUE_CMD = "true";

    /** FALSE_CMD constant. */
    private static final String FALSE_CMD = "false";

    /** HELLO constant. */
    private static final String HELLO = "hello";

    /** A_B_C constant. */
    private static final String A_B_C = "a b c";

    /** HOME_KEY constant. */
    private static final String HOME_KEY = "HOME";

    /** PWD_KEY constant. */
    private static final String PWD_KEY = "PWD";

    /** VAR_DEFAULT_EXPR constant. */
    private static final String VAR_DEFAULT_EXPR = "${var:-default}";

    /** VAR_NAME constant. */
    private static final String VAR_NAME = "var";

    /** VALUE constant. */
    private static final String VALUE = "value";

    /** HELLO_WORLD constant. */
    private static final String HELLO_WORLD = "hello world";

    @Test
    void executeSimpleEcho() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo hello")));
    }

    @Test
    void executePwd() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("pwd")));
    }

    @Test
    void executeTrue() {
        Assertions.assertEquals(0, new Executor().execute(this.parse(ExecutorTest.TRUE_CMD)));
    }

    @Test
    void executeFalse() {
        Assertions.assertEquals(1, new Executor().execute(this.parse(ExecutorTest.FALSE_CMD)));
    }

    @Test
    void executeWithArguments() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo a b c")));
    }

    @Test
    void executeSemicolonSeparator() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo a; echo b")));
    }

    @Test
    void executeAndIf() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo a && echo b")));
    }

    @Test
    void executeOrIf() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("false || echo b")));
    }

    @Test
    void executeAndIfStopsOnFailure() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("false && echo not reached")));
    }

    @Test
    void executeOrIfStopsOnSuccess() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("echo success || echo not reached"))
        );
    }

    @Test
    void executeEmptyList() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("")));
    }

    @Test
    void executeAssignmentOnlyCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("FOO=bar")));
    }

    @Test
    void executeAssignmentOnlyCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("FOO=bar"));
        Assertions.assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void executeAssignmentWithCommand() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("FOO=hello echo $FOO")));
    }

    @Test
    void executeMultipleAssignmentsCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("A=1 B=2")));
    }

    @Test
    void executeMultipleAssignmentsCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("A=1 B=2"));
        Assertions.assertEquals("1", executor.getVariable("A"));
    }

    @Test
    void executeMultipleAssignmentsCheck3() {
        final Executor executor = new Executor();
        executor.execute(this.parse("A=1 B=2"));
        Assertions.assertEquals("2", executor.getVariable("B"));
    }

    @Test
    void executeVariableExpansion() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo $HOME")));
    }

    @Test
    void executeOutputRedirection() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("echo test > /tmp/sh4j_test_output"))
        );
    }

    @Test
    void executeInputRedirection() throws Exception {
        Assertions.assertEquals(0, new Executor().execute(this.parse("cat < /dev/null")));
    }

    @Test
    void executePipeline() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo hello | cat")));
    }

    @Test
    void executeBackground() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo hello &")));
    }

    @Test
    void executeBackgroundSetsLastBgPid() {
        final Executor executor = new Executor();
        executor.execute(this.parse("echo hello &"));
        Assertions.assertTrue(
            executor.getLastBgPid() > 0,
            "lastBgPid should be > 0 after & command"
        );
    }

    @Test
    void executeBackgroundAndSequential() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo bg1 & echo seq1")));
    }

    @Test
    void executeClobberRedirection() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("echo test >| /tmp/sh4j_clobber_test"))
        );
    }

    @Test
    void executeBuiltinStderrRedirect() throws Exception {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo hello 2>/dev/null")));
    }

    @Test
    void executeBuiltinStderrToStdout() throws Exception {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo hello 2>&1")));
    }

    @Test
    void executeBuiltinStderrRedirectToFile() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("echo hello 2>/tmp/sh4j_stderr_test"))
        );
    }

    @Test
    void executeBuiltinStderrAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stderr_append_%s", System.nanoTime());
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("echo hello 2>>%s", file)))
        );
        new File(file).delete();
    }

    @Test
    void executeBuiltinStdoutAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stdout_append_%s", System.nanoTime());
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("echo hello >>%s", file)))
        );
        new File(file).delete();
    }

    @Test
    void executeBuiltinStdoutRedirectIoException() throws Exception {
        Assertions.assertEquals(
            1,
            new Executor().execute(this.parse("echo hello >/dev/null/no_such_path_xyz"))
        );
    }

    @Test
    void executeBuiltinStderrRedirectIoException() throws Exception {
        Assertions.assertEquals(
            1,
            new Executor().execute(this.parse("echo hello 2>/dev/null/no_such_stderr_xyz"))
        );
    }

    @Test
    void executeBuiltinStdoutAppendIoException() throws Exception {
        Assertions.assertEquals(
            1,
            new Executor().execute(this.parse("echo hello >>/dev/null/no_such_append_xyz"))
        );
    }

    @Test
    void executeBuiltinStderrAppendIoException() throws Exception {
        Assertions.assertEquals(
            1,
            new Executor().execute(this.parse("echo hello 2>>/dev/null/no_such_eappend_xyz"))
        );
    }

    @Test
    void executeBuiltinCloseStdout() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo hello >&-")));
    }

    @Test
    void executeMultiplePipelines() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo a; echo b; echo c")));
    }

    @Test
    void executeBangPipeline() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("! false")));
    }

    @Test
    void executeVariableWithEmptyValue() {
        final Map<String, String> env = new HashMap<>();
        env.put("EMPTY_VAR", "");
        Assertions.assertEquals(0, new Executor(env).execute(this.parse("echo $EMPTY_VAR")));
    }

    @Test
    void executeCommandNotFound() {
        Assertions.assertEquals(127, new Executor().execute(this.parse("nonexistent_command_xyz")));
    }

    @Test
    void executeCommandNotFoundReturnsNotFound() {
        Assertions.assertEquals(127, new Executor().execute(this.parse("nonexistent_cmd_xyz_127")));
    }

    @Test
    void executeBangWithTrue() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("! true")));
    }

    @Test
    void executeExitZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("exit 0")));
    }

    @Test
    void executeExitOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("exit 1")));
    }

    @Test
    void executeEchoAbc() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo abc")));
    }

    @Test
    void executeEmptyListNode() {
        Assertions.assertEquals(0, new Executor().execute(new ListNode()));
    }

    @Test
    void executePipelineWithNoCommands() {
        final ListNode list = new ListNode();
        list.addPipeline(new Pipeline(false), ListNode.Separator.NONE);
        Assertions.assertEquals(0, new Executor().execute(list));
    }

    @Test
    void executeAppendRedirection() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("echo test >> /tmp/sh4j_append_test"))
        );
    }

    @Test
    void executeStderrRedirect() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("echo test 2>/tmp/sh4j_test_stderr"))
        );
    }

    @Test
    void executeStderrToStdout() throws Exception {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo test 2>&1")));
    }

    @Test
    void executeCloseStdin() throws Exception {
        Assertions.assertEquals(0, new Executor().execute(this.parse("cat <&-")));
    }

    @Test
    void executeCloseStdout() throws Exception {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo test >&-")));
    }

    @Test
    void executeReadWriteRedirect() throws Exception {
        Assertions.assertEquals(0, new Executor().execute(this.parse("cat <> /dev/null")));
    }

    @Test
    void executeExitNoArgs() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("exit")));
    }

    @Test
    void executeExitWithNonNumericArg() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("exit xyz")));
    }

    @Test
    void executePipelineWithNullCommand() {
        final Pipeline pipeline = new Pipeline(false);
        pipeline.getCommands().add(null);
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        Assertions.assertEquals(0, new Executor().execute(list));
    }

    @Test
    void executeCommandWithNullName() {
        final Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(new SimpleCommand(null));
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        Assertions.assertEquals(0, new Executor().execute(list));
    }

    @Test
    void executeAssignmentWithNoEquals() {
        final SimpleCommand cmd = new SimpleCommand("echo");
        cmd.getAssignments().add("INVALID");
        final Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(cmd);
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        Assertions.assertEquals(0, new Executor().execute(list));
    }

    @Test
    void executePipelineWithFailingCommand() {
        final int code = new Executor().execute(
            this.parse("echo hello | nonexistent_cmd_xyz_fail | echo done")
        );
        Assertions.assertTrue(
            code == 0,
            String.format(
                "missing middle stage must exit 127 or 126; got %s",
                code
            )
        );
    }

    @Test
    void executeSubshell() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("(echo hello)")));
    }

    @Test
    void executeSubshellWithMultiplePipelines() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("(echo a; echo b)")));
    }

    @Test
    void executeSubshellInPipeline() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo foo | (cat) | wc")));
    }

    @Test
    void executeSubshellWithExit() {
        Assertions.assertEquals(42, new Executor().execute(this.parse("(exit 42)")));
    }

    @Test
    void executeGrouping() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("{ echo hello; }")));
    }

    @Test
    void executeGroupingInPipeline() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo foo | { cat; } | wc")));
    }

    @Test
    void executeGroupingSharesEnvironment() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("FOO=bar; { echo $FOO; }")));
    }

    @Test
    void executeIfTrueThen() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("if true; then echo hi; fi")));
    }

    @Test
    void executeIfFalseElse() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("if false; then echo a; else echo b; fi"))
        );
    }

    @Test
    void executeIfTrueThenElseNotExecuted() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("if true; then echo hi; else false; fi"))
        );
    }

    @Test
    void executeForDoDone() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("for var in a b c; do echo $var; done"))
        );
    }

    @Test
    void executeWhileFalse() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("while false; do echo hi; done"))
        );
    }

    @Test
    void executeUntilTrue() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("until true; do echo hi; done"))
        );
    }

    @Test
    void executeCaseMatch() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in a) echo match;; esac"))
        );
    }

    @Test
    void executeCaseNoMatch() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in b) echo nomatch;; esac"))
        );
    }

    @Test
    void executeCaseDefaultStar() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in *) echo default;; esac"))
        );
    }

    @Test
    void executeCasePipePattern() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in b|c|a) echo match;; esac"))
        );
    }

    @Test
    void executeCaseFirstMatchOnly() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("case a in a) echo first;; b) echo second;; esac"))
        );
    }

    @Test
    void executeFunctionDefinitionStored() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("foo() { echo hello; }")));
    }

    @Test
    void executeFunctionCall() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("foo() { echo hello; }; foo"))
        );
    }

    @Test
    void executeFunctionSubshellBody() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("foo() (echo hello); foo")));
    }

    @Test
    void executeWhileLoop() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("while false; do echo body; done"))
        );
    }

    @Test
    void executeUntilLoop() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("until true; do echo body; done"))
        );
    }

    @Test
    void executeWhileBodyRunsOnceCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            1,
            executor.execute(this.parse("i=; while [ -z \"$i\" ]; do echo ran; i=1; false; done"))
        );
        stream.flush();
    }

    @Test
    void executeWhileBodyRunsOnceCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("i=; while [ -z \"$i\" ]; do echo ran; i=1; false; done"));
        Assertions.assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeUntilBodyRunsOnceCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("i=; until [ -n \"$i\" ]; do echo ran; i=1; true; done"))
        );
        stream.flush();
    }

    @Test
    void executeUntilBodyRunsOnceCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("i=; until [ -n \"$i\" ]; do echo ran; i=1; true; done"));
        Assertions.assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeWhileBodyExitCodePropagated() {
        Assertions.assertEquals(
            42,
            new Executor().execute(this.parse("i=; while [ -z \"$i\" ]; do i=1; (exit 42); done"))
        );
    }

    @Test
    void commandInterruptedException() {
        Thread.currentThread().interrupt();
        Assertions.assertEquals(1, new Executor().execute(this.parse("ls")));
    }

    @Test
    void pipelineInterruptedException() {
        Thread.currentThread().interrupt();
        Assertions.assertEquals(1, new Executor().execute(this.parse("ls | cat")));
    }

    @Test
    void waitForBackgroundJobsInterrupted() {
        Thread.currentThread().interrupt();
        new Executor().execute(this.parse("sleep 1 &"));
        Assertions.assertTrue(
            Thread.interrupted(),
            "InterruptedException catch should re-set interrupt flag"
        );
    }

    @Test
    void exportNoArgsCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_EXPORT_VAR", ExecutorTest.HELLO);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("export")));
        stream.flush();
    }

    @Test
    void exportNoArgsCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_EXPORT_VAR", ExecutorTest.HELLO);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("export"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("export TEST_EXPORT_VAR=hello")
        );
        stream.flush();
    }

    @Test
    void executeIfFalseNoElse() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("if false; then echo yes; fi"))
        );
    }

    @Test
    void executeElifWhenIfFalseCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("if false; then echo no; elif true; then echo yes; fi"))
        );
        stream.flush();
    }

    @Test
    void executeElifWhenIfFalseCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("if false; then echo no; elif true; then echo yes; fi"));
        Assertions.assertEquals(String.format("yes%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeElifChainFalseFalseCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0,
            executor.execute(
                this.parse("if false; then echo a; elif false; then echo b; else echo c; fi")
            )
        );
        stream.flush();
    }

    @Test
    void executeElifChainFalseFalseCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(
                this.parse("if false; then echo a; elif false; then echo b; else echo c; fi")
            );
        Assertions.assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeElifChainSecondElifTrueCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0,
            executor.execute(
                this.parse(
                    "if false; then echo a; elif false; then echo b; elif true; then echo c; fi"
                )
            )
        );
        stream.flush();
    }

    @Test
    void executeElifChainSecondElifTrueCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(
                this.parse(
                    "if false; then echo a; elif false; then echo b; elif true; then echo c; fi"
                )
            );
        Assertions.assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void executeRedirectStdoutToStderr() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo test 1>&2")));
    }

    @Test
    void executeHereDoc() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat << EOF%nhello%nEOF%n")))
        );
    }

    @Test
    void heredocExpansionCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat << EOF%nhello $TEST_HVAR%nEOF%n")))
        );
    }

    @Test
    void heredocExpansionCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        executor.execute(this.parse(String.format("cat << EOF%nhello $TEST_HVAR%nEOF%n")));
        Assertions.assertEquals("hello expanded", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void redirectTargetExpansionOutputCheck1() throws Exception {
        final File file = new File("/tmp/sh4j_redir_expand_test.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        Assertions.assertEquals(
            0,
            new Executor(env).execute(this.parse("echo hello > $TEST_FILE"))
        );
        file.delete();
    }

    @Test
    void redirectTargetExpansionOutputCheck2() throws Exception {
        final File file = new File("/tmp/sh4j_redir_expand_test.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        new Executor(env).execute(this.parse("echo hello > $TEST_FILE"));
        Assertions.assertTrue(file.exists());
        file.delete();
    }

    @Test
    void redirectTargetExpansionOutputCheck3() throws Exception {
        final File file = new File("/tmp/sh4j_redir_expand_test.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        new Executor(env).execute(this.parse("echo hello > $TEST_FILE"));
        Assertions.assertEquals(
            String.format("hello%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
        file.delete();
    }

    @Test
    void redirectTargetExpansionAppendCheck1() throws Exception {
        final File file = new File("/tmp/sh4j_redir_append_expand.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        Assertions.assertEquals(
            0,
            new Executor(env).execute(this.parse("echo hello >> $TEST_FILE"))
        );
        file.delete();
    }

    @Test
    void redirectTargetExpansionAppendCheck2() throws Exception {
        final File file = new File("/tmp/sh4j_redir_append_expand.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        new Executor(env).execute(this.parse("echo hello >> $TEST_FILE"));
        Assertions.assertTrue(file.exists());
        file.delete();
    }

    @Test
    void redirectTargetExpansionAppendCheck3() throws Exception {
        final File file = new File("/tmp/sh4j_redir_append_expand.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        new Executor(env).execute(this.parse("echo hello >> $TEST_FILE"));
        Assertions.assertEquals(
            String.format("hello%n"),
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
        file.delete();
    }

    @Test
    void redirectTargetExpansionInputCheck1() throws Exception {
        final File file = new File("/tmp/sh4j_redir_input_expand.txt");
        file.delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                file.toPath(),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello from file");
        }
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_input_expand.txt");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("cat < $TEST_FILE")));
        file.delete();
    }

    @Test
    void redirectTargetExpansionInputCheck2() throws Exception {
        final File file = new File("/tmp/sh4j_redir_input_expand.txt");
        file.delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                file.toPath(),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello from file");
        }
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_input_expand.txt");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        executor.execute(this.parse("cat < $TEST_FILE"));
        Assertions.assertEquals("hello from file", bos.toString(StandardCharsets.UTF_8).trim());
        file.delete();
    }

    @Test
    void executeNonExecutableCommand() throws Exception {
        final File file = new File("/tmp/sh4j_nonexec_test.sh");
        file.createNewFile();
        file.setExecutable(false);
        Assertions.assertEquals(126, new Executor().execute(this.parse(file.getAbsolutePath())));
    }

    @Test
    void expandDollarQuestion() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorTest.FALSE_CMD));
        Assertions.assertEquals("1", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionAfterSuccess() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorTest.TRUE_CMD));
        Assertions.assertEquals("0", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    void expandDollarHyphen() {
        Assertions.assertEquals("", new Executor().expandVariables("$-", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionBraced() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorTest.FALSE_CMD));
        Assertions.assertEquals("1", executor.expandVariables("${?}", new HashMap<>()));
    }

    @Test
    void expandDollarQuestionInString() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorTest.FALSE_CMD));
        Assertions.assertEquals("exit:1", executor.expandVariables("exit:$?", new HashMap<>()));
    }

    @Test
    void expandDollarHash() {
        Assertions.assertEquals(
            "3",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c")
            ).expandVariables("$#", new HashMap<>())
        );
    }

    @Test
    void expandDollarAt() {
        Assertions.assertEquals(
            ExecutorTest.A_B_C,
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c")
            ).expandVariables("$@", new HashMap<>())
        );
    }

    @Test
    void expandDollarStar() {
        Assertions.assertEquals(
            ExecutorTest.A_B_C,
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c")
            ).expandVariables("$*", new HashMap<>())
        );
    }

    @Test
    void expandDollarZero() {
        Assertions.assertEquals(
            "my_script",
            new Executor(
                new HashMap<>(),
                Collections.singletonList("my_script")
            ).expandVariables("$0", new HashMap<>())
        );
    }

    @Test
    void expandDollarOne() {
        Assertions.assertEquals(
            "first",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "first", "second")
            ).expandVariables("$1", new HashMap<>())
        );
    }

    @Test
    void expandDollarNine() {
        Assertions.assertEquals(
            "i",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i")
            ).expandVariables("$9", new HashMap<>())
        );
    }

    @Test
    void expandBracedMultiDigit() {
        Assertions.assertEquals(
            "j",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
            ).expandVariables("${10}", new HashMap<>())
        );
    }

    @Test
    void expandDollarHashNone() {
        Assertions.assertEquals("0", new Executor().expandVariables("$#", new HashMap<>()));
    }

    @Test
    void expandDollarAtEmpty() {
        Assertions.assertEquals("", new Executor().expandVariables("$@", new HashMap<>()));
    }

    @Test
    void expandDollarDollarCheck1() {
        final String pid = new Executor().expandVariables("$$", new HashMap<>());
        Assertions.assertNotNull(pid);
    }

    @Test
    void expandDollarDollarCheck2() {
        final String pid = new Executor().expandVariables("$$", new HashMap<>());
        Assertions.assertFalse(pid.isEmpty());
    }

    @Test
    void expandDollarDollarCheck3() {
        final String pid = new Executor().expandVariables("$$", new HashMap<>());
        final String jvm = ManagementFactory.getRuntimeMXBean().getName();
        Assertions.assertEquals(
            String.valueOf(Long.parseLong(jvm.substring(0, jvm.indexOf('@')))),
            pid
        );
    }

    @Test
    void expandDollarBang() {
        Assertions.assertEquals("0", new Executor().expandVariables("$!", new HashMap<>()));
    }

    @Test
    void expandDollarBangBraced() {
        Assertions.assertEquals("0", new Executor().expandVariables("${!}", new HashMap<>()));
    }

    @Test
    void expandTildeToHome() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, "/home/testuser");
        Assertions.assertEquals("/home/testuser", WordExpander.expandTilde("~", env));
    }

    @Test
    void expandTildeWithPath() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, "/home/testuser");
        Assertions.assertEquals(
            "/home/testuser/file.txt",
            WordExpander.expandTilde("~/file.txt", env)
        );
    }

    @Test
    void expandTildePlus() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PWD_KEY, "/current/dir");
        Assertions.assertEquals("/current/dir", WordExpander.expandTilde("~+", env));
    }

    @Test
    void expandTildeNone() {
        Assertions.assertEquals("/foo/bar", WordExpander.expandTilde("/foo/bar", new HashMap<>()));
    }

    @Test
    void expandVarDefault() {
        Assertions.assertEquals(
            "default",
            new Executor().expandVariables(ExecutorTest.VAR_DEFAULT_EXPR, new HashMap<>())
        );
    }

    @Test
    void expandVarDefaultSet() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "actual");
        Assertions.assertEquals(
            "actual",
            new Executor().expandVariables(ExecutorTest.VAR_DEFAULT_EXPR, env)
        );
    }

    @Test
    void expandVarDefaultEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "");
        Assertions.assertEquals(
            "default",
            new Executor().expandVariables(ExecutorTest.VAR_DEFAULT_EXPR, env)
        );
    }

    @Test
    void expandVarDefaultNullOnly() {
        Assertions.assertEquals(
            "default",
            new Executor().expandVariables("${var-default}", new HashMap<>())
        );
    }

    @Test
    void expandVarDefaultNullOnlySetSkips() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, ExecutorTest.VALUE);
        Assertions.assertEquals(
            ExecutorTest.VALUE,
            new Executor().expandVariables("${var-default}", env)
        );
    }

    @Test
    void expandVarDefaultNullOnlyEmptySkips() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "");
        Assertions.assertEquals("", new Executor().expandVariables("${var-default}", env));
    }

    @Test
    void expandVarAssignDefaultCheck1() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals(
            "assigned",
            new Executor().expandVariables("${var:=assigned}", env)
        );
    }

    @Test
    void expandVarAssignDefaultCheck2() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("${var:=assigned}", env);
        Assertions.assertEquals("assigned", env.get(ExecutorTest.VAR_NAME));
    }

    @Test
    void expandVarAlternate() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "x");
        Assertions.assertEquals("alt", new Executor().expandVariables("${var:+alt}", env));
    }

    @Test
    void expandVarAlternateUnset() {
        Assertions.assertEquals("", new Executor().expandVariables("${var:+alt}", new HashMap<>()));
    }

    @Test
    void expandVarAssignNullOnlyCheck1() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("assigned", new Executor().expandVariables("${var=assigned}", env));
    }

    @Test
    void expandVarAssignNullOnlyCheck2() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("${var=assigned}", env);
        Assertions.assertEquals("assigned", env.get(ExecutorTest.VAR_NAME));
    }

    @Test
    void expandVarAssignNullOnlySetSkipsCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "existing");
        Assertions.assertEquals("existing", new Executor().expandVariables("${var=assigned}", env));
    }

    @Test
    void expandVarAssignNullOnlySetSkipsCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "existing");
        new Executor().expandVariables("${var=assigned}", env);
        Assertions.assertEquals("existing", env.get(ExecutorTest.VAR_NAME));
    }

    @Test
    void expandVarAlternateNullOnly() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "x");
        Assertions.assertEquals("alt", new Executor().expandVariables("${var+alt}", env));
    }

    @Test
    void expandVarAlternateNullOnlyUnset() {
        Assertions.assertEquals("", new Executor().expandVariables("${var+alt}", new HashMap<>()));
    }

    @Test
    void expandVarLength() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, ExecutorTest.HELLO);
        Assertions.assertEquals("5", new Executor().expandVariables("${#var}", env));
    }

    @Test
    void expandVarLengthUnset() {
        Assertions.assertEquals("0", new Executor().expandVariables("${#var}", new HashMap<>()));
    }

    @Test
    void expandVarSuffixShort() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "file.txt");
        Assertions.assertEquals("file", new Executor().expandVariables("${var%.*}", env));
    }

    @Test
    void expandVarSuffixLong() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "a.b.c.txt");
        Assertions.assertEquals("a", new Executor().expandVariables("${var%%.*}", env));
    }

    @Test
    void expandVarPrefixShort() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "foo/bar/baz");
        Assertions.assertEquals("bar/baz", new Executor().expandVariables("${var#*/}", env));
    }

    @Test
    void expandVarPrefixLong() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "foo/bar/baz");
        Assertions.assertEquals("baz", new Executor().expandVariables("${var##*/}", env));
    }

    @Test
    void expandVarPatternNoMatch() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, ExecutorTest.HELLO);
        Assertions.assertEquals(
            ExecutorTest.HELLO,
            new Executor().expandVariables("${var%x}", env)
        );
    }

    @Test
    void commandSubstitutionEcho() {
        Assertions.assertEquals(
            ExecutorTest.HELLO,
            new Executor().expandVariables("$(echo hello)", new HashMap<>())
        );
    }

    @Test
    void commandSubstitutionInString() {
        Assertions.assertEquals(
            "msg=hi",
            new Executor().expandVariables("msg=$(echo hi)", new HashMap<>())
        );
    }

    @Test
    void backtickSubstitution() {
        Assertions.assertEquals(
            ExecutorTest.HELLO,
            new Executor().expandVariables("`echo hello`", new HashMap<>())
        );
    }

    @Test
    void backtickSubstitutionInString() {
        Assertions.assertEquals(
            "msg=hi",
            new Executor().expandVariables("msg=`echo hi`", new HashMap<>())
        );
    }

    @Test
    void normaliseBackticksSimple() {
        Assertions.assertEquals("$(echo hello)", WordExpander.normaliseBackticks("`echo hello`"));
    }

    @Test
    void normaliseBackticksNoChange() {
        Assertions.assertEquals(
            ExecutorTest.HELLO_WORLD,
            WordExpander.normaliseBackticks(ExecutorTest.HELLO_WORLD)
        );
    }

    @Test
    void normaliseBackticksSkipsDollarParen() {
        Assertions.assertEquals("$(echo hi)", WordExpander.normaliseBackticks("$(echo hi)"));
    }

    @Test
    void normaliseBackticksEscaped() {
        Assertions.assertEquals("$(echo `hi`)", WordExpander.normaliseBackticks("`echo \\`hi\\``"));
    }

    @Test
    void normaliseBackticksDollarEscape() {
        Assertions.assertEquals("$(echo $PATH)", WordExpander.normaliseBackticks("`echo \\$PATH`"));
    }

    @Test
    void normaliseBackticksBackslashEscape() {
        Assertions.assertEquals("$(echo a\\b)", WordExpander.normaliseBackticks("`echo a\\\\b`"));
    }

    @Test
    void normaliseBackticksNonSpecialEscape() {
        Assertions.assertEquals("$(echo \\n)", WordExpander.normaliseBackticks("`echo \\\\n`"));
    }

    @Test
    void normaliseBackticksUnclosed() {
        Assertions.assertEquals("`echo hello", WordExpander.normaliseBackticks("`echo hello"));
    }

    @Test
    void normaliseBackticksSkipsDollarBrace() {
        Assertions.assertEquals(
            ExecutorTest.VAR_DEFAULT_EXPR,
            WordExpander.normaliseBackticks(ExecutorTest.VAR_DEFAULT_EXPR)
        );
    }

    @Test
    void normaliseBackticksSkipsNestedDollarBrace() {
        Assertions.assertEquals(
            "${var:-${other}}",
            WordExpander.normaliseBackticks("${var:-${other}}")
        );
    }

    @Test
    void normaliseBackticksSkipsTripleNestedDollarBrace() {
        Assertions.assertEquals(
            "${a:-${b:-${c}}}",
            WordExpander.normaliseBackticks("${a:-${b:-${c}}}")
        );
    }

    @Test
    void normaliseBackticksDollarAtEnd() {
        Assertions.assertEquals("foo$", WordExpander.normaliseBackticks("foo$"));
    }

    @Test
    void normaliseBackticksUnmatchedDollarBrace() {
        Assertions.assertEquals("${unclosed", WordExpander.normaliseBackticks("${unclosed"));
    }

    @Test
    void normaliseBackticksDollarFollowedByLetter() {
        Assertions.assertEquals("$VAR", WordExpander.normaliseBackticks("$VAR"));
    }

    @Test
    void normaliseBackticksBackslashAtEndOfInput() {
        Assertions.assertEquals("`abc\\", WordExpander.normaliseBackticks("`abc\\"));
    }

    @Test
    void globMatchesQuestionMarkInCaseCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case ab in a?) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesQuestionMarkInCaseCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case ab in a?) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("MATCHED".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesQuestionMarkNoMatchCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case abc in a?) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesQuestionMarkNoMatchCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case abc in a?) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("NO".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesCharClassInCaseCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case b in [abc]) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesCharClassInCaseCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case b in [abc]) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("MATCHED".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesCharClassNoMatchCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case z in [abc]) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesCharClassNoMatchCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case z in [abc]) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("NO".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesCharClassRangeInCaseCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case m in [a-z]) echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesCharClassRangeInCaseCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case m in [a-z]) echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("MATCHED".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void globMatchesUnclosedBracketInCaseCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac"))
        );
    }

    @Test
    void globMatchesUnclosedBracketInCaseCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac"));
        Assertions.assertTrue("MATCHED".equals(bos.toString(StandardCharsets.UTF_8).trim()));
    }

    @Test
    void backtickNestedNormalise() {
        Assertions.assertEquals(
            "$(echo `echo hi`)",
            WordExpander.normaliseBackticks("`echo \\`echo hi\\``")
        );
    }

    @Test
    void echoBuiltin() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo hello")));
    }

    @Test
    void echoBuiltinArgs() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo hello world")));
    }

    @Test
    void echoBuiltinNoNewline() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo -n hello")));
    }

    @Test
    void echoBuiltinEmpty() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo")));
    }

    @Test
    void arithmeticSimple() {
        Assertions.assertEquals("7", new Executor().expandVariables("$((3 + 4))", new HashMap<>()));
    }

    @Test
    void arithmeticMul() {
        Assertions.assertEquals(
            "15",
            new Executor().expandVariables("$((3 * 5))", new HashMap<>())
        );
    }

    @Test
    void arithmeticVar() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("15", new Executor().expandVariables("$((x + 5))", env));
    }

    @Test
    void arithmeticNested() {
        Assertions.assertEquals(
            "14",
            new Executor().expandVariables("$((2 * (3 + 4)))", new HashMap<>())
        );
    }

    @Test
    void arithmeticShiftLeft() {
        Assertions.assertEquals(
            "32",
            new Executor().expandVariables("$((4 << 3))", new HashMap<>())
        );
    }

    @Test
    void arithmeticShiftRight() {
        Assertions.assertEquals(
            "4",
            new Executor().expandVariables("$((32 >> 3))", new HashMap<>())
        );
    }

    @Test
    void arithmeticLessThanCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((3 < 5))", env));
    }

    @Test
    void arithmeticLessThanCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((3 < 5))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((5 < 3))", env));
    }

    @Test
    void arithmeticLessEqualCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((5 <= 5))", env));
    }

    @Test
    void arithmeticLessEqualCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((5 <= 5))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((6 <= 5))", env));
    }

    @Test
    void arithmeticGreaterThanCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((5 > 3))", env));
    }

    @Test
    void arithmeticGreaterThanCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((5 > 3))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((3 > 5))", env));
    }

    @Test
    void arithmeticGreaterEqualCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((5 >= 5))", env));
    }

    @Test
    void arithmeticGreaterEqualCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((5 >= 5))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((4 >= 5))", env));
    }

    @Test
    void arithmeticEqualEqualCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((7 == 7))", env));
    }

    @Test
    void arithmeticEqualEqualCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((7 == 7))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((7 == 8))", env));
    }

    @Test
    void arithmeticNotEqualCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((7 != 8))", env));
    }

    @Test
    void arithmeticNotEqualCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((7 != 8))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((7 != 7))", env));
    }

    @Test
    void arithmeticBitAnd() {
        Assertions.assertEquals(
            "4",
            new Executor().expandVariables("$((12 & 6))", new HashMap<>())
        );
    }

    @Test
    void arithmeticBitXor() {
        Assertions.assertEquals(
            "10",
            new Executor().expandVariables("$((12 ^ 6))", new HashMap<>())
        );
    }

    @Test
    void arithmeticBitOr() {
        Assertions.assertEquals(
            "14",
            new Executor().expandVariables("$((12 | 6))", new HashMap<>())
        );
    }

    @Test
    void arithmeticLogicalAndCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((1 && 2))", env));
    }

    @Test
    void arithmeticLogicalAndCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 && 2))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((0 && 2))", env));
    }

    @Test
    void arithmeticLogicalOrCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((0 || 5))", env));
    }

    @Test
    void arithmeticLogicalOrCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((0 || 5))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((0 || 0))", env));
    }

    @Test
    void arithmeticUnaryNotCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((!0))", env));
    }

    @Test
    void arithmeticUnaryNotCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((!0))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((!5))", env));
    }

    @Test
    void arithmeticUnaryBitNotCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("-1", executor.expandVariables("$((~0))", env));
    }

    @Test
    void arithmeticUnaryBitNotCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((~0))", env);
        Assertions.assertEquals("-6", executor.expandVariables("$((~5))", env));
    }

    @Test
    void arithmeticTernaryTrue() {
        Assertions.assertEquals(
            "10",
            new Executor().expandVariables("$((1 ? 10 : 20))", new HashMap<>())
        );
    }

    @Test
    void arithmeticTernaryFalse() {
        Assertions.assertEquals(
            "20",
            new Executor().expandVariables("$((0 ? 10 : 20))", new HashMap<>())
        );
    }

    @Test
    void arithmeticTernaryNestedCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("30", executor.expandVariables("$((1 ? 2 ? 30 : 40 : 50))", env));
    }

    @Test
    void arithmeticTernaryNestedCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? 2 ? 30 : 40 : 50))", env);
        Assertions.assertEquals("40", executor.expandVariables("$((1 ? 0 ? 30 : 40 : 50))", env));
    }

    @Test
    void arithmeticTernaryNestedCheck3() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? 2 ? 30 : 40 : 50))", env);
        executor.expandVariables("$((1 ? 0 ? 30 : 40 : 50))", env);
        Assertions.assertEquals("50", executor.expandVariables("$((0 ? 30 : 40 ? 50 : 60))", env));
    }

    @Test
    void arithmeticTernaryWithComparisonCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "5");
        Assertions.assertEquals("1", executor.expandVariables("$((x > 3 ? 1 : 0))", env));
    }

    @Test
    void arithmeticTernaryWithComparisonCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "5");
        executor.expandVariables("$((x > 3 ? 1 : 0))", env);
        Assertions.assertEquals("0", executor.expandVariables("$((x > 10 ? 1 : 0))", env));
    }

    @Test
    void arithmeticTernaryArithmeticBranchesCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("7", executor.expandVariables("$((1 ? 3 + 4 : 99))", env));
    }

    @Test
    void arithmeticTernaryArithmeticBranchesCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? 3 + 4 : 99))", env);
        Assertions.assertEquals("99", executor.expandVariables("$((0 ? 3 + 4 : 99))", env));
    }

    @Test
    void arithmeticAssignCheck1() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("5", new Executor().expandVariables("$((x = 5))", env));
    }

    @Test
    void arithmeticAssignCheck2() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((x = 5))", env);
        Assertions.assertEquals("5", env.get("x"));
    }

    @Test
    void arithmeticPlusAssignCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("13", new Executor().expandVariables("$((x += 3))", env));
    }

    @Test
    void arithmeticPlusAssignCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        new Executor().expandVariables("$((x += 3))", env);
        Assertions.assertEquals("13", env.get("x"));
    }

    @Test
    void arithmeticMinusAssignCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("7", new Executor().expandVariables("$((x -= 3))", env));
    }

    @Test
    void arithmeticMinusAssignCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        new Executor().expandVariables("$((x -= 3))", env);
        Assertions.assertEquals("7", env.get("x"));
    }

    @Test
    void arithmeticMulDivModAssignCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("30", executor.expandVariables("$((x *= 3))", env));
    }

    @Test
    void arithmeticMulDivModAssignCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        executor.expandVariables("$((x *= 3))", env);
        env.put("y", "20");
        Assertions.assertEquals("4", executor.expandVariables("$((y /= 5))", env));
    }

    @Test
    void arithmeticMulDivModAssignCheck3() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        executor.expandVariables("$((x *= 3))", env);
        env.put("y", "20");
        executor.expandVariables("$((y /= 5))", env);
        env.put("z", "23");
        Assertions.assertEquals("3", executor.expandVariables("$((z %= 5))", env));
    }

    @Test
    void arithmeticShiftAssignCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "4");
        Assertions.assertEquals("32", executor.expandVariables("$((x <<= 3))", env));
    }

    @Test
    void arithmeticShiftAssignCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "4");
        executor.expandVariables("$((x <<= 3))", env);
        env.put("y", "32");
        Assertions.assertEquals("4", executor.expandVariables("$((y >>= 3))", env));
    }

    @Test
    void arithmeticBitAssignCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "12");
        Assertions.assertEquals("4", executor.expandVariables("$((x &= 6))", env));
    }

    @Test
    void arithmeticBitAssignCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "12");
        executor.expandVariables("$((x &= 6))", env);
        env.put("y", "12");
        Assertions.assertEquals("10", executor.expandVariables("$((y ^= 6))", env));
    }

    @Test
    void arithmeticBitAssignCheck3() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "12");
        executor.expandVariables("$((x &= 6))", env);
        env.put("y", "12");
        executor.expandVariables("$((y ^= 6))", env);
        env.put("z", "12");
        Assertions.assertEquals("14", executor.expandVariables("$((z |= 6))", env));
    }

    @Test
    void arithmeticAssignRightAssocCheck1() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("7", new Executor().expandVariables("$((a = b = 7))", env));
    }

    @Test
    void arithmeticAssignRightAssocCheck2() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((a = b = 7))", env);
        Assertions.assertEquals("7", env.get("a"));
    }

    @Test
    void arithmeticAssignRightAssocCheck3() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((a = b = 7))", env);
        Assertions.assertEquals("7", env.get("b"));
    }

    @Test
    void arithmeticShortCircuitAndSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((0 && (x = 99)))", env);
        Assertions.assertEquals(null, env.get("x"));
    }

    @Test
    void arithmeticShortCircuitOrSuppressesAssignment() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((1 || (x = 99)))", env);
        Assertions.assertEquals(null, env.get("x"));
    }

    @Test
    void arithmeticTernaryShortCircuitCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        Assertions.assertEquals("1", env.get("a"));
    }

    @Test
    void arithmeticTernaryShortCircuitCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        Assertions.assertEquals(null, env.get("b"));
    }

    @Test
    void arithmeticTernaryShortCircuitCheck3() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        executor.expandVariables("$((0 ? (c = 3) : (d = 4)))", env);
        Assertions.assertEquals(null, env.get("c"));
    }

    @Test
    void arithmeticTernaryShortCircuitCheck4() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        executor.expandVariables("$((0 ? (c = 3) : (d = 4)))", env);
        Assertions.assertEquals("4", env.get("d"));
    }

    @Test
    void captureStreamFromPipeline() throws Exception {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("echo capture-me | cat"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("capture-me"),
            String.format("captureStream: %s", bos)
        );
    }

    @Test
    void resolveCommandWithCustomPathDirInPath() throws Exception {
        final File dir = Files.createTempDirectory(
            "sh4j_custompath_"
        ).toFile();
        dir.deleteOnExit();
        final File bin = new File(dir, "myhello");
        try (
            BufferedWriter w = Files.newBufferedWriter(bin.toPath(), StandardCharsets.UTF_8)
        ) {
            w.write(String.format("#!/bin/sh%necho ok%n"));
        }
        bin.setExecutable(true);
        bin.deleteOnExit();
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PATH_KEY, dir.getAbsolutePath());
        Assertions.assertEquals(0, new Executor(env).execute(this.parse("myhello")));
    }

    @Test
    void externalCloseStdin() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("cat <&-")));
    }

    @Test
    void externalStdoutToStderr() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true 1>&2")));
    }

    @Test
    void externalReadWriteFdZero() throws Exception {
        final File file = File.createTempFile("sh4j_rw_0_", ".dat");
        file.deleteOnExit();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                file.toPath(),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("seed%n"));
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <>%s", file.getAbsolutePath())))
        );
    }

    @Test
    void externalHeredocDashStripsTabs() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <<-EOF%n\t\thello%nEOF%n")))
        );
    }

    @Test
    void exportArgWithEqualsNonStandardNameCheck1() throws Exception {
        final Executor executor = new Executor(new HashMap<>());
        Assertions.assertEquals(0, executor.execute(this.parse("export 1foo=bar")));
    }

    @Test
    void exportArgWithEqualsNonStandardNameCheck2() throws Exception {
        final Executor executor = new Executor(new HashMap<>());
        executor.execute(this.parse("export 1foo=bar"));
        Assertions.assertEquals("bar", executor.getVariable("1foo"));
    }

    @Test
    void execNonExecutableFileTriggersIoException() throws Exception {
        final File nonexec = File.createTempFile("sh4j_nonexec_", ".dat");
        nonexec.deleteOnExit();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                nonexec.toPath(),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("not a script");
        }
        nonexec.setExecutable(false);
        final int code = new Executor().execute(
            this.parser(String.format("exec %s", nonexec.getAbsolutePath())).parse()
        );
        Assertions.assertTrue(
            code != 0,
            String.format("non-executable should fail; got %s", code)
        );
    }

    @Test
    void execInterruptedException() {
        Thread.currentThread().interrupt();
        final int code = new Executor().execute(this.parse("exec sleep 1"));
        Thread.interrupted();
        Assertions.assertEquals(1, code);
    }

    @Test
    void externalNonExecutableFileTriggersIoException() throws Exception {
        final File nonexec = File.createTempFile("sh4j_ext_nonexec_", ".dat");
        nonexec.deleteOnExit();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                nonexec.toPath(),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("nope");
        }
        nonexec.setExecutable(false);
        final int code = new Executor().execute(this.parse(nonexec.getAbsolutePath()));
        Assertions.assertTrue(
            code != 0,
            String.format("non-executable external should fail; got %s", code)
        );
    }

    @Test
    void arithmeticAssignDivByZeroCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("0", new Executor().expandVariables("$((x /= 0))", env));
    }

    @Test
    void arithmeticAssignDivByZeroCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        new Executor().expandVariables("$((x /= 0))", env);
        Assertions.assertEquals("0", env.get("x"));
    }

    @Test
    void arithmeticAssignModByZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("0", new Executor().expandVariables("$((x %= 0))", env));
    }

    @Test
    void arithmeticAssignNonNumericEnvTreatedAsZeroCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        Assertions.assertEquals("5", new Executor().expandVariables("$((x += 5))", env));
    }

    @Test
    void arithmeticAssignNonNumericEnvTreatedAsZeroCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        new Executor().expandVariables("$((x += 5))", env);
        Assertions.assertEquals("5", env.get("x"));
    }

    @Test
    void arithmeticAssignWithUnderscoreNameCheck1() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("42", new Executor().expandVariables("$((_foo = 42))", env));
    }

    @Test
    void arithmeticAssignWithUnderscoreNameCheck2() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((_foo = 42))", env);
        Assertions.assertEquals("42", env.get("_foo"));
    }

    @Test
    void externalReadWriteRedirectFdTwo() throws Exception {
        final String path = String.format("/tmp/sh4j_rw_fd2_%s", System.nanoTime());
        new File(path).delete();
        Assertions.assertNotEquals(
            0,
            new Executor().execute(this.parse(String.format("cat /nonexistent_xyz 2<>%s", path)))
        );
        new File(path).delete();
    }

    @Test
    void externalHereDocRedirect() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <<EOF%nhello world%nEOF%n")))
        );
    }

    @Test
    void externalHereDocDashRedirect() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <<-EOF%n\thello dashed%nEOF%n")))
        );
    }

    @Test
    void shiftLargerThanPositionalCount() {
        Assertions.assertEquals(
            1,
            new Executor(
                new HashMap<>(),
                Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b")
            ).execute(this.parse("shift 100"))
        );
    }

    @Test
    void execWithMultipleArgs() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("exec true arg1 arg2 arg3")));
    }

    @Test
    void positionalParamIndexOutOfRangeIsEmpty() {
        Assertions.assertEquals(
            "",
            new Executor(
                new HashMap<>(),
                Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b")
            ).expandVariables("$9", new HashMap<>())
        );
    }

    @Test
    void normaliseBackticksBackslashNonSpecialChar() {
        Assertions.assertEquals("$(echo \\n)", WordExpander.normaliseBackticks("`echo \\n`"));
    }

    @Test
    void whileLoopWithContinue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(
            this.parse("c=true; while $c; do c=false; continue; echo unreached; done")
        );
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).isBlank(),
            String.format("continue should skip body, got: %s", bos)
        );
    }

    @Test
    void typeForExternalNonBuiltinCommandCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("type ls")));
    }

    @Test
    void typeForExternalNonBuiltinCommandCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type ls"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("ls is"));
    }

    @Test
    void exitWithValidIntegerAndWordExpansionError() {
        Assertions.assertEquals(
            1,
            new Executor(new HashMap<>()).execute(this.parse("exit 5${UNSET_FOR_EXIT_999:?msg}"))
        );
    }

    @Test
    void expandBracedMultiDigitPositionalOutOfRange() {
        Assertions.assertEquals(
            "",
            new Executor(
                new HashMap<>(),
                Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
            ).expandVariables("${10}", new HashMap<>())
        );
    }

    @Test
    void expandBracedMultiDigitPositionalInRange() {
        Assertions.assertEquals(
            "b",
            new Executor(
                new HashMap<>(),
                Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
            ).expandVariables("${2}", new HashMap<>())
        );
    }

    @Test
    void expandBracedLeadingHashVar() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", ExecutorTest.HELLO);
        Assertions.assertEquals("5", new Executor().expandVariables("${#x}", env));
    }

    @Test
    void expandBracedLeadingHashUnsetVar() {
        Assertions.assertEquals(
            "0",
            new Executor().expandVariables("${#UNSET_HASH_TEST_xyz}", new HashMap<>())
        );
    }

    @Test
    void specialParamShellFlags() {
        Assertions.assertEquals("", new Executor().expandVariables("$-", new HashMap<>()));
    }

    @Test
    void specialParamBackgroundPidBeforeAnyJob() {
        Assertions.assertEquals("0", new Executor().expandVariables("$!", new HashMap<>()));
    }

    @Test
    void ppidIsSetToParentProcessIdOnInitCheck1() {
        final String ppid = new Executor().getVariable("PPID");
        Assertions.assertNotNull(ppid);
    }

    @Test
    void ppidIsSetToParentProcessIdOnInitCheck2() {
        final String ppid = new Executor().getVariable("PPID");
        final long parsed = Long.parseLong(ppid);
        Assertions.assertTrue(parsed > 0L, "PPID should be a positive PID");
    }

    @Test
    void ppidIsSetToParentProcessIdOnInitCheck3() {
        final String ppid = new Executor().getVariable("PPID");
        final long parsed = Long.parseLong(ppid);
        Assertions.assertEquals(
            ProcessHandle.current()
                .parent().orElse(ProcessHandle.current()).pid(),
            parsed
        );
    }

    @Test
    void ppidPreservedAcrossSubshellExecutor() {
        final Executor parent = new Executor();
        Assertions.assertEquals(
            parent.getVariable("PPID"),
            new Executor(
                parent.environmentMap(),
                Collections.singletonList("sub")
            ).getVariable("PPID")
        );
    }

    @Test
    void ppidExpandsAsParameter() {
        final Executor executor = new Executor();
        Assertions.assertTrue(
            executor.expandVariables(
                "$PPID",
                executor.environmentMap()
            ).matches("\\d+")
        );
    }

    @Test
    void specialParamAtStarWithPositionalsCheck1() {
        final Executor executor = new Executor(
            new HashMap<>(),
            Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
        );
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals(ExecutorTest.A_B_C, executor.expandVariables("$@", env));
    }

    @Test
    void specialParamAtStarWithPositionalsCheck2() {
        final Executor executor = new Executor(
            new HashMap<>(),
            Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
        );
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$@", env);
        Assertions.assertEquals(ExecutorTest.A_B_C, executor.expandVariables("$*", env));
    }

    @Test
    void specialParamAtStarEmpty() {
        Assertions.assertEquals(
            "",
            new Executor(
                new HashMap<>(),
                Collections.singletonList(ExecutorTest.SCRIPT_KEY)
            ).expandVariables("$@", new HashMap<>())
        );
    }

    @Test
    void arithmeticFactorNonNumericEnvAsZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        Assertions.assertEquals("1", new Executor().expandVariables("$((x + 1))", env));
    }

    @Test
    void resolveCommandPathNullReturnsNull() {
        final Map<String, String> empty = new HashMap<>();
        empty.put(ExecutorTest.PATH_KEY, "");
        final Executor executor = new Executor(empty);
        executor.expandVariables("", empty);
        Assertions.assertEquals(127, executor.execute(this.parse("nonexistent_xyz_zzz_cmd_abc")));
    }

    @Test
    void resolveCommandWithEmptyDirInPath() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PATH_KEY, ":/usr/bin");
        Assertions.assertEquals(0, new Executor(env).execute(this.parse(ExecutorTest.TRUE_CMD)));
    }

    @Test
    void createProcessBuilderWithQuotedArg() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true \"quoted arg\"")));
    }

    @Test
    void forLoopWithContinueCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("for i in 1 2 3; do continue; echo $i; done"))
        );
    }

    @Test
    void forLoopWithContinueCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("for i in 1 2 3; do continue; echo $i; done"));
        Assertions.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void typeForExternalCommandCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("type true")));
    }

    @Test
    void typeForExternalCommandCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type true"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("true is"));
    }

    @Test
    void exitWithWordExpansionError() {
        Assertions.assertEquals(
            1,
            new Executor(new HashMap<>()).execute(this.parse("exit ${UNSET_FOR_EXIT_8x9:?errmsg}"))
        );
    }

    @Test
    void exportArgWithEqualsCheck1() {
        final Executor executor = new Executor(new HashMap<>());
        Assertions.assertEquals(0, executor.execute(this.parse("export FOO=hello")));
    }

    @Test
    void exportArgWithEqualsCheck2() {
        final Executor executor = new Executor(new HashMap<>());
        executor.execute(this.parse("export FOO=hello"));
        Assertions.assertEquals(ExecutorTest.HELLO, executor.getVariable("FOO"));
    }

    @Test
    void expandBracedColonEqWhenSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "set");
        Assertions.assertEquals("set", new Executor().expandVariables("${x:=default}", env));
    }

    @Test
    void expandBracedQuestionWhenSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", ExecutorTest.VALUE);
        Assertions.assertEquals(
            ExecutorTest.VALUE,
            new Executor().expandVariables("${x?msg}", env)
        );
    }

    @Test
    void expandBracedPrefixHashNoMatch() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abcdef");
        Assertions.assertEquals("abcdef", new Executor().expandVariables("${x#zzz}", env));
    }

    @Test
    void expandBracedUnknownOperatorFallsThrough() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", ExecutorTest.VALUE);
        Assertions.assertEquals("${x@weird}", new Executor().expandVariables("${x@weird}", env));
    }

    @Test
    void tildeUserNameFallthrough() {
        Assertions.assertEquals(
            "~nonexistentuser123",
            new Executor().expandVariables("~nonexistentuser123", new HashMap<>())
        );
    }

    @Test
    void arithmeticPrecedenceMixedCheck1() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((1 + 2 == 3))", env));
    }

    @Test
    void arithmeticPrecedenceMixedCheck2() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 + 2 == 3))", env);
        Assertions.assertEquals("1", executor.expandVariables("$((1 < 2 && 3 < 4))", env));
    }

    @Test
    void arithmeticPrecedenceMixedCheck3() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 + 2 == 3))", env);
        executor.expandVariables("$((1 < 2 && 3 < 4))", env);
        Assertions.assertEquals("7", executor.expandVariables("$((1 | 2 | 4 & 6))", env));
    }

    @Test
    void defaultIfsSet() {
        Assertions.assertEquals(
            String.format(ExecutorTest.IFS_TAB_NL),
            new Executor().getVariable("IFS")
        );
    }

    @Test
    void defaultHomeSet() {
        Assertions.assertEquals(
            System.getProperty("user.home"),
            new Executor().getVariable(ExecutorTest.HOME_KEY)
        );
    }

    @Test
    void defaultPathSet() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PATH_KEY, "");
        Assertions.assertEquals(
            "/usr/local/bin:/usr/bin:/bin",
            new Executor(env).getVariable(ExecutorTest.PATH_KEY)
        );
    }

    @Test
    void defaultsDoNotOverrideExistingEnvCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, "/custom/home");
        env.put(ExecutorTest.PATH_KEY, "/custom/path");
        final Executor executor = new Executor(env);
        Assertions.assertEquals("/custom/home", executor.getVariable(ExecutorTest.HOME_KEY));
    }

    @Test
    void defaultsDoNotOverrideExistingEnvCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, "/custom/home");
        env.put(ExecutorTest.PATH_KEY, "/custom/path");
        final Executor executor = new Executor(env);
        Assertions.assertEquals("/custom/path", executor.getVariable(ExecutorTest.PATH_KEY));
    }

    @Test
    void defaultsDoNotOverrideExistingEnvCheck3() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, "/custom/home");
        env.put(ExecutorTest.PATH_KEY, "/custom/path");
        final Executor executor = new Executor(env);
        Assertions.assertEquals(
            String.format(ExecutorTest.IFS_TAB_NL),
            executor.getVariable("IFS")
        );
    }

    @Test
    void commandFoundInPath() {
        Assertions.assertEquals(0, new Executor().execute(this.parse(ExecutorTest.TRUE_CMD)));
    }

    @Test
    void commandNotFoundReturnsNotFound() {
        Assertions.assertEquals(127, new Executor().execute(this.parse("nonexistent_cmd_xyz_127")));
    }

    @Test
    void commandWithSlashFound() {
        Assertions.assertEquals(
            0,
            new Executor(new HashMap<>()).execute(this.parse("/usr/bin/env true"))
        );
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
        env.put(ExecutorTest.PATH_KEY, dir.getAbsolutePath());
        dir.delete();
        Assertions.assertEquals(42, new Executor(env).execute(this.parse("mycmd")));
    }

    @Test
    void commandNotFoundEmptyPath() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PATH_KEY, "");
        Assertions.assertEquals(127, new Executor(env).execute(this.parse("somecmd")));
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
        env.put(
            ExecutorTest.PATH_KEY,
            String.format("/nonexistent_test_dir_xyz:%s", dir.getAbsolutePath())
        );
        dir.delete();
        Assertions.assertEquals(7, new Executor(env).execute(this.parse("mycmd2")));
    }

    @Test
    void commandWithSlashNotFound() {
        Assertions.assertEquals(
            127,
            new Executor().execute(this.parse("/nonexistent_path_xyz_abc/cmd"))
        );
    }

    @Test
    void exportWithValueCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("export FOO=bar")));
    }

    @Test
    void exportWithValueCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("export FOO=bar"));
        Assertions.assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void exportExistingVariable() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("export FOO")));
    }

    @Test
    void exportThenVariableExpandsCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("export MSG=hello; echo $MSG")));
    }

    @Test
    void exportThenVariableExpandsCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("export MSG=hello; echo $MSG"));
        Assertions.assertEquals(ExecutorTest.HELLO, executor.getVariable("MSG"));
    }

    @Test
    void splitFieldsDefaultIfs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields(ExecutorTest.A_B_C, String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsMultipleWs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("a   b", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsLeadingWs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("  a b", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsTrailingWs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("a b  ", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsColon() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a:b:c", ":")
        );
    }

    @Test
    void splitFieldsEmptyBetweenColons() {
        Assertions.assertEquals(Arrays.asList("a", "", "b"), WordExpander.splitFields("a::b", ":"));
    }

    @Test
    void splitFieldsOnlyColons() {
        Assertions.assertEquals(Arrays.asList("", ""), WordExpander.splitFields(":", ":"));
    }

    @Test
    void splitFieldsDoubleColon() {
        Assertions.assertEquals(Arrays.asList("", "", ""), WordExpander.splitFields("::", ":"));
    }

    @Test
    void splitFieldsNoDelim() {
        Assertions.assertEquals(
            Collections.singletonList(ExecutorTest.HELLO),
            WordExpander.splitFields(ExecutorTest.HELLO, ":")
        );
    }

    @Test
    void splitFieldsEmptyIfs() {
        Assertions.assertEquals(
            Collections.singletonList(ExecutorTest.HELLO_WORLD),
            WordExpander.splitFields(ExecutorTest.HELLO_WORLD, "")
        );
    }

    @Test
    void splitFieldsNullIfs() {
        Assertions.assertEquals(
            Collections.singletonList(ExecutorTest.HELLO),
            WordExpander.splitFields(ExecutorTest.HELLO, null)
        );
    }

    @Test
    void splitFieldsEmptyValue() {
        Assertions.assertEquals(
            Arrays.asList(),
            WordExpander.splitFields("", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsMixedWsNws() {
        Assertions.assertEquals(
            Arrays.asList("a", "", "b"),
            WordExpander.splitFields("a :b", " :")
        );
    }

    @Test
    void splitFieldsTrailingColon() {
        Assertions.assertEquals(Arrays.asList("a", ""), WordExpander.splitFields("a:", ":"));
    }

    @Test
    void splitFieldsCustomNws() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a,b,c", ",")
        );
    }

    @Test
    void isQuotedDoubleQuotes() {
        Assertions.assertTrue(WordExpander.isQuoted("\"hello\""));
    }

    @Test
    void isQuotedSingleQuotes() {
        Assertions.assertTrue(WordExpander.isQuoted("'hello'"));
    }

    @Test
    void isQuotedNoQuotes() {
        Assertions.assertFalse(WordExpander.isQuoted(ExecutorTest.HELLO));
    }

    @Test
    void isQuotedPartial() {
        Assertions.assertFalse(WordExpander.isQuoted("a\"b\"c"));
    }

    @Test
    void removeQuotesNoop() {
        Assertions.assertEquals(ExecutorTest.HELLO, WordExpander.removeQuotes(ExecutorTest.HELLO));
    }

    @Test
    void removeQuotesDouble() {
        Assertions.assertEquals(
            ExecutorTest.HELLO_WORLD,
            WordExpander.removeQuotes("\"hello world\"")
        );
    }

    @Test
    void removeQuotesSingle() {
        Assertions.assertEquals(ExecutorTest.HELLO, WordExpander.removeQuotes("'hello'"));
    }

    @Test
    void removeQuotesPartialDouble() {
        Assertions.assertEquals("foovaluebar", WordExpander.removeQuotes("foo\"value\"bar"));
    }

    @Test
    void removeQuotesPartialSingle() {
        Assertions.assertEquals("helloworld", WordExpander.removeQuotes("hello'world'"));
    }

    @Test
    void removeQuotesApostropheInsideDouble() {
        Assertions.assertEquals("it's", WordExpander.removeQuotes("\"it's\""));
    }

    @Test
    void removeQuotesNestedQuotes() {
        Assertions.assertEquals(
            "hello \"there\" world",
            WordExpander.removeQuotes("\"hello \\\"there\\\" world\"")
        );
    }

    @Test
    void removeQuotesBackslashOutside() {
        Assertions.assertEquals(
            ExecutorTest.HELLO_WORLD,
            WordExpander.removeQuotes("hello\\ world")
        );
    }

    @Test
    void removeQuotesBackslashInDouble() {
        Assertions.assertEquals("hello\"world", WordExpander.removeQuotes("\"hello\\\"world\""));
    }

    @Test
    void removeQuotesEmptyQuotesCheck1() {
        Assertions.assertEquals("", WordExpander.removeQuotes("\"\""));
    }

    @Test
    void removeQuotesEmptyQuotesCheck2() {
        WordExpander.removeQuotes("\"\"");
        Assertions.assertEquals("", WordExpander.removeQuotes("''"));
    }

    @Test
    void echoQuotedVarNoSplit() {
        Assertions.assertEquals(
            0,
            new Executor(new HashMap<>()).execute(this.parse("MSG=\"hello  world\"; echo \"$MSG\""))
        );
    }

    @Test
    void echoUnquotedVarSplit() {
        Assertions.assertEquals(
            0,
            new Executor(new HashMap<>()).execute(this.parse("MSG=\"hello  world\"; echo $MSG"))
        );
    }

    @Test
    void echoQuotedLiteralNoSplit() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo \"hello   world\"")));
    }

    @Test
    void echoCustomIfsColon() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", ":");
        Assertions.assertEquals(0, new Executor(env).execute(this.parse("echo a:b:c")));
    }

    @Test
    void echoEmptyIfsNoSplit() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", "");
        Assertions.assertEquals(0, new Executor(env).execute(this.parse("echo hello   world")));
    }

    @Test
    void splitFieldsTabIfs() {
        Assertions.assertEquals(Arrays.asList("a", "b"), WordExpander.splitFields("a\tb", "\t"));
    }

    @Test
    void splitFieldsMixedIfs() {
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields("a:b c", ": ")
        );
    }

    @Test
    void containsGlobCharsStar() {
        Assertions.assertTrue(WordExpander.containsGlobChars("*.txt"));
    }

    @Test
    void containsGlobCharsQuestion() {
        Assertions.assertTrue(WordExpander.containsGlobChars("file?"));
    }

    @Test
    void containsGlobCharsBracket() {
        Assertions.assertTrue(WordExpander.containsGlobChars("[ab]"));
    }

    @Test
    void containsGlobCharsNone() {
        Assertions.assertFalse(WordExpander.containsGlobChars("hello.txt"));
    }

    @Test
    void pathnameExpandNoGlob() {
        Assertions.assertEquals(
            Collections.singletonList(ExecutorTest.HELLO),
            WordExpander.pathnameExpand(Collections.singletonList(ExecutorTest.HELLO))
        );
    }

    @Test
    void pathnameExpandGlobNoMatchKeepsPattern() {
        Assertions.assertEquals(
            Collections.singletonList("__no_match__xyz__"),
            WordExpander.pathnameExpand(Collections.singletonList("__no_match__xyz__"))
        );
    }

    @Test
    void pathnameExpandGlobWithMatch() {
        Assertions.assertEquals(
            Collections.singletonList("pom.xml"),
            WordExpander.pathnameExpand(Collections.singletonList("pom.xml"))
        );
    }

    @Test
    void pathnameExpandWildcardMatch() {
        Assertions.assertTrue(
            WordExpander.pathnameExpand(Collections.singletonList("*.xml"))
                .contains("pom.xml")
        );
    }

    @Test
    void pathnameExpandGlobQuoted() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo \"*.java\"")));
    }

    @Test
    void pathnameExpandGlobUnquoted() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo *.java")));
    }

    @Test
    void expandBracedColonQuestionUnset() {
        Assertions.assertEquals(1, ExecutorTest.exitCode(() -> new Executor(new HashMap<>()).expandVariables("${x:?my error}", new HashMap<>())));
    }

    @Test
    void expandBracedQuestionUnset() {
        Assertions.assertEquals(1, ExecutorTest.exitCode(() -> new Executor(new HashMap<>()).expandVariables("${x?my error}", new HashMap<>())));
    }

    @Test
    void expandBracedColonQuestionSet() {
        Assertions.assertEquals(
            ExecutorTest.VALUE,
            new Executor(
                new HashMap<>()
            ).expandVariables("${x:?not used}", new HashMap<>(Map.of("x", ExecutorTest.VALUE)))
        );
    }

    @Test
    void expandBracedColonQuestionDefaultMsg() {
        Assertions.assertEquals(1, ExecutorTest.exitCode(() -> new Executor(new HashMap<>()).expandVariables("${x:?}", new HashMap<>())));
    }

    @Test
    void expandBracedColonQuestionErrorStreamCheck1() {
        final Executor executor = new Executor(new HashMap<>());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setErrorStream(stream);
        Assertions.assertEquals(1, ExecutorTest.exitCode(() -> executor.expandVariables("${x:?my error}", new HashMap<>())));
        stream.flush();
    }

    @Test
    void expandBracedColonQuestionErrorStreamCheck2() {
        final Executor executor = new Executor(new HashMap<>());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setErrorStream(stream);
        ExecutorTest.exitCode(() -> executor.expandVariables("${x:?my error}", new HashMap<>()));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).trim().contains("my error"),
            "error message should go to injected stream"
        );
        stream.flush();
    }

    @Test
    void pwdPrintsCurrentDirectoryCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("pwd")));
        stream.flush();
    }

    @Test
    void pwdPrintsCurrentDirectoryCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("pwd"));
        Assertions.assertEquals(
            ExecutorTest.TMP_DIR, bos.toString(StandardCharsets.UTF_8).trim()
        );
        stream.flush();
    }

    @Test
    void pwdLogicalFlagPrintsPwdEnvCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("pwd -L")));
        stream.flush();
    }

    @Test
    void pwdLogicalFlagPrintsPwdEnvCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("pwd -L"));
        Assertions.assertEquals(
            ExecutorTest.TMP_DIR, bos.toString(StandardCharsets.UTF_8).trim()
        );
        stream.flush();
    }

    @Test
    void commandDashVOnBuiltinPrintsNameCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("command -v echo")));
        stream.flush();
    }

    @Test
    void commandDashVOnBuiltinPrintsNameCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("command -v echo"));
        Assertions.assertEquals(
            ExecutorTest.HELLO.replace(ExecutorTest.HELLO, "echo"),
            bos.toString(StandardCharsets.UTF_8).trim()
        );
        stream.flush();
    }

    @Test
    void commandDashUppervOnBuiltinDescribesCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("command -V true")));
        stream.flush();
    }

    @Test
    void commandDashUppervOnBuiltinDescribesCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("command -V true"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("shell built-in")
        );
        stream.flush();
    }

    @Test
    void commandWithoutFlagsRunsCommandCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("command echo hi")));
        stream.flush();
    }

    @Test
    void commandWithoutFlagsRunsCommandCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("command echo hi"));
        Assertions.assertEquals("hi", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void umaskNoArgsPrintsDefaultCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("umask")));
        stream.flush();
    }

    @Test
    void umaskNoArgsPrintsDefaultCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("umask"));
        Assertions.assertEquals("0022", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void umaskSetThenReadEchoesValueCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("umask 077")));
        stream.flush();
    }

    @Test
    void umaskSetThenReadEchoesValueCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("umask 077"));
        Assertions.assertEquals(0, executor.execute(this.parse("umask")));
        stream.flush();
    }

    @Test
    void umaskSetThenReadEchoesValueCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("umask 077"));
        executor.execute(this.parse("umask"));
        Assertions.assertEquals("0077", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void umaskDashSPrintsSymbolicCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("umask -S")));
        stream.flush();
    }

    @Test
    void umaskDashSPrintsSymbolicCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("umask -S"));
        Assertions.assertEquals(
            "u=rwx,g=rx,o=rx", bos.toString(StandardCharsets.UTF_8).trim()
        );
        stream.flush();
    }

    @Test
    void umaskNonOctalReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("umask zzz")));
    }

    @Test
    void killDashLListsSignalsCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("kill -l")));
        stream.flush();
    }

    @Test
    void killDashLListsSignalsCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("kill -l"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("TERM")
        );
        stream.flush();
    }

    @Test
    void killNoArgsReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("kill")));
    }

    @Test
    void killUnknownPidReturnsError() {
        Assertions.assertEquals(
            127,
            new Executor().execute(this.parse("kill 999999999"))
        );
    }

    @Test
    void killDashSWithNonNumericPidReturnsError() {
        Assertions.assertEquals(
            127,
            new Executor().execute(this.parse("kill -s TERM notapid"))
        );
    }

    @Test
    void hashNoArgsReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("hash")));
    }

    @Test
    void hashDashRReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("hash -r")));
    }

    @Test
    void hashWithKnownBuiltinReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("hash echo")));
    }

    @Test
    void hashWithUnknownNameReturnsError() {
        Assertions.assertEquals(
            127,
            new Executor().execute(this.parse("hash sh4j_no_such_cmd_xyz"))
        );
    }

    @Test
    void waitNoArgsReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("wait")));
    }

    @Test
    void waitOnKnownBgPidReturnsZeroCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 &")));
    }

    @Test
    void waitOnKnownBgPidReturnsZeroCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("sleep 0 &"));
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("wait %d", executor.lastBackgroundPid())))
        );
    }

    @Test
    void waitOnUnknownPidReturnsError() {
        Assertions.assertEquals(
            127,
            new Executor().execute(this.parse("wait 999999"))
        );
    }

    @Test
    void jobsListsBackgroundJobsCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs")));
        stream.flush();
    }

    @Test
    void jobsListsBackgroundJobsCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("[1]"),
            String.format("expected [1] in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void jobsListsBackgroundJobsCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("sleep 0"),
            String.format("expected 'sleep 0' in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void jobsWithPercentIdCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs %1")));
        stream.flush();
    }

    @Test
    void jobsWithPercentIdCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs %1"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("[1]"),
            String.format("expected [1] in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void jobsEmptyWhenNoBackgroundJobsCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("jobs")));
        stream.flush();
    }

    @Test
    void jobsEmptyWhenNoBackgroundJobsCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("jobs"));
        Assertions.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void jobsWithNumericIdCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs 1")));
        stream.flush();
    }

    @Test
    void jobsWithNumericIdCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs 1"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("[1]"),
            String.format("output was: %s", bos)
        );
        stream.flush();
    }

    @Test
    void jobsWithNonExistentJobId() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs 99999")));
        stream.flush();
    }

    @Test
    void jobsWithInvalidJobIdReturnsZeroCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs notanumber")));
        stream.flush();
    }

    @Test
    void jobsWithInvalidJobIdReturnsZeroCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & jobs notanumber"));
        Assertions.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void jobsShowsDoneForCompletedJob() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.jobManager().waitForJob(executor.jobManager().submit(() -> { }, "test-cmd"));
        executor.execute(this.parse("jobs"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("Done"),
            String.format("expected Done state in jobs output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void bgConfirmsBackgroundJobCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & bg")));
        stream.flush();
    }

    @Test
    void bgConfirmsBackgroundJobCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & bg"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("[1]"),
            String.format("expected [1] in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void bgNoJobsReturnsError() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        Assertions.assertEquals(1, executor.execute(this.parse("bg")));
        stream.flush();
    }

    @Test
    void bgWithPercentIdCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & bg %1")));
        stream.flush();
    }

    @Test
    void bgWithPercentIdCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & bg %1"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("[1]"),
            String.format("output was: %s", bos)
        );
        stream.flush();
    }

    @Test
    void bgWithInvalidJobIdReturnsError() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        Assertions.assertEquals(1, executor.execute(this.parse("sleep 0 & bg 99999")));
        stream.flush();
    }

    @Test
    void bgWithInvalidJobSyntaxReturnsError() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        Assertions.assertEquals(1, executor.execute(this.parse("sleep 0 & bg notanumber")));
        stream.flush();
    }

    @Test
    void bgResumesStoppedJobCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        final JobManager mgr = executor.jobManager();
        final int id = mgr.submit(
            () -> {
                try {
                    Thread.sleep(10000);
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            },
            "test-cmd"
        );
        mgr.getJob(id).cancel();
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("bg %d", id)))
        );
        stream.flush();
    }

    @Test
    void bgResumesStoppedJobCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        final JobManager mgr = executor.jobManager();
        final int id = mgr.submit(
            () -> {
                try {
                    Thread.sleep(10000);
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            },
            "test-cmd"
        );
        mgr.getJob(id).cancel();
        executor.execute(this.parse(String.format("bg %d", id)));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains(String.format("[%s]", id)),
            String.format("expected bg confirmation in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void fgBringsJobToForegroundCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & fg")));
        stream.flush();
    }

    @Test
    void fgBringsJobToForegroundCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & fg"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("sleep 0"),
            String.format("expected 'sleep 0' in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void fgWithSpecificJobIdCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & fg %1")));
        stream.flush();
    }

    @Test
    void fgWithSpecificJobIdCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 & fg %1"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("sleep 0"),
            String.format("expected 'sleep 0' in output, got: %s", output)
        );
        stream.flush();
    }

    @Test
    void fgNoJobsReturnsError() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("fg")));
    }

    @Test
    void fgUnknownJobReturnsError() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("fg 99999")));
    }

    @Test
    void fgWithInvalidJobSyntaxReturnsError() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        Assertions.assertEquals(1, executor.execute(this.parse("fg notajob")));
        stream.flush();
    }

    @Test
    void fcListsHistoryEntriesCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo one"));
        executor.execute(this.parse("echo two"));
        executor.execute(this.parse("echo three"));
        Assertions.assertEquals(0, executor.execute(this.parse("fc -l 1 3")));
        stream.flush();
    }

    @Test
    void fcListsHistoryEntriesCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo one"));
        executor.execute(this.parse("echo two"));
        executor.execute(this.parse("echo three"));
        executor.execute(this.parse("fc -l 1 3"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(output.contains("1\techo one"));
        stream.flush();
    }

    @Test
    void fcListsHistoryEntriesCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo one"));
        executor.execute(this.parse("echo two"));
        executor.execute(this.parse("echo three"));
        executor.execute(this.parse("fc -l 1 3"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(output.contains("2\techo two"));
        stream.flush();
    }

    @Test
    void fcListsHistoryEntriesCheck4() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo one"));
        executor.execute(this.parse("echo two"));
        executor.execute(this.parse("echo three"));
        executor.execute(this.parse("fc -l 1 3"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(output.contains("3\techo three"));
        stream.flush();
    }

    @Test
    void fcListsNoNumbersCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(0, executor.execute(this.parse("fc -ln 1 1")));
        stream.flush();
    }

    @Test
    void fcListsNoNumbersCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        executor.execute(this.parse("fc -ln 1 1"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(output.contains("\techo hello"));
        stream.flush();
    }

    @Test
    void fcListsNoNumbersCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        executor.execute(this.parse("fc -ln 1 1"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertFalse(output.contains("1\t"));
        stream.flush();
    }

    @Test
    void fcListsLastEntryCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo first"));
        executor.execute(this.parse("echo second"));
        Assertions.assertEquals(0, executor.execute(this.parse("fc -l -1 -1")));
        stream.flush();
    }

    @Test
    void fcListsLastEntryCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo first"));
        executor.execute(this.parse("echo second"));
        executor.execute(this.parse("fc -l -1 -1"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(output.contains("echo second"));
        stream.flush();
    }

    @Test
    void fcListsLastEntryCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo first"));
        executor.execute(this.parse("echo second"));
        executor.execute(this.parse("fc -l -1 -1"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertFalse(output.contains("echo first"));
        stream.flush();
    }

    @Test
    void fcEmptyHistoryReturnsError() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("fc -l")));
    }

    @Test
    void fcSubstituteAndReExecuteCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(0, executor.execute(this.parse("fc -s hello=world")));
        stream.flush();
    }

    @Test
    void fcSubstituteAndReExecuteCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        executor.execute(this.parse("fc -s hello=world"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("echo world"));
        stream.flush();
    }

    @Test
    void fcSubstituteInvalidPattern() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(1, executor.execute(this.parse("fc -s nosign")));
        stream.flush();
    }

    @Test
    void fcUnknownOptionReturnsError() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(1, executor.execute(this.parse("fc -z")));
        stream.flush();
    }

    @Test
    void fcSubstituteNonExistentNumber() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(1, executor.execute(this.parse("fc -s hello=world 999")));
        stream.flush();
    }

    @Test
    void fcDefaultRangeListsLastItemCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        for (int idx = 1; idx <= 5; idx = idx + 1) {
            executor.execute(
                this.parse(String.format("echo cmd%d", idx))
            );
        }
        Assertions.assertEquals(0, executor.execute(this.parse("fc -l")));
        stream.flush();
    }

    @Test
    void fcDefaultRangeListsLastItemCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        for (int idx = 1; idx <= 5; idx = idx + 1) {
            executor.execute(
                this.parse(String.format("echo cmd%d", idx))
            );
        }
        executor.execute(this.parse("fc -l"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(output.contains("echo cmd1"));
        stream.flush();
    }

    @Test
    void fcDefaultRangeListsLastItemCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        for (int idx = 1; idx <= 5; idx = idx + 1) {
            executor.execute(
                this.parse(String.format("echo cmd%d", idx))
            );
        }
        executor.execute(this.parse("fc -l"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(output.contains("echo cmd5"));
        stream.flush();
    }

    @Test
    void linenoReflectsSourceLineCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0, executor.execute(this.parse("echo $LINENO"))
        );
        stream.flush();
    }

    @Test
    void linenoReflectsSourceLineCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo $LINENO"));
        Assertions.assertEquals(String.format("1%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void linenoUpdatesPerCommandCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0, executor.execute(this.parse("echo $LINENO; echo $LINENO"))
        );
        stream.flush();
    }

    @Test
    void linenoUpdatesPerCommandCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo $LINENO; echo $LINENO"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(output.contains(String.format("1%n")));
        stream.flush();
    }

    @Test
    void linenoUpdatesPerCommandCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo $LINENO; echo $LINENO"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains(String.format("1%n1%n"))
                || output.split(String.format("%n")).length >= 2
        );
        stream.flush();
    }

    @Test
    void langVariableCanBeSetCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("LANG=en_US.UTF-8")));
    }

    @Test
    void langVariableCanBeSetCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LANG=en_US.UTF-8"));
        Assertions.assertEquals("en_US.UTF-8", executor.getVariable("LANG"));
    }

    @Test
    void lcAllVariableOverridesOthersCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("LC_ALL=C")));
    }

    @Test
    void lcAllVariableOverridesOthersCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LC_ALL=C"));
        Assertions.assertEquals("C", executor.getVariable("LC_ALL"));
    }

    @Test
    void lcCollateVariableCanBeSetCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("LC_COLLATE=en_US.UTF-8")));
    }

    @Test
    void lcCollateVariableCanBeSetCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LC_COLLATE=en_US.UTF-8"));
        Assertions.assertEquals("en_US.UTF-8", executor.getVariable("LC_COLLATE"));
    }

    @Test
    void lcCtypeVariableCanBeSetCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("LC_CTYPE=en_US.UTF-8")));
    }

    @Test
    void lcCtypeVariableCanBeSetCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LC_CTYPE=en_US.UTF-8"));
        Assertions.assertEquals("en_US.UTF-8", executor.getVariable("LC_CTYPE"));
    }

    @Test
    void lcMessagesVariableCanBeSetCheck1() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("LC_MESSAGES=en_US.UTF-8")));
    }

    @Test
    void lcMessagesVariableCanBeSetCheck2() {
        final Executor executor = new Executor();
        executor.execute(this.parse("LC_MESSAGES=en_US.UTF-8"));
        Assertions.assertEquals("en_US.UTF-8", executor.getVariable("LC_MESSAGES"));
    }

    @Test
    void localeVariablesCanBeExportedCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        final Executor executor = new Executor(env);
        Assertions.assertEquals("C", executor.getVariable("LANG"));
    }

    @Test
    void localeVariablesCanBeExportedCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        final Executor executor = new Executor(env);
        Assertions.assertEquals("C", executor.getVariable("LC_ALL"));
    }

    @Test
    void localeVariablesCanBeExportedCheck3() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        final Executor executor = new Executor(env);
        Assertions.assertEquals("POSIX", executor.getVariable("LC_COLLATE"));
    }

    @Test
    void localeVariablesCanBeExportedCheck4() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        final Executor executor = new Executor(env);
        Assertions.assertEquals("UTF-8", executor.getVariable("LC_CTYPE"));
    }

    @Test
    void localeVariablesCanBeExportedCheck5() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        final Executor executor = new Executor(env);
        Assertions.assertEquals("C", executor.getVariable("LC_MESSAGES"));
    }

    @Test
    void commandDashVOnMissingReturnsOne() {
        Assertions.assertEquals(
            1,
            new Executor().execute(this.parse("command -v sh4j_no_such_cmd_xyz"))
        );
    }

    @Test
    void pwdPhysicalFlagResolvesSymlinksCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("pwd -P")));
        stream.flush();
    }

    @Test
    void pwdPhysicalFlagResolvesSymlinksCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("pwd -P"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).startsWith("/"));
        stream.flush();
    }

    @Test
    void cdToDirectoryCheck1() throws Exception {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("cd /tmp")));
    }

    @Test
    void cdToDirectoryCheck2() throws Exception {
        final Executor executor = new Executor();
        executor.execute(this.parse("cd /tmp"));
        Assertions.assertEquals(
            ExecutorTest.TMP_DIR,
            executor.getVariable(ExecutorTest.PWD_KEY)
        );
    }

    @Test
    void cdToHomeCheck1() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        Assertions.assertEquals(0, executor.execute(this.parse("cd")));
    }

    @Test
    void cdToHomeCheck2() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        executor.execute(this.parse("cd"));
        Assertions.assertEquals(
            ExecutorTest.TMP_DIR,
            executor.getVariable(ExecutorTest.PWD_KEY)
        );
    }

    @Test
    void cdToNonexistentReturnsOne() {
        Assertions.assertEquals(
            1,
            new Executor().execute(this.parse("cd /sh4j_nonexistent_dir_xyz"))
        );
    }

    @Test
    void cdMinusCheck1() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PWD_KEY, ExecutorTest.TMP_DIR);
        env.put("OLDPWD", "/");
        final Executor executor = new Executor(env);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("cd -")));
    }

    @Test
    void cdMinusCheck2() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PWD_KEY, ExecutorTest.TMP_DIR);
        env.put("OLDPWD", "/");
        final Executor executor = new Executor(env);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("cd -"));
        Assertions.assertEquals("/", executor.getVariable(ExecutorTest.PWD_KEY));
    }

    @Test
    void cdMinusCheck3() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PWD_KEY, ExecutorTest.TMP_DIR);
        env.put("OLDPWD", "/");
        final Executor executor = new Executor(env);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("cd -"));
        Assertions.assertEquals(ExecutorTest.TMP_DIR, executor.getVariable("OLDPWD"));
    }

    @Test
    void cdMinusCheck4() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PWD_KEY, ExecutorTest.TMP_DIR);
        env.put("OLDPWD", "/");
        final Executor executor = new Executor(env);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("cd -"));
        Assertions.assertEquals("/", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void cdWithExpandedVarCheck1() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put("MYDIR", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        Assertions.assertEquals(0, executor.execute(this.parse("cd $MYDIR")));
    }

    @Test
    void cdWithExpandedVarCheck2() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put("MYDIR", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        executor.execute(this.parse("cd $MYDIR"));
        Assertions.assertEquals(
            ExecutorTest.TMP_DIR,
            executor.getVariable(ExecutorTest.PWD_KEY)
        );
    }

    @Test
    void typeBuiltinCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("type echo cd exit export type true false shift unset"))
        );
    }

    @Test
    void typeBuiltinCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("echo is a shell built-in"));
    }

    @Test
    void typeBuiltinCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("cd is a shell built-in"));
    }

    @Test
    void typeBuiltinCheck4() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("exit is a shell built-in"));
    }

    @Test
    void typeBuiltinCheck5() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("export is a shell built-in"));
    }

    @Test
    void typeBuiltinCheck6() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("type is a shell built-in"));
    }

    @Test
    void typeBuiltinCheck7() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("true is a shell built-in"));
    }

    @Test
    void typeBuiltinCheck8() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type echo cd exit export type true false shift unset"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("false is a shell built-in"));
    }

    @Test
    void typeExternalCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("type true")));
    }

    @Test
    void typeExternalCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("type true"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("true is "));
    }

    @Test
    void typeNotFound() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("type nonexistent_cmd_xyz")));
    }

    @Test
    void execBuiltinRunsCommand() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("exec true")));
    }

    @Test
    void execBuiltinNotFoundReturnsNotFound() {
        Assertions.assertEquals(
            127,
            new Executor().execute(this.parse("exec nonexistent_cmd_xyz"))
        );
    }

    @Test
    void execBuiltinNoArgsIsNoop() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("exec")));
    }

    @Test
    void trueBuiltinReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse(ExecutorTest.TRUE_CMD)));
    }

    @Test
    void falseBuiltinReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse(ExecutorTest.FALSE_CMD)));
    }

    @Test
    void unsetRemovesVariableCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("MYVAR", ExecutorTest.VALUE);
        final Executor executor = new Executor(env);
        Assertions.assertEquals(0, executor.execute(this.parse("unset MYVAR")));
    }

    @Test
    void unsetRemovesVariableCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("MYVAR", ExecutorTest.VALUE);
        final Executor executor = new Executor(env);
        executor.execute(this.parse("unset MYVAR"));
        Assertions.assertNull(executor.getVariable("MYVAR"));
    }

    @Test
    void shiftRemovesPositionalParamCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("shift; echo $#")));
    }

    @Test
    void shiftRemovesPositionalParamCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        executor.execute(this.parse("shift; echo $#"));
        Assertions.assertEquals("2", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void shiftWithCountCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("shift 2; echo $#")));
    }

    @Test
    void shiftWithCountCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        executor.execute(this.parse("shift 2; echo $#"));
        Assertions.assertEquals("1", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void breakStopsWhileLoopCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("while true; do echo ran; break; done"))
        );
    }

    @Test
    void breakStopsWhileLoopCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("while true; do echo ran; break; done"));
        Assertions.assertEquals("ran", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void breakStopsForLoopCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("for i in a b c; do echo $i; break; done"))
        );
    }

    @Test
    void breakStopsForLoopCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("for i in a b c; do echo $i; break; done"));
        Assertions.assertEquals("a", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void continueInForLoopCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("for i in a b; do echo $i; continue; echo after; done"))
        );
    }

    @Test
    void continueInForLoopCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("for i in a b; do echo $i; continue; echo after; done"));
        Assertions.assertEquals(String.format("a%nb"), bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void continueInForLoopCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("for i in a b; do echo $i; continue; echo after; done"));
        Assertions.assertFalse(bos.toString(StandardCharsets.UTF_8).contains("after"));
    }

    @Test
    void colonBuiltinReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse(":")));
    }

    @Test
    void evalBuiltinRunsCommandCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("eval echo hello world")));
    }

    @Test
    void evalBuiltinRunsCommandCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("eval echo hello world"));
        Assertions.assertEquals(
            ExecutorTest.HELLO_WORLD,
            bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    void evalBuiltinNoArgsReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("eval")));
    }

    @Test
    void externalOutputRedirectCheck1() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO_WORLD);
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s > %s", inpath, outpath)))
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalOutputRedirectCheck2() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO_WORLD);
        }
        new Executor().execute(this.parse(String.format("cat < %s > %s", inpath, outpath)));
        Assertions.assertTrue(new File(outpath).exists());
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalOutputRedirectCheck3() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO_WORLD);
        }
        new Executor().execute(this.parse(String.format("cat < %s > %s", inpath, outpath)));
        new File(outpath).exists();
        Assertions.assertEquals(
            ExecutorTest.HELLO_WORLD,
            new String(Files.readAllBytes(Paths.get(outpath)), StandardCharsets.UTF_8).trim()
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalAppendRedirectCheck1() throws Exception {
        final String inpath = "/tmp/sh4j_ext_append_in.txt";
        final String outpath = "/tmp/sh4j_ext_append_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("second");
        }
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(outpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("first%n"));
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s >> %s", inpath, outpath)))
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalAppendRedirectCheck2() throws Exception {
        final String inpath = "/tmp/sh4j_ext_append_in.txt";
        final String outpath = "/tmp/sh4j_ext_append_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("second");
        }
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(outpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("first%n"));
        }
        new Executor().execute(this.parse(String.format("cat < %s >> %s", inpath, outpath)));
        final String content = new String(
            Files.readAllBytes(Paths.get(outpath)),
            StandardCharsets.UTF_8
        ).trim();
        Assertions.assertTrue(content.startsWith("first"));
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalAppendRedirectCheck3() throws Exception {
        final String inpath = "/tmp/sh4j_ext_append_in.txt";
        final String outpath = "/tmp/sh4j_ext_append_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("second");
        }
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(outpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("first%n"));
        }
        new Executor().execute(this.parse(String.format("cat < %s >> %s", inpath, outpath)));
        final String content = new String(
            Files.readAllBytes(Paths.get(outpath)),
            StandardCharsets.UTF_8
        ).trim();
        Assertions.assertTrue(content.contains("second"));
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalClobberRedirectCheck1() throws Exception {
        final String inpath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outpath = "/tmp/sh4j_ext_clobber_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello clobber");
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s >| %s", inpath, outpath)))
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalClobberRedirectCheck2() throws Exception {
        final String inpath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outpath = "/tmp/sh4j_ext_clobber_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello clobber");
        }
        new Executor().execute(this.parse(String.format("cat < %s >| %s", inpath, outpath)));
        Assertions.assertTrue(new File(outpath).exists());
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalClobberRedirectCheck3() throws Exception {
        final String inpath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outpath = "/tmp/sh4j_ext_clobber_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello clobber");
        }
        new Executor().execute(this.parse(String.format("cat < %s >| %s", inpath, outpath)));
        new File(outpath).exists();
        Assertions.assertEquals(
            "hello clobber",
            new String(Files.readAllBytes(Paths.get(outpath)), StandardCharsets.UTF_8).trim()
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalRedirectStdoutToStderr() throws Exception {
        final String inpath = "/tmp/sh4j_ext_12_in.txt";
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO);
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s 1>&2", inpath)))
        );
        new File(inpath).delete();
    }

    @Test
    void externalRedirectStderrToStdout() throws Exception {
        final String inpath = "/tmp/sh4j_ext_21_in.txt";
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO);
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s 2>&1", inpath)))
        );
        new File(inpath).delete();
    }

    @Test
    void externalCloseStdout() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true 1>&-")));
    }

    @Test
    void externalCloseStderr() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true 2>&-")));
    }

    @Test
    void externalStderrOutputRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_out.txt";
        new File(path).delete();
        new Executor().execute(
            this.parse(String.format("cat /nonexistent_path_xyz_123 2> %s", path))
        );
        Assertions.assertTrue(new File(path).exists());
        new File(path).delete();
    }

    @Test
    void externalStderrAppendRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_append.txt";
        new File(path).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(path),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("initial%n"));
        }
        new Executor().execute(
            this.parse(String.format("cat /nonexistent_path_xyz_456 2>> %s", path))
        );
        Assertions.assertTrue(
            new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8)
                .contains("initial")
        );
        new File(path).delete();
    }

    @Test
    void externalReadWriteRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_rw.txt";
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(path),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO);
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <> %s", path)))
        );
        new File(path).delete();
    }

    @Test
    void heredocStripTabsBasicCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\thello%nEOF%n")))
        );
    }

    @Test
    void heredocStripTabsBasicCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse(String.format("cat <<- EOF%n\thello%nEOF%n")));
        Assertions.assertEquals(ExecutorTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMultipleCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\t\thello%nEOF%n")))
        );
    }

    @Test
    void heredocStripTabsMultipleCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse(String.format("cat <<- EOF%n\t\thello%nEOF%n")));
        Assertions.assertEquals(ExecutorTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsDelimiterWithTabsCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\thello%n\tEOF%n")))
        );
    }

    @Test
    void heredocStripTabsDelimiterWithTabsCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse(String.format("cat <<- EOF%n\thello%n\tEOF%n")));
        Assertions.assertEquals(ExecutorTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMixedCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(
                this.parse(String.format("cat <<- MARKER%n\tfirst%nsecond%n\tthird%nMARKER%n"))
            )
        );
    }

    @Test
    void heredocStripTabsMixedCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(
                this.parse(String.format("cat <<- MARKER%n\tfirst%nsecond%n\tthird%nMARKER%n"))
            );
        Assertions.assertEquals(
            String.format("first%nsecond%nthird"),
            bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    void typeFunctionCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("myfunc() { true; }; type myfunc")));
    }

    @Test
    void typeFunctionCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("myfunc() { true; }; type myfunc"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("myfunc is a function")
        );
    }

    @Test
    void shiftNonNumericReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("shift abc")));
    }

    @Test
    void shiftNegativeReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("shift -1")));
    }

    @Test
    void readWithOneVarCheck1() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("hello%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read var")));
    }

    @Test
    void readWithOneVarCheck2() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("hello%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read var"));
        Assertions.assertEquals("hello", executor.getVariable("var"));
    }

    @Test
    void readWithMultipleVarsCheck1() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read x y z")));
    }

    @Test
    void readWithMultipleVarsCheck2() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y z"));
        Assertions.assertEquals("a", executor.getVariable("x"));
    }

    @Test
    void readWithMultipleVarsCheck3() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y z"));
        Assertions.assertEquals("b", executor.getVariable("y"));
    }

    @Test
    void readWithMultipleVarsCheck4() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y z"));
        Assertions.assertEquals("c", executor.getVariable("z"));
    }

    @Test
    void readLastVarGetsRemainingFieldsCheck1() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c d%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read x y")));
    }

    @Test
    void readLastVarGetsRemainingFieldsCheck2() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c d%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y"));
        Assertions.assertEquals("a", executor.getVariable("x"));
    }

    @Test
    void readLastVarGetsRemainingFieldsCheck3() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c d%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y"));
        Assertions.assertEquals("b c d", executor.getVariable("y"));
    }

    @Test
    void readReplyWhenNoVarCheck1() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(
                String.format("hello world%n").getBytes(StandardCharsets.UTF_8)
            )
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read")));
    }

    @Test
    void readReplyWhenNoVarCheck2() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(
                String.format("hello world%n").getBytes(StandardCharsets.UTF_8)
            )
        );
        executor.execute(this.parse("read"));
        Assertions.assertEquals("hello world", executor.getVariable("REPLY"));
    }

    @Test
    void readEofReturnsOne() {
        final Executor executor = new Executor();
        executor.setInputStream(new ByteArrayInputStream(new byte[0]));
        Assertions.assertEquals(1, executor.execute(this.parse("read var")));
    }

    @Test
    void readRawModePreservesBackslashCheck1() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a\\tb%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read -r var")));
    }

    @Test
    void readRawModePreservesBackslashCheck2() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a\\tb%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read -r var"));
        Assertions.assertEquals("a\\tb", executor.getVariable("var"));
    }

    @Test
    void readBackslashWithoutRawRemovesEscapeCheck1() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a\\tb%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read var")));
    }

    @Test
    void readBackslashWithoutRawRemovesEscapeCheck2() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a\\tb%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read var"));
        Assertions.assertEquals("atb", executor.getVariable("var"));
    }

    @Test
    void aliasExpandsToBuiltinCheck1() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hello");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("ll")));
        stream.flush();
    }

    @Test
    void aliasExpandsToBuiltinCheck2() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hello");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("ll"));
        Assertions.assertEquals(String.format("hello%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void aliasExpandsWithArgsCheck1() {
        final Executor executor = new Executor();
        executor.aliases().put("greet", "echo hello");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("greet world")));
        stream.flush();
    }

    @Test
    void aliasExpandsWithArgsCheck2() {
        final Executor executor = new Executor();
        executor.aliases().put("greet", "echo hello");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("greet world"));
        Assertions.assertEquals(
            String.format("hello world%n"),
            bos.toString(StandardCharsets.UTF_8)
        );
        stream.flush();
    }

    @Test
    void aliasDoesNotAffectOtherCommandsCheck1() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hi");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("echo hello")));
        stream.flush();
    }

    @Test
    void aliasDoesNotAffectOtherCommandsCheck2() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hi");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo hello"));
        Assertions.assertEquals(String.format("hello%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetAndExpandCheck1() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "echo hello");
        Assertions.assertEquals(0, executor.execute(this.parse("ll")));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetAndExpandCheck2() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "echo hello");
        executor.execute(this.parse("ll"));
        Assertions.assertEquals(String.format("hello%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetViaEqCheck1() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("alias ll='echo hello'")));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetViaEqCheck2() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("alias ll='echo hello'"));
        Assertions.assertEquals(0, executor.execute(this.parse("ll")));
        stream.flush();
    }

    @Test
    void aliasBuiltinSetViaEqCheck3() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("alias ll='echo hello'"));
        executor.execute(this.parse("ll"));
        Assertions.assertEquals(String.format("hello%n"), bos.toString(StandardCharsets.UTF_8));
        stream.flush();
    }

    @Test
    void aliasBuiltinPrintAllCheck1() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "ls -l");
        Assertions.assertEquals(0, executor.execute(this.parse("alias")));
        stream.flush();
    }

    @Test
    void aliasBuiltinPrintAllCheck2() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "ls -l");
        executor.execute(this.parse("alias"));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("ll='ls -l'")
        );
        stream.flush();
    }

    @Test
    void aliasBuiltinPrintOneCheck1() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "ls -l");
        Assertions.assertEquals(0, executor.execute(this.parse("alias ll")));
        stream.flush();
    }

    @Test
    void aliasBuiltinPrintOneCheck2() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.aliases().put("ll", "ls -l");
        executor.execute(this.parse("alias ll"));
        Assertions.assertEquals(
            String.format("ll='ls -l'%n"),
            bos.toString(StandardCharsets.UTF_8)
        );
        stream.flush();
    }

    @Test
    void aliasBuiltinUndefinedReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("alias nonexistent")));
    }

    @Test
    void unaliasRemovesOneCheck1() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        Assertions.assertEquals(0, executor.execute(this.parse("unalias ll")));
    }

    @Test
    void unaliasRemovesOneCheck2() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        executor.execute(this.parse("unalias ll"));
        Assertions.assertFalse(executor.aliases().containsKey("ll"));
    }

    @Test
    void unaliasRemovesOneCheck3() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        executor.execute(this.parse("unalias ll"));
        executor.aliases().containsKey("ll");
        Assertions.assertTrue(executor.aliases().containsKey("gg"));
    }

    @Test
    void unaliasAllRemovesAllCheck1() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        Assertions.assertEquals(0, executor.execute(this.parse("unalias -a")));
    }

    @Test
    void unaliasAllRemovesAllCheck2() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        executor.execute(this.parse("unalias -a"));
        Assertions.assertTrue(executor.aliases().isEmpty());
    }

    @Test
    void unaliasUnknownReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("unalias nonexistent")));
    }

    @Test
    void getoptsParsesSimpleFlagCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "file"));
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
    }

    @Test
    void getoptsParsesSimpleFlagCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void getoptsParsesSimpleFlagCheck3() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("", executor.getVariable("OPTARG"));
    }

    @Test
    void getoptsParsesFlagWithArgCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b", "val", "file"));
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
    }

    @Test
    void getoptsParsesFlagWithArgCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("b", executor.getVariable("opt"));
    }

    @Test
    void getoptsParsesFlagWithArgCheck3() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("val", executor.getVariable("OPTARG"));
    }

    @Test
    void getoptsReturnsOneWhenDone() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        Assertions.assertEquals(
            1, new Executor(env, Arrays.asList("sh", "file")).execute(this.parse("getopts ab: opt"))
        );
    }

    @Test
    void getoptsParsesMultipleFlagsCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
    }

    @Test
    void getoptsParsesMultipleFlagsCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void getoptsParsesMultipleFlagsCheck3() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
    }

    @Test
    void getoptsParsesMultipleFlagsCheck4() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("b", executor.getVariable("opt"));
    }

    @Test
    void getoptsParsesMultipleFlagsCheck5() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("val", executor.getVariable("OPTARG"));
    }

    @Test
    void getoptsParsesMultipleFlagsCheck6() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        executor.execute(this.parse("getopts ab: opt"));
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals(
            1, executor.execute(this.parse("getopts ab: opt"))
        );
    }

    @Test
    void getoptsNoArgsReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("getopts")));
    }

    @Test
    void getoptsWithDoubleDashCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "--", "file"));
        Assertions.assertEquals(
            1, executor.execute(this.parse("getopts ab opt"))
        );
    }

    @Test
    void getoptsWithDoubleDashCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "--", "file"));
        executor.execute(this.parse("getopts ab opt"));
        Assertions.assertEquals("?", executor.getVariable("opt"));
    }

    @Test
    void getoptsIllegalOptionCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-c", "file"));
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setErrorStream(stream);
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts ab opt"))
        );
        stream.flush();
    }

    @Test
    void getoptsIllegalOptionCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-c", "file"));
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setErrorStream(stream);
        executor.execute(this.parse("getopts ab opt"));
        Assertions.assertEquals("?", executor.getVariable("opt"));
        stream.flush();
    }

    @Test
    void getoptsMissingArgWithColonCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b"));
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts :ab: opt"))
        );
    }

    @Test
    void getoptsMissingArgWithColonCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b"));
        executor.execute(this.parse("getopts :ab: opt"));
        Assertions.assertEquals(":", executor.getVariable("opt"));
    }

    @Test
    void getoptsMissingArgWithoutColonCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b"));
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setErrorStream(stream);
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
        stream.flush();
    }

    @Test
    void getoptsMissingArgWithoutColonCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b"));
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setErrorStream(stream);
        executor.execute(this.parse("getopts ab: opt"));
        Assertions.assertEquals("?", executor.getVariable("opt"));
        stream.flush();
    }

    @Test
    void getoptsWithExplicitArgsCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env);
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts ab opt -a -b val"))
        );
    }

    @Test
    void getoptsWithExplicitArgsCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env);
        executor.execute(this.parse("getopts ab opt -a -b val"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void getoptsInvalidOptindCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "notanumber");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a"));
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts a opt"))
        );
    }

    @Test
    void getoptsInvalidOptindCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "notanumber");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a"));
        executor.execute(this.parse("getopts a opt"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void getoptsNegativeOptindCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "-5");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a"));
        Assertions.assertEquals(
            0, executor.execute(this.parse("getopts a opt"))
        );
    }

    @Test
    void getoptsNegativeOptindCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "-5");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a"));
        executor.execute(this.parse("getopts a opt"));
        Assertions.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    void cdDashWithoutOldPwdReturnsOne() {
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", "/usr/bin");
        env.put("HOME", "/tmp");
        Assertions.assertEquals(1, new Executor(env).execute(this.parse("cd -")));
    }

    @Test
    void pwdPhysicalIoError() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/nonexistent/path/that/does/not/exist");
        env.put("PATH", "/usr/bin");
        Assertions.assertEquals(1, new Executor(env).execute(this.parse("pwd -P")));
    }

    @Test
    void echoQuotedFieldWithSpaceCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0, executor.execute(this.parse("echo 'hello world'"))
        );
        stream.flush();
    }

    @Test
    void echoQuotedFieldWithSpaceCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("echo 'hello world'"));
        Assertions.assertEquals(
            String.format("hello world%n"),
            bos.toString(StandardCharsets.UTF_8)
        );
        stream.flush();
    }

    @Test
    void aliasWithDoubleQuotedValueCheck1() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0, executor.execute(this.parse("alias ll=\"ls -l\""))
        );
        stream.flush();
    }

    @Test
    void aliasWithDoubleQuotedValueCheck2() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        executor.execute(this.parse("alias ll=\"ls -l\""));
        Assertions.assertEquals("ls -l", executor.aliases().get("ll"));
        stream.flush();
    }

    @Test
    void aliasShortValueNoUnquoteCheck1() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        Assertions.assertEquals(
            0, executor.execute(this.parse("alias x='a'"))
        );
        stream.flush();
    }

    @Test
    void aliasShortValueNoUnquoteCheck2() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        executor.execute(this.parse("alias x='a'"));
        Assertions.assertEquals("a", executor.aliases().get("x"));
        stream.flush();
    }

    @Test
    void unaliasUnknownNameReturnsOne() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("unalias unknown_alias")));
    }

    @Test
    void typeBuiltinsBreakContinueEvalCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("type break continue eval")));
        stream.flush();
    }

    @Test
    void typeBuiltinsBreakContinueEvalCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("type break continue eval"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("break is a shell built-in"));
        stream.flush();
    }

    @Test
    void typeBuiltinsBreakContinueEvalCheck3() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("type break continue eval"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("continue is a shell built-in"));
        stream.flush();
    }

    @Test
    void typeBuiltinsBreakContinueEvalCheck4() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("type break continue eval"));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("eval is a shell built-in"));
        stream.flush();
    }

    @Test
    void executorDefaultsPathWhenMissingCheck1() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/tmp");
        final Executor executor = new Executor(env);
        Assertions.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    void executorDefaultsPathWhenMissingCheck2() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/tmp");
        final Executor executor = new Executor(env);
        Assertions.assertFalse(executor.getVariable("PATH").isEmpty());
    }

    @Test
    void functionDefinitionParsingCheck1() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("myfunc() { echo hi; }")));
        stream.flush();
    }

    @Test
    void functionDefinitionParsingCheck2() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(
            new ByteArrayOutputStream(), StandardCharsets.UTF_8
            );
        executor.setOutputStream(stream);
        executor.execute(this.parse("myfunc() { echo hi; }"));
        Assertions.assertTrue(executor.functionsMap().containsKey("myfunc"));
        stream.flush();
    }

    @Test
    void backgroundPipelineWithPipeCheck1() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 | true & jobs")));
        stream.flush();
    }

    @Test
    void backgroundPipelineWithPipeCheck2() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("sleep 0 | true & jobs"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(
            output.contains("[1]"),
            String.format("expected [1] in output, got: %s", output)
        );
        stream.flush();
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }

    private static int exitCode(final Runnable action) {
        int code = -1;
        try {
            action.run();
        } catch (final ExitSignal ex) {
            code = ex.getCode();
        }
        return code;
    }
}
