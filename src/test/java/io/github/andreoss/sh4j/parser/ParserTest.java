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
 * Tests for Parser.
 *
 * @since 1.0
 */
final class ParserTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    void parseSimpleCommand() {
        final ListNode list = this.parse("echo hello");
        Assertions.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
        final SimpleCommand cmd = (SimpleCommand) pipeline.getCommands().get(0);
        Assertions.assertEquals(ParserTest.ECHO_CMD, cmd.getName());
        Assertions.assertEquals(1, cmd.getArguments().size());
        Assertions.assertEquals("hello", cmd.getArguments().get(0));
    }

    @Test
    void parsePipeline() {
        final ListNode list = this.parse("echo a | grep b");
        Assertions.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) pipeline.getCommands().get(0)).getName()
        );
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
        Assertions.assertEquals(
            "cmd1",
            ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName()
        );
        Assertions.assertEquals(
            "cmd2",
            ((SimpleCommand) list.getPipelines().get(1).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseListWithBlankLinesAndLeadingTrailingNewlines() {
        final ListNode list = this.parse(String.format("%n%ncmd1%n%n%ncmd2%n%n"));
        Assertions.assertEquals(2, list.getPipelines().size());
        Assertions.assertEquals(
            "cmd1",
            ((SimpleCommand) list.getPipelines().get(0).getCommands().get(0)).getName()
        );
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
    void parseCommandWithNoArguments() {
        final SimpleCommand cmd = this.command(this.parse("ls"));
        Assertions.assertEquals("ls", cmd.getName());
        Assertions.assertTrue(cmd.getArguments().isEmpty());
    }

    @Test
    void parseInputRedirection() {
        final SimpleCommand cmd = this.command(this.parse("cat < file.txt"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(0, cmd.getRedirections().get(0).getFd());
        Assertions.assertEquals(RedirectionType.INPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("file.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseOutputRedirection() {
        final SimpleCommand cmd = this.command(this.parse("echo hello > out.txt"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(1, cmd.getRedirections().get(0).getFd());
        Assertions.assertEquals(RedirectionType.OUTPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseAppendRedirection() {
        final SimpleCommand cmd = this.command(this.parse("echo hello >> out.txt"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.APPEND, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("out.txt", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseClobberRedirection() {
        final SimpleCommand cmd = this.command(this.parse("echo test >| file.txt"));
        Assertions.assertTrue(cmd.getRedirections().size() >= 1);
        Assertions.assertTrue(
            cmd.getRedirections().stream().anyMatch(r -> r.getType() == RedirectionType.CLOBBER)
        );
    }

    @Test
    void parseCommandWithDollar() {
        final SimpleCommand cmd = this.command(this.parse("echo $VAR"));
        Assertions.assertEquals(ParserTest.ECHO_CMD, cmd.getName());
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
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof Subshell);
        final Subshell sub = (Subshell) pipeline.getCommands().get(0);
        Assertions.assertEquals(1, sub.getList().getPipelines().size());
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
    void parseSubshellInPipeline() {
        final ListNode list = this.parse("echo foo | (cat) | wc");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertEquals(3, pipeline.getCommands().size());
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof SimpleCommand);
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof Subshell);
        Assertions.assertTrue(pipeline.getCommands().get(2) instanceof SimpleCommand);
    }

    @Test
    void parseGrouping() {
        final ListNode list = this.parse("{ echo hello; }");
        Assertions.assertEquals(1, list.getPipelines().size());
        final Pipeline pipeline = list.getPipelines().get(0);
        Assertions.assertEquals(1, pipeline.getCommands().size());
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof Grouping);
        final Grouping glob = (Grouping) pipeline.getCommands().get(0);
        Assertions.assertEquals(1, glob.getList().getPipelines().size());
        Assertions.assertEquals(
            ParserTest.ECHO_CMD,
            ((SimpleCommand) glob.getList().getPipelines().get(0).getCommands().get(0)).getName()
        );
    }

    @Test
    void parseGroupingInPipeline() {
        final ListNode list = this.parse("echo foo | { cat; } | wc");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertEquals(3, pipeline.getCommands().size());
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof SimpleCommand);
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof Grouping);
        Assertions.assertTrue(pipeline.getCommands().get(2) instanceof SimpleCommand);
    }

    @Test
    void parseExpectThrowsOnMissingWord() {
        try {
            this.parse("echo <");
            Assertions.fail("Expected ParseException");
        } catch (ParseException e) {
            Assertions.assertEquals(ParseException.class, e.getClass());
        }
    }

    @Test
    void parseCommandStartingWithInputRedirect() {
        final SimpleCommand cmd = this.command(this.parse("< input.txt cat"));
        Assertions.assertEquals("<", cmd.getName());
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithOutputRedirect() {
        final SimpleCommand cmd = this.command(this.parse("> out.txt echo"));
        Assertions.assertEquals(">", cmd.getName());
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithClobberRedirect() {
        final SimpleCommand cmd = this.command(this.parse(">| file.txt echo"));
        Assertions.assertEquals(">|", cmd.getName());
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseCommandStartingWithAppendRedirect() {
        final SimpleCommand cmd = this.command(this.parse(">> file.txt echo"));
        Assertions.assertEquals(">>", cmd.getName());
        Assertions.assertEquals(2, cmd.getArguments().size());
    }

    @Test
    void parseAssignmentWordAfterCommandName() {
        final SimpleCommand cmd = this.command(this.parse("echo FOO=bar"));
        Assertions.assertEquals(ParserTest.ECHO_CMD, cmd.getName());
        Assertions.assertTrue(cmd.getArguments().contains("FOO=bar"));
        Assertions.assertTrue(cmd.getAssignments().isEmpty());
    }

    @Test
    void parseIoNumberRedirect() {
        final SimpleCommand cmd = this.command(this.parse("2>file"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals("file", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupInputRedirect() {
        final SimpleCommand cmd = this.command(this.parse("cmd <&3"));
        Assertions.assertEquals("cmd", cmd.getName());
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("3", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseCloseInputRedirect() {
        final SimpleCommand cmd = this.command(this.parse("cmd <&-"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.DUP_INPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("-", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseDupOutputRedirect() {
        final SimpleCommand cmd = this.command(this.parse("cmd >&2"));
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.DUP_OUTPUT, cmd.getRedirections().get(0).getType());
        Assertions.assertEquals("2", cmd.getRedirections().get(0).getTarget());
    }

    @Test
    void parseReadWriteRedirect() {
        final SimpleCommand cmd = this.command(this.parse("cmd <>file"));
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
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof IfNode);
    }

    @Test
    void parseIfThenElseFi() {
        final ListNode list = this.parse("if false; then echo a; else echo b; fi");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof IfNode);
        Assertions.assertNotNull(((IfNode) pipeline.getCommands().get(0)).getElseBody());
    }

    @Test
    void parseForDoDone() {
        final ListNode list = this.parse("for var in a b c; do echo $var; done");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertEquals(1, pipeline.getCommands().size());
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof ForNode);
        final ForNode node = (ForNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("var", node.getVariable());
        Assertions.assertEquals(3, node.getWords().size());
        Assertions.assertEquals("a", node.getWords().get(0));
        Assertions.assertEquals("c", node.getWords().get(2));
    }

    @Test
    void parseWhileDoDone() {
        final ListNode list = this.parse("while true; do echo hi; done");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof WhileNode);
        Assertions.assertFalse(((WhileNode) pipeline.getCommands().get(0)).isUntil());
    }

    @Test
    void parseUntilDoDone() {
        final ListNode list = this.parse("until false; do echo hi; done");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof WhileNode);
        Assertions.assertTrue(((WhileNode) pipeline.getCommands().get(0)).isUntil());
    }

    @Test
    void parseCaseSimple() {
        final ListNode list = this.parse("case foo in pat) echo match;; esac");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof CaseNode);
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("foo", num.getWord());
        Assertions.assertEquals(1, num.getItems().size());
        Assertions.assertEquals("pat", num.getItems().get(0).getPatterns().get(0));
    }

    @Test
    void parseCasePipePattern() {
        final ListNode list = this.parse("case foo in a|b) echo match;; esac");
        final Pipeline pipeline = this.firstPipeline(list);
        final CaseNode num = (CaseNode) pipeline.getCommands().get(0);
        Assertions.assertEquals(2, num.getItems().get(0).getPatterns().size());
        Assertions.assertEquals("a", num.getItems().get(0).getPatterns().get(0));
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
    void parseFunctionDefinition() {
        final ListNode list = this.parse("foo() { echo bar; }");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof FunctionNode);
        Assertions.assertEquals("foo", ((FunctionNode) pipeline.getCommands().get(0)).getName());
    }

    @Test
    void parseFunctionSubshellBody() {
        final ListNode list = this.parse("foo() (echo bar)");
        final Pipeline pipeline = this.firstPipeline(list);
        final FunctionNode num = (FunctionNode) pipeline.getCommands().get(0);
        Assertions.assertEquals("foo", num.getName());
        Assertions.assertTrue(num.getBody() instanceof Subshell);
    }

    @Test
    void parseIfInPipeline() {
        final ListNode list = this.parse("true | if true; then echo hi; fi");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertTrue(pipeline.getCommands().get(0) instanceof SimpleCommand);
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof IfNode);
    }

    @Test
    void parseForInPipeline() {
        final ListNode list = this.parse("true | for var in a; do echo $var; done");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof ForNode);
    }

    @Test
    void parseWhileInPipeline() {
        final ListNode list = this.parse("true | while false; do echo hi; done");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof WhileNode);
    }

    @Test
    void parseCaseInPipeline() {
        final ListNode list = this.parse("true | case foo in pat) echo hi;; esac");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof CaseNode);
    }

    @Test
    void parseFunctionInPipeline() {
        final ListNode list = this.parse("true | foo() { echo bar; }");
        final Pipeline pipeline = this.firstPipeline(list);
        Assertions.assertEquals(2, pipeline.getCommands().size());
        Assertions.assertTrue(pipeline.getCommands().get(1) instanceof FunctionNode);
    }

    @Test
    void parseExpectWordThrows() {
        try {
            this.parse("if true; then echo hi");
            Assertions.fail("Expected ParseException");
        } catch (ParseException e) {
            Assertions.assertEquals(ParseException.class, e.getClass());
        }
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
        try {
            this.parse("foo() bar");
            Assertions.fail("Expected ParseException");
        } catch (ParseException e) {
            Assertions.assertEquals(ParseException.class, e.getClass());
        }
    }

    @Test
    void parseEmptyWhileCondition() {
        Assertions.assertEquals(1, this.parse("while do; echo hi; done").getPipelines().size());
    }

    @Test
    void parseAssignmentWithoutCommand() {
        final SimpleCommand cmd = this.command(this.parse("FOO=bar"));
        Assertions.assertEquals("", cmd.getName());
        Assertions.assertEquals(1, cmd.getAssignments().size());
        Assertions.assertEquals("FOO=bar", cmd.getAssignments().get(0));
    }

    @Test
    void parseMultipleAssignmentsThenCommand() {
        final SimpleCommand cmd = this.command(this.parse("FOO=1 BAR=2 cmd arg"));
        Assertions.assertEquals("cmd", cmd.getName());
        Assertions.assertEquals(2, cmd.getAssignments().size());
        Assertions.assertEquals("arg", cmd.getArguments().get(0));
    }

    @Test
    void parseInlineAssignmentMidCommand() {
        final SimpleCommand cmd = this.command(this.parse("cmd FOO=1 arg"));
        Assertions.assertEquals("cmd", cmd.getName());
        Assertions.assertEquals(0, cmd.getAssignments().size());
        Assertions.assertEquals(2, cmd.getArguments().size());
        Assertions.assertEquals("FOO=1", cmd.getArguments().get(0));
        Assertions.assertEquals("arg", cmd.getArguments().get(1));
    }

    @Test
    void parseElifChainNestedElif() {
        final ListNode list = this.parse(
            "if false; then a; elif false; then b; elif true; then c; fi"
        );
        Assertions.assertEquals(1, list.getPipelines().size());
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
    void parseHereDocRedirect() {
        final SimpleCommand cmd = (SimpleCommand) this.parse(String
            .format("cat <<EOF%nhello%nEOF%n")).getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(1, cmd.getRedirections().size());
        Assertions.assertEquals(RedirectionType.HERE_DOC, cmd.getRedirections().get(0).getType());
    }

    @Test
    void parseCaseMultilineBody() {
        final ListNode list = this.parse(
            String.format("case x in%n  a) echo aa;;%n  b) echo bb;;%nesac")
        );
        Assertions.assertEquals(1, list.getPipelines().size());
        Assertions.assertEquals(
            2,
            ((CaseNode) list.getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    void parseCaseMultilineWithBlankLinesBetweenArms() {
        final ListNode list = this.parse(
            String.format("case y in%n%n  a) echo aa;;%n%n  b) echo bb;;%n%nesac")
        );
        Assertions.assertEquals(1, list.getPipelines().size());
        Assertions.assertEquals(
            2,
            ((CaseNode) list.getPipelines().get(0).getCommands().get(0)).getItems().size()
        );
    }

    @Test
    void parseHereDocStripTabs() {
        final SimpleCommand cmd = (SimpleCommand) this.parse(String
            .format("cat <<-EOF%n\thello%nEOF%n")).getPipelines().get(0).getCommands().get(0);
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

    @Test
    void quotedHeredocDelimiters() {
        for (final String delim : new String[] {"'EOF'", "\"EOF\"", "\\EOF"}) {
            final ListNode list = this.parse(
                String.format("cat <<%s%nbody%nEOF%n", delim)
            );
            final SimpleCommand cmd = (SimpleCommand) list.getPipelines()
                .get(0).getCommands().get(0);
            Assertions.assertTrue(cmd.getRedirections().get(0).isHereDocQuoted());
            Assertions.assertEquals("EOF", cmd.getRedirections().get(0).getTarget());
        }
    }

    @Test
    void redirectionDescriptorBoundaries() {
        final SimpleCommand explicit = (SimpleCommand) this.parse("cmd 2>out")
            .getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(2, explicit.getRedirections().get(0).getFd());
        final SimpleCommand implicit = (SimpleCommand) this.parse("cmd <in")
            .getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(0, implicit.getRedirections().get(0).getFd());
        final SimpleCommand dup = (SimpleCommand) this.parse("cmd 2>&1")
            .getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(RedirectionType.DUP_OUTPUT, dup.getRedirections().get(0).getType());
        final SimpleCommand readwrite = (SimpleCommand) this.parse("cmd <>file")
            .getPipelines().get(0).getCommands().get(0);
        Assertions.assertEquals(
            RedirectionType.READWRITE,
            readwrite.getRedirections().get(0).getType()
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
    void pipelineSeparatorsInCompoundConditions() {
        Assertions.assertEquals(
            1,
            this.parse("if a & b; then c; fi").getPipelines().size()
        );
        Assertions.assertEquals(
            1,
            this.parse("while a && b; do c; done").getPipelines().size()
        );
        Assertions.assertEquals(
            1,
            this.parse("until a || b; do c; done").getPipelines().size()
        );
    }

    @Test
    void assignmentAndNamedCommandLineNumbers() {
        final ListNode list = this.parse(String.format("X=1%necho hi%n"));
        final SimpleCommand assignment = (SimpleCommand) list.getPipelines()
            .get(0).getCommands().get(0);
        final SimpleCommand named = (SimpleCommand) list.getPipelines()
            .get(1).getCommands().get(0);
        Assertions.assertEquals(1, assignment.getLineNumber());
        Assertions.assertEquals(2, named.getLineNumber());
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

    private Pipeline firstPipeline(final ListNode list) {
        return list.getPipelines().get(0);
    }

    private Node firstCommand(final ListNode list) {
        return list.getPipelines().get(0).getCommands().get(0);
    }
}
