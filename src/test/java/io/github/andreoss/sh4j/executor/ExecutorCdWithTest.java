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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: cd with.
 *
 * @since 1.0
 */
final class ExecutorCdWithTest {

    /** PATH_KEY constant. */
    private static final String PATH_KEY = "PATH";

    /** IFS_TAB_NL constant. */
    private static final String IFS_TAB_NL = " \t%n";

    /** TMP_DIR constant. */
    private static final String TMP_DIR = "/tmp";

    /** HELLO constant. */
    private static final String HELLO = "hello";

    /** HOME_KEY constant. */
    private static final String HOME_KEY = "HOME";

    @Test
    void cdWithExpandedVarFirst() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put("MYDIR", ExecutorCdWithTest.TMP_DIR);
        Assertions.assertEquals(0, new Executor(env).execute(this.parse("cd $MYDIR")));
    }

    @Test
    void cdWithExpandedVarSecond() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put("MYDIR", ExecutorCdWithTest.TMP_DIR);
        final Executor executor = new Executor(env);
        executor.execute(this.parse("cd $MYDIR"));
        Assertions.assertEquals(ExecutorCdWithTest.TMP_DIR, executor.getVariable("PWD"));
    }

    @Test
    void cdDashWithoutOldPwdReturnsOne() {
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", "/usr/bin");
        env.put("HOME", "/tmp");
        Assertions.assertEquals(1, new Executor(env).execute(this.parse("cd -")));
    }

    @Test
    void colonBuiltinReturnsZero() {
        Assertions.assertEquals(0, new Executor().execute(this.parse(":")));
    }

    @Test
    void commandInterruptedException() {
        Thread.currentThread().interrupt();
        Assertions.assertEquals(1, new Executor().execute(this.parse("ls")));
    }

    @Test
    void commandSubstitutionEcho() {
        Assertions.assertEquals(
            ExecutorCdWithTest.HELLO,
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
    void commandFoundInPath() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true")));
    }

    @Test
    void commandNotFoundReturnsNotFound() {
        Assertions.assertEquals(127, new Executor().execute(this.parse("nonexistent_cmd_xyz_127")));
    }

    @Test
    void commandWithSlashFound() {
        Assertions.assertEquals(
            0,
            new Executor(new HashMap<>()).execute(this.parse("/usr/bin/env true"))
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
        env.put(ExecutorCdWithTest.PATH_KEY, dir.getAbsolutePath());
        dir.delete();
        Assertions.assertEquals(42, new Executor(env).execute(this.parse("mycmd")));
    }

    @Test
    void commandNotFoundEmptyPath() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorCdWithTest.PATH_KEY, "");
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
            ExecutorCdWithTest.PATH_KEY,
            String.format("/nonexistent_test_dir_xyz:%s", dir.getAbsolutePath())
        );
        dir.delete();
        Assertions.assertEquals(7, new Executor(env).execute(this.parse("mycmd2")));
    }

    @Test
    void commandWithSlashNotFound() {
        Assertions.assertEquals(
            127,
            new Executor().execute(this.parse("/nonexistent_path_xyz_abc/cmd"))
        );
    }

    @Test
    void commandDashVOnBuiltinPrintsNameFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("command -v echo")));
        stream.flush();
    }

    @Test
    void commandDashVOnBuiltinPrintsNameSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("command -v echo"));
        Assertions.assertEquals(
            ExecutorCdWithTest.HELLO.replace(ExecutorCdWithTest.HELLO, "echo"),
            bos.toString(StandardCharsets.UTF_8).trim()
        );
        stream.flush();
    }

    @Test
    void commandDashUppervOnBuiltinDescribesFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("command -V true")));
        stream.flush();
    }

    @Test
    void commandDashUppervOnBuiltinDescribesSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("command -V true"));
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("shell built-in"));
        stream.flush();
    }

    @Test
    void commandWithoutFlagsRunsCommandFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        Assertions.assertEquals(0, executor.execute(this.parse("command echo hi")));
        stream.flush();
    }

    @Test
    void commandWithoutFlagsRunsCommandSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        final PrintSink stream = new PrintSink(bos, StandardCharsets.UTF_8);
        executor.setOutputStream(stream);
        executor.execute(this.parse("command echo hi"));
        Assertions.assertEquals("hi", bos.toString(StandardCharsets.UTF_8).trim());
        stream.flush();
    }

    @Test
    void commandDashVOnMissingReturnsOne() {
        Assertions.assertEquals(
            1,
            new Executor().execute(this.parse("command -v sh4j_no_such_cmd_xyz"))
        );
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
    void continueInForLoopFirst() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        Assertions.assertEquals(
            0,
            executor.execute(this.parse("for i in a b; do echo $i; continue; echo after; done"))
        );
    }

    @Test
    void continueInForLoopSecond() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("for i in a b; do echo $i; continue; echo after; done"));
        Assertions.assertEquals(String.format("a%nb"), bos.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void continueInForLoopThird() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Executor executor = new Executor();
        executor.setCaptureStream(bos);
        executor.execute(this.parse("for i in a b; do echo $i; continue; echo after; done"));
        Assertions.assertFalse(bos.toString(StandardCharsets.UTF_8).contains("after"));
    }

    @Test
    void createProcessBuilderWithQuotedArg() {
        Assertions.assertEquals(0, new Executor().execute(this.parse("true \"quoted arg\"")));
    }

    @Test
    void defaultIfsSet() {
        Assertions.assertEquals(
            String.format(ExecutorCdWithTest.IFS_TAB_NL),
            new Executor().getVariable("IFS")
        );
    }

    @Test
    void defaultHomeSet() {
        Assertions.assertEquals(
            System.getProperty("user.home"),
            new Executor().getVariable(ExecutorCdWithTest.HOME_KEY)
        );
    }

    @Test
    void defaultPathSet() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorCdWithTest.PATH_KEY, "");
        Assertions.assertEquals(
            "/usr/local/bin:/usr/bin:/bin",
            new Executor(env).getVariable(ExecutorCdWithTest.PATH_KEY)
        );
    }

    @Test
    void defaultsDoNotOverrideExistingEnvFirst() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorCdWithTest.HOME_KEY, "/custom/home");
        env.put(ExecutorCdWithTest.PATH_KEY, "/custom/path");
        Assertions.assertEquals(
            "/custom/home",
            new Executor(env).getVariable(ExecutorCdWithTest.HOME_KEY)
        );
    }

    @Test
    void defaultsDoNotOverrideExistingEnvSecond() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorCdWithTest.HOME_KEY, "/custom/home");
        env.put(ExecutorCdWithTest.PATH_KEY, "/custom/path");
        Assertions.assertEquals(
            "/custom/path",
            new Executor(env).getVariable(ExecutorCdWithTest.PATH_KEY)
        );
    }

    @Test
    void defaultsDoNotOverrideExistingEnvThird() {
        final Map<String, String> env = new HashMap<>();
        env.put(ExecutorCdWithTest.HOME_KEY, "/custom/home");
        env.put(ExecutorCdWithTest.PATH_KEY, "/custom/path");
        Assertions.assertEquals(
            String.format(ExecutorCdWithTest.IFS_TAB_NL),
            new Executor(env).getVariable("IFS")
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
    void echoQuotedVarNoSplit() {
        Assertions.assertEquals(
            0,
            new Executor(new HashMap<>()).execute(
                this.parse("MSG=\"hello  world\"; echo \"$MSG\"")
            )
        );
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
