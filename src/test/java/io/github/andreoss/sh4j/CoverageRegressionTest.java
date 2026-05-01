/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayInputStream;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Covers shell branches that the existing suites leave unexercised.
 *
 * @since 1.0
 */
final class CoverageRegressionTest {

    @Test
    void fcWithAssignmentSubstitutesTheMatchedCommand() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.stdout(
                new ByteArrayInputStream(new byte[0]),
                "echo hello",
                "fc -s hello=world"
            ),
            CoreMatchers.containsString("world")
        );
    }

    @Test
    void waitAfterBackgroundJobReturnsZero() {
        MatcherAssert.assertThat(
            Integer.valueOf(Sh4j.INSTANCE.run("sleep 0 & wait")),
            CoreMatchers.equalTo(Integer.valueOf(0))
        );
    }

    @Test
    void untilLoopTerminatesWhenConditionBecomesTrue() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("i=0; until [ $i -ge 2 ]; do i=$((i+1)); done; echo finished"),
            CoreMatchers.containsString("finished")
        );
    }

    @Test
    void killReportsUnknownPidAsFailure() {
        MatcherAssert.assertThat(
            Integer.valueOf(Sh4j.INSTANCE.run("kill 999999999")),
            CoreMatchers.equalTo(Integer.valueOf(127))
        );
    }

    @Test
    void getoptsIllegalOptionYieldsQuestionMark() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("set -- -z; getopts ab o; echo \"$o\""),
            CoreMatchers.containsString("?")
        );
    }
}
