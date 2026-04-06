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
    void parseSimpleCommandCheck1() {
        final ListNode list = this.parse("echo hello");
        Assertions.assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseSimpleCommandCheck2() {
        final ListNode list = this.parse("echo hello");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
    }

    @Test
    void parseSimpleCommandCheck3() {
        final ListNode list = this.parse("echo hello");
        final Pipeline pipeline = list.getPipelines().get(0);
        final SimpleCommand cmd = (SimpleCommand) pipeline.getCommands().get(0);
        Assertions.assertEquals(ParserTest.ECHO_CMD, cmd.getName());
    }

    @Test
    void parseSimpleCommandCheck4() {
        final ListNode list = this.parse("echo hello");
        final Pipeline pipeline = list.getPipelines().get(0);
        final SimpleCommand cmd = (SimpleCommand) pipeline.getCommands().get(0);
        Assertions.assertEquals(1, cmd.getArguments().size());
    }

    @Test
    void parseSimpleCommandCheck5() {
        final ListNode list = this.parse("echo hello");
        final Pipeline pipeline = list.getPipelines().get(0);
        final SimpleCommand cmd = (SimpleCommand) pipeline.getCommands().get(0);
        Assertions.assertEquals("hello", cmd.getArguments().get(0));
    }

    @Test
    void parsePipelineCheck1() {
        final ListNode list = this.parse("echo a | grep b");
        Assertions.assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parsePipelineCheck2() {
        final ListNode list = this.parse("echo a | grep b");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(2, pipeline.getCommands().size());
    }

    @Test
    void parsePipelineCheck3() {
        final ListNode list = this.parse("echo a | grep b");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) pipeline.getCommands().get(0)).getName()
        );
    }

    @Test
    void parsePipelineCheck4() {
        final ListNode list = this.parse("echo a | grep b");
        final Pipeline pipeline = list.getPipelines().get(0);
        ((SimpleCommand) pipeline.getCommands().get(0)).getName();
        Assertions.assertEquals("grep", ((SimpleCommand) pipeline.getCommands().get(1)).getName());
    }

    @Test
    void parseListWithSemicolonCheck1() {
        final ListNode list = this.parse("cmd1 ; cmd2");
        Assertions.assertEquals(2, list.getPipelines().size());
    }

    @Test
    void parseListWithSemicolonCheck2() {
        final ListNode list = this.parse("cmd1 ; cmd2");
        Assertions.assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAndCheck1() {
        final ListNode list = this.parse("cmd1 && cmd2");
        Assertions.assertEquals(2, list.getPipelines().size());
    }

    @Test
    void parseListWithAndCheck2() {
        final ListNode list = this.parse("cmd1 && cmd2");
        Assertions.assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithOrCheck1() {
        final ListNode list = this.parse("cmd1 || cmd2");
        Assertions.assertEquals(2, list.getPipelines().size());
    }

    @Test
    void parseListWithOrCheck2() {
        final ListNode list = this.parse("cmd1 || cmd2");
        Assertions.assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAmpCheck1() {
        final ListNode list = this.parse("cmd1 & cmd2");
        Assertions.assertEquals(2, list.getPipelines().size());
    }

    @Test
    void parseListWithAmpCheck2() {
        final ListNode list = this.parse("cmd1 & cmd2");
        Assertions.assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
    }

    @Test
    void parseListWithNewlineSeparatorCheck1() {
        final ListNode list = this.parse(String.format("cmd1%ncmd2"));
        Assertions.assertEquals(2, list.getPipelines().size());
    }

    @Test
    void parseListWithNewlineSeparatorCheck2() {
        final ListNode list = this.parse(String.format("cmd1%ncmd2"));
        Assertions.assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
    }

    @Test
    void parseListWithNewlineSeparatorCheck3() {
        final ListNode list = this.parse(String.format("cmd1%ncmd2"));
        list.getSeparators().get(0);
        Assertions.assertEquals(
            "cmd1",
            ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseListWithNewlineSeparatorCheck4() {
        final ListNode list = this.parse(String.format("cmd1%ncmd2"));
        list.getSeparators().get(0);
        ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName();
        Assertions.assertEquals(
            "cmd2",
            ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlinesCheck1() {
        final ListNode list = this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n"));
        Assertions.assertEquals(2, list.getPipelines().size());
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlinesCheck2() {
        final ListNode list = this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n"));
        Assertions.assertEquals(
            "cmd1",
            ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlinesCheck3() {
        final ListNode list = this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n"));
        ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName();
        Assertions.assertEquals(
            "cmd2",
            ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseBang() {
        Assertions.assertTrue(this.parse("! echo hello").getPipelines().get(0).isBang());
    }

    @Test
    void parseCommandWithNoArgumentsCheck1() {
        final SimpleCommand cmd = this.command(this.parse("ls"));
        Assertions.assertEquals("ls", cmd.getName());
    }

    @Test
    void parseCommandWithNoArgumentsCheck2() {
        final SimpleCommand cmd = this.command(this.parse("ls"));
        Assertions.assertTrue(cmd.getArguments().isEmpty());
    }

    @Test
    void parseInputRedirectionCheck1() {
        final SimpleCommand cmd = this.command(this.parse("cat < file.txt"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseInputRedirectionCheck2() {
        final SimpleCommand cmd = this.command(this.parse("cat < file.txt"));
        Assertions.assertEquals(0, cmd.getRedirections().get(0).getFd());
    }

    @Test
    void parseInputRedirectionCheck3() {
        final SimpleCommand cmd = this.command(this.parse("cat < file.txt"));
        cmd.getRedirections().get(0).getFd();
        Assertions.assertEquals(RedirectionType.INPUT, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseInputRedirectionCheck4() {
        final SimpleCommand cmd = this.command(this.parse("cat < file.txt"));
        cmd.getRedirections().get(0).getFd();
        cmd.getRedirections().get(0).getType();
        Assertions.assertEquals("file.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseOutputRedirectionCheck1() {
        final SimpleCommand cmd = this.command(this.parse("echo hello > out.txt"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseOutputRedirectionCheck2() {
        final SimpleCommand cmd = this.command(this.parse("echo hello > out.txt"));
        Assertions.assertEquals(1, cmd.getRedirections().get(0).getFd());
    }

    @Test
    void parseOutputRedirectionCheck3() {
        final SimpleCommand cmd = this.command(this.parse("echo hello > out.txt"));
        cmd.getRedirections().get(0).getFd();
        Assertions.assertEquals(RedirectionType.OUTPUT, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseOutputRedirectionCheck4() {
        final SimpleCommand cmd = this.command(this.parse("echo hello > out.txt"));
        cmd.getRedirections().get(0).getFd();
        cmd.getRedirections().get(0).getType();
        Assertions.assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseAppendRedirectionCheck1() {
        final SimpleCommand cmd = this.command(this.parse("echo hello >> out.txt"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseAppendRedirectionCheck2() {
        final SimpleCommand cmd = this.command(this.parse("echo hello >> out.txt"));
        Assertions.assertEquals(RedirectionType.APPEND, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseAppendRedirectionCheck3() {
        final SimpleCommand cmd = this.command(this.parse("echo hello >> out.txt"));
        cmd.getRedirections().get(0).getType();
        Assertions.assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseClobberRedirectionCheck1() {
        final SimpleCommand cmd = this.command(this.parse("echo test >| file.txt"));
        Assertions.assertTrue(cmd.getRedirections().size() >= 1);
    }

    @Test
    void parseClobberRedirectionCheck2() {
        final SimpleCommand cmd = this.command(this.parse("echo test >| file.txt"));
        Assertions.assertTrue(
            cmd.getRedirections().stream().anyMatch(r -> r.getType() == RedirectionType.CLOBBER)
        );
    }

    @Test
    void parseCommandWithDollarCheck1() {
        final SimpleCommand cmd = this.command(this.parse("echo $VAR"));
        Assertions.assertEquals(ParserTest.ECHO_CMD, cmd.getName());
    }

    @Test
    void parseCommandWithDollarCheck2() {
        final SimpleCommand cmd = this.command(this.parse("echo $VAR"));
        Assertions.assertEquals("$VAR", cmd.getArguments().get(0));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsCheck1() {
        final ListNode list = this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5");
        Assertions.assertEquals(5, list.getPipelines().size());
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsCheck2() {
        final ListNode list = this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5");
        Assertions.assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsCheck3() {
        final ListNode list = this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5");
        list.getSeparators().get(0);
        Assertions.assertEquals(ListNode.Separator.AMP, list.getSeparators().get(1));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsCheck4() {
        final ListNode list = this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5");
        list.getSeparators().get(0);
        list.getSeparators().get(1);
        Assertions.assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparatorsCheck5() {
        final ListNode list = this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5");
        list.getSeparators().get(0);
        list.getSeparators().get(1);
        list.getSeparators().get(2);
        Assertions.assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(3));
    }

    @Test
    void parseEmptyPipeline() {
        Assertions.assertEquals(1, this.parse("").getPipelines().size());
    }

    @Test
    void parseSubshellCheck1() {
        final ListNode list = this.parse("(echo hello)");
        Assertions.assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseSubshellCheck2() {
        final ListNode list = this.parse("(echo hello)");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
    }

    @Test
    void parseSubshellCheck3() {
        final ListNode list = this.parse("(echo hello)");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof Subshell);
    }

    @Test
    void parseSubshellCheck4() {
        final ListNode list = this.parse("(echo hello)");
        final Pipeline pipeline = list.getPipelines().get(0);
        final Subshell sub = (Subshell) pipeline.getCommands().get(0);
        Assertions.assertEquals(1, sub.getList().getPipelines().size());
    }

    @Test
    void parseSubshellCheck5() {
        final ListNode list = this.parse("(echo hello)");
        final Pipeline pipeline = list.getPipelines().get(0);
        final Subshell sub = (Subshell) pipeline.getCommands().get(0);
        Assertions.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) sub.getList().getPipelines().get(0).getCommands().get(0)).getName()
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
    void parseSubshellInPipelineCheck1() {
        final Pipeline pipeline = this.pipelineOf("echo foo | (cat) | wc");
        Assertions.assertEquals(3, pipeline.getCommands().size());
    }

    @Test
    void parseSubshellInPipelineCheck2() {
        final Pipeline pipeline = this.pipelineOf("echo foo | (cat) | wc");
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof SimpleCommand);
    }

    @Test
    void parseSubshellInPipelineCheck3() {
        final Pipeline pipeline = this.pipelineOf("echo foo | (cat) | wc");
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof Subshell);
    }

    @Test
    void parseSubshellInPipelineCheck4() {
        final Pipeline pipeline = this.pipelineOf("echo foo | (cat) | wc");
        Assertions.assertTrue(pipeline.getCommands().get(2) instanceof SimpleCommand);
    }

    @Test
    void parseGroupingCheck1() {
        final ListNode list = this.parse("{ echo hello; }");
        Assertions.assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseGroupingCheck2() {
        final ListNode list = this.parse("{ echo hello; }");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
    }

    @Test
    void parseGroupingCheck3() {
        final ListNode list = this.parse("{ echo hello; }");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof Grouping);
    }

    @Test
    void parseGroupingCheck4() {
        final ListNode list = this.parse("{ echo hello; }");
        final Pipeline pipeline = list.getPipelines().get(0);
        final Grouping glob = (Grouping) pipeline.getCommands().get(0);
        Assertions.assertEquals(1, glob.getList().getPipelines().size());
    }

    @Test
    void parseGroupingCheck5() {
        final ListNode list = this.parse("{ echo hello; }");
        final Pipeline pipeline = list.getPipelines().get(0);
        final Grouping glob = (Grouping) pipeline.getCommands().get(0);
        Assertions.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) glob.getList().getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseGroupingInPipelineCheck1() {
        final Pipeline pipeline = this.pipelineOf("echo foo | { cat; } | wc");
        Assertions.assertEquals(3, pipeline.getCommands().size());
    }

    @Test
    void parseGroupingInPipelineCheck2() {
        final Pipeline pipeline = this.pipelineOf("echo foo | { cat; } | wc");
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof SimpleCommand);
    }

    @Test
    void parseGroupingInPipelineCheck3() {
        final Pipeline pipeline = this.pipelineOf("echo foo | { cat; } | wc");
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof Grouping);
    }

    @Test
    void parseGroupingInPipelineCheck4() {
        final Pipeline pipeline = this.pipelineOf("echo foo | { cat; } | wc");
        Assertions.assertTrue(pipeline.getCommands().get(2) instanceof SimpleCommand);
    }

    @Test
    void parseExpectThrowsOnMissingWord() {
        Assertions.assertThrowsExactly(ParseException.class, () -> this.parse("echo <"));
    }

    @Test
    void parseCommandStartingWithInputRedirectCheck1() {
        final SimpleCommand cmd = this.command(this.parse("< input.txt cat"));
        Assertions.assertEquals("<", cmd.getName());
    }

    @Test
    void parseCommandStartingWithInputRedirectCheck2() {
        final SimpleCommand cmd = this.command(this.parse("< input.txt cat"));
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithOutputRedirectCheck1() {
        final SimpleCommand cmd = this.command(this.parse("> out.txt echo"));
        Assertions.assertEquals(">", cmd.getName());
    }

    @Test
    void parseCommandStartingWithOutputRedirectCheck2() {
        final SimpleCommand cmd = this.command(this.parse("> out.txt echo"));
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithClobberRedirectCheck1() {
        final SimpleCommand cmd = this.command(this.parse(">| file.txt echo"));
        Assertions.assertEquals(">|", cmd.getName());
    }

    @Test
    void parseCommandStartingWithClobberRedirectCheck2() {
        final SimpleCommand cmd = this.command(this.parse(">| file.txt echo"));
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithAppendRedirectCheck1() {
        final SimpleCommand cmd = this.command(this.parse(">> file.txt echo"));
        Assertions.assertEquals(">>", cmd.getName());
    }

    @Test
    void parseCommandStartingWithAppendRedirectCheck2() {
        final SimpleCommand cmd = this.command(this.parse(">> file.txt echo"));
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseAssignmentWordAfterCommandNameCheck1() {
        final SimpleCommand cmd = this.command(this.parse("echo FOO=bar"));
        Assertions.assertEquals(ParserTest.ECHO_CMD, cmd.getName());
    }

    @Test
    void parseAssignmentWordAfterCommandNameCheck2() {
        final SimpleCommand cmd = this.command(this.parse("echo FOO=bar"));
        Assertions.assertTrue(cmd.getArguments().contains("FOO=bar"));
    }

    @Test
    void parseAssignmentWordAfterCommandNameCheck3() {
        final SimpleCommand cmd = this.command(this.parse("echo FOO=bar"));
        Assertions.assertTrue(cmd.getAssignments().isEmpty());
    }

    @Test
    void parseIoNumberRedirectCheck1() {
        final SimpleCommand cmd = this.command(this.parse("2>file"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseIoNumberRedirectCheck2() {
        final SimpleCommand cmd = this.command(this.parse("2>file"));
        Assertions.assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupInputRedirectCheck1() {
        final SimpleCommand cmd = this.command(this.parse("cmd <&3"));
        Assertions.assertEquals("cmd", cmd.getName());
    }

    @Test
    void parseDupInputRedirectCheck2() {
        final SimpleCommand cmd = this.command(this.parse("cmd <&3"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseDupInputRedirectCheck3() {
        final SimpleCommand cmd = this.command(this.parse("cmd <&3"));
        Assertions.assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseDupInputRedirectCheck4() {
        final SimpleCommand cmd = this.command(this.parse("cmd <&3"));
        cmd.getRedirections().get(0).getType();
        Assertions.assertEquals("3", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseCloseInputRedirectCheck1() {
        final SimpleCommand cmd = this.command(this.parse("cmd <&-"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseCloseInputRedirectCheck2() {
        final SimpleCommand cmd = this.command(this.parse("cmd <&-"));
        Assertions.assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseCloseInputRedirectCheck3() {
        final SimpleCommand cmd = this.command(this.parse("cmd <&-"));
        cmd.getRedirections().get(0).getType();
        Assertions.assertEquals("-", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupOutputRedirectCheck1() {
        final SimpleCommand cmd = this.command(this.parse("cmd >&2"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseDupOutputRedirectCheck2() {
        final SimpleCommand cmd = this.command(this.parse("cmd >&2"));
        Assertions.assertEquals(RedirectionType.DUP_OUTPUT, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseDupOutputRedirectCheck3() {
        final SimpleCommand cmd = this.command(this.parse("cmd >&2"));
        cmd.getRedirections().get(0).getType();
        Assertions.assertEquals("2", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseReadWriteRedirectCheck1() {
        final SimpleCommand cmd = this.command(this.parse("cmd <>file"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseReadWriteRedirectCheck2() {
        final SimpleCommand cmd = this.command(this.parse("cmd <>file"));
        Assertions.assertEquals(RedirectionType.READWRITE, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseReadWriteRedirectCheck3() {
        final SimpleCommand cmd = this.command(this.parse("cmd <>file"));
        cmd.getRedirections().get(0).getType();
        Assertions.assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseIfThenFiCheck1() {
        final ListNode list = this.parse("if true; then echo hi; fi");
        Assertions.assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseIfThenFiCheck2() {
        final ListNode list = this.parse("if true; then echo hi; fi");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
    }

    @Test
    void parseIfThenFiCheck3() {
        final ListNode list = this.parse("if true; then echo hi; fi");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof IfNode);
    }

    @Test
    void parseIfThenElseFiCheck1() {
        final Pipeline pipeline = this.pipelineOf("if false; then echo a; else echo b; fi");
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof IfNode);
    }

    @Test
    void parseIfThenElseFiCheck2() {
        final Pipeline pipeline = this.pipelineOf("if false; then echo a; else echo b; fi");
        Assertions.assertNotNull(((IfNode) pipeline.getCommands().get(0)).getElseBody());
    }

    @Test
    void parseForDoDoneCheck1() {
        final Pipeline pipeline = this.pipelineOf("for var in a b c; do echo $var; done");
        Assertions.assertEquals(1, pipeline.getCommands().size());
    }

    @Test
    void parseForDoDoneCheck2() {
        final Pipeline pipeline = this.pipelineOf("for var in a b c; do echo $var; done");
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof ForNode);
    }

    @Test
    void parseForDoDoneCheck3() {
        final Pipeline pipeline = this.pipelineOf("for var in a b c; do echo $var; done");
        final ForNode node = (ForNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("var", node.getVariable());
    }

    @Test
    void parseForDoDoneCheck4() {
        final Pipeline pipeline = this.pipelineOf("for var in a b c; do echo $var; done");
        final ForNode node = (ForNode) pipeline.getCommands().get(0);
        Assertions.assertEquals(3, node.getWords().size());
    }

    @Test
    void parseForDoDoneCheck5() {
        final Pipeline pipeline = this.pipelineOf("for var in a b c; do echo $var; done");
        final ForNode node = (ForNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("a", node.getWords().get(0));
    }

    @Test
    void parseForDoDoneCheck6() {
        final Pipeline pipeline = this.pipelineOf("for var in a b c; do echo $var; done");
        final ForNode node = (ForNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("c", node.getWords().get(2));
    }

    @Test
    void parseWhileDoDoneCheck1() {
        final Pipeline pipeline = this.pipelineOf("while true; do echo hi; done");
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof WhileNode);
    }

    @Test
    void parseWhileDoDoneCheck2() {
        final Pipeline pipeline = this.pipelineOf("while true; do echo hi; done");
        Assertions.assertFalse(((WhileNode) pipeline.getCommands().get(0)).isUntil());
    }

    @Test
    void parseUntilDoDoneCheck1() {
        final Pipeline pipeline = this.pipelineOf("until false; do echo hi; done");
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof WhileNode);
    }

    @Test
    void parseUntilDoDoneCheck2() {
        final Pipeline pipeline = this.pipelineOf("until false; do echo hi; done");
        Assertions.assertTrue(((WhileNode) pipeline.getCommands().get(0)).isUntil());
    }

    @Test
    void parseCaseSimpleCheck1() {
        final Pipeline pipeline = this.pipelineOf("case foo in pat) echo match;; esac");
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof CaseNode);
    }

    @Test
    void parseCaseSimpleCheck2() {
        final Pipeline pipeline = this.pipelineOf("case foo in pat) echo match;; esac");
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("foo", num.getWord());
    }

    @Test
    void parseCaseSimpleCheck3() {
        final Pipeline pipeline = this.pipelineOf("case foo in pat) echo match;; esac");
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals(1, num.getItems().size());
    }

    @Test
    void parseCaseSimpleCheck4() {
        final Pipeline pipeline = this.pipelineOf("case foo in pat) echo match;; esac");
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("pat", num.getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseCasePipePatternCheck1() {
        final Pipeline pipeline = this.pipelineOf("case foo in a|b) echo match;; esac");
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals(2, num.getItems().get(0).getPatterns().size());
    }

    @Test
    void parseCasePipePatternCheck2() {
        final Pipeline pipeline = this.pipelineOf("case foo in a|b) echo match;; esac");
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        num.getItems().get(0).getPatterns().size();
        Assertions.assertEquals("a", num.getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseCasePipePatternCheck3() {
        final Pipeline pipeline = this.pipelineOf("case foo in a|b) echo match;; esac");
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        num.getItems().get(0).getPatterns().size();
        num.getItems().get(0).getPatterns().get(0);
        Assertions.assertEquals("b", num.getItems().get(0).getPatterns().get(1));
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
    void parseFunctionDefinitionCheck1() {
        final Pipeline pipeline = this.pipelineOf("foo() { echo bar; }");
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof FunctionNode);
    }

    @Test
    void parseFunctionDefinitionCheck2() {
        final Pipeline pipeline = this.pipelineOf("foo() { echo bar; }");
        Assertions.assertEquals("foo", ((FunctionNode) pipeline.getCommands().get(0)).getName());
    }

    @Test
    void parseFunctionSubshellBodyCheck1() {
        final Pipeline pipeline = this.pipelineOf("foo() (echo bar)");
        final FunctionNode num = (FunctionNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("foo", num.getName());
    }

    @Test
    void parseFunctionSubshellBodyCheck2() {
        final Pipeline pipeline = this.pipelineOf("foo() (echo bar)");
        final FunctionNode num = (FunctionNode) pipeline.getCommands().get(0);
        Assertions.assertTrue(num.getBody() instanceof Subshell);
    }

    @Test
    void parseIfInPipelineCheck1() {
        final Pipeline pipeline = this.pipelineOf("true | if true; then echo hi; fi");
        Assertions.assertEquals(2, pipeline.getCommands().size());
    }

    @Test
    void parseIfInPipelineCheck2() {
        final Pipeline pipeline = this.pipelineOf("true | if true; then echo hi; fi");
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof SimpleCommand);
    }

    @Test
    void parseIfInPipelineCheck3() {
        final Pipeline pipeline = this.pipelineOf("true | if true; then echo hi; fi");
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof IfNode);
    }

    @Test
    void parseForInPipelineCheck1() {
        final Pipeline pipeline = this.pipelineOf("true | for var in a; do echo $var; done");
        Assertions.assertEquals(2, pipeline.getCommands().size());
    }

    @Test
    void parseForInPipelineCheck2() {
        final Pipeline pipeline = this.pipelineOf("true | for var in a; do echo $var; done");
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof ForNode);
    }

    @Test
    void parseWhileInPipelineCheck1() {
        final Pipeline pipeline = this.pipelineOf("true | while false; do echo hi; done");
        Assertions.assertEquals(2, pipeline.getCommands().size());
    }

    @Test
    void parseWhileInPipelineCheck2() {
        final Pipeline pipeline = this.pipelineOf("true | while false; do echo hi; done");
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof WhileNode);
    }

    @Test
    void parseCaseInPipelineCheck1() {
        final Pipeline pipeline = this.pipelineOf("true | case foo in pat) echo hi;; esac");
        Assertions.assertEquals(2, pipeline.getCommands().size());
    }

    @Test
    void parseCaseInPipelineCheck2() {
        final Pipeline pipeline = this.pipelineOf("true | case foo in pat) echo hi;; esac");
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof CaseNode);
    }

    @Test
    void parseFunctionInPipelineCheck1() {
        final Pipeline pipeline = this.pipelineOf("true | foo() { echo bar; }");
        Assertions.assertEquals(2, pipeline.getCommands().size());
    }

    @Test
    void parseFunctionInPipelineCheck2() {
        final Pipeline pipeline = this.pipelineOf("true | foo() { echo bar; }");
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof FunctionNode);
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
    void parseAssignmentWithoutCommandCheck1() {
        final SimpleCommand cmd = this.command(this.parse("FOO=bar"));
        Assertions.assertEquals("", cmd.getName());
    }

    @Test
    void parseAssignmentWithoutCommandCheck2() {
        final SimpleCommand cmd = this.command(this.parse("FOO=bar"));
        Assertions.assertEquals(1, cmd.getAssignments().size());
    }

    @Test
    void parseAssignmentWithoutCommandCheck3() {
        final SimpleCommand cmd = this.command(this.parse("FOO=bar"));
        Assertions.assertEquals("FOO=bar", cmd.getAssignments().get(0));
    }

    @Test
    void parseMultipleAssignmentsThenCommandCheck1() {
        final SimpleCommand cmd = this.command(this.parse("FOO=1 BAR=2 cmd arg"));
        Assertions.assertEquals("cmd", cmd.getName());
    }

    @Test
    void parseMultipleAssignmentsThenCommandCheck2() {
        final SimpleCommand cmd = this.command(this.parse("FOO=1 BAR=2 cmd arg"));
        Assertions.assertEquals(2, cmd.getAssignments().size());
    }

    @Test
    void parseMultipleAssignmentsThenCommandCheck3() {
        final SimpleCommand cmd = this.command(this.parse("FOO=1 BAR=2 cmd arg"));
        Assertions.assertEquals("arg", cmd.getArguments().get(0));
    }

    @Test
    void parseInlineAssignmentMidCommandCheck1() {
        final SimpleCommand cmd = this.command(this.parse("cmd FOO=1 arg"));
        Assertions.assertEquals("cmd", cmd.getName());
    }

    @Test
    void parseInlineAssignmentMidCommandCheck2() {
        final SimpleCommand cmd = this.command(this.parse("cmd FOO=1 arg"));
        Assertions.assertEquals(0, cmd.getAssignments().size());
    }

    @Test
    void parseInlineAssignmentMidCommandCheck3() {
        final SimpleCommand cmd = this.command(this.parse("cmd FOO=1 arg"));
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseInlineAssignmentMidCommandCheck4() {
        final SimpleCommand cmd = this.command(this.parse("cmd FOO=1 arg"));
        Assertions.assertEquals("FOO=1", cmd.getArguments().get(0));
    }

    @Test
    void parseInlineAssignmentMidCommandCheck5() {
        final SimpleCommand cmd = this.command(this.parse("cmd FOO=1 arg"));
        cmd.getArguments().get(0);
        Assertions.assertEquals("arg", cmd.getArguments().get(1));
    }

    @Test
    void parseElifChainNestedElifCheck1() {
        final ListNode list = this.parse(
            "if false; then a; elif false; then b; elif true; then c; fi"
        );
        Assertions.assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseElifChainNestedElifCheck2() {
        final ListNode list = this.parse(
            "if false; then a; elif false; then b; elif true; then c; fi"
        );
        Assertions.assertTrue(list.getPipelines().get(0).getCommands().get(0) instanceof IfNode);
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
    void parseHereDocRedirectCheck1() {
        final SimpleCommand cmd = this.command(
            this.parse(String.format("cat <<EOF%nhello%nEOF%n"))
        );
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseHereDocRedirectCheck2() {
        final SimpleCommand cmd = this.command(
            this.parse(String.format("cat <<EOF%nhello%nEOF%n"))
        );
        Assertions.assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseCaseMultilineBodyCheck1() {
        final ListNode list = this.parse(
            String.format("case x in%n  a) echo aa;;%n  b) echo bb;;%nesac")
        );
        Assertions.assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseCaseMultilineBodyCheck2() {
        final ListNode list = this.parse(
            String.format("case x in%n  a) echo aa;;%n  b) echo bb;;%nesac")
        );
        Assertions.assertEquals(
            2,
            ((CaseNode) list.getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    void parseCaseMultilineWithBlankLinesBetweenArmsCheck1() {
        final ListNode list = this.parse(
            String.format("case y in%n%n  a) echo aa;;%n%n  b) echo bb;;%n%nesac")
        );
        Assertions.assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseCaseMultilineWithBlankLinesBetweenArmsCheck2() {
        final ListNode list = this.parse(
            String.format("case y in%n%n  a) echo aa;;%n%n  b) echo bb;;%n%nesac")
        );
        Assertions.assertEquals(
            2,
            ((CaseNode) list.getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    void parseHereDocStripTabsCheck1() {
        final SimpleCommand cmd = this.command(
            this.parse(String.format("cat <<-EOF%n\thello%nEOF%n"))
        );
        Assertions.assertEquals(1, cmd.getRedirections().size());
    }

    @Test
    void parseHereDocStripTabsCheck2() {
        final SimpleCommand cmd = this.command(
            this.parse(String.format("cat <<-EOF%n\thello%nEOF%n"))
        );
        Assertions.assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseSeparatorsInListCheck1() {
        final ListNode list = this.parse("cmd1 & cmd2 && cmd3 || cmd4");
        Assertions.assertEquals(4, list.getPipelines().size());
    }

    @Test
    void parseSeparatorsInListCheck2() {
        final ListNode list = this.parse("cmd1 & cmd2 && cmd3 || cmd4");
        Assertions.assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
    }

    @Test
    void parseSeparatorsInListCheck3() {
        final ListNode list = this.parse("cmd1 & cmd2 && cmd3 || cmd4");
        list.getSeparators().get(0);
        Assertions.assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(1));
    }

    @Test
    void parseSeparatorsInListCheck4() {
        final ListNode list = this.parse("cmd1 & cmd2 && cmd3 || cmd4");
        list.getSeparators().get(0);
        list.getSeparators().get(1);
        Assertions.assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
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
    void redirectionInputConstructorCheck1() {
        final Redirection result = new Redirection(0, RedirectionType.INPUT, "/dev/null");
        Assertions.assertEquals(0, result.getFd());
    }

    @Test
    void redirectionInputConstructorCheck2() {
        final Redirection result = new Redirection(0, RedirectionType.INPUT, "/dev/null");
        Assertions.assertEquals(RedirectionType.INPUT, result.getType());
    }

    @Test
    void redirectionInputConstructorCheck3() {
        final Redirection result = new Redirection(0, RedirectionType.INPUT, "/dev/null");
        Assertions.assertEquals("/dev/null", result.getTarget());
    }

    @Test
    void redirectionOutputConstructorCheck1() {
        final Redirection result = new Redirection(1, RedirectionType.OUTPUT, "/tmp/out");
        Assertions.assertEquals(1, result.getFd());
    }

    @Test
    void redirectionOutputConstructorCheck2() {
        final Redirection result = new Redirection(1, RedirectionType.OUTPUT, "/tmp/out");
        Assertions.assertEquals(RedirectionType.OUTPUT, result.getType());
    }

    @Test
    void redirectionOutputConstructorCheck3() {
        final Redirection result = new Redirection(1, RedirectionType.OUTPUT, "/tmp/out");
        Assertions.assertEquals("/tmp/out", result.getTarget());
    }

    @Test
    void redirectionAppendConstructorCheck1() {
        final Redirection result = new Redirection(2, RedirectionType.APPEND, "/tmp/log");
        Assertions.assertEquals(2, result.getFd());
    }

    @Test
    void redirectionAppendConstructorCheck2() {
        final Redirection result = new Redirection(2, RedirectionType.APPEND, "/tmp/log");
        Assertions.assertEquals(RedirectionType.APPEND, result.getType());
    }

    @Test
    void redirectionAppendConstructorCheck3() {
        final Redirection result = new Redirection(2, RedirectionType.APPEND, "/tmp/log");
        Assertions.assertEquals("/tmp/log", result.getTarget());
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
    void redirectionDescriptorBoundariesCheck1() {
        Assertions.assertEquals(
            2,
            this.commandOf("cmd 2>out").getRedirections().get(0).getFd()
        );
    }

    @Test
    void redirectionDescriptorBoundariesCheck2() {
        this.commandOf("cmd 2>out").getRedirections().get(0).getFd();
        Assertions.assertEquals(
            0,
            this.commandOf("cmd <in").getRedirections().get(0).getFd()
        );
    }

    @Test
    void redirectionDescriptorBoundariesCheck3() {
        this.commandOf("cmd 2>out").getRedirections().get(0).getFd();
        this.commandOf("cmd <in").getRedirections().get(0).getFd();
        Assertions.assertEquals(
            RedirectionType.DUP_OUTPUT,
            this.commandOf("cmd 2>&1").getRedirections().get(0).getType()
        );
    }

    @Test
    void redirectionDescriptorBoundariesCheck4() {
        this.commandOf("cmd 2>out").getRedirections().get(0).getFd();
        this.commandOf("cmd <in").getRedirections().get(0).getFd();
        this.commandOf("cmd 2>&1").getRedirections().get(0).getType();
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
    void pipelineSeparatorsInCompoundConditionsCheck1() {
        Assertions.assertEquals(
            1,
            this.parse("if a & b; then c; fi").getPipelines().size()
        );
    }

    @Test
    void pipelineSeparatorsInCompoundConditionsCheck2() {
        this.parse("if a & b; then c; fi").getPipelines().size();
        Assertions.assertEquals(
            1,
            this.parse("while a && b; do c; done").getPipelines().size()
        );
    }

    @Test
    void pipelineSeparatorsInCompoundConditionsCheck3() {
        this.parse("if a & b; then c; fi").getPipelines().size();
        this.parse("while a && b; do c; done").getPipelines().size();
        Assertions.assertEquals(
            1,
            this.parse("until a || b; do c; done").getPipelines().size()
        );
    }

    @Test
    void assignmentAndNamedCommandLineNumbersCheck1() {
        final ListNode list = this.parse(String.format("X=1%necho hi%n"));
        Assertions.assertEquals(
            1,
            ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0))
                .getLineNumber()
        );
    }

    @Test
    void assignmentAndNamedCommandLineNumbersCheck2() {
        final ListNode list = this.parse(String.format("X=1%necho hi%n"));
        ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0))
                .getLineNumber();
        Assertions.assertEquals(
            2,
            ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0))
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
