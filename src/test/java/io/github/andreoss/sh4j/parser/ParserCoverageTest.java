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
            Integer.valueOf(this.parse("then").getPipelines().size()),
            CoreMatchers.equalTo(Integer.valueOf(1))
        );
    }

    @Test
    void heredocWithoutDescriptorDefaultsToStandardInput() {
        MatcherAssert.assertThat(
            Integer.valueOf(this.redirection("cat <<EOF%nbody%nEOF%n").getFd()),
            CoreMatchers.equalTo(Integer.valueOf(0))
        );
    }

    @Test
    void assignmentWordBeforeReservedWordSplitsIntoThreePipelines() {
        MatcherAssert.assertThat(
            Integer.valueOf(
                this.parse("a=1 if true; then :; fi").getPipelines().size()
            ),
            CoreMatchers.equalTo(Integer.valueOf(3))
        );
    }

    @Test
    void compoundNodesReportZeroAsTheirLine() {
        MatcherAssert.assertThat(
            Integer.valueOf(new IfNode(
                new ListNode(), new ListNode(), new ListNode()
            ).getLineNumber()),
            CoreMatchers.equalTo(Integer.valueOf(0))
        );
    }

    @Test
    void pipeFollowedByReservedWordKeepsBothStages() {
        MatcherAssert.assertThat(
            Integer.valueOf(
                this.parse("echo a | then").getPipelines().get(0)
                    .getCommands().size()
            ),
            CoreMatchers.equalTo(Integer.valueOf(2))
        );
    }

    @Test
    void semicolonBeforeDoIsAcceptedInWhileHead() {
        MatcherAssert.assertThat(
            Integer.valueOf(
                this.parse("while false; do :; done").getPipelines().size()
            ),
            CoreMatchers.equalTo(Integer.valueOf(1))
        );
    }

    @Test
    void ioNumberBeforeRedirectionIsParsed() {
        MatcherAssert.assertThat(
            Integer.valueOf(
                this.command("cat <<EOF%nbody%nEOF%n")
                    .getRedirections().size()
            ),
            CoreMatchers.equalTo(Integer.valueOf(1))
        );
    }

    private ListNode parse(final String source) {
        return new Parser(new Lexer(new CharReader(source))).parse();
    }

    private SimpleCommand command(final String source) {
        return (SimpleCommand) this.parse(source)
            .getPipelines().get(0).getCommands().get(0);
    }

    private Redirection redirection(final String source) {
        return this.command(source).getRedirections().get(0);
    }
}
