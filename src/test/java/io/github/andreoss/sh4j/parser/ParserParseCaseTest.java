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
 * Tests for {@link Parser}: parse case.
 *
 * @since 1.0
 */
final class ParserParseCaseTest {

    private Parser parser(final String input) {
        return new Parser(new Lexer(new CharReader(input)));
    }

    private ListNode parse(final String input) {
        return this.parser(input).parse();
    }

    private SimpleCommand command(final ListNode list) {
        return (SimpleCommand) list.getPipelines().get(0).getCommands().get(0);
    }

    private CaseNode caseOf(final String source) {
        return (CaseNode) this.node(source, 0);
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

    @Test
    void parseCaseParenPattern() {
        Assertions.assertEquals(
            "pat",
            this.caseOf("case foo in (pat) echo match;; esac").getItems().get(0).getPatterns().get(
                0
            )
        );
    }

    @Test
    void parseFunctionDefinitionFirst() {
        Assertions.assertTrue(this.node("foo() { echo bar; }", 0) instanceof FunctionNode);
    }

    @Test
    void parseFunctionDefinitionSecond() {
        Assertions.assertEquals(
            "foo",
            ((FunctionNode) this.node("foo() { echo bar; }", 0)).getName()
        );
    }

    @Test
    void parseFunctionSubshellBodyFirst() {
        Assertions.assertEquals("foo", ((FunctionNode) this.node("foo() (echo bar)", 0)).getName());
    }

    @Test
    void parseFunctionSubshellBodySecond() {
        Assertions.assertTrue(
            ((FunctionNode) this.node("foo() (echo bar)", 0)).getBody() instanceof Subshell
        );
    }

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
    void parseForInPipelineFirst() {
        Assertions.assertEquals(
            2,
            this.pipelineOf("true | for var in a; do echo $var; done").getCommands().size()
        );
    }

    @Test
    void parseForInPipelineSecond() {
        Assertions.assertTrue(
            this.node("true | for var in a; do echo $var; done", 1) instanceof ForNode
        );
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

    @Test
    void parseCaseInPipelineFirst() {
        Assertions.assertEquals(
            2,
            this.pipelineOf("true | case foo in pat) echo hi;; esac").getCommands().size()
        );
    }

    @Test
    void parseCaseInPipelineSecond() {
        Assertions.assertTrue(
            this.node("true | case foo in pat) echo hi;; esac", 1) instanceof CaseNode
        );
    }

    @Test
    void parseFunctionInPipelineFirst() {
        Assertions.assertEquals(
            2,
            this.pipelineOf("true | foo() { echo bar; }").getCommands().size()
        );
    }

    @Test
    void parseFunctionInPipelineSecond() {
        Assertions.assertTrue(this.node("true | foo() { echo bar; }", 1) instanceof FunctionNode);
    }

    @Test
    void parseExpectWordThrows() {
        Assertions.assertThrowsExactly(
            ParseException.class,
            () -> this.parse("if true; then echo hi")
        );
    }

    @Test
    void parseFunctionBodyWithIf() {
        Assertions.assertTrue(
            this.node("foo() if true; then echo hi; fi", 0) instanceof FunctionNode
        );
    }

    @Test
    void parseFunctionBodyWithFor() {
        Assertions.assertTrue(
            this.node("foo() for var in a; do echo $var; done" , 0) instanceof FunctionNode
        );
    }

    @Test
    void parseFunctionBodyWithWhile() {
        Assertions.assertTrue(
            this.node("foo() while false; do echo hi; done" , 0) instanceof FunctionNode
        );
    }

    @Test
    void parseFunctionBodyWithCase() {
        Assertions.assertTrue(
            this.parse("foo() case foo in pat) echo hi;; esac").getPipelines().get(
                0
            ).getCommands().get(0) instanceof FunctionNode
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
        Assertions.assertEquals(
            "FOO=bar",
            this.command(this.parse("FOO=bar")).getAssignments().get(0)
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
    void parseElifChainNestedElifFirst() {
        Assertions.assertEquals(
            1,
            this.parse(
                "if false; then a; elif false; then b; elif true; then c; fi"
            ).getPipelines().size()
        );
    }

    @Test
    void parseElifChainNestedElifSecond() {
        Assertions.assertTrue(
            this.node(
                "if false; then a; elif false; then b; elif true; then c; fi",
                0
            ) instanceof IfNode
        );
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
}
