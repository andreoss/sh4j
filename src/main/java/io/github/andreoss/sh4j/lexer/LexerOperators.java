/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Operator tokenisation for {@link Lexer}.
 *
 * @since 1.0
 */
final class LexerOperators {

    private LexerOperators() {
    }

    /**
     * Tries to match an operator at the current position.
     *
     * @param reader Input cursor
     * @return The operator token, or null when not an operator
     */
    static Token tryOperator(final CharReader reader) {
        final int chr = reader.peek();
        final Position pos = new Position(reader.getLine(), reader.getColumn());
        final Token result;
        if (chr == '&') {
            result = LexerOperators.tryAmpersand(reader, pos);
        } else if (chr == '|') {
            result = LexerOperators.tryPipe(reader, pos);
        } else if (chr == ';') {
            result = LexerOperators.trySemi(reader, pos);
        } else if (chr == '<') {
            result = LexerOperators.tryLess(reader, pos);
        } else if (chr == '>') {
            result = LexerOperators.tryGreater(reader, pos);
        } else if (chr == '(') {
            result = LexerOperators.singleCharOp(reader, TokenType.LPAREN, pos);
        } else if (chr == ')') {
            result = LexerOperators.singleCharOp(reader, TokenType.RPAREN, pos);
        } else if (chr == '{') {
            result = LexerOperators.singleCharOp(reader, TokenType.LBRACE, pos);
        } else if (chr == '}') {
            result = LexerOperators.singleCharOp(reader, TokenType.RBRACE, pos);
        } else {
            result = null;
        }
        return result;
    }

    private static Token singleCharOp(
        final CharReader reader, final TokenType type, final Position pos
    ) {
        return new Token(type, String.valueOf((char) reader.consume()), pos);
    }

    private static Token tryAmpersand(final CharReader reader, final Position pos) {
        reader.consume();
        final Token result;
        if (reader.peek() == '&') {
            reader.consume();
            result = new Token(TokenType.AND_IF, "&&", pos);
        } else {
            result = new Token(TokenType.AMP, "&", pos);
        }
        return result;
    }

    private static Token tryPipe(final CharReader reader, final Position pos) {
        reader.consume();
        final Token result;
        if (reader.peek() == '|') {
            reader.consume();
            result = new Token(TokenType.OR_IF, "||", pos);
        } else {
            result = new Token(TokenType.PIPE, "|", pos);
        }
        return result;
    }

    private static Token trySemi(final CharReader reader, final Position pos) {
        reader.consume();
        final Token result;
        if (reader.peek() == ';') {
            reader.consume();
            result = new Token(TokenType.DSEMI, ";;", pos);
        } else {
            result = new Token(TokenType.SEMI, ";", pos);
        }
        return result;
    }

    private static Token tryLess(final CharReader reader, final Position pos) {
        reader.consume();
        final Token result;
        final int next = reader.peek();
        if (next == '<') {
            reader.consume();
            if (reader.peek() == '-') {
                reader.consume();
                result = new Token(TokenType.DLESSDASH, "<<-", pos);
            } else {
                result = new Token(TokenType.DLESS, "<<", pos);
            }
        } else if (next == '&') {
            reader.consume();
            result = new Token(TokenType.LESSAND, "<&", pos);
        } else if (next == '>') {
            reader.consume();
            result = new Token(TokenType.LESSGREATER, "<>", pos);
        } else {
            result = new Token(TokenType.LESS, "<", pos);
        }
        return result;
    }

    private static Token tryGreater(final CharReader reader, final Position pos) {
        reader.consume();
        final Token result;
        final int next = reader.peek();
        if (next == '>') {
            reader.consume();
            result = new Token(TokenType.DGREATER, ">>", pos);
        } else if (next == '|') {
            reader.consume();
            result = new Token(TokenType.CLOBBER, ">|", pos);
        } else if (next == '&') {
            reader.consume();
            result = new Token(TokenType.GREATAND, ">&", pos);
        } else {
            result = new Token(TokenType.GREAT, ">", pos);
        }
        return result;
    }
}
