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
 * Tests for {@link Sh4j}: output redirection errors (POSIX 2.7.2, 2.8.1, 2.14 set -e).
 *
 * @since 1.0
 */
final class PosixOutputRedirectionErrorTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void groupToMissingDirectoryFails() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("{ echo a; } > /nonexistent/x; echo \"rc=$?\"")
        );
    }

    @Test
    void groupToMissingDirectoryReportsOnStderr() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("{ echo a; } > /nonexistent/x").stderr()
                .contains("/nonexistent/x")
        );
    }

    @Test
    void groupToMissingDirectoryExitsUnderErrexit() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; { echo a; } > /nonexistent/x; echo after")
        );
    }

    @Test
    void groupToMissingDirectoryExitsWithFailureUnderErrexit() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("set -e; { echo a; } > /nonexistent/x; echo after")
        );
    }

    @Test
    void subshellToMissingDirectoryExitsUnderErrexit() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; (echo a) > /nonexistent/x; echo after")
        );
    }

    @Test
    void echoToMissingDirectoryFails() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("echo a > /nonexistent/x; echo \"rc=$?\"")
        );
    }

    @Test
    void echoToMissingDirectoryReportsOnStderr() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo a > /nonexistent/x").stderr()
                .contains("/nonexistent/x")
        );
    }

    @Test
    void appendToMissingDirectoryReportsOnStderr() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo a >> /nonexistent/x").stderr()
                .contains("/nonexistent/x")
        );
    }

    @Test
    void externalToMissingDirectoryReportsOnStderr() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("cat /dev/null > /nonexistent/x").stderr()
                .contains("/nonexistent/x")
        );
    }

    @Test
    void echoToMissingDirectoryExitsUnderErrexit() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; echo a > /nonexistent/x; echo after")
        );
    }

    @Test
    void functionToMissingDirectoryExitsUnderErrexit() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; f() { echo a; }; f > /nonexistent/x; echo after")
        );
    }

    @Test
    void ifToMissingDirectoryExitsUnderErrexit() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; if true; then :; fi > /nonexistent/x; echo after")
        );
    }

    @Test
    void forToMissingDirectoryExitsUnderErrexit() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; for i in 1; do :; done > /nonexistent/x; echo after")
        );
    }

    @Test
    void whileToMissingDirectoryExitsUnderErrexit() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; while false; do :; done > /nonexistent/x; echo after")
        );
    }

    @Test
    void caseToMissingDirectoryExitsUnderErrexit() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; case a in a) :;; esac > /nonexistent/x; echo after")
        );
    }

    @Test
    void groupFromMissingFileExitsUnderErrexit() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("set -e; { echo a; } < /nonexistent/x; echo after")
        );
    }

    @Test
    void echoOrListContinuesUnderErrexit() {
        Assertions.assertEquals(
            String.format("handled%nafter%n"),
            Sh4j.INSTANCE.out(
                "set -e; echo a > /nonexistent/x || echo handled; echo after"
            )
        );
    }

    @Test
    void groupOrListContinuesUnderErrexit() {
        Assertions.assertEquals(
            String.format("handled%nafter%n"),
            Sh4j.INSTANCE.out(
                "set -e; { echo a; } > /nonexistent/x || echo handled; echo after"
            )
        );
    }

    @Test
    void groupInConditionContinuesUnderErrexit() {
        Assertions.assertEquals(
            String.format("no%n"),
            Sh4j.INSTANCE.out(
                "set -e; if { :; } > /nonexistent/x; then echo yes; else echo no; fi"
            )
        );
    }

    @Test
    void groupInPipelineContinuesUnderErrexit() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out(
                "set -e; { echo a; } > /nonexistent/x | cat; echo after"
            )
        );
    }

    @Test
    void messageFollowsEarlierStderrRedirection() {
        Assertions.assertFalse(
            Sh4j.INSTANCE.runFull("echo a 2>/dev/null > /nonexistent/x").stderr()
                .contains("/nonexistent/x")
        );
    }

    @Test
    void unboundDescriptorReportsOnStderr() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("echo a >&7").stderr().contains("7")
        );
    }

    @Test
    void noclobberRefusalKeepsMessage() throws IOException {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                String.format(
                    "cd '%s' && set -C && echo a > kept",
                    PosixOutputRedirectionErrorTest.existing().getAbsolutePath()
                )
            ).stderr().contains("kept")
        );
    }

    @Test
    void noclobberRefusalOnGroupExitsUnderErrexit() throws IOException {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s' && set -eC && { echo a; } > kept; echo after",
                    PosixOutputRedirectionErrorTest.existing().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void groupToWritableFileStillWorks() throws IOException {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "set -e; cd '%s' && { echo a; } > out; cat out",
                    new TempDirs(PosixOutputRedirectionErrorTest.root).folder().getAbsolutePath()
                )
            )
        );
    }

    private static File existing() throws IOException {
        final File dir = new TempDirs(PosixOutputRedirectionErrorTest.root).folder();
        Files.writeString(
            dir.toPath().resolve("kept"), String.format("old%n"), StandardCharsets.UTF_8
        );
        return dir;
    }
}
