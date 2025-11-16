/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * The set of lexical categories the {@link Lexer} can emit. Each constant
 * corresponds to a POSIX shell token class from the standard's lexical
 * grammar (section 2.10).
 * @since 1.0
 */
public enum TokenType {

    /** Generic word (command name, argument). */
    WORD,
    /** A {@code NAME=value} assignment word. */
    ASSIGNMENT_WORD,
    /** A bare name suitable as an identifier. */
    NAME,
    /** End-of-line newline character. */
    NEWLINE,
    /** Numeric file descriptor preceding a redirection operator. */
    IO_NUMBER,
    /** Logical AND operator {@code &&}. */
    AND_IF,
    /** Logical OR operator {@code ||}. */
    OR_IF,
    /** Case-arm terminator {@code ;;}. */
    DSEMI,
    /** Here-document operator {@code <<}. */
    DLESS,
    /** Append-output operator {@code >>}. */
    DGREATER,
    /** Read-from-fd duplicate operator {@code <&}. */
    LESSAND,
    /** Write-to-fd duplicate operator {@code >&}. */
    GREATAND,
    /** Read-write redirection operator {@code <>}. */
    LESSGREATER,
    /** Tab-stripping here-document operator {@code <<-}. */
    DLESSDASH,
    /** No-clobber output operator {@code >|}. */
    CLOBBER,
    /** Pipeline operator {@code |}. */
    PIPE,
    /** Background-job operator {@code &}. */
    AMP,
    /** Statement separator {@code ;}. */
    SEMI,
    /** Input redirection operator {@code <}. */
    LESS,
    /** Output redirection operator {@code >}. */
    GREAT,
    /** Subshell-open punctuation {@code (}. */
    LPAREN,
    /** Subshell-close punctuation {@code )}. */
    RPAREN,
    /** Brace-group open punctuation: opening curly brace. */
    LBRACE,
    /** Brace-group close punctuation: closing curly brace. */
    RBRACE,
    /** End-of-input sentinel. */
    END_OF_FILE
}
