/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LexerTest {
    private Lexer lexer(String input) {
        return new Lexer(new CharReader(input));
    }

    @Test
    void eofOnEmptyInput() {
        Lexer l = lexer("");
        Token t = l.nextToken();
        assertEquals(TokenType.END_OF_FILE, t.type());
        assertEquals("", t.text());
    }

    @Test
    void newline() {
        Lexer l = lexer("\n");
        Token t = l.nextToken();
        assertEquals(TokenType.NEWLINE, t.type());
        assertEquals("\n", t.text());
    }

    @Test
    void singleCharOperators() {
        assertOperator("|", TokenType.PIPE);
        assertOperator("&", TokenType.AMP);
        assertOperator(";", TokenType.SEMI);
        assertOperator("<", TokenType.LESS);
        assertOperator(">", TokenType.GREAT);
        assertOperator("(", TokenType.LPAREN);
        assertOperator(")", TokenType.RPAREN);
    }

    @Test
    void multiCharOperators() {
        assertOperator("&&", TokenType.AND_IF);
        assertOperator("||", TokenType.OR_IF);
        assertOperator(";;", TokenType.DSEMI);
        assertOperator("<<", TokenType.DLESS);
        assertOperator(">>", TokenType.DGREATER);
        assertOperator("<&", TokenType.LESSAND);
        assertOperator(">&", TokenType.GREATAND);
        assertOperator("<>", TokenType.LESSGREATER);
        assertOperator("<<-", TokenType.DLESSDASH);
        assertOperator(">|", TokenType.CLOBBER);
    }

    private void assertOperator(String op, TokenType expected) {
        Lexer l = lexer(op);
        Token t = l.nextToken();
        assertEquals(expected, t.type(), "Operator " + op);
        assertEquals(op, t.text(), "Operator text " + op);
    }

    @Test
    void simpleWord() {
        Lexer l = lexer("echo");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("echo", t.text());
    }

    @Test
    void wordWithWhitespacePrefix() {
        Lexer l = lexer("  ls");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("ls", t.text());
    }

    @Test
    void multipleTokens() {
        Lexer l = lexer("echo hello\n");
        Token t1 = l.nextToken();
        assertEquals(TokenType.WORD, t1.type());
        assertEquals("echo", t1.text());
        Token t2 = l.nextToken();
        assertEquals(TokenType.WORD, t2.type());
        assertEquals("hello", t2.text());
        Token t3 = l.nextToken();
        assertEquals(TokenType.NEWLINE, t3.type());
    }

    @Test
    void operatorsSeparatedBySpaces() {
        Lexer l = lexer("&& ||");
        Token t1 = l.nextToken();
        assertEquals(TokenType.AND_IF, t1.type());
        Token t2 = l.nextToken();
        assertEquals(TokenType.OR_IF, t2.type());
    }

    @Test
    void wordThenOperator() {
        Lexer l = lexer("cmd&");
        Token t1 = l.nextToken();
        assertEquals(TokenType.WORD, t1.type());
        assertEquals("cmd", t1.text());
        Token t2 = l.nextToken();
        assertEquals(TokenType.AMP, t2.type());
    }

    @Test
    void operatorThenWord() {
        Lexer l = lexer(">out");
        Token t1 = l.nextToken();
        assertEquals(TokenType.GREAT, t1.type());
        Token t2 = l.nextToken();
        assertEquals(TokenType.WORD, t2.type());
        assertEquals("out", t2.text());
    }

    @Test
    void consecutiveNewlines() {
        Lexer l = lexer("\n\n");
        assertEquals(TokenType.NEWLINE, l.nextToken().type());
        assertEquals(TokenType.NEWLINE, l.nextToken().type());
    }

    @Test
    void tokenPositions() {
        Lexer l = lexer("  word");
        Token t = l.nextToken();
        assertEquals(1, t.line());
        assertEquals(2, t.column());
    }

    @Test
    void eofAfterTokens() {
        Lexer l = lexer("a");
        l.nextToken();
        Token t = l.nextToken();
        assertEquals(TokenType.END_OF_FILE, t.type());
    }

    @Test
    void whitespaceSkippedBetweenTokens() {
        Lexer l = lexer("a   b");
        assertEquals("a", l.nextToken().text());
        assertEquals("b", l.nextToken().text());
    }

    @Test
    void tabIsWhitespace() {
        Lexer l = lexer("\tword");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("word", t.text());
    }

    @Test
    void equalsAloneIsWordNotAssignment() {
        Lexer l = lexer("=");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("=", t.text());
    }

    @Test
    void numberPrefixedNameIsNotAssignment() {
        Lexer l = lexer("1a=b");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
    }

    @Test
    void invalidVarNameCharIsNotAssignment() {
        Lexer l = lexer("a@b=c");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
    }

    @Test
    void wordBreaksAtLeftParen() {
        Lexer l = lexer("echo(");
        Token t1 = l.nextToken();
        assertEquals(TokenType.WORD, t1.type());
        assertEquals("echo", t1.text());
        Token t2 = l.nextToken();
        assertEquals(TokenType.LPAREN, t2.type());
    }

    @Test
    void wordBreaksAtRightParen() {
        Lexer l = lexer("echo)");
        Token t1 = l.nextToken();
        assertEquals(TokenType.WORD, t1.type());
        assertEquals("echo", t1.text());
        Token t2 = l.nextToken();
        assertEquals(TokenType.RPAREN, t2.type());
    }

    @Test
    void singleQuotedWord() {
        Lexer l = lexer("'hello'");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("'hello'", t.text());
    }

    @Test
    void doubleQuotedWord() {
        Lexer l = lexer("\"hello\"");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"hello\"", t.text());
    }

    @Test
    void backslashEscapesInsideWord() {
        Lexer l = lexer("a\\ b");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("a\\ b", t.text());
    }

    @Test
    void backslashContinuation() {
        Lexer l = lexer("a\\\nb");
        Token t = l.nextToken();
        assertEquals("ab", t.text());
    }

    @Test
    void mixedQuoting() {
        Lexer l = lexer("a'b'c");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("a'b'c", t.text());
    }

    @Test
    void doubleQuotedBackslashDollar() {
        Lexer l = lexer("\"\\$\"");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"\\$\"", t.text());
    }

    @Test
    void doubleQuotedBackslashBacktick() {
        Lexer l = lexer("\"\\`\"");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"\\`\"", t.text());
    }

    @Test
    void doubleQuotedBackslashNewline() {
        Lexer l = lexer("\"a\\\nb\"");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"ab\"", t.text());
    }

    @Test
    void doubleQuotedBackslashBackslash() {
        Lexer l = lexer("\"a\\\\b\"");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"a\\\\b\"", t.text());
    }

    @Test
    void doubleQuotedBackslashDoublequote() {
        Lexer l = lexer("\"a\\\"b\"");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"a\\\"b\"", t.text());
    }

    @Test
    void wordBreakAtAllOperators() {
        for (String op : new String[]{"&", "|", ";", "<", ">", "(", ")"}) {
            Lexer l = lexer("a" + op);
            assertEquals("a", l.nextToken().text());
            assertEquals(op, l.nextToken().text());
        }
    }

    @Test
    void assignmentWordEdgeCases() {
        Lexer l = lexer("=value");
        assertEquals(TokenType.WORD, l.nextToken().type());
        Lexer l2 = lexer("_=value");
        assertEquals(TokenType.ASSIGNMENT_WORD, l2.nextToken().type());
    }

    @Test
    void backslashAtEof() {
        Lexer l = lexer("a\\");
        assertEquals("a\\", l.nextToken().text());
    }

    @Test
    void equalsAloneIsWord() {
        Lexer l = lexer("=value");
        assertEquals(TokenType.WORD, l.nextToken().type());
    }

    @Test
    void doubleQuotedBackslashNonSpecialKeptLiteral() {
        Lexer l = lexer("\"\\a\"");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"\\a\"", t.text());
    }

    @Test
    void ioNumberBeforeInputRedirect() {
        Lexer l = lexer("2<file");
        Token t1 = l.nextToken();
        assertEquals(TokenType.IO_NUMBER, t1.type());
        assertEquals("2", t1.text());
        Token t2 = l.nextToken();
        assertEquals(TokenType.LESS, t2.type());
    }

    @Test
    void digitsThenSpaceIsWordNotIoNumber() {
        Lexer l = lexer("42 ");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("42", t.text());
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuote() {
        Lexer l = lexer("\"a'b\"");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"a'b\"", t.text());
    }

    @Test
    void backslashNewlineLineContinuation() {
        Lexer l = lexer("ab\\\ncd");
        Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("abcd", t.text());
    }
}
