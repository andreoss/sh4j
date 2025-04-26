/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Immutable lexer output. Carries the token's category plus its raw text
 * and the source position it came from.
 *
 * @param type   the token's syntactic category
 * @param text   the verbatim characters that produced the token
 * @param line   1-based source line where the token starts
 * @param column 1-based source column where the token starts
 *
 * @since 1.0
 */
public record Token(TokenType type, String text, int line, int column) {

    /**
     * Builds a token, reading its position from the given reader.
     *
     * @param type   the token's syntactic category
     * @param text   the verbatim characters that produced the token
     * @param reader the lexer's input cursor, queried for line/column
     * @return a token at the reader's current position
     */
    public static Token of(final TokenType type, final String text, final CharReader reader) {
        return new Token(type, text, reader.getLine(), reader.getColumn());
    }
}
