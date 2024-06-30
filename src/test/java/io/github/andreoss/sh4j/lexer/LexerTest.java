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
}
