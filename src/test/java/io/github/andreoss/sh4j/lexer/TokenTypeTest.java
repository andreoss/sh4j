/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for TokenType.
 * @since 1.0
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
final class TokenTypeTest {

    @Test
    void allEnumValuesPresent() {
        Assertions.assertTrue(TokenType.values().length > 0);
    }

    @Test
    void valueOfAllTypes() {
        Assertions.assertEquals(TokenType.WORD, TokenType.valueOf("WORD"));
        Assertions.assertEquals(TokenType.ASSIGNMENT_WORD, TokenType.valueOf("ASSIGNMENT_WORD"));
        Assertions.assertEquals(TokenType.NAME, TokenType.valueOf("NAME"));
        Assertions.assertEquals(TokenType.NEWLINE, TokenType.valueOf("NEWLINE"));
        Assertions.assertEquals(TokenType.IO_NUMBER, TokenType.valueOf("IO_NUMBER"));
        Assertions.assertEquals(TokenType.AND_IF, TokenType.valueOf("AND_IF"));
        Assertions.assertEquals(TokenType.OR_IF, TokenType.valueOf("OR_IF"));
        Assertions.assertEquals(TokenType.DSEMI, TokenType.valueOf("DSEMI"));
        Assertions.assertEquals(TokenType.DLESS, TokenType.valueOf("DLESS"));
        Assertions.assertEquals(TokenType.DGREATER, TokenType.valueOf("DGREATER"));
        Assertions.assertEquals(TokenType.LESSAND, TokenType.valueOf("LESSAND"));
        Assertions.assertEquals(TokenType.GREATAND, TokenType.valueOf("GREATAND"));
        Assertions.assertEquals(TokenType.LESSGREATER, TokenType.valueOf("LESSGREATER"));
        Assertions.assertEquals(TokenType.DLESSDASH, TokenType.valueOf("DLESSDASH"));
        Assertions.assertEquals(TokenType.CLOBBER, TokenType.valueOf("CLOBBER"));
        Assertions.assertEquals(TokenType.PIPE, TokenType.valueOf("PIPE"));
        Assertions.assertEquals(TokenType.AMP, TokenType.valueOf("AMP"));
        Assertions.assertEquals(TokenType.SEMI, TokenType.valueOf("SEMI"));
        Assertions.assertEquals(TokenType.LESS, TokenType.valueOf("LESS"));
        Assertions.assertEquals(TokenType.GREAT, TokenType.valueOf("GREAT"));
        Assertions.assertEquals(TokenType.LPAREN, TokenType.valueOf("LPAREN"));
        Assertions.assertEquals(TokenType.RPAREN, TokenType.valueOf("RPAREN"));
        Assertions.assertEquals(TokenType.END_OF_FILE, TokenType.valueOf("END_OF_FILE"));
    }
}
