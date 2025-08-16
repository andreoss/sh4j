/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for Token.
 * @since 1.0
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
public final class TokenTest {

    @Test
    public void createsTokenWithTypeTextAndPosition() {
        final Token tokn = new Token(TokenType.WORD, "echo", 1, 0);
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("echo", tokn.text());
        Assert.assertEquals(1, tokn.line());
        Assert.assertEquals(0, tokn.column());
    }

    @Test
    public void createsTokenFromCharReader() {
        final Token tokn = Token.of(TokenType.WORD, "abc", new CharReader("abc"));
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("abc", tokn.text());
        Assert.assertEquals(1, tokn.line());
        Assert.assertEquals(0, tokn.column());
    }

    @Test
    public void tokenEquality() {
        Assert.assertEquals(
            new Token(TokenType.WORD, "x", 1, 0),
            new Token(TokenType.WORD, "x", 1, 0)
        );
    }

    @Test
    public void tokenToString() {
        Assert.assertTrue(
            new Token(TokenType.NEWLINE, String.format("%n"), 1, 0).toString().contains("NEWLINE")
        );
    }
}
