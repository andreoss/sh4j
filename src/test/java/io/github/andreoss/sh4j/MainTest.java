/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.executor.Executor;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for Main.
 *
 * @since 1.0
 */
final class MainTest {
    @TempDir
    private static Path root;

    @Test
    void exitCodeNoArgsInteractiveIoException() {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException();
                    }

                    @Override
                    public int read(final byte[] buf, final int off, final int len)
                        throws IOException {
                        throw new IOException();
                    }
                },
                () -> Main.exitCode(new String[0])
            )
        );
    }

    @Test
    void exitCodeDashSReadFailure() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> MainTest.withStdin(
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException();
                    }

                    @Override
                    public int read(final byte[] buf, final int off, final int len)
                        throws IOException {
                        throw new IOException();
                    }
                },
                () -> Main.exitCode(new String[] {"-s"})
            )
        );
    }

    @Test
    void runScriptWithEcho() throws IOException {
        final File script = TempDirs.file(MainTest.root, "echo.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("echo hello%n"));
        }
        Assertions.assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptWithExitZero() throws IOException {
        final File script = TempDirs.file(MainTest.root, "exit0.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 0%n"));
        }
        Assertions.assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptWithExitOne() throws IOException {
        final File script = TempDirs.file(MainTest.root, "exit1.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 1%n"));
        }
        Assertions.assertEquals(1, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptFileNotFound() {
        Assertions.assertEquals(1, Main.runScript("/nonexistent/path/to/script.sh"));
    }

    @Test
    void exitCodeDashCRunsCommand() {
        Assertions.assertEquals(7, Main.exitCode(new String[] {"-c", "exit 7"}));
    }

    @Test
    void exitCodeWithArgsReturnsRunScriptResult() throws IOException {
        final File script = TempDirs.file(MainTest.root, "test_exit.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 42%n"));
        }
        Assertions.assertEquals(42, Main.exitCode(new String[] {script.getAbsolutePath()}));
    }

    @Test
    void exitCodeNoArgsInteractiveEof() {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[0])
            )
        );
    }

    @Test
    void exitCodeNoArgsInteractiveEmptyLine() {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                MainTest.input("%n"),
                () -> Main.exitCode(new String[0])
            )
        );
    }

    @Test
    void exitCodeNoArgsInteractiveWithCommand() {
        Assertions.assertEquals(
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
    void exitCodeWithMultipleArgsPassesPositionalParams() throws IOException {
        final File script = TempDirs.file(MainTest.root, "args.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit $#%n"));
        }
        Assertions.assertEquals(
            3,
            Main.exitCode(new String[] {script.getAbsolutePath(), "a", "b", "c"})
        );
    }

    @Test
    void exitCodeWithTwoArgsExitsOnFirstPositional() throws IOException {
        final File script = TempDirs.file(MainTest.root, "first.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit $1%n"));
        }
        Assertions.assertEquals(7, Main.exitCode(new String[] {script.getAbsolutePath(), "7"}));
    }

    @Test
    void exitCodeDashSReadsStdin() {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(
                    String.format("echo streamed%n").getBytes(StandardCharsets.UTF_8)
                ),
                () -> Main.exitCode(new String[] {"-s"})
            )
        );
    }

    @Test
    void exitCodeDashIWithEof() {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[] {"-i"})
            )
        );
    }

    @Test
    void exitCodeDashDashRunsScript() throws IOException {
        final File script = TempDirs.file(MainTest.root, "dashdash.sh");
        Files.write(script.toPath(), String.format("exit 4%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            4, Main.exitCode(new String[] {"--", script.getAbsolutePath()})
        );
    }

    @Test
    void exitCodeUnknownOptionSkipsToScript() throws IOException {
        final File script = TempDirs.file(MainTest.root, "unknown.sh");
        Files.write(script.toPath(), String.format("exit 5%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(
            5, Main.exitCode(new String[] {"-x", script.getAbsolutePath()})
        );
    }

    @Test
    void runSourceParseErrorReturnsTwo() {
        Assertions.assertEquals(2, Main.runSource("if", Collections.singletonList("sh")));
    }

    @Test
    void runSourceExitStatus() {
        Assertions.assertEquals(
            6, Main.runSource("exit 6", Collections.singletonList("sh"))
        );
    }

    @Test
    void runStartupFileExecutesScript() throws IOException {
        final File startup = TempDirs.file(MainTest.root, "env.sh");
        try (BufferedWriter w = Files.newBufferedWriter(startup.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("export STARTUP_LOADED=yes%n"));
        }
        final Executor executor = new Executor();
        Main.runStartupFile(executor, startup.getAbsolutePath());
        Assertions.assertEquals("yes", executor.getVariable("STARTUP_LOADED"));
    }

    @Test
    void runStartupFileNotFoundPrintsError() {
        final Executor executor = new Executor();
        Main.runStartupFile(executor, "/nonexistent/env.sh");
        Assertions.assertEquals(executor.getVariable("PWD"), executor.getVariable("PWD"));
    }

    @Test
    void interactiveWithEnvFileLoadsStartup() throws IOException {
        final File startup = TempDirs.file(MainTest.root, "env.sh");
        try (BufferedWriter w = Files.newBufferedWriter(startup.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("export FROM_ENV=yes%n"));
        }
        final String saved = System.getenv("ENV");
        try {
            System.setProperty("ENV", startup.getAbsolutePath());
            final Executor executor = new Executor();
            Main.runStartupFile(executor, startup.getAbsolutePath());
            Assertions.assertEquals("yes", executor.getVariable("FROM_ENV"));
        } finally {
            if (saved != null) {
                System.setProperty("ENV", saved);
            } else {
                System.clearProperty("ENV");
            }
        }
    }

    @Test
    void interactiveWithEnvFileExecutesStartup() throws IOException {
        final File startup = TempDirs.file(MainTest.root, "env.sh");
        try (BufferedWriter w = Files.newBufferedWriter(startup.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("export STARTUP_RAN=yes%n"));
        }
        final int result = MainTest.withStdin(
            new ByteArrayInputStream(new byte[0]),
            () -> Main.interactive(startup.getAbsolutePath())
        );
        Assertions.assertEquals(0, result);
    }

    @Test
    void exitCodeDashCMissingArgument() {
        Assertions.assertEquals(
            1,
            MainTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[] {"-c"})
            )
        );
    }

    @Test
    void exitCodeDashSEmptyStdin() {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[] {"-s"})
            )
        );
    }

    @Test
    void runSourceRunsExitTrap() throws IOException {
        final File marker = TempDirs.file(MainTest.root, "trap-marker.txt");
        Assertions.assertEquals(
            0,
            Main.runSource(
                String.format(
                    "trap 'echo trapped > %s' EXIT; echo body",
                    marker.getAbsolutePath()
                ),
                Collections.singletonList("sh")
            )
        );
        Assertions.assertEquals(
            String.format("trapped%s", System.lineSeparator()),
            new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void runSourcePropagatesExitAndRunsTrap() throws IOException {
        final File marker = TempDirs.file(MainTest.root, "exit-trap-marker.txt");
        Assertions.assertEquals(
            3,
            Main.runSource(
                String.format(
                    "trap 'echo trapped > %s' EXIT; exit 3",
                    marker.getAbsolutePath()
                ),
                Collections.singletonList("sh")
            )
        );
        Assertions.assertTrue(marker.isFile());
    }

    @Test
    void interactiveIncompleteQuoteAtEof() {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream("echo 'x".getBytes(StandardCharsets.UTF_8)),
                () -> Main.interactive(null)
            )
        );
    }

    @Test
    void interactiveBlankLinesAndParseError() {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                MainTest.input("%n%nif%necho done%n"),
                () -> Main.interactive(null)
            )
        );
    }

    @Test
    void interactiveCustomPromptsAndContinuation() throws IOException {
        final File startup = TempDirs.file(MainTest.root, "prompt-env.sh");
        Files.write(
            startup.toPath(),
            String.format("PS1='p1> '%nPS2='p2> '%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                MainTest.input("echo 'a%nb'%n"),
                () -> Main.interactive(startup.getAbsolutePath())
            )
        );
    }

    @Test
    void interactiveCapturedOutputAndPrompts() throws IOException {
        final File startup = TempDirs.file(MainTest.root, "capture-env.sh");
        Files.write(
            startup.toPath(),
            String.format("PS1='one> '%nPS2='two> '%n").getBytes(StandardCharsets.UTF_8)
        );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        Assertions.assertEquals(
            0,
            MainTest.withStreams(
                MainTest.input("echo hello%necho 'a%nb'%n%nif%n"),
                out,
                err,
                () -> Main.interactive(startup.getAbsolutePath())
            )
        );
        final String text = out.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(text.contains("one> "));
        Assertions.assertTrue(text.contains("two> "));
        Assertions.assertTrue(text.contains("hello"));
        Assertions.assertTrue(text.contains("a"));
        Assertions.assertTrue(err.toString(StandardCharsets.UTF_8).contains("syntax error"));
    }

    @Test
    void interactiveBlankAndIncompleteDoubleQuoteAtEof() {
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        Assertions.assertEquals(
            0,
            MainTest.withStreams(
                MainTest.input("%n%n%necho \"x"),
                new ByteArrayOutputStream(),
                err,
                () -> Main.interactive(null)
            )
        );
    }

    @Test
    void exitCodeDashCWithPositionalArguments() {
        Assertions.assertEquals(
            7, Main.exitCode(new String[] {"-c", "exit $1", "sh", "7"})
        );
    }

    @Test
    void executeLineWithIncompleteIf() {
        final Executor executor = new Executor();
        Main.executeLine(executor, "if");
        Assertions.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    void runStartupFileWithParseError() throws IOException {
        final File startup = TempDirs.file(MainTest.root, "bad-env.sh");
        Files.write(startup.toPath(), String.format("if%n").getBytes(StandardCharsets.UTF_8));
        final Executor executor = new Executor();
        Main.runStartupFile(executor, startup.getAbsolutePath());
        Assertions.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    void isIncompleteWithUnclosedSingleQuote() {
        Assertions.assertTrue(Main.isIncomplete("echo 'hello"));
    }

    @Test
    void isIncompleteWithUnclosedDoubleQuote() {
        Assertions.assertTrue(Main.isIncomplete("echo \"hello"));
    }

    @Test
    void isIncompleteWithClosedSingleQuote() {
        Assertions.assertFalse(Main.isIncomplete("echo 'hello'"));
    }

    @Test
    void isIncompleteWithClosedDoubleQuote() {
        Assertions.assertFalse(Main.isIncomplete("echo \"hello\""));
    }

    @Test
    void isIncompleteWithEscapedBackslash() {
        Assertions.assertFalse(Main.isIncomplete("echo hello\\\\"));
    }

    @Test
    void isIncompleteWithTrailingBackslash() {
        Assertions.assertTrue(Main.isIncomplete("echo hello\\"));
    }

    @Test
    void isIncompleteWithNestedSingleInDouble() {
        Assertions.assertFalse(Main.isIncomplete("echo \"it's fine\""));
    }

    @Test
    void isIncompleteWithNestedDoubleInSingle() {
        Assertions.assertFalse(Main.isIncomplete("echo 'he said \"hi\"'"));
    }

    @Test
    void executeLineWithParseError() {
        final Executor executor = new Executor();
        Main.executeLine(executor, "echo 'unclosed");
        Assertions.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    void executeLineWithValidCommand() {
        final Executor executor = new Executor();
        Main.executeLine(executor, "export PS1_TEST=yes");
        Assertions.assertEquals("yes", executor.getVariable("PS1_TEST"));
    }

    @Test
    void interactiveWithMultiLineInput() throws IOException {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                MainTest.input("echo 'hello%nworld'%n"),
                () -> Main.interactive(null)
            )
        );
    }

    @Test
    void interactiveWithEmptyEnvFile() throws IOException {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.interactive("")
            )
        );
    }

    @Test
    void executeLineWithLexerError() {
        final Executor executor = new Executor();
        Main.executeLine(executor, "`unclosed backtick");
        Assertions.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    void interactiveWithContinuationPrompt() throws IOException {
        Assertions.assertEquals(
            0,
            MainTest.withStdin(
                MainTest.input("echo '%n'%n"),
                () -> Main.interactive(null)
            )
        );
    }

    private static ByteArrayInputStream input(final String format, final Object... args) {
        return new ByteArrayInputStream(
            String.format(format, args).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static int withStdin(final InputStream input, final IntSupplier action) {
        System.setIn(input);
        try {
            return action.getAsInt();
        } finally {
            System.setIn(System.in);
        }
    }

    private static int withStreams(
        final InputStream input, final ByteArrayOutputStream out,
        final ByteArrayOutputStream err, final IntSupplier action
    ) {
        final PrintStream priorout = System.out;
        final PrintStream priorerr = System.err;
        System.setIn(input);
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        try {
            return action.getAsInt();
        } finally {
            System.setIn(System.in);
            System.setOut(priorout);
            System.setErr(priorerr);
        }
    }

}
