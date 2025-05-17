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
        final Lexer l = this.lexer("");
        final Token t = l.nextToken();
        assertEquals(TokenType.END_OF_FILE, t.type());
        assertEquals("", t.text());
    }

    @Test
    void newline() {
        final Lexer l = this.lexer("\n");
        final Token t = l.nextToken();
        assertEquals(TokenType.NEWLINE, t.type());
        assertEquals("\n", t.text());
    }

    @Test
    void singleCharOperators() {
        this.assertOperator("|", TokenType.PIPE);
        this.assertOperator("&", TokenType.AMP);
        this.assertOperator(";", TokenType.SEMI);
        this.assertOperator("<", TokenType.LESS);
        this.assertOperator(">", TokenType.GREAT);
        this.assertOperator("(", TokenType.LPAREN);
        this.assertOperator(")", TokenType.RPAREN);
    }

    @Test
    void multiCharOperators() {
        this.assertOperator("&&", TokenType.AND_IF);
        this.assertOperator("||", TokenType.OR_IF);
        this.assertOperator(";;", TokenType.DSEMI);
        this.assertOperator("<<", TokenType.DLESS);
        this.assertOperator(">>", TokenType.DGREATER);
        this.assertOperator("<&", TokenType.LESSAND);
        this.assertOperator(">&", TokenType.GREATAND);
        this.assertOperator("<>", TokenType.LESSGREATER);
        this.assertOperator("<<-", TokenType.DLESSDASH);
        this.assertOperator(">|", TokenType.CLOBBER);
    }

    private void assertOperator(String op, TokenType expected) {
        final Lexer l = this.lexer(op);
        final Token t = l.nextToken();
        assertEquals(expected, t.type(), "Operator " + op);
        assertEquals(op, t.text(), "Operator text " + op);
    }

    @Test
    void simpleWord() {
        final Lexer l = this.lexer("echo");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("echo", t.text());
    }

    @Test
    void wordWithWhitespacePrefix() {
        final Lexer l = this.lexer("  ls");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("ls", t.text());
    }

    @Test
    void multipleTokens() {
        final Lexer l = this.lexer("echo hello\n");
        final Token t1 = l.nextToken();
        assertEquals(TokenType.WORD, t1.type());
        assertEquals("echo", t1.text());
        final Token t2 = l.nextToken();
        assertEquals(TokenType.WORD, t2.type());
        assertEquals("hello", t2.text());
        assertEquals(TokenType.NEWLINE, l.nextToken().type());
    }

    @Test
    void operatorsSeparatedBySpaces() {
        final Lexer l = this.lexer("&& ||");
        final Token t1 = l.nextToken();
        assertEquals(TokenType.AND_IF, t1.type());
        final Token t2 = l.nextToken();
        assertEquals(TokenType.OR_IF, t2.type());
    }

    @Test
    void wordThenOperator() {
        final Lexer l = this.lexer("cmd&");
        final Token t1 = l.nextToken();
        assertEquals(TokenType.WORD, t1.type());
        assertEquals("cmd", t1.text());
        final Token t2 = l.nextToken();
        assertEquals(TokenType.AMP, t2.type());
    }

    @Test
    void operatorThenWord() {
        final Lexer l = this.lexer(">out");
        final Token t1 = l.nextToken();
        assertEquals(TokenType.GREAT, t1.type());
        final Token t2 = l.nextToken();
        assertEquals(TokenType.WORD, t2.type());
        assertEquals("out", t2.text());
    }

    @Test
    void consecutiveNewlines() {
        final Lexer l = this.lexer("\n\n");
        assertEquals(TokenType.NEWLINE, l.nextToken().type());
        assertEquals(TokenType.NEWLINE, l.nextToken().type());
    }

    @Test
    void tokenPositions() {
        final Lexer l = this.lexer("  word");
        final Token t = l.nextToken();
        assertEquals(1, t.line());
        assertEquals(2, t.column());
    }

    @Test
    void eofAfterTokens() {
        final Lexer l = this.lexer("a");
        l.nextToken();
        final Token t = l.nextToken();
        assertEquals(TokenType.END_OF_FILE, t.type());
    }

    @Test
    void whitespaceSkippedBetweenTokens() {
        final Lexer l = this.lexer("a   b");
        assertEquals("a", l.nextToken().text());
        assertEquals("b", l.nextToken().text());
    }

    @Test
    void tabIsWhitespace() {
        final Lexer l = this.lexer("\tword");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("word", t.text());
    }

    @Test
    void equalsAloneIsWordNotAssignment() {
        final Lexer l = this.lexer("=");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("=", t.text());
    }

    @Test
    void numberPrefixedNameIsNotAssignment() {
        final Lexer l = this.lexer("1a=b");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
    }

    @Test
    void invalidVarNameCharIsNotAssignment() {
        final Lexer l = this.lexer("a@b=c");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
    }

    @Test
    void wordBreaksAtLeftParen() {
        final Lexer l = this.lexer("echo(");
        final Token t1 = l.nextToken();
        assertEquals(TokenType.WORD, t1.type());
        assertEquals("echo", t1.text());
        assertEquals(TokenType.LPAREN, l.nextToken().type());
    }

    @Test
    void wordBreaksAtRightParen() {
        final Lexer l = this.lexer("echo)");
        final Token t1 = l.nextToken();
        assertEquals(TokenType.WORD, t1.type());
        assertEquals("echo", t1.text());
        assertEquals(TokenType.RPAREN, l.nextToken().type());
    }

    @Test
    void singleQuotedWord() {
        final Lexer l = this.lexer("'hello'");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("'hello'", t.text());
    }

    @Test
    void doubleQuotedWord() {
        final Lexer l = this.lexer("\"hello\"");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"hello\"", t.text());
    }

    @Test
    void backslashEscapesInsideWord() {
        final Lexer l = this.lexer("a\\ b");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("a\\ b", t.text());
    }

    @Test
    void backslashContinuation() {
        final Lexer l = this.lexer("a\\\nb");
        final Token t = l.nextToken();
        assertEquals("ab", t.text());
    }

    @Test
    void mixedQuoting() {
        final Lexer l = this.lexer("a'b'c");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("a'b'c", t.text());
    }

    @Test
    void doubleQuotedBackslashDollar() {
        final Lexer l = this.lexer("\"\\$\"");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"\\$\"", t.text());
    }

    @Test
    void doubleQuotedBackslashBacktick() {
        final Lexer l = this.lexer("\"\\`\"");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"\\`\"", t.text());
    }

    @Test
    void doubleQuotedBackslashNewline() {
        final Lexer l = this.lexer("\"a\\\nb\"");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"ab\"", t.text());
    }

    @Test
    void doubleQuotedBackslashBackslash() {
        final Lexer l = this.lexer("\"a\\\\b\"");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"a\\\\b\"", t.text());
    }

    @Test
    void doubleQuotedBackslashDoublequote() {
        final Lexer l = this.lexer("\"a\\\"b\"");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"a\\\"b\"", t.text());
    }

    @Test
    void wordBreakAtAllOperators() {
        for (final String op : new String[]{"&", "|", ";", "<", ">", "(", ")"}) {
            final Lexer l = this.lexer("a" + op);
            assertEquals("a", l.nextToken().text());
            assertEquals(op, l.nextToken().text());
        }
    }

    @Test
    void assignmentWordEdgeCases() {
        final Lexer l = this.lexer("=value");
        assertEquals(TokenType.WORD, l.nextToken().type());
        assertEquals(TokenType.ASSIGNMENT_WORD, this.lexer("_=value").nextToken().type());
    }

    @Test
    void backslashAtEof() {
        final Lexer l = this.lexer("a\\");
        assertEquals("a\\", l.nextToken().text());
    }

    @Test
    void equalsAloneIsWord() {
        final Lexer l = this.lexer("=value");
        assertEquals(TokenType.WORD, l.nextToken().type());
    }

    @Test
    void doubleQuotedBackslashNonSpecialKeptLiteral() {
        final Lexer l = this.lexer("\"\\a\"");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"\\a\"", t.text());
    }

    @Test
    void ioNumberBeforeInputRedirect() {
        final Lexer l = this.lexer("2<file");
        final Token t1 = l.nextToken();
        assertEquals(TokenType.IO_NUMBER, t1.type());
        assertEquals("2", t1.text());
        assertEquals(TokenType.LESS, l.nextToken().type());
    }

    @Test
    void digitsThenSpaceIsWordNotIoNumber() {
        final Lexer l = this.lexer("42 ");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("42", t.text());
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuote() {
        final Lexer l = this.lexer("\"a'b\"");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("\"a'b\"", t.text());
    }

    @Test
    void backslashNewlineLineContinuation() {
        final Lexer l = this.lexer("ab\\\ncd");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("abcd", t.text());
    }

    @Test
    void dollarParenStaysInWord() {
        final Lexer l = this.lexer("$(echo hi)");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("$(echo hi)", t.text());
    }

    @Test
    void dollarDoubleParenArithStaysInWord() {
        final Lexer l = this.lexer("$((1+2*3))");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("$((1+2*3))", t.text());
    }

    @Test
    void dollarParenWithNestedParensStaysInWord() {
        final Lexer l = this.lexer("$(echo $((1+2)))");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("$(echo $((1+2)))", t.text());
    }

    @Test
    void dollarBraceStaysInWord() {
        final Lexer l = this.lexer("${VAR}");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("${VAR}", t.text());
    }

    @Test
    void dollarBraceWithNestedBraceStaysInWord() {
        final Lexer l = this.lexer("${a:-${b}}");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("${a:-${b}}", t.text());
    }

    @Test
    void backtickStaysInWord() {
        final Lexer l = this.lexer("`echo hi`");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("`echo hi`", t.text());
    }

    @Test
    void backtickWithBackslashEscape() {
        final Lexer l = this.lexer("`echo \\`hi\\``");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("`echo \\`hi\\``", t.text());
    }

    @Test
    void wordWithDollarParenAndTrailingText() {
        final Lexer l = this.lexer("a$(echo b)c");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("a$(echo b)c", t.text());
    }

    @Test
    void wordWithBacktickAndTrailingText() {
        final Lexer l = this.lexer("a`b`c");
        final Token t = l.nextToken();
        assertEquals(TokenType.WORD, t.type());
        assertEquals("a`b`c", t.text());
    }

    @Test
    void readHereDocContentSkipsLeadingNewline() {
        final Lexer l = this.lexer("\nhello\nEOF\n");
        assertEquals("hello", l.readHereDocContent("EOF", false));
    }

    @Test
    void readHereDocContentEmptyBody() {
        final Lexer l = this.lexer("EOF\n");
        assertEquals("", l.readHereDocContent("EOF", false));
    }

    @Test
    void readHereDocContentMultipleLines() {
        final Lexer l = this.lexer("\nfirst\nsecond\nthird\nEOF\n");
        assertEquals("first\nsecond\nthird", l.readHereDocContent("EOF", false));
    }
}
