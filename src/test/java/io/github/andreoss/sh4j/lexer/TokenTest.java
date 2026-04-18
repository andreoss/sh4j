/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for Token.
 *
 * @since 1.0
 */
final class TokenTest {

    @Test
    void createsTokenWithTypeTextAndPositionFirst() {
        MatcherAssert.assertThat(
            new Token(TokenType.WORD, "echo", new Position(1, 0)).type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void createsTokenWithTypeTextAndPositionSecond() {
        final Token tokn = new Token(TokenType.WORD, "echo", new Position(1, 0));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("echo"));
    }

    @Test
    void createsTokenWithTypeTextAndPositionThird() {
        final Token tokn = new Token(TokenType.WORD, "echo", new Position(1, 0));
        MatcherAssert.assertThat(tokn.line(), CoreMatchers.equalTo(1));
    }

    @Test
    void createsTokenWithTypeTextAndPositionFourth() {
        final Token tokn = new Token(TokenType.WORD, "echo", new Position(1, 0));
        tokn.line();
        MatcherAssert.assertThat(tokn.column(), CoreMatchers.equalTo(0));
    }

    @Test
    void createsTokenFromCharReaderFirst() {
        MatcherAssert.assertThat(
            Token.ofReader(TokenType.WORD, "abc", new CharReader("abc")).type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void createsTokenFromCharReaderSecond() {
        final Token tokn = Token.ofReader(TokenType.WORD, "abc", new CharReader("abc"));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("abc"));
    }

    @Test
    void createsTokenFromCharReaderThird() {
        final Token tokn = Token.ofReader(TokenType.WORD, "abc", new CharReader("abc"));
        MatcherAssert.assertThat(tokn.line(), CoreMatchers.equalTo(1));
    }

    @Test
    void createsTokenFromCharReaderFourth() {
        final Token tokn = Token.ofReader(TokenType.WORD, "abc", new CharReader("abc"));
        tokn.line();
        MatcherAssert.assertThat(tokn.column(), CoreMatchers.equalTo(0));
    }

    @Test
    void tokenEquality() {
        MatcherAssert.assertThat(
            new Token(TokenType.WORD, "x", new Position(1, 0)),
            CoreMatchers.equalTo(new Token(TokenType.WORD, "x", new Position(1, 0)))
        );
    }

    @Test
    void tokenEqualityNegativeCasesFirst() {
        MatcherAssert.assertThat(
            new Token(TokenType.WORD, "x", new Position(1, 0)),
            CoreMatchers.not(CoreMatchers.equalTo((Object) null))
        );
    }

    @Test
    void tokenEqualityNegativeCasesSecond() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        MatcherAssert.assertThat(base, CoreMatchers.not(CoreMatchers.equalTo("x")));
    }

    @Test
    void tokenEqualityNegativeCasesThird() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(new Token(TokenType.WORD, "y", new Position(1, 0)))
            )
        );
    }

    @Test
    void tokenEqualityNegativeCasesFourth() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(new Token(TokenType.NEWLINE, "x", new Position(1, 0)))
            )
        );
    }

    @Test
    void tokenEqualityNegativeCasesFifth() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(new Token(TokenType.WORD, "x", new Position(2, 0)))
            )
        );
    }

    @Test
    void tokenEqualityNegativeCasesSixth() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(new Token(TokenType.WORD, "x", new Position(1, 1)))
            )
        );
    }

    @Test
    void tokenEqualityNegativeCasesSeventh() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        MatcherAssert.assertThat(base, CoreMatchers.equalTo(base));
    }

    @Test
    void tokenHashCodeAndNullTextFirst() {
        final Token first = new Token(TokenType.WORD, "x", new Position(1, 0));
        new Token(null, null, new Position(0, 0));
        MatcherAssert.assertThat(
            first.hashCode(),
            CoreMatchers.equalTo(new Token(TokenType.WORD, "x", new Position(1, 0)).hashCode())
        );
    }

    @Test
    void tokenHashCodeAndNullTextSecond() {
        final Token first = new Token(TokenType.WORD, "x", new Position(1, 0));
        new Token(null, null, new Position(0, 0));
        MatcherAssert.assertThat(
            first.hashCode(),
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0)).hashCode()
                )
            )
        );
    }

    @Test
    void tokenHashCodeAndNullTextThird() {
        new Token(TokenType.WORD, "x", new Position(1, 0));
        final Token empty = new Token(null, null, new Position(0, 0));
        MatcherAssert.assertThat(
            empty,
            CoreMatchers.equalTo(new Token(null, null, new Position(0, 0)))
        );
    }

    @Test
    void tokenHashCodeAndNullTextFourth() {
        final Token first = new Token(TokenType.WORD, "x", new Position(1, 0));
        final Token empty = new Token(null, null, new Position(0, 0));
        MatcherAssert.assertThat(empty, CoreMatchers.not(CoreMatchers.equalTo(first)));
    }

    @Test
    void tokenHashCodeAndNullTextFifth() {
        final Token empty = new Token(null, null, new Position(0, 0));
        MatcherAssert.assertThat(empty.hashCode(), CoreMatchers.equalTo(0));
    }

    @Test
    void tokenToString() {
        MatcherAssert.assertThat(
            new Token(TokenType.NEWLINE, String.format("%n"), new Position(1, 0)).toString(),
            CoreMatchers.containsString("NEWLINE")
        );
    }
}
