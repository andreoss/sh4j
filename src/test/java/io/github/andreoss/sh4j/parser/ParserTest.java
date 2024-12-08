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
        ListNode list = parser("echo hello").parse();
        assertEquals(1, list.getPipelines().size());
        
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        
        SimpleCommand cmd = (SimpleCommand) pipeline.getCommands().get(0);
        assertEquals("echo", cmd.getName());
        assertEquals(1, cmd.getArguments().size());
        assertEquals("hello", cmd.getArguments().get(0));
    }

    @Test
    void parsePipeline() {
        ListNode list = parser("echo a | grep b").parse();
        assertEquals(1, list.getPipelines().size());
        
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertEquals("echo", ((SimpleCommand) pipeline.getCommands().get(0)).getName());
        assertEquals("grep", ((SimpleCommand) pipeline.getCommands().get(1)).getName());
    }

    @Test
    void parseListWithSemicolon() {
        ListNode list = parser("cmd1 ; cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAnd() {
        ListNode list = parser("cmd1 && cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithOr() {
        ListNode list = parser("cmd1 || cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(0));
    }

    @Test
    void parseListWithAmp() {
        ListNode list = parser("cmd1 & cmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
    }

    @Test
    void parseListWithNewlineSeparator() {
        ListNode list = parser("cmd1\ncmd2").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
        assertEquals("cmd1", ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName());
        assertEquals("cmd2", ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName());
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlines() {
        ListNode list = parser("\n\ncmd1\n\n\ncmd2\n\n").parse();
        assertEquals(2, list.getPipelines().size());
        assertEquals("cmd1", ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName());
        assertEquals("cmd2", ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName());
    }

    @Test
    void parseBang() {
        ListNode list = parser("! echo hello").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertTrue(pipeline.isBang());
    }

    @Test
    void parseCommandWithNoArguments() {
        ListNode list = parser("ls").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("ls", cmd.getName());
        assertTrue(cmd.getArguments().isEmpty());
    }

    @Test
    void parseInputRedirection() {
        ListNode list = parser("cat < file.txt").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(0, cmd.getRedirections().get(0).getFd());
        assertEquals(RedirectionType.INPUT, cmd.getRedirections().get(0).getType());
        assertEquals("file.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseOutputRedirection() {
        ListNode list = parser("echo hello > out.txt").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(1, cmd.getRedirections().get(0).getFd());
        assertEquals(RedirectionType.OUTPUT, cmd.getRedirections().get(0).getType());
        assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseAppendRedirection() {
        ListNode list = parser("echo hello >> out.txt").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.APPEND, cmd.getRedirections().get(0).getType());
        assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseClobberRedirection() {
        ListNode list = parser("echo test >| file.txt").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertTrue(cmd.getRedirections().size() >= 1);
        boolean hasClobber = cmd.getRedirections().stream()
            .anyMatch(r -> r.getType() == RedirectionType.CLOBBER);
        assertTrue(hasClobber);
    }

    @Test
    void parseCommandWithDollar() {
        ListNode list = parser("echo $VAR").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("echo", cmd.getName());
        assertEquals("$VAR", cmd.getArguments().get(0));
    }

    @Test
    void parseMultipleCommandsWithMultipleSeparators() {
        ListNode list = parser("cmd1; cmd2 & cmd3 || cmd4 && cmd5").parse();
        assertEquals(5, list.getPipelines().size());
        assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
        assertEquals(ListNode.Separator.AMP, list.getSeparators().get(1));
        assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
        assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(3));
    }

    @Test
    void parseEmptyPipeline() {
        ListNode list = parser("").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseSubshell() {
        ListNode list = parser("(echo hello)").parse();
        assertEquals(1, list.getPipelines().size());
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        assertInstanceOf(Subshell.class, pipeline.getCommands().get(0));
        Subshell sub = (Subshell) pipeline.getCommands().get(0);
        assertEquals(1, sub.getList().getPipelines().size());
        SimpleCommand inner = (SimpleCommand) sub.getList().getPipelines().get(0).getCommands().get(0);
        assertEquals("echo", inner.getName());
    }

    @Test
    void parseSubshellWithMultiplePipelines() {
        ListNode list = parser("(echo a; echo b)").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        Subshell sub = (Subshell) pipeline.getCommands().get(0);
        assertEquals(2, sub.getList().getPipelines().size());
    }

    @Test
    void parseSubshellInPipeline() {
        ListNode list = parser("echo foo | (cat) | wc").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(3, pipeline.getCommands().size());
        assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(0));
        assertInstanceOf(Subshell.class, pipeline.getCommands().get(1));
        assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(2));
    }

    @Test
    void parseGrouping() {
        ListNode list = parser("{ echo hello; }").parse();
        assertEquals(1, list.getPipelines().size());
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        assertInstanceOf(Grouping.class, pipeline.getCommands().get(0));
        Grouping g = (Grouping) pipeline.getCommands().get(0);
        assertEquals(1, g.getList().getPipelines().size());
        SimpleCommand inner = (SimpleCommand) g.getList().getPipelines().get(0).getCommands().get(0);
        assertEquals("echo", inner.getName());
    }

    @Test
    void parseGroupingInPipeline() {
        ListNode list = parser("echo foo | { cat; } | wc").parse();
        Pipeline pipeline = list.getPipelines().get(0);
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
        ListNode list = parser("< input.txt cat").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("<", cmd.getName());
        assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithOutputRedirect() {
        ListNode list = parser("> out.txt echo").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(">", cmd.getName());
        assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithClobberRedirect() {
        ListNode list = parser(">| file.txt echo").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(">|", cmd.getName());
        assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithAppendRedirect() {
        ListNode list = parser(">> file.txt echo").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(">>", cmd.getName());
        assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseAssignmentWordAfterCommandName() {
        ListNode list = parser("echo FOO=bar").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("echo", cmd.getName());
        assertTrue(cmd.getAssignments().contains("FOO=bar"));
    }

    @Test
    void parseIoNumberRedirect() {
        ListNode list = parser("2>file").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupInputRedirect() {
        ListNode list = parser("cmd <&3").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("cmd", cmd.getName());
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        assertEquals("3", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseCloseInputRedirect() {
        ListNode list = parser("cmd <&-").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        assertEquals("-", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupOutputRedirect() {
        ListNode list = parser("cmd >&2").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.DUP_OUTPUT, cmd.getRedirections().get(0).getType());
        assertEquals("2", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseReadWriteRedirect() {
        ListNode list = parser("cmd <>file").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.READWRITE, cmd.getRedirections().get(0).getType());
        assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseIfThenFi() {
        ListNode list = parser("if true; then echo hi; fi").parse();
        assertEquals(1, list.getPipelines().size());
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        assertInstanceOf(IfNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseIfThenElseFi() {
        ListNode list = parser("if false; then echo a; else echo b; fi").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(IfNode.class, pipeline.getCommands().get(0));
        IfNode n = (IfNode) pipeline.getCommands().get(0);
        assertNotNull(n.getElseBody());
    }

    @Test
    void parseForDoDone() {
        ListNode list = parser("for var in a b c; do echo $var; done").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(1, pipeline.getCommands().size());
        assertInstanceOf(ForNode.class, pipeline.getCommands().get(0));
        ForNode n = (ForNode) pipeline.getCommands().get(0);
        assertEquals("var", n.getVariable());
        assertEquals(3, n.getWords().size());
        assertEquals("a", n.getWords().get(0));
        assertEquals("c", n.getWords().get(2));
    }

    @Test
    void parseWhileDoDone() {
        ListNode list = parser("while true; do echo hi; done").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(WhileNode.class, pipeline.getCommands().get(0));
        WhileNode n = (WhileNode) pipeline.getCommands().get(0);
        assertFalse(n.isUntil());
    }

    @Test
    void parseUntilDoDone() {
        ListNode list = parser("until false; do echo hi; done").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(WhileNode.class, pipeline.getCommands().get(0));
        WhileNode n = (WhileNode) pipeline.getCommands().get(0);
        assertTrue(n.isUntil());
    }

    @Test
    void parseCaseSimple() {
        ListNode list = parser("case foo in pat) echo match;; esac").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(CaseNode.class, pipeline.getCommands().get(0));
        CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals("foo", n.getWord());
        assertEquals(1, n.getItems().size());
        assertEquals("pat", n.getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseCasePipePattern() {
        ListNode list = parser("case foo in a|b) echo match;; esac").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals(2, n.getItems().get(0).getPatterns().size());
        assertEquals("a", n.getItems().get(0).getPatterns().get(0));
        assertEquals("b", n.getItems().get(0).getPatterns().get(1));
    }

    @Test
    void parseCaseMultipleItems() {
        ListNode list = parser("case foo in a) echo a;; b) echo b;; esac").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals(2, n.getItems().size());
    }

    @Test
    void parseCaseEmptyBody() {
        ListNode list = parser("case foo in a);; esac").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals(1, n.getItems().size());
    }

    @Test
    void parseCaseParenPattern() {
        ListNode list = parser("case foo in (pat) echo match;; esac").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        CaseNode n = (CaseNode) pipeline.getCommands().get(0);
        assertEquals("pat", n.getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseFunctionDefinition() {
        ListNode list = parser("foo() { echo bar; }").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
        FunctionNode n = (FunctionNode) pipeline.getCommands().get(0);
        assertEquals("foo", n.getName());
    }

    @Test
    void parseFunctionSubshellBody() {
        ListNode list = parser("foo() (echo bar)").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        FunctionNode n = (FunctionNode) pipeline.getCommands().get(0);
        assertEquals("foo", n.getName());
        assertInstanceOf(Subshell.class, n.getBody());
    }

    @Test
    void parseIfInPipeline() {
        ListNode list = parser("true | if true; then echo hi; fi").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(SimpleCommand.class, pipeline.getCommands().get(0));
        assertInstanceOf(IfNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseForInPipeline() {
        ListNode list = parser("true | for var in a; do echo $var; done").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(ForNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseWhileInPipeline() {
        ListNode list = parser("true | while false; do echo hi; done").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(WhileNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseCaseInPipeline() {
        ListNode list = parser("true | case foo in pat) echo hi;; esac").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(CaseNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseFunctionInPipeline() {
        ListNode list = parser("true | foo() { echo bar; }").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertEquals(2, pipeline.getCommands().size());
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(1));
    }

    @Test
    void parseExpectWordThrows() {
        assertThrows(ParseException.class, () -> parser("if true; then echo hi").parse());
    }

    @Test
    void parseFunctionBodyWithIf() {
        ListNode list = parser("foo() if true; then echo hi; fi").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseFunctionBodyWithFor() {
        ListNode list = parser("foo() for var in a; do echo $var; done").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseFunctionBodyWithWhile() {
        ListNode list = parser("foo() while false; do echo hi; done").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseFunctionBodyWithCase() {
        ListNode list = parser("foo() case foo in pat) echo hi;; esac").parse();
        Pipeline pipeline = list.getPipelines().get(0);
        assertInstanceOf(FunctionNode.class, pipeline.getCommands().get(0));
    }

    @Test
    void parseFunctionBodyException() {
        assertThrows(ParseException.class, () -> parser("foo() bar").parse());
    }

    @Test
    void parseEmptyWhileCondition() {
        ListNode list = parser("while do; echo hi; done").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseAssignmentWithoutCommand() {
        ListNode list = parser("FOO=bar").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("", cmd.getName());
        assertEquals(1, cmd.getAssignments().size());
        assertEquals("FOO=bar", cmd.getAssignments().get(0));
    }

    @Test
    void parseMultipleAssignmentsThenCommand() {
        ListNode list = parser("FOO=1 BAR=2 cmd arg").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("cmd", cmd.getName());
        assertEquals(2, cmd.getAssignments().size());
        assertEquals("arg", cmd.getArguments().get(0));
    }

    @Test
    void parseInlineAssignmentMidCommand() {
        ListNode list = parser("cmd FOO=1 arg").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals("cmd", cmd.getName());
        assertEquals(1, cmd.getAssignments().size());
        assertEquals("FOO=1", cmd.getAssignments().get(0));
    }

    @Test
    void parseElifChainNestedElif() {
        ListNode list = parser("if false; then a; elif false; then b; elif true; then c; fi").parse();
        assertEquals(1, list.getPipelines().size());
        Pipeline p = list.getPipelines().get(0);
        assertInstanceOf(IfNode.class, p.getCommands().get(0));
    }

    @Test
    void parseElifChainWithElse() {
        ListNode list = parser("if false; then a; elif false; then b; else c; fi").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseHereDocRedirect() {
        ListNode list = parser("cat <<EOF\nhello\nEOF\n").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseHereDocStripTabs() {
        ListNode list = parser("cat <<-EOF\n\thello\nEOF\n").parse();
        SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        assertEquals(1, cmd.getRedirections().size());
        assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseSeparatorsInList() {
        ListNode list = parser("cmd1 & cmd2 && cmd3 || cmd4").parse();
        assertEquals(4, list.getPipelines().size());
        assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
        assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(1));
        assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
    }

    @Test
    void parseWhileWithAmpInCondition() {
        ListNode list = parser("while true & false; do echo hi; done").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseWhileWithAndIfInCondition() {
        ListNode list = parser("while true && false; do echo hi; done").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void parseWhileWithOrIfInCondition() {
        ListNode list = parser("while true || false; do echo hi; done").parse();
        assertEquals(1, list.getPipelines().size());
    }

    @Test
    void redirectionInputFactory() {
        Redirection r = Redirection.input(0, "/dev/null");
        assertEquals(0, r.getFd());
        assertEquals(RedirectionType.INPUT, r.getType());
        assertEquals("/dev/null", r.getTarget());
    }

    @Test
    void redirectionOutputFactory() {
        Redirection r = Redirection.output(1, "/tmp/out");
        assertEquals(1, r.getFd());
        assertEquals(RedirectionType.OUTPUT, r.getType());
        assertEquals("/tmp/out", r.getTarget());
    }

    @Test
    void redirectionAppendFactory() {
        Redirection r = Redirection.append(2, "/tmp/log");
        assertEquals(2, r.getFd());
        assertEquals(RedirectionType.APPEND, r.getType());
        assertEquals("/tmp/log", r.getTarget());
    }
}
