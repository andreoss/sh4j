/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Operator tokenisation for {@link Lexer}.
 * @since 1.0
 */
final class LexerOperators {

    /**
     * Utility class; no instances.
     */
    private LexerOperators() {
        super();
    }

    /**
     * Tries to match an operator at the current position.
     * @param reader Input cursor
     * @return The operator token, or null when not an operator
     */
    static Token tryOperator(final CharReader reader) {
        final int chr = reader.peek();
        final int line = reader.getLine();
        final int col = reader.getColumn();
        final Token result;
        if (chr == '&') {
            result = LexerOperators.tryAmpersand(reader, line, col);
        } else if (chr == '|') {
            result = LexerOperators.tryPipe(reader, line, col);
        } else if (chr == ';') {
            result = LexerOperators.trySemi(reader, line, col);
        } else if (chr == '<') {
            result = LexerOperators.tryLess(reader, line, col);
        } else if (chr == '>') {
            result = LexerOperators.tryGreater(reader, line, col);
        } else if (chr == '(') {
            result = LexerOperators.singleCharOp(
                reader, TokenType.LPAREN, line, col
            );
        } else if (chr == ')') {
            result = LexerOperators.singleCharOp(
                reader, TokenType.RPAREN, line, col
            );
        } else if (chr == '{') {
            result = LexerOperators.singleCharOp(
                reader, TokenType.LBRACE, line, col
            );
        } else if (chr == '}') {
            result = LexerOperators.singleCharOp(
                reader, TokenType.RBRACE, line, col
            );
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Consumes one single-character operator.
     * @param reader Input cursor
     * @param type Token type
     * @param line Source line
     * @param col Source column
     * @return The token
     */
    private static Token singleCharOp(
        final CharReader reader, final TokenType type,
        final int line, final int col
    ) {
        return new Token(type, String.valueOf((char) reader.consume()), line, col);
    }

    /**
     * Consumes an ampersand operator.
     * @param reader Input cursor
     * @param line Source line
     * @param col Source column
     * @return The token
     */
    private static Token tryAmpersand(
        final CharReader reader, final int line, final int col
    ) {
        reader.consume();
        final Token result;
        if (reader.peek() == '&') {
            reader.consume();
            result = new Token(TokenType.AND_IF, "&&", line, col);
        } else {
            result = new Token(TokenType.AMP, "&", line, col);
        }
        return result;
    }

    /**
     * Consumes a pipe operator.
     * @param reader Input cursor
     * @param line Source line
     * @param col Source column
     * @return The token
     */
    private static Token tryPipe(
        final CharReader reader, final int line, final int col
    ) {
        reader.consume();
        final Token result;
        if (reader.peek() == '|') {
            reader.consume();
            result = new Token(TokenType.OR_IF, "||", line, col);
        } else {
            result = new Token(TokenType.PIPE, "|", line, col);
        }
        return result;
    }

    /**
     * Consumes a semicolon operator.
     * @param reader Input cursor
     * @param line Source line
     * @param col Source column
     * @return The token
     */
    private static Token trySemi(
        final CharReader reader, final int line, final int col
    ) {
        reader.consume();
        final Token result;
        if (reader.peek() == ';') {
            reader.consume();
            result = new Token(TokenType.DSEMI, ";;", line, col);
        } else {
            result = new Token(TokenType.SEMI, ";", line, col);
        }
        return result;
    }

    /**
     * Consumes a less-than operator.
     * @param reader Input cursor
     * @param line Source line
     * @param col Source column
     * @return The token
     */
    private static Token tryLess(
        final CharReader reader, final int line, final int col
    ) {
        reader.consume();
        final Token result;
        final int next = reader.peek();
        if (next == '<') {
            reader.consume();
            if (reader.peek() == '-') {
                reader.consume();
                result = new Token(TokenType.DLESSDASH, "<<-", line, col);
            } else {
                result = new Token(TokenType.DLESS, "<<", line, col);
            }
        } else if (next == '&') {
            reader.consume();
            result = new Token(TokenType.LESSAND, "<&", line, col);
        } else if (next == '>') {
            reader.consume();
            result = new Token(TokenType.LESSGREATER, "<>", line, col);
        } else {
            result = new Token(TokenType.LESS, "<", line, col);
        }
        return result;
    }

    /**
     * Consumes a greater-than operator.
     * @param reader Input cursor
     * @param line Source line
     * @param col Source column
     * @return The token
     */
    private static Token tryGreater(
        final CharReader reader, final int line, final int col
    ) {
        reader.consume();
        final Token result;
        final int next = reader.peek();
        if (next == '>') {
            reader.consume();
            result = new Token(TokenType.DGREATER, ">>", line, col);
        } else if (next == '|') {
            reader.consume();
            result = new Token(TokenType.CLOBBER, ">|", line, col);
        } else if (next == '&') {
            reader.consume();
            result = new Token(TokenType.GREATAND, ">&", line, col);
        } else {
            result = new Token(TokenType.GREAT, ">", line, col);
        }
        return result;
    }
}
