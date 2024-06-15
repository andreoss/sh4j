/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

public class Lexer {
    private final CharReader reader;

    public Lexer(CharReader reader) {
        this.reader = reader;
    }

    public Token nextToken() {
        skipWhitespace();
        if (reader.isEOF()) {
            return new Token(TokenType.END_OF_FILE, "", reader);
        }
        int c = reader.peek();
        if (c == '\n') {
            reader.consume();
            return new Token(TokenType.NEWLINE, "\n", reader);
        }
        Token op = tryOperator();
        if (op != null) {
            return op;
        }
        return readWord();
    }

    private void skipWhitespace() {
        while (!reader.isEOF() && (reader.peek() == ' ' || reader.peek() == '\t')) {
            reader.consume();
        }
    }

    private Token tryOperator() {
        int c = reader.peek();
        int line = reader.getLine();
        int col = reader.getColumn();
        return switch (c) {
            case '&' -> tryAmpersand(line, col);
            case '|' -> tryPipe(line, col);
            case ';' -> trySemi(line, col);
            case '<' -> tryLess(line, col);
            case '>' -> tryGreater(line, col);
            case '(' -> singleCharOp(TokenType.LPAREN, line, col);
            case ')' -> singleCharOp(TokenType.RPAREN, line, col);
            default -> null;
        };
    }

    private Token singleCharOp(TokenType type, int line, int col) {
        int c = reader.consume();
        return new Token(type, String.valueOf((char) c), line, col);
    }

    private Token tryAmpersand(int line, int col) {
        reader.consume();
        if (reader.peek() == '&') {
            reader.consume();
            return new Token(TokenType.AND_IF, "&&", line, col);
        }
        return new Token(TokenType.AMP, "&", line, col);
    }

    private Token tryPipe(int line, int col) {
        reader.consume();
        if (reader.peek() == '|') {
            reader.consume();
            return new Token(TokenType.OR_IF, "||", line, col);
        }
        return new Token(TokenType.PIPE, "|", line, col);
    }

    private Token trySemi(int line, int col) {
        reader.consume();
        if (reader.peek() == ';') {
            reader.consume();
            return new Token(TokenType.DSEMI, ";;", line, col);
        }
        return new Token(TokenType.SEMI, ";", line, col);
    }

    private Token tryLess(int line, int col) {
        reader.consume();
        if (reader.peek() == '<') {
            reader.consume();
            if (reader.peek() == '-') {
                reader.consume();
                return new Token(TokenType.DLESSDASH, "<<-", line, col);
            }
            return new Token(TokenType.DLESS, "<<", line, col);
        }
        if (reader.peek() == '&') {
            reader.consume();
            return new Token(TokenType.LESSAND, "<&", line, col);
        }
        if (reader.peek() == '>') {
            reader.consume();
            return new Token(TokenType.LESSGREATER, "<>", line, col);
        }
        return new Token(TokenType.LESS, "<", line, col);
    }

    private Token tryGreater(int line, int col) {
        reader.consume();
        if (reader.peek() == '>') {
            reader.consume();
            return new Token(TokenType.DGREATER, ">>", line, col);
        }
        if (reader.peek() == '|') {
            reader.consume();
            return new Token(TokenType.CLOBBER, ">|", line, col);
        }
        if (reader.peek() == '&') {
            reader.consume();
            return new Token(TokenType.GREATAND, ">&", line, col);
        }
        return new Token(TokenType.GREAT, ">", line, col);
    }

    private Token readWord() {
        int line = reader.getLine();
        int col = reader.getColumn();
        StringBuilder sb = new StringBuilder();
        while (!reader.isEOF()) {
            int c = reader.peek();
            if (c == '\n' || c == ' ' || c == '\t' || isOperatorChar(c)) {
                break;
            }
            sb.append((char) reader.consume());
        }
        String text = sb.toString();
        return new Token(TokenType.WORD, text, line, col);
    }

    private static boolean isOperatorChar(int c) {
        return c == '&' || c == '|' || c == ';' || c == '<' || c == '>' || c == '(' || c == ')';
    }
}
