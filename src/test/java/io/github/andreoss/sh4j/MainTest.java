/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.IntSupplier;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for Main.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnnecessaryLocalRule"})
public final class MainTest {

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void runScriptWithEcho() throws IOException {
        final File script = this.tempdir.newFile("echo.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("echo hello%n"));
        }
        Assert.assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    public void runScriptWithExitZero() throws IOException {
        final File script = this.tempdir.newFile("exit0.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 0%n"));
        }
        Assert.assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    public void runScriptWithExitOne() throws IOException {
        final File script = this.tempdir.newFile("exit1.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 1%n"));
        }
        Assert.assertEquals(1, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    public void runScriptFileNotFound() {
        Assert.assertEquals(1, Main.runScript("/nonexistent/path/to/script.sh"));
    }

    @Test
    public void exitCodeDashCRunsCommand() {
        Assert.assertEquals(7, Main.exitCode(new String[] {"-c", "exit 7"}));
    }

    @Test
    public void exitCodeWithArgsReturnsRunScriptResult() throws IOException {
        final File script = this.tempdir.newFile("test_exit.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 42%n"));
        }
        Assert.assertEquals(42, Main.exitCode(new String[] {script.getAbsolutePath()}));
    }

    @Test
    public void exitCodeNoArgsInteractiveEof() {
        Assert.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[0])
            )
        );
    }

    @Test
    public void exitCodeNoArgsInteractiveEmptyLine() {
        Assert.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(String.format("%n").getBytes(StandardCharsets.UTF_8)),
                () -> Main.exitCode(new String[0])
            )
        );
    }

    @Test
    public void exitCodeNoArgsInteractiveWithCommand() {
        Assert.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(
                    String.format("echo hello%n").getBytes(StandardCharsets.UTF_8)
                ),
                () -> Main.exitCode(new String[0])
            )
        );
    }

    @Test
    public void exitCodeWithMultipleArgsPassesPositionalParams() throws IOException {
        final File script = this.tempdir.newFile("args.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit $#%n"));
        }
        Assert.assertEquals(
            3,
            Main.exitCode(new String[] {script.getAbsolutePath(), "a", "b", "c"})
        );
    }

    @Test
    public void exitCodeWithTwoArgsExitsOnFirstPositional() throws IOException {
        final File script = this.tempdir.newFile("first.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit $1%n"));
        }
        Assert.assertEquals(7, Main.exitCode(new String[] {script.getAbsolutePath(), "7"}));
    }

    @Test
    public void exitCodeNoArgsInteractiveIoException() {
        Assert.assertEquals(
            0,
            MainTest.withStdin(
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException();
                    }
                },
                () -> Main.exitCode(new String[0])
            )
        );
    }

    @Test
    public void exitCodeDashSReadsStdin() {
        Assert.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream("echo streamed\n".getBytes(StandardCharsets.UTF_8)),
                () -> Main.exitCode(new String[] {"-s"})
            )
        );
    }

    @Test
    public void exitCodeDashIWithEof() {
        Assert.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[] {"-i"})
            )
        );
    }

    @Test
    public void exitCodeDashDashRunsScript() throws IOException {
        final File script = this.tempdir.newFile("dashdash.sh");
        Files.write(script.toPath(), "exit 4\n".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(
            4, Main.exitCode(new String[] {"--", script.getAbsolutePath()})
        );
    }

    @Test
    public void exitCodeUnknownOptionSkipsToScript() throws IOException {
        final File script = this.tempdir.newFile("unknown.sh");
        Files.write(script.toPath(), "exit 5\n".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(
            5, Main.exitCode(new String[] {"-x", script.getAbsolutePath()})
        );
    }

    @Test
    public void runSourceParseErrorReturnsTwo() {
        Assert.assertEquals(2, Main.runSource("if", java.util.Collections.singletonList("sh")));
    }

    @Test
    public void runSourceExitStatus() {
        Assert.assertEquals(
            6, Main.runSource("exit 6", java.util.Collections.singletonList("sh"))
        );
    }

    @Test
    public void runStartupFileExecutesScript() throws IOException {
        final File startup = this.tempdir.newFile("env.sh");
        try (BufferedWriter w = Files.newBufferedWriter(startup.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("export STARTUP_LOADED=yes%n"));
        }
        final io.github.andreoss.sh4j.executor.Executor executor =
            new io.github.andreoss.sh4j.executor.Executor();
        Main.runStartupFile(executor, startup.getAbsolutePath());
        Assert.assertEquals("yes", executor.getVariable("STARTUP_LOADED"));
    }

    @Test
    public void runStartupFileNotFoundPrintsError() {
        final io.github.andreoss.sh4j.executor.Executor executor =
            new io.github.andreoss.sh4j.executor.Executor();
        final String pathBefore = executor.getVariable("PWD");
        Main.runStartupFile(executor, "/nonexistent/env.sh");
        Assert.assertEquals(pathBefore, executor.getVariable("PWD"));
    }

    @Test
    public void interactiveWithEnvFileLoadsStartup() throws IOException {
        final File startup = this.tempdir.newFile("env.sh");
        try (BufferedWriter w = Files.newBufferedWriter(startup.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("export FROM_ENV=yes%n"));
        }
        final String saved = System.getenv("ENV");
        try {
            System.setProperty("ENV", startup.getAbsolutePath());
            final io.github.andreoss.sh4j.executor.Executor executor =
                new io.github.andreoss.sh4j.executor.Executor();
            Main.runStartupFile(executor, startup.getAbsolutePath());
            Assert.assertEquals("yes", executor.getVariable("FROM_ENV"));
        } finally {
            if (saved != null) {
                System.setProperty("ENV", saved);
            } else {
                System.clearProperty("ENV");
            }
        }
    }

    @Test
    public void interactiveWithEnvFileExecutesStartup() throws IOException {
        final File startup = this.tempdir.newFile("env.sh");
        try (BufferedWriter w = Files.newBufferedWriter(startup.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("export STARTUP_RAN=yes%n"));
        }
        final int result = MainTest.withStdin(
            new ByteArrayInputStream(new byte[0]),
            () -> Main.interactive(startup.getAbsolutePath())
        );
        Assert.assertEquals(0, result);
    }

    @SuppressWarnings("PMD.CloseResource")
    private static int withStdin(final InputStream input, final IntSupplier action) {
        final InputStream prior = System.in;
        System.setIn(input);
        try {
            return action.getAsInt();
        } finally {
            System.setIn(prior);
        }
    }

    @Test
    public void exitCodeDashCWithPositionalArguments() {
        Assert.assertEquals(
            7, Main.exitCode(new String[] {"-c", "exit $1", "sh", "7"})
        );
    }

    @Test(expected = IllegalStateException.class)
    public void exitCodeDashSReadFailure() {
        MainTest.withStdin(
            new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException();
                }
            },
            () -> Main.exitCode(new String[] {"-s"})
        );
    }

    @Test
    public void executeLineWithIncompleteIf() {
        final io.github.andreoss.sh4j.executor.Executor executor =
            new io.github.andreoss.sh4j.executor.Executor();
        Main.executeLine(executor, "if");
        Assert.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    public void runStartupFileWithParseError() throws IOException {
        final File startup = this.tempdir.newFile("bad-env.sh");
        Files.write(startup.toPath(), "if\n".getBytes(StandardCharsets.UTF_8));
        final io.github.andreoss.sh4j.executor.Executor executor =
            new io.github.andreoss.sh4j.executor.Executor();
        Main.runStartupFile(executor, startup.getAbsolutePath());
        Assert.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    public void isIncompleteWithUnclosedSingleQuote() {
        Assert.assertTrue(Main.isIncomplete("echo 'hello"));
    }

    @Test
    public void isIncompleteWithUnclosedDoubleQuote() {
        Assert.assertTrue(Main.isIncomplete("echo \"hello"));
    }

    @Test
    public void isIncompleteWithClosedSingleQuote() {
        Assert.assertFalse(Main.isIncomplete("echo 'hello'"));
    }

    @Test
    public void isIncompleteWithClosedDoubleQuote() {
        Assert.assertFalse(Main.isIncomplete("echo \"hello\""));
    }

    @Test
    public void isIncompleteWithEscapedBackslash() {
        Assert.assertFalse(Main.isIncomplete("echo hello\\\\"));
    }

    @Test
    public void isIncompleteWithTrailingBackslash() {
        Assert.assertTrue(Main.isIncomplete("echo hello\\"));
    }

    @Test
    public void isIncompleteWithNestedSingleInDouble() {
        Assert.assertFalse(Main.isIncomplete("echo \"it's fine\""));
    }

    @Test
    public void isIncompleteWithNestedDoubleInSingle() {
        Assert.assertFalse(Main.isIncomplete("echo 'he said \"hi\"'"));
    }

    @Test
    public void executeLineWithParseError() {
        final io.github.andreoss.sh4j.executor.Executor executor =
            new io.github.andreoss.sh4j.executor.Executor();
        Main.executeLine(executor, "echo 'unclosed");
        Assert.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    public void executeLineWithValidCommand() {
        final io.github.andreoss.sh4j.executor.Executor executor =
            new io.github.andreoss.sh4j.executor.Executor();
        Main.executeLine(executor, "export PS1_TEST=yes");
        Assert.assertEquals("yes", executor.getVariable("PS1_TEST"));
    }

    @Test
    public void interactiveWithMultiLineInput() throws IOException {
        final String input = String.format("echo 'hello%nworld'%n");
        Assert.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                () -> Main.interactive(null)
            )
        );
    }

    @Test
    public void interactiveWithEmptyEnvFile() throws IOException {
        Assert.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.interactive("")
            )
        );
    }

    @Test
    public void executeLineWithLexerError() {
        final io.github.andreoss.sh4j.executor.Executor executor =
            new io.github.andreoss.sh4j.executor.Executor();
        Main.executeLine(executor, "`unclosed backtick");
        Assert.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    public void interactiveWithContinuationPrompt() throws IOException {
        final String input = String.format("echo '%n'%n");
        Assert.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                () -> Main.interactive(null)
            )
        );
    }
}
