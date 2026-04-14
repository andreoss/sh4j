/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.executor.Executor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Main}: is incomplete.
 *
 * @since 1.0
 */
final class MainIsIncompleteTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

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
    void runScriptWithEcho() throws IOException {
        final File script = new TempDirs(MainIsIncompleteTest.root).file("echo.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("echo hello%n"));
        }
        Assertions.assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptWithExitZero() throws IOException {
        final File script = new TempDirs(MainIsIncompleteTest.root).file("exit0.sh");
        try (BufferedWriter w = Files.newBufferedWriter(script.toPath(), StandardCharsets.UTF_8)) {
            w.write(String.format("exit 0%n"));
        }
        Assertions.assertEquals(0, Main.runScript(script.getAbsolutePath()));
    }

    @Test
    void runScriptWithExitOne() throws IOException {
        final File script = new TempDirs(MainIsIncompleteTest.root).file("exit1.sh");
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
    void runSourceParseErrorReturnsTwo() {
        Assertions.assertEquals(2, Main.runSource("if", Collections.singletonList("sh")));
    }

    @Test
    void runSourceExitStatus() {
        Assertions.assertEquals(6, Main.runSource("exit 6", Collections.singletonList("sh")));
    }

    @Test
    void runStartupFileExecutesScript() throws IOException {
        final File startup = new TempDirs(MainIsIncompleteTest.root).file("env.sh");
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
    void runSourceRunsExitTrapFirst() throws IOException {
        Assertions.assertEquals(
            0,
            Main.runSource(
                String.format(
                    "trap 'echo trapped > %s' EXIT; echo body",
                    new TempDirs(MainIsIncompleteTest.root).file(
                        "trap-marker.txt"
                    ).getAbsolutePath()
                ),
                Collections.singletonList("sh")
            )
        );
    }

    @Test
    void runSourceRunsExitTrapSecond() throws IOException {
        final File marker = new TempDirs(MainIsIncompleteTest.root).file("trap-marker.txt");
        Main.runSource(
            String.format("trap 'echo trapped > %s' EXIT; echo body", marker.getAbsolutePath()),
            Collections.singletonList("sh")
        );
        Assertions.assertEquals(
            String.format("trapped%s", System.lineSeparator()),
            new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8)
        );
    }

    @Test
    void runSourcePropagatesExitAndRunsTrapFirst() throws IOException {
        Assertions.assertEquals(
            3,
            Main.runSource(
                String.format(
                    "trap 'echo trapped > %s' EXIT; exit 3",
                    new TempDirs(MainIsIncompleteTest.root).file(
                        "exit-trap-marker.txt"
                    ).getAbsolutePath()
                ),
                Collections.singletonList("sh")
            )
        );
    }

    @Test
    void runSourcePropagatesExitAndRunsTrapSecond() throws IOException {
        final File marker = new TempDirs(MainIsIncompleteTest.root).file("exit-trap-marker.txt");
        Main.runSource(
            String.format("trap 'echo trapped > %s' EXIT; exit 3", marker.getAbsolutePath()),
            Collections.singletonList("sh")
        );
        Assertions.assertTrue(marker.isFile());
    }

    @Test
    void runStartupFileWithParseError() throws IOException {
        final File startup = new TempDirs(MainIsIncompleteTest.root).file("bad-env.sh");
        Files.write(startup.toPath(), String.format("if%n").getBytes(StandardCharsets.UTF_8));
        final Executor executor = new Executor();
        Main.runStartupFile(executor, startup.getAbsolutePath());
        Assertions.assertNotNull(executor.getVariable("PATH"));
    }
}
