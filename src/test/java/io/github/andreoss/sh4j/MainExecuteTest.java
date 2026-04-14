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
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Main}: execute.
 *
 * @since 1.0
 */
final class MainExecuteTest {

    /** Standard streams at class load. */
    private static final MainExecuteTest.Console ORIGINAL = new MainExecuteTest.Console(
        System.in, System.out, System.err
    );

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    private static ByteArrayInputStream input(final String format) {
        return new ByteArrayInputStream(
            String.format(format).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static int withStdin(
        final InputStream input, final IntSupplier action
    ) {
        System.setIn(input);
        try {
            return action.getAsInt();
        } finally {
            MainExecuteTest.ORIGINAL.restore();
        }
    }

    private static int withStreams(
        final InputStream input, final ByteArrayOutputStream out,
        final ByteArrayOutputStream err, final IntSupplier action
    ) {
        try (
            PrintStream sout = new PrintStream(out, true, StandardCharsets.UTF_8);
            PrintStream serr = new PrintStream(err, true, StandardCharsets.UTF_8)
        ) {
            System.setIn(input);
            System.setOut(sout);
            System.setErr(serr);
            return action.getAsInt();
        } finally {
            MainExecuteTest.ORIGINAL.restore();
        }
    }

    /**
     * Standard streams to put back after a test swaps them.
     *
     * @param in Standard input
     * @param out Standard output
     * @param err Standard error
     * @since 1.0
     */
    private record Console(InputStream in, PrintStream out, PrintStream err) {

        /**
         * Installs these streams as the standard streams.
         */
        void restore() {
            System.setIn(this.in);
            System.setOut(this.out);
            System.setErr(this.err);
        }
    }

    /**
     * Input stream whose every read fails.
     *
     * @since 1.0
     */
    private static final class FailingInput extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException();
        }

        @Override
        public int read(final byte[] buf, final int off, final int len)
            throws IOException {
            throw new IOException();
        }
    }

    @Test
    void executeLineWithIncompleteIf() {
        final Executor executor = new Executor();
        Main.executeLine(executor, "if");
        Assertions.assertNotNull(executor.getVariable("PATH"));
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
    void executeLineWithLexerError() {
        final Executor executor = new Executor();
        Main.executeLine(executor, "`unclosed backtick");
        Assertions.assertNotNull(executor.getVariable("PATH"));
    }

    @Test
    void exitCodeNoArgsInteractiveIoException() throws IOException {
        try (InputStream failing = new MainExecuteTest.FailingInput()) {
            Assertions.assertEquals(
                0,
                MainExecuteTest.withStdin(
                    failing, () -> Main.exitCode(new String[0])
                )
            );
        }
    }

    @Test
    void exitCodeDashSReadFailure() throws IOException {
        try (InputStream failing = new MainExecuteTest.FailingInput()) {
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> MainExecuteTest.withStdin(
                    failing, () -> Main.exitCode(new String[] {"-s"})
                )
            );
        }
    }

    @Test
    void exitCodeDashCRunsCommand() {
        Assertions.assertEquals(7, Main.exitCode(new String[] {"-c", "exit 7"}));
    }

