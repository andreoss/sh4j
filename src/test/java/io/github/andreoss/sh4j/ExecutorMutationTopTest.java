/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Executor}: top.
 *
 * @since 1.0
 */
final class ExecutorMutationTopTest {

    @Test
    void topLevelLoopAndReturnSignalsAreReportedFirst() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("break").exitCode(),
            CoreMatchers.equalTo(0)
        );
    }

    @Test
    void topLevelLoopAndReturnSignalsAreReportedSecond() {
        Sh4j.INSTANCE.runFull("break");
        CoreMatchers.equalTo(0);
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("break").stderr(),
            CoreMatchers.containsString("break: not in a loop")
        );
    }

    @Test
    void topLevelLoopAndReturnSignalsAreReportedThird() {
        Sh4j.INSTANCE.runFull("break");
        CoreMatchers.equalTo(0);
        Sh4j.INSTANCE.runFull("break");
        CoreMatchers.containsString("break: not in a loop");
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("continue").exitCode(),
            CoreMatchers.equalTo(0)
        );
    }

    @Test
    void topLevelLoopAndReturnSignalsAreReportedFourth() {
        Sh4j.INSTANCE.runFull("break");
        CoreMatchers.equalTo(0);
        Sh4j.INSTANCE.runFull("break");
        CoreMatchers.containsString("break: not in a loop");
        Sh4j.INSTANCE.runFull("continue");
        CoreMatchers.equalTo(0);
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("continue").stderr(),
            CoreMatchers.containsString("continue: not in a loop")
        );
    }

    @Test
    void topLevelLoopAndReturnSignalsAreReportedFifth() {
        Sh4j.INSTANCE.runFull("break");
        CoreMatchers.equalTo(0);
        Sh4j.INSTANCE.runFull("break");
        CoreMatchers.containsString("break: not in a loop");
        Sh4j.INSTANCE.runFull("continue");
        CoreMatchers.equalTo(0);
        Sh4j.INSTANCE.runFull("continue");
        CoreMatchers.containsString("continue: not in a loop");
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("return 5").exitCode(),
            CoreMatchers.equalTo(5)
        );
    }

    @Test
    void topLevelLoopAndReturnSignalsAreReportedSixth() {
        Sh4j.INSTANCE.runFull("break");
        CoreMatchers.equalTo(0);
        Sh4j.INSTANCE.runFull("break");
        CoreMatchers.containsString("break: not in a loop");
        Sh4j.INSTANCE.runFull("continue");
        CoreMatchers.equalTo(0);
        Sh4j.INSTANCE.runFull("continue");
        CoreMatchers.containsString("continue: not in a loop");
        Sh4j.INSTANCE.runFull("return 5");
        CoreMatchers.equalTo(5);
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("return 5").stderr(),
            CoreMatchers.containsString("return: not in a function")
        );
    }

    @Test
    void trapRuntimeErrorIsReportedFirst() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("trap 'if' EXIT; echo body").stdout(),
            CoreMatchers.equalTo(String.format("body%n"))
        );
    }

    @Test
    void trapRuntimeErrorIsReportedSecond() {
        Sh4j.INSTANCE.runFull("trap 'if' EXIT; echo body");
        CoreMatchers.equalTo(String.format("body%n"));
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.runFull("trap 'if' EXIT; echo body").stderr(),
            CoreMatchers.containsString("sh: trap:")
        );
    }
}
