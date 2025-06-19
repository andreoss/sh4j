/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for Token.
 * @since 1.0
 */
final class TokenTest {

    @Test
    void createsTokenWithTypeTextAndPosition() {
        final Token tokn = new Token(TokenType.WORD, "echo", 1, 0);
        Assertions.assertEquals(TokenType.WORD, tokn.type());
        Assertions.assertEquals("echo", tokn.text());
        Assertions.assertEquals(1, tokn.line());
        Assertions.assertEquals(0, tokn.column());
    }

    @Test
    void createsTokenFromCharReader() {
        final Token tokn = Token.of(TokenType.WORD, "abc", new CharReader("abc"));
        Assertions.assertEquals(TokenType.WORD, tokn.type());
        Assertions.assertEquals("abc", tokn.text());
        Assertions.assertEquals(1, tokn.line());
        Assertions.assertEquals(0, tokn.column());
    }

    @Test
    void tokenEquality() {
        Assertions.assertEquals(
            new Token(TokenType.WORD, "x", 1, 0),
            new Token(TokenType.WORD, "x", 1, 0)
        );
    }

    @Test
    void tokenToString() {
        Assertions.assertTrue(new Token(TokenType.NEWLINE, String.format("%n"), 1, 0).toString().contains("NEWLINE"));
    }
}
