/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

public record Token(TokenType type, String text, int line, int column) {
    public Token(TokenType type, String text, CharReader reader) {
        this(type, text, reader.getLine(), reader.getColumn());
    }
}
