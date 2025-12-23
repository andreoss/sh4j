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
    void createsTokenWithTypeTextAndPosition() {
        final Token tokn = new Token(TokenType.WORD, "echo", new Position(1, 0));
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("echo"));
        MatcherAssert.assertThat(tokn.line(), CoreMatchers.equalTo(1));
        MatcherAssert.assertThat(tokn.column(), CoreMatchers.equalTo(0));
    }

    @Test
    void createsTokenFromCharReader() {
        final Token tokn = Token.ofReader(TokenType.WORD, "abc", new CharReader("abc"));
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("abc"));
        MatcherAssert.assertThat(tokn.line(), CoreMatchers.equalTo(1));
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
    void tokenEqualityNegativeCases() {
        final Token base = new Token(TokenType.WORD, "x", new Position(1, 0));
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(CoreMatchers.equalTo((Object) null))
        );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(CoreMatchers.equalTo("x"))
        );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0))
                )
            )
        );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.NEWLINE, "x", new Position(1, 0))
                )
            )
        );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "x", new Position(2, 0))
                )
            )
        );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "x", new Position(1, 1))
                )
            )
        );
        MatcherAssert.assertThat(
            base,
            CoreMatchers.equalTo(base)
        );
    }

    @Test
    void tokenHashCodeAndNullText() {
        final Token first = new Token(TokenType.WORD, "x", new Position(1, 0));
        final Token empty = new Token(null, null, new Position(0, 0));
        MatcherAssert.assertThat(
            first.hashCode(),
            CoreMatchers.equalTo(
                new Token(TokenType.WORD, "x", new Position(1, 0)).hashCode()
            )
        );
        MatcherAssert.assertThat(
            first.hashCode(),
            CoreMatchers.not(
                CoreMatchers.equalTo(
                    new Token(TokenType.WORD, "y", new Position(1, 0)).hashCode()
                )
            )
        );
        MatcherAssert.assertThat(
            empty,
            CoreMatchers.equalTo(
                new Token(null, null, new Position(0, 0))
            )
        );
        MatcherAssert.assertThat(
            empty,
            CoreMatchers.not(CoreMatchers.equalTo(first))
        );
        MatcherAssert.assertThat(
            empty.hashCode(),
            CoreMatchers.equalTo(0)
        );
    }

    @Test
    void tokenToString() {
        MatcherAssert.assertThat(
            new Token(TokenType.NEWLINE, String.format("%n"), new Position(1, 0)).toString(),
            CoreMatchers.containsString("NEWLINE")
        );
    }
}
