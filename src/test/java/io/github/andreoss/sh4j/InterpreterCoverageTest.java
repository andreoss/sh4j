/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Covers interpreter branches that the existing suites leave unexercised.
 *
 * @since 1.0
 */
final class InterpreterCoverageTest {

    @Test
    void commandResolvesBeforeAnyAliasIsDefined() {
        MatcherAssert.assertThat(
            Integer.valueOf(Sh4j.INSTANCE.run("echo hi")),
            CoreMatchers.equalTo(Integer.valueOf(0))
        );
    }

    @Test
    void reservedWordAsArgumentIsNotACommand() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("if true; then echo yes; fi"),
            CoreMatchers.containsString("yes")
        );
    }

    @Test
    void arithmeticUnaryPlusAndMinusChain() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("echo $((+ -3 + 5))").trim(),
            CoreMatchers.equalTo("2")
        );
    }

    @Test
    void arithmeticBinaryOperatorAtEndOfInputIsError() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("echo $((1 *))"),
            CoreMatchers.equalTo("")
        );
    }

    @Test
    void fcListWithoutHistoryEntriesReportsFailure() {
        MatcherAssert.assertThat(
            Integer.valueOf(Sh4j.INSTANCE.run("fc -l")),
            CoreMatchers.equalTo(Integer.valueOf(1))
        );
    }

    @Test
    void reservedWordAfterPipeStillRunsTheLaterStage() {
        MatcherAssert.assertThat(
            Sh4j.INSTANCE.out("if true | then echo x; fi"),
            CoreMatchers.equalTo(String.format("x%n"))
        );
    }

    @Test
    void trailingReservedTokenInPipelineIsTreatedAsACommand() {
        MatcherAssert.assertThat(
            Integer.valueOf(Sh4j.INSTANCE.run("true | fi")),
            CoreMatchers.equalTo(Integer.valueOf(127))
        );
    }
}
