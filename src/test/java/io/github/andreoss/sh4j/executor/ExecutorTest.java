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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.SimpleCommand;

/**
 * Tests for Executor.
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

    /** CAT_INPUT_FMT constant. */
    private static final String CAT_INPUT_FMT = "cat < %s";

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
    void executeAssignmentOnly() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("FOO=bar")));
        Assertions.assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void executeAssignmentWithCommand() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("FOO=hello echo $FOO")));
    }

    @Test
    void executeMultipleAssignments() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("A=1 B=2")));
        Assertions.assertEquals("1", executor.getVariable("A"));
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
            executor.getLastBgPid() > 0, "lastBgPid should be > 0 after & command"
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
        Assertions.assertEquals(0, new Executor().execute(this.parse(String.format("echo hello 2>%s", "/tmp/sh4j_stderr_test"))));
    }

    @Test
    void executeBuiltinStderrAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stderr_append_%s", System.nanoTime());
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("echo hello 2>>%s", file)))
        );
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stdout_append_%s", System.nanoTime());
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("echo hello >>%s", file)))
        );
        new java.io.File(file).delete();
    }

    @Test
    void executeBuiltinStdoutRedirectIoException() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("echo hello >/dev/null/no_such_path_xyz"))
        );
    }

    @Test
    void executeBuiltinStderrRedirectIoException() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("echo hello 2>/dev/null/no_such_stderr_xyz"))
        );
    }

    @Test
    void executeBuiltinStdoutAppendIoException() throws Exception {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("echo hello >>/dev/null/no_such_append_xyz"))
        );
    }

    @Test
    void executeBuiltinStderrAppendIoException() throws Exception {
        Assertions.assertEquals(
            0,
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
        final ListNode list = this.parse("echo $EMPTY_VAR");
        final Map<String, String> env = new HashMap<>();
        env.put("EMPTY_VAR", "");
        Assertions.assertEquals(0, new Executor(env).execute(list));
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
        final Executor executor = new Executor();
        final int code = executor.execute(
            this.parse("echo hello | nonexistent_cmd_xyz_fail | echo done")
        );
        Assertions.assertTrue(code == 127 || code == 126,
                "Pipeline with missing middle stage must exit with POSIX "
                        + String.format(
                            "command-not-found(127) or not-executable(126); got %s",
                            code
                        ));
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
    void executeWhileBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("while exit $?; do echo ran; exit 1; done");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assertions.assertEquals(1, executor.execute(list));
        Assertions.assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeUntilBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("exit 1; until exit $?; do echo ran; exit 0; done");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assertions.assertEquals(0, executor.execute(list));
        Assertions.assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeWhileBodyExitCodePropagated() {
        Assertions.assertEquals(
            42,
            new Executor().execute(this.parse("while exit $?; do exit 42; done"))
        );
    }

    @Test
    void commandInterruptedException() {
        final ListNode list = this.parse("pwd");
        Thread.currentThread().interrupt();
        Assertions.assertEquals(1, new Executor().execute(list));
    }

    @Test
    void pipelineInterruptedException() {
        final ListNode list = this.parse("pwd | cat");
        Thread.currentThread().interrupt();
        Assertions.assertEquals(1, new Executor().execute(list));
    }

    @Test
    void waitForBackgroundJobsInterrupted() {
        final ListNode list = this.parse("sleep 1 &");
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        executor.execute(list);
        Assertions.assertTrue(
            Thread.interrupted(),
            "InterruptedException catch should re-set interrupt flag"
        );
    }

    @Test
    void exportNoArgs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_EXPORT_VAR", ExecutorTest.HELLO);
        final Executor executor = new Executor(env);
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assertions.assertEquals(0, executor.execute(this.parse("export")));
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("export TEST_EXPORT_VAR=hello")
        );
    }

    @Test
    void executeIfFalseNoElse() {
        Assertions.assertEquals(
            1,
            new Executor().execute(this.parse("if false; then echo yes; fi"))
        );
    }

    @Test
    void executeElifWhenIfFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse("if false; then echo no; elif true; then echo yes; fi");
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assertions.assertEquals(0, executor.execute(list));
        Assertions.assertEquals(String.format("yes%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeElifChainFalseFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse(
            "if false; then echo a; elif false; then echo b; else echo c; fi"
        );
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assertions.assertEquals(0, executor.execute(list));
        Assertions.assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeElifChainSecondElifTrue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ListNode list = this.parse(
            "if false; then echo a; elif false; then echo b; elif true; then echo c; fi"
        );
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assertions.assertEquals(0, executor.execute(list));
        Assertions.assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
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
    void heredocExpansion() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat << EOF%nhello $TEST_HVAR%nEOF%n")))
        );
        Assertions.assertEquals("hello expanded", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void redirectTargetExpansionOutput() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_redir_expand_test.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        Assertions.assertEquals(
            0,
            new Executor(env).execute(this.parse("echo hello > $TEST_FILE"))
        );
        Assertions.assertTrue(file.exists());
        Assertions.assertEquals(
            String.format("hello%n"),
            new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
        file.delete();
    }

    @Test
    void redirectTargetExpansionAppend() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_redir_append_expand.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        Assertions.assertEquals(
            0,
            new Executor(env).execute(this.parse("echo hello >> $TEST_FILE"))
        );
        Assertions.assertTrue(file.exists());
        Assertions.assertEquals(
            String.format("hello%n"),
            new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
        file.delete();
    }

    @Test
    void redirectTargetExpansionInput() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_redir_input_expand.txt");
        file.delete();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
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
        Assertions.assertEquals("hello from file", bos.toString(StandardCharsets.UTF_8).trim());
        file.delete();
    }

    @Test
    void executeNonExecutableCommand() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_nonexec_test.sh");
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
                List.of("sh", "a", "b", "c")
            ).expandVariables("$#", new HashMap<>())
        );
    }

    @Test
    void expandDollarAt() {
        Assertions.assertEquals(
            ExecutorTest.A_B_C,
            new Executor(
                new HashMap<>(),
                List.of("sh", "a", "b", "c")
            ).expandVariables("$@", new HashMap<>())
        );
    }

    @Test
    void expandDollarStar() {
        Assertions.assertEquals(
            ExecutorTest.A_B_C,
            new Executor(
                new HashMap<>(),
                List.of("sh", "a", "b", "c")
            ).expandVariables("$*", new HashMap<>())
        );
    }

    @Test
    void expandDollarZero() {
        Assertions.assertEquals(
            "my_script",
            new Executor(
                new HashMap<>(),
                List.of("my_script")
            ).expandVariables("$0", new HashMap<>())
        );
    }

    @Test
    void expandDollarOne() {
        Assertions.assertEquals(
            "first",
            new Executor(
                new HashMap<>(),
                List.of("sh", "first", "second")
            ).expandVariables("$1", new HashMap<>())
        );
    }

    @Test
    void expandDollarNine() {
        Assertions.assertEquals(
            "i",
            new Executor(
                new HashMap<>(),
                List.of("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i")
            ).expandVariables("$9", new HashMap<>())
        );
    }

    @Test
    void expandBracedMultiDigit() {
        Assertions.assertEquals(
            "j",
            new Executor(
                new HashMap<>(),
                List.of("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
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
    void expandDollarDollar() {
        final Executor executor = new Executor();
        final String pid = executor.expandVariables("$$", new HashMap<>());
        Assertions.assertNotNull(pid);
        Assertions.assertFalse(pid.isEmpty());
        Assertions.assertEquals(String.valueOf(ProcessHandle.current().pid()), pid);
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
    void expandVarAssignDefault() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals(
            "assigned",
            new Executor().expandVariables("${var:=assigned}", env)
        );
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
    void expandVarAssignNullOnly() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("assigned", new Executor().expandVariables("${var=assigned}", env));
        Assertions.assertEquals("assigned", env.get(ExecutorTest.VAR_NAME));
    }

    @Test
    void expandVarAssignNullOnlySetSkips() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "existing");
        Assertions.assertEquals("existing", new Executor().expandVariables("${var=assigned}", env));
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
    void globMatchesQuestionMarkInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case ab in a?) echo MATCHED;; *) echo NO;; esac"))
        );
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void globMatchesQuestionMarkNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case abc in a?) echo MATCHED;; *) echo NO;; esac"))
        );
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case b in [abc]) echo MATCHED;; *) echo NO;; esac"))
        );
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void globMatchesCharClassNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case z in [abc]) echo MATCHED;; *) echo NO;; esac"))
        );
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("NO"));
    }

    @Test
    void globMatchesCharClassRangeInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case m in [a-z]) echo MATCHED;; *) echo NO;; esac"))
        );
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    void globMatchesUnclosedBracketInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac"))
        );
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
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
    void arithmeticLessThan() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((3 < 5))", env));
        Assertions.assertEquals("0", executor.expandVariables("$((5 < 3))", env));
    }

    @Test
    void arithmeticLessEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((5 <= 5))", env));
        Assertions.assertEquals("0", executor.expandVariables("$((6 <= 5))", env));
    }

    @Test
    void arithmeticGreaterThan() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((5 > 3))", env));
        Assertions.assertEquals("0", executor.expandVariables("$((3 > 5))", env));
    }

    @Test
    void arithmeticGreaterEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((5 >= 5))", env));
        Assertions.assertEquals("0", executor.expandVariables("$((4 >= 5))", env));
    }

    @Test
    void arithmeticEqualEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((7 == 7))", env));
        Assertions.assertEquals("0", executor.expandVariables("$((7 == 8))", env));
    }

    @Test
    void arithmeticNotEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((7 != 8))", env));
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
    void arithmeticLogicalAnd() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((1 && 2))", env));
        Assertions.assertEquals("0", executor.expandVariables("$((0 && 2))", env));
    }

    @Test
    void arithmeticLogicalOr() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((0 || 5))", env));
        Assertions.assertEquals("0", executor.expandVariables("$((0 || 0))", env));
    }

    @Test
    void arithmeticUnaryNot() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((!0))", env));
        Assertions.assertEquals("0", executor.expandVariables("$((!5))", env));
    }

    @Test
    void arithmeticUnaryBitNot() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("-1", executor.expandVariables("$((~0))", env));
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
    void arithmeticTernaryNested() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("30", executor.expandVariables("$((1 ? 2 ? 30 : 40 : 50))", env));
        Assertions.assertEquals("40", executor.expandVariables("$((1 ? 0 ? 30 : 40 : 50))", env));
        Assertions.assertEquals("50", executor.expandVariables("$((0 ? 30 : 40 ? 50 : 60))", env));
    }

    @Test
    void arithmeticTernaryWithComparison() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "5");
        Assertions.assertEquals("1", executor.expandVariables("$((x > 3 ? 1 : 0))", env));
        Assertions.assertEquals("0", executor.expandVariables("$((x > 10 ? 1 : 0))", env));
    }

    @Test
    void arithmeticTernaryArithmeticBranches() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("7", executor.expandVariables("$((1 ? 3 + 4 : 99))", env));
        Assertions.assertEquals("99", executor.expandVariables("$((0 ? 3 + 4 : 99))", env));
    }

    @Test
    void arithmeticAssign() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("5", new Executor().expandVariables("$((x = 5))", env));
        Assertions.assertEquals("5", env.get("x"));
    }

    @Test
    void arithmeticPlusAssign() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("13", new Executor().expandVariables("$((x += 3))", env));
        Assertions.assertEquals("13", env.get("x"));
    }

    @Test
    void arithmeticMinusAssign() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("7", new Executor().expandVariables("$((x -= 3))", env));
        Assertions.assertEquals("7", env.get("x"));
    }

    @Test
    void arithmeticMulDivModAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("30", executor.expandVariables("$((x *= 3))", env));
        env.put("y", "20");
        Assertions.assertEquals("4", executor.expandVariables("$((y /= 5))", env));
        env.put("z", "23");
        Assertions.assertEquals("3", executor.expandVariables("$((z %= 5))", env));
    }

    @Test
    void arithmeticShiftAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "4");
        Assertions.assertEquals("32", executor.expandVariables("$((x <<= 3))", env));
        env.put("y", "32");
        Assertions.assertEquals("4", executor.expandVariables("$((y >>= 3))", env));
    }

    @Test
    void arithmeticBitAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "12");
        Assertions.assertEquals("4", executor.expandVariables("$((x &= 6))", env));
        env.put("y", "12");
        Assertions.assertEquals("10", executor.expandVariables("$((y ^= 6))", env));
        env.put("z", "12");
        Assertions.assertEquals("14", executor.expandVariables("$((z |= 6))", env));
    }

    @Test
    void arithmeticAssignRightAssoc() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("7", new Executor().expandVariables("$((a = b = 7))", env));
        Assertions.assertEquals("7", env.get("a"));
        Assertions.assertEquals("7", env.get("b"));
    }

    @Test
    void arithmeticShortCircuitAndSuppressesAssignment() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((0 && (x = 99)))", env);
        Assertions.assertEquals(null, env.get("x"));
    }

    @Test
    void arithmeticShortCircuitOrSuppressesAssignment() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 || (x = 99)))", env);
        Assertions.assertEquals(null, env.get("x"));
    }

    @Test
    void arithmeticTernaryShortCircuit() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        Assertions.assertEquals("1", env.get("a"));
        Assertions.assertEquals(null, env.get("b"));
        executor.expandVariables("$((0 ? (c = 3) : (d = 4)))", env);
        Assertions.assertEquals(null, env.get("c"));
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
        final java.io.File dir = java.nio.file.Files.createTempDirectory(
            "sh4j_custompath_"
        ).toFile();
        dir.deleteOnExit();
        final java.io.File bin = new java.io.File(dir, "myhello");
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(bin.toPath(), StandardCharsets.UTF_8)
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
        final java.io.File file = java.io.File.createTempFile("sh4j_rw_0_", ".dat");
        file.deleteOnExit();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                file.toPath(),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("seed%n"));
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parser(String.format("cat <>%s", file.getAbsolutePath())).parse())
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
    void exportArgWithEqualsNonStandardName() throws Exception {
        final Executor executor = new Executor(new HashMap<>());
        Assertions.assertEquals(0, executor.execute(this.parse("export 1foo=bar")));
        Assertions.assertEquals("bar", executor.getVariable("1foo"));
    }

    @Test
    void execNonExecutableFileTriggersIoException() throws Exception {
        final java.io.File nonexec = java.io.File.createTempFile("sh4j_nonexec_", ".dat");
        nonexec.deleteOnExit();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                nonexec.toPath(),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("not a script");
        }
        nonexec.setExecutable(false);
        final Executor executor = new Executor();
        final int code = executor.execute(
            this.parser(String.format("exec %s", nonexec.getAbsolutePath())).parse()
        );
        Assertions.assertTrue(code != 0, String.format("non-executable should fail; got %s", code));
    }

    @Test
    void execInterruptedException() {
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        final int code = executor.execute(this.parse("exec sleep 1"));
        Thread.interrupted();
        Assertions.assertEquals(1, code);
    }

    @Test
    void externalNonExecutableFileTriggersIoException() throws Exception {
        final java.io.File nonexec = java.io.File.createTempFile("sh4j_ext_nonexec_", ".dat");
        nonexec.deleteOnExit();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                nonexec.toPath(),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("nope");
        }
        nonexec.setExecutable(false);
        final Executor executor = new Executor();
        final int code = executor.execute(this.parse(nonexec.getAbsolutePath()));
        Assertions.assertTrue(
            code != 0,
            String.format("non-executable external should fail; got %s", code)
        );
    }

    @Test
    void arithmeticAssignDivByZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("0", new Executor().expandVariables("$((x /= 0))", env));
        Assertions.assertEquals("0", env.get("x"));
    }

    @Test
    void arithmeticAssignModByZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("0", new Executor().expandVariables("$((x %= 0))", env));
    }

    @Test
    void arithmeticAssignNonNumericEnvTreatedAsZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        Assertions.assertEquals("5", new Executor().expandVariables("$((x += 5))", env));
        Assertions.assertEquals("5", env.get("x"));
    }

    @Test
    void arithmeticAssignWithUnderscoreName() {
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("42", new Executor().expandVariables("$((_foo = 42))", env));
        Assertions.assertEquals("42", env.get("_foo"));
    }

    @Test
    void externalReadWriteRedirectFdTwo() throws Exception {
        final String path = String.format("/tmp/sh4j_rw_fd2_%s", System.nanoTime());
        new java.io.File(path).delete();
        Assertions.assertNotEquals(
            0,
            new Executor().execute(this.parse(String.format("cat /nonexistent_xyz 2<>%s", path)))
        );
        new java.io.File(path).delete();
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
            0,
            new Executor(
                new HashMap<>(),
                java.util.List.of(ExecutorTest.SCRIPT_KEY, "a", "b")
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
                java.util.List.of(ExecutorTest.SCRIPT_KEY, "a", "b")
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
    void typeForExternalNonBuiltinCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("type ls")));
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
                java.util.List.of(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
            ).expandVariables("${10}", new HashMap<>())
        );
    }

    @Test
    void expandBracedMultiDigitPositionalInRange() {
        Assertions.assertEquals(
            "b",
            new Executor(
                new HashMap<>(),
                java.util.List.of(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
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
    void specialParamAtStarWithPositionals() {
        final Executor executor = new Executor(
            new HashMap<>(),
            java.util.List.of(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
        );
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals(ExecutorTest.A_B_C, executor.expandVariables("$@", env));
        Assertions.assertEquals(ExecutorTest.A_B_C, executor.expandVariables("$*", env));
    }

    @Test
    void specialParamAtStarEmpty() {
        Assertions.assertEquals(
            "",
            new Executor(
                new HashMap<>(),
                java.util.List.of(ExecutorTest.SCRIPT_KEY)
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
    void forLoopWithContinue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("for i in 1 2 3; do continue; echo $i; done"))
        );
        Assertions.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void typeForExternalCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("type true")));
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
    void exportArgWithEquals() {
        final Executor executor = new Executor(new HashMap<>());
        Assertions.assertEquals(0, executor.execute(this.parse("export FOO=hello")));
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
        Assertions.assertEquals("value@weird}", new Executor().expandVariables("${x@weird}", env));
    }

    @Test
    void tildeUserNameFallthrough() {
        Assertions.assertEquals(
            "~nonexistentuser123",
            new Executor().expandVariables("~nonexistentuser123", new HashMap<>())
        );
    }

    @Test
    void arithmeticPrecedenceMixed() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assertions.assertEquals("1", executor.expandVariables("$((1 + 2 == 3))", env));
        Assertions.assertEquals("1", executor.expandVariables("$((1 < 2 && 3 < 4))", env));
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
    void defaultsDoNotOverrideExistingEnv() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, "/custom/home");
        env.put(ExecutorTest.PATH_KEY, "/custom/path");
        final Executor executor = new Executor(env);
        Assertions.assertEquals("/custom/home", executor.getVariable(ExecutorTest.HOME_KEY));
        Assertions.assertEquals("/custom/path", executor.getVariable(ExecutorTest.PATH_KEY));
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
            new Executor(new HashMap<>()).execute(this.parse("/user/.nix-profile/bin/ls"))
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
        final Executor executor = new Executor(env);
        dir.delete();
        Assertions.assertEquals(42, executor.execute(this.parse("mycmd")));
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
        final Executor executor = new Executor(env);
        dir.delete();
        Assertions.assertEquals(7, executor.execute(this.parse("mycmd2")));
    }

    @Test
    void commandWithSlashNotFound() {
        Assertions.assertEquals(
            127,
            new Executor().execute(this.parse("/nonexistent_path_xyz_abc/cmd"))
        );
    }

    @Test
    void exportWithValue() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("export FOO=bar")));
        Assertions.assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void exportExistingVariable() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("export FOO")));
    }

    @Test
    void exportThenVariableExpands() {
        final Executor executor = new Executor();
        Assertions.assertEquals(0, executor.execute(this.parse("export MSG=hello; echo $MSG")));
        Assertions.assertEquals(ExecutorTest.HELLO, executor.getVariable("MSG"));
    }

    @Test
    void splitFieldsDefaultIfs() {
        Assertions.assertEquals(
            List.of("a", "b", "c"),
            WordExpander.splitFields(ExecutorTest.A_B_C, String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsMultipleWs() {
        Assertions.assertEquals(
            List.of("a", "b"),
            WordExpander.splitFields("a   b", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsLeadingWs() {
        Assertions.assertEquals(
            List.of("a", "b"),
            WordExpander.splitFields("  a b", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsTrailingWs() {
        Assertions.assertEquals(
            List.of("a", "b"),
            WordExpander.splitFields("a b  ", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsColon() {
        Assertions.assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a:b:c", ":"));
    }

    @Test
    void splitFieldsEmptyBetweenColons() {
        Assertions.assertEquals(List.of("a", "", "b"), WordExpander.splitFields("a::b", ":"));
    }

    @Test
    void splitFieldsOnlyColons() {
        Assertions.assertEquals(List.of("", ""), WordExpander.splitFields(":", ":"));
    }

    @Test
    void splitFieldsDoubleColon() {
        Assertions.assertEquals(List.of("", "", ""), WordExpander.splitFields("::", ":"));
    }

    @Test
    void splitFieldsNoDelim() {
        Assertions.assertEquals(
            List.of(ExecutorTest.HELLO),
            WordExpander.splitFields(ExecutorTest.HELLO, ":")
        );
    }

    @Test
    void splitFieldsEmptyIfs() {
        Assertions.assertEquals(
            List.of(ExecutorTest.HELLO_WORLD),
            WordExpander.splitFields(ExecutorTest.HELLO_WORLD, "")
        );
    }

    @Test
    void splitFieldsNullIfs() {
        Assertions.assertEquals(
            List.of(ExecutorTest.HELLO),
            WordExpander.splitFields(ExecutorTest.HELLO, null)
        );
    }

    @Test
    void splitFieldsEmptyValue() {
        Assertions.assertEquals(
            List.of(),
            WordExpander.splitFields("", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    void splitFieldsMixedWsNws() {
        Assertions.assertEquals(List.of("a", "", "b"), WordExpander.splitFields("a :b", " :"));
    }

    @Test
    void splitFieldsTrailingColon() {
        Assertions.assertEquals(List.of("a", ""), WordExpander.splitFields("a:", ":"));
    }

    @Test
    void splitFieldsCustomNws() {
        Assertions.assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a,b,c", ","));
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
    void removeQuotesEmptyQuotes() {
        Assertions.assertEquals("", WordExpander.removeQuotes("\"\""));
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
        Assertions.assertEquals(List.of("a", "b"), WordExpander.splitFields("a\tb", "\t"));
    }

    @Test
    void splitFieldsMixedIfs() {
        Assertions.assertEquals(List.of("a", "b", "c"), WordExpander.splitFields("a:b c", ": "));
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
            List.of(ExecutorTest.HELLO),
            WordExpander.pathnameExpand(List.of(ExecutorTest.HELLO))
        );
    }

    @Test
    void pathnameExpandGlobNoMatchKeepsPattern() {
        Assertions.assertEquals(
            List.of("__no_match__xyz__"),
            WordExpander.pathnameExpand(List.of("__no_match__xyz__"))
        );
    }

    @Test
    void pathnameExpandGlobWithMatch() {
        Assertions.assertEquals(
            List.of("pom.xml"),
            WordExpander.pathnameExpand(List.of("pom.xml"))
        );
    }

    @Test
    void pathnameExpandWildcardMatch() {
        Assertions.assertTrue(WordExpander.pathnameExpand(List.of("*.xml")).contains("pom.xml"));
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
        Assertions.assertEquals(
            "",
            new Executor(new HashMap<>()).expandVariables("${x:?my error}", new HashMap<>())
        );
    }

    @Test
    void expandBracedQuestionUnset() {
        Assertions.assertEquals(
            "",
            new Executor(new HashMap<>()).expandVariables("${x?my error}", new HashMap<>())
        );
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
        Assertions.assertEquals(
            "",
            new Executor(new HashMap<>()).expandVariables("${x:?}", new HashMap<>())
        );
    }

    @Test
    void expandBracedColonQuestionErrorStream() {
        final Executor executor = new Executor(new HashMap<>());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setErrorStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.expandVariables("${x:?my error}", new HashMap<>());
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).trim().contains("my error"),
            "error message should go to injected stream"
        );
    }

    @Test
    void cdToDirectory() throws Exception {
        final String saved = System.getProperty(ExecutorTest.USER_DIR_PROP);
        try {
            final Executor executor = new Executor();
            Assertions.assertEquals(0, executor.execute(this.parse("cd /tmp")));
            Assertions.assertEquals(
                ExecutorTest.TMP_DIR,
                executor.getVariable(ExecutorTest.PWD_KEY)
            );
        } finally {
            System.setProperty(ExecutorTest.USER_DIR_PROP, saved);
        }
    }

    @Test
    void cdToHome() throws Exception {
        final String saved = System.getProperty(ExecutorTest.USER_DIR_PROP);
        try {
            final Map<String, String> env = new HashMap<>();
            env.put(ExecutorTest.HOME_KEY, ExecutorTest.TMP_DIR);
            final Executor executor = new Executor(env);
            Assertions.assertEquals(0, executor.execute(this.parse("cd")));
            Assertions.assertEquals(
                ExecutorTest.TMP_DIR,
                executor.getVariable(ExecutorTest.PWD_KEY)
            );
        } finally {
            System.setProperty(ExecutorTest.USER_DIR_PROP, saved);
        }
    }

    @Test
    void cdToNonexistentReturnsOne() {
        Assertions.assertEquals(
            1,
            new Executor().execute(this.parse("cd /sh4j_nonexistent_dir_xyz"))
        );
    }

    @Test
    void cdMinus() throws Exception {
        final String saved = System.getProperty(ExecutorTest.USER_DIR_PROP);
        try {
            final Map<String, String> env = new HashMap<>();
            env.put(ExecutorTest.PWD_KEY, ExecutorTest.TMP_DIR);
            env.put("OLDPWD", "/");
            final Executor executor = new Executor(env);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            executor.setCaptureStream(bos);
            Assertions.assertEquals(0, executor.execute(this.parse("cd -")));
            Assertions.assertEquals("/", executor.getVariable(ExecutorTest.PWD_KEY));
            Assertions.assertEquals(ExecutorTest.TMP_DIR, executor.getVariable("OLDPWD"));
            Assertions.assertEquals("/", bos.toString(StandardCharsets.UTF_8).trim());
        } finally {
            System.setProperty(ExecutorTest.USER_DIR_PROP, saved);
        }
    }

    @Test
    void cdWithExpandedVar() throws Exception {
        final String saved = System.getProperty(ExecutorTest.USER_DIR_PROP);
        try {
            final Map<String, String> env = new HashMap<>();
            env.put("MYDIR", ExecutorTest.TMP_DIR);
            final Executor executor = new Executor(env);
            Assertions.assertEquals(0, executor.execute(this.parse("cd $MYDIR")));
            Assertions.assertEquals(
                ExecutorTest.TMP_DIR,
                executor.getVariable(ExecutorTest.PWD_KEY)
            );
        } finally {
            System.setProperty(ExecutorTest.USER_DIR_PROP, saved);
        }
    }

    @Test
    void typeBuiltin() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("type echo cd exit export type true false shift unset"))
        );
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(out.contains("echo is a shell built-in"));
        Assertions.assertTrue(out.contains("cd is a shell built-in"));
        Assertions.assertTrue(out.contains("exit is a shell built-in"));
        Assertions.assertTrue(out.contains("export is a shell built-in"));
        Assertions.assertTrue(out.contains("type is a shell built-in"));
        Assertions.assertTrue(out.contains("true is a shell built-in"));
        Assertions.assertTrue(out.contains("false is a shell built-in"));
    }

    @Test
    void typeExternal() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("type true")));
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
    void unsetRemovesVariable() {
        final Map<String, String> env = new HashMap<>();
        env.put("MYVAR", ExecutorTest.VALUE);
        final Executor executor = new Executor(env);
        Assertions.assertEquals(0, executor.execute(this.parse("unset MYVAR")));
        Assertions.assertNull(executor.getVariable("MYVAR"));
    }

    @Test
    void shiftRemovesPositionalParam() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(List.of("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("shift; echo $#")));
        Assertions.assertEquals("2", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void shiftWithCount() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(List.of("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("shift 2; echo $#")));
        Assertions.assertEquals("1", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void breakStopsWhileLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("while exit $?; do echo ran; break; exit 1; done"))
        );
        Assertions.assertEquals("ran", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void breakStopsForLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("for i in a b c; do echo $i; break; done"))
        );
        Assertions.assertEquals("a", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void continueInForLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("for i in a b; do echo $i; continue; echo after; done"))
        );
        Assertions.assertEquals(String.format("a%nb"), bos.toString(StandardCharsets.UTF_8).trim());
        Assertions.assertFalse(bos.toString(StandardCharsets.UTF_8).contains("after"));
    }

    @Test
    void colonBuiltinReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse(":")));
    }

    @Test
    void evalBuiltinRunsCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("eval echo hello world")));
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
    void externalOutputRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new java.io.File(outpath).delete();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Path.of(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO_WORLD);
        }
        Assertions.assertEquals(0, new Executor().execute(this.parse(String.format(ExecutorTest.CAT_INPUT_FMT, inpath) + String.format(" > %s", outpath))));
        Assertions.assertTrue(new java.io.File(outpath).exists());
        Assertions.assertEquals(
            ExecutorTest.HELLO_WORLD,
            java.nio.file.Files.readString(java.nio.file.Path.of(outpath)).trim()
        );
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    void externalAppendRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_append_in.txt";
        final String outpath = "/tmp/sh4j_ext_append_out.txt";
        new java.io.File(outpath).delete();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Path.of(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("second");
        }
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Path.of(outpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("first%n"));
        }
        Assertions.assertEquals(0, new Executor().execute(this.parse(String.format(ExecutorTest.CAT_INPUT_FMT, inpath) + String.format(" >> %s", outpath))));
        final String content = java.nio.file.Files.readString(
            java.nio.file.Path.of(outpath)
        ).trim();
        Assertions.assertTrue(content.startsWith("first"));
        Assertions.assertTrue(content.contains("second"));
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    void externalClobberRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outpath = "/tmp/sh4j_ext_clobber_out.txt";
        new java.io.File(outpath).delete();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Path.of(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello clobber");
        }
        Assertions.assertEquals(0, new Executor().execute(this.parse(String.format(ExecutorTest.CAT_INPUT_FMT, inpath) + String.format(" >| %s", outpath))));
        Assertions.assertTrue(new java.io.File(outpath).exists());
        Assertions.assertEquals(
            "hello clobber",
            java.nio.file.Files.readString(java.nio.file.Path.of(outpath)).trim()
        );
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    void externalRedirectStdoutToStderr() throws Exception {
        final String inpath = "/tmp/sh4j_ext_12_in.txt";
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Path.of(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO);
        }
        Assertions.assertEquals(0, new Executor().execute(this.parse(String.format(ExecutorTest.CAT_INPUT_FMT, inpath) + " 1>&2")));
        new java.io.File(inpath).delete();
    }

    @Test
    void externalRedirectStderrToStdout() throws Exception {
        final String inpath = "/tmp/sh4j_ext_21_in.txt";
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Path.of(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO);
        }
        Assertions.assertEquals(0, new Executor().execute(this.parse(String.format(ExecutorTest.CAT_INPUT_FMT, inpath) + " 2>&1")));
        new java.io.File(inpath).delete();
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
        new java.io.File(path).delete();
        final Executor executor = new Executor();
        executor.execute(this.parse(String.format("cat /nonexistent_path_xyz_123 2> %s", path)));
        Assertions.assertTrue(new java.io.File(path).exists());
        new java.io.File(path).delete();
    }

    @Test
    void externalStderrAppendRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_append.txt";
        new java.io.File(path).delete();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Path.of(path),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("initial%n"));
        }
        final Executor executor = new Executor();
        executor.execute(this.parse(String.format("cat /nonexistent_path_xyz_456 2>> %s", path)));
        Assertions.assertTrue(
            java.nio.file.Files.readString(java.nio.file.Path.of(path)).contains("initial")
        );
        new java.io.File(path).delete();
    }

    @Test
    void externalReadWriteRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_rw.txt";
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Path.of(path),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO);
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <> %s", path)))
        );
        new java.io.File(path).delete();
    }

    @Test
    void heredocStripTabsBasic() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\thello%nEOF%n")))
        );
        Assertions.assertEquals(ExecutorTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMultiple() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\t\thello%nEOF%n")))
        );
        Assertions.assertEquals(ExecutorTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsDelimiterWithTabs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\thello%n\tEOF%n")))
        );
        Assertions.assertEquals(ExecutorTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void heredocStripTabsMixed() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(
                this.parse(String.format("cat <<- MARKER%n\tfirst%nsecond%n\tthird%nMARKER%n"))
            )
        );
        Assertions.assertEquals(
            String.format("first%nsecond%nthird"),
            bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    void typeFunction() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("myfunc() { true; }; type myfunc")));
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

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
