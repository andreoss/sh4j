/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Covers parser branches that ParserTest leaves unexercised.
 *
 * @since 1.0
 */
final class ParserCoverageTest {

    @Test
    void reservedWordAloneStillYieldsOnePipeline() {
        MatcherAssert.assertThat(
            Integer.valueOf(
                new Parser(new Lexer(new CharReader("then"))).parse()
                    .getPipelines().size()
            ),
            CoreMatchers.equalTo(Integer.valueOf(1))
        );
    }

    @Test
    void heredocWithoutDescriptorDefaultsToStandardInput() {
        final Node stage = new Parser(
            new Lexer(new CharReader("cat <<EOF%nbody%nEOF%n"))
        ).parse().getPipelines().get(0).getCommands().get(0);
        final SimpleCommand cmd = (SimpleCommand) stage;
        MatcherAssert.assertThat(
            Integer.valueOf(cmd.getRedirections().get(0).getFd()),
            CoreMatchers.equalTo(Integer.valueOf(0))
        );
    }

    @Test
    void assignmentWordBeforeReservedWordSplitsIntoThreePipelines() {
        MatcherAssert.assertThat(
            Integer.valueOf(
                new Parser(new Lexer(
                    new CharReader("a=1 if true; then :; fi")
                )).parse().getPipelines().size()
            ),
            CoreMatchers.equalTo(Integer.valueOf(3))
        );
    }

    @Test
    void compoundNodesReportZeroAsTheirLine() {
        final Node node = new IfNode(
            new ListNode(), new ListNode(), new ListNode()
        );
        MatcherAssert.assertThat(
            Integer.valueOf(node.getLineNumber()),
            CoreMatchers.equalTo(Integer.valueOf(0))
        );
    }

    @Test
    void pipeFollowedByReservedWordKeepsBothStages() {
        MatcherAssert.assertThat(
            Integer.valueOf(
                new Parser(new Lexer(
                    new CharReader("echo a | then")
                )).parse().getPipelines().get(0).getCommands().size()
            ),
            CoreMatchers.equalTo(Integer.valueOf(2))
        );
    }

    @Test
    void semicolonBeforeDoIsAcceptedInWhileHead() {
        MatcherAssert.assertThat(
            Integer.valueOf(
                new Parser(new Lexer(
                    new CharReader("while false; do :; done")
                )).parse().getPipelines().size()
            ),
            CoreMatchers.equalTo(Integer.valueOf(1))
        );
    }

    @Test
    void ioNumberBeforeRedirectionIsParsed() {
        final Node stage = new Parser(
            new Lexer(new CharReader("cat <<EOF%nbody%nEOF%n"))
        ).parse().getPipelines().get(0).getCommands().get(0);
        final SimpleCommand cmd = (SimpleCommand) stage;
        MatcherAssert.assertThat(
            Integer.valueOf(cmd.getRedirections().size()),
            CoreMatchers.equalTo(Integer.valueOf(1))
        );
    }
}
