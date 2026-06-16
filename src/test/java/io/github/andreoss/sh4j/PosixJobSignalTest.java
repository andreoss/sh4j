/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link Sh4j}: a background job that receives a signal ends with
 * status 128 plus the signal, even when the signal lands while the job
 * starts, and the job runs in the environment of the moment it started.
 *
 * @since 1.0
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
final class PosixJobSignalTest {

    @RepeatedTest(50)
    void killSignalsEveryNewJob() {
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

    @RepeatedTest(50)
    void killedNewJobReportsNoError() {
        Assertions.assertEquals(
            "",
            Sh4j.INSTANCE.runFull("sleep 30 & kill \"$!\"; wait \"$!\"").stderr()
        );
    }

    @Test
    void backgroundJobKeepsVariablesOfItsStart() {
        Assertions.assertEquals(
            String.format("old%n"),
            Sh4j.INSTANCE.out("x=old; echo \"$x\" & x=new; wait")
        );
    }

    @Test
    void backgroundJobKeepsDirectoryOfItsStart() {
        Assertions.assertEquals(
            String.format("/%n"),
            Sh4j.INSTANCE.out("cd /; pwd & cd /usr; wait")
        );
    }
}
