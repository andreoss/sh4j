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
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: echo unquoted.
 *
 * @since 1.0
 */
final class ExecutorEchoUnquotedTest {

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
    void echoQuotedFieldWithSpaceFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("echo 'hello world'")));
        stream.flush();
    }

    @Test
    void echoQuotedFieldWithSpaceSecond() {
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
    void evalBuiltinRunsCommandFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(0, executor.execute(this.parse("eval echo hello world")));
    }

    @Test
    void evalBuiltinRunsCommandSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("eval echo hello world"));
        Assertions.assertEquals("hello world", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void evalBuiltinNoArgsReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("eval")));
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
        Assertions.assertTrue(code != 0, String.format("non-executable should fail; got %s", code));
    }

    @Test
    void execInterruptedException() {
        Thread.currentThread().interrupt();
        final int code = new Executor().execute(this.parse("exec sleep 1"));
        Thread.interrupted();
        Assertions.assertEquals(1, code);
    }

    @Test
    void execWithMultipleArgs() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("exec true arg1 arg2 arg3")));
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
    void executeSimpleEcho() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("echo hello")));
    }

    @Test
    void executePwd() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("pwd")));
    }

    @Test
    void executeTrue() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true")));
    }

    @Test
    void executeFalse() {
        Assertions.assertEquals(1, new Executor().execute(this.parse("false")));
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
    void executeAssignmentOnlyFirst() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("FOO=bar")));
    }

    @Test
    void executeAssignmentOnlySecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("FOO=bar"));
        Assertions.assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void executeAssignmentWithCommand() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("FOO=hello echo $FOO")));
    }

    @Test
    void executeMultipleAssignmentsFirst() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("A=1 B=2")));
    }

    @Test
    void executeMultipleAssignmentsSecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("A=1 B=2"));
        Assertions.assertEquals("1", executor.getVariable("A"));
    }

    @Test
    void executeMultipleAssignmentsThird() {
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

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
