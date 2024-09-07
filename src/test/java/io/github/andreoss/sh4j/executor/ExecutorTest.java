/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import static org.junit.jupiter.api.Assertions.*;

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
    void executeIfFalseNoElse() {
        ListNode list = parser("if false; then echo yes; fi").parse();
        Executor executor = new Executor();
        assertEquals(1, executor.execute(list));
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
}
