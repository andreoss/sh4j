/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        final Token t = Token.of(TokenType.WORD, "abc", new CharReader("abc"));
        assertEquals(TokenType.WORD, t.type());
        assertEquals("abc", t.text());
        assertEquals(1, t.line());
        assertEquals(0, t.column());
    }

    @Test
    void tokenEquality() {
        assertEquals(new Token(TokenType.WORD, "x", 1, 0), new Token(TokenType.WORD, "x", 1, 0));
    }

    @Test
    void tokenToString() {
        assertTrue(new Token(TokenType.NEWLINE, "\n", 1, 0).toString().contains("NEWLINE"));
    }
}
