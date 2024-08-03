/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

public enum TokenType {
    WORD,
    ASSIGNMENT_WORD,
    NAME,
    NEWLINE,
    IO_NUMBER,
    AND_IF,
    OR_IF,
    DSEMI,
    DLESS,
    DGREATER,
    LESSAND,
    GREATAND,
    LESSGREATER,
    DLESSDASH,
    CLOBBER,
    PIPE,
    AMP,
    SEMI,
    LESS,
    GREAT,
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    END_OF_FILE
}
