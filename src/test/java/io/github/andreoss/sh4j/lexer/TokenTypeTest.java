/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for TokenType.
 * @since 1.0
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
public final class TokenTypeTest {

    @Test
    public void allEnumValuesPresent() {
        Assert.assertTrue(TokenType.values().length > 0);
    }

    @Test
    public void valueOfAllTypes() {
        Assert.assertEquals(TokenType.WORD, TokenType.valueOf("WORD"));
        Assert.assertEquals(TokenType.ASSIGNMENT_WORD, TokenType.valueOf("ASSIGNMENT_WORD"));
        Assert.assertEquals(TokenType.NAME, TokenType.valueOf("NAME"));
        Assert.assertEquals(TokenType.NEWLINE, TokenType.valueOf("NEWLINE"));
        Assert.assertEquals(TokenType.IO_NUMBER, TokenType.valueOf("IO_NUMBER"));
        Assert.assertEquals(TokenType.AND_IF, TokenType.valueOf("AND_IF"));
        Assert.assertEquals(TokenType.OR_IF, TokenType.valueOf("OR_IF"));
        Assert.assertEquals(TokenType.DSEMI, TokenType.valueOf("DSEMI"));
        Assert.assertEquals(TokenType.DLESS, TokenType.valueOf("DLESS"));
        Assert.assertEquals(TokenType.DGREATER, TokenType.valueOf("DGREATER"));
        Assert.assertEquals(TokenType.LESSAND, TokenType.valueOf("LESSAND"));
        Assert.assertEquals(TokenType.GREATAND, TokenType.valueOf("GREATAND"));
        Assert.assertEquals(TokenType.LESSGREATER, TokenType.valueOf("LESSGREATER"));
        Assert.assertEquals(TokenType.DLESSDASH, TokenType.valueOf("DLESSDASH"));
        Assert.assertEquals(TokenType.CLOBBER, TokenType.valueOf("CLOBBER"));
        Assert.assertEquals(TokenType.PIPE, TokenType.valueOf("PIPE"));
        Assert.assertEquals(TokenType.AMP, TokenType.valueOf("AMP"));
        Assert.assertEquals(TokenType.SEMI, TokenType.valueOf("SEMI"));
        Assert.assertEquals(TokenType.LESS, TokenType.valueOf("LESS"));
        Assert.assertEquals(TokenType.GREAT, TokenType.valueOf("GREAT"));
        Assert.assertEquals(TokenType.LPAREN, TokenType.valueOf("LPAREN"));
        Assert.assertEquals(TokenType.RPAREN, TokenType.valueOf("RPAREN"));
        Assert.assertEquals(TokenType.END_OF_FILE, TokenType.valueOf("END_OF_FILE"));
    }
}
