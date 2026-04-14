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
 * Tests for {@link Parser}: parse clobber.
 *
 * @since 1.0
 */
final class ParserParseClobberTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    void parseClobberRedirectionFirst() {
        Assertions.assertTrue(
            this.command(this.parse("echo test >| file.txt")).getRedirections().size() >= 1
        );
    }

    @Test
    void parseClobberRedirectionSecond() {
        Assertions.assertTrue(
            this.command(this.parse("echo test >| file.txt")).getRedirections().stream().anyMatch(
                r -> r.getType() == RedirectionType.CLOBBER
            )
        );
    }

    @Test
    void parseCommandWithDollarFirst() {
        Assertions.assertEquals(
            ParserParseClobberTest.ECHO_CMD,
            this.command(this.parse("echo $VAR")).getName()
        );
    }

    @Test
    void parseCommandWithDollarSecond() {
        Assertions.assertEquals(
            "$VAR",
            this.command(this.parse("echo $VAR")).getArguments().get(0)
        );
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsFirst() {
        Assertions.assertEquals(
            5,
            this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getPipelines().size()
        );
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsSecond() {
        Assertions.assertEquals(
            ListNode.Separator.SEMI,
            this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getSeparators().get(0)
        );
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsThird() {
        Assertions.assertEquals(
            ListNode.Separator.AMP,
            this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getSeparators().get(1)
        );
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsFourth() {
        Assertions.assertEquals(
            ListNode.Separator.OR_IF,
            this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getSeparators().get(2)
        );
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsFifth() {
        Assertions.assertEquals(
            ListNode.Separator.AND_IF,
            this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getSeparators().get(3)
        );
    }

    @Test
    void parseEmptyPipeline() {
        Assertions.assertEquals(1, this.parse("").getPipelines().size());
    }

    @Test
    void parseSubshellFirst() {
        Assertions.assertEquals(1, this.parse("(echo hello)").getPipelines().size());
    }

    @Test
    void parseSubshellSecond() {
        Assertions.assertEquals(
            1,
            this.parse("(echo hello)").getPipelines().get(0).getCommands().size()
        );
    }

    @Test
    void parseSubshellThird() {
        Assertions.assertTrue(this.node("(echo hello)", 0) instanceof Subshell);
    }

    @Test
    void parseSubshellFourth() {
        Assertions.assertEquals(
            1,
            ((Subshell) this.node("(echo hello)", 0)).getList().getPipelines().size()
        );
    }

    @Test
    void parseSubshellFifth() {
        Assertions.assertEquals(
            ParserParseClobberTest.ECHO_CMD,
            this.command(((Subshell) this.node("(echo hello)", 0)).getList()).getName()
        );
    }

    @Test
    void parseSubshellWithMultiplePipelines() {
        Assertions.assertEquals(
            2,
            ((Subshell) this.node("(echo a; echo b)", 0)).getList().getPipelines().size()
        );
    }

    @Test
    void parseSubshellInPipelineFirst() {
        Assertions.assertEquals(3, this.pipelineOf("echo foo | (cat) | wc").getCommands().size());
    }

    @Test
    void parseSubshellInPipelineSecond() {
        Assertions.assertTrue(this.node("echo foo | (cat) | wc", 0) instanceof SimpleCommand);
    }

    @Test
    void parseSubshellInPipelineThird() {
        Assertions.assertTrue(this.node("echo foo | (cat) | wc", 1) instanceof Subshell);
    }

    @Test
    void parseSubshellInPipelineFourth() {
        Assertions.assertTrue(this.node("echo foo | (cat) | wc", 2) instanceof SimpleCommand);
    }

    @Test
    void parseGroupingFirst() {
        Assertions.assertEquals(1, this.parse("{ echo hello; }").getPipelines().size());
    }

    @Test
    void parseGroupingSecond() {
        Assertions.assertEquals(
            1,
            this.parse("{ echo hello; }").getPipelines().get(0).getCommands().size()
        );
    }

    @Test
    void parseGroupingThird() {
        Assertions.assertTrue(this.node("{ echo hello; }", 0) instanceof Grouping);
    }

    @Test
    void parseGroupingFourth() {
        Assertions.assertEquals(
            1,
            ((Grouping) this.node("{ echo hello; }", 0)).getList().getPipelines().size()
        );
    }

    @Test
    void parseGroupingFifth() {
        Assertions.assertEquals(
            ParserParseClobberTest.ECHO_CMD,
            this.command(((Grouping) this.node("{ echo hello; }", 0)).getList()).getName()
        );
    }

    @Test
    void parseGroupingInPipelineFirst() {
        Assertions.assertEquals(
            3,
            this.pipelineOf("echo foo | { cat; } | wc").getCommands().size()
        );
    }

    @Test
    void parseGroupingInPipelineSecond() {
        Assertions.assertTrue(this.node("echo foo | { cat; } | wc", 0) instanceof SimpleCommand);
    }

    @Test
    void parseGroupingInPipelineThird() {
        Assertions.assertTrue(this.node("echo foo | { cat; } | wc", 1) instanceof Grouping);
    }

    @Test
    void parseGroupingInPipelineFourth() {
        Assertions.assertTrue(this.node("echo foo | { cat; } | wc", 2) instanceof SimpleCommand);
    }

    @Test
    void parseExpectThrowsOnMissingWord() {
        Assertions.assertThrowsExactly(ParseException.class, () -> this.parse("echo <"));
    }

    @Test
    void parseCommandStartingWithInputRedirectFirst() {
        Assertions.assertEquals("<", this.command(this.parse("< input.txt cat")).getName());
    }

    @Test
    void parseCommandStartingWithInputRedirectSecond() {
        Assertions.assertEquals(
            2,
            this.command(this.parse("< input.txt cat")).getArguments().size()
        );
    }

    @Test
    void parseCommandStartingWithOutputRedirectFirst() {
        Assertions.assertEquals(">", this.command(this.parse("> out.txt echo")).getName());
    }

    @Test
    void parseCommandStartingWithOutputRedirectSecond() {
        Assertions.assertEquals(
            2,
            this.command(this.parse("> out.txt echo")).getArguments().size()
        );
    }

    @Test
    void parseCommandStartingWithClobberRedirectFirst() {
        Assertions.assertEquals(">|", this.command(this.parse(">| file.txt echo")).getName());
    }

    @Test
    void parseCommandStartingWithClobberRedirectSecond() {
        Assertions.assertEquals(
            2,
            this.command(this.parse(">| file.txt echo")).getArguments().size()
        );
    }

    @Test
    void parseCommandStartingWithAppendRedirectFirst() {
        Assertions.assertEquals(">>", this.command(this.parse(">> file.txt echo")).getName());
    }

    @Test
    void parseCommandStartingWithAppendRedirectSecond() {
        Assertions.assertEquals(
            2,
            this.command(this.parse(">> file.txt echo")).getArguments().size()
        );
    }

    @Test
    void parseAssignmentWordAfterCommandNameFirst() {
        Assertions.assertEquals(
            ParserParseClobberTest.ECHO_CMD,
            this.command(this.parse("echo FOO=bar")).getName()
        );
    }

    @Test
    void parseAssignmentWordAfterCommandNameSecond() {
        Assertions.assertTrue(
            this.command(this.parse("echo FOO=bar")).getArguments().contains("FOO=bar")
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
