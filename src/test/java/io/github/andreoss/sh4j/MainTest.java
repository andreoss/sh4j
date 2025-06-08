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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class MainTest {

    @TempDir
    File tempdir;

    private InputStream origin;

    @BeforeEach
    void saveSystemIn() {
        this.origin = System.in;
    }

    @AfterEach
    void restoreSystemIn() {
        System.setIn(this.origin);
    }

    @Test
    void runScriptWithEcho() throws IOException {
        final File script = new File(this.tempdir, "echo.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("echo hello%n"));
        }
        assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptWithExitZero() throws IOException {
        final File script = new File(this.tempdir, "exit0.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 0%n"));
        }
        assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptWithExitOne() throws IOException {
        final File script = new File(this.tempdir, "exit1.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 1%n"));
        }
        assertEquals(1, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptFileNotFound() {
        assertEquals(1, Main.runScript("/nonexistent/path/to/script.sh"));
    }

    @Test
    void exitCodeWithArgsReturnsRunScriptResult() throws IOException {
        final File script = new File(this.tempdir, "test_exit.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 42%n"));
        }
        assertEquals(42, Main.exitCode(new String[] {script.getAbsolutePath()}));
    }

    @Test
    void exitCodeNoArgsInteractiveEof() {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        assertEquals(0, Main.exitCode(new String[0]));
    }

    @Test
    void exitCodeNoArgsInteractiveEmptyLine() {
        System.setIn(new ByteArrayInputStream(String.format("%n").getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, Main.exitCode(new String[0]));
    }

    @Test
    void exitCodeNoArgsInteractiveWithCommand() {
        System.setIn(new ByteArrayInputStream(String.format("echo hello%n").getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, Main.exitCode(new String[0]));
    }

    @Test
    void exitCodeWithMultipleArgsPassesPositionalParams() throws IOException {
        final File script = new File(this.tempdir, "args.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit $#%n"));
        }
        assertEquals(3, Main.exitCode(new String[] {script.getAbsolutePath(), "a", "b", "c"}));
    }

    @Test
    void exitCodeWithTwoArgsExitsOnFirstPositional() throws IOException {
        final File script = new File(this.tempdir, "first.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit $1%n"));
        }
        assertEquals(7, Main.exitCode(new String[] {script.getAbsolutePath(), "7"}));
    }

    @Test
    void exitCodeNoArgsInteractiveIoException() {
        System.setIn(new InputStream() {

            public int read() throws IOException {
                throw new IOException();
            }
        });
        assertEquals(0, Main.exitCode(new String[0]));
    }
}
