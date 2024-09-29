/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
}
