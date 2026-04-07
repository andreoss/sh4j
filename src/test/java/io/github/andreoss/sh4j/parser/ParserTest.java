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
 * Tests for Parser.
 *
 * @since 1.0
 */
final class ParserTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    void parseSimpleCommandFirst() {
        Assertions.assertEquals(1, this.parse("echo hello").getPipelines().size());
    }

    @Test
    void parseSimpleCommandSecond() {
        Assertions.assertEquals(1, this.parse("echo hello").getPipelines().get(0).getCommands().size());
    }

    @Test
    void parseSimpleCommandThird() {
        Assertions.assertEquals(ParserTest.ECHO_CMD, ((SimpleCommand) this.parse("echo hello").getPipelines().get(0).getCommands().get(0)).getName());
    }

    @Test
    void parseSimpleCommandFourth() {
        Assertions.assertEquals(1, ((SimpleCommand) this.parse("echo hello").getPipelines().get(0).getCommands().get(0)).getArguments().size());
    }

    @Test
    void parseSimpleCommandFifth() {
        Assertions.assertEquals("hello", ((SimpleCommand) this.parse("echo hello").getPipelines().get(0).getCommands().get(0)).getArguments().get(0));
    }

    @Test
    void parsePipelineFirst() {
        Assertions.assertEquals(1, this.parse("echo a | grep b").getPipelines().size());
    }

    @Test
    void parsePipelineSecond() {
        Assertions.assertEquals(2, this.parse("echo a | grep b").getPipelines().get(0).getCommands().size());
    }

    @Test
    void parsePipelineThird() {
        Assertions.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) this.parse("echo a | grep b").getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    void parsePipelineFourth() {
        Assertions.assertEquals("grep", ((SimpleCommand) this.parse("echo a | grep b").getPipelines().get(0).getCommands().get(1)).getName());
    }

    @Test
    void parseListWithSemicolonFirst() {
        Assertions.assertEquals(2, this.parse("cmd1 ; cmd2").getPipelines().size());
    }

    @Test
    void parseListWithSemicolonSecond() {
        Assertions.assertEquals(ListNode.Separator.SEMI, this.parse("cmd1 ; cmd2").getSeparators().get(0));
    }

    @Test
    void parseListWithAndFirst() {
        Assertions.assertEquals(2, this.parse("cmd1 && cmd2").getPipelines().size());
    }

    @Test
    void parseListWithAndSecond() {
        Assertions.assertEquals(ListNode.Separator.AND_IF, this.parse("cmd1 && cmd2").getSeparators().get(0));
    }

    @Test
    void parseListWithOrFirst() {
        Assertions.assertEquals(2, this.parse("cmd1 || cmd2").getPipelines().size());
    }

    @Test
    void parseListWithOrSecond() {
        Assertions.assertEquals(ListNode.Separator.OR_IF, this.parse("cmd1 || cmd2").getSeparators().get(0));
    }

    @Test
    void parseListWithAmpFirst() {
        Assertions.assertEquals(2, this.parse("cmd1 & cmd2").getPipelines().size());
    }

    @Test
    void parseListWithAmpSecond() {
        Assertions.assertEquals(ListNode.Separator.AMP, this.parse("cmd1 & cmd2").getSeparators().get(0));
    }

    @Test
    void parseListWithNewlineSeparatorFirst() {
        Assertions.assertEquals(2, this.parse(String.format("cmd1%ncmd2")).getPipelines().size());
    }

    @Test
    void parseListWithNewlineSeparatorSecond() {
        Assertions.assertEquals(ListNode.Separator.SEMI, this.parse(String.format("cmd1%ncmd2")).getSeparators().get(0));
    }

    @Test
    void parseListWithNewlineSeparatorThird() {
        Assertions.assertEquals(
            "cmd1",
            ((SimpleCommand) this.parse(String.format("cmd1%ncmd2")).getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseListWithNewlineSeparatorFourth() {
        Assertions.assertEquals(
            "cmd2",
            ((SimpleCommand) this.parse(String.format("cmd1%ncmd2")).getPipelines().get(1).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlinesFirst() {
        Assertions.assertEquals(2, this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n")).getPipelines().size());
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlinesSecond() {
        Assertions.assertEquals(
            "cmd1",
            ((SimpleCommand) this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n")).getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlinesThird() {
        Assertions.assertEquals(
            "cmd2",
            ((SimpleCommand) this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n")).getPipelines().get(1).getCommands().get(0)).getName()
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
        Assertions.assertEquals(1, this.command(this.parse("cat < file.txt")).getRedirections().size());
    }

    @Test
    void parseInputRedirectionSecond() {
        Assertions.assertEquals(0, this.command(this.parse("cat < file.txt")).getRedirections().get(0).getFd());
    }

    @Test
    void parseInputRedirectionThird() {
        Assertions.assertEquals(RedirectionType.INPUT, this.command(this.parse("cat < file.txt")).getRedirections().get(0).getType());
    }

    @Test
    void parseInputRedirectionFourth() {
        Assertions.assertEquals("file.txt", this.command(this.parse("cat < file.txt")).getRedirections().get(0).getTarget());
    }

    @Test
    void parseOutputRedirectionFirst() {
        Assertions.assertEquals(1, this.command(this.parse("echo hello > out.txt")).getRedirections().size());
    }

    @Test
    void parseOutputRedirectionSecond() {
        Assertions.assertEquals(1, this.command(this.parse("echo hello > out.txt")).getRedirections().get(0).getFd());
    }

    @Test
    void parseOutputRedirectionThird() {
        Assertions.assertEquals(RedirectionType.OUTPUT, this.command(this.parse("echo hello > out.txt")).getRedirections().get(0).getType());
    }

    @Test
    void parseOutputRedirectionFourth() {
        Assertions.assertEquals("out.txt", this.command(this.parse("echo hello > out.txt")).getRedirections().get(0).getTarget());
    }

    @Test
    void parseAppendRedirectionFirst() {
        Assertions.assertEquals(1, this.command(this.parse("echo hello >> out.txt")).getRedirections().size());
    }

    @Test
    void parseAppendRedirectionSecond() {
        Assertions.assertEquals(RedirectionType.APPEND, this.command(this.parse("echo hello >> out.txt")).getRedirections().get(0).getType());
    }

    @Test
    void parseAppendRedirectionThird() {
        Assertions.assertEquals("out.txt", this.command(this.parse("echo hello >> out.txt")).getRedirections().get(0).getTarget());
    }

    @Test
    void parseClobberRedirectionFirst() {
        Assertions.assertTrue(this.command(this.parse("echo test >| file.txt")).getRedirections().size() >= 1);
    }

    @Test
    void parseClobberRedirectionSecond() {
        Assertions.assertTrue(
            this.command(this.parse("echo test >| file.txt")).getRedirections().stream().anyMatch(r -> r.getType() == RedirectionType.CLOBBER)
        );
    }

    @Test
    void parseCommandWithDollarFirst() {
        Assertions.assertEquals(ParserTest.ECHO_CMD, this.command(this.parse("echo $VAR")).getName());
    }

    @Test
    void parseCommandWithDollarSecond() {
        Assertions.assertEquals("$VAR", this.command(this.parse("echo $VAR")).getArguments().get(0));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsFirst() {
        Assertions.assertEquals(5, this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getPipelines().size());
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsSecond() {
        Assertions.assertEquals(ListNode.Separator.SEMI, this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getSeparators().get(0));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsThird() {
        Assertions.assertEquals(ListNode.Separator.AMP, this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getSeparators().get(1));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsFourth() {
        Assertions.assertEquals(ListNode.Separator.OR_IF, this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getSeparators().get(2));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsFifth() {
        Assertions.assertEquals(ListNode.Separator.AND_IF, this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5").getSeparators().get(3));
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
        Assertions.assertEquals(1, this.parse("(echo hello)").getPipelines().get(0).getCommands().size());
    }

    @Test
    void parseSubshellThird() {
        Assertions.assertTrue(this.parse("(echo hello)").getPipelines().get(0).getCommands().get(0) instanceof Subshell);
    }

    @Test
    void parseSubshellFourth() {
        Assertions.assertEquals(1, ((Subshell) this.parse("(echo hello)").getPipelines().get(0).getCommands().get(0)).getList().getPipelines().size());
    }

    @Test
    void parseSubshellFifth() {
        Assertions.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) ((Subshell) this.parse("(echo hello)").getPipelines().get(0).getCommands().get(0)).getList().getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseSubshellWithMultiplePipelines() {
        Assertions.assertEquals(
            2,
            (
                (Subshell) this.parse("(echo a; echo b)").getPipelines().get(0).getCommands().get(0)
            ).getList().getPipelines().size()
        );
    }

    @Test
    void parseSubshellInPipelineFirst() {
        Assertions.assertEquals(3, this.pipelineOf("echo foo | (cat) | wc").getCommands().size());
    }

    @Test
    void parseSubshellInPipelineSecond() {
        Assertions.assertTrue(this.pipelineOf("echo foo | (cat) | wc").getCommands().get(0) instanceof SimpleCommand);
    }

    @Test
    void parseSubshellInPipelineThird() {
        Assertions.assertTrue(this.pipelineOf("echo foo | (cat) | wc").getCommands().get(1) instanceof Subshell);
    }

    @Test
    void parseSubshellInPipelineFourth() {
        Assertions.assertTrue(this.pipelineOf("echo foo | (cat) | wc").getCommands().get(2) instanceof SimpleCommand);
    }

    @Test
    void parseGroupingFirst() {
        Assertions.assertEquals(1, this.parse("{ echo hello; }").getPipelines().size());
    }

    @Test
    void parseGroupingSecond() {
        Assertions.assertEquals(1, this.parse("{ echo hello; }").getPipelines().get(0).getCommands().size());
    }

    @Test
    void parseGroupingThird() {
        Assertions.assertTrue(this.parse("{ echo hello; }").getPipelines().get(0).getCommands().get(0) instanceof Grouping);
    }

    @Test
    void parseGroupingFourth() {
        Assertions.assertEquals(1, ((Grouping) this.parse("{ echo hello; }").getPipelines().get(0).getCommands().get(0)).getList().getPipelines().size());
    }

    @Test
    void parseGroupingFifth() {
        Assertions.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) ((Grouping) this.parse("{ echo hello; }").getPipelines().get(0).getCommands().get(0)).getList().getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseGroupingInPipelineFirst() {
        Assertions.assertEquals(3, this.pipelineOf("echo foo | { cat; } | wc").getCommands().size());
    }

    @Test
    void parseGroupingInPipelineSecond() {
        Assertions.assertTrue(this.pipelineOf("echo foo | { cat; } | wc").getCommands().get(0) instanceof SimpleCommand);
    }

    @Test
    void parseGroupingInPipelineThird() {
        Assertions.assertTrue(this.pipelineOf("echo foo | { cat; } | wc").getCommands().get(1) instanceof Grouping);
    }

    @Test
    void parseGroupingInPipelineFourth() {
        Assertions.assertTrue(this.pipelineOf("echo foo | { cat; } | wc").getCommands().get(2) instanceof SimpleCommand);
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
        Assertions.assertEquals(2, this.command(this.parse("< input.txt cat")).getArguments().size());
    }

    @Test
    void parseCommandStartingWithOutputRedirectFirst() {
        Assertions.assertEquals(">", this.command(this.parse("> out.txt echo")).getName());
    }

    @Test
    void parseCommandStartingWithOutputRedirectSecond() {
        Assertions.assertEquals(2, this.command(this.parse("> out.txt echo")).getArguments().size());
    }

    @Test
    void parseCommandStartingWithClobberRedirectFirst() {
        Assertions.assertEquals(">|", this.command(this.parse(">| file.txt echo")).getName());
    }

    @Test
    void parseCommandStartingWithClobberRedirectSecond() {
        Assertions.assertEquals(2, this.command(this.parse(">| file.txt echo")).getArguments().size());
    }

    @Test
    void parseCommandStartingWithAppendRedirectFirst() {
        Assertions.assertEquals(">>", this.command(this.parse(">> file.txt echo")).getName());
    }

    @Test
    void parseCommandStartingWithAppendRedirectSecond() {
        Assertions.assertEquals(2, this.command(this.parse(">> file.txt echo")).getArguments().size());
    }

    @Test
    void parseAssignmentWordAfterCommandNameFirst() {
        Assertions.assertEquals(ParserTest.ECHO_CMD, this.command(this.parse("echo FOO=bar")).getName());
    }

    @Test
    void parseAssignmentWordAfterCommandNameSecond() {
        Assertions.assertTrue(this.command(this.parse("echo FOO=bar")).getArguments().contains("FOO=bar"));
    }

    @Test
    void parseAssignmentWordAfterCommandNameThird() {
        Assertions.assertTrue(this.command(this.parse("echo FOO=bar")).getAssignments().isEmpty());
    }

    @Test
    void parseIoNumberRedirectFirst() {
        Assertions.assertEquals(1, this.command(this.parse("2>file")).getRedirections().size());
    }

    @Test
    void parseIoNumberRedirectSecond() {
        Assertions.assertEquals("file", this.command(this.parse("2>file")).getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupInputRedirectFirst() {
        Assertions.assertEquals("cmd", this.command(this.parse("cmd <&3")).getName());
    }

    @Test
    void parseDupInputRedirectSecond() {
        Assertions.assertEquals(1, this.command(this.parse("cmd <&3")).getRedirections().size());
    }

    @Test
    void parseDupInputRedirectThird() {
        Assertions.assertEquals(RedirectionType.DUP_INPUT, this.command(this.parse("cmd <&3")).getRedirections().get(0).getType());
    }

    @Test
    void parseDupInputRedirectFourth() {
        Assertions.assertEquals("3", this.command(this.parse("cmd <&3")).getRedirections().get(0).getTarget());
    }

    @Test
    void parseCloseInputRedirectFirst() {
        Assertions.assertEquals(1, this.command(this.parse("cmd <&-")).getRedirections().size());
    }

    @Test
    void parseCloseInputRedirectSecond() {
        Assertions.assertEquals(RedirectionType.DUP_INPUT, this.command(this.parse("cmd <&-")).getRedirections().get(0).getType());
    }

    @Test
    void parseCloseInputRedirectThird() {
        Assertions.assertEquals("-", this.command(this.parse("cmd <&-")).getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupOutputRedirectFirst() {
        Assertions.assertEquals(1, this.command(this.parse("cmd >&2")).getRedirections().size());
    }

    @Test
    void parseDupOutputRedirectSecond() {
        Assertions.assertEquals(RedirectionType.DUP_OUTPUT, this.command(this.parse("cmd >&2")).getRedirections().get(0).getType());
    }

    @Test
    void parseDupOutputRedirectThird() {
        Assertions.assertEquals("2", this.command(this.parse("cmd >&2")).getRedirections().get(0).getTarget());
    }

    @Test
    void parseReadWriteRedirectFirst() {
        Assertions.assertEquals(1, this.command(this.parse("cmd <>file")).getRedirections().size());
    }

    @Test
    void parseReadWriteRedirectSecond() {
        Assertions.assertEquals(RedirectionType.READWRITE, this.command(this.parse("cmd <>file")).getRedirections().get(0).getType());
    }

    @Test
    void parseReadWriteRedirectThird() {
        Assertions.assertEquals("file", this.command(this.parse("cmd <>file")).getRedirections().get(0).getTarget());
    }

    @Test
    void parseIfThenFiFirst() {
        Assertions.assertEquals(1, this.parse("if true; then echo hi; fi").getPipelines().size());
    }

    @Test
    void parseIfThenFiSecond() {
        Assertions.assertEquals(1, this.parse("if true; then echo hi; fi").getPipelines().get(0).getCommands().size());
    }

    @Test
    void parseIfThenFiThird() {
        Assertions.assertTrue(this.parse("if true; then echo hi; fi").getPipelines().get(0).getCommands().get(0) instanceof IfNode);
    }

    @Test
    void parseIfThenElseFiFirst() {
        Assertions.assertTrue(this.pipelineOf("if false; then echo a; else echo b; fi").getCommands().get(0) instanceof IfNode);
    }

    @Test
    void parseIfThenElseFiSecond() {
        Assertions.assertNotNull(((IfNode) this.pipelineOf("if false; then echo a; else echo b; fi").getCommands().get(0)).getElseBody());
    }

    @Test
    void parseForDoDoneFirst() {
        Assertions.assertEquals(1, this.pipelineOf("for var in a b c; do echo $var; done").getCommands().size());
    }

    @Test
    void parseForDoDoneSecond() {
        Assertions.assertTrue(this.pipelineOf("for var in a b c; do echo $var; done").getCommands().get(0) instanceof ForNode);
    }

    @Test
    void parseForDoDoneThird() {
        Assertions.assertEquals("var", ((ForNode) this.pipelineOf("for var in a b c; do echo $var; done").getCommands().get(0)).getVariable());
    }

    @Test
    void parseForDoDoneFourth() {
        Assertions.assertEquals(3, ((ForNode) this.pipelineOf("for var in a b c; do echo $var; done").getCommands().get(0)).getWords().size());
    }

    @Test
    void parseForDoDoneFifth() {
        Assertions.assertEquals("a", ((ForNode) this.pipelineOf("for var in a b c; do echo $var; done").getCommands().get(0)).getWords().get(0));
    }

    @Test
    void parseForDoDoneSixth() {
        Assertions.assertEquals("c", ((ForNode) this.pipelineOf("for var in a b c; do echo $var; done").getCommands().get(0)).getWords().get(2));
    }

    @Test
    void parseWhileDoDoneFirst() {
        Assertions.assertTrue(this.pipelineOf("while true; do echo hi; done").getCommands().get(0) instanceof WhileNode);
    }

    @Test
    void parseWhileDoDoneSecond() {
        Assertions.assertFalse(((WhileNode) this.pipelineOf("while true; do echo hi; done").getCommands().get(0)).isUntil());
    }

    @Test
    void parseUntilDoDoneFirst() {
        Assertions.assertTrue(this.pipelineOf("until false; do echo hi; done").getCommands().get(0) instanceof WhileNode);
    }

    @Test
    void parseUntilDoDoneSecond() {
        Assertions.assertTrue(((WhileNode) this.pipelineOf("until false; do echo hi; done").getCommands().get(0)).isUntil());
    }

    @Test
    void parseCaseSimpleFirst() {
        Assertions.assertTrue(this.pipelineOf("case foo in pat) echo match;; esac").getCommands().get(0) instanceof CaseNode);
    }

    @Test
    void parseCaseSimpleSecond() {
        Assertions.assertEquals("foo", ((CaseNode) this.pipelineOf("case foo in pat) echo match;; esac").getCommands().get(0)).getWord());
    }

    @Test
    void parseCaseSimpleThird() {
        Assertions.assertEquals(1, ((CaseNode) this.pipelineOf("case foo in pat) echo match;; esac").getCommands().get(0)).getItems().size());
    }

    @Test
    void parseCaseSimpleFourth() {
        Assertions.assertEquals("pat", ((CaseNode) this.pipelineOf("case foo in pat) echo match;; esac").getCommands().get(0)).getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseCasePipePatternFirst() {
        Assertions.assertEquals(2, ((CaseNode) this.pipelineOf("case foo in a|b) echo match;; esac").getCommands().get(0)).getItems().get(0).getPatterns().size());
    }

    @Test
    void parseCasePipePatternSecond() {
        Assertions.assertEquals("a", ((CaseNode) this.pipelineOf("case foo in a|b) echo match;; esac").getCommands().get(0)).getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseCasePipePatternThird() {
        Assertions.assertEquals("b", ((CaseNode) this.pipelineOf("case foo in a|b) echo match;; esac").getCommands().get(0)).getItems().get(0).getPatterns().get(1));
    }

    @Test
    void parseCaseMultipleItems() {
        Assertions.assertEquals(
            2,
            ((CaseNode) this.parse("case foo in a) echo a;; b) echo b;; esac")
                .getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    void parseCaseEmptyBody() {
        Assertions.assertEquals(
            1,
            ((CaseNode) this.parse("case foo in a);; esac")
                .getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    void parseCaseParenPattern() {
        Assertions.assertEquals(
            "pat",
            ((CaseNode) this.parse("case foo in (pat) echo match;; esac")
                .getPipelines().get(0).getCommands().get(0))
                .getItems().get(0).getPatterns().get(0)
        );
    }

    @Test
    void parseFunctionDefinitionFirst() {
        Assertions.assertTrue(this.pipelineOf("foo() { echo bar; }").getCommands().get(0) instanceof FunctionNode);
    }

    @Test
    void parseFunctionDefinitionSecond() {
        Assertions.assertEquals("foo", ((FunctionNode) this.pipelineOf("foo() { echo bar; }").getCommands().get(0)).getName());
    }

    @Test
    void parseFunctionSubshellBodyFirst() {
        Assertions.assertEquals("foo", ((FunctionNode) this.pipelineOf("foo() (echo bar)").getCommands().get(0)).getName());
    }

    @Test
    void parseFunctionSubshellBodySecond() {
        Assertions.assertTrue(((FunctionNode) this.pipelineOf("foo() (echo bar)").getCommands().get(0)).getBody() instanceof Subshell);
    }

    @Test
    void parseIfInPipelineFirst() {
        Assertions.assertEquals(2, this.pipelineOf("true | if true; then echo hi; fi").getCommands().size());
    }

    @Test
    void parseIfInPipelineSecond() {
        Assertions.assertTrue(this.pipelineOf("true | if true; then echo hi; fi").getCommands().get(0) instanceof SimpleCommand);
    }

    @Test
    void parseIfInPipelineThird() {
        Assertions.assertTrue(this.pipelineOf("true | if true; then echo hi; fi").getCommands().get(1) instanceof IfNode);
    }

    @Test
    void parseForInPipelineFirst() {
        Assertions.assertEquals(2, this.pipelineOf("true | for var in a; do echo $var; done").getCommands().size());
    }

    @Test
    void parseForInPipelineSecond() {
        Assertions.assertTrue(this.pipelineOf("true | for var in a; do echo $var; done").getCommands().get(1) instanceof ForNode);
    }

    @Test
    void parseWhileInPipelineFirst() {
        Assertions.assertEquals(2, this.pipelineOf("true | while false; do echo hi; done").getCommands().size());
    }

    @Test
    void parseWhileInPipelineSecond() {
        Assertions.assertTrue(this.pipelineOf("true | while false; do echo hi; done").getCommands().get(1) instanceof WhileNode);
    }

    @Test
    void parseCaseInPipelineFirst() {
        Assertions.assertEquals(2, this.pipelineOf("true | case foo in pat) echo hi;; esac").getCommands().size());
    }

    @Test
    void parseCaseInPipelineSecond() {
        Assertions.assertTrue(this.pipelineOf("true | case foo in pat) echo hi;; esac").getCommands().get(1) instanceof CaseNode);
    }

    @Test
    void parseFunctionInPipelineFirst() {
        Assertions.assertEquals(2, this.pipelineOf("true | foo() { echo bar; }").getCommands().size());
    }

    @Test
    void parseFunctionInPipelineSecond() {
        Assertions.assertTrue(this.pipelineOf("true | foo() { echo bar; }").getCommands().get(1) instanceof FunctionNode);
    }

    @Test
    void parseExpectWordThrows() {
        Assertions.assertThrowsExactly(ParseException.class, () -> this.parse("if true; then echo hi"));
    }

    @Test
    void parseFunctionBodyWithIf() {
        Assertions.assertTrue(
            this.parse("foo() if true; then echo hi; fi").getPipelines().get(0).getCommands().get(0)
                instanceof FunctionNode
        );
    }

    @Test
    void parseFunctionBodyWithFor() {
        Assertions.assertTrue(
            this.parse(
                "foo() for var in a; do echo $var; done"
            ).getPipelines().get(0).getCommands().get(0)
                instanceof FunctionNode
        );
    }

    @Test
    void parseFunctionBodyWithWhile() {
        Assertions.assertTrue(
            this.parse(
                "foo() while false; do echo hi; done"
            ).getPipelines().get(0).getCommands().get(0)
                instanceof FunctionNode
        );
    }

    @Test
    void parseFunctionBodyWithCase() {
        Assertions.assertTrue(
            this.parse(
                "foo() case foo in pat) echo hi;; esac"
            ).getPipelines().get(0).getCommands().get(0)
                instanceof FunctionNode
        );
    }

    @Test
    void parseFunctionBodyException() {
        Assertions.assertThrowsExactly(ParseException.class, () -> this.parse("foo() bar"));
    }

    @Test
    void parseEmptyWhileCondition() {
        Assertions.assertEquals(1, this.parse("while do; echo hi; done").getPipelines().size());
    }

    @Test
    void parseAssignmentWithoutCommandFirst() {
        Assertions.assertEquals("", this.command(this.parse("FOO=bar")).getName());
    }

    @Test
    void parseAssignmentWithoutCommandSecond() {
        Assertions.assertEquals(1, this.command(this.parse("FOO=bar")).getAssignments().size());
    }

    @Test
    void parseAssignmentWithoutCommandThird() {
        Assertions.assertEquals("FOO=bar", this.command(this.parse("FOO=bar")).getAssignments().get(0));
    }

    @Test
    void parseMultipleAssignmentsThenCommandFirst() {
        Assertions.assertEquals("cmd", this.command(this.parse("FOO=1 BAR=2 cmd arg")).getName());
    }

    @Test
    void parseMultipleAssignmentsThenCommandSecond() {
        Assertions.assertEquals(2, this.command(this.parse("FOO=1 BAR=2 cmd arg")).getAssignments().size());
    }

    @Test
    void parseMultipleAssignmentsThenCommandThird() {
        Assertions.assertEquals("arg", this.command(this.parse("FOO=1 BAR=2 cmd arg")).getArguments().get(0));
    }

    @Test
    void parseInlineAssignmentMidCommandFirst() {
        Assertions.assertEquals("cmd", this.command(this.parse("cmd FOO=1 arg")).getName());
    }

    @Test
    void parseInlineAssignmentMidCommandSecond() {
        Assertions.assertEquals(0, this.command(this.parse("cmd FOO=1 arg")).getAssignments().size());
    }

    @Test
    void parseInlineAssignmentMidCommandThird() {
        Assertions.assertEquals(2, this.command(this.parse("cmd FOO=1 arg")).getArguments().size());
    }

    @Test
    void parseInlineAssignmentMidCommandFourth() {
        Assertions.assertEquals("FOO=1", this.command(this.parse("cmd FOO=1 arg")).getArguments().get(0));
    }

    @Test
    void parseInlineAssignmentMidCommandFifth() {
        Assertions.assertEquals("arg", this.command(this.parse("cmd FOO=1 arg")).getArguments().get(1));
    }

    @Test
    void parseElifChainNestedElifFirst() {
        Assertions.assertEquals(1, this.parse(
            "if false; then a; elif false; then b; elif true; then c; fi"
        ).getPipelines().size());
    }

    @Test
    void parseElifChainNestedElifSecond() {
        Assertions.assertTrue(this.parse(
            "if false; then a; elif false; then b; elif true; then c; fi"
        ).getPipelines().get(0).getCommands().get(0) instanceof IfNode);
    }

    @Test
    void parseElifChainWithElse() {
        Assertions.assertEquals(
            1,
            this.parse("if false; then a; elif false; then b; else c; fi").getPipelines().size()
        );
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
    void parseSubshellWithTrailingSemi() {
        Assertions.assertEquals(1, this.parse("(echo a;)").getPipelines().size());
    }

    @Test
    void parseGroupingWithTrailingSemi() {
        Assertions.assertEquals(1, this.parse("{ echo a; }").getPipelines().size());
    }

    @Test
    void parseEmptyIfThenBody() {
        Assertions.assertEquals(1, this.parse("if true; then fi").getPipelines().size());
    }

    @Test
    void parseEmptyForBody() {
        Assertions.assertEquals(1, this.parse("for i in 1 2; do done").getPipelines().size());
    }

    @Test
    void parseTwoPipelinesSemiTerminatedBeforeReserved() {
        Assertions.assertEquals(
            1,
            this.parse("if true; then echo a; echo b; fi").getPipelines().size()
        );
    }

    @Test
    void parsePipelineEmptyPipeStage() {
        Assertions.assertEquals(
            1,
            this.parse("if echo a | echo b; then echo c; fi").getPipelines().size()
        );
    }

    @Test
    void parseHereDocRedirectFirst() {
        Assertions.assertEquals(1, this.command(
            this.parse(String.format("cat <<EOF%nhello%nEOF%n"))
        ).getRedirections().size());
    }

    @Test
    void parseHereDocRedirectSecond() {
        Assertions.assertEquals(RedirectionType.HERE_DOC, this.command(
            this.parse(String.format("cat <<EOF%nhello%nEOF%n"))
        ).getRedirections().get(0).getType());
    }

    @Test
    void parseCaseMultilineBodyFirst() {
        Assertions.assertEquals(1, this.parse(
            String.format("case x in%n  a) echo aa;;%n  b) echo bb;;%nesac")
        ).getPipelines().size());
    }

    @Test
    void parseCaseMultilineBodySecond() {
        Assertions.assertEquals(
            2,
            ((CaseNode) this.parse(
                            String.format("case x in%n  a) echo aa;;%n  b) echo bb;;%nesac")
                        ).getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    void parseCaseMultilineWithBlankLinesBetweenArmsFirst() {
        Assertions.assertEquals(1, this.parse(
            String.format("case y in%n%n  a) echo aa;;%n%n  b) echo bb;;%n%nesac")
        ).getPipelines().size());
    }

    @Test
    void parseCaseMultilineWithBlankLinesBetweenArmsSecond() {
        Assertions.assertEquals(
            2,
            ((CaseNode) this.parse(
                            String.format("case y in%n%n  a) echo aa;;%n%n  b) echo bb;;%n%nesac")
                        ).getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    void parseHereDocStripTabsFirst() {
        Assertions.assertEquals(1, this.command(
            this.parse(String.format("cat <<-EOF%n\thello%nEOF%n"))
        ).getRedirections().size());
    }

    @Test
    void parseHereDocStripTabsSecond() {
        Assertions.assertEquals(RedirectionType.HERE_DOC, this.command(
            this.parse(String.format("cat <<-EOF%n\thello%nEOF%n"))
        ).getRedirections().get(0).getType());
    }

    @Test
    void parseSeparatorsInListFirst() {
        Assertions.assertEquals(4, this.parse("cmd1 & cmd2 && cmd3 || cmd4").getPipelines().size());
    }

    @Test
    void parseSeparatorsInListSecond() {
        Assertions.assertEquals(ListNode.Separator.AMP, this.parse("cmd1 & cmd2 && cmd3 || cmd4").getSeparators().get(0));
    }

    @Test
    void parseSeparatorsInListThird() {
        Assertions.assertEquals(ListNode.Separator.AND_IF, this.parse("cmd1 & cmd2 && cmd3 || cmd4").getSeparators().get(1));
    }

    @Test
    void parseSeparatorsInListFourth() {
        Assertions.assertEquals(ListNode.Separator.OR_IF, this.parse("cmd1 & cmd2 && cmd3 || cmd4").getSeparators().get(2));
    }

    @Test
    void parseWhileWithAmpInCondition() {
        Assertions.assertEquals(
            1,
            this.parse("while true & false; do echo hi; done").getPipelines().size()
        );
    }

    @Test
    void parseWhileWithAndIfInCondition() {
        Assertions.assertEquals(
            1,
            this.parse("while true && false; do echo hi; done").getPipelines().size()
        );
    }

    @Test
    void parseWhileWithOrIfInCondition() {
        Assertions.assertEquals(
            1,
            this.parse("while true || false; do echo hi; done").getPipelines().size()
        );
    }

    @Test
    void redirectionInputConstructorFirst() {
        Assertions.assertEquals(0, new Redirection(0, RedirectionType.INPUT, "/dev/null").getFd());
    }

    @Test
    void redirectionInputConstructorSecond() {
        Assertions.assertEquals(RedirectionType.INPUT, new Redirection(0, RedirectionType.INPUT, "/dev/null").getType());
    }

    @Test
    void redirectionInputConstructorThird() {
        Assertions.assertEquals("/dev/null", new Redirection(0, RedirectionType.INPUT, "/dev/null").getTarget());
    }

    @Test
    void redirectionOutputConstructorFirst() {
        Assertions.assertEquals(1, new Redirection(1, RedirectionType.OUTPUT, "/tmp/out").getFd());
    }

    @Test
    void redirectionOutputConstructorSecond() {
        Assertions.assertEquals(RedirectionType.OUTPUT, new Redirection(1, RedirectionType.OUTPUT, "/tmp/out").getType());
    }

    @Test
    void redirectionOutputConstructorThird() {
        Assertions.assertEquals("/tmp/out", new Redirection(1, RedirectionType.OUTPUT, "/tmp/out").getTarget());
    }

    @Test
    void redirectionAppendConstructorFirst() {
        Assertions.assertEquals(2, new Redirection(2, RedirectionType.APPEND, "/tmp/log").getFd());
    }

    @Test
    void redirectionAppendConstructorSecond() {
        Assertions.assertEquals(RedirectionType.APPEND, new Redirection(2, RedirectionType.APPEND, "/tmp/log").getType());
    }

    @Test
    void redirectionAppendConstructorThird() {
        Assertions.assertEquals("/tmp/log", new Redirection(2, RedirectionType.APPEND, "/tmp/log").getTarget());
    }

    @ParameterizedTest
    @ValueSource(strings = {"'EOF'", "\"EOF\"", "\\EOF"})
    void quotedHeredocDelimiterIsQuoted(final String delim) {
        Assertions.assertTrue(
            this.heredocCommand(delim).getRedirections().get(0).isHereDocQuoted()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"'EOF'", "\"EOF\"", "\\EOF"})
    void quotedHeredocDelimiterTarget(final String delim) {
        Assertions.assertEquals(
            "EOF",
            this.heredocCommand(delim).getRedirections().get(0).getTarget()
        );
    }

    @Test
    void redirectionDescriptorBoundariesFirst() {
        Assertions.assertEquals(
            2,
            this.commandOf("cmd 2>out").getRedirections().get(0).getFd()
        );
    }

    @Test
    void redirectionDescriptorBoundariesSecond() {
        this.commandOf("cmd 2>out");
        Assertions.assertEquals(
            0,
            this.commandOf("cmd <in").getRedirections().get(0).getFd()
        );
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
    void redirectionWithoutTargetFails() {
        Assertions.assertThrows(
            ParseException.class,
            () -> this.parse("cat >")
        );
    }

    @Test
    void pipelineSeparatorsInCompoundConditionsFirst() {
        Assertions.assertEquals(
            1,
            this.parse("if a & b; then c; fi").getPipelines().size()
        );
    }

    @Test
    void pipelineSeparatorsInCompoundConditionsSecond() {
        this.parse("if a & b; then c; fi");
        Assertions.assertEquals(
            1,
            this.parse("while a && b; do c; done").getPipelines().size()
        );
    }

    @Test
    void pipelineSeparatorsInCompoundConditionsThird() {
        this.parse("if a & b; then c; fi");
        this.parse("while a && b; do c; done");
        Assertions.assertEquals(
            1,
            this.parse("until a || b; do c; done").getPipelines().size()
        );
    }

    @Test
    void assignmentAndNamedCommandLineNumbersFirst() {
        Assertions.assertEquals(
            1,
            ((SimpleCommand) this.parse(String.format("X=1%necho hi%n")).getPipelines().get(0).getCommands().get(0))
                .getLineNumber()
        );
    }

    @Test
    void assignmentAndNamedCommandLineNumbersSecond() {
        Assertions.assertEquals(
            2,
            ((SimpleCommand) this.parse(String.format("X=1%necho hi%n")).getPipelines().get(1).getCommands().get(0))
                .getLineNumber()
        );
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
        return (SimpleCommand) this.parse(
            String.format("cat <<%s%nbody%nEOF%n", delim)
        ).getPipelines().get(0).getCommands().get(0);
    }
}
