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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: positional.
 *
 * @since 1.0
 */
final class ExecutorPositionalTest {

    /** TMP_DIR constant. */
    private static final String TMP_DIR = "/tmp";

    /** HELLO constant. */
    private static final String HELLO = "hello";

    @Test
    void positionalParamIndexOutOfRangeIsEmpty() {
        Assertions.assertEquals(
            "",
            new Executor(new HashMap<>(), Arrays.asList("script", "a", "b")).expandVariables(
                "$9",
                new HashMap<>()
            )
        );
    }

    @Test
    void ppidIsSetToParentProcessIdOnInitFirst() {
        Assertions.assertNotNull(new Executor().getVariable("PPID"));
    }

    @Test
    void ppidIsSetToParentProcessIdOnInitSecond() {
        Assertions.assertTrue(
            Long.parseLong(new Executor().getVariable("PPID")) > 0L,
            "PPID should be a positive PID"
        );
    }

    @Test
    void ppidIsSetToParentProcessIdOnInitThird() {
        Assertions.assertEquals(
            ProcessHandle.current().parent().orElse(ProcessHandle.current()).pid(),
            Long.parseLong(new Executor().getVariable("PPID"))
        );
    }

    @Test
    void ppidPreservedAcrossSubshellExecutor() {
        final Executor parent = new Executor();
        Assertions.assertEquals(
            parent.getVariable("PPID"),
            new Executor(parent.environmentMap(), Collections.singletonList("sub")).getVariable(
                "PPID"
            )
        );
    }

    @Test
    void ppidExpandsAsParameter() {
        final Executor executor = new Executor();
        Assertions.assertTrue(
            executor.expandVariables("$PPID", executor.environmentMap()).matches("\\d+")
        );
    }

    @Test
    void pwdPrintsCurrentDirectoryFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorPositionalTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("pwd")));
        stream.flush();
    }

    @Test
    void pwdPrintsCurrentDirectorySecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorPositionalTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("pwd"));
        Assertions.assertEquals(
            ExecutorPositionalTest.TMP_DIR,
            bos.toString(StandardCharsets.UTF_8).trim()
        );
        stream.flush();
    }

    @Test
    void pwdLogicalFlagPrintsPwdEnvFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorPositionalTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("pwd -L")));
        stream.flush();
    }

    @Test
    void pwdLogicalFlagPrintsPwdEnvSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorPositionalTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("pwd -L"));
        Assertions.assertEquals(
            ExecutorPositionalTest.TMP_DIR,
            bos.toString(StandardCharsets.UTF_8).trim()
        );
        stream.flush();
    }

    @Test
    void pwdPhysicalFlagResolvesSymlinksFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorPositionalTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("pwd -P")));
        stream.flush();
    }

    @Test
    void pwdPhysicalFlagResolvesSymlinksSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", ExecutorPositionalTest.TMP_DIR);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("pwd -P"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).startsWith("/"));
        stream.flush();
    }

    @Test
    void pwdPhysicalIoError() {
        final Map<String, String> env = new HashMap<>();
        env.put("PWD", "/nonexistent/path/that/does/not/exist");
        env.put("PATH", "/usr/bin");
        Assertions.assertEquals(1, new Executor(env).execute(this.parse("pwd -P")));
    }

    @Test
    void readWithOneVarFirst() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("hello%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read var")));
    }

    @Test
    void readWithOneVarSecond() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("hello%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read var"));
        Assertions.assertEquals("hello", executor.getVariable("var"));
    }

    @Test
    void readWithMultipleVarsFirst() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read x y z")));
    }

    @Test
    void readWithMultipleVarsSecond() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y z"));
        Assertions.assertEquals("a", executor.getVariable("x"));
    }

    @Test
    void readWithMultipleVarsThird() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y z"));
        Assertions.assertEquals("b", executor.getVariable("y"));
    }

    @Test
    void readWithMultipleVarsFourth() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y z"));
        Assertions.assertEquals("c", executor.getVariable("z"));
    }

    @Test
    void readLastVarGetsRemainingFieldsFirst() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c d%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read x y")));
    }

    @Test
    void readLastVarGetsRemainingFieldsSecond() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c d%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y"));
        Assertions.assertEquals("a", executor.getVariable("x"));
    }

    @Test
    void readLastVarGetsRemainingFieldsThird() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a b c d%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read x y"));
        Assertions.assertEquals("b c d", executor.getVariable("y"));
    }

    @Test
    void readReplyWhenNoVarFirst() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(
                String.format("hello world%n").getBytes(StandardCharsets.UTF_8)
            )
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read")));
    }

    @Test
    void readReplyWhenNoVarSecond() {
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
    void readRawModePreservesBackslashFirst() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a\\tb%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read -r var")));
    }

    @Test
    void readRawModePreservesBackslashSecond() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a\\tb%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read -r var"));
        Assertions.assertEquals("a\\tb", executor.getVariable("var"));
    }

    @Test
    void readBackslashWithoutRawRemovesEscapeFirst() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a\\tb%n").getBytes(StandardCharsets.UTF_8))
        );
        Assertions.assertEquals(0, executor.execute(this.parse("read var")));
    }

    @Test
    void readBackslashWithoutRawRemovesEscapeSecond() {
        final Executor executor = new Executor();
        executor.setInputStream(
            new ByteArrayInputStream(String.format("a\\tb%n").getBytes(StandardCharsets.UTF_8))
        );
        executor.execute(this.parse("read var"));
        Assertions.assertEquals("atb", executor.getVariable("var"));
    }

    @Test
    void redirectTargetExpansionOutputFirst() throws Exception {
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
    void redirectTargetExpansionOutputSecond() throws Exception {
        final File file = new File("/tmp/sh4j_redir_expand_test.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_expand_test.txt");
        new Executor(env).execute(this.parse("echo hello > $TEST_FILE"));
        Assertions.assertTrue(file.exists());
        file.delete();
    }

    @Test
    void redirectTargetExpansionOutputThird() throws Exception {
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
    void redirectTargetExpansionAppendFirst() throws Exception {
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
    void redirectTargetExpansionAppendSecond() throws Exception {
        final File file = new File("/tmp/sh4j_redir_append_expand.txt");
        file.delete();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_FILE", "/tmp/sh4j_redir_append_expand.txt");
        new Executor(env).execute(this.parse("echo hello >> $TEST_FILE"));
        Assertions.assertTrue(file.exists());
        file.delete();
    }

    @Test
    void redirectTargetExpansionAppendThird() throws Exception {
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
    void redirectTargetExpansionInputFirst() throws Exception {
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
    void redirectTargetExpansionInputSecond() throws Exception {
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
    void removeQuotesNoop() {
        Assertions.assertEquals(
            ExecutorPositionalTest.HELLO,
            WordExpander.removeQuotes(ExecutorPositionalTest.HELLO)
        );
    }

    @Test
    void removeQuotesDouble() {
        Assertions.assertEquals("hello world", WordExpander.removeQuotes("\"hello world\""));
    }

    @Test
    void removeQuotesSingle() {
        Assertions.assertEquals(ExecutorPositionalTest.HELLO, WordExpander.removeQuotes("'hello'"));
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
