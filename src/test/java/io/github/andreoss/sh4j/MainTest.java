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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for Main.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnnecessaryLocalRule"})
final class MainTest {

    /** Field. */
    @TempDir
    private File tempdir;

    @Test
    void runScriptWithEcho() throws IOException {
        final File script = new File(this.tempdir, "echo.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("echo hello%n"));
        }
        Assertions.assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptWithExitZero() throws IOException {
        final File script = new File(this.tempdir, "exit0.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 0%n"));
        }
        Assertions.assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptWithExitOne() throws IOException {
        final File script = new File(this.tempdir, "exit1.sh");
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
    void exitCodeWithArgsReturnsRunScriptResult() throws IOException {
        final File script = new File(this.tempdir, "test_exit.sh");
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
                new ByteArrayInputStream(String.format("%n").getBytes(StandardCharsets.UTF_8)),
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
        final File script = new File(this.tempdir, "args.sh");
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
        final File script = new File(this.tempdir, "first.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit $1%n"));
        }
        Assertions.assertEquals(7, Main.exitCode(new String[] {script.getAbsolutePath(), "7"}));
    }

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
                },
                () -> Main.exitCode(new String[0])
            )
        );
    }

    /**
     * Run an action with System.in temporarily replaced, restoring on exit.
     * @param input Temporary System.in
     * @param action Action that returns an int
     * @return Action result
     */
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
}
