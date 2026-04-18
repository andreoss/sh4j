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
 * Tests for {@link Parser}: parse case.
 *
 * @since 1.0
 */
final class ParserParseCaseTest {

    @Test
    void parseCaseMultilineBodyFirst() {
        Assertions.assertEquals(
            1,
            this.parse(
                String.format("case x in%n  a) echo aa;;%n  b) echo bb;;%nesac")
            ).getPipelines().size()
        );
    }

    @Test
    void parseCaseMultilineBodySecond() {
        Assertions.assertEquals(
            2,
            this.caseOf(
                String.format("case x in%n  a) echo aa;;%n  b) echo bb;;%nesac")
            ).getItems().size()
        );
    }

    @Test
    void parseCaseMultilineWithBlankLinesBetweenArmsFirst() {
        Assertions.assertEquals(
            1,
            this.parse(
                String.format("case y in%n%n  a) echo aa;;%n%n  b) echo bb;;%n%nesac")
            ).getPipelines().size()
        );
    }

    @Test
    void parseCaseMultilineWithBlankLinesBetweenArmsSecond() {
        Assertions.assertEquals(
            2,
            this.caseOf(
                String.format("case y in%n%n  a) echo aa;;%n%n  b) echo bb;;%n%nesac")
            ).getItems().size()
        );
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
    void parseGroupingWithTrailingSemi() {
        Assertions.assertEquals(1, this.parse("{ echo a; }").getPipelines().size());
    }

    @Test
    void parseHereDocRedirectFirst() {
        Assertions.assertEquals(
            1,
            this.command(
                this.parse(String.format("cat <<EOF%nhello%nEOF%n"))
            ).getRedirections().size()
        );
    }

    @Test
    void parseHereDocRedirectSecond() {
        Assertions.assertEquals(
            RedirectionType.HERE_DOC,
            this.command(
                this.parse(String.format("cat <<EOF%nhello%nEOF%n"))
            ).getRedirections().get(0).getType()
        );
    }

    @Test
    void parseHereDocStripTabsFirst() {
        Assertions.assertEquals(
            1,
            this.command(
                this.parse(String.format("cat <<-EOF%n\thello%nEOF%n"))
            ).getRedirections().size()
        );
    }

    @Test
    void parseHereDocStripTabsSecond() {
        Assertions.assertEquals(
            RedirectionType.HERE_DOC,
            this.command(
                this.parse(String.format("cat <<-EOF%n\thello%nEOF%n"))
            ).getRedirections().get(0).getType()
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
    void parseSeparatorsInListFirst() {
        Assertions.assertEquals(4, this.parse("cmd1 & cmd2 && cmd3 || cmd4").getPipelines().size());
    }

    @Test
    void parseSeparatorsInListSecond() {
        Assertions.assertEquals(
            ListNode.Separator.AMP,
            this.parse("cmd1 & cmd2 && cmd3 || cmd4").getSeparators().get(0)
        );
    }

    @Test
    void parseSeparatorsInListThird() {
        Assertions.assertEquals(
            ListNode.Separator.AND_IF,
            this.parse("cmd1 & cmd2 && cmd3 || cmd4").getSeparators().get(1)
        );
    }

    @Test
    void parseSeparatorsInListFourth() {
        Assertions.assertEquals(
            ListNode.Separator.OR_IF,
            this.parse("cmd1 & cmd2 && cmd3 || cmd4").getSeparators().get(2)
        );
    }

    @Test
    void parseTwoPipelinesSemiTerminatedBeforeReserved() {
        Assertions.assertEquals(
            1,
            this.parse("if true; then echo a; echo b; fi").getPipelines().size()
        );
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
    void pipelineSeparatorsInCompoundConditionsFirst() {
        Assertions.assertEquals(1, this.parse("if a & b; then c; fi").getPipelines().size());
    }

    @Test
    void pipelineSeparatorsInCompoundConditionsSecond() {
        this.parse("if a & b; then c; fi");
        Assertions.assertEquals(1, this.parse("while a && b; do c; done").getPipelines().size());
    }

    @Test
    void pipelineSeparatorsInCompoundConditionsThird() {
        this.parse("if a & b; then c; fi");
        this.parse("while a && b; do c; done");
        Assertions.assertEquals(1, this.parse("until a || b; do c; done").getPipelines().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"'EOF'", "\"EOF\"", "\\EOF"})
    void quotedHeredocDelimiterIsQuoted(final String delim) {
        Assertions.assertTrue(
            this.heredocCommand(delim).getRedirections().get(0).isHereDocQuoted()
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

    private SimpleCommand heredocCommand(final String delim) {
        return (SimpleCommand) this.node(
            String.format("cat <<%s%nbody%nEOF%n", delim),
            0
        );
    }
}
