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
            case '{' -> singleCharOp(TokenType.LBRACE, line, col);
            case '}' -> singleCharOp(TokenType.RBRACE, line, col);
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
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        while (!reader.isEOF()) {
            int c = reader.peek();

            if (c == '\\' && !inSingleQuote && !inDoubleQuote) {
                reader.consume();
                if (reader.peek() == '\n') {
                    reader.consume();
                    continue;
                }
                sb.append('\\');
                if (!reader.isEOF()) {
                    sb.append((char) reader.consume());
                }
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                reader.consume();
                sb.append('\'');
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                reader.consume();
                sb.append('"');
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (inSingleQuote) {
                sb.append((char) reader.consume());
                continue;
            }

            if (inDoubleQuote) {
                if (c == '\\') {
                    reader.consume();
                    sb.append('\\');
                    if (reader.peek() == '\n') {
                        reader.consume();
                        sb.deleteCharAt(sb.length() - 1);
                        continue;
                    }
                    if (reader.peek() == '$' || reader.peek() == '`'
                            || reader.peek() == '"' || reader.peek() == '\\') {
                        sb.append((char) reader.consume());
                    }
                    continue;
                }
                sb.append((char) reader.consume());
                continue;
            }

            if (c == '$') {
                reader.consume();
                sb.append('$');
                if (!reader.isEOF() && reader.peek() == '(') {
                    reader.consume();
                    sb.append('(');
                    boolean arith = !reader.isEOF() && reader.peek() == '(';
                    if (arith) { reader.consume(); sb.append('('); }
                    int depth = arith ? 2 : 1;
                    while (!reader.isEOF() && depth > 0) {
                        int nc = reader.consume();
                        sb.append((char) nc);
                        if (nc == '(') depth++;
                        else if (nc == ')') depth--;
                    }
                    continue;
                }
                if (!reader.isEOF() && reader.peek() == '{') {
                    reader.consume();
                    sb.append('{');
                    int depth = 1;
                    while (!reader.isEOF() && depth > 0) {
                        int nc = reader.consume();
                        sb.append((char) nc);
                        if (nc == '{') depth++;
                        else if (nc == '}') depth--;
                    }
                    continue;
                }
                continue;
            }

            if (c == '`') {
                reader.consume();
                sb.append('`');
                while (!reader.isEOF()) {
                    int nc = reader.consume();
                    sb.append((char) nc);
                    if (nc == '`') break;
                    if (nc == '\\' && !reader.isEOF()) {
                        sb.append((char) reader.consume());
                    }
                }
                continue;
            }

            if (c == '\n' || c == ' ' || c == '\t' || isOperatorChar(c)) {
                break;
            }

            sb.append((char) reader.consume());
        }
        String text = sb.toString();
        if (isAllDigits(text) && !reader.isEOF()
                && (reader.peek() == '<' || reader.peek() == '>')) {
            return new Token(TokenType.IO_NUMBER, text, line, col);
        }
        TokenType type = isAssignmentWord(text) ? TokenType.ASSIGNMENT_WORD : TokenType.WORD;
        return new Token(type, text, line, col);
    }

    public String readHereDocContent(String delimiter, boolean stripTabs) {
        StringBuilder content = new StringBuilder();
        if (reader.peek() == '\n') {
            reader.consume();
        }
        while (!reader.isEOF()) {
            StringBuilder line = new StringBuilder();
            while (!reader.isEOF()) {
                int c = reader.peek();
                if (c == '\n') break;
                if (stripTabs && line.length() == 0 && c == '\t') {
                    reader.consume();
                    continue;
                }
                line.append((char) reader.consume());
            }
            if (!reader.isEOF()) reader.consume();
            String lineStr = line.toString();
            String check = stripTabs ? stripLeadingTabs(lineStr) : lineStr;
            if (check.equals(delimiter)) break;
            if (content.length() > 0) content.append('\n');
            content.append(lineStr);
        }
        return content.toString();
    }

    private static String stripLeadingTabs(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == '\t') i++;
        return s.substring(i);
    }

    private boolean isAssignmentWord(String text) {
        if (text.isEmpty() || text.equals("=")) {
            return false;
        }
        int eqIndex = text.indexOf('=');
        if (eqIndex <= 0) {
            return false;
        }
        String name = text.substring(0, eqIndex);
        return isValidVarName(name);
    }

    private boolean isValidVarName(String name) {
        char first = name.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllDigits(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    private static boolean isOperatorChar(int c) {
        return c == '&' || c == '|' || c == ';' || c == '<' || c == '>' || c == '(' || c == ')' || c == '{' || c == '}';
    }
}
