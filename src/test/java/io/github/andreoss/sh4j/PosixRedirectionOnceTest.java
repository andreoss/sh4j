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
 * Tests for {@link Sh4j}: the word after a redirection operator is expanded
 * exactly once, with or without the {@code noclobber} option (POSIX 2.7).
 *
 * @since 1.0
 */
final class PosixRedirectionOnceTest {

    /** Command substitution that logs each of its runs. */
    private static final String LOGGED = "\"$(echo x >> log; echo out)\"";

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void noclobberRunsArithmeticInTargetOnce() throws IOException {
        Assertions.assertEquals(
            String.format("1 f1%n"),
            PosixRedirectionOnceTest.inside(
                "set -C; n=0; echo a > \"f$((n+=1))\"; echo \"$n\" f*"
            )
        );
    }

    @Test
    void noclobberRunsSubstitutionOfBuiltinOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("set -C; echo a > %s")
        );
    }

    @Test
    void noclobberRunsSubstitutionOfGroupOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("set -C; { :; } > %s")
        );
    }

    @Test
    void noclobberRunsSubstitutionOfFunctionOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("set -C; f() { :; } > %s; f")
        );
    }

    @Test
    void noclobberRunsSubstitutionOfExecOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("set -C; exec 3> %s")
        );
    }

    @Test
    void noclobberRunsSubstitutionOfClobberOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("set -C; echo a >| %s")
        );
    }

    @Test
    void noclobberRunsSubstitutionOfExternalOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("set -C; cat /dev/null > %s")
        );
    }

    @Test
    void runsSubstitutionOfBuiltinOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("echo a > %s")
        );
    }

    @Test
    void runsSubstitutionOfExternalOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("cat /dev/null > %s")
        );
    }

    @Test
    void createsOneFileForExternalArithmeticTarget() throws IOException {
        Assertions.assertEquals(
            String.format("1 f1%n"),
            PosixRedirectionOnceTest.inside(
                "n=0; cat /dev/null > \"f$((n+=1))\"; echo \"$n\" f*"
            )
        );
    }

    @Test
    void runsSubstitutionOfExternalInputOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.inside("cat < \"$(echo x >> log; echo log)\"")
        );
    }

    @Test
    void runsSubstitutionOfExecDescriptorOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("exec 3> %s")
        );
    }

    @Test
    void runsSubstitutionOfExecAppendOnce() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            PosixRedirectionOnceTest.logged("exec 3>> %s")
        );
    }

    @Test
    void runsSubstitutionOfExecStdoutOnce() throws IOException {
        final File dir = new TempDirs(PosixRedirectionOnceTest.root).folder();
        Sh4j.INSTANCE.run(
            String.format(
                "cd '%s' && { exec > %s; }",
                dir.getAbsolutePath(),
                PosixRedirectionOnceTest.LOGGED
            )
        );
        Assertions.assertEquals(
            String.format("x%n"),
            Files.readString(new File(dir, "log").toPath(), StandardCharsets.UTF_8)
        );
    }

    @Test
    void noclobberRefusesExistingQuotedTarget() throws IOException {
        Assertions.assertEquals(
            String.format("a%n"),
            PosixRedirectionOnceTest.inside(
                "set -C; echo a > \"it's\"; echo b > \"it's\"; cat \"it's\""
            )
        );
    }

    private static String logged(final String script) throws IOException {
        return PosixRedirectionOnceTest.inside(
            String.format(
                "%s; cat log", String.format(script, PosixRedirectionOnceTest.LOGGED)
            )
        );
    }

    private static String inside(final String script) throws IOException {
        return Sh4j.INSTANCE.out(
            String.format(
                "cd '%s' && { %s; }",
                new TempDirs(PosixRedirectionOnceTest.root).folder().getAbsolutePath(),
                script
            )
        );
    }
}