    @Test
    void exitCodeWithArgsReturnsRunScriptResult() throws IOException {
        final File script = new TempDirs(MainExecuteTest.root).file("test_exit.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 42%n"));
        }
        Assertions.assertEquals(42, Main.exitCode(new String[] {script.getAbsolutePath()}));
    }

    @Test
    void exitCodeNoArgsInteractiveEof() {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[0])
            )
        );
    }

    @Test
    void exitCodeNoArgsInteractiveEmptyLine() {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(MainExecuteTest.input("%n"), () -> Main.exitCode(new String[0]))
        );
    }

    @Test
    void exitCodeNoArgsInteractiveWithCommand() {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(
                new ByteArrayInputStream(
                    String.format("echo hello%n").getBytes(StandardCharsets.UTF_8)
                ),
                () -> Main.exitCode(new String[0])
            )
        );
    }

    @Test
    void exitCodeWithMultipleArgsPassesPositionalParams() throws IOException {
        final File script = new TempDirs(MainExecuteTest.root).file("args.sh");
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
        final File script = new TempDirs(MainExecuteTest.root).file("first.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit $1%n"));
        }
        Assertions.assertEquals(7, Main.exitCode(new String[] {script.getAbsolutePath(), "7"}));
    }

    @Test
    void exitCodeDashSReadsStdin() {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(
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
            MainExecuteTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[] {"-i"})
            )
        );
    }

    @Test
    void exitCodeDashDashRunsScript() throws IOException {
        final File script = new TempDirs(MainExecuteTest.root).file("dashdash.sh");
        Files.write(script.toPath(), String.format("exit 4%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(4, Main.exitCode(new String[] {"--", script.getAbsolutePath()}));
    }

    @Test
    void exitCodeUnknownOptionSkipsToScript() throws IOException {
        final File script = new TempDirs(MainExecuteTest.root).file("unknown.sh");
        Files.write(script.toPath(), String.format("exit 5%n").getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(5, Main.exitCode(new String[] {"-x", script.getAbsolutePath()}));
    }

    @Test
    void exitCodeDashCMissingArgument() {
        Assertions.assertEquals(
            1,
            MainExecuteTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[] {"-c"})
            )
        );
    }

    @Test
    void exitCodeDashSEmptyStdin() {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.exitCode(new String[] {"-s"})
            )
        );
    }

    @Test
    void exitCodeDashCWithPositionalArguments() {
        Assertions.assertEquals(7, Main.exitCode(new String[] {"-c", "exit $1", "sh", "7"}));
    }

    @Test
    void interactiveWithEnvFileLoadsStartup() throws IOException {
        final File startup = new TempDirs(MainExecuteTest.root).file("env.sh");
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
        final File startup = new TempDirs(MainExecuteTest.root).file("env.sh");
        try (BufferedWriter w = Files.newBufferedWriter(startup.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("export STARTUP_RAN=yes%n"));
        }
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(
                new ByteArrayInputStream(new byte[0]),
                () -> Main.interactive(startup.getAbsolutePath())
            )
        );
    }

    @Test
    void interactiveIncompleteQuoteAtEof() {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(
                new ByteArrayInputStream("echo 'x".getBytes(StandardCharsets.UTF_8)),
                () -> Main.interactive(null)
            )
        );
    }

    @Test
    void interactiveBlankLinesAndParseError() {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(
                MainExecuteTest.input("%n%nif%necho done%n"),
                () -> Main.interactive(null)
            )
        );
    }

    @Test
    void interactiveCustomPromptsAndContinuation() throws IOException {
        final File startup = new TempDirs(MainExecuteTest.root).file("prompt-env.sh");
        Files.write(
            startup.toPath(),
            String.format("PS1='p1> '%nPS2='p2> '%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(
                MainExecuteTest.input("echo 'a%nb'%n"),
                () -> Main.interactive(startup.getAbsolutePath())
            )
        );
    }

    @Test
    void interactiveCapturedOutputAndPromptsFirst() throws IOException {
        final File startup = new TempDirs(MainExecuteTest.root).file("capture-env.sh");
        Files.write(
            startup.toPath(),
            String.format("PS1='one> '%nPS2='two> '%n").getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStreams(
                MainExecuteTest.input("echo hello%necho 'a%nb'%n%nif%n"),
                new ByteArrayOutputStream(),
                new ByteArrayOutputStream(),
                () -> Main.interactive(startup.getAbsolutePath())
            )
        );
    }

    @Test
    void interactiveCapturedOutputAndPromptsSecond() throws IOException {
        final File startup = new TempDirs(MainExecuteTest.root).file("capture-env.sh");
        Files.write(
            startup.toPath(),
            String.format("PS1='one> '%nPS2='two> '%n").getBytes(StandardCharsets.UTF_8)
        );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        MainExecuteTest.withStreams(
            MainExecuteTest.input("echo hello%necho 'a%nb'%n%nif%n"),
            out,
            new ByteArrayOutputStream(),
            () -> Main.interactive(startup.getAbsolutePath())
        );
        Assertions.assertTrue(out.toString(StandardCharsets.UTF_8).contains("one> "));
    }

    @Test
    void interactiveCapturedOutputAndPromptsThird() throws IOException {
        final File startup = new TempDirs(MainExecuteTest.root).file("capture-env.sh");
        Files.write(
            startup.toPath(),
            String.format("PS1='one> '%nPS2='two> '%n").getBytes(StandardCharsets.UTF_8)
        );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        MainExecuteTest.withStreams(
            MainExecuteTest.input("echo hello%necho 'a%nb'%n%nif%n"),
            out,
            new ByteArrayOutputStream(),
            () -> Main.interactive(startup.getAbsolutePath())
        );
        Assertions.assertTrue(out.toString(StandardCharsets.UTF_8).contains("two> "));
    }

    @Test
    void interactiveCapturedOutputAndPromptsFourth() throws IOException {
        final File startup = new TempDirs(MainExecuteTest.root).file("capture-env.sh");
        Files.write(
            startup.toPath(),
            String.format("PS1='one> '%nPS2='two> '%n").getBytes(StandardCharsets.UTF_8)
        );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        MainExecuteTest.withStreams(
            MainExecuteTest.input("echo hello%necho 'a%nb'%n%nif%n"),
            out,
            new ByteArrayOutputStream(),
            () -> Main.interactive(startup.getAbsolutePath())
        );
        Assertions.assertTrue(out.toString(StandardCharsets.UTF_8).contains("hello"));
    }

    @Test
    void interactiveCapturedOutputAndPromptsFifth() throws IOException {
        final File startup = new TempDirs(MainExecuteTest.root).file("capture-env.sh");
        Files.write(
            startup.toPath(),
            String.format("PS1='one> '%nPS2='two> '%n").getBytes(StandardCharsets.UTF_8)
        );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        MainExecuteTest.withStreams(
            MainExecuteTest.input("echo hello%necho 'a%nb'%n%nif%n"),
            out,
            new ByteArrayOutputStream(),
            () -> Main.interactive(startup.getAbsolutePath())
        );
        Assertions.assertTrue(out.toString(StandardCharsets.UTF_8).contains("a"));
    }

    @Test
    void interactiveCapturedOutputAndPromptsSixth() throws IOException {
        final File startup = new TempDirs(MainExecuteTest.root).file("capture-env.sh");
        Files.write(
            startup.toPath(),
            String.format("PS1='one> '%nPS2='two> '%n").getBytes(StandardCharsets.UTF_8)
        );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        MainExecuteTest.withStreams(
            MainExecuteTest.input("echo hello%necho 'a%nb'%n%nif%n"),
            out,
            err,
            () -> Main.interactive(startup.getAbsolutePath())
        );
        Assertions.assertTrue(err.toString(StandardCharsets.UTF_8).contains("syntax error"));
    }

    @Test
    void interactiveBlankAndIncompleteDoubleQuoteAtEofFirst() {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStreams(
                MainExecuteTest.input("%n%n%necho \"x"),
                new ByteArrayOutputStream(),
                new ByteArrayOutputStream(),
                () -> Main.interactive(null)
            )
        );
    }

    @Test
    void interactiveBlankAndIncompleteDoubleQuoteAtEofSecond() {
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final ByteArrayOutputStream recorded = new ByteArrayOutputStream();
        MainExecuteTest.withStreams(
            MainExecuteTest.input("%n%n%necho \"x"),
            recorded,
            err,
            () -> Main.interactive(null)
        );
        Assertions.assertTrue(
            recorded.toString(StandardCharsets.UTF_8).contains("$"),
            "the REPL must print a prompt before reporting EOF"
        );
    }

    @Test
    void interactiveWithMultiLineInput() throws IOException {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(
                MainExecuteTest.input("echo 'hello%nworld'%n"),
                () -> Main.interactive(null)
            )
        );
    }

    @Test
    void interactiveWithEmptyEnvFile() throws IOException {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(new ByteArrayInputStream(new byte[0]), () -> Main.interactive(""))
        );
    }

    @Test
    void interactiveWithContinuationPrompt() throws IOException {
        Assertions.assertEquals(
            0,
            MainExecuteTest.withStdin(MainExecuteTest.input("echo '%n'%n"), () -> Main.interactive(null))
        );
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
}
