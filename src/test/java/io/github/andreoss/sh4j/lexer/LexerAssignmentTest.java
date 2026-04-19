/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Lexer}: assignment.
 *
 * @since 1.0
 */
final class LexerAssignmentTest {

    @Test
    void assignmentWordEdgeCasesFirst() {
        MatcherAssert.assertThat(
            this.lexer("=value").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void assignmentWordEdgeCasesSecond() {
        this.lexer("=value").nextToken();
        MatcherAssert.assertThat(
            this.lexer("_=value").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
    }

    @Test
    void assignmentWordNameValidationFirst() {
        MatcherAssert.assertThat(
            this.lexer("A1=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
    }

    @Test
    void assignmentWordNameValidationSecond() {
        this.lexer("A1=x").nextToken();
        MatcherAssert.assertThat(
            this.lexer("_A=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
    }

    @Test
    void assignmentWordNameValidationThird() {
        this.lexer("A1=x").nextToken();
        this.lexer("_A=x").nextToken();
        MatcherAssert.assertThat(
            this.lexer("1A=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void assignmentWordNameValidationFourth() {
        this.lexer("A1=x").nextToken();
        this.lexer("_A=x").nextToken();
        this.lexer("1A=x").nextToken();
        MatcherAssert.assertThat(
            this.lexer("A-B=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void backslashEscapesInsideWordFirst() {
        MatcherAssert.assertThat(
            this.lexer("a\\ b").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void backslashEscapesInsideWordSecond() {
        MatcherAssert.assertThat(
            this.lexer("a\\ b").nextToken().text(),
            CoreMatchers.equalTo("a\\ b")
        );
    }

    @Test
    void backslashContinuation() {
        MatcherAssert.assertThat(
            this.lexer(String.format("a\\%nb")).nextToken().text(),
            CoreMatchers.equalTo("ab")
        );
    }

    @Test
    void backslashAtEof() {
        MatcherAssert.assertThat(this.lexer("a\\").nextToken().text(), CoreMatchers.equalTo("a\\"));
    }

    @Test
    void backtickWithEscapedBacktick() {
        MatcherAssert.assertThat(
            this.lexer("`a\\`b`").nextToken().text(),
            CoreMatchers.equalTo("`a\\`b`")
        );
    }

    @Test
    void consecutiveNewlinesFirst() {
        MatcherAssert.assertThat(
            this.lexer(String.format("%n%n")).nextToken().type(),
            CoreMatchers.equalTo(TokenType.NEWLINE)
        );
    }

    @Test
    void consecutiveNewlinesSecond() {
        final Lexer list = this.lexer(String.format("%n%n"));
        list.nextToken();
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
    }

    @Test
    void descriptorDigitsAreRecognisedFirst() {
        MatcherAssert.assertThat(
            this.lexer("2>file").nextToken().type(),
            CoreMatchers.equalTo(TokenType.IO_NUMBER)
        );
    }

    @Test
    void descriptorDigitsAreRecognisedSecond() {
        final Lexer digits = this.lexer("2>file");
        digits.nextToken();
        MatcherAssert.assertThat(digits.nextToken().type(), CoreMatchers.equalTo(TokenType.GREAT));
    }

    @Test
    void descriptorDigitsAreRecognisedThird() {
        final Lexer digits = this.lexer("2>file");
        digits.nextToken();
        digits.nextToken();
        MatcherAssert.assertThat(
            this.lexer("2a>file").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void descriptorDigitsAreRecognisedFourth() {
        final Lexer digits = this.lexer("2>file");
        digits.nextToken();
        digits.nextToken();
        final Lexer mixed = this.lexer("2a>file");
        mixed.nextToken();
        MatcherAssert.assertThat(mixed.nextToken().type(), CoreMatchers.equalTo(TokenType.GREAT));
    }

    @Test
    void digitsThenSpaceIsWordNotIoNumberFirst() {
        MatcherAssert.assertThat(
            this.lexer("42 ").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void digitsThenSpaceIsWordNotIoNumberSecond() {
        MatcherAssert.assertThat(this.lexer("42 ").nextToken().text(), CoreMatchers.equalTo("42"));
    }

    @Test
    void doubleQuotedWordFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"hello\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedWordSecond() {
        MatcherAssert.assertThat(
            this.lexer("\"hello\"").nextToken().text(),
            CoreMatchers.equalTo("\"hello\"")
        );
    }

    @Test
    void doubleQuotedBackslashDollarFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"\\$\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashDollarSecond() {
        MatcherAssert.assertThat(
            this.lexer("\"\\$\"").nextToken().text(),
            CoreMatchers.equalTo("\"\\$\"")
        );
    }

    @Test
    void doubleQuotedBackslashBacktickFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"\\`\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashBacktickSecond() {
        MatcherAssert.assertThat(
            this.lexer("\"\\`\"").nextToken().text(),
            CoreMatchers.equalTo("\"\\`\"")
        );
    }

    @Test
    void doubleQuotedBackslashNewlineFirst() {
        MatcherAssert.assertThat(
            this.lexer(String.format("\"a\\%nb\"")).nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashNewlineSecond() {
        MatcherAssert.assertThat(
            this.lexer(String.format("\"a\\%nb\"")).nextToken().text(),
            CoreMatchers.equalTo("\"ab\"")
        );
    }

    @Test
    void doubleQuotedBackslashBackslashFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\\\b\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashBackslashSecond() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\\\b\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\\\b\"")
        );
    }

    @Test
    void doubleQuotedBackslashDoublequoteFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\\"b\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashDoublequoteSecond() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\\"b\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\\"b\"")
        );
    }

    @Test
    void doubleQuotedBackslashNonSpecialKeptLiteralFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"\\a\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashNonSpecialKeptLiteralSecond() {
        MatcherAssert.assertThat(
            this.lexer("\"\\a\"").nextToken().text(),
            CoreMatchers.equalTo("\"\\a\"")
        );
    }

    @Test
    void doubleQuotedEscapesFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\$b\\`c\\\"d\\\\e\"")
        );
    }

    @Test
    void doubleQuotedEscapesSecond() {
        this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken();
        MatcherAssert.assertThat(
            this.lexer("\"a\\qb\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\qb\"")
        );
    }

    @Test
    void doubleQuotedEscapesThird() {
        this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken();
        this.lexer("\"a\\qb\"").nextToken();
        MatcherAssert.assertThat(
            this.lexer(String.format("\"a\\%nb\"")).nextToken().text(),
            CoreMatchers.equalTo("\"ab\"")
        );
    }

    @Test
    void eofOnEmptyInputFirst() {
        MatcherAssert.assertThat(
            this.lexer("").nextToken().type(),
            CoreMatchers.equalTo(TokenType.END_OF_FILE)
        );
    }

    @Test
    void eofOnEmptyInputSecond() {
        MatcherAssert.assertThat(this.lexer("").nextToken().text(), CoreMatchers.equalTo(""));
    }

    @Test
    void eofAfterTokens() {
        final Lexer list = this.lexer("a");
        list.nextToken();
        MatcherAssert.assertThat(
            list.nextToken().type(),
            CoreMatchers.equalTo(TokenType.END_OF_FILE)
        );
    }

    @Test
    void equalsAloneIsWordNotAssignmentFirst() {
        MatcherAssert.assertThat(
            this.lexer("=").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    private Lexer lexer(final String input) {
        return new Lexer(new CharReader(input));
    }
}
