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
 * Tests for {@link Parser}: parse assignment.
 *
 * @since 1.0
 */
final class ParserParseAssignmentTest {

    @Test
    void parseAssignmentWordAfterCommandNameThird() {
        Assertions.assertTrue(this.command(this.parse("echo FOO=bar")).getAssignments().isEmpty());
    }

    @Test
    void parseCaseSimpleFirst() {
        Assertions.assertTrue(
            this.node("case foo in pat) echo match;; esac", 0) instanceof CaseNode
        );
    }

    @Test
    void parseCaseSimpleSecond() {
        Assertions.assertEquals(
            "foo",
            ((CaseNode) this.node("case foo in pat) echo match;; esac", 0)).getWord()
        );
    }

    @Test
    void parseCaseSimpleThird() {
        Assertions.assertEquals(
            1,
            ((CaseNode) this.node("case foo in pat) echo match;; esac", 0)).getItems().size()
        );
    }

    @Test
    void parseCaseSimpleFourth() {
        Assertions.assertEquals(
            "pat",
            ((CaseNode) this.node("case foo in pat) echo match;; esac", 0)).getItems().get(
                0
            ).getPatterns().get(0)
        );
    }

    @Test
    void parseCasePipePatternFirst() {
        Assertions.assertEquals(
            2,
            this.caseOf("case foo in a|b) echo match;; esac").getItems().get(0).getPatterns().size()
        );
    }

    @Test
    void parseCasePipePatternSecond() {
        Assertions.assertEquals(
            "a",
            this.caseOf("case foo in a|b) echo match;; esac").getItems().get(0).getPatterns().get(
                0
            )
        );
    }

    @Test
    void parseCasePipePatternThird() {
        Assertions.assertEquals(
            "b",
            this.caseOf("case foo in a|b) echo match;; esac").getItems().get(0).getPatterns().get(
                1
            )
        );
    }

    @Test
    void parseCaseMultipleItems() {
        Assertions.assertEquals(
            2,
            this.caseOf("case foo in a) echo a;; b) echo b;; esac").getItems().size()
        );
    }

    @Test
    void parseCaseEmptyBody() {
        Assertions.assertEquals(1, this.caseOf("case foo in a);; esac").getItems().size());
    }

    @Test
    void parseCloseInputRedirectFirst() {
        Assertions.assertEquals(1, this.command(this.parse("cmd <&-")).getRedirections().size());
    }

    @Test
    void parseCloseInputRedirectSecond() {
        Assertions.assertEquals(
            RedirectionType.DUP_INPUT,
            this.command(this.parse("cmd <&-")).getRedirections().get(0).getType()
        );
    }

    @Test
    void parseCloseInputRedirectThird() {
        Assertions.assertEquals(
            "-",
            this.command(this.parse("cmd <&-")).getRedirections().get(0).getTarget()
        );
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
        Assertions.assertEquals(
            RedirectionType.DUP_INPUT,
            this.command(this.parse("cmd <&3")).getRedirections().get(0).getType()
        );
    }

    @Test
    void parseDupInputRedirectFourth() {
        Assertions.assertEquals(
            "3",
            this.command(this.parse("cmd <&3")).getRedirections().get(0).getTarget()
        );
    }

    @Test
    void parseDupOutputRedirectFirst() {
        Assertions.assertEquals(1, this.command(this.parse("cmd >&2")).getRedirections().size());
    }

    @Test
    void parseDupOutputRedirectSecond() {
        Assertions.assertEquals(
            RedirectionType.DUP_OUTPUT,
            this.command(this.parse("cmd >&2")).getRedirections().get(0).getType()
        );
    }

    @Test
    void parseDupOutputRedirectThird() {
        Assertions.assertEquals(
            "2",
            this.command(this.parse("cmd >&2")).getRedirections().get(0).getTarget()
        );
    }

    @Test
    void parseForDoDoneFirst() {
        Assertions.assertEquals(
            1,
            this.pipelineOf("for var in a b c; do echo $var; done").getCommands().size()
        );
    }

    @Test
    void parseForDoDoneSecond() {
        Assertions.assertTrue(
            this.node("for var in a b c; do echo $var; done", 0) instanceof ForNode
        );
    }

    @Test
    void parseForDoDoneThird() {
        Assertions.assertEquals(
            "var",
            ((ForNode) this.node("for var in a b c; do echo $var; done", 0)).getVariable()
        );
    }

    @Test
    void parseForDoDoneFourth() {
        Assertions.assertEquals(
            3,
            ((ForNode) this.node("for var in a b c; do echo $var; done", 0)).getWords().size()
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
}
