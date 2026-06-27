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
 * Tests for {@link Sh4j}: the redirections of a function call are expanded
 * in the current shell environment, so their side effects persist (POSIX
 * 2.7 and 2.9.5).
 *
 * @since 1.0
 */
final class PosixFunctionRedirectionTest {

    /** Temporary root directory. */
    @TempDir
    private static Path root;

    @Test
    void keepsArithmeticOfFileTarget() throws IOException {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s'; m=0; f() { :; }; f >\"x$((m+=1))\"; echo \"$m\"",
                    new TempDirs(PosixFunctionRedirectionTest.root).folder().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void keepsArithmeticOfDuplicationTarget() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("m=0; f() { :; }; f >&\"$((m+=1))\"; echo \"$m\"")
        );
    }

    @Test
    void keepsDefaultAssignmentOfFileTarget() throws IOException {
        Assertions.assertEquals(
            String.format("out%n"),
            Sh4j.INSTANCE.out(
                String.format(
                    "cd '%s'; unset y; f() { :; }; f >\"${y:=out}\"; echo \"$y\"",
                    new TempDirs(PosixFunctionRedirectionTest.root).folder().getAbsolutePath()
                )
            )
        );
    }

    @Test
    void showsSideEffectInsideBody() {
        Assertions.assertEquals(
            String.format("in 1%n"),
            Sh4j.INSTANCE.out("m=0; f() { echo \"in $m\"; }; f 2>&\"$((m+=1))\"")
        );
    }

    @Test
    void keepsSideEffectWithPrefixAssignment() {
        Assertions.assertEquals(
            String.format("1 2%n"),
            Sh4j.INSTANCE.out("m=0; f() { :; }; x=2 f >&\"$((m+=1))\"; echo \"$m $x\"")
        );
    }

    @Test
    void keepsAssignmentOfBody() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("f() { z=1; }; f >&\"1\"; echo \"$z\"")
        );
    }

    @Test
    void keepsArithmeticOfPrefixAssignment() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("m=0; f() { :; }; x=$((m+=1)) f; echo \"$m\"")
        );
    }

    @Test
    void keepsArithmeticOfExternalTarget() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("m=0; true >&\"$((m+=1))\"; echo \"$m\"")
        );
    }
}
