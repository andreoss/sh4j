/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link Lexer}: simple word.
 *
 * @since 1.0
 */
final class LexerSimpleWordTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    void simpleWordSecond() {
        final Token tokn = this.lexer(LexerSimpleWordTest.ECHO_CMD).nextToken();
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(LexerSimpleWordTest.ECHO_CMD));
    }

    @Test
    void singleCharOperatorsFirst() {
        this.assertOperator("|", TokenType.PIPE);
    }

    @Test
    void singleCharOperatorsSecond() {
        this.assertOperator("&", TokenType.AMP);
    }

    @Test
    void singleCharOperatorsThird() {
        this.assertOperator(";", TokenType.SEMI);
    }

    @Test
    void singleCharOperatorsFourth() {
        this.assertOperator("<", TokenType.LESS);
    }

    @Test
    void singleCharOperatorsFifth() {
        this.assertOperator(">", TokenType.GREAT);
    }

    @Test
    void singleCharOperatorsSixth() {
        this.assertOperator("(", TokenType.LPAREN);
    }

    @Test
    void singleCharOperatorsSeventh() {
        this.assertOperator(")", TokenType.RPAREN);
    }

    @Test
    void singleQuotedWordFirst() {
        MatcherAssert.assertThat(
            this.lexer("'hello'").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void singleQuotedWordSecond() {
        final Token tokn = this.lexer("'hello'").nextToken();
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("'hello'"));
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuoteFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"a'b\"").nextToken().text(),
            CoreMatchers.equalTo("\"a'b\"")
        );
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuoteSecond() {
        this.lexer("\"a'b\"").nextToken();
        MatcherAssert.assertThat(
            this.lexer("'a\"b'").nextToken().text(),
            CoreMatchers.equalTo("'a\"b'")
        );
    }

    @Test
    void tabIsWhitespaceFirst() {
        MatcherAssert.assertThat(
            this.lexer("\tword").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void tabIsWhitespaceSecond() {
        final Token tokn = this.lexer("\tword").nextToken();
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("word"));
    }

    @Test
    void tokenPositionsFirst() {
        MatcherAssert.assertThat(this.lexer("  word").nextToken().line(), CoreMatchers.equalTo(1));
    }

    @Test
    void tokenPositionsSecond() {
        final Token tokn = this.lexer("  word").nextToken();
        tokn.line();
        MatcherAssert.assertThat(tokn.column(), CoreMatchers.equalTo(2));
    }

    @Test
    void whitespaceSkippedBetweenTokensFirst() {
        MatcherAssert.assertThat(this.lexer("a   b").nextToken().text(), CoreMatchers.equalTo("a"));
    }

    @Test
    void whitespaceSkippedBetweenTokensSecond() {
        final Lexer list = this.lexer("a   b");
        list.nextToken();
        MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo("b"));
    }

    @Test
    void wordWithWhitespacePrefixFirst() {
        MatcherAssert.assertThat(
            this.lexer("  ls").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void wordWithWhitespacePrefixSecond() {
        final Token tokn = this.lexer("  ls").nextToken();
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("ls"));
    }

    @Test
    void wordThenOperatorFirst() {
        MatcherAssert.assertThat(
            this.lexer("cmd&").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void wordThenOperatorSecond() {
        final Token tokfirst = this.lexer("cmd&").nextToken();
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo("cmd"));
    }

    @Test
    void wordThenOperatorThird() {
        final Lexer list = this.lexer("cmd&");
        list.nextToken();
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.AMP));
    }

    @Test
    void wordBreaksAtLeftParenFirst() {
        MatcherAssert.assertThat(
            this.lexer("echo(").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void wordBreaksAtLeftParenSecond() {
        final Token tokfirst = this.lexer("echo(").nextToken();
        MatcherAssert.assertThat(
            tokfirst.text(),
            CoreMatchers.equalTo(LexerSimpleWordTest.ECHO_CMD)
        );
    }

    @Test
    void wordBreaksAtLeftParenThird() {
        final Lexer list = this.lexer("echo(");
        list.nextToken();
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.LPAREN));
    }

    @Test
    void wordBreaksAtRightParenFirst() {
        MatcherAssert.assertThat(
            this.lexer("echo)").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void wordBreaksAtRightParenSecond() {
        final Token tokfirst = this.lexer("echo)").nextToken();
        MatcherAssert.assertThat(
            tokfirst.text(),
            CoreMatchers.equalTo(LexerSimpleWordTest.ECHO_CMD)
        );
    }

    @Test
    void wordBreaksAtRightParenThird() {
        final Lexer list = this.lexer("echo)");
        list.nextToken();
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.RPAREN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"&", "|", ";", "<", ">", "(", ")"})
    void wordBreakKeepsWordBeforeOperator(final String oper) {
        MatcherAssert.assertThat(
            this.lexer(String.format("a%s", oper)).nextToken().text(),
            CoreMatchers.equalTo("a")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"&", "|", ";", "<", ">", "(", ")"})
    void wordBreakEmitsOperator(final String oper) {
        final Lexer list = this.lexer(String.format("a%s", oper));
        list.nextToken();
        MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo(oper));
    }

    private Lexer lexer(final String input) {
        return new Lexer(new CharReader(input));
    }

    private void assertOperator(final String oper, final TokenType expected) {
        final Token tokn = this.lexer(oper).nextToken();
        MatcherAssert.assertThat(
            String.format("Operator %s", oper),
            tokn.type(),
            CoreMatchers.equalTo(expected)
        );
        MatcherAssert.assertThat(
            String.format("Operator text %s", oper),
            tokn.text(),
            CoreMatchers.equalTo(oper)
        );
    }
}
