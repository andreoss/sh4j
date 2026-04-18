/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link Parser}: quoted heredoc delimiter.
 *
 * @since 1.0
 */
final class ParserQuotedHeredocDelimiterTest {

    @ParameterizedTest
    @ValueSource(strings = {"'EOF'", "\"EOF\"", "\\EOF"})
    void quotedHeredocDelimiterTarget(final String delim) {
        Assertions.assertEquals(
            "EOF",
            this.heredocCommand(delim).getRedirections().get(0).getTarget()
        );
    }

    @Test
    void redirectionAppendConstructorFirst() {
        Assertions.assertEquals(2, new Redirection(2, RedirectionType.APPEND, "/tmp/log").getFd());
    }

    @Test
    void redirectionAppendConstructorSecond() {
        Assertions.assertEquals(
            RedirectionType.APPEND,
            new Redirection(2, RedirectionType.APPEND, "/tmp/log").getType()
        );
    }

    @Test
    void redirectionAppendConstructorThird() {
        Assertions.assertEquals(
            "/tmp/log",
            new Redirection(2, RedirectionType.APPEND, "/tmp/log").getTarget()
        );
    }

    @Test
    void redirectionDescriptorBoundariesFirst() {
        Assertions.assertEquals(2, this.commandOf("cmd 2>out").getRedirections().get(0).getFd());
    }

    @Test
    void redirectionDescriptorBoundariesSecond() {
        this.commandOf("cmd 2>out");
        Assertions.assertEquals(0, this.commandOf("cmd <in").getRedirections().get(0).getFd());
    }

    @Test
    void redirectionDescriptorBoundariesThird() {
        this.commandOf("cmd 2>out");
        this.commandOf("cmd <in");
        Assertions.assertEquals(
            RedirectionType.DUP_OUTPUT,
            this.commandOf("cmd 2>&1").getRedirections().get(0).getType()
        );
    }

    @Test
    void redirectionDescriptorBoundariesFourth() {
        this.commandOf("cmd 2>out");
        this.commandOf("cmd <in");
        this.commandOf("cmd 2>&1");
        Assertions.assertEquals(
            RedirectionType.READWRITE,
            this.commandOf("cmd <>file").getRedirections().get(0).getType()
        );
    }

    @Test
    void redirectionInputConstructorFirst() {
        Assertions.assertEquals(0, new Redirection(0, RedirectionType.INPUT, "/dev/null").getFd());
    }

    @Test
    void redirectionInputConstructorSecond() {
        Assertions.assertEquals(
            RedirectionType.INPUT,
            new Redirection(0, RedirectionType.INPUT, "/dev/null").getType()
        );
    }

    @Test
    void redirectionInputConstructorThird() {
        Assertions.assertEquals(
            "/dev/null",
            new Redirection(0, RedirectionType.INPUT, "/dev/null").getTarget()
        );
    }

    @Test
    void redirectionOutputConstructorFirst() {
        Assertions.assertEquals(1, new Redirection(1, RedirectionType.OUTPUT, "/tmp/out").getFd());
    }

    @Test
    void redirectionOutputConstructorSecond() {
        Assertions.assertEquals(
            RedirectionType.OUTPUT,
            new Redirection(1, RedirectionType.OUTPUT, "/tmp/out").getType()
        );
    }

    @Test
    void redirectionOutputConstructorThird() {
        Assertions.assertEquals(
            "/tmp/out",
            new Redirection(1, RedirectionType.OUTPUT, "/tmp/out").getTarget()
        );
    }

    @Test
    void redirectionWithoutTargetFails() {
        Assertions.assertThrows(ParseException.class, () -> this.parse("cat >"));
    }

    @Test
    void simpleCommandToStringWithoutName() {
        final SimpleCommand cmd = new SimpleCommand(null);
        cmd.addArgument("a");
        cmd.addArgument("b");
        Assertions.assertEquals("a b", cmd.toString());
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }

    private SimpleCommand command(final ListNode list) {
        return (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
    }

    private Node node(final String source, final int index) {
        return this.pipelineOf(source).getCommands().get(index);
    }

    private Pipeline pipelineOf(final String source) {
        return this.firstPipeline(this.parse(source));
    }

    private SimpleCommand commandOf(final String source) {
        return this.command(this.parse(source));
    }

    private Pipeline firstPipeline(final ListNode list) {
        return list.getPipelines().get(0);
    }

    private SimpleCommand heredocCommand(final String delim) {
        return (SimpleCommand) this.node(
            String.format("cat <<%s%nbody%nEOF%n", delim),
            0
        );
    }
}
