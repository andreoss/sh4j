/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: input redirection errors (POSIX 2.7.1, 2.8.1).
 *
 * @since 1.0
 */
final class PosixInputRedirectionErrorTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void readFromMissingFileFailsWithoutReadingStdin() {
        Assertions.assertEquals(
            String.format("rc=1 v=[]%n"),
            Sh4j.INSTANCE.stdout(
                PosixInputRedirectionErrorTest.data(),
                "read v < /nonexistent/x; echo \"rc=$? v=[$v]\""
            )
        );
    }

    @Test
    void readFromMissingFileReportsOnStderr() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                PosixInputRedirectionErrorTest.data(),
                "read v < /nonexistent/x"
            ).stderr().contains("/nonexistent/x")
        );
    }

    @Test
    void catFromMissingFileIsNotRun() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                PosixInputRedirectionErrorTest.data(),
                "cat < /nonexistent/x; echo \"rc=$?\""
            )
        );
    }

    @Test
    void groupFromMissingFileIsNotRun() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                PosixInputRedirectionErrorTest.data(),
                "{ read v; echo \"got=$v\"; } < /nonexistent/x; echo \"rc=$?\""
            )
        );
    }

    @Test
    void whileLoopFromMissingFileIsNotRun() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                PosixInputRedirectionErrorTest.data(),
                "while read l; do echo \"l=$l\"; done < /nonexistent/x; echo \"rc=$?\""
            )
        );
    }

    @Test
    void functionFromMissingFileIsNotRun() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("f() { echo in; }; f < /nonexistent/x; echo \"rc=$?\"")
        );
    }

    @Test
    void functionDefinitionRedirectionFromMissingFileFails() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("f() { echo in; } < /nonexistent/x; f; echo \"rc=$?\"")
        );
    }

    @Test
    void subshellFromMissingFileIsNotRun() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("( echo sub ) < /nonexistent/x; echo \"rc=$?\"")
        );
    }

    @Test
    void echoWithMissingInputIsNotRun() {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out("echo x < /nonexistent/x; echo \"rc=$?\"")
        );
    }

    @Test
    void execDescriptorFromMissingFileExitsShell() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("exec 3< /nonexistent/x; echo after")
        );
    }

    @Test
    void execDescriptorFromMissingFileExitsWithFailure() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("exec 3< /nonexistent/x; echo after")
        );
    }

    @Test
    void execDescriptorFromMissingFileReportsOnStderr() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("exec 3< /nonexistent/x").stderr()
                .contains("/nonexistent/x")
        );
    }

    @Test
    void execStdinFromMissingFileExitsShell() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("exec < /nonexistent/x; echo after")
        );
    }

    @Test
    void execCommandFromMissingFileExitsWithFailure() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run("exec cat < /nonexistent/x; echo after")
        );
    }

    @Test
    void colonFromMissingFileExitsShell() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out(": < /nonexistent/x; echo after")
        );
    }

    @Test
    void colonFromMissingFileExitsWithFailure() {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run(": < /nonexistent/x; echo after")
        );
    }

    @Test
    void evalFromMissingFileExitsShell() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("eval 'echo e' < /nonexistent/x; echo after")
        );
    }

    @Test
    void colonToMissingDirectoryExitsShell() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out(": > /nonexistent/x; echo after")
        );
    }

    @Test
    void colonThroughCommandFromMissingFileContinues() {
        Assertions.assertEquals(
            String.format("after 1%n"),
            Sh4j.INSTANCE.out("command : < /nonexistent/x; echo \"after $?\"")
        );
    }

    @Test
    void readFromUnreadableFileFails() throws IOException {
        Assertions.assertEquals(
            String.format("rc=1 v=[]%n"),
            Sh4j.INSTANCE.stdout(
                PosixInputRedirectionErrorTest.data(),
                String.format(
                    "cd '%s' && read v < secret; echo \"rc=$? v=[$v]\"",
                    PosixInputRedirectionErrorTest.locked().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void catFromUnreadableFileFails() throws IOException {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                PosixInputRedirectionErrorTest.data(),
                String.format(
                    "cd '%s' && cat < secret; echo \"rc=$?\"",
                    PosixInputRedirectionErrorTest.locked().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void colonFromUnreadableFileExitsShell() throws IOException {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s' && : < secret; echo after",
                    PosixInputRedirectionErrorTest.locked().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void readFromDirectoryFails() throws IOException {
        Assertions.assertEquals(
            String.format("rc=1 v=[]%n"),
            Sh4j.INSTANCE.stdout(
                PosixInputRedirectionErrorTest.data(),
                String.format(
                    "cd '%s' && read v < sub; echo \"rc=$? v=[$v]\"",
                    PosixInputRedirectionErrorTest.nested().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void catFromDirectoryFails() throws IOException {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.stdout(
                PosixInputRedirectionErrorTest.data(),
                String.format(
                    "cd '%s' && cat < sub; echo \"rc=$?\"",
                    PosixInputRedirectionErrorTest.nested().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void colonFromDirectoryContinues() throws IOException {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s' && : < sub; echo after",
                    PosixInputRedirectionErrorTest.nested().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void execDescriptorFromDirectoryContinues() throws IOException {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s' && exec 3< sub; echo after",
                    PosixInputRedirectionErrorTest.nested().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void readFromExistingFileStillWorks() throws IOException {
        final File dir = new TempDirs(PosixInputRedirectionErrorTest.root).folder();
        Files.writeString(
            dir.toPath().resolve("in"), String.format("line%n"), StandardCharsets.UTF_8
        );
        Assertions.assertEquals(
            String.format("v=[line]%n"),
            Sh4j.INSTANCE.stdout(
                PosixInputRedirectionErrorTest.data(),
                String.format(
                    "cd '%s' && read v < in; echo \"v=[$v]\"",
                    dir.getAbsolutePath()
                )
            )
        );
    }

    private static InputStream data() {
        return new ByteArrayInputStream(String.format("data%n").getBytes(StandardCharsets.UTF_8));
    }

    private static File locked() throws IOException {
        final File dir = new TempDirs(PosixInputRedirectionErrorTest.root).folder();
        final Path secret = dir.toPath().resolve("secret");
        Files.writeString(secret, String.format("hidden%n"), StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(secret, PosixFilePermissions.fromString("---------"));
        return dir;
    }

    private static File nested() throws IOException {
        final File dir = new TempDirs(PosixInputRedirectionErrorTest.root).folder();
        Files.createDirectory(dir.toPath().resolve("sub"));
        return dir;
    }
}
