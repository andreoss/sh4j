/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.SimpleCommand;

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
        ListNode list = parser("sleep 1 &").parse();
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
}
