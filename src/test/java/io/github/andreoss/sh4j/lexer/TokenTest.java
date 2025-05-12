/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TokenTest {
    @Test
    void createsTokenWithTypeTextAndPosition() {
        final Token t = new Token(TokenType.WORD, "echo", 1, 0);
        assertEquals(TokenType.WORD, t.type());
        assertEquals("echo", t.text());
        assertEquals(1, t.line());
        assertEquals(0, t.column());
    }

    @Test
    void createsTokenFromCharReader() {
        final CharReader r = new CharReader("abc");
        final Token t = Token.of(TokenType.WORD, "abc", r);
        assertEquals(TokenType.WORD, t.type());
        assertEquals("abc", t.text());
        assertEquals(1, t.line());
        assertEquals(0, t.column());
    }

    @Test
    void tokenEquality() {
        final Token a = new Token(TokenType.WORD, "x", 1, 0);
        final Token b = new Token(TokenType.WORD, "x", 1, 0);
        assertEquals(a, b);
    }

    @Test
    void tokenToString() {
        final Token t = new Token(TokenType.NEWLINE, "\n", 1, 0);
        assertTrue(t.toString().contains("NEWLINE"));
    }
}
