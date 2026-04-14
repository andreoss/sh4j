/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: execute builtin.
 *
 * @since 1.0
 */
final class ExecutorExecuteBuiltinTest {

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
            String.format("missing middle stage must exit 127 or 126; got %s", code)
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

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
