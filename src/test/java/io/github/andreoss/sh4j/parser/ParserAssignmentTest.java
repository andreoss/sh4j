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
 * Tests for {@link Parser}: assignment.
 *
 * @since 1.0
 */
final class ParserAssignmentTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    void assignmentAndNamedCommandLineNumbersFirst() {
        Assertions.assertEquals(
            1,
            this.simple(String.format("X=1%necho hi%n"), 0, 0).getLineNumber()
        );
    }

    @Test
    void assignmentAndNamedCommandLineNumbersSecond() {
        Assertions.assertEquals(
            2,
            this.simple(String.format("X=1%necho hi%n"), 1, 0).getLineNumber()
        );
    }

    @Test
    void parseSimpleCommandFirst() {
        Assertions.assertEquals(1, this.parse("echo hello").getPipelines().size());
    }

    @Test
    void parseSimpleCommandSecond() {
        Assertions.assertEquals(
            1,
            this.parse("echo hello").getPipelines().get(0).getCommands().size()
        );
    }

    @Test
    void parseSimpleCommandThird() {
        Assertions.assertEquals(
            ParserAssignmentTest.ECHO_CMD,
            this.simple("echo hello", 0, 0).getName()
        );
    }

    @Test
    void parseSimpleCommandFourth() {
        Assertions.assertEquals(1, this.simple("echo hello", 0, 0).getArguments().size());
    }

    @Test
    void parseSimpleCommandFifth() {
        Assertions.assertEquals("hello", this.simple("echo hello", 0, 0).getArguments().get(0));
    }

    @Test
    void parsePipelineFirst() {
        Assertions.assertEquals(1, this.parse("echo a | grep b").getPipelines().size());
    }

    @Test
    void parsePipelineSecond() {
        Assertions.assertEquals(
            2,
            this.parse("echo a | grep b").getPipelines().get(0).getCommands().size()
        );
    }

    @Test
    void parsePipelineThird() {
        Assertions.assertEquals(
            ParserAssignmentTest.ECHO_CMD,
            this.simple("echo a | grep b", 0, 0).getName()
        );
    }

    @Test
    void parsePipelineFourth() {
        Assertions.assertEquals("grep", this.simple("echo a | grep b", 0, 1).getName());
    }

    @Test
    void parseListWithSemicolonFirst() {
        Assertions.assertEquals(2, this.parse("cmd1 ; cmd2").getPipelines().size());
    }

    @Test
    void parseListWithSemicolonSecond() {
        Assertions.assertEquals(
            ListNode.Separator.SEMI,
            this.parse("cmd1 ; cmd2").getSeparators().get(0)
        );
    }

    @Test
    void parseListWithAndFirst() {
        Assertions.assertEquals(2, this.parse("cmd1 && cmd2").getPipelines().size());
    }

    @Test
    void parseListWithAndSecond() {
        Assertions.assertEquals(
            ListNode.Separator.AND_IF,
            this.parse("cmd1 && cmd2").getSeparators().get(0)
        );
    }

    @Test
    void parseListWithOrFirst() {
        Assertions.assertEquals(2, this.parse("cmd1 || cmd2").getPipelines().size());
    }

    @Test
    void parseListWithOrSecond() {
        Assertions.assertEquals(
            ListNode.Separator.OR_IF,
            this.parse("cmd1 || cmd2").getSeparators().get(0)
        );
    }

    @Test
    void parseListWithAmpFirst() {
        Assertions.assertEquals(2, this.parse("cmd1 & cmd2").getPipelines().size());
    }

    @Test
    void parseListWithAmpSecond() {
        Assertions.assertEquals(
            ListNode.Separator.AMP,
            this.parse("cmd1 & cmd2").getSeparators().get(0)
        );
    }

    @Test
    void parseListWithNewlineSeparatorFirst() {
        Assertions.assertEquals(2, this.parse(String.format("cmd1%ncmd2")).getPipelines().size());
    }

    @Test
    void parseListWithNewlineSeparatorSecond() {
        Assertions.assertEquals(
            ListNode.Separator.SEMI,
            this.parse(String.format("cmd1%ncmd2")).getSeparators().get(0)
        );
    }

    @Test
    void parseListWithNewlineSeparatorThird() {
        Assertions.assertEquals("cmd1", this.simple(String.format("cmd1%ncmd2"), 0, 0).getName());
    }

    @Test
    void parseListWithNewlineSeparatorFourth() {
        Assertions.assertEquals("cmd2", this.simple(String.format("cmd1%ncmd2"), 1, 0).getName());
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlinesFirst() {
        Assertions.assertEquals(
            2,
            this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n")).getPipelines().size()
        );
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlinesSecond() {
        Assertions.assertEquals(
            "cmd1",
            this.simple(String.format("%n%ncmd1%n%n%ncmd2%n%n"), 0, 0).getName()
        );
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlinesThird() {
        Assertions.assertEquals(
            "cmd2",
            this.simple(String.format("%n%ncmd1%n%n%ncmd2%n%n"), 1, 0).getName()
        );
    }

    @Test
    void parseBang() {
        Assertions.assertTrue(this.parse("! echo hello").getPipelines().get(0).isBang());
    }

    @Test
    void parseCommandWithNoArgumentsFirst() {
        Assertions.assertEquals("ls", this.command(this.parse("ls")).getName());
    }

    @Test
    void parseCommandWithNoArgumentsSecond() {
        Assertions.assertTrue(this.command(this.parse("ls")).getArguments().isEmpty());
    }

    @Test
    void parseInputRedirectionFirst() {
        Assertions.assertEquals(
            1,
            this.command(this.parse("cat < file.txt")).getRedirections().size()
        );
    }

    @Test
    void parseInputRedirectionSecond() {
        Assertions.assertEquals(
            0,
            this.command(this.parse("cat < file.txt")).getRedirections().get(0).getFd()
        );
    }

    @Test
    void parseInputRedirectionThird() {
        Assertions.assertEquals(
            RedirectionType.INPUT,
            this.command(this.parse("cat < file.txt")).getRedirections().get(0).getType()
        );
    }

    @Test
    void parseInputRedirectionFourth() {
        Assertions.assertEquals(
            "file.txt",
            this.command(this.parse("cat < file.txt")).getRedirections().get(0).getTarget()
        );
    }

    @Test
    void parseOutputRedirectionFirst() {
        Assertions.assertEquals(
            1,
            this.command(this.parse("echo hello > out.txt")).getRedirections().size()
        );
    }

    @Test
    void parseOutputRedirectionSecond() {
        Assertions.assertEquals(
            1,
            this.command(this.parse("echo hello > out.txt")).getRedirections().get(0).getFd()
        );
    }

    @Test
    void parseOutputRedirectionThird() {
        Assertions.assertEquals(
            RedirectionType.OUTPUT,
            this.command(this.parse("echo hello > out.txt")).getRedirections().get(0).getType()
        );
    }

    @Test
    void parseOutputRedirectionFourth() {
        Assertions.assertEquals(
            "out.txt",
            this.command(this.parse("echo hello > out.txt")).getRedirections().get(0).getTarget()
        );
    }

    @Test
    void parseAppendRedirectionFirst() {
        Assertions.assertEquals(
            1,
            this.command(this.parse("echo hello >> out.txt")).getRedirections().size()
        );
    }

    @Test
    void parseAppendRedirectionSecond() {
        Assertions.assertEquals(
            RedirectionType.APPEND,
            this.command(this.parse("echo hello >> out.txt")).getRedirections().get(0).getType()
        );
    }

    @Test
    void parseAppendRedirectionThird() {
        Assertions.assertEquals(
            "out.txt",
            this.command(this.parse("echo hello >> out.txt")).getRedirections().get(0).getTarget()
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

    private SimpleCommand simple(final String source, final int pipe, final int index) {
        return (SimpleCommand) this.parse(source).getPipelines().get(pipe).getCommands().get(index);
    }
}
