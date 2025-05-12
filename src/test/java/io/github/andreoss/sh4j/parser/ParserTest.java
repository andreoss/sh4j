/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;

class ParserTest {
    private Parser parser(String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    @Test
    void parseSimpleCommand() {
        final ListNode list = parser("echo hello").parse();
        assertEquals(1, list.getPipelines().size());
        
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        
        final SimpleCommand cmd = (SimpleCommand) pipeline.getCommands().get(0);
        assertEquals("echo", cmd.getName());
        assertEquals(1, cmd.getArguments().size());
        assertEquals("hello", cmd.getArguments().get(0));
    }

    @Test
    void parsePipeline() {
        final ListNode list = parser("echo a | grep b").parse();
        assertEquals(1, list.getPipelines().size());
        
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertEquals("echo", ((SimpleCommand) pipeline.getCommands().get(0)).getName());
        assertEquals("grep", ((SimpleCommand) pipeline.getCommands().get(1)).getName());
    }

    @Test
    void parseListWithSemicolon() {
        final ListNode list = parser("cmd1 ; cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAnd() {
        final ListNode list = parser("cmd1 && cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithOr() {
        final ListNode list = parser("cmd1 || cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAmp() {
        final ListNode list = parser("cmd1 & cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
    }

    @Test
    void parseListWithNewlineSeparator() {
        final ListNode list = parser("cmd1\ncmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
        assertEquals("cmd1", ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName());
        assertEquals("cmd2", ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName());
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlines() {
        final ListNode list = parser("\n\ncmd1\n\n\ncmd2\n\n").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals("cmd1", ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName());
        assertEquals("cmd2", ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName());
    }

    @Test
    void parseBang() {
        final ListNode list = parser("! echo hello").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertTrue(pipeline.isBang());
    }

    @Test
    void parseCommandWithNoArguments() {
        final ListNode list = parser("ls").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("ls", cmd.getName());
        assertTrue(cmd.getArguments().isEmpty());
    }

    @Test
    void parseInputRedirection() {
        final ListNode list = parser("cat < file.txt").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(0, cmd.getRedirections().get(0).getFd());
        assertEquals(RedirectionType.INPUT, cmd.getRedirections().get(0).getType());
        assertEquals("file.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseOutputRedirection() {
        final ListNode list = parser("echo hello > out.txt").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(1, cmd.getRedirections().get(0).getFd());
        assertEquals(RedirectionType.OUTPUT, cmd.getRedirections().get(0).getType());
        assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseAppendRedirection() {
        final ListNode list = parser("echo hello >> out.txt").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.APPEND, cmd.getRedirections().get(0).getType());
        assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseClobberRedirection() {
        final ListNode list = parser("echo test >| file.txt").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertTrue(cmd.getRedirections().size() >= 1);
        final boolean hasClobber = cmd.getRedirections().stream()
            .anyMatch(r -> r.getType() == RedirectionType.CLOBBER);
        assertTrue(hasClobber);
    }

    @Test
    void parseCommandWithDollar() {
        final ListNode list = parser("echo $VAR").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("echo", cmd.getName());
        assertEquals("$VAR", cmd.getArguments().get(0));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparators() {
        final ListNode list = parser("cmd1; cmd2 & cmd3 || cmd4 && cmd5").parse();
        assertEquals(5, list.getPipelines().size());
        assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
        assertEquals(ListNode.Separator.AMP, list.getSeparators().get(1));
        assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
        assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(3));
    }

    @Test
    void parseEmptyPipeline() {
        final ListNode list = parser("").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseSubshell() {
        final ListNode list = parser("(echo hello)").parse();
        assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        assertInstanceOf(Subshell.class, pipeline.getCommands().get(0));
        final Subshell sub = (Subshell) pipeline.getCommands().get(0);
        assertEquals(1, sub.getList().getPipelines().size());
        final SimpleCommand inner = (SimpleCommand) sub.getList().getPipelines().get(0).getCommands().get(0);
        assertEquals("echo", inner.getName());
    }

    @Test
    void parseSubshellWithMultiplePipelines() {
        final ListNode list = parser("(echo a; echo b)").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        final Subshell sub = (Subshell) pipeline.getCommands().get(0);
        assertEquals(2, sub.getList().getPipelines().size());
    }

    @Test
    void parseSubshellInPipeline() {
        final ListNode list = parser("echo foo | (cat) | wc").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(3, pipeline.getCommands().size());
        assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(0));
        assertInstanceOf(Subshell.class, pipeline.getCommands().get(1));
        assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(2));
    }

    @Test
    void parseGrouping() {
        final ListNode list = parser("{ echo hello; }").parse();
        assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        assertInstanceOf(Grouping.class, pipeline.getCommands().get(0));
        final Grouping g = (Grouping) pipeline.getCommands().get(0);
        assertEquals(1, g.getList().getPipelines().size());
        final SimpleCommand inner = (SimpleCommand) g.getList().getPipelines().get(0).getCommands().get(0);
        assertEquals("echo", inner.getName());
    }

    @Test
    void parseGroupingInPipeline() {
        final ListNode list = parser("echo foo | { cat; } | wc").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(3, pipeline.getCommands().size());
        assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(0));
        assertInstanceOf(Grouping.class, pipeline.getCommands().get(1));
        assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(2));
    }

    @Test
    void parseExpectThrowsOnMissingWord() {
        assertThrows(ParseException.class, () -> parser("echo <").parse());
    }

    @Test
    void parseCommandStartingWithInputRedirect() {
        final ListNode list = parser("< input.txt cat").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("<", cmd.getName());
        assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithOutputRedirect() {
        final ListNode list = parser("> out.txt echo").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(">", cmd.getName());
        assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithClobberRedirect() {
        final ListNode list = parser(">| file.txt echo").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(">|", cmd.getName());
        assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithAppendRedirect() {
        final ListNode list = parser(">> file.txt echo").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(">>", cmd.getName());
        assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseAssignmentWordAfterCommandName() {
        final ListNode list = parser("echo FOO=bar").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("echo", cmd.getName());
        assertTrue(cmd.getAssignments().contains("FOO=bar"));
    }

    @Test
    void parseIoNumberRedirect() {
        final ListNode list = parser("2>file").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupInputRedirect() {
        final ListNode list = parser("cmd <&3").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("cmd", cmd.getName());
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        assertEquals("3", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseCloseInputRedirect() {
        final ListNode list = parser("cmd <&-").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        assertEquals("-", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupOutputRedirect() {
        final ListNode list = parser("cmd >&2").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.DUP_OUTPUT, cmd.getRedirections().get(0).getType());
        assertEquals("2", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseReadWriteRedirect() {
        final ListNode list = parser("cmd <>file").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.READWRITE, cmd.getRedirections().get(0).getType());
        assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseIfThenFi() {
        final ListNode list = parser("if true; then echo hi; fi").parse();
        assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        assertInstanceOf(IfNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseIfThenElseFi() {
        final ListNode list = parser("if false; then echo a; else echo b; fi").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(IfNode.class, pipeline.getCommands().get(0));
        final IfNode n = (IfNode) pipeline.getCommands().get(0);
        assertNotNull(n.getElseBody());
    }

    @Test
    void parseForDoDone() {
        final ListNode list = parser("for var in a b c; do echo $var; done").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        assertInstanceOf(ForNode.class, pipeline.getCommands().get(0));
        final ForNode n = (ForNode) pipeline.getCommands().get(0);
        assertEquals("var", n.getVariable());
        assertEquals(3, n.getWords().size());
        assertEquals("a", n.getWords().get(0));
        assertEquals("c", n.getWords().get(2));
    }

    @Test
    void parseWhileDoDone() {
        final ListNode list = parser("while true; do echo hi; done").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(WhileNode.class, pipeline.getCommands().get(0));
        final WhileNode n = (WhileNode) pipeline.getCommands().get(0);
        assertFalse(n.isUntil());
    }

    @Test
    void parseUntilDoDone() {
        final ListNode list = parser("until false; do echo hi; done").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(WhileNode.class, pipeline.getCommands().get(0));
        final WhileNode n = (WhileNode) pipeline.getCommands().get(0);
        assertTrue(n.isUntil());
    }

    @Test
    void parseCaseSimple() {
        final ListNode list = parser("case foo in pat) echo match;; esac").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(CaseNode.class, pipeline.getCommands().get(0));
        final CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals("foo", n.getWord());
        assertEquals(1, n.getItems().size());
        assertEquals("pat", n.getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseCasePipePattern() {
        final ListNode list = parser("case foo in a|b) echo match;; esac").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        final CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals(2, n.getItems().get(0).getPatterns().size());
        assertEquals("a", n.getItems().get(0).getPatterns().get(0));
        assertEquals("b", n.getItems().get(0).getPatterns().get(1));
    }

    @Test
    void parseCaseMultipleItems() {
        final ListNode list = parser("case foo in a) echo a;; b) echo b;; esac").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        final CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals(2, n.getItems().size());
    }

    @Test
    void parseCaseEmptyBody() {
        final ListNode list = parser("case foo in a);; esac").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        final CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals(1, n.getItems().size());
    }

    @Test
    void parseCaseParenPattern() {
        final ListNode list = parser("case foo in (pat) echo match;; esac").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        final CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals("pat", n.getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseFunctionDefinition() {
        final ListNode list = parser("foo() { echo bar; }").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
        final FunctionNode n = (FunctionNode) pipeline.getCommands().get(0);
        assertEquals("foo", n.getName());
    }

    @Test
    void parseFunctionSubshellBody() {
        final ListNode list = parser("foo() (echo bar)").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        final FunctionNode n = (FunctionNode) pipeline.getCommands().get(0);
        assertEquals("foo", n.getName());
        assertInstanceOf(Subshell.class, n.getBody());
    }

    @Test
    void parseIfInPipeline() {
        final ListNode list = parser("true | if true; then echo hi; fi").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(0));
        assertInstanceOf(IfNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseForInPipeline() {
        final ListNode list = parser("true | for var in a; do echo $var; done").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(ForNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseWhileInPipeline() {
        final ListNode list = parser("true | while false; do echo hi; done").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(WhileNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseCaseInPipeline() {
        final ListNode list = parser("true | case foo in pat) echo hi;; esac").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(CaseNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseFunctionInPipeline() {
        final ListNode list = parser("true | foo() { echo bar; }").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseExpectWordThrows() {
        assertThrows(ParseException.class, () -> parser("if true; then echo hi").parse());
    }

    @Test
    void parseFunctionBodyWithIf() {
        final ListNode list = parser("foo() if true; then echo hi; fi").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseFunctionBodyWithFor() {
        final ListNode list = parser("foo() for var in a; do echo $var; done").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseFunctionBodyWithWhile() {
        final ListNode list = parser("foo() while false; do echo hi; done").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseFunctionBodyWithCase() {
        final ListNode list = parser("foo() case foo in pat) echo hi;; esac").parse();
        final Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseFunctionBodyException() {
        assertThrows(ParseException.class, () -> parser("foo() bar").parse());
    }

    @Test
    void parseEmptyWhileCondition() {
        final ListNode list = parser("while do; echo hi; done").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseAssignmentWithoutCommand() {
        final ListNode list = parser("FOO=bar").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("", cmd.getName());
        assertEquals(1, cmd.getAssignments().size());
        assertEquals("FOO=bar", cmd.getAssignments().get(0));
    }

    @Test
    void parseMultipleAssignmentsThenCommand() {
        final ListNode list = parser("FOO=1 BAR=2 cmd arg").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("cmd", cmd.getName());
        assertEquals(2, cmd.getAssignments().size());
        assertEquals("arg", cmd.getArguments().get(0));
    }

    @Test
    void parseInlineAssignmentMidCommand() {
        final ListNode list = parser("cmd FOO=1 arg").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("cmd", cmd.getName());
        assertEquals(1, cmd.getAssignments().size());
        assertEquals("FOO=1", cmd.getAssignments().get(0));
    }

    @Test
    void parseElifChainNestedElif() {
        final ListNode list = parser("if false; then a; elif false; then b; elif true; then c; fi").parse();
        assertEquals(1, list.getPipelines().size());
        final Pipeline p = list.getPipelines().get(0);
        assertInstanceOf(IfNode.class, p.getCommands().get(0));
    }

    @Test
    void parseElifChainWithElse() {
        final ListNode list = parser("if false; then a; elif false; then b; else c; fi").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseTopLevelTrailingSemicolon() {
        final ListNode list = parser("echo a;").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseTopLevelTrailingAmpersand() {
        final ListNode list = parser("sleep 0 &").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseSubshellWithTrailingSemi() {
        final ListNode list = parser("(echo a;)").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseGroupingWithTrailingSemi() {
        final ListNode list = parser("{ echo a; }").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseEmptyIfThenBody() {
        final ListNode list = parser("if true; then fi").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseEmptyForBody() {
        final ListNode list = parser("for i in 1 2; do done").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseTwoPipelinesSemiTerminatedBeforeReserved() {
        // exercises the post-separator reserved-terminator break in parseList
        final ListNode list = parser("if true; then echo a; echo b; fi").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parsePipelineEmptyPipeStage() {
        // Pipe followed by `fi` -> second stage returns null, break loop
        final ListNode list = parser("if echo a | echo b; then echo c; fi").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseHereDocRedirect() {
        final ListNode list = parser("cat <<EOF\nhello\nEOF\n").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseCaseMultilineBody() {
        final ListNode list = parser("case x in\n  a) echo aa;;\n  b) echo bb;;\nesac").parse();
        assertEquals(1, list.getPipelines().size());
        final CaseNode c = (CaseNode) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(2, c.getItems().size());
    }

    @Test
    void parseCaseMultilineWithBlankLinesBetweenArms() {
        final ListNode list = parser("case y in\n\n  a) echo aa;;\n\n  b) echo bb;;\n\nesac").parse();
        assertEquals(1, list.getPipelines().size());
        final CaseNode c = (CaseNode) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(2, c.getItems().size());
    }

    @Test
    void parseHereDocStripTabs() {
        final ListNode list = parser("cat <<-EOF\n\thello\nEOF\n").parse();
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseSeparatorsInList() {
        final ListNode list = parser("cmd1 & cmd2 && cmd3 || cmd4").parse();
        assertEquals(4, list.getPipelines().size());
        assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
        assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(1));
        assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
    }

    @Test
    void parseWhileWithAmpInCondition() {
        final ListNode list = parser("while true & false; do echo hi; done").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseWhileWithAndIfInCondition() {
        final ListNode list = parser("while true && false; do echo hi; done").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseWhileWithOrIfInCondition() {
        final ListNode list = parser("while true || false; do echo hi; done").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void redirectionInputConstructor() {
        final Redirection r = new Redirection(0, RedirectionType.INPUT, "/dev/null");
        assertEquals(0, r.getFd());
        assertEquals(RedirectionType.INPUT, r.getType());
        assertEquals("/dev/null", r.getTarget());
    }

    @Test
    void redirectionOutputConstructor() {
        final Redirection r = new Redirection(1, RedirectionType.OUTPUT, "/tmp/out");
        assertEquals(1, r.getFd());
        assertEquals(RedirectionType.OUTPUT, r.getType());
        assertEquals("/tmp/out", r.getTarget());
    }

    @Test
    void redirectionAppendConstructor() {
        final Redirection r = new Redirection(2, RedirectionType.APPEND, "/tmp/log");
        assertEquals(2, r.getFd());
        assertEquals(RedirectionType.APPEND, r.getType());
        assertEquals("/tmp/log", r.getTarget());
    }
}
