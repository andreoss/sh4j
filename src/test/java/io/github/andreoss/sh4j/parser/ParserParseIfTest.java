/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Parser}: parse if.
 *
 * @since 1.0
 */
final class ParserParseIfTest {

    @Test
    void parseIfInPipelineFirst() {
        Assertions.assertEquals(
            2,
            this.pipelineOf("true | if true; then echo hi; fi").getCommands().size()
        );
    }

    @Test
    void parseIfInPipelineSecond() {
        Assertions.assertTrue(
            this.node("true | if true; then echo hi; fi", 0) instanceof SimpleCommand
        );
    }

    @Test
    void parseIfInPipelineThird() {
        Assertions.assertTrue(this.node("true | if true; then echo hi; fi", 1) instanceof IfNode);
    }

    @Test
    void parseInlineAssignmentMidCommandFirst() {
        Assertions.assertEquals("cmd", this.command(this.parse("cmd FOO=1 arg")).getName());
    }

    @Test
    void parseInlineAssignmentMidCommandSecond() {
        Assertions.assertEquals(
            0,
            this.command(this.parse("cmd FOO=1 arg")).getAssignments().size()
        );
    }

    @Test
    void parseInlineAssignmentMidCommandThird() {
        Assertions.assertEquals(2, this.command(this.parse("cmd FOO=1 arg")).getArguments().size());
    }

    @Test
    void parseInlineAssignmentMidCommandFourth() {
        Assertions.assertEquals(
            "FOO=1",
            this.command(this.parse("cmd FOO=1 arg")).getArguments().get(0)
        );
    }

    @Test
    void parseInlineAssignmentMidCommandFifth() {
        Assertions.assertEquals(
            "arg",
            this.command(this.parse("cmd FOO=1 arg")).getArguments().get(1)
        );
    }

    @Test
    void parseMultipleAssignmentsThenCommandFirst() {
        Assertions.assertEquals("cmd", this.command(this.parse("FOO=1 BAR=2 cmd arg")).getName());
    }

    @Test
    void parseMultipleAssignmentsThenCommandSecond() {
        Assertions.assertEquals(
            2,
            this.command(this.parse("FOO=1 BAR=2 cmd arg")).getAssignments().size()
        );
    }

    @Test
    void parseMultipleAssignmentsThenCommandThird() {
        Assertions.assertEquals(
            "arg",
            this.command(this.parse("FOO=1 BAR=2 cmd arg")).getArguments().get(0)
        );
    }

    @Test
    void parseSubshellWithTrailingSemi() {
        Assertions.assertEquals(1, this.parse("(echo a;)").getPipelines().size());
    }

    @Test
    void parseTopLevelTrailingSemicolon() {
        Assertions.assertEquals(1, this.parse("echo a;").getPipelines().size());
    }

    @Test
    void parseTopLevelTrailingAmpersand() {
        Assertions.assertEquals(1, this.parse("sleep 0 &").getPipelines().size());
    }

    @Test
    void parseWhileInPipelineFirst() {
        Assertions.assertEquals(
            2,
            this.pipelineOf("true | while false; do echo hi; done").getCommands().size()
        );
    }

    @Test
    void parseWhileInPipelineSecond() {
        Assertions.assertTrue(
            this.node("true | while false; do echo hi; done", 1) instanceof WhileNode
        );
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

    private Pipeline firstPipeline(final ListNode list) {
        return list.getPipelines().get(0);
    }
}
