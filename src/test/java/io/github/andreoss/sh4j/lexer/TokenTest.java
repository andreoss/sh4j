/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TokenTest {

    @Test
    void createsTokenWithTypeTextAndPosition() {
        final Token tokn = new Token(TokenType.WORD, "echo", 1, 0);
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("echo", tokn.text());
        assertEquals(1, tokn.line());
        assertEquals(0, tokn.column());
    }

    @Test
    void createsTokenFromCharReader() {
        final Token tokn = Token.of(TokenType.WORD, "abc", new CharReader("abc"));
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("abc", tokn.text());
        assertEquals(1, tokn.line());
        assertEquals(0, tokn.column());
    }

    @Test
    void tokenEquality() {
        assertEquals(new Token(TokenType.WORD, "x", 1, 0), new Token(TokenType.WORD, "x", 1, 0));
    }

    @Test
    void tokenToString() {
        assertTrue(new Token(TokenType.NEWLINE, String.format("%n"), 1, 0).toString().contains("NEWLINE"));
    }
}
