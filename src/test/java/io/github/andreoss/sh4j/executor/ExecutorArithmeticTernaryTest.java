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
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: arithmetic ternary.
 *
 * @since 1.0
 */
final class ExecutorArithmeticTernaryTest {

    /** TMP_DIR constant. */
    private static final String TMP_DIR = "/tmp";

    /** HOME_KEY constant. */
    private static final String HOME_KEY = "HOME";

    /** PWD_KEY constant. */
    private static final String PWD_KEY = "PWD";

    @Test
    void arithmeticTernaryShortCircuitFourth() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 ? (a = 1) : (b = 2)))", env);
        executor.expandVariables("$((0 ? (c = 3) : (d = 4)))", env);
        Assertions.assertEquals("4", env.get("d"));
    }

    @Test
    void arithmeticAssignDivByZeroFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "10");
        Assertions.assertEquals("0", new Executor().expandVariables("$((x /= 0))", env));
    }

    @Test
    void arithmeticAssignDivByZeroSecond() {
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
    void arithmeticAssignNonNumericEnvTreatedAsZeroFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        Assertions.assertEquals("5", new Executor().expandVariables("$((x += 5))", env));
    }

    @Test
    void arithmeticAssignNonNumericEnvTreatedAsZeroSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        new Executor().expandVariables("$((x += 5))", env);
        Assertions.assertEquals("5", env.get("x"));
    }

    @Test
    void arithmeticAssignWithUnderscoreNameFirst() {
        Assertions.assertEquals(
            "42",
            new Executor().expandVariables("$((_foo = 42))", new HashMap<>())
        );
    }

    @Test
    void arithmeticAssignWithUnderscoreNameSecond() {
        final Map<String, String> env = new HashMap<>();
        new Executor().expandVariables("$((_foo = 42))", env);
        Assertions.assertEquals("42", env.get("_foo"));
    }

    @Test
    void arithmeticFactorNonNumericEnvAsZero() {
        final Map<String, String> env = new HashMap<>();
        env.put("x", "abc");
        Assertions.assertEquals("1", new Executor().expandVariables("$((x + 1))", env));
    }

    @Test
    void arithmeticPrecedenceMixedFirst() {
        Assertions.assertEquals(
            "1",
            new Executor().expandVariables("$((1 + 2 == 3))", new HashMap<>())
        );
    }

    @Test
    void arithmeticPrecedenceMixedSecond() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 + 2 == 3))", env);
        Assertions.assertEquals("1", executor.expandVariables("$((1 < 2 && 3 < 4))", env));
    }

    @Test
    void arithmeticPrecedenceMixedThird() {
        final Executor executor = new Executor();
        final Map<String, String> env = new HashMap<>();
        executor.expandVariables("$((1 + 2 == 3))", env);
        executor.expandVariables("$((1 < 2 && 3 < 4))", env);
        Assertions.assertEquals("7", executor.expandVariables("$((1 | 2 | 4 & 6))", env));
    }

    @Test
    void backgroundPipelineWithPipeFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 | true & jobs")));
        stream.flush();
    }

    @Test
    void backgroundPipelineWithPipeSecond() {
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

    @Test
    void backtickSubstitution() {
        Assertions.assertEquals(
            "hello",
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
    void backtickNestedNormalise() {
        Assertions.assertEquals(
            "$(echo `echo hi`)",
            WordExpander.normaliseBackticks("`echo \\`echo hi\\``")
        );
    }

    @Test
    void bgConfirmsBackgroundJobFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & bg")));
        stream.flush();
    }

    @Test
    void bgConfirmsBackgroundJobSecond() {
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
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(1, executor.execute(this.parse("bg")));
        stream.flush();
    }

    @Test
    void bgWithPercentIdFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("sleep 0 & bg %1")));
        stream.flush();
    }

    @Test
    void bgWithPercentIdSecond() {
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
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(1, executor.execute(this.parse("sleep 0 & bg 99999")));
        stream.flush();
    }

    @Test
    void bgWithInvalidJobSyntaxReturnsError() {
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(1, executor.execute(this.parse("sleep 0 & bg notanumber")));
        stream.flush();
    }

    @Test
    void bgResumesStoppedJobFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        final JobManager mgr = executor.jobManager();
        final int id = mgr.submit(
            () -> {
                try {
                    Thread.sleep(10_000);
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            },
            "test-cmd"
        );
        mgr.getJob(id).cancel();
        Assertions.assertEquals(0, executor.execute(this.parse(String.format("bg %d", id))));
        stream.flush();
    }

    @Test
    void bgResumesStoppedJobSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        final JobManager mgr = executor.jobManager();
        final int id = mgr.submit(
            () -> {
                try {
                    Thread.sleep(10_000);
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
    void breakStopsWhileLoopFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("while true; do echo ran; break; done"))
        );
    }

    @Test
    void breakStopsWhileLoopSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("while true; do echo ran; break; done"));
        Assertions.assertEquals("ran", bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void breakStopsForLoopFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("for i in a b c; do echo $i; break; done"))
        );
    }

    @Test
    void breakStopsForLoopSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("for i in a b c; do echo $i; break; done"));
        Assertions.assertEquals("a", bos.toString(StandardCharsets.UTF_8).trim());
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
    void cdToDirectoryFirst() throws Exception {
        Assertions.assertEquals(0, new Executor().execute(this.parse("cd /tmp")));
    }

    @Test
    void cdToDirectorySecond() throws Exception {
        final Executor executor = new Executor();
        executor.execute(this.parse("cd /tmp"));
        Assertions.assertEquals(
            ExecutorArithmeticTernaryTest.TMP_DIR,
            executor.getVariable(ExecutorArithmeticTernaryTest.PWD_KEY)
        );
    }

    @Test
    void cdToHomeFirst() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorArithmeticTernaryTest.HOME_KEY, ExecutorArithmeticTernaryTest.TMP_DIR);
        Assertions.assertEquals(0, new Executor(env).execute(this.parse("cd")));
    }

    @Test
    void cdToHomeSecond() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorArithmeticTernaryTest.HOME_KEY, ExecutorArithmeticTernaryTest.TMP_DIR);
        final Executor executor = new Executor(env);
        executor.execute(this.parse("cd"));
        Assertions.assertEquals(
            ExecutorArithmeticTernaryTest.TMP_DIR,
            executor.getVariable(ExecutorArithmeticTernaryTest.PWD_KEY)
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
    void cdMinusFirst() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorArithmeticTernaryTest.PWD_KEY, ExecutorArithmeticTernaryTest.TMP_DIR);
        env.put("OLDPWD", "/");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(new ByteArrayOutputStream());
        Assertions.assertEquals(0, executor.execute(this.parse("cd -")));
    }

    @Test
    void cdMinusSecond() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorArithmeticTernaryTest.PWD_KEY, ExecutorArithmeticTernaryTest.TMP_DIR);
        env.put("OLDPWD", "/");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(new ByteArrayOutputStream());
        executor.execute(this.parse("cd -"));
        Assertions.assertEquals("/", executor.getVariable(ExecutorArithmeticTernaryTest.PWD_KEY));
    }

    @Test
    void cdMinusThird() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorArithmeticTernaryTest.PWD_KEY, ExecutorArithmeticTernaryTest.TMP_DIR);
        env.put("OLDPWD", "/");
        final Executor executor = new Executor(env);
        executor.setCaptureStream(new ByteArrayOutputStream());
        executor.execute(this.parse("cd -"));
        Assertions.assertEquals(
            ExecutorArithmeticTernaryTest.TMP_DIR,
            executor.getVariable("OLDPWD")
        );
    }

    @Test
    void cdMinusFourth() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorArithmeticTernaryTest.PWD_KEY, ExecutorArithmeticTernaryTest.TMP_DIR);
        env.put("OLDPWD", "/");
        final Executor executor = new Executor(env);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("cd -"));
        Assertions.assertEquals("/", bos.toString(StandardCharsets.UTF_8).trim());
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
