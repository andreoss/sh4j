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
 * Tests for {@link Sh4j}: the kill utility signals every pid operand and
 * rejects an invalid signal or a missing operand before it signals anything.
 *
 * @since 1.0
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
final class PosixKillOperandsTest {

    @Test
    void killContinuesPastUnknownPid() {
        Assertions.assertEquals(
            String.format("143%n"),
            Sh4j.INSTANCE.out(
                "sleep 30 & p=\"$!\"; kill 999999 \"$p\" 2>/dev/null; wait \"$p\"; echo \"$?\""
            )
        );
    }

    @Test
    void killFailsWhenOneOfManyPidsIsUnknown() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    " ",
                    "sleep 30 & p=\"$!\"; kill 999999 \"$p\" 2>/dev/null; s=\"$?\";",
                    "kill \"$p\" 2>/dev/null; wait; echo \"$s\""
                )
            )
        );
    }

    @Test
    void killReportsEveryUnknownPid() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("kill 999999 999998").stderr().contains("999998")
        );
    }

    @Test
    void killSignalsEveryJobPid() {
        Assertions.assertEquals(
            String.format("143%n143%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    " ",
                    "sleep 30 & a=\"$!\"; sleep 30 & b=\"$!\"; kill \"$a\" \"$b\";",
                    "wait \"$a\"; echo \"$?\"; wait \"$b\"; echo \"$?\""
                )
            )
        );
    }

    @Test
    void killContinuesPastUnknownJobId() {
        Assertions.assertEquals(
            String.format("143%n"),
            Sh4j.INSTANCE.out(
                "sleep 30 & p=\"$!\"; kill %9 %1 2>/dev/null; wait \"$p\"; echo \"$?\""
            )
        );
    }

    @Test
    void killFailsWhenOneOfManyJobIdsIsUnknown() {
        Assertions.assertEquals(
            String.format("1%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    " ",
                    "sleep 30 & p=\"$!\"; kill %9 %1 2>/dev/null; s=\"$?\";",
                    "kill \"$p\" 2>/dev/null; wait; echo \"$s\""
                )
            )
        );
    }

    @Test
    void killInvalidSignalNameFailsWithUsageStatus() {
        Assertions.assertEquals(
            String.format("2%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    " ",
                    "sleep 30 & p=\"$!\"; kill -BOGUS \"$p\" 2>/dev/null; s=\"$?\";",
                    "kill \"$p\"; wait; echo \"$s\""
                )
            )
        );
    }

    @Test
    void killInvalidSignalNameSparesTarget() {
        Assertions.assertEquals(
            String.format("0%n"),
            Sh4j.INSTANCE.out(
                String.join(
                    " ",
                    "sleep 30 & p=\"$!\"; kill -s BOGUS \"$p\" 2>/dev/null;",
                    "kill -0 \"$p\"; s=\"$?\"; kill \"$p\"; wait; echo \"$s\""
                )
            )
        );
    }

    @Test
    void killInvalidSignalNameReportsName() {
        Assertions.assertTrue(
            Sh4j.INSTANCE.runFull("kill -s BOGUS 999999").stderr().contains("BOGUS")
        );
    }

    @Test
    void killInvalidSignalNumberFails() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("kill -s 999 999999"));
    }

    @Test
    void killLowercaseSignalNameIsAccepted() {
        Assertions.assertEquals(
            String.format("130%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill -s int \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killUserSignalNameIsAccepted() {
        Assertions.assertEquals(
            String.format("138%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill -USR1 \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killDoubleDashEndsOptions() {
        Assertions.assertEquals(
            String.format("143%n"),
            Sh4j.INSTANCE.out("sleep 30 & kill -- \"$!\"; wait \"$!\"; echo \"$?\"")
        );
    }

    @Test
    void killWithoutOperandsFails() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("kill"));
    }

    @Test
    void killWithoutOperandsReportsUsage() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull("kill").stderr().contains("usage"));
    }

    @Test
    void killSignalWithoutPidReportsUsage() {
        Assertions.assertTrue(Sh4j.INSTANCE.runFull("kill -s").stderr().contains("usage"));
    }

    @Test
    void killSignalWithoutPidFails() {
        Assertions.assertEquals(2, Sh4j.INSTANCE.run("kill -9"));
    }
}
