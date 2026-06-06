/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: POSIX 2.9.3.1 exit status of asynchronous lists.
 *
 * @since 1.0
 */
final class PosixAsyncExitTest {

    @Test
    void exitInJobGivesStatusToWait() {
        Assertions.assertEquals(
            String.format("5%n"),
            Sh4j.INSTANCE.out("exit 5 & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void exitInSubshellJobGivesStatusToWait() {
        Assertions.assertEquals(
            String.format("5%n"),
            Sh4j.INSTANCE.out("(exit 5) & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void exitInBraceGroupJobGivesStatusToWait() {
        Assertions.assertEquals(
            String.format("6%n"),
            Sh4j.INSTANCE.out("{ exit 6; } & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void exitInIfJobGivesStatusToWait() {
        Assertions.assertEquals(
            String.format("9%n"),
            Sh4j.INSTANCE.out("if exit 9; then :; fi & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void exitInLoopConditionJobGivesStatusToWait() {
        Assertions.assertEquals(
            String.format("4%n"),
            Sh4j.INSTANCE.out("while exit 4; do :; done & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void exitInFunctionJobGivesStatusToWait() {
        Assertions.assertEquals(
            String.format("8%n"),
            Sh4j.INSTANCE.out("f() { exit 8; }; f & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void bareExitInJobUsesLastStatus() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("true; exit & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void exitInJobDoesNotEndTheShell() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("exit 5 & wait; echo after")
        );
    }

    @Test
    void childShellExitGivesStatusToWait() {
        Assertions.assertEquals(
            String.format("7%n"),
            Sh4j.INSTANCE.out("sh -c 'exit 7' & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void falseJobGivesOneToWait() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out("false & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void trueJobGivesZeroToWait() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("true & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void pipelineJobGivesLastStageStatus() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("exit 5 | cat & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void specialBuiltinErrorInJobFails() {
        Assertions.assertNotEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("shift 9 2>/dev/null & wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void exitTrapInJobSeesExitStatus() {
        Assertions.assertEquals(
            String.format("t5%n5%n"),
            Sh4j.INSTANCE.out(
                "{ trap 'echo \"t$?\"' EXIT; exit 5; } & wait \"$!\"; echo \"$?\""
            )
        );
    }

    @Test
    void exitInJobReportsStatusToRun() {
        Assertions.assertEquals(
            4,
            Sh4j.INSTANCE.run("exit 3 & wait \"$!\"; exit \"$(($? + 1))\"")
        );
    }
}
