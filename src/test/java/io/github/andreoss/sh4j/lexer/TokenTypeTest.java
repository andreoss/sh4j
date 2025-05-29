/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TokenTypeTest {

    @Test
    void allEnumValuesPresent() {
        assertTrue(TokenType.values().length > 0);
    }

    @Test
    void valueOfAllTypes() {
        assertEquals(TokenType.WORD, TokenType.valueOf("WORD"));
        assertEquals(TokenType.ASSIGNMENT_WORD, TokenType.valueOf("ASSIGNMENT_WORD"));
        assertEquals(TokenType.NAME, TokenType.valueOf("NAME"));
        assertEquals(TokenType.NEWLINE, TokenType.valueOf("NEWLINE"));
        assertEquals(TokenType.IO_NUMBER, TokenType.valueOf("IO_NUMBER"));
        assertEquals(TokenType.AND_IF, TokenType.valueOf("AND_IF"));
        assertEquals(TokenType.OR_IF, TokenType.valueOf("OR_IF"));
        assertEquals(TokenType.DSEMI, TokenType.valueOf("DSEMI"));
        assertEquals(TokenType.DLESS, TokenType.valueOf("DLESS"));
        assertEquals(TokenType.DGREATER, TokenType.valueOf("DGREATER"));
        assertEquals(TokenType.LESSAND, TokenType.valueOf("LESSAND"));
        assertEquals(TokenType.GREATAND, TokenType.valueOf("GREATAND"));
        assertEquals(TokenType.LESSGREATER, TokenType.valueOf("LESSGREATER"));
        assertEquals(TokenType.DLESSDASH, TokenType.valueOf("DLESSDASH"));
        assertEquals(TokenType.CLOBBER, TokenType.valueOf("CLOBBER"));
        assertEquals(TokenType.PIPE, TokenType.valueOf("PIPE"));
        assertEquals(TokenType.AMP, TokenType.valueOf("AMP"));
        assertEquals(TokenType.SEMI, TokenType.valueOf("SEMI"));
        assertEquals(TokenType.LESS, TokenType.valueOf("LESS"));
        assertEquals(TokenType.GREAT, TokenType.valueOf("GREAT"));
        assertEquals(TokenType.LPAREN, TokenType.valueOf("LPAREN"));
        assertEquals(TokenType.RPAREN, TokenType.valueOf("RPAREN"));
        assertEquals(TokenType.END_OF_FILE, TokenType.valueOf("END_OF_FILE"));
    }
}
