/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for Token.
 * @since 1.0
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
public final class TokenTest {

    @Test
    public void createsTokenWithTypeTextAndPosition() {
        final Token tokn = new Token(TokenType.WORD, "echo", 1, 0);
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("echo"));
        assertThat(tokn.line(), equalTo(1));
        assertThat(tokn.column(), equalTo(0));
    }

    @Test
    public void createsTokenFromCharReader() {
        final Token tokn = Token.of(TokenType.WORD, "abc", new CharReader("abc"));
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("abc"));
        assertThat(tokn.line(), equalTo(1));
        assertThat(tokn.column(), equalTo(0));
    }

    @Test
    public void tokenEquality() {
        assertThat(
            new Token(TokenType.WORD, "x", 1, 0),
            equalTo(new Token(TokenType.WORD, "x", 1, 0))
        );
    }

    @Test
    public void tokenEqualityNegativeCases() {
        final Token base = new Token(TokenType.WORD, "x", 1, 0);
        assertThat(base, CoreMatchers.not(equalTo((Object)null)));
        assertThat(base, CoreMatchers.not(equalTo("x")));
        assertThat(
            base,
            CoreMatchers.not(equalTo(new Token(TokenType.WORD, "y", 1, 0)))
        );
        assertThat(
            base,
            CoreMatchers.not(equalTo(new Token(TokenType.NEWLINE, "x", 1, 0)))
        );
        assertThat(
            base,
            CoreMatchers.not(equalTo(new Token(TokenType.WORD, "x", 2, 0)))
        );
        assertThat(
            base,
            CoreMatchers.not(equalTo(new Token(TokenType.WORD, "x", 1, 1)))
        );
        assertThat(base, equalTo(base));
    }

    @Test
    public void tokenHashCodeAndNullText() {
        final Token first = new Token(TokenType.WORD, "x", 1, 0);
        assertThat(first.hashCode(), equalTo(new Token(TokenType.WORD, "x", 1, 0).hashCode()));
        assertThat(first.hashCode(), CoreMatchers.not(equalTo(new Token(TokenType.WORD, "y", 1, 0).hashCode())));
        final Token empty = new Token(null, null, 0, 0);
        assertThat(empty, equalTo(new Token(null, null, 0, 0)));
        assertThat(empty, CoreMatchers.not(equalTo(first)));
        assertThat(empty.hashCode(), equalTo(0));
    }

    @Test
    public void tokenToString() {
        assertThat(
            new Token(TokenType.NEWLINE, String.format("%n"), 1, 0).toString(),
            CoreMatchers.containsString("NEWLINE")
        );
    }
}
