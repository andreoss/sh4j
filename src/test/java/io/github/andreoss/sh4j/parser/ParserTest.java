/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import org.junit.Assert;
import org.junit.Test;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;

/**
 * Tests for Parser.
 * @since 1.0
 */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class ParserTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    public void parseSimpleCommand() {
        final ListNode list = this.parse("echo hello");
        Assert.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(1, pipeline.getCommands().size());
        final SimpleCommand cmd = (SimpleCommand) pipeline.getCommands().get(0);
        Assert.assertEquals(ParserTest.ECHO_CMD, cmd.getName());
        Assert.assertEquals(1, cmd.getArguments().size());
        Assert.assertEquals("hello", cmd.getArguments().get(0));
    }

    @Test
    public void parsePipeline() {
        final ListNode list = this.parse("echo a | grep b");
        Assert.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(2, pipeline.getCommands().size());
        Assert.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) pipeline.getCommands().get(0)).getName()
        );
        Assert.assertEquals("grep", ((SimpleCommand) pipeline.getCommands().get(1)).getName());
    }

    @Test
    public void parseListWithSemicolon() {
        final ListNode list = this.parse("cmd1 ; cmd2");
        Assert.assertEquals(2, list.getPipelines().size());
        Assert.assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
    }

    @Test
    public void parseListWithAnd() {
        final ListNode list = this.parse("cmd1 && cmd2");
        Assert.assertEquals(2, list.getPipelines().size());
        Assert.assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(0));
    }

    @Test
    public void parseListWithOr() {
        final ListNode list = this.parse("cmd1 || cmd2");
        Assert.assertEquals(2, list.getPipelines().size());
        Assert.assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(0));
    }

    @Test
    public void parseListWithAmp() {
        final ListNode list = this.parse("cmd1 & cmd2");
        Assert.assertEquals(2, list.getPipelines().size());
        Assert.assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
    }

    @Test
    public void parseListWithNewlineSeparator() {
        final ListNode list = this.parse(String.format("cmd1%ncmd2"));
        Assert.assertEquals(2, list.getPipelines().size());
        Assert.assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
        Assert.assertEquals(
            "cmd1",
            ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName()
        );
        Assert.assertEquals(
            "cmd2",
            ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName()
        );
    }

    @Test
    public void parseListWithBlankLinesAndLeadingTrailingNewlines() {
        final ListNode list = this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n"));
        Assert.assertEquals(2, list.getPipelines().size());
        Assert.assertEquals(
            "cmd1",
            ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName()
        );
        Assert.assertEquals(
            "cmd2",
            ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName()
        );
    }

    @Test
    public void parseBang() {
        Assert.assertTrue(this.parse("! echo hello").getPipelines().get(0).isBang());
    }

    @Test
    public void parseCommandWithNoArguments() {
        final ListNode list = this.parse("ls");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals("ls", cmd.getName());
        Assert.assertTrue(cmd.getArguments().isEmpty());
    }

    @Test
    public void parseInputRedirection() {
        final ListNode list = this.parse("cat < file.txt");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals(0, cmd.getRedirections().get(0).getFd());
        Assert.assertEquals(RedirectionType.INPUT, cmd.getRedirections().get(0).getType());
        Assert.assertEquals("file.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    public void parseOutputRedirection() {
        final ListNode list = this.parse("echo hello > out.txt");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals(1, cmd.getRedirections().get(0).getFd());
        Assert.assertEquals(RedirectionType.OUTPUT, cmd.getRedirections().get(0).getType());
        Assert.assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    public void parseAppendRedirection() {
        final ListNode list = this.parse("echo hello >> out.txt");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals(RedirectionType.APPEND, cmd.getRedirections().get(0).getType());
        Assert.assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    public void parseClobberRedirection() {
        final ListNode list = this.parse("echo test >| file.txt");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertTrue(cmd.getRedirections().size() >= 1);
        Assert.assertTrue(
            cmd.getRedirections().stream().anyMatch(r -> r.getType() == RedirectionType.CLOBBER)
        );
    }

    @Test
    public void parseCommandWithDollar() {
        final ListNode list = this.parse("echo $VAR");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(ParserTest.ECHO_CMD, cmd.getName());
        Assert.assertEquals("$VAR", cmd.getArguments().get(0));
    }

    @Test
    public void parseMultipleCommandsWithMultipleSeparators() {
        final ListNode list = this.parse("cmd1; cmd2 & cmd3 || cmd4 && cmd5");
        Assert.assertEquals(5, list.getPipelines().size());
        Assert.assertEquals(ListNode.Separator.SEMI, list.getSeparators().get(0));
        Assert.assertEquals(ListNode.Separator.AMP, list.getSeparators().get(1));
        Assert.assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
        Assert.assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(3));
    }

    @Test
    public void parseEmptyPipeline() {
        Assert.assertEquals(1, this.parse("").getPipelines().size());
    }

    @Test
    public void parseSubshell() {
        final ListNode list = this.parse("(echo hello)");
        Assert.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(1, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof Subshell);
        final Subshell sub = (Subshell) pipeline.getCommands().get(0);
        Assert.assertEquals(1, sub.getList().getPipelines().size());
        Assert.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) sub.getList().getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    public void parseSubshellWithMultiplePipelines() {
        Assert.assertEquals(
            2,
            (
                (Subshell) this.parse("(echo a; echo b)").getPipelines().get(0).getCommands().get(0)
            ).getList().getPipelines().size()
        );
    }

    @Test
    public void parseSubshellInPipeline() {
        final ListNode list = this.parse("echo foo | (cat) | wc");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(3, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof SimpleCommand);
        Assert.assertTrue(pipeline.getCommands().get(1) instanceof Subshell);
        Assert.assertTrue(pipeline.getCommands().get(2) instanceof SimpleCommand);
    }

    @Test
    public void parseGrouping() {
        final ListNode list = this.parse("{ echo hello; }");
        Assert.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(1, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof Grouping);
        final Grouping glob = (Grouping) pipeline.getCommands().get(0);
        Assert.assertEquals(1, glob.getList().getPipelines().size());
        Assert.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) glob.getList().getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    public void parseGroupingInPipeline() {
        final ListNode list = this.parse("echo foo | { cat; } | wc");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(3, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof SimpleCommand);
        Assert.assertTrue(pipeline.getCommands().get(1) instanceof Grouping);
        Assert.assertTrue(pipeline.getCommands().get(2) instanceof SimpleCommand);
    }

    @Test
    public void parseExpectThrowsOnMissingWord() {
        try {
            this.parse("echo <");
            Assert.fail("Expected ParseException");
        } catch (ParseException e) {
            Assert.assertEquals(ParseException.class, e.getClass());
        }
    }

    @Test
    public void parseCommandStartingWithInputRedirect() {
        final ListNode list = this.parse("< input.txt cat");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals("<", cmd.getName());
        Assert.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    public void parseCommandStartingWithOutputRedirect() {
        final ListNode list = this.parse("> out.txt echo");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(">", cmd.getName());
        Assert.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    public void parseCommandStartingWithClobberRedirect() {
        final ListNode list = this.parse(">| file.txt echo");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(">|", cmd.getName());
        Assert.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    public void parseCommandStartingWithAppendRedirect() {
        final ListNode list = this.parse(">> file.txt echo");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(">>", cmd.getName());
        Assert.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    public void parseAssignmentWordAfterCommandName() {
        final ListNode list = this.parse("echo FOO=bar");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(ParserTest.ECHO_CMD, cmd.getName());
        Assert.assertTrue(cmd.getAssignments().contains("FOO=bar"));
    }

    @Test
    public void parseIoNumberRedirect() {
        final ListNode list = this.parse("2>file");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    public void parseDupInputRedirect() {
        final ListNode list = this.parse("cmd <&3");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals("cmd", cmd.getName());
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        Assert.assertEquals("3", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    public void parseCloseInputRedirect() {
        final ListNode list = this.parse("cmd <&-");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        Assert.assertEquals("-", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    public void parseDupOutputRedirect() {
        final ListNode list = this.parse("cmd >&2");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals(RedirectionType.DUP_OUTPUT, cmd.getRedirections().get(0).getType());
        Assert.assertEquals("2", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    public void parseReadWriteRedirect() {
        final ListNode list = this.parse("cmd <>file");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals(RedirectionType.READWRITE, cmd.getRedirections().get(0).getType());
        Assert.assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    public void parseIfThenFi() {
        final ListNode list = this.parse("if true; then echo hi; fi");
        Assert.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(1, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof IfNode);
    }

    @Test
    public void parseIfThenElseFi() {
        final ListNode list = this.parse("if false; then echo a; else echo b; fi");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof IfNode);
        Assert.assertNotNull(((IfNode) pipeline.getCommands().get(0)).getElseBody());
    }

    @Test
    public void parseForDoDone() {
        final ListNode list = this.parse("for var in a b c; do echo $var; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(1, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof ForNode);
        final ForNode node = (ForNode) pipeline.getCommands().get(0);
        Assert.assertEquals("var", node.getVariable());
        Assert.assertEquals(3, node.getWords().size());
        Assert.assertEquals("a", node.getWords().get(0));
        Assert.assertEquals("c", node.getWords().get(2));
    }

    @Test
    public void parseWhileDoDone() {
        final ListNode list = this.parse("while true; do echo hi; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof WhileNode);
        Assert.assertFalse(((WhileNode) pipeline.getCommands().get(0)).isUntil());
    }

    @Test
    public void parseUntilDoDone() {
        final ListNode list = this.parse("until false; do echo hi; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof WhileNode);
        Assert.assertTrue(((WhileNode) pipeline.getCommands().get(0)).isUntil());
    }

    @Test
    public void parseCaseSimple() {
        final ListNode list = this.parse("case foo in pat) echo match;; esac");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof CaseNode);
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assert.assertEquals("foo", num.getWord());
        Assert.assertEquals(1, num.getItems().size());
        Assert.assertEquals("pat", num.getItems().get(0).getPatterns().get(0));
    }

    @Test
    public void parseCasePipePattern() {
        final ListNode list = this.parse("case foo in a|b) echo match;; esac");
        final Pipeline pipeline = list.getPipelines().get(0);
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assert.assertEquals(2, num.getItems().get(0).getPatterns().size());
        Assert.assertEquals("a", num.getItems().get(0).getPatterns().get(0));
        Assert.assertEquals("b", num.getItems().get(0).getPatterns().get(1));
    }

    @Test
    public void parseCaseMultipleItems() {
        Assert.assertEquals(
            2,
            ((CaseNode) this.parse("case foo in a) echo a;; b) echo b;; esac")
                .getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    public void parseCaseEmptyBody() {
        Assert.assertEquals(
            1,
            ((CaseNode) this.parse("case foo in a);; esac")
                .getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    public void parseCaseParenPattern() {
        Assert.assertEquals(
            "pat",
            ((CaseNode) this.parse("case foo in (pat) echo match;; esac")
                .getPipelines().get(0).getCommands().get(0))
                .getItems().get(0).getPatterns().get(0)
        );
    }

    @Test
    public void parseFunctionDefinition() {
        final ListNode list = this.parse("foo() { echo bar; }");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof FunctionNode);
        Assert.assertEquals("foo", ((FunctionNode) pipeline.getCommands().get(0)).getName());
    }

    @Test
    public void parseFunctionSubshellBody() {
        final ListNode list = this.parse("foo() (echo bar)");
        final Pipeline pipeline = list.getPipelines().get(0);
        final FunctionNode num = (FunctionNode) pipeline.getCommands().get(0);
        Assert.assertEquals("foo", num.getName());
        Assert.assertTrue(num.getBody() instanceof Subshell);
    }

    @Test
    public void parseIfInPipeline() {
        final ListNode list = this.parse("true | if true; then echo hi; fi");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(2, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(0) instanceof SimpleCommand);
        Assert.assertTrue(pipeline.getCommands().get(1) instanceof IfNode);
    }

    @Test
    public void parseForInPipeline() {
        final ListNode list = this.parse("true | for var in a; do echo $var; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(2, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(1) instanceof ForNode);
    }

    @Test
    public void parseWhileInPipeline() {
        final ListNode list = this.parse("true | while false; do echo hi; done");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(2, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(1) instanceof WhileNode);
    }

    @Test
    public void parseCaseInPipeline() {
        final ListNode list = this.parse("true | case foo in pat) echo hi;; esac");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(2, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(1) instanceof CaseNode);
    }

    @Test
    public void parseFunctionInPipeline() {
        final ListNode list = this.parse("true | foo() { echo bar; }");
        final Pipeline pipeline = list.getPipelines().get(0);
        Assert.assertEquals(2, pipeline.getCommands().size());
        Assert.assertTrue(pipeline.getCommands().get(1) instanceof FunctionNode);
    }

    @Test
    public void parseExpectWordThrows() {
        try {
            this.parse("if true; then echo hi");
            Assert.fail("Expected ParseException");
        } catch (ParseException e) {
            Assert.assertEquals(ParseException.class, e.getClass());
        }
    }

    @Test
    public void parseFunctionBodyWithIf() {
        Assert.assertTrue(
            this.parse("foo() if true; then echo hi; fi").getPipelines().get(0).getCommands().get(0)
                instanceof FunctionNode
        );
    }

    @Test
    public void parseFunctionBodyWithFor() {
        Assert.assertTrue(
            this.parse(
                "foo() for var in a; do echo $var; done"
            ).getPipelines().get(0).getCommands().get(0)
                instanceof FunctionNode
        );
    }

    @Test
    public void parseFunctionBodyWithWhile() {
        Assert.assertTrue(
            this.parse(
                "foo() while false; do echo hi; done"
            ).getPipelines().get(0).getCommands().get(0)
                instanceof FunctionNode
        );
    }

    @Test
    public void parseFunctionBodyWithCase() {
        Assert.assertTrue(
            this.parse(
                "foo() case foo in pat) echo hi;; esac"
            ).getPipelines().get(0).getCommands().get(0)
                instanceof FunctionNode
        );
    }

    @Test
    public void parseFunctionBodyException() {
        try {
            this.parse("foo() bar");
            Assert.fail("Expected ParseException");
        } catch (ParseException e) {
            Assert.assertEquals(ParseException.class, e.getClass());
        }
    }

    @Test
    public void parseEmptyWhileCondition() {
        Assert.assertEquals(1, this.parse("while do; echo hi; done").getPipelines().size());
    }

    @Test
    public void parseAssignmentWithoutCommand() {
        final ListNode list = this.parse("FOO=bar");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals("", cmd.getName());
        Assert.assertEquals(1, cmd.getAssignments().size());
        Assert.assertEquals("FOO=bar", cmd.getAssignments().get(0));
    }

    @Test
    public void parseMultipleAssignmentsThenCommand() {
        final ListNode list = this.parse("FOO=1 BAR=2 cmd arg");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals("cmd", cmd.getName());
        Assert.assertEquals(2, cmd.getAssignments().size());
        Assert.assertEquals("arg", cmd.getArguments().get(0));
    }

    @Test
    public void parseInlineAssignmentMidCommand() {
        final ListNode list = this.parse("cmd FOO=1 arg");
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals("cmd", cmd.getName());
        Assert.assertEquals(1, cmd.getAssignments().size());
        Assert.assertEquals("FOO=1", cmd.getAssignments().get(0));
    }

    @Test
    public void parseElifChainNestedElif() {
        final ListNode list = this.parse(
            "if false; then a; elif false; then b; elif true; then c; fi"
        );
        Assert.assertEquals(1, list.getPipelines().size());
        Assert.assertTrue(list.getPipelines().get(0).getCommands().get(0) instanceof IfNode);
    }

    @Test
    public void parseElifChainWithElse() {
        Assert.assertEquals(
            1,
            this.parse("if false; then a; elif false; then b; else c; fi").getPipelines().size()
        );
    }

    @Test
    public void parseTopLevelTrailingSemicolon() {
        Assert.assertEquals(1, this.parse("echo a;").getPipelines().size());
    }

    @Test
    public void parseTopLevelTrailingAmpersand() {
        Assert.assertEquals(1, this.parse("sleep 0 &").getPipelines().size());
    }

    @Test
    public void parseSubshellWithTrailingSemi() {
        Assert.assertEquals(1, this.parse("(echo a;)").getPipelines().size());
    }

    @Test
    public void parseGroupingWithTrailingSemi() {
        Assert.assertEquals(1, this.parse("{ echo a; }").getPipelines().size());
    }

    @Test
    public void parseEmptyIfThenBody() {
        Assert.assertEquals(1, this.parse("if true; then fi").getPipelines().size());
    }

    @Test
    public void parseEmptyForBody() {
        Assert.assertEquals(1, this.parse("for i in 1 2; do done").getPipelines().size());
    }

    @Test
    public void parseTwoPipelinesSemiTerminatedBeforeReserved() {
        Assert.assertEquals(
            1,
            this.parse("if true; then echo a; echo b; fi").getPipelines().size()
        );
    }

    @Test
    public void parsePipelineEmptyPipeStage() {
        Assert.assertEquals(
            1,
            this.parse("if echo a | echo b; then echo c; fi").getPipelines().size()
        );
    }

    @Test
    public void parseHereDocRedirect() {
        final ListNode list = this.parse(String.format("cat <<EOF%nhello%nEOF%n"));
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    public void parseCaseMultilineBody() {
        final ListNode list = this.parse(
            String.format("case x in%n  a) echo aa;;%n  b) echo bb;;%nesac")
        );
        Assert.assertEquals(1, list.getPipelines().size());
        Assert.assertEquals(
            2,
            ((CaseNode) list.getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    public void parseCaseMultilineWithBlankLinesBetweenArms() {
        final ListNode list = this.parse(
            String.format("case y in%n%n  a) echo aa;;%n%n  b) echo bb;;%n%nesac")
        );
        Assert.assertEquals(1, list.getPipelines().size());
        Assert.assertEquals(
            2,
            ((CaseNode) list.getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    public void parseHereDocStripTabs() {
        final ListNode list = this.parse(String.format("cat <<-EOF%n\thello%nEOF%n"));
        final SimpleCommand cmd = (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
        Assert.assertEquals(1, cmd.getRedirections().size());
        Assert.assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    public void parseSeparatorsInList() {
        final ListNode list = this.parse("cmd1 & cmd2 && cmd3 || cmd4");
        Assert.assertEquals(4, list.getPipelines().size());
        Assert.assertEquals(ListNode.Separator.AMP, list.getSeparators().get(0));
        Assert.assertEquals(ListNode.Separator.AND_IF, list.getSeparators().get(1));
        Assert.assertEquals(ListNode.Separator.OR_IF, list.getSeparators().get(2));
    }

    @Test
    public void parseWhileWithAmpInCondition() {
        Assert.assertEquals(
            1,
            this.parse("while true & false; do echo hi; done").getPipelines().size()
        );
    }

    @Test
    public void parseWhileWithAndIfInCondition() {
        Assert.assertEquals(
            1,
            this.parse("while true && false; do echo hi; done").getPipelines().size()
        );
    }

    @Test
    public void parseWhileWithOrIfInCondition() {
        Assert.assertEquals(
            1,
            this.parse("while true || false; do echo hi; done").getPipelines().size()
        );
    }

    @Test
    public void redirectionInputConstructor() {
        final Redirection result = new Redirection(0, RedirectionType.INPUT, "/dev/null");
        Assert.assertEquals(0, result.getFd());
        Assert.assertEquals(RedirectionType.INPUT, result.getType());
        Assert.assertEquals("/dev/null", result.getTarget());
    }

    @Test
    public void redirectionOutputConstructor() {
        final Redirection result = new Redirection(1, RedirectionType.OUTPUT, "/tmp/out");
        Assert.assertEquals(1, result.getFd());
        Assert.assertEquals(RedirectionType.OUTPUT, result.getType());
        Assert.assertEquals("/tmp/out", result.getTarget());
    }

    @Test
    public void redirectionAppendConstructor() {
        final Redirection result = new Redirection(2, RedirectionType.APPEND, "/tmp/log");
        Assert.assertEquals(2, result.getFd());
        Assert.assertEquals(RedirectionType.APPEND, result.getType());
        Assert.assertEquals("/tmp/log", result.getTarget());
    }

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }
}
