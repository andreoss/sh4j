/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import org.junit.jupiter.api.Test;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import org.junit.jupiter.api.Assertions;

/**
 * Tests for Parser.
 * @since 1.0
 */
final class ParserTest {

    @Test
    void parseSimpleCommand() {
        final ListNode list = this.parse("echo hello");
        Assertions.assertEquals(1, list.getPipelines().size());
        
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
        
        final SimpleCommand cmd = (SimpleCommand) pipeline.getCommands().get(0);
        Assertions.assertEquals("echo", cmd.getName());
        Assertions.assertEquals(1, cmd.getArguments().size());
        Assertions.assertEquals("hello", cmd.getArguments().get(0));
    }

    @Test
    void parsePipeline() {
        final ListNode list = this.parse("echo a | grep b");
        Assertions.assertEquals(1, list.getPipelines().size());
        
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertEquals("echo", ((SimpleCommand) pipeline.getCommands().get(0)).getName());
        Assertions.assertEquals("grep", ((SimpleCommand) pipeline.getCommands().get(1)).getName());
    }

    @Test
    void parseListWithSemicolon() {
        final ListNode list = this.parse("cmd1 ; cmd2");
        Assertions.assertEquals(2, list.getPipelines().size());
        Assertions.assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAnd() {
        final ListNode list = this.parse("cmd1 && cmd2");
        Assertions.assertEquals(2, list.getPipelines().size());
        Assertions.assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithOr() {
        final ListNode list = this.parse("cmd1 || cmd2");
        Assertions.assertEquals(2, list.getPipelines().size());
        Assertions.assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAmp() {
        final ListNode list = this.parse("cmd1 & cmd2");
        Assertions.assertEquals(2, list.getPipelines().size());
        Assertions.assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
    }

    @Test
    void parseListWithNewlineSeparator() {
        final ListNode list = this.parse(String.format("cmd1%ncmd2"));
        Assertions.assertEquals(2, list.getPipelines().size());
        Assertions.assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
        Assertions.assertEquals("cmd1", ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName());
        Assertions.assertEquals("cmd2", ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName());
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlines() {
        final ListNode list = this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n"));
        Assertions.assertEquals(2, list.getPipelines().size());
        Assertions.assertEquals("cmd1", ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName());
        Assertions.assertEquals("cmd2", ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName());
    }

    @Test
    void parseBang() {
        final ListNode list = this.parse("! echo hello");
        Assertions.assertTrue(list.getPipelines().get(0).isBang());
    }

    @Test
    void parseCommandWithNoArguments() {
        final ListNode list = this.parse("ls");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals("ls", cmd.getName());
        Assertions.assertTrue(cmd.getArguments().isEmpty());
    }

    @Test
    void parseInputRedirection() {
        final ListNode list = this.parse("cat < file.txt");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(0, cmd.getRedirections().get(0).getFd());
        Assertions.assertEquals(RedirectionType.INPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("file.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseOutputRedirection() {
        final ListNode list = this.parse("echo hello > out.txt");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(1, cmd.getRedirections().get(0).getFd());
        Assertions.assertEquals(RedirectionType.OUTPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseAppendRedirection() {
        final ListNode list = this.parse("echo hello >> out.txt");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.APPEND, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseClobberRedirection() {
        final ListNode list = this.parse("echo test >| file.txt");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertTrue(cmd.getRedirections().size() >= 1);
        final boolean clobber = cmd.getRedirections().stream()
            .anyMatch(r -> r.getType() == RedirectionType.CLOBBER);
        Assertions.assertTrue(clobber);
    }

    @Test
    void parseCommandWithDollar() {
        final ListNode list = this.parse("echo $VAR");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals("echo", cmd.getName());
        Assertions.assertEquals("$VAR", cmd.getArguments().get(0));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparators() {
        final ListNode list = this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5");
        Assertions.assertEquals(5, list.getPipelines().size());
        Assertions.assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
        Assertions.assertEquals(ListNode.Separator.AMP, list.getSeparators().get(1));
        Assertions.assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
        Assertions.assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(3));
    }

    @Test
    void parseEmptyPipeline() {
        Assertions.assertEquals(1, this.parse("").getPipelines().size());
    }

    @Test
    void parseSubshell() {
        final ListNode list = this.parse("(echo hello)");
        Assertions.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
        Assertions.assertInstanceOf(Subshell.class, pipeline.getCommands().get(0));
        final Subshell sub = (Subshell) pipeline.getCommands().get(0);
        Assertions.assertEquals(1, sub.getList().getPipelines().size());
        Assertions.assertEquals("echo", ((SimpleCommand) sub.getList().getPipelines().get(0).getCommands().get(0)).getName());
    }

    @Test
    void parseSubshellWithMultiplePipelines() {
        final ListNode list = this.parse("(echo a; echo b)");
        Assertions.assertEquals(2, ((Subshell) list.getPipelines().get(0).getCommands().get(0)).getList().getPipelines().size());
    }

    @Test
    void parseSubshellInPipeline() {
        final ListNode list = this.parse("echo foo | (cat) | wc");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(3, pipeline.getCommands().size());
        Assertions.assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(0));
        Assertions.assertInstanceOf(Subshell.class, pipeline.getCommands().get(1));
        Assertions.assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(2));
    }

    @Test
    void parseGrouping() {
        final ListNode list = this.parse("{ echo hello; }");
        Assertions.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
        Assertions.assertInstanceOf(Grouping.class, pipeline.getCommands().get(0));
        final Grouping glob = (Grouping) pipeline.getCommands().get(0);
        Assertions.assertEquals(1, glob.getList().getPipelines().size());
        Assertions.assertEquals("echo", ((SimpleCommand) glob.getList().getPipelines().get(0).getCommands().get(0)).getName());
    }

    @Test
    void parseGroupingInPipeline() {
        final ListNode list = this.parse("echo foo | { cat; } | wc");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(3, pipeline.getCommands().size());
        Assertions.assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(0));
        Assertions.assertInstanceOf(Grouping.class, pipeline.getCommands().get(1));
        Assertions.assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(2));
    }

    @Test
    void parseExpectThrowsOnMissingWord() {
        Assertions.assertThrows(ParseException.class, () -> this.parse("echo <"));
    }

    @Test
    void parseCommandStartingWithInputRedirect() {
        final ListNode list = this.parse("< input.txt cat");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals("<", cmd.getName());
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithOutputRedirect() {
        final ListNode list = this.parse("> out.txt echo");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(">", cmd.getName());
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithClobberRedirect() {
        final ListNode list = this.parse(">| file.txt echo");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(">|", cmd.getName());
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithAppendRedirect() {
        final ListNode list = this.parse(">> file.txt echo");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(">>", cmd.getName());
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseAssignmentWordAfterCommandName() {
        final ListNode list = this.parse("echo FOO=bar");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals("echo", cmd.getName());
        Assertions.assertTrue(cmd.getAssignments().contains("FOO=bar"));
    }

    @Test
    void parseIoNumberRedirect() {
        final ListNode list = this.parse("2>file");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupInputRedirect() {
        final ListNode list = this.parse("cmd <&3");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals("cmd", cmd.getName());
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("3", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseCloseInputRedirect() {
        final ListNode list = this.parse("cmd <&-");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("-", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupOutputRedirect() {
        final ListNode list = this.parse("cmd >&2");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.DUP_OUTPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("2", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseReadWriteRedirect() {
        final ListNode list = this.parse("cmd <>file");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.READWRITE, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseIfThenFi() {
        final ListNode list = this.parse("if true; then echo hi; fi");
        Assertions.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
        Assertions.assertInstanceOf(IfNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseIfThenElseFi() {
        final ListNode list = this.parse("if false; then echo a; else echo b; fi");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertInstanceOf(IfNode.class, pipeline.getCommands().get(0));
        final IfNode num = (IfNode) pipeline.getCommands().get(0);
        Assertions.assertNotNull(num.getElseBody());
    }

    @Test
    void parseForDoDone() {
        final ListNode list = this.parse("for var in a b c; do echo $var; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
        Assertions.assertInstanceOf(ForNode.class, pipeline.getCommands().get(0));
        final ForNode num = (ForNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("var", num.getVariable());
        Assertions.assertEquals(3, num.getWords().size());
        Assertions.assertEquals("a", num.getWords().get(0));
        Assertions.assertEquals("c", num.getWords().get(2));
    }

    @Test
    void parseWhileDoDone() {
        final ListNode list = this.parse("while true; do echo hi; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertInstanceOf(WhileNode.class, pipeline.getCommands().get(0));
        final WhileNode num = (WhileNode) pipeline.getCommands().get(0);
        Assertions.assertFalse(num.isUntil());
    }

    @Test
    void parseUntilDoDone() {
        final ListNode list = this.parse("until false; do echo hi; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertInstanceOf(WhileNode.class, pipeline.getCommands().get(0));
        final WhileNode num = (WhileNode) pipeline.getCommands().get(0);
        Assertions.assertTrue(num.isUntil());
    }

    @Test
    void parseCaseSimple() {
        final ListNode list = this.parse("case foo in pat) echo match;; esac");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertInstanceOf(CaseNode.class, pipeline.getCommands().get(0));
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("foo", num.getWord());
        Assertions.assertEquals(1, num.getItems().size());
        Assertions.assertEquals("pat", num.getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseCasePipePattern() {
        final ListNode list = this.parse("case foo in a|b) echo match;; esac");
        final Pipeline pipeline = list.getPipelines().get(0);
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals(2, num.getItems().get(0).getPatterns().size());
        Assertions.assertEquals("a", num.getItems().get(0).getPatterns().get(0));
        Assertions.assertEquals("b", num.getItems().get(0).getPatterns().get(1));
    }

    @Test
    void parseCaseMultipleItems() {
        final ListNode list = this.parse("case foo in a) echo a;; b) echo b;; esac");
        final Pipeline pipeline = list.getPipelines().get(0);
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals(2, num.getItems().size());
    }

    @Test
    void parseCaseEmptyBody() {
        final ListNode list = this.parse("case foo in a);; esac");
        final Pipeline pipeline = list.getPipelines().get(0);
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals(1, num.getItems().size());
    }

    @Test
    void parseCaseParenPattern() {
        final ListNode list = this.parse("case foo in (pat) echo match;; esac");
        final Pipeline pipeline = list.getPipelines().get(0);
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("pat", num.getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseFunctionDefinition() {
        final ListNode list = this.parse("foo() { echo bar; }");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
        final FunctionNode num = (FunctionNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("foo", num.getName());
    }

    @Test
    void parseFunctionSubshellBody() {
        final ListNode list = this.parse("foo() (echo bar)");
        final Pipeline pipeline = list.getPipelines().get(0);
        final FunctionNode num = (FunctionNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("foo", num.getName());
        Assertions.assertInstanceOf(Subshell.class, num.getBody());
    }

    @Test
    void parseIfInPipeline() {
        final ListNode list = this.parse("true | if true; then echo hi; fi");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(0));
        Assertions.assertInstanceOf(IfNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseForInPipeline() {
        final ListNode list = this.parse("true | for var in a; do echo $var; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertInstanceOf(ForNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseWhileInPipeline() {
        final ListNode list = this.parse("true | while false; do echo hi; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertInstanceOf(WhileNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseCaseInPipeline() {
        final ListNode list = this.parse("true | case foo in pat) echo hi;; esac");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertInstanceOf(CaseNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseFunctionInPipeline() {
        final ListNode list = this.parse("true | foo() { echo bar; }");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseExpectWordThrows() {
        Assertions.assertThrows(ParseException.class, () -> this.parse("if true; then echo hi"));
    }

    @Test
    void parseFunctionBodyWithIf() {
        Assertions.assertInstanceOf(FunctionNode.class, this.parse("foo() if true; then echo hi; fi").getPipelines().get(0).getCommands().get(0));
    }

    @Test
    void parseFunctionBodyWithFor() {
        Assertions.assertInstanceOf(FunctionNode.class, this.parse("foo() for var in a; do echo $var; done").getPipelines().get(0).getCommands().get(0));
    }

    @Test
    void parseFunctionBodyWithWhile() {
        Assertions.assertInstanceOf(FunctionNode.class, this.parse("foo() while false; do echo hi; done").getPipelines().get(0).getCommands().get(0));
    }

    @Test
    void parseFunctionBodyWithCase() {
        Assertions.assertInstanceOf(FunctionNode.class, this.parse("foo() case foo in pat) echo hi;; esac").getPipelines().get(0).getCommands().get(0));
    }

    @Test
    void parseFunctionBodyException() {
        Assertions.assertThrows(ParseException.class, () -> this.parse("foo() bar"));
    }

    @Test
    void parseEmptyWhileCondition() {
        Assertions.assertEquals(1, this.parse("while do; echo hi; done").getPipelines().size());
    }

    @Test
    void parseAssignmentWithoutCommand() {
        final ListNode list = this.parse("FOO=bar");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals("", cmd.getName());
        Assertions.assertEquals(1, cmd.getAssignments().size());
        Assertions.assertEquals("FOO=bar", cmd.getAssignments().get(0));
    }

    @Test
    void parseMultipleAssignmentsThenCommand() {
        final ListNode list = this.parse("FOO=1 BAR=2 cmd arg");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals("cmd", cmd.getName());
        Assertions.assertEquals(2, cmd.getAssignments().size());
        Assertions.assertEquals("arg", cmd.getArguments().get(0));
    }

    @Test
    void parseInlineAssignmentMidCommand() {
        final ListNode list = this.parse("cmd FOO=1 arg");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals("cmd", cmd.getName());
        Assertions.assertEquals(1, cmd.getAssignments().size());
        Assertions.assertEquals("FOO=1", cmd.getAssignments().get(0));
    }

    @Test
    void parseElifChainNestedElif() {
        final ListNode list = this.parse(
            "if false; then a; elif false; then b; elif true; then c; fi"
        );
        Assertions.assertEquals(1, list.getPipelines().size());
        Assertions.assertInstanceOf(IfNode.class, list.getPipelines().get(0).getCommands().get(0));
    }

    @Test
    void parseElifChainWithElse() {
        Assertions.assertEquals(1, this.parse("if false; then a; elif false; then b; else c; fi").getPipelines().size());
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
        Assertions.assertEquals(1, this.parse("if true; then echo a; echo b; fi").getPipelines().size());
    }

    @Test
    void parsePipelineEmptyPipeStage() {
        Assertions.assertEquals(1, this.parse("if echo a | echo b; then echo c; fi").getPipelines().size());
    }

    @Test
    void parseHereDocRedirect() {
        final ListNode list = this.parse(String.format("cat <<EOF%nhello%nEOF%n"));
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseCaseMultilineBody() {
        final ListNode list = this.parse(String.format("case x in%n  a) echo aa;;%n  b) echo bb;;%nesac"));
        Assertions.assertEquals(1, list.getPipelines().size());
        Assertions.assertEquals(2, ((CaseNode) list.getPipelines().get(0).getCommands().get(0)).getItems().size());
    }

    @Test
    void parseCaseMultilineWithBlankLinesBetweenArms() {
        final ListNode list = this.parse(String.format("case y in%n%n  a) echo aa;;%n%n  b) echo bb;;%n%nesac"));
        Assertions.assertEquals(1, list.getPipelines().size());
        Assertions.assertEquals(2, ((CaseNode) list.getPipelines().get(0).getCommands().get(0)).getItems().size());
    }

    @Test
    void parseHereDocStripTabs() {
        final ListNode list = this.parse(String.format("cat <<-EOF%n\thello%nEOF%n"));
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseSeparatorsInList() {
        final ListNode list = this.parse("cmd1 & cmd2 && cmd3 || cmd4");
        Assertions.assertEquals(4, list.getPipelines().size());
        Assertions.assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
        Assertions.assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(1));
        Assertions.assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
    }

    @Test
    void parseWhileWithAmpInCondition() {
        Assertions.assertEquals(1, this.parse("while true & false; do echo hi; done").getPipelines().size());
    }

    @Test
    void parseWhileWithAndIfInCondition() {
        Assertions.assertEquals(1, this.parse("while true && false; do echo hi; done").getPipelines().size());
    }

    @Test
    void parseWhileWithOrIfInCondition() {
        Assertions.assertEquals(1, this.parse("while true || false; do echo hi; done").getPipelines().size());
    }

    @Test
    void redirectionInputConstructor() {
        final Redirection result = new Redirection(0, RedirectionType.INPUT, "/dev/null");
        Assertions.assertEquals(0, result.getFd());
        Assertions.assertEquals(RedirectionType.INPUT, result.getType());
        Assertions.assertEquals("/dev/null", result.getTarget());
    }

    @Test
    void redirectionOutputConstructor() {
        final Redirection result = new Redirection(1, RedirectionType.OUTPUT, "/tmp/out");
        Assertions.assertEquals(1, result.getFd());
        Assertions.assertEquals(RedirectionType.OUTPUT, result.getType());
        Assertions.assertEquals("/tmp/out", result.getTarget());
    }

    @Test
    void redirectionAppendConstructor() {
        final Redirection result = new Redirection(2, RedirectionType.APPEND, "/tmp/log");
        Assertions.assertEquals(2, result.getFd());
        Assertions.assertEquals(RedirectionType.APPEND, result.getType());
        Assertions.assertEquals("/tmp/log", result.getTarget());
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }

}
