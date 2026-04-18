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
 * Tests for {@link Parser}: parse for do.
 *
 * @since 1.0
 */
final class ParserParseForDoTest {

    @Test
    void parseForDoDoneFifth() {
        Assertions.assertEquals(
            "a",
            ((ForNode) this.node("for var in a b c; do echo $var; done", 0)).getWords().get(0)
        );
    }

    @Test
    void parseForDoDoneSixth() {
        Assertions.assertEquals(
            "c",
            ((ForNode) this.node("for var in a b c; do echo $var; done", 0)).getWords().get(2)
        );
    }

    @Test
    void parseIfThenFiFirst() {
        Assertions.assertEquals(1, this.parse("if true; then echo hi; fi").getPipelines().size());
    }

    @Test
    void parseIfThenFiSecond() {
        Assertions.assertEquals(
            1,
            this.parse("if true; then echo hi; fi").getPipelines().get(0).getCommands().size()
        );
    }

    @Test
    void parseIfThenFiThird() {
        Assertions.assertTrue(this.node("if true; then echo hi; fi", 0) instanceof IfNode);
    }

    @Test
    void parseIfThenElseFiFirst() {
        Assertions.assertTrue(
            this.node("if false; then echo a; else echo b; fi", 0) instanceof IfNode
        );
    }

    @Test
    void parseIfThenElseFiSecond() {
        Assertions.assertNotNull(
            ((IfNode) this.node("if false; then echo a; else echo b; fi", 0)).getElseBody()
        );
    }

    @Test
    void parseIoNumberRedirectFirst() {
        Assertions.assertEquals(1, this.command(this.parse("2>file")).getRedirections().size());
    }

    @Test
    void parseIoNumberRedirectSecond() {
        Assertions.assertEquals(
            "file",
            this.command(this.parse("2>file")).getRedirections().get(0).getTarget()
        );
    }

    @Test
    void parseReadWriteRedirectFirst() {
        Assertions.assertEquals(1, this.command(this.parse("cmd <>file")).getRedirections().size());
    }

    @Test
    void parseReadWriteRedirectSecond() {
        Assertions.assertEquals(
            RedirectionType.READWRITE,
            this.command(this.parse("cmd <>file")).getRedirections().get(0).getType()
        );
    }

    @Test
    void parseReadWriteRedirectThird() {
        Assertions.assertEquals(
            "file",
            this.command(this.parse("cmd <>file")).getRedirections().get(0).getTarget()
        );
    }

    @Test
    void parseUntilDoDoneFirst() {
        Assertions.assertTrue(this.node("until false; do echo hi; done", 0) instanceof WhileNode);
    }

    @Test
    void parseUntilDoDoneSecond() {
        Assertions.assertTrue(
            ((WhileNode) this.node("until false; do echo hi; done", 0)).isUntil()
        );
    }

    @Test
    void parseWhileDoDoneFirst() {
        Assertions.assertTrue(this.node("while true; do echo hi; done", 0) instanceof WhileNode);
    }

    @Test
    void parseWhileDoDoneSecond() {
        Assertions.assertFalse(
            ((WhileNode) this.node("while true; do echo hi; done", 0)).isUntil()
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
