/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sh4j}: POSIX 2.9.2 exit in a command of a pipeline.
 *
 * @since 1.0
 */
final class PosixPipelineExitTest {

    @Test
    void exitInIfConditionOfFirstStageGivesLastStatus() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("if exit 9; then echo yes; else echo no; fi | cat; echo \"$?\"")
        );
    }

    @Test
    void exitInIfConditionOfLastStageGivesItsStatus() {
        Assertions.assertEquals(
            String.format("9%n"),
            Sh4j.INSTANCE.out(": | if exit 9; then echo y; else echo n; fi; echo \"$?\"")
        );
    }

    @Test
    void exitInIfConditionInBraceGroupStage() {
        Assertions.assertEquals(
            String.format("9%n"),
            Sh4j.INSTANCE.out("cat </dev/null | { if exit 9; then :; fi; }; echo \"$?\"")
        );
    }

    @Test
    void exitInWhileConditionInBraceGroupStage() {
        Assertions.assertEquals(
            String.format("4%n"),
            Sh4j.INSTANCE.out(": | { while exit 4; do :; done; }; echo \"$?\"")
        );
    }

    @Test
    void exitInWhileBodyStageEndsTheLoop() {
        Assertions.assertEquals(
            String.format("4%n"),
            Sh4j.INSTANCE.out(": | while :; do exit 4; done; echo \"$?\"")
        );
    }

    @Test
    void exitInUntilConditionStage() {
        Assertions.assertEquals(
            String.format("13%n"),
            Sh4j.INSTANCE.out(": | until exit 13; do echo body; done; echo \"$?\"")
        );
    }

    @Test
    void exitInForBodyStageEndsTheLoop() {
        Assertions.assertEquals(
            String.format("i1%n5%n"),
            Sh4j.INSTANCE.out(": | for i in 1 2; do echo \"i$i\"; exit 5; done; echo \"$?\"")
        );
    }

    @Test
    void exitInCaseStage() {
        Assertions.assertEquals(
            String.format("11%n"),
            Sh4j.INSTANCE.out(": | case x in x) exit 11; echo no;; esac; echo \"$?\"")
        );
    }

    @Test
    void exitInIfBodyStage() {
        Assertions.assertEquals(
            String.format("12%n"),
            Sh4j.INSTANCE.out(": | if true; then exit 12; echo no; fi; echo \"$?\"")
        );
    }

    @Test
    void exitInBraceGroupStageSkipsTheRest() {
        Assertions.assertEquals(
            String.format("3%n"),
            Sh4j.INSTANCE.out(": | { exit 3; echo no; }; echo \"$?\"")
        );
    }

    @Test
    void exitInFirstStageDoesNotEndTheShell() {
        Assertions.assertEquals(
            String.format("after%n"),
            Sh4j.INSTANCE.out("exit 5 | cat; echo after")
        );
    }

    @Test
    void exitInFunctionStage() {
        Assertions.assertEquals(
            String.format("6%n"),
            Sh4j.INSTANCE.out("f() { exit 6; }; : | f; echo \"$?\"")
        );
    }

    @Test
    void exitInFunctionInBraceGroupStage() {
        Assertions.assertEquals(
            String.format("6%n"),
            Sh4j.INSTANCE.out(": | { f() { exit 6; }; f; echo no; }; echo \"$?\"")
        );
    }

    @Test
    void errexitInLoopStage() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out(": | while :; do set -e; false; echo no; done; echo \"$?\"")
        );
    }

    @Test
    void exitTrapRunsOnceAfterExitInIfCondition() {
        Assertions.assertEquals(
            String.format("t%n8%n"),
            Sh4j.INSTANCE.out(": | { trap 'echo t' EXIT; if exit 8; then :; fi; }; echo \"$?\"")
        );
    }

    @Test
    void exitTrapSetInIfConditionStageRuns() {
        Assertions.assertEquals(
            String.format("t%n14%n"),
            Sh4j.INSTANCE.out(": | if trap 'echo t' EXIT; then exit 14; fi; echo \"$?\"")
        );
    }

    @Test
    void exitInSubshellStage() {
        Assertions.assertEquals(
            String.format("15%n"),
            Sh4j.INSTANCE.out(": | ( exit 15; echo no ); echo \"$?\"")
        );
    }

    @Test
    void breakInLoopStage() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out(": | while :; do break; done; echo \"$?\"")
        );
    }
}
