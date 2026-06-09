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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: expand var.
 *
 * @since 1.0
 */
final class ExecutorExpandVarTest {

    /** SCRIPT_KEY constant. */
    private static final String SCRIPT_KEY = "script";

    /** HELLO constant. */
    private static final String HELLO = "hello";

    /** VALUE constant. */
    private static final String VALUE = "value";

    /** HELLO_WORLD constant. */
    private static final String HELLO_WORLD = "hello world";

    @Test
    void expandVarPatternNoMatch() {
        final Map<String, String> env = new HashMap<>();
        env.put("var", ExecutorExpandVarTest.HELLO);
        Assertions.assertEquals(
            ExecutorExpandVarTest.HELLO,
            new Executor().expandVariables("${var%x}", env)
        );
    }

    @Test
    void expandBracedMultiDigitPositionalOutOfRange() {
        Assertions.assertEquals(
            "",
            new Executor(
                new HashMap<>(),
                Arrays.asList(ExecutorExpandVarTest.SCRIPT_KEY, "a", "b", "c")
            ).expandVariables("${10}", new HashMap<>())
        );
    }

    @Test
    void expandBracedMultiDigitPositionalInRange() {
        Assertions.assertEquals(
            "b",
            new Executor(
                new HashMap<>(),
                Arrays.asList(ExecutorExpandVarTest.SCRIPT_KEY, "a", "b", "c")
            ).expandVariables("${2}", new HashMap<>())
        );
    }

