/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Sh4j}: noclobber (POSIX 2.7.2), redirections placed
 * anywhere in a simple command (POSIX 2.9.1) and redirections of a function
 * definition (POSIX 2.9.5).
 *
 * @since 1.0
 */
final class PosixCommandRedirectTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void noclobberRefusesExistingFile() throws IOException {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside(
                    "set -C; echo a > nc; echo b > nc 2>/dev/null; echo \"rc=$?\""
                )
            )
        );
    }

    @Test
    void noclobberKeepsExistingContent() throws IOException {
        Assertions.assertEquals(
            String.format("a%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside(
                    "set -C; echo a > nc; echo b > nc 2>/dev/null; cat nc"
                )
            )
        );
    }

    @Test
    void noclobberReportsError() throws IOException {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull(
                PosixCommandRedirectTest.inside("set -C; echo a > nc; echo b > nc")
            ).stderr().contains("cannot overwrite existing file")
        );
    }

    @Test
    void clobberOperatorOverridesNoclobber() throws IOException {
        Assertions.assertEquals(
            String.format("c%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside("set -C; echo a > nc; echo c >| nc; cat nc")
            )
        );
    }

    @Test
    void noclobberAllowsDevNull() throws IOException {
        Assertions.assertEquals(
            String.format("rc=0%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside("set -C; echo x > /dev/null; echo \"rc=$?\"")
            )
        );
    }

    @Test
    void noclobberAllowsNewFile() throws IOException {
        Assertions.assertEquals(
            String.format("n%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside("set -C; rm -f nw; echo n > nw; cat nw")
            )
        );
    }

    @Test
    void noclobberLongOptionRefusesExistingFile() throws IOException {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside(
                    "set -o noclobber; echo a > nc; echo b > nc 2>/dev/null; echo \"rc=$?\""
                )
            )
        );
    }

    @Test
    void clearedNoclobberOverwrites() throws IOException {
        Assertions.assertEquals(
            String.format("b%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside("set -C; echo a > nc; set +C; echo b > nc; cat nc")
            )
        );
    }

    @Test
    void noclobberRefusesExternalCommand() throws IOException {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside(
                    "set -C; echo a > nc; ls > nc 2>/dev/null; echo \"rc=$?\""
                )
            )
        );
    }

    @Test
    void noclobberRefusesExecBinding() throws IOException {
        Assertions.assertEquals(
            1,
            Sh4j.INSTANCE.run(
                PosixCommandRedirectTest.inside("set -C; echo a > nc; exec 2>/dev/null >nc")
            )
        );
    }

    @Test
    void noclobberRefusesFunctionRedirection() throws IOException {
        Assertions.assertEquals(
            String.format("rc=1%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside(
                    "set -C; echo a > fr; f() { echo body; } > fr; f 2>/dev/null; echo \"rc=$?\""
                )
            )
        );
    }

    @Test
    void redirectionBeforeCommandName() throws IOException {
        Assertions.assertEquals(
            String.format("x%n"),
            Sh4j.INSTANCE.out(PosixCommandRedirectTest.inside(">ro echo x; cat ro"))
        );
    }

    @Test
    void redirectionBetweenArguments() throws IOException {
        Assertions.assertEquals(
            String.format("a b%n"),
            Sh4j.INSTANCE.out(PosixCommandRedirectTest.inside("echo >ab a b; cat ab"))
        );
    }

    @Test
    void descriptorRedirectionBeforeAssignment() {
        Assertions.assertEquals(
            String.format("ok%n"),
            Sh4j.INSTANCE.out("2>/dev/null x=1 echo ok")
        );
    }

    @Test
    void descriptorRedirectionBeforeAssignmentKeepsItLocal() {
        Assertions.assertEquals(
            String.format("x=%n"),
            Sh4j.INSTANCE.out("2>/dev/null x=1 echo ok >/dev/null; echo \"x=$x\"")
        );
    }

    @Test
    void functionRedirectionAppliesOnCall() throws IOException {
        Assertions.assertEquals(
            String.format("body%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside("f() { echo body; } > fr; f; cat fr")
            )
        );
    }

    @Test
    void functionRedirectionAppliesOnEveryCall() throws IOException {
        Assertions.assertEquals(
            String.format("body%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside(
                    "f() { echo body; } > fr; f; rm fr; f; cat fr"
                )
            )
        );
    }

    @Test
    void functionDefinitionDoesNotOpenTarget() throws IOException {
        Assertions.assertEquals(
            String.format("none%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside(
                    "rm -f fd; f() { echo body; } > fd; test -e fd || echo none"
                )
            )
        );
    }

    @Test
    void functionDescriptorRedirectionAppliesOnCall() throws IOException {
        Assertions.assertEquals(
            String.format("e%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside("g() { echo e >&2; } 2>fe; g; cat fe")
            )
        );
    }

    @Test
    void functionInputRedirectionAppliesOnCall() throws IOException {
        Assertions.assertEquals(
            String.format("in%n"),
            Sh4j.INSTANCE.out(
                PosixCommandRedirectTest.inside(
                    "echo in > fi; h() { read v; echo \"$v\"; } < fi; h"
                )
            )
        );
    }

    private static String inside(final String script) throws IOException {
        return String.format(
            "cd %s && %s",
            new TempDirs(PosixCommandRedirectTest.root).folder().getAbsolutePath(),
            script
        );
    }
}
