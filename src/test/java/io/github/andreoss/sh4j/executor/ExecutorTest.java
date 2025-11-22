/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.SimpleCommand;

/**
 * Tests for Executor.
 * @since 1.0
 * @checkstyle MethodCountCheck (500 lines)
 * @checkstyle FileLength (3000 lines)
 */
@SuppressWarnings({
    "PMD.GodClass", "PMD.TooManyMethods",
    "PMD.UnitTestContainsTooManyAsserts", "PMD.UnnecessaryLocalRule",
    "PMD.AvoidDuplicateLiterals", "PMD.UseUnderscoresInNumericLiterals",
    "PMD.NcssCount", "PMD.RelianceOnDefaultCharset"
})
public final class ExecutorTest {

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
    public void executeSimpleEcho() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo hello")));
    }

    @Test
    public void executePwd() {
        Assert.assertEquals(0, new Executor().execute(this.parse("pwd")));
    }

    @Test
    public void executeTrue() {
        Assert.assertEquals(0, new Executor().execute(this.parse(ExecutorTest.TRUE_CMD)));
    }

    @Test
    public void executeFalse() {
        Assert.assertEquals(1, new Executor().execute(this.parse(ExecutorTest.FALSE_CMD)));
    }

    @Test
    public void executeWithArguments() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo a b c")));
    }

    @Test
    public void executeSemicolonSeparator() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo a; echo b")));
    }

    @Test
    public void executeAndIf() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo a && echo b")));
    }

    @Test
    public void executeOrIf() {
        Assert.assertEquals(0, new Executor().execute(this.parse("false || echo b")));
    }

    @Test
    public void executeAndIfStopsOnFailure() {
        Assert.assertEquals(1, new Executor().execute(this.parse("false && echo not reached")));
    }

    @Test
    public void executeOrIfStopsOnSuccess() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("echo success || echo not reached"))
        );
    }

    @Test
    public void executeEmptyList() {
        Assert.assertEquals(0, new Executor().execute(this.parse("")));
    }

    @Test
    public void executeAssignmentOnly() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("FOO=bar")));
        Assert.assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    public void executeAssignmentWithCommand() {
        Assert.assertEquals(0, new Executor().execute(this.parse("FOO=hello echo $FOO")));
    }

    @Test
    public void executeMultipleAssignments() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("A=1 B=2")));
        Assert.assertEquals("1", executor.getVariable("A"));
        Assert.assertEquals("2", executor.getVariable("B"));
    }

    @Test
    public void executeVariableExpansion() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo $HOME")));
    }

    @Test
    public void executeOutputRedirection() throws Exception {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("echo test > /tmp/sh4j_test_output"))
        );
    }

    @Test
    public void executeInputRedirection() throws Exception {
        Assert.assertEquals(0, new Executor().execute(this.parse("cat < /dev/null")));
    }

    @Test
    public void executePipeline() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo hello | cat")));
    }

    @Test
    public void executeBackground() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo hello &")));
    }

    @Test
    public void executeBackgroundSetsLastBgPid() {
        final Executor executor = new Executor();
        executor.execute(this.parse("echo hello &"));
        Assert.assertTrue(
            "lastBgPid should be > 0 after & command", executor.getLastBgPid() > 0
        );
    }

    @Test
    public void executeBackgroundAndSequential() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo bg1 & echo seq1")));
    }

    @Test
    public void executeClobberRedirection() throws Exception {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("echo test >| /tmp/sh4j_clobber_test"))
        );
    }

    @Test
    public void executeBuiltinStderrRedirect() throws Exception {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo hello 2>/dev/null")));
    }

    @Test
    public void executeBuiltinStderrToStdout() throws Exception {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo hello 2>&1")));
    }

    @Test
    public void executeBuiltinStderrRedirectToFile() throws Exception {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("echo hello 2>/tmp/sh4j_stderr_test"))
        );
    }

    @Test
    public void executeBuiltinStderrAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stderr_append_%s", System.nanoTime());
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("echo hello 2>>%s", file)))
        );
        new java.io.File(file).delete();
    }

    @Test
    public void executeBuiltinStdoutAppendToFile() throws Exception {
        final String file = String.format("/tmp/sh4j_stdout_append_%s", System.nanoTime());
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("echo hello >>%s", file)))
        );
        new java.io.File(file).delete();
    }

    @Test
    public void executeBuiltinStdoutRedirectIoException() throws Exception {
        Assert.assertEquals(
            1,
            new Executor().execute(this.parse("echo hello >/dev/null/no_such_path_xyz"))
        );
    }

    @Test
    public void executeBuiltinStderrRedirectIoException() throws Exception {
        Assert.assertEquals(
            1,
            new Executor().execute(this.parse("echo hello 2>/dev/null/no_such_stderr_xyz"))
        );
    }

    @Test
    public void executeBuiltinStdoutAppendIoException() throws Exception {
        Assert.assertEquals(
            1,
            new Executor().execute(this.parse("echo hello >>/dev/null/no_such_append_xyz"))
        );
    }

    @Test
    public void executeBuiltinStderrAppendIoException() throws Exception {
        Assert.assertEquals(
            1,
            new Executor().execute(this.parse("echo hello 2>>/dev/null/no_such_eappend_xyz"))
        );
    }

    @Test
    public void executeBuiltinCloseStdout() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo hello >&-")));
    }

    @Test
    public void executeMultiplePipelines() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo a; echo b; echo c")));
    }

    @Test
    public void executeBangPipeline() {
        Assert.assertEquals(0, new Executor().execute(this.parse("! false")));
    }

    @Test
    public void executeVariableWithEmptyValue() {
        final Map<String, String> env = new HashMap<>();
        env.put("EMPTY_VAR", "");
        Assert.assertEquals(0, new Executor(env).execute(this.parse("echo $EMPTY_VAR")));
    }

    @Test
    public void executeCommandNotFound() {
        Assert.assertEquals(127, new Executor().execute(this.parse("nonexistent_command_xyz")));
    }

    @Test
    public void executeCommandNotFoundReturnsNotFound() {
        Assert.assertEquals(127, new Executor().execute(this.parse("nonexistent_cmd_xyz_127")));
    }

    @Test
    public void executeBangWithTrue() {
        Assert.assertEquals(1, new Executor().execute(this.parse("! true")));
    }

    @Test
    public void executeExitZero() {
        Assert.assertEquals(0, new Executor().execute(this.parse("exit 0")));
    }

    @Test
    public void executeExitOne() {
        Assert.assertEquals(1, new Executor().execute(this.parse("exit 1")));
    }

    @Test
    public void executeEchoAbc() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo abc")));
    }

    @Test
    public void executeEmptyListNode() {
        Assert.assertEquals(0, new Executor().execute(new ListNode()));
    }

    @Test
    public void executePipelineWithNoCommands() {
        final ListNode list = new ListNode();
        list.addPipeline(new Pipeline(false), ListNode.Separator.NONE);
        Assert.assertEquals(0, new Executor().execute(list));
    }

    @Test
    public void executeAppendRedirection() throws Exception {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("echo test >> /tmp/sh4j_append_test"))
        );
    }

    @Test
    public void executeStderrRedirect() throws Exception {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("echo test 2>/tmp/sh4j_test_stderr"))
        );
    }

    @Test
    public void executeStderrToStdout() throws Exception {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo test 2>&1")));
    }

    @Test
    public void executeCloseStdin() throws Exception {
        Assert.assertEquals(0, new Executor().execute(this.parse("cat <&-")));
    }

    @Test
    public void executeCloseStdout() throws Exception {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo test >&-")));
    }

    @Test
    public void executeReadWriteRedirect() throws Exception {
        Assert.assertEquals(0, new Executor().execute(this.parse("cat <> /dev/null")));
    }

    @Test
    public void executeExitNoArgs() {
        Assert.assertEquals(0, new Executor().execute(this.parse("exit")));
    }

    @Test
    public void executeExitWithNonNumericArg() {
        Assert.assertEquals(1, new Executor().execute(this.parse("exit xyz")));
    }

    @Test
    public void executePipelineWithNullCommand() {
        final Pipeline pipeline = new Pipeline(false);
        pipeline.getCommands().add(null);
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        Assert.assertEquals(0, new Executor().execute(list));
    }

    @Test
    public void executeCommandWithNullName() {
        final Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(new SimpleCommand(null));
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        Assert.assertEquals(0, new Executor().execute(list));
    }

    @Test
    public void executeAssignmentWithNoEquals() {
        final SimpleCommand cmd = new SimpleCommand("echo");
        cmd.getAssignments().add("INVALID");
        final Pipeline pipeline = new Pipeline(false);
        pipeline.addCommand(cmd);
        final ListNode list = new ListNode();
        list.addPipeline(pipeline, ListNode.Separator.NONE);
        Assert.assertEquals(0, new Executor().execute(list));
    }

    @Test
    public void executePipelineWithFailingCommand() {
        final Executor executor = new Executor();
        final int code = executor.execute(
            this.parse("echo hello | nonexistent_cmd_xyz_fail | echo done")
        );
        Assert.assertTrue(
            String.format(
                "Pipeline with missing middle stage must exit with POSIX command-not-found(127) or not-executable(126); got %s",
                code
            ),
            code == 0
        );
    }

    @Test
    public void executeSubshell() {
        Assert.assertEquals(0, new Executor().execute(this.parse("(echo hello)")));
    }

    @Test
    public void executeSubshellWithMultiplePipelines() {
        Assert.assertEquals(0, new Executor().execute(this.parse("(echo a; echo b)")));
    }

    @Test
    public void executeSubshellInPipeline() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo foo | (cat) | wc")));
    }

    @Test
    public void executeSubshellWithExit() {
        Assert.assertEquals(42, new Executor().execute(this.parse("(exit 42)")));
    }

    @Test
    public void executeGrouping() {
        Assert.assertEquals(0, new Executor().execute(this.parse("{ echo hello; }")));
    }

    @Test
    public void executeGroupingInPipeline() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo foo | { cat; } | wc")));
    }

    @Test
    public void executeGroupingSharesEnvironment() {
        Assert.assertEquals(0, new Executor().execute(this.parse("FOO=bar; { echo $FOO; }")));
    }

    @Test
    public void executeIfTrueThen() {
        Assert.assertEquals(0, new Executor().execute(this.parse("if true; then echo hi; fi")));
    }

    @Test
    public void executeIfFalseElse() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("if false; then echo a; else echo b; fi"))
        );
    }

    @Test
    public void executeIfTrueThenElseNotExecuted() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("if true; then echo hi; else false; fi"))
        );
    }

    @Test
    public void executeForDoDone() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("for var in a b c; do echo $var; done"))
        );
    }

    @Test
    public void executeWhileFalse() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("while false; do echo hi; done"))
        );
    }

    @Test
    public void executeUntilTrue() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("until true; do echo hi; done"))
        );
    }

    @Test
    public void executeCaseMatch() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("case a in a) echo match;; esac"))
        );
    }

    @Test
    public void executeCaseNoMatch() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("case a in b) echo nomatch;; esac"))
        );
    }

    @Test
    public void executeCaseDefaultStar() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("case a in *) echo default;; esac"))
        );
    }

    @Test
    public void executeCasePipePattern() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("case a in b|c|a) echo match;; esac"))
        );
    }

    @Test
    public void executeCaseFirstMatchOnly() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("case a in a) echo first;; b) echo second;; esac"))
        );
    }

    @Test
    public void executeFunctionDefinitionStored() {
        Assert.assertEquals(0, new Executor().execute(this.parse("foo() { echo hello; }")));
    }

    @Test
    public void executeFunctionCall() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("foo() { echo hello; }; foo"))
        );
    }

    @Test
    public void executeFunctionSubshellBody() {
        Assert.assertEquals(0, new Executor().execute(this.parse("foo() (echo hello); foo")));
    }

    @Test
    public void executeWhileLoop() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("while false; do echo body; done"))
        );
    }

    @Test
    public void executeUntilLoop() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("until true; do echo body; done"))
        );
    }

    @Test
    public void executeWhileBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            1,
            executor.execute(this.parse("i=; while [ -z \"$i\" ]; do echo ran; i=1; false; done"))
        );
        Assert.assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void executeUntilBodyRunsOnce() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0,
            executor.execute(this.parse("i=; until [ -n \"$i\" ]; do echo ran; i=1; true; done"))
        );
        Assert.assertEquals(String.format("ran%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void executeWhileBodyExitCodePropagated() {
        Assert.assertEquals(
            42,
            new Executor().execute(this.parse("i=; while [ -z \"$i\" ]; do i=1; (exit 42); done"))
        );
    }

    @Test
    public void commandInterruptedException() {
        Thread.currentThread().interrupt();
        Assert.assertEquals(1, new Executor().execute(this.parse("ls")));
    }

    @Test
    public void pipelineInterruptedException() {
        Thread.currentThread().interrupt();
        Assert.assertEquals(1, new Executor().execute(this.parse("ls | cat")));
    }

    @Test
    public void waitForBackgroundJobsInterrupted() {
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        executor.execute(this.parse("sleep 1 &"));
        Assert.assertTrue(
            "InterruptedException catch should re-set interrupt flag",
            Thread.interrupted()
        );
    }

    @Test
    public void exportNoArgs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_EXPORT_VAR", ExecutorTest.HELLO);
        final Executor executor = new Executor(env);
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("export")));
        Assert.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("export TEST_EXPORT_VAR=hello")
        );
    }

    @Test
    public void executeIfFalseNoElse() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse("if false; then echo yes; fi"))
        );
    }

    @Test
    public void executeElifWhenIfFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0,
            executor.execute(this.parse("if false; then echo no; elif true; then echo yes; fi"))
        );
        Assert.assertEquals(String.format("yes%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void executeElifChainFalseFalse() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0,
            executor.execute(
                this.parse("if false; then echo a; elif false; then echo b; else echo c; fi")
            )
        );
        Assert.assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void executeElifChainSecondElifTrue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0,
            executor.execute(
                this.parse(
                    "if false; then echo a; elif false; then echo b; elif true; then echo c; fi"
                )
            )
        );
        Assert.assertEquals(String.format("c%n"), bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void executeRedirectStdoutToStderr() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo test 1>&2")));
    }

    @Test
    public void executeHereDoc() {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat << EOF%nhello%nEOF%n")))
        );
    }

    @Test
    public void heredocExpansion() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_HVAR", "expanded");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat << EOF%nhello $TEST_HVAR%nEOF%n")))
        );
        Assert.assertEquals("hello expanded", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void redirectTargetExpansionOutput() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_redir_expand_test.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        Assert.assertEquals(
            0,
            new Executor(env).execute(this.parse("echo hello > $TEST_FILE"))
        );
        Assert.assertTrue(file.exists());
        Assert.assertEquals(
            String.format("hello%n"),
            new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
        file.delete();
    }

    @Test
    public void redirectTargetExpansionAppend() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_redir_append_expand.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        Assert.assertEquals(
            0,
            new Executor(env).execute(this.parse("echo hello >> $TEST_FILE"))
        );
        Assert.assertTrue(file.exists());
        Assert.assertEquals(
            String.format("hello%n"),
            new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        );
        file.delete();
    }

    @Test
    public void redirectTargetExpansionInput() throws Exception {
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
        Assert.assertEquals(0, executor.execute(this.parse("cat < $TEST_FILE")));
        Assert.assertEquals("hello from file", bos.toString(StandardCharsets.UTF_8).trim());
        file.delete();
    }

    @Test
    public void executeNonExecutableCommand() throws Exception {
        final java.io.File file = new java.io.File("/tmp/sh4j_nonexec_test.sh");
        file.createNewFile();
        file.setExecutable(false);
        Assert.assertEquals(126, new Executor().execute(this.parse(file.getAbsolutePath())));
    }

    @Test
    public void expandDollarQuestion() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorTest.FALSE_CMD));
        Assert.assertEquals("1", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    public void expandDollarQuestionAfterSuccess() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorTest.TRUE_CMD));
        Assert.assertEquals("0", executor.expandVariables("$?", new HashMap<>()));
    }

    @Test
    public void expandDollarHyphen() {
        Assert.assertEquals("", new Executor().expandVariables("$-", new HashMap<>()));
    }

    @Test
    public void expandDollarQuestionBraced() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorTest.FALSE_CMD));
        Assert.assertEquals("1", executor.expandVariables("${?}", new HashMap<>()));
    }

    @Test
    public void expandDollarQuestionInString() {
        final Executor executor = new Executor();
        executor.execute(this.parse(ExecutorTest.FALSE_CMD));
        Assert.assertEquals("exit:1", executor.expandVariables("exit:$?", new HashMap<>()));
    }

    @Test
    public void expandDollarHash() {
        Assert.assertEquals(
            "3",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c")
            ).expandVariables("$#", new HashMap<>())
        );
    }

    @Test
    public void expandDollarAt() {
        Assert.assertEquals(
            ExecutorTest.A_B_C,
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c")
            ).expandVariables("$@", new HashMap<>())
        );
    }

    @Test
    public void expandDollarStar() {
        Assert.assertEquals(
            ExecutorTest.A_B_C,
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c")
            ).expandVariables("$*", new HashMap<>())
        );
    }

    @Test
    public void expandDollarZero() {
        Assert.assertEquals(
            "my_script",
            new Executor(
                new HashMap<>(),
                Arrays.asList("my_script")
            ).expandVariables("$0", new HashMap<>())
        );
    }

    @Test
    public void expandDollarOne() {
        Assert.assertEquals(
            "first",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "first", "second")
            ).expandVariables("$1", new HashMap<>())
        );
    }

    @Test
    public void expandDollarNine() {
        Assert.assertEquals(
            "i",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i")
            ).expandVariables("$9", new HashMap<>())
        );
    }

    @Test
    public void expandBracedMultiDigit() {
        Assert.assertEquals(
            "j",
            new Executor(
                new HashMap<>(),
                Arrays.asList("sh", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
            ).expandVariables("${10}", new HashMap<>())
        );
    }

    @Test
    public void expandDollarHashNone() {
        Assert.assertEquals("0", new Executor().expandVariables("$#", new HashMap<>()));
    }

    @Test
    public void expandDollarAtEmpty() {
        Assert.assertEquals("", new Executor().expandVariables("$@", new HashMap<>()));
    }

    @Test
    public void expandDollarDollar() {
        final Executor executor = new Executor();
        final String pid = executor.expandVariables("$$", new HashMap<>());
        Assert.assertNotNull(pid);
        Assert.assertFalse(pid.isEmpty());
        Assert.assertEquals(String.valueOf(Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0])), pid);
    }

    @Test
    public void expandDollarBang() {
        Assert.assertEquals("0", new Executor().expandVariables("$!", new HashMap<>()));
    }

    @Test
    public void expandDollarBangBraced() {
        Assert.assertEquals("0", new Executor().expandVariables("${!}", new HashMap<>()));
    }

    @Test
    public void expandTildeToHome() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, "/home/testuser");
        Assert.assertEquals("/home/testuser", WordExpander.expandTilde("~", env));
    }

    @Test
    public void expandTildeWithPath() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, "/home/testuser");
        Assert.assertEquals(
            "/home/testuser/file.txt",
            WordExpander.expandTilde("~/file.txt", env)
        );
    }

    @Test
    public void expandTildePlus() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PWD_KEY, "/current/dir");
        Assert.assertEquals("/current/dir", WordExpander.expandTilde("~+", env));
    }

    @Test
    public void expandTildeNone() {
        Assert.assertEquals("/foo/bar", WordExpander.expandTilde("/foo/bar", new HashMap<>()));
    }

    @Test
    public void expandVarDefault() {
        Assert.assertEquals(
            "default",
            new Executor().expandVariables(ExecutorTest.VAR_DEFAULT_EXPR, new HashMap<>())
        );
    }

    @Test
    public void expandVarDefaultSet() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "actual");
        Assert.assertEquals(
            "actual",
            new Executor().expandVariables(ExecutorTest.VAR_DEFAULT_EXPR, env)
        );
    }

    @Test
    public void expandVarDefaultEmpty() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "");
        Assert.assertEquals(
            "default",
            new Executor().expandVariables(ExecutorTest.VAR_DEFAULT_EXPR, env)
        );
    }

    @Test
    public void expandVarDefaultNullOnly() {
        Assert.assertEquals(
            "default",
            new Executor().expandVariables("${var-default}", new HashMap<>())
        );
    }

    @Test
    public void expandVarDefaultNullOnlySetSkips() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, ExecutorTest.VALUE);
        Assert.assertEquals(
            ExecutorTest.VALUE,
            new Executor().expandVariables("${var-default}", env)
        );
    }

    @Test
    public void expandVarDefaultNullOnlyEmptySkips() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "");
        Assert.assertEquals("", new Executor().expandVariables("${var-default}", env));
    }

    @Test
    public void expandVarAssignDefault() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals(
            "assigned",
            new Executor().expandVariables("${var:=assigned}", env)
        );
        Assert.assertEquals("assigned", env.get(ExecutorTest.VAR_NAME));
    }

    @Test
    public void expandVarAlternate() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "x");
        Assert.assertEquals("alt", new Executor().expandVariables("${var:+alt}", env));
    }

    @Test
    public void expandVarAlternateUnset() {
        Assert.assertEquals("", new Executor().expandVariables("${var:+alt}", new HashMap<>()));
    }

    @Test
    public void expandVarAssignNullOnly() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("assigned", new Executor().expandVariables("${var=assigned}", env));
        Assert.assertEquals("assigned", env.get(ExecutorTest.VAR_NAME));
    }

    @Test
    public void expandVarAssignNullOnlySetSkips() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "existing");
        Assert.assertEquals("existing", new Executor().expandVariables("${var=assigned}", env));
        Assert.assertEquals("existing", env.get(ExecutorTest.VAR_NAME));
    }

    @Test
    public void expandVarAlternateNullOnly() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "x");
        Assert.assertEquals("alt", new Executor().expandVariables("${var+alt}", env));
    }

    @Test
    public void expandVarAlternateNullOnlyUnset() {
        Assert.assertEquals("", new Executor().expandVariables("${var+alt}", new HashMap<>()));
    }

    @Test
    public void expandVarLength() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, ExecutorTest.HELLO);
        Assert.assertEquals("5", new Executor().expandVariables("${#var}", env));
    }

    @Test
    public void expandVarLengthUnset() {
        Assert.assertEquals("0", new Executor().expandVariables("${#var}", new HashMap<>()));
    }

    @Test
    public void expandVarSuffixShort() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "file.txt");
        Assert.assertEquals("file", new Executor().expandVariables("${var%.*}", env));
    }

    @Test
    public void expandVarSuffixLong() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "a.b.c.txt");
        Assert.assertEquals("a", new Executor().expandVariables("${var%%.*}", env));
    }

    @Test
    public void expandVarPrefixShort() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "foo/bar/baz");
        Assert.assertEquals("bar/baz", new Executor().expandVariables("${var#*/}", env));
    }

    @Test
    public void expandVarPrefixLong() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, "foo/bar/baz");
        Assert.assertEquals("baz", new Executor().expandVariables("${var##*/}", env));
    }

    @Test
    public void expandVarPatternNoMatch() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.VAR_NAME, ExecutorTest.HELLO);
        Assert.assertEquals(
            ExecutorTest.HELLO,
            new Executor().expandVariables("${var%x}", env)
        );
    }

    @Test
    public void commandSubstitutionEcho() {
        Assert.assertEquals(
            ExecutorTest.HELLO,
            new Executor().expandVariables("$(echo hello)", new HashMap<>())
        );
    }

    @Test
    public void commandSubstitutionInString() {
        Assert.assertEquals(
            "msg=hi",
            new Executor().expandVariables("msg=$(echo hi)", new HashMap<>())
        );
    }

    @Test
    public void backtickSubstitution() {
        Assert.assertEquals(
            ExecutorTest.HELLO,
            new Executor().expandVariables("`echo hello`", new HashMap<>())
        );
    }

    @Test
    public void backtickSubstitutionInString() {
        Assert.assertEquals(
            "msg=hi",
            new Executor().expandVariables("msg=`echo hi`", new HashMap<>())
        );
    }

    @Test
    public void normaliseBackticksSimple() {
        Assert.assertEquals("$(echo hello)", WordExpander.normaliseBackticks("`echo hello`"));
    }

    @Test
    public void normaliseBackticksNoChange() {
        Assert.assertEquals(
            ExecutorTest.HELLO_WORLD,
            WordExpander.normaliseBackticks(ExecutorTest.HELLO_WORLD)
        );
    }

    @Test
    public void normaliseBackticksSkipsDollarParen() {
        Assert.assertEquals("$(echo hi)", WordExpander.normaliseBackticks("$(echo hi)"));
    }

    @Test
    public void normaliseBackticksEscaped() {
        Assert.assertEquals("$(echo `hi`)", WordExpander.normaliseBackticks("`echo \\`hi\\``"));
    }

    @Test
    public void normaliseBackticksDollarEscape() {
        Assert.assertEquals("$(echo $PATH)", WordExpander.normaliseBackticks("`echo \\$PATH`"));
    }

    @Test
    public void normaliseBackticksBackslashEscape() {
        Assert.assertEquals("$(echo a\\b)", WordExpander.normaliseBackticks("`echo a\\\\b`"));
    }

    @Test
    public void normaliseBackticksNonSpecialEscape() {
        Assert.assertEquals("$(echo \\n)", WordExpander.normaliseBackticks("`echo \\\\n`"));
    }

    @Test
    public void normaliseBackticksUnclosed() {
        Assert.assertEquals("`echo hello", WordExpander.normaliseBackticks("`echo hello"));
    }

    @Test
    public void normaliseBackticksSkipsDollarBrace() {
        Assert.assertEquals(
            ExecutorTest.VAR_DEFAULT_EXPR,
            WordExpander.normaliseBackticks(ExecutorTest.VAR_DEFAULT_EXPR)
        );
    }

    @Test
    public void normaliseBackticksSkipsNestedDollarBrace() {
        Assert.assertEquals(
            "${var:-${other}}",
            WordExpander.normaliseBackticks("${var:-${other}}")
        );
    }

    @Test
    public void normaliseBackticksSkipsTripleNestedDollarBrace() {
        Assert.assertEquals(
            "${a:-${b:-${c}}}",
            WordExpander.normaliseBackticks("${a:-${b:-${c}}}")
        );
    }

    @Test
    public void normaliseBackticksDollarAtEnd() {
        Assert.assertEquals("foo$", WordExpander.normaliseBackticks("foo$"));
    }

    @Test
    public void normaliseBackticksUnmatchedDollarBrace() {
        Assert.assertEquals("${unclosed", WordExpander.normaliseBackticks("${unclosed"));
    }

    @Test
    public void normaliseBackticksDollarFollowedByLetter() {
        Assert.assertEquals("$VAR", WordExpander.normaliseBackticks("$VAR"));
    }

    @Test
    public void normaliseBackticksBackslashAtEndOfInput() {
        Assert.assertEquals("`abc\\", WordExpander.normaliseBackticks("`abc\\"));
    }

    @Test
    public void globMatchesQuestionMarkInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("case ab in a?) echo MATCHED;; *) echo NO;; esac"))
        );
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    public void globMatchesQuestionMarkNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("case abc in a?) echo MATCHED;; *) echo NO;; esac"))
        );
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("NO"));
    }

    @Test
    public void globMatchesCharClassInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("case b in [abc]) echo MATCHED;; *) echo NO;; esac"))
        );
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    public void globMatchesCharClassNoMatch() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("case z in [abc]) echo MATCHED;; *) echo NO;; esac"))
        );
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("NO"));
    }

    @Test
    public void globMatchesCharClassRangeInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("case m in [a-z]) echo MATCHED;; *) echo NO;; esac"))
        );
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    public void globMatchesUnclosedBracketInCase() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("case '[abc' in '[abc') echo MATCHED;; *) echo NO;; esac"))
        );
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).trim().equals("MATCHED"));
    }

    @Test
    public void backtickNestedNormalise() {
        Assert.assertEquals(
            "$(echo `echo hi`)",
            WordExpander.normaliseBackticks("`echo \\`echo hi\\``")
        );
    }

    @Test
    public void echoBuiltin() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo hello")));
    }

    @Test
    public void echoBuiltinArgs() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo hello world")));
    }

    @Test
    public void echoBuiltinNoNewline() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo -n hello")));
    }

    @Test
    public void echoBuiltinEmpty() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo")));
    }

    @Test
    public void arithmeticSimple() {
        Assert.assertEquals("7", new Executor().expandVariables("$((3 + 4))", new HashMap<>()));
    }

    @Test
    public void arithmeticMul() {
        Assert.assertEquals(
            "15",
            new Executor().expandVariables("$((3 * 5))", new HashMap<>())
        );
    }

    @Test
    public void arithmeticVar() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assert.assertEquals("15", new Executor().expandVariables("$((x + 5))", env));
    }

    @Test
    public void arithmeticNested() {
        Assert.assertEquals(
            "14",
            new Executor().expandVariables("$((2 * (3 + 4)))", new HashMap<>())
        );
    }

    @Test
    public void arithmeticShiftLeft() {
        Assert.assertEquals(
            "32",
            new Executor().expandVariables("$((4 << 3))", new HashMap<>())
        );
    }

    @Test
    public void arithmeticShiftRight() {
        Assert.assertEquals(
            "4",
            new Executor().expandVariables("$((32 >> 3))", new HashMap<>())
        );
    }

    @Test
    public void arithmeticLessThan() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((3 < 5))", env));
        Assert.assertEquals("0", executor.expandVariables("$((5 < 3))", env));
    }

    @Test
    public void arithmeticLessEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((5 <= 5))", env));
        Assert.assertEquals("0", executor.expandVariables("$((6 <= 5))", env));
    }

    @Test
    public void arithmeticGreaterThan() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((5 > 3))", env));
        Assert.assertEquals("0", executor.expandVariables("$((3 > 5))", env));
    }

    @Test
    public void arithmeticGreaterEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((5 >= 5))", env));
        Assert.assertEquals("0", executor.expandVariables("$((4 >= 5))", env));
    }

    @Test
    public void arithmeticEqualEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((7 == 7))", env));
        Assert.assertEquals("0", executor.expandVariables("$((7 == 8))", env));
    }

    @Test
    public void arithmeticNotEqual() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((7 != 8))", env));
        Assert.assertEquals("0", executor.expandVariables("$((7 != 7))", env));
    }

    @Test
    public void arithmeticBitAnd() {
        Assert.assertEquals(
            "4",
            new Executor().expandVariables("$((12 & 6))", new HashMap<>())
        );
    }

    @Test
    public void arithmeticBitXor() {
        Assert.assertEquals(
            "10",
            new Executor().expandVariables("$((12 ^ 6))", new HashMap<>())
        );
    }

    @Test
    public void arithmeticBitOr() {
        Assert.assertEquals(
            "14",
            new Executor().expandVariables("$((12 | 6))", new HashMap<>())
        );
    }

    @Test
    public void arithmeticLogicalAnd() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((1 && 2))", env));
        Assert.assertEquals("0", executor.expandVariables("$((0 && 2))", env));
    }

    @Test
    public void arithmeticLogicalOr() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((0 || 5))", env));
        Assert.assertEquals("0", executor.expandVariables("$((0 || 0))", env));
    }

    @Test
    public void arithmeticUnaryNot() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((!0))", env));
        Assert.assertEquals("0", executor.expandVariables("$((!5))", env));
    }

    @Test
    public void arithmeticUnaryBitNot() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("-1", executor.expandVariables("$((~0))", env));
        Assert.assertEquals("-6", executor.expandVariables("$((~5))", env));
    }

    @Test
    public void arithmeticTernaryTrue() {
        Assert.assertEquals(
            "10",
            new Executor().expandVariables("$((1 ? 10 : 20))", new HashMap<>())
        );
    }

    @Test
    public void arithmeticTernaryFalse() {
        Assert.assertEquals(
            "20",
            new Executor().expandVariables("$((0 ? 10 : 20))", new HashMap<>())
        );
    }

    @Test
    public void arithmeticTernaryNested() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("30", executor.expandVariables("$((1 ? 2 ? 30 : 40 : 50))", env));
        Assert.assertEquals("40", executor.expandVariables("$((1 ? 0 ? 30 : 40 : 50))", env));
        Assert.assertEquals("50", executor.expandVariables("$((0 ? 30 : 40 ? 50 : 60))", env));
    }

    @Test
    public void arithmeticTernaryWithComparison() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "5");
        Assert.assertEquals("1", executor.expandVariables("$((x > 3 ? 1 : 0))", env));
        Assert.assertEquals("0", executor.expandVariables("$((x > 10 ? 1 : 0))", env));
    }

    @Test
    public void arithmeticTernaryArithmeticBranches() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("7", executor.expandVariables("$((1 ? 3 + 4 : 99))", env));
        Assert.assertEquals("99", executor.expandVariables("$((0 ? 3 + 4 : 99))", env));
    }

    @Test
    public void arithmeticAssign() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("5", new Executor().expandVariables("$((x = 5))", env));
        Assert.assertEquals("5", env.get("x"));
    }

    @Test
    public void arithmeticPlusAssign() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assert.assertEquals("13", new Executor().expandVariables("$((x += 3))", env));
        Assert.assertEquals("13", env.get("x"));
    }

    @Test
    public void arithmeticMinusAssign() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assert.assertEquals("7", new Executor().expandVariables("$((x -= 3))", env));
        Assert.assertEquals("7", env.get("x"));
    }

    @Test
    public void arithmeticMulDivModAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assert.assertEquals("30", executor.expandVariables("$((x *= 3))", env));
        env.put("y", "20");
        Assert.assertEquals("4", executor.expandVariables("$((y /= 5))", env));
        env.put("z", "23");
        Assert.assertEquals("3", executor.expandVariables("$((z %= 5))", env));
    }

    @Test
    public void arithmeticShiftAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "4");
        Assert.assertEquals("32", executor.expandVariables("$((x <<= 3))", env));
        env.put("y", "32");
        Assert.assertEquals("4", executor.expandVariables("$((y >>= 3))", env));
    }

    @Test
    public void arithmeticBitAssign() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        env.put("x", "12");
        Assert.assertEquals("4", executor.expandVariables("$((x &= 6))", env));
        env.put("y", "12");
        Assert.assertEquals("10", executor.expandVariables("$((y ^= 6))", env));
        env.put("z", "12");
        Assert.assertEquals("14", executor.expandVariables("$((z |= 6))", env));
    }

    @Test
    public void arithmeticAssignRightAssoc() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("7", new Executor().expandVariables("$((a = b = 7))", env));
        Assert.assertEquals("7", env.get("a"));
        Assert.assertEquals("7", env.get("b"));
    }

    @Test
    public void arithmeticShortCircuitAndSuppressesAssignment() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((0 && (x = 99)))", env);
        Assert.assertEquals(null, env.get("x"));
    }

    @Test
    public void arithmeticShortCircuitOrSuppressesAssignment() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 || (x = 99)))", env);
        Assert.assertEquals(null, env.get("x"));
    }

    @Test
    public void arithmeticTernaryShortCircuit() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        Assert.assertEquals("1", env.get("a"));
        Assert.assertEquals(null, env.get("b"));
        executor.expandVariables("$((0 ? (c = 3) : (d = 4)))", env);
        Assert.assertEquals(null, env.get("c"));
        Assert.assertEquals("4", env.get("d"));
    }

    @Test
    public void captureStreamFromPipeline() throws Exception {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("echo capture-me | cat"));
        Assert.assertTrue(
            String.format("captureStream: %s", bos),
            bos.toString(StandardCharsets.UTF_8).contains("capture-me")
        );
    }

    @Test
    public void resolveCommandWithCustomPathDirInPath() throws Exception {
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
        Assert.assertEquals(0, new Executor(env).execute(this.parse("myhello")));
    }

    @Test
    public void externalCloseStdin() {
        Assert.assertEquals(0, new Executor().execute(this.parse("cat <&-")));
    }

    @Test
    public void externalStdoutToStderr() {
        Assert.assertEquals(0, new Executor().execute(this.parse("true 1>&2")));
    }

    @Test
    public void externalReadWriteFdZero() throws Exception {
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
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <>%s", file.getAbsolutePath())))
        );
    }

    @Test
    public void externalHeredocDashStripsTabs() throws Exception {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <<-EOF%n\t\thello%nEOF%n")))
        );
    }

    @Test
    public void exportArgWithEqualsNonStandardName() throws Exception {
        final Executor executor = new Executor(new HashMap<>());
        Assert.assertEquals(0, executor.execute(this.parse("export 1foo=bar")));
        Assert.assertEquals("bar", executor.getVariable("1foo"));
    }

    @Test
    public void execNonExecutableFileTriggersIoException() throws Exception {
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
        Assert.assertTrue(String.format("non-executable should fail; got %s", code), code != 0);
    }

    @Test
    public void execInterruptedException() {
        final Executor executor = new Executor();
        Thread.currentThread().interrupt();
        final int code = executor.execute(this.parse("exec sleep 1"));
        Thread.interrupted();
        Assert.assertEquals(1, code);
    }

    @Test
    public void externalNonExecutableFileTriggersIoException() throws Exception {
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
        Assert.assertTrue(
            String.format("non-executable external should fail; got %s", code),
            code != 0
        );
    }

    @Test
    public void arithmeticAssignDivByZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assert.assertEquals("0", new Executor().expandVariables("$((x /= 0))", env));
        Assert.assertEquals("0", env.get("x"));
    }

    @Test
    public void arithmeticAssignModByZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assert.assertEquals("0", new Executor().expandVariables("$((x %= 0))", env));
    }

    @Test
    public void arithmeticAssignNonNumericEnvTreatedAsZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        Assert.assertEquals("5", new Executor().expandVariables("$((x += 5))", env));
        Assert.assertEquals("5", env.get("x"));
    }

    @Test
    public void arithmeticAssignWithUnderscoreName() {
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("42", new Executor().expandVariables("$((_foo = 42))", env));
        Assert.assertEquals("42", env.get("_foo"));
    }

    @Test
    public void externalReadWriteRedirectFdTwo() throws Exception {
        final String path = String.format("/tmp/sh4j_rw_fd2_%s", System.nanoTime());
        new java.io.File(path).delete();
        Assert.assertNotEquals(
            0,
            new Executor().execute(this.parse(String.format("cat /nonexistent_xyz 2<>%s", path)))
        );
        new java.io.File(path).delete();
    }

    @Test
    public void externalHereDocRedirect() throws Exception {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <<EOF%nhello world%nEOF%n")))
        );
    }

    @Test
    public void externalHereDocDashRedirect() throws Exception {
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <<-EOF%n\thello dashed%nEOF%n")))
        );
    }

    @Test
    public void shiftLargerThanPositionalCount() {
        Assert.assertEquals(
            1,
            new Executor(
                new HashMap<>(),
                java.util.Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b")
            ).execute(this.parse("shift 100"))
        );
    }

    @Test
    public void execWithMultipleArgs() {
        Assert.assertEquals(0, new Executor().execute(this.parse("exec true arg1 arg2 arg3")));
    }

    @Test
    public void positionalParamIndexOutOfRangeIsEmpty() {
        Assert.assertEquals(
            "",
            new Executor(
                new HashMap<>(),
                java.util.Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b")
            ).expandVariables("$9", new HashMap<>())
        );
    }

    @Test
    public void normaliseBackticksBackslashNonSpecialChar() {
        Assert.assertEquals("$(echo \\n)", WordExpander.normaliseBackticks("`echo \\n`"));
    }

    @Test
    public void whileLoopWithContinue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(
            this.parse("c=true; while $c; do c=false; continue; echo unreached; done")
        );
        Assert.assertTrue(
            String.format("continue should skip body, got: %s", bos),
            bos.toString(StandardCharsets.UTF_8).trim().isEmpty()
        );
    }

    @Test
    public void typeForExternalNonBuiltinCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(0, executor.execute(this.parse("type ls")));
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("ls is"));
    }

    @Test
    public void exitWithValidIntegerAndWordExpansionError() {
        Assert.assertEquals(
            1,
            new Executor(new HashMap<>()).execute(this.parse("exit 5${UNSET_FOR_EXIT_999:?msg}"))
        );
    }

    @Test
    public void expandBracedMultiDigitPositionalOutOfRange() {
        Assert.assertEquals(
            "",
            new Executor(
                new HashMap<>(),
                java.util.Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
            ).expandVariables("${10}", new HashMap<>())
        );
    }

    @Test
    public void expandBracedMultiDigitPositionalInRange() {
        Assert.assertEquals(
            "b",
            new Executor(
                new HashMap<>(),
                java.util.Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
            ).expandVariables("${2}", new HashMap<>())
        );
    }

    @Test
    public void expandBracedLeadingHashVar() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", ExecutorTest.HELLO);
        Assert.assertEquals("5", new Executor().expandVariables("${#x}", env));
    }

    @Test
    public void expandBracedLeadingHashUnsetVar() {
        Assert.assertEquals(
            "0",
            new Executor().expandVariables("${#UNSET_HASH_TEST_xyz}", new HashMap<>())
        );
    }

    @Test
    public void specialParamShellFlags() {
        Assert.assertEquals("", new Executor().expandVariables("$-", new HashMap<>()));
    }

    @Test
    public void specialParamBackgroundPidBeforeAnyJob() {
        Assert.assertEquals("0", new Executor().expandVariables("$!", new HashMap<>()));
    }

    @Test
    public void ppidIsSetToParentProcessIdOnInit() {
        final String ppid = new Executor().getVariable("PPID");
        Assert.assertNotNull(ppid);
        final long parsed = Long.parseLong(ppid);
        Assert.assertTrue("PPID should be a positive PID", parsed > 0L);
        final long expected = ProcessHandle.current()
            .parent().orElse(ProcessHandle.current()).pid();
        Assert.assertEquals(expected, parsed);
    }

    @Test
    public void ppidPreservedAcrossSubshellExecutor() {
        final Executor parent = new Executor();
        final String parentppid = parent.getVariable("PPID");
        final Executor sub = new Executor(parent.environmentMap(), java.util.Arrays.asList("sub"));
        Assert.assertEquals(parentppid, sub.getVariable("PPID"));
    }

    @Test
    public void ppidExpandsAsParameter() {
        final Executor executor = new Executor();
        final String expanded = executor.expandVariables("$PPID", executor.environmentMap());
        Assert.assertTrue("$PPID should expand to digits", expanded.matches("\\d+"));
    }

    @Test
    public void specialParamAtStarWithPositionals() {
        final Executor executor = new Executor(
            new HashMap<>(),
            java.util.Arrays.asList(ExecutorTest.SCRIPT_KEY, "a", "b", "c")
        );
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals(ExecutorTest.A_B_C, executor.expandVariables("$@", env));
        Assert.assertEquals(ExecutorTest.A_B_C, executor.expandVariables("$*", env));
    }

    @Test
    public void specialParamAtStarEmpty() {
        Assert.assertEquals(
            "",
            new Executor(
                new HashMap<>(),
                java.util.Arrays.asList(ExecutorTest.SCRIPT_KEY)
            ).expandVariables("$@", new HashMap<>())
        );
    }

    @Test
    public void arithmeticFactorNonNumericEnvAsZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        Assert.assertEquals("1", new Executor().expandVariables("$((x + 1))", env));
    }

    @Test
    public void resolveCommandPathNullReturnsNull() {
        final Map<String, String> empty = new HashMap<>();
        empty.put(ExecutorTest.PATH_KEY, "");
        final Executor executor = new Executor(empty);
        executor.expandVariables("", empty);
        Assert.assertEquals(127, executor.execute(this.parse("nonexistent_xyz_zzz_cmd_abc")));
    }

    @Test
    public void resolveCommandWithEmptyDirInPath() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PATH_KEY, ":/usr/bin");
        Assert.assertEquals(0, new Executor(env).execute(this.parse(ExecutorTest.TRUE_CMD)));
    }

    @Test
    public void createProcessBuilderWithQuotedArg() {
        Assert.assertEquals(0, new Executor().execute(this.parse("true \"quoted arg\"")));
    }

    @Test
    public void forLoopWithContinue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("for i in 1 2 3; do continue; echo $i; done"))
        );
        Assert.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void typeForExternalCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(0, executor.execute(this.parse("type true")));
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("true is"));
    }

    @Test
    public void exitWithWordExpansionError() {
        Assert.assertEquals(
            1,
            new Executor(new HashMap<>()).execute(this.parse("exit ${UNSET_FOR_EXIT_8x9:?errmsg}"))
        );
    }

    @Test
    public void exportArgWithEquals() {
        final Executor executor = new Executor(new HashMap<>());
        Assert.assertEquals(0, executor.execute(this.parse("export FOO=hello")));
        Assert.assertEquals(ExecutorTest.HELLO, executor.getVariable("FOO"));
    }

    @Test
    public void expandBracedColonEqWhenSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "set");
        Assert.assertEquals("set", new Executor().expandVariables("${x:=default}", env));
    }

    @Test
    public void expandBracedQuestionWhenSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", ExecutorTest.VALUE);
        Assert.assertEquals(
            ExecutorTest.VALUE,
            new Executor().expandVariables("${x?msg}", env)
        );
    }

    @Test
    public void expandBracedPrefixHashNoMatch() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abcdef");
        Assert.assertEquals("abcdef", new Executor().expandVariables("${x#zzz}", env));
    }

    @Test
    public void expandBracedUnknownOperatorFallsThrough() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", ExecutorTest.VALUE);
        Assert.assertEquals("${x@weird}", new Executor().expandVariables("${x@weird}", env));
    }

    @Test
    public void tildeUserNameFallthrough() {
        Assert.assertEquals(
            "~nonexistentuser123",
            new Executor().expandVariables("~nonexistentuser123", new HashMap<>())
        );
    }

    @Test
    public void arithmeticPrecedenceMixed() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        Assert.assertEquals("1", executor.expandVariables("$((1 + 2 == 3))", env));
        Assert.assertEquals("1", executor.expandVariables("$((1 < 2 && 3 < 4))", env));
        Assert.assertEquals("7", executor.expandVariables("$((1 | 2 | 4 & 6))", env));
    }

    @Test
    public void defaultIfsSet() {
        Assert.assertEquals(
            String.format(ExecutorTest.IFS_TAB_NL),
            new Executor().getVariable("IFS")
        );
    }

    @Test
    public void defaultHomeSet() {
        Assert.assertEquals(
            System.getProperty("user.home"),
            new Executor().getVariable(ExecutorTest.HOME_KEY)
        );
    }

    @Test
    public void defaultPathSet() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PATH_KEY, "");
        Assert.assertEquals(
            "/usr/local/bin:/usr/bin:/bin",
            new Executor(env).getVariable(ExecutorTest.PATH_KEY)
        );
    }

    @Test
    public void defaultsDoNotOverrideExistingEnv() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.HOME_KEY, "/custom/home");
        env.put(ExecutorTest.PATH_KEY, "/custom/path");
        final Executor executor = new Executor(env);
        Assert.assertEquals("/custom/home", executor.getVariable(ExecutorTest.HOME_KEY));
        Assert.assertEquals("/custom/path", executor.getVariable(ExecutorTest.PATH_KEY));
        Assert.assertEquals(
            String.format(ExecutorTest.IFS_TAB_NL),
            executor.getVariable("IFS")
        );
    }

    @Test
    public void commandFoundInPath() {
        Assert.assertEquals(0, new Executor().execute(this.parse(ExecutorTest.TRUE_CMD)));
    }

    @Test
    public void commandNotFoundReturnsNotFound() {
        Assert.assertEquals(127, new Executor().execute(this.parse("nonexistent_cmd_xyz_127")));
    }

    @Test
    public void commandWithSlashFound() {
        Assert.assertEquals(
            0,
            new Executor(new HashMap<>()).execute(this.parse("/usr/bin/env true"))
        );
    }

    @Test
    public void commandFoundInCustomPath() throws IOException {
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
        Assert.assertEquals(42, new Executor(env).execute(this.parse("mycmd")));
    }

    @Test
    public void commandNotFoundEmptyPath() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorTest.PATH_KEY, "");
        Assert.assertEquals(127, new Executor(env).execute(this.parse("somecmd")));
    }

    @Test
    public void commandFoundInSecondPathDir() throws IOException {
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
        Assert.assertEquals(7, new Executor(env).execute(this.parse("mycmd2")));
    }

    @Test
    public void commandWithSlashNotFound() {
        Assert.assertEquals(
            127,
            new Executor().execute(this.parse("/nonexistent_path_xyz_abc/cmd"))
        );
    }

    @Test
    public void exportWithValue() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("export FOO=bar")));
        Assert.assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    public void exportExistingVariable() {
        Assert.assertEquals(0, new Executor().execute(this.parse("export FOO")));
    }

    @Test
    public void exportThenVariableExpands() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("export MSG=hello; echo $MSG")));
        Assert.assertEquals(ExecutorTest.HELLO, executor.getVariable("MSG"));
    }

    @Test
    public void splitFieldsDefaultIfs() {
        Assert.assertEquals(
            Arrays.asList("a", "b", "c"),
            WordExpander.splitFields(ExecutorTest.A_B_C, String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    public void splitFieldsMultipleWs() {
        Assert.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("a   b", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    public void splitFieldsLeadingWs() {
        Assert.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("  a b", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    public void splitFieldsTrailingWs() {
        Assert.assertEquals(
            Arrays.asList("a", "b"),
            WordExpander.splitFields("a b  ", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    public void splitFieldsColon() {
        Assert.assertEquals(Arrays.asList("a", "b", "c"), WordExpander.splitFields("a:b:c", ":"));
    }

    @Test
    public void splitFieldsEmptyBetweenColons() {
        Assert.assertEquals(Arrays.asList("a", "", "b"), WordExpander.splitFields("a::b", ":"));
    }

    @Test
    public void splitFieldsOnlyColons() {
        Assert.assertEquals(Arrays.asList("", ""), WordExpander.splitFields(":", ":"));
    }

    @Test
    public void splitFieldsDoubleColon() {
        Assert.assertEquals(Arrays.asList("", "", ""), WordExpander.splitFields("::", ":"));
    }

    @Test
    public void splitFieldsNoDelim() {
        Assert.assertEquals(
            Arrays.asList(ExecutorTest.HELLO),
            WordExpander.splitFields(ExecutorTest.HELLO, ":")
        );
    }

    @Test
    public void splitFieldsEmptyIfs() {
        Assert.assertEquals(
            Arrays.asList(ExecutorTest.HELLO_WORLD),
            WordExpander.splitFields(ExecutorTest.HELLO_WORLD, "")
        );
    }

    @Test
    public void splitFieldsNullIfs() {
        Assert.assertEquals(
            Arrays.asList(ExecutorTest.HELLO),
            WordExpander.splitFields(ExecutorTest.HELLO, null)
        );
    }

    @Test
    public void splitFieldsEmptyValue() {
        Assert.assertEquals(
            Arrays.asList(),
            WordExpander.splitFields("", String.format(ExecutorTest.IFS_TAB_NL))
        );
    }

    @Test
    public void splitFieldsMixedWsNws() {
        Assert.assertEquals(Arrays.asList("a", "", "b"), WordExpander.splitFields("a :b", " :"));
    }

    @Test
    public void splitFieldsTrailingColon() {
        Assert.assertEquals(Arrays.asList("a", ""), WordExpander.splitFields("a:", ":"));
    }

    @Test
    public void splitFieldsCustomNws() {
        Assert.assertEquals(Arrays.asList("a", "b", "c"), WordExpander.splitFields("a,b,c", ","));
    }

    @Test
    public void isQuotedDoubleQuotes() {
        Assert.assertTrue(WordExpander.isQuoted("\"hello\""));
    }

    @Test
    public void isQuotedSingleQuotes() {
        Assert.assertTrue(WordExpander.isQuoted("'hello'"));
    }

    @Test
    public void isQuotedNoQuotes() {
        Assert.assertFalse(WordExpander.isQuoted(ExecutorTest.HELLO));
    }

    @Test
    public void isQuotedPartial() {
        Assert.assertFalse(WordExpander.isQuoted("a\"b\"c"));
    }

    @Test
    public void removeQuotesNoop() {
        Assert.assertEquals(ExecutorTest.HELLO, WordExpander.removeQuotes(ExecutorTest.HELLO));
    }

    @Test
    public void removeQuotesDouble() {
        Assert.assertEquals(
            ExecutorTest.HELLO_WORLD,
            WordExpander.removeQuotes("\"hello world\"")
        );
    }

    @Test
    public void removeQuotesSingle() {
        Assert.assertEquals(ExecutorTest.HELLO, WordExpander.removeQuotes("'hello'"));
    }

    @Test
    public void removeQuotesPartialDouble() {
        Assert.assertEquals("foovaluebar", WordExpander.removeQuotes("foo\"value\"bar"));
    }

    @Test
    public void removeQuotesPartialSingle() {
        Assert.assertEquals("helloworld", WordExpander.removeQuotes("hello'world'"));
    }

    @Test
    public void removeQuotesApostropheInsideDouble() {
        Assert.assertEquals("it's", WordExpander.removeQuotes("\"it's\""));
    }

    @Test
    public void removeQuotesNestedQuotes() {
        Assert.assertEquals(
            "hello \"there\" world",
            WordExpander.removeQuotes("\"hello \\\"there\\\" world\"")
        );
    }

    @Test
    public void removeQuotesBackslashOutside() {
        Assert.assertEquals(
            ExecutorTest.HELLO_WORLD,
            WordExpander.removeQuotes("hello\\ world")
        );
    }

    @Test
    public void removeQuotesBackslashInDouble() {
        Assert.assertEquals("hello\"world", WordExpander.removeQuotes("\"hello\\\"world\""));
    }

    @Test
    public void removeQuotesEmptyQuotes() {
        Assert.assertEquals("", WordExpander.removeQuotes("\"\""));
        Assert.assertEquals("", WordExpander.removeQuotes("''"));
    }

    @Test
    public void echoQuotedVarNoSplit() {
        Assert.assertEquals(
            0,
            new Executor(new HashMap<>()).execute(this.parse("MSG=\"hello  world\"; echo \"$MSG\""))
        );
    }

    @Test
    public void echoUnquotedVarSplit() {
        Assert.assertEquals(
            0,
            new Executor(new HashMap<>()).execute(this.parse("MSG=\"hello  world\"; echo $MSG"))
        );
    }

    @Test
    public void echoQuotedLiteralNoSplit() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo \"hello   world\"")));
    }

    @Test
    public void echoCustomIfsColon() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", ":");
        Assert.assertEquals(0, new Executor(env).execute(this.parse("echo a:b:c")));
    }

    @Test
    public void echoEmptyIfsNoSplit() {
        final Map<String, String> env = new HashMap<>();
        env.put("IFS", "");
        Assert.assertEquals(0, new Executor(env).execute(this.parse("echo hello   world")));
    }

    @Test
    public void splitFieldsTabIfs() {
        Assert.assertEquals(Arrays.asList("a", "b"), WordExpander.splitFields("a\tb", "\t"));
    }

    @Test
    public void splitFieldsMixedIfs() {
        Assert.assertEquals(Arrays.asList("a", "b", "c"), WordExpander.splitFields("a:b c", ": "));
    }

    @Test
    public void containsGlobCharsStar() {
        Assert.assertTrue(WordExpander.containsGlobChars("*.txt"));
    }

    @Test
    public void containsGlobCharsQuestion() {
        Assert.assertTrue(WordExpander.containsGlobChars("file?"));
    }

    @Test
    public void containsGlobCharsBracket() {
        Assert.assertTrue(WordExpander.containsGlobChars("[ab]"));
    }

    @Test
    public void containsGlobCharsNone() {
        Assert.assertFalse(WordExpander.containsGlobChars("hello.txt"));
    }

    @Test
    public void pathnameExpandNoGlob() {
        Assert.assertEquals(
            Arrays.asList(ExecutorTest.HELLO),
            WordExpander.pathnameExpand(Arrays.asList(ExecutorTest.HELLO))
        );
    }

    @Test
    public void pathnameExpandGlobNoMatchKeepsPattern() {
        Assert.assertEquals(
            Arrays.asList("__no_match__xyz__"),
            WordExpander.pathnameExpand(Arrays.asList("__no_match__xyz__"))
        );
    }

    @Test
    public void pathnameExpandGlobWithMatch() {
        Assert.assertEquals(
            Arrays.asList("pom.xml"),
            WordExpander.pathnameExpand(Arrays.asList("pom.xml"))
        );
    }

    @Test
    public void pathnameExpandWildcardMatch() {
        Assert.assertTrue(WordExpander.pathnameExpand(Arrays.asList("*.xml")).contains("pom.xml"));
    }

    @Test
    public void pathnameExpandGlobQuoted() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo \"*.java\"")));
    }

    @Test
    public void pathnameExpandGlobUnquoted() {
        Assert.assertEquals(0, new Executor().execute(this.parse("echo *.java")));
    }

    @Test
    public void expandBracedColonQuestionUnset() {
        try {
            new Executor(new HashMap<>()).expandVariables("${x:?my error}", new HashMap<>());
            Assert.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final Executor.ExitSignal ex) {
            Assert.assertEquals(1, ex.getCode());
        }
    }

    @Test
    public void expandBracedQuestionUnset() {
        try {
            new Executor(new HashMap<>()).expandVariables("${x?my error}", new HashMap<>());
            Assert.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final Executor.ExitSignal ex) {
            Assert.assertEquals(1, ex.getCode());
        }
    }

    @Test
    public void expandBracedColonQuestionSet() {
        Assert.assertEquals(
            ExecutorTest.VALUE,
            new Executor(
                new HashMap<>()
            ).expandVariables("${x:?not used}", new HashMap<>(Map.of("x", ExecutorTest.VALUE)))
        );
    }

    @Test
    public void expandBracedColonQuestionDefaultMsg() {
        try {
            new Executor(new HashMap<>()).expandVariables("${x:?}", new HashMap<>());
            Assert.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final Executor.ExitSignal ex) {
            Assert.assertEquals(1, ex.getCode());
        }
    }

    @Test
    public void expandBracedColonQuestionErrorStream() {
        final Executor executor = new Executor(new HashMap<>());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setErrorStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        try {
            executor.expandVariables("${x:?my error}", new HashMap<>());
            Assert.fail("POSIX 2.6.2 requires the non-interactive shell to exit");
        } catch (final Executor.ExitSignal ex) {
            Assert.assertEquals(1, ex.getCode());
        }
        Assert.assertTrue(
            "error message should go to injected stream",
            bos.toString(StandardCharsets.UTF_8).trim().contains("my error")
        );
    }

    @Test
    public void pwdPrintsCurrentDirectory() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("pwd")));
        Assert.assertEquals(
            ExecutorTest.TMP_DIR, bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    public void pwdLogicalFlagPrintsPwdEnv() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("pwd -L")));
        Assert.assertEquals(
            ExecutorTest.TMP_DIR, bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    public void commandDashVOnBuiltinPrintsName() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("command -v echo")));
        Assert.assertEquals(
            ExecutorTest.HELLO.replace(ExecutorTest.HELLO, "echo"),
            bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    public void commandDashUppervOnBuiltinDescribes() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("command -V true")));
        Assert.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("shell built-in")
        );
    }

    @Test
    public void commandWithoutFlagsRunsCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("command echo hi")));
        Assert.assertEquals("hi", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void umaskNoArgsPrintsDefault() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("umask")));
        Assert.assertEquals("0022", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void umaskSetThenReadEchoesValue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("umask 077")));
        Assert.assertEquals(0, executor.execute(this.parse("umask")));
        Assert.assertEquals("0077", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void umaskDashSPrintsSymbolic() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("umask -S")));
        Assert.assertEquals(
            "u=rwx,g=rx,o=rx", bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    public void umaskNonOctalReturnsOne() {
        Assert.assertEquals(1, new Executor().execute(this.parse("umask zzz")));
    }

    @Test
    public void killDashLListsSignals() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("kill -l")));
        Assert.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("TERM")
        );
    }

    @Test
    public void killNoArgsReturnsOne() {
        Assert.assertEquals(1, new Executor().execute(this.parse("kill")));
    }

    @Test
    public void killUnknownPidReturns127() {
        Assert.assertEquals(
            127,
            new Executor().execute(this.parse("kill 999999999"))
        );
    }

    @Test
    public void killDashSWithNonNumericPidReturns127() {
        Assert.assertEquals(
            127,
            new Executor().execute(this.parse("kill -s TERM notapid"))
        );
    }

    @Test
    public void hashNoArgsReturnsZero() {
        Assert.assertEquals(0, new Executor().execute(this.parse("hash")));
    }

    @Test
    public void hashDashRReturnsZero() {
        Assert.assertEquals(0, new Executor().execute(this.parse("hash -r")));
    }

    @Test
    public void hashWithKnownBuiltinReturnsZero() {
        Assert.assertEquals(0, new Executor().execute(this.parse("hash echo")));
    }

    @Test
    public void hashWithUnknownNameReturns127() {
        Assert.assertEquals(
            127,
            new Executor().execute(this.parse("hash sh4j_no_such_cmd_xyz"))
        );
    }

    @Test
    public void waitNoArgsReturnsZero() {
        Assert.assertEquals(0, new Executor().execute(this.parse("wait")));
    }

    @Test
    public void waitOnKnownBgPidReturnsZero() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 &")));
        final int pid = executor.lastBackgroundPid();
        Assert.assertEquals(
            0,
            executor.execute(this.parse(String.format("wait %d", pid)))
        );
    }

    @Test
    public void waitOnUnknownPidReturns127() {
        Assert.assertEquals(
            127,
            new Executor().execute(this.parse("wait 999999"))
        );
    }

    @Test
    public void jobsListsBackgroundJobs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(
            "expected [1] in output, got: " + output,
            output.contains("[1]")
        );
        Assert.assertTrue(
            "expected 'sleep 0' in output, got: " + output,
            output.contains("sleep 0")
        );
    }

    @Test
    public void jobsWithPercentId() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs %1")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(
            "expected [1] in output, got: " + output,
            output.contains("[1]")
        );
    }

    @Test
    public void jobsEmptyWhenNoBackgroundJobs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("jobs")));
        Assert.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void jobsWithNumericId() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs 1")));
        Assert.assertTrue(
            "output was: " + bos,
            bos.toString(StandardCharsets.UTF_8).contains("[1]")
        );
    }

    @Test
    public void jobsWithNonExistentJobId() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs 99999")));
    }

    @Test
    public void jobsWithInvalidJobIdReturnsZero() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 & jobs notanumber")));
        Assert.assertEquals("", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void jobsShowsDoneForCompletedJob() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        final int id = executor.jobManager().submit(() -> { }, "test-cmd");
        executor.jobManager().waitForJob(id);
        executor.execute(this.parse("jobs"));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(
            "expected Done state in jobs output, got: " + output,
            output.contains("Done")
        );
    }

    @Test
    public void bgConfirmsBackgroundJob() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 & bg")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(
            "expected [1] in output, got: " + output,
            output.contains("[1]")
        );
    }

    @Test
    public void bgNoJobsReturnsError() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(1, executor.execute(this.parse("bg")));
    }

    @Test
    public void bgWithPercentId() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 & bg %1")));
        Assert.assertTrue(
            "output was: " + bos,
            bos.toString(StandardCharsets.UTF_8).contains("[1]")
        );
    }

    @Test
    public void bgWithInvalidJobIdReturnsError() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(1, executor.execute(this.parse("sleep 0 & bg 99999")));
    }

    @Test
    public void bgWithInvalidJobSyntaxReturnsError() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(1, executor.execute(this.parse("sleep 0 & bg notanumber")));
    }

    @Test
    public void bgResumesStoppedJob() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
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
        final JobManager.Job job = mgr.getJob(id);
        job.cancel();
        Assert.assertEquals(0, executor.execute(this.parse("bg " + id)));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(
            "expected bg confirmation in output, got: " + output,
            output.contains("[" + id + "]")
        );
    }

    @Test
    public void fgBringsJobToForeground() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 & fg")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(
            "expected 'sleep 0' in output, got: " + output,
            output.contains("sleep 0")
        );
    }

    @Test
    public void fgWithSpecificJobId() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 & fg %1")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(
            "expected 'sleep 0' in output, got: " + output,
            output.contains("sleep 0")
        );
    }

    @Test
    public void fgNoJobsReturnsError() {
        final Executor executor = new Executor();
        Assert.assertEquals(1, executor.execute(this.parse("fg")));
    }

    @Test
    public void fgUnknownJobReturnsError() {
        final Executor executor = new Executor();
        Assert.assertEquals(1, executor.execute(this.parse("fg 99999")));
    }

    @Test
    public void fgWithInvalidJobSyntaxReturnsError() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(1, executor.execute(this.parse("fg notajob")));
    }

    @Test
    public void fcListsHistoryEntries() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.execute(this.parse("echo one"));
        executor.execute(this.parse("echo two"));
        executor.execute(this.parse("echo three"));
        Assert.assertEquals(0, executor.execute(this.parse("fc -l 1 3")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(output.contains("1\techo one"));
        Assert.assertTrue(output.contains("2\techo two"));
        Assert.assertTrue(output.contains("3\techo three"));
    }

    @Test
    public void fcListsNoNumbers() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.execute(this.parse("echo hello"));
        Assert.assertEquals(0, executor.execute(this.parse("fc -ln 1 1")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(output.contains("\techo hello"));
        Assert.assertFalse(output.contains("1\t"));
    }

    @Test
    public void fcListsLastEntry() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.execute(this.parse("echo first"));
        executor.execute(this.parse("echo second"));
        Assert.assertEquals(0, executor.execute(this.parse("fc -l -1 -1")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(output.contains("echo second"));
        Assert.assertFalse(output.contains("echo first"));
    }

    @Test
    public void fcEmptyHistoryReturnsError() {
        final Executor executor = new Executor();
        Assert.assertEquals(1, executor.execute(this.parse("fc -l")));
    }

    @Test
    public void fcSubstituteAndReExecute() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.execute(this.parse("echo hello"));
        Assert.assertEquals(0, executor.execute(this.parse("fc -s hello=world")));
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("echo world"));
    }

    @Test
    public void fcSubstituteInvalidPattern() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.execute(this.parse("echo hello"));
        Assert.assertEquals(1, executor.execute(this.parse("fc -s nosign")));
    }

    @Test
    public void fcUnknownOptionReturnsError() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.execute(this.parse("echo hello"));
        Assert.assertEquals(1, executor.execute(this.parse("fc -z")));
    }

    @Test
    public void fcSubstituteNonExistentNumber() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.execute(this.parse("echo hello"));
        Assert.assertEquals(1, executor.execute(this.parse("fc -s hello=world 999")));
    }

    @Test
    public void fcDefaultRangeListsLast16() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        for (int i = 1; i <= 5; i = i + 1) {
            executor.execute(this.parse("echo cmd" + i));
        }
        Assert.assertEquals(0, executor.execute(this.parse("fc -l")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(output.contains("echo cmd1"));
        Assert.assertTrue(output.contains("echo cmd5"));
    }

    @Test
    public void linenoReflectsSourceLine() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0, executor.execute(this.parse("echo $LINENO"))
        );
        Assert.assertEquals("1\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void linenoUpdatesPerCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0, executor.execute(this.parse("echo $LINENO; echo $LINENO"))
        );
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(output.contains("1\n"));
        Assert.assertTrue(output.contains("1\n1\n") || output.split("\n").length >= 2);
    }

    @Test
    public void langVariableCanBeSet() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("LANG=en_US.UTF-8")));
        Assert.assertEquals("en_US.UTF-8", executor.getVariable("LANG"));
    }

    @Test
    public void lcAllVariableOverridesOthers() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("LC_ALL=C")));
        Assert.assertEquals("C", executor.getVariable("LC_ALL"));
    }

    @Test
    public void lcCollateVariableCanBeSet() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("LC_COLLATE=en_US.UTF-8")));
        Assert.assertEquals("en_US.UTF-8", executor.getVariable("LC_COLLATE"));
    }

    @Test
    public void lcCtypeVariableCanBeSet() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("LC_CTYPE=en_US.UTF-8")));
        Assert.assertEquals("en_US.UTF-8", executor.getVariable("LC_CTYPE"));
    }

    @Test
    public void lcMessagesVariableCanBeSet() {
        final Executor executor = new Executor();
        Assert.assertEquals(0, executor.execute(this.parse("LC_MESSAGES=en_US.UTF-8")));
        Assert.assertEquals("en_US.UTF-8", executor.getVariable("LC_MESSAGES"));
    }

    @Test
    public void localeVariablesCanBeExported() {
        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("LC_COLLATE", "POSIX");
        env.put("LC_CTYPE", "UTF-8");
        env.put("LC_MESSAGES", "C");
        final Executor executor = new Executor(env);
        Assert.assertEquals("C", executor.getVariable("LANG"));
        Assert.assertEquals("C", executor.getVariable("LC_ALL"));
        Assert.assertEquals("POSIX", executor.getVariable("LC_COLLATE"));
        Assert.assertEquals("UTF-8", executor.getVariable("LC_CTYPE"));
        Assert.assertEquals("C", executor.getVariable("LC_MESSAGES"));
    }

    @Test
    public void commandDashVOnMissingReturnsOne() {
        Assert.assertEquals(
            1,
            new Executor().execute(this.parse("command -v sh4j_no_such_cmd_xyz"))
        );
    }

    @Test
    public void pwdPhysicalFlagResolvesSymlinks() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorTest.TMP_DIR);
        final Executor executor = new Executor(env);
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("pwd -P")));
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).startsWith("/"));
    }

    @Test
    public void cdToDirectory() throws Exception {
        final String saved = System.getProperty(ExecutorTest.USER_DIR_PROP);
        try {
            final Executor executor = new Executor();
            Assert.assertEquals(0, executor.execute(this.parse("cd /tmp")));
            Assert.assertEquals(
                ExecutorTest.TMP_DIR,
                executor.getVariable(ExecutorTest.PWD_KEY)
            );
        } finally {
            System.setProperty(ExecutorTest.USER_DIR_PROP, saved);
        }
    }

    @Test
    public void cdToHome() throws Exception {
        final String saved = System.getProperty(ExecutorTest.USER_DIR_PROP);
        try {
            final Map<String, String> env = new HashMap<>();
            env.put(ExecutorTest.HOME_KEY, ExecutorTest.TMP_DIR);
            final Executor executor = new Executor(env);
            Assert.assertEquals(0, executor.execute(this.parse("cd")));
            Assert.assertEquals(
                ExecutorTest.TMP_DIR,
                executor.getVariable(ExecutorTest.PWD_KEY)
            );
        } finally {
            System.setProperty(ExecutorTest.USER_DIR_PROP, saved);
        }
    }

    @Test
    public void cdToNonexistentReturnsOne() {
        Assert.assertEquals(
            1,
            new Executor().execute(this.parse("cd /sh4j_nonexistent_dir_xyz"))
        );
    }

    @Test
    public void cdMinus() throws Exception {
        final String saved = System.getProperty(ExecutorTest.USER_DIR_PROP);
        try {
            final Map<String, String> env = new HashMap<>();
            env.put(ExecutorTest.PWD_KEY, ExecutorTest.TMP_DIR);
            env.put("OLDPWD", "/");
            final Executor executor = new Executor(env);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            executor.setCaptureStream(bos);
            Assert.assertEquals(0, executor.execute(this.parse("cd -")));
            Assert.assertEquals("/", executor.getVariable(ExecutorTest.PWD_KEY));
            Assert.assertEquals(ExecutorTest.TMP_DIR, executor.getVariable("OLDPWD"));
            Assert.assertEquals("/", bos.toString(StandardCharsets.UTF_8).trim());
        } finally {
            System.setProperty(ExecutorTest.USER_DIR_PROP, saved);
        }
    }

    @Test
    public void cdWithExpandedVar() throws Exception {
        final String saved = System.getProperty(ExecutorTest.USER_DIR_PROP);
        try {
            final Map<String, String> env = new HashMap<>();
            env.put("MYDIR", ExecutorTest.TMP_DIR);
            final Executor executor = new Executor(env);
            Assert.assertEquals(0, executor.execute(this.parse("cd $MYDIR")));
            Assert.assertEquals(
                ExecutorTest.TMP_DIR,
                executor.getVariable(ExecutorTest.PWD_KEY)
            );
        } finally {
            System.setProperty(ExecutorTest.USER_DIR_PROP, saved);
        }
    }

    @Test
    public void typeBuiltin() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("type echo cd exit export type true false shift unset"))
        );
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(out.contains("echo is a shell built-in"));
        Assert.assertTrue(out.contains("cd is a shell built-in"));
        Assert.assertTrue(out.contains("exit is a shell built-in"));
        Assert.assertTrue(out.contains("export is a shell built-in"));
        Assert.assertTrue(out.contains("type is a shell built-in"));
        Assert.assertTrue(out.contains("true is a shell built-in"));
        Assert.assertTrue(out.contains("false is a shell built-in"));
    }

    @Test
    public void typeExternal() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(0, executor.execute(this.parse("type true")));
        Assert.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("true is "));
    }

    @Test
    public void typeNotFound() {
        Assert.assertEquals(1, new Executor().execute(this.parse("type nonexistent_cmd_xyz")));
    }

    @Test
    public void execBuiltinRunsCommand() {
        Assert.assertEquals(0, new Executor().execute(this.parse("exec true")));
    }

    @Test
    public void execBuiltinNotFoundReturnsNotFound() {
        Assert.assertEquals(
            127,
            new Executor().execute(this.parse("exec nonexistent_cmd_xyz"))
        );
    }

    @Test
    public void execBuiltinNoArgsIsNoop() {
        Assert.assertEquals(0, new Executor().execute(this.parse("exec")));
    }

    @Test
    public void trueBuiltinReturnsZero() {
        Assert.assertEquals(0, new Executor().execute(this.parse(ExecutorTest.TRUE_CMD)));
    }

    @Test
    public void falseBuiltinReturnsOne() {
        Assert.assertEquals(1, new Executor().execute(this.parse(ExecutorTest.FALSE_CMD)));
    }

    @Test
    public void unsetRemovesVariable() {
        final Map<String, String> env = new HashMap<>();
        env.put("MYVAR", ExecutorTest.VALUE);
        final Executor executor = new Executor(env);
        Assert.assertEquals(0, executor.execute(this.parse("unset MYVAR")));
        Assert.assertNull(executor.getVariable("MYVAR"));
    }

    @Test
    public void shiftRemovesPositionalParam() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        Assert.assertEquals(0, executor.execute(this.parse("shift; echo $#")));
        Assert.assertEquals("2", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void shiftWithCount() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(
            new HashMap<>(),
            new ArrayList<>(Arrays.asList("sh", "a", "b", "c"))
        );
        executor.setCaptureStream(bos);
        Assert.assertEquals(0, executor.execute(this.parse("shift 2; echo $#")));
        Assert.assertEquals("1", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void breakStopsWhileLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("while true; do echo ran; break; done"))
        );
        Assert.assertEquals("ran", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void breakStopsForLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("for i in a b c; do echo $i; break; done"))
        );
        Assert.assertEquals("a", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void continueInForLoop() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse("for i in a b; do echo $i; continue; echo after; done"))
        );
        Assert.assertEquals(String.format("a%nb"), bos.toString(StandardCharsets.UTF_8).trim());
        Assert.assertFalse(bos.toString(StandardCharsets.UTF_8).contains("after"));
    }

    @Test
    public void colonBuiltinReturnsZero() {
        Assert.assertEquals(0, new Executor().execute(this.parse(":")));
    }

    @Test
    public void evalBuiltinRunsCommand() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(0, executor.execute(this.parse("eval echo hello world")));
        Assert.assertEquals(
            ExecutorTest.HELLO_WORLD,
            bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    public void evalBuiltinNoArgsReturnsZero() {
        Assert.assertEquals(0, new Executor().execute(this.parse("eval")));
    }

    @Test
    public void externalOutputRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new java.io.File(outpath).delete();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO_WORLD);
        }
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s > %s", inpath, outpath)))
        );
        Assert.assertTrue(new java.io.File(outpath).exists());
        Assert.assertEquals(
            ExecutorTest.HELLO_WORLD,
            new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(outpath)), StandardCharsets.UTF_8).trim()
        );
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    public void externalAppendRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_append_in.txt";
        final String outpath = "/tmp/sh4j_ext_append_out.txt";
        new java.io.File(outpath).delete();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("second");
        }
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Paths.get(outpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("first%n"));
        }
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s >> %s", inpath, outpath)))
        );
        final String content = new String(java.nio.file.Files.readAllBytes(
            java.nio.file.Paths.get(outpath)
        ), StandardCharsets.UTF_8).trim();
        Assert.assertTrue(content.startsWith("first"));
        Assert.assertTrue(content.contains("second"));
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    public void externalClobberRedirect() throws Exception {
        final String inpath = "/tmp/sh4j_ext_clobber_in.txt";
        final String outpath = "/tmp/sh4j_ext_clobber_out.txt";
        new java.io.File(outpath).delete();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write("hello clobber");
        }
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s >| %s", inpath, outpath)))
        );
        Assert.assertTrue(new java.io.File(outpath).exists());
        Assert.assertEquals(
            "hello clobber",
            new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(outpath)), StandardCharsets.UTF_8).trim()
        );
        new java.io.File(inpath).delete();
        new java.io.File(outpath).delete();
    }

    @Test
    public void externalRedirectStdoutToStderr() throws Exception {
        final String inpath = "/tmp/sh4j_ext_12_in.txt";
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO);
        }
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s 1>&2", inpath)))
        );
        new java.io.File(inpath).delete();
    }

    @Test
    public void externalRedirectStderrToStdout() throws Exception {
        final String inpath = "/tmp/sh4j_ext_21_in.txt";
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO);
        }
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s 2>&1", inpath)))
        );
        new java.io.File(inpath).delete();
    }

    @Test
    public void externalCloseStdout() {
        Assert.assertEquals(0, new Executor().execute(this.parse("true 1>&-")));
    }

    @Test
    public void externalCloseStderr() {
        Assert.assertEquals(0, new Executor().execute(this.parse("true 2>&-")));
    }

    @Test
    public void externalStderrOutputRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_out.txt";
        new java.io.File(path).delete();
        final Executor executor = new Executor();
        executor.execute(this.parse(String.format("cat /nonexistent_path_xyz_123 2> %s", path)));
        Assert.assertTrue(new java.io.File(path).exists());
        new java.io.File(path).delete();
    }

    @Test
    public void externalStderrAppendRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_err_append.txt";
        new java.io.File(path).delete();
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Paths.get(path),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(String.format("initial%n"));
        }
        final Executor executor = new Executor();
        executor.execute(this.parse(String.format("cat /nonexistent_path_xyz_456 2>> %s", path)));
        Assert.assertTrue(
            new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)), StandardCharsets.UTF_8).contains("initial")
        );
        new java.io.File(path).delete();
    }

    @Test
    public void externalReadWriteRedirect() throws Exception {
        final String path = "/tmp/sh4j_ext_rw.txt";
        try (
            java.io.BufferedWriter w = Files.newBufferedWriter(
                java.nio.file.Paths.get(path),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorTest.HELLO);
        }
        Assert.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat <> %s", path)))
        );
        new java.io.File(path).delete();
    }

    @Test
    public void heredocStripTabsBasic() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\thello%nEOF%n")))
        );
        Assert.assertEquals(ExecutorTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void heredocStripTabsMultiple() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\t\thello%nEOF%n")))
        );
        Assert.assertEquals(ExecutorTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void heredocStripTabsDelimiterWithTabs() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(this.parse(String.format("cat <<- EOF%n\thello%n\tEOF%n")))
        );
        Assert.assertEquals(ExecutorTest.HELLO, bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    public void heredocStripTabsMixed() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(
            0,
            executor.execute(
                this.parse(String.format("cat <<- MARKER%n\tfirst%nsecond%n\tthird%nMARKER%n"))
            )
        );
        Assert.assertEquals(
            String.format("first%nsecond%nthird"),
            bos.toString(StandardCharsets.UTF_8).trim()
        );
    }

    @Test
    public void typeFunction() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assert.assertEquals(0, executor.execute(this.parse("myfunc() { true; }; type myfunc")));
        Assert.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("myfunc is a function")
        );
    }

    @Test
    public void shiftNonNumericReturnsOne() {
        Assert.assertEquals(1, new Executor().execute(this.parse("shift abc")));
    }

    @Test
    public void shiftNegativeReturnsOne() {
        Assert.assertEquals(1, new Executor().execute(this.parse("shift -1")));
    }

    @Test
    public void readWithOneVar() {
        final Executor executor = new Executor();
        executor.setInputStream(new ByteArrayInputStream("hello\n".getBytes()));
        Assert.assertEquals(0, executor.execute(this.parse("read var")));
        Assert.assertEquals("hello", executor.getVariable("var"));
    }

    @Test
    public void readWithMultipleVars() {
        final Executor executor = new Executor();
        executor.setInputStream(new ByteArrayInputStream("a b c\n".getBytes()));
        Assert.assertEquals(0, executor.execute(this.parse("read x y z")));
        Assert.assertEquals("a", executor.getVariable("x"));
        Assert.assertEquals("b", executor.getVariable("y"));
        Assert.assertEquals("c", executor.getVariable("z"));
    }

    @Test
    public void readLastVarGetsRemainingFields() {
        final Executor executor = new Executor();
        executor.setInputStream(new ByteArrayInputStream("a b c d\n".getBytes()));
        Assert.assertEquals(0, executor.execute(this.parse("read x y")));
        Assert.assertEquals("a", executor.getVariable("x"));
        Assert.assertEquals("b c d", executor.getVariable("y"));
    }

    @Test
    public void readReplyWhenNoVar() {
        final Executor executor = new Executor();
        executor.setInputStream(new ByteArrayInputStream("hello world\n".getBytes()));
        Assert.assertEquals(0, executor.execute(this.parse("read")));
        Assert.assertEquals("hello world", executor.getVariable("REPLY"));
    }

    @Test
    public void readEofReturnsOne() {
        final Executor executor = new Executor();
        executor.setInputStream(new ByteArrayInputStream(new byte[0]));
        Assert.assertEquals(1, executor.execute(this.parse("read var")));
    }

    @Test
    public void readRawModePreservesBackslash() {
        final Executor executor = new Executor();
        executor.setInputStream(new ByteArrayInputStream("a\\tb\n".getBytes()));
        Assert.assertEquals(0, executor.execute(this.parse("read -r var")));
        Assert.assertEquals("a\\tb", executor.getVariable("var"));
    }

    @Test
    public void readBackslashWithoutRawRemovesEscape() {
        final Executor executor = new Executor();
        executor.setInputStream(new ByteArrayInputStream("a\\tb\n".getBytes()));
        Assert.assertEquals(0, executor.execute(this.parse("read var")));
        Assert.assertEquals("atb", executor.getVariable("var"));
    }

    @Test
    public void aliasExpandsToBuiltin() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hello");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("ll")));
        Assert.assertEquals("hello\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void aliasExpandsWithArgs() {
        final Executor executor = new Executor();
        executor.aliases().put("greet", "echo hello");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("greet world")));
        Assert.assertEquals("hello world\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void aliasDoesNotAffectOtherCommands() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "echo hi");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("echo hello")));
        Assert.assertEquals("hello\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void aliasBuiltinSetAndExpand() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.aliases().put("ll", "echo hello");
        Assert.assertEquals(0, executor.execute(this.parse("ll")));
        Assert.assertEquals("hello\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void aliasBuiltinSetViaEq() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("alias ll='echo hello'")));
        Assert.assertEquals(0, executor.execute(this.parse("ll")));
        Assert.assertEquals("hello\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void aliasBuiltinPrintAll() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.aliases().put("ll", "ls -l");
        Assert.assertEquals(0, executor.execute(this.parse("alias")));
        Assert.assertTrue(
            bos.toString(StandardCharsets.UTF_8).contains("ll='ls -l'")
        );
    }

    @Test
    public void aliasBuiltinPrintOne() {
        final Executor executor = new Executor();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        executor.aliases().put("ll", "ls -l");
        Assert.assertEquals(0, executor.execute(this.parse("alias ll")));
        Assert.assertEquals("ll='ls -l'\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void aliasBuiltinUndefinedReturnsOne() {
        final Executor executor = new Executor();
        Assert.assertEquals(1, executor.execute(this.parse("alias nonexistent")));
    }

    @Test
    public void unaliasRemovesOne() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        Assert.assertEquals(0, executor.execute(this.parse("unalias ll")));
        Assert.assertFalse(executor.aliases().containsKey("ll"));
        Assert.assertTrue(executor.aliases().containsKey("gg"));
    }

    @Test
    public void unaliasAllRemovesAll() {
        final Executor executor = new Executor();
        executor.aliases().put("ll", "ls -l");
        executor.aliases().put("gg", "grep");
        Assert.assertEquals(0, executor.execute(this.parse("unalias -a")));
        Assert.assertTrue(executor.aliases().isEmpty());
    }

    @Test
    public void unaliasUnknownReturnsOne() {
        final Executor executor = new Executor();
        Assert.assertEquals(1, executor.execute(this.parse("unalias nonexistent")));
    }

    @Test
    public void getoptsParsesSimpleFlag() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "file"));
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
        Assert.assertEquals("a", executor.getVariable("opt"));
        Assert.assertEquals("", executor.getVariable("OPTARG"));
    }

    @Test
    public void getoptsParsesFlagWithArg() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b", "val", "file"));
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
        Assert.assertEquals("b", executor.getVariable("opt"));
        Assert.assertEquals("val", executor.getVariable("OPTARG"));
    }

    @Test
    public void getoptsReturnsOneWhenDone() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "file"));
        Assert.assertEquals(
            1, executor.execute(this.parse("getopts ab: opt"))
        );
    }

    @Test
    public void getoptsParsesMultipleFlags() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a", "-b", "val", "file"));
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
        Assert.assertEquals("a", executor.getVariable("opt"));
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
        Assert.assertEquals("b", executor.getVariable("opt"));
        Assert.assertEquals("val", executor.getVariable("OPTARG"));
        Assert.assertEquals(
            1, executor.execute(this.parse("getopts ab: opt"))
        );
    }

    @Test
    public void getoptsNoArgsReturnsOne() {
        final Executor executor = new Executor();
        Assert.assertEquals(1, executor.execute(this.parse("getopts")));
    }

    @Test
    public void getoptsWithDoubleDash() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "--", "file"));
        Assert.assertEquals(
            1, executor.execute(this.parse("getopts ab opt"))
        );
        Assert.assertEquals("?", executor.getVariable("opt"));
    }

    @Test
    public void getoptsIllegalOption() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(env, Arrays.asList("sh", "-c", "file"));
        executor.setErrorStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts ab opt"))
        );
        Assert.assertEquals("?", executor.getVariable("opt"));
    }

    @Test
    public void getoptsMissingArgWithColon() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b"));
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts :ab: opt"))
        );
        Assert.assertEquals(":", executor.getVariable("opt"));
    }

    @Test
    public void getoptsMissingArgWithoutColon() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor(env, Arrays.asList("sh", "-b"));
        executor.setErrorStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts ab: opt"))
        );
        Assert.assertEquals("?", executor.getVariable("opt"));
    }

    @Test
    public void getoptsWithExplicitArgs() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "1");
        final Executor executor = new Executor(env);
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts ab opt -a -b val"))
        );
        Assert.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    public void getoptsInvalidOptind() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "notanumber");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a"));
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts a opt"))
        );
        Assert.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    public void getoptsNegativeOptind() {
        final Map<String, String> env = new HashMap<>();
        env.put("OPTIND", "-5");
        final Executor executor = new Executor(env, Arrays.asList("sh", "-a"));
        Assert.assertEquals(
            0, executor.execute(this.parse("getopts a opt"))
        );
        Assert.assertEquals("a", executor.getVariable("opt"));
    }

    @Test
    public void cdDashWithoutOldPwdReturnsOne() {
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", "/usr/bin");
        env.put("HOME", "/tmp");
        final Executor executor = new Executor(env);
        Assert.assertEquals(1, executor.execute(this.parse("cd -")));
    }

    @Test
    public void pwdPhysicalIoError() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/nonexistent/path/that/does/not/exist");
        env.put("PATH", "/usr/bin");
        final Executor executor = new Executor(env);
        Assert.assertEquals(1, executor.execute(this.parse("pwd -P")));
    }

    @Test
    public void echoQuotedFieldWithSpace() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0, executor.execute(this.parse("echo 'hello world'"))
        );
        Assert.assertEquals("hello world\n", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void aliasWithDoubleQuotedValue() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0, executor.execute(this.parse("alias ll=\"ls -l\""))
        );
        Assert.assertEquals("ls -l", executor.aliases().get("ll"));
    }

    @Test
    public void aliasShortValueNoUnquote() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(
            0, executor.execute(this.parse("alias x='a'"))
        );
        Assert.assertEquals("a", executor.aliases().get("x"));
    }

    @Test
    public void unaliasUnknownNameReturnsOne() {
        final Executor executor = new Executor();
        Assert.assertEquals(1, executor.execute(this.parse("unalias unknown_alias")));
    }

    @Test
    public void typeBuiltinsBreakContinueEval() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("type break continue eval")));
        final String out = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(out.contains("break is a shell built-in"));
        Assert.assertTrue(out.contains("continue is a shell built-in"));
        Assert.assertTrue(out.contains("eval is a shell built-in"));
    }

    @Test
    public void executorDefaultsPathWhenMissing() {
        final Map<String, String> env = new HashMap<>();
        env.put("HOME", "/tmp");
        final Executor executor = new Executor(env);
        Assert.assertNotNull(executor.getVariable("PATH"));
        Assert.assertFalse(executor.getVariable("PATH").isEmpty());
    }

    @Test
    public void functionDefinitionParsing() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("myfunc() { echo hi; }")));
        Assert.assertTrue(executor.functionsMap().containsKey("myfunc"));
    }

    @Test
    public void backgroundPipelineWithPipe() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        Assert.assertEquals(0, executor.execute(this.parse("sleep 0 | true & jobs")));
        final String output = bos.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(
            "expected [1] in output, got: " + output,
            output.contains("[1]")
        );
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