    @Test
    void expandBracedLeadingHashVar() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", ExecutorExpandVarTest.HELLO);
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
    void expandBracedColonEqWhenSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "set");
        Assertions.assertEquals("set", new Executor().expandVariables("${x:=default}", env));
    }

    @Test
    void expandBracedQuestionWhenSet() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", ExecutorExpandVarTest.VALUE);
        Assertions.assertEquals(
            ExecutorExpandVarTest.VALUE,
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
        env.put("x", ExecutorExpandVarTest.VALUE);
        Assertions.assertEquals("${x@weird}", new Executor().expandVariables("${x@weird}", env));
    }

    @Test
    void expandBracedColonQuestionUnset() {
        Assertions.assertEquals(
            1,
            ExecutorExpandVarTest.exitCode(
                () -> new Executor(new HashMap<>()).expandVariables(
                    "${x:?my error}",
                    new HashMap<>()
                )
            )
        );
    }

    @Test
    void expandBracedQuestionUnset() {
        Assertions.assertEquals(
            1,
            ExecutorExpandVarTest.exitCode(
                () -> new Executor(new HashMap<>()).expandVariables(
                    "${x?my error}",
                    new HashMap<>()
                )
            )
        );
    }

    @Test
    void expandBracedColonQuestionSet() {
        Assertions.assertEquals(
            ExecutorExpandVarTest.VALUE,
            new Executor(new HashMap<>()).expandVariables(
                "${x:?not used}",
                new HashMap<>(Map.of("x", ExecutorExpandVarTest.VALUE))
            )
        );
    }

    @Test
    void expandBracedColonQuestionDefaultMsg() {
        Assertions.assertEquals(
            1,
            ExecutorExpandVarTest.exitCode(
                () -> new Executor(new HashMap<>()).expandVariables("${x:?}", new HashMap<>())
            )
        );
    }

    @Test
    void expandBracedColonQuestionErrorStreamFirst() {
        final Executor executor = new Executor(new HashMap<>());
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setErrorStream(stream);
        Assertions.assertEquals(
            1,
            ExecutorExpandVarTest.exitCode(
                () -> executor.expandVariables("${x:?my error}", new HashMap<>())
            )
        );
        stream.flush();
    }

    @Test
    void expandBracedColonQuestionErrorStreamSecond() {
        final Executor executor = new Executor(new HashMap<>());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setErrorStream(stream);
        ExecutorExpandVarTest.exitCode(
            () -> executor.expandVariables("${x:?my error}", new HashMap<>())
        );
        Assertions.assertTrue(
            bos.toString(StandardCharsets.UTF_8).trim().contains("my error"),
            "error message should go to injected stream"
        );
        stream.flush();
    }

    @Test
    void exportNoArgsFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_EXPORT_VAR", ExecutorExpandVarTest.HELLO);
        final Executor executor = new Executor(env);
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("export")));
        stream.flush();
    }

    @Test
    void exportNoArgsSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_EXPORT_VAR", ExecutorExpandVarTest.HELLO);
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
    void exportArgWithEqualsNonStandardNameFails() throws Exception {
        Assertions.assertEquals(
            2,
            new Executor(new HashMap<>()).execute(this.parse("export 1foo=bar"))
        );
    }

    @Test
    void exportArgWithEqualsNonStandardNameIsNotSet() throws Exception {
        final Executor executor = new Executor(new HashMap<>());
        executor.execute(this.parse("export 1foo=bar"));
        Assertions.assertNull(executor.getVariable("1foo"));
    }

    @Test
    void exportArgWithEqualsFirst() {
        Assertions.assertEquals(
            0,
            new Executor(new HashMap<>()).execute(this.parse("export FOO=hello"))
        );
    }

    @Test
    void exportArgWithEqualsSecond() {
        final Executor executor = new Executor(new HashMap<>());
        executor.execute(this.parse("export FOO=hello"));
        Assertions.assertEquals(ExecutorExpandVarTest.HELLO, executor.getVariable("FOO"));
    }

    @Test
    void exportWithValueFirst() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("export FOO=bar")));
    }

    @Test
    void exportWithValueSecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("export FOO=bar"));
        Assertions.assertEquals("bar", executor.getVariable("FOO"));
    }

    @Test
    void exportExistingVariable() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("export FOO")));
    }

    @Test
    void exportThenVariableExpandsFirst() {
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse("export MSG=hello; echo $MSG"))
        );
    }

    @Test
    void exportThenVariableExpandsSecond() {
        final Executor executor = new Executor();
        executor.execute(this.parse("export MSG=hello; echo $MSG"));
        Assertions.assertEquals(ExecutorExpandVarTest.HELLO, executor.getVariable("MSG"));
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
    void externalOutputRedirectFirst() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorExpandVarTest.HELLO_WORLD);
        }
        Assertions.assertEquals(
            0,
            new Executor().execute(this.parse(String.format("cat < %s > %s", inpath, outpath)))
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalOutputRedirectSecond() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorExpandVarTest.HELLO_WORLD);
        }
        new Executor().execute(this.parse(String.format("cat < %s > %s", inpath, outpath)));
        Assertions.assertTrue(new File(outpath).exists());
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalOutputRedirectThird() throws Exception {
        final String inpath = "/tmp/sh4j_ext_in.txt";
        final String outpath = "/tmp/sh4j_ext_out.txt";
        new File(outpath).delete();
        try (
            BufferedWriter w = Files.newBufferedWriter(
                Paths.get(inpath),
                StandardCharsets.UTF_8
            )
        ) {
            w.write(ExecutorExpandVarTest.HELLO_WORLD);
        }
        new Executor().execute(this.parse(String.format("cat < %s > %s", inpath, outpath)));
        new File(outpath).exists();
        Assertions.assertEquals(
            ExecutorExpandVarTest.HELLO_WORLD,
            new String(Files.readAllBytes(Paths.get(outpath)), StandardCharsets.UTF_8).trim()
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalAppendRedirectFirst() throws Exception {
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
    void externalAppendRedirectSecond() throws Exception {
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
        Assertions.assertTrue(
            new String(
                Files.readAllBytes(Paths.get(outpath)),
                StandardCharsets.UTF_8
            ).trim().startsWith("first")
        );
        new File(inpath).delete();
        new File(outpath).delete();
    }

    @Test
    void externalAppendRedirectThird() throws Exception {
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
        Assertions.assertTrue(
            new String(
                Files.readAllBytes(Paths.get(outpath)),
                StandardCharsets.UTF_8
            ).trim().contains("second")
        );
        new File(inpath).delete();
        new File(outpath).delete();
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
