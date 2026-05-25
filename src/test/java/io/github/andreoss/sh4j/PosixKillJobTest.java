/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link Sh4j}: the kill utility signals the shell's own
 * asynchronous lists, POSIX 2.9.3.1 and the kill utility.
 *
 * @since 1.0
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
final class PosixKillJobTest {

    @Test
    void killPidTerminatesExternalJob() {
        Assertions.assertEquals(
            String.format("rc=143%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill \"$!\"; wait \"$!\"; echo \"rc=$?\"")
        );
    }

    @Test
    void killJobIdLetsWaitReturn() {
        Assertions.assertEquals(
            String.format("done%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill %1; wait; echo done")
        );
    }

    @Test
    void killSignalNameKillReportsKillStatus() {
        Assertions.assertEquals(
            String.format("137%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill -s KILL \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killSignalNumberNineReportsKillStatus() {
        Assertions.assertEquals(
            String.format("137%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill -9 \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killSignalIntReportsIntStatus() {
        Assertions.assertEquals(
            String.format("130%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill -INT \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killSubshellJobReportsTermStatus() {
        Assertions.assertEquals(
            String.format("143%n"),
            Sh4j.INSTANCE.out("(sleep 30) & kill \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killBraceGroupJobReportsTermStatus() {
        Assertions.assertEquals(
            String.format("143%n"),
            Sh4j.INSTANCE.out("{ sleep 30; } & kill \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killBraceGroupJobSkipsLaterCommands() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.out("{ sleep 30; echo late; } & kill \"$!\"; wait \"$!\"")
        );
    }

    @Test
    void killStopsBuiltinLoopJob() {
        Assertions.assertEquals(
            String.format("143%n"),
            Sh4j.INSTANCE.out("while :; do :; done & kill \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killStopsPipelineJob() {
        Assertions.assertEquals(
            String.format("143%n"),
            Sh4j.INSTANCE.out("sleep 30 | cat & kill \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killStopsBuiltinPipelineJob() {
        Assertions.assertEquals(
            String.format("143%n"),
            Sh4j.INSTANCE.out(
                "while :; do :; done | cat & kill \"$!\"; wait \"$!\"; echo \"$?\""
            )
        );
    }

    @Test
    void killOfRunningJobSucceeds() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill \"$!\"; s=\"$?\"; wait; echo \"$s\"")
        );
    }

    @Test
    void killZeroProbesRunningJob() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill -0 \"$!\"; echo \"$?\"; kill \"$!\"; wait")
        );
    }

    @Test
    void killZeroFailsAfterJobEnded() {
        Assertions.assertNotEquals(
            0,
            Sh4j.INSTANCE.run("true & wait \"$!\"; kill -0 \"$!\"")
        );
    }

    @Test
    void killFailsForUnknownJobId() {
        Assertions.assertNotEquals(0, Sh4j.INSTANCE.run("kill %9"));
    }

    @Test
    void killFailsForMalformedJobId() {
        Assertions.assertNotEquals(0, Sh4j.INSTANCE.run("kill %x"));
    }

    @Test
    void killUnknownPidReportsError() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("kill 999999").stderr().contains("999999")
        );
    }

    @Test
    void killStopsAtFirstUnknownPid() {
        Assertions.assertEquals(127, Sh4j.INSTANCE.run("kill 999999 999998"));
    }

    @Test
    void killUnknownSignalNameDefaultsToTerm() {
        Assertions.assertEquals(
            String.format("143%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill -s FOO \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killZeroProbesShellItself() {
        Assertions.assertEquals(0, Sh4j.INSTANCE.run("kill -0 \"$$\""));
    }

    @Test
    void killZeroProbesOtherProcess() {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(
                String.join(
                    " ",
                    "p=$(sh -c 'sleep 30 >/dev/null 2>&1 & echo \"$!\"');",
                    "kill -0 \"$p\"; s=\"$?\"; kill \"$p\"; exit \"$s\""
                )
            )
        );
    }

    @Test
    void killShellItselfRunsTrap() {
        Assertions.assertEquals(
            String.format("got%nafter%n"),
            Sh4j.INSTANCE.out("trap 'echo got' TERM; kill \"$$\"; echo after")
        );
    }

    @Test
    void killShellItselfWithoutTrapExits() {
        Assertions.assertEquals(143, Sh4j.INSTANCE.run("kill \"$$\"; echo after"));
    }

    @Test
    void killShellItselfWithNumberedSignalExits() {
        Assertions.assertEquals(138, Sh4j.INSTANCE.run("kill -10 \"$$\"; echo after"));
    }

    @Test
    void killTerminatesOtherProcess() {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(
                "p=$(sh -c 'sleep 30 >/dev/null 2>&1 & echo \"$!\"'); kill \"$p\""
            )
        );
    }

    @Test
    void killForciblyTerminatesOtherProcess() {
        Assertions.assertEquals(
            0,
            Sh4j.INSTANCE.run(
                "p=$(sh -c 'sleep 30 >/dev/null 2>&1 & echo \"$!\"'); kill -9 \"$p\""
            )
        );
    }
}
