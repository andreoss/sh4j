/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: the EXIT trap of a subshell environment.
 *
 * @since 1.0
 */
final class PosixSubshellExitTrapTest {

    @Test
    void runsTrapWhenSubshellEnds() {
        Assertions.assertEquals(
            String.format("in%nbye%nout%n"),
            Sh4j.INSTANCE.out("(trap 'echo bye' EXIT; echo in); echo out")
        );
    }

    @Test
    void runsTrapWhenSubshellExits() {
        Assertions.assertEquals(
            String.format("bye%nrc=3%n"),
            Sh4j.INSTANCE.out("(trap 'echo bye' EXIT; exit 3); echo \"rc=$?\"")
        );
    }

    @Test
    void capturesTrapOutputOfSubstitution() {
        Assertions.assertEquals(
            String.format("[v%nT]%n"),
            Sh4j.INSTANCE.out("x=$(trap 'echo T' EXIT; echo v); echo \"[$x]\"")
        );
    }

    @Test
    void runsInheritedTrapOnlyInParent() {
        Assertions.assertEquals(
            String.format("sub%nouter%n"),
            Sh4j.INSTANCE.out("trap 'echo outer' EXIT; (echo sub)")
        );
    }

    @Test
    void runsTrapSetAgainInSubshell() {
        Assertions.assertEquals(
            String.format("s%nouter%nouter%n"),
            Sh4j.INSTANCE.out("trap 'echo outer' EXIT; (trap 'echo outer' EXIT; echo s)")
        );
    }

    @Test
    void keepsStatusOfFailingSubshell() {
        Assertions.assertEquals(
            String.format("bye%nrc=1%n"),
            Sh4j.INSTANCE.out("(trap 'echo bye' EXIT; false); echo \"rc=$?\"")
        );
    }

    @Test
    void showsExitStatusToTrapAction() {
        Assertions.assertEquals(
            String.format("st=4%n"),
            Sh4j.INSTANCE.out("(trap 'echo st=$?' EXIT; exit 4)")
        );
    }

    @Test
    void returnsSubshellStatusAfterTrap() {
        Assertions.assertEquals(
            4,
            Sh4j.INSTANCE.run("(trap 'echo st' EXIT; exit 4)")
        );
    }

    @Test
    void runsTrapOfPipelineSubshell() {
        Assertions.assertEquals(
            String.format("a%nP%n"),
            Sh4j.INSTANCE.out("(trap 'echo P' EXIT; echo a) | cat")
        );
    }

    @Test
    void runsTrapOfPipelineStage() {
        Assertions.assertEquals(
            String.format("Q%n"),
            Sh4j.INSTANCE.out("trap 'echo Q' EXIT | cat")
        );
    }

    @Test
    void runsTrapOfBackgroundJob() {
        Assertions.assertEquals(
            String.format("B%n"),
            Sh4j.INSTANCE.out("trap 'echo B' EXIT & wait")
        );
    }

    @Test
    void skipsRemovedTrap() {
        Assertions.assertEquals(
            String.format("z%n"),
            Sh4j.INSTANCE.out("(trap 'echo Q' EXIT; trap - EXIT; echo z)")
        );
    }

    @Test
    void ignoresOtherSignalTrapAtSubshellEnd() {
        Assertions.assertEquals(
            String.format("k%n"),
            Sh4j.INSTANCE.out("(trap 'echo I' INT; echo k)")
        );
    }

    @Test
    void runsNestedSubshellTrapFirst() {
        Assertions.assertEquals(
            String.format("d1%ndeep%nd2%nX%n"),
            Sh4j.INSTANCE.out("(trap 'echo X' EXIT; (trap 'echo deep' EXIT; echo d1); echo d2)")
        );
    }
}
