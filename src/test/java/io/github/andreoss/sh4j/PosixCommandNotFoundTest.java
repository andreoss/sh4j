/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: command search failures (POSIX 2.9.1.1, 2.8.2).
 *
 * @since 1.0
 */
final class PosixCommandNotFoundTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void missingCommandReportsOnStderr() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("nosuchcmd_q").stderr().contains("nosuchcmd_q")
        );
    }

    @Test
    void missingCommandReturnsNotFound() {
        Assertions.assertEquals(
            String.format("rc=127%n"),
            Sh4j.INSTANCE.out("nosuchcmd_q; echo \"rc=$?\"")
        );
    }

    @Test
    void missingCommandMessageFollowsErrorRedirection() throws IOException {
        Assertions.assertTrue(
            Sh4j.INSTANCE.out(
                PosixCommandNotFoundTest.in("errfile", "nosuchcmd_q 2> f; cat f")
            ).contains("nosuchcmd_q")
        );
    }

    @Test
    void missingCommandMessageLeavesStderrWhenRedirected() throws IOException {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull(
                PosixCommandNotFoundTest.in("errgone", "nosuchcmd_q 2> f")
            ).stderr()
        );
    }

    @Test
    void missingCommandToDevNullPrintsOnlyFollowingOutput() {
        Assertions.assertEquals(
            String.format("ok%n"),
            Sh4j.INSTANCE.runFull("nosuchcmd_q 2>/dev/null; echo ok").stdout()
        );
    }

    @Test
    void missingPathReportsOnStderr() throws IOException {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                PosixCommandNotFoundTest.in("nopath", "./nosuchfile")
            ).stderr().contains("nosuchfile")
        );
    }

    @Test
    void missingPathReturnsNotFound() throws IOException {
        Assertions.assertEquals(
            String.format("127%n"),
            Sh4j.INSTANCE.out(
                PosixCommandNotFoundTest.in("nopathrc", "./nosuchfile; echo $?")
            )
        );
    }

    @Test
    void execMissingPathReportsOnStderr() throws IOException {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                PosixCommandNotFoundTest.in("execmsg", "exec ./nosuchfile")
            ).stderr().contains("nosuchfile")
        );
    }

    @Test
    void execMissingPathExitsNotFound() throws IOException {
        Assertions.assertEquals(
            127,
            Sh4j.INSTANCE.run(
                PosixCommandNotFoundTest.in("execrc", "exec ./nosuchfile; echo after")
            )
        );
    }

    @Test
    void execMissingCommandMessageFollowsErrorRedirection() throws IOException {
        final File dir = new TempDirs(PosixCommandNotFoundTest.root).folder("execerr");
        Sh4j.INSTANCE.run(
            PosixCommandNotFoundTest.in(dir, "exec nosuchcmd_q 2> f")
        );
        Assertions.assertTrue(
            Files.readString(dir.toPath().resolve("f"), StandardCharsets.UTF_8)
                .contains("nosuchcmd_q")
        );
    }

    @Test
    void unexecutableFileReportsOnStderr() throws IOException {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                PosixCommandNotFoundTest.in(PosixCommandNotFoundTest.plain("plainmsg"), "./f")
            ).stderr().contains("./f")
        );
    }

    @Test
    void unexecutableFileReturnsCannotExecute() throws IOException {
        Assertions.assertEquals(
            String.format("126%n"),
            Sh4j.INSTANCE.out(
                PosixCommandNotFoundTest.in(
                    PosixCommandNotFoundTest.plain("plainrc"), "./f; echo $?"
                )
            )
        );
    }

    @Test
    void directoryReportsOnStderr() throws IOException {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                PosixCommandNotFoundTest.in("dirmsg", "mkdir sub && ./sub")
            ).stderr().contains("./sub")
        );
    }

    @Test
    void directoryReturnsCannotExecute() throws IOException {
        Assertions.assertEquals(
            String.format("126%n"),
            Sh4j.INSTANCE.out(
                PosixCommandNotFoundTest.in("dirrc", "mkdir sub && ./sub; echo $?")
            )
        );
    }

    @Test
    void missingCommandStillCreatesOutputFile() throws IOException {
        Assertions.assertEquals(
            String.format("out%n"),
            Sh4j.INSTANCE.out(
                PosixCommandNotFoundTest.in(
                    "outfile", "nosuchcmd_q > out 2>/dev/null; ls out"
                )
            )
        );
    }

    @Test
    void commandVerboseOnMissingPrintsNothing() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("command -v nosuchcmd_q").stderr()
        );
    }

    @Test
    void commandVerboseOnMissingFails() {
        Assertions.assertNotEquals(
            0,
            Sh4j.INSTANCE.run("command -v nosuchcmd_q")
        );
    }

    private static File plain(final String name) throws IOException {
        final File dir = new TempDirs(PosixCommandNotFoundTest.root).folder(name);
        final Path file = dir.toPath().resolve("f");
        Files.writeString(file, String.format("echo hi%n"), StandardCharsets.UTF_8);
        file.toFile().setExecutable(false, false);
        return dir;
    }

    private static String in(final String name, final String script) throws IOException {
        return PosixCommandNotFoundTest.in(
            new TempDirs(PosixCommandNotFoundTest.root).folder(name), script
        );
    }

    private static String in(final File dir, final String script) {
        return String.format("cd '%s' && %s", dir.getAbsolutePath(), script);
    }
}
