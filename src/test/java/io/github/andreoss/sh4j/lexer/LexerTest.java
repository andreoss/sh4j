/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LexerTest {

    @Test
    void eofOnEmptyInput() {
        final Lexer list = this.lexer("");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.END_OF_FILE, tokn.type());
        assertEquals("", tokn.text());
    }

    @Test
    void newline() {
        final Lexer list = this.lexer(String.format("%n"));
        final Token tokn = list.nextToken();
        assertEquals(TokenType.NEWLINE, tokn.type());
        assertEquals(String.format("%n"), tokn.text());
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

    @Test
    void simpleWord() {
        final Lexer list = this.lexer("echo");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("echo", tokn.text());
    }

    @Test
    void wordWithWhitespacePrefix() {
        final Lexer list = this.lexer("  ls");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("ls", tokn.text());
    }

    @Test
    void multipleTokens() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        final Token tokfirst = list.nextToken();
        assertEquals(TokenType.WORD, tokfirst.type());
        assertEquals("echo", tokfirst.text());
        final Token toksecond = list.nextToken();
        assertEquals(TokenType.WORD, toksecond.type());
        assertEquals("hello", toksecond.text());
        assertEquals(TokenType.NEWLINE, list.nextToken().type());
    }

    @Test
    void operatorsSeparatedBySpaces() {
        final Lexer list = this.lexer("&& ||");
        assertEquals(TokenType.AND_IF, list.nextToken().type());
        assertEquals(TokenType.OR_IF, list.nextToken().type());
    }

    @Test
    void wordThenOperator() {
        final Lexer list = this.lexer("cmd&");
        final Token tokfirst = list.nextToken();
        assertEquals(TokenType.WORD, tokfirst.type());
        assertEquals("cmd", tokfirst.text());
        assertEquals(TokenType.AMP, list.nextToken().type());
    }

    @Test
    void operatorThenWord() {
        final Lexer list = this.lexer(">out");
        assertEquals(TokenType.GREAT, list.nextToken().type());
        final Token toksecond = list.nextToken();
        assertEquals(TokenType.WORD, toksecond.type());
        assertEquals("out", toksecond.text());
    }

    @Test
    void consecutiveNewlines() {
        final Lexer list = this.lexer(String.format("%n%n"));
        assertEquals(TokenType.NEWLINE, list.nextToken().type());
        assertEquals(TokenType.NEWLINE, list.nextToken().type());
    }

    @Test
    void tokenPositions() {
        final Lexer list = this.lexer("  word");
        final Token tokn = list.nextToken();
        assertEquals(1, tokn.line());
        assertEquals(2, tokn.column());
    }

    @Test
    void eofAfterTokens() {
        final Lexer list = this.lexer("a");
        list.nextToken();
        assertEquals(TokenType.END_OF_FILE, list.nextToken().type());
    }

    @Test
    void whitespaceSkippedBetweenTokens() {
        final Lexer list = this.lexer("a   b");
        assertEquals("a", list.nextToken().text());
        assertEquals("b", list.nextToken().text());
    }

    @Test
    void tabIsWhitespace() {
        final Lexer list = this.lexer("\tword");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("word", tokn.text());
    }

    @Test
    void equalsAloneIsWordNotAssignment() {
        final Lexer list = this.lexer("=");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("=", tokn.text());
    }

    @Test
    void numberPrefixedNameIsNotAssignment() {
        final Lexer list = this.lexer("1a=b");
        assertEquals(TokenType.WORD, list.nextToken().type());
    }

    @Test
    void invalidVarNameCharIsNotAssignment() {
        final Lexer list = this.lexer("a@b=c");
        assertEquals(TokenType.WORD, list.nextToken().type());
    }

    @Test
    void wordBreaksAtLeftParen() {
        final Lexer list = this.lexer("echo(");
        final Token tokfirst = list.nextToken();
        assertEquals(TokenType.WORD, tokfirst.type());
        assertEquals("echo", tokfirst.text());
        assertEquals(TokenType.LPAREN, list.nextToken().type());
    }

    @Test
    void wordBreaksAtRightParen() {
        final Lexer list = this.lexer("echo)");
        final Token tokfirst = list.nextToken();
        assertEquals(TokenType.WORD, tokfirst.type());
        assertEquals("echo", tokfirst.text());
        assertEquals(TokenType.RPAREN, list.nextToken().type());
    }

    @Test
    void singleQuotedWord() {
        final Lexer list = this.lexer("'hello'");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("'hello'", tokn.text());
    }

    @Test
    void doubleQuotedWord() {
        final Lexer list = this.lexer("\"hello\"");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("\"hello\"", tokn.text());
    }

    @Test
    void backslashEscapesInsideWord() {
        final Lexer list = this.lexer("a\\ b");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("a\\ b", tokn.text());
    }

    @Test
    void backslashContinuation() {
        final Lexer list = this.lexer(String.format("a\\%nb"));
        assertEquals("ab", list.nextToken().text());
    }

    @Test
    void mixedQuoting() {
        final Lexer list = this.lexer("a'b'c");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("a'b'c", tokn.text());
    }

    @Test
    void doubleQuotedBackslashDollar() {
        final Lexer list = this.lexer("\"\\$\"");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("\"\\$\"", tokn.text());
    }

    @Test
    void doubleQuotedBackslashBacktick() {
        final Lexer list = this.lexer("\"\\`\"");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("\"\\`\"", tokn.text());
    }

    @Test
    void doubleQuotedBackslashNewline() {
        final Lexer list = this.lexer(String.format("\"a\\%nb\""));
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("\"ab\"", tokn.text());
    }

    @Test
    void doubleQuotedBackslashBackslash() {
        final Lexer list = this.lexer("\"a\\\\b\"");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("\"a\\\\b\"", tokn.text());
    }

    @Test
    void doubleQuotedBackslashDoublequote() {
        final Lexer list = this.lexer("\"a\\\"b\"");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("\"a\\\"b\"", tokn.text());
    }

    @Test
    void wordBreakAtAllOperators() {
        for (final String oper : new String[]{"&", "|", ";", "<", ">", "(", ")"}) {
            final Lexer list = this.lexer(String.format("a%s", oper));
            assertEquals("a", list.nextToken().text());
            assertEquals(oper, list.nextToken().text());
        }
    }

    @Test
    void assignmentWordEdgeCases() {
        assertEquals(TokenType.WORD, this.lexer("=value").nextToken().type());
        assertEquals(TokenType.ASSIGNMENT_WORD, this.lexer("_=value").nextToken().type());
    }

    @Test
    void backslashAtEof() {
        assertEquals("a\\", this.lexer("a\\").nextToken().text());
    }

    @Test
    void equalsAloneIsWord() {
        assertEquals(TokenType.WORD, this.lexer("=value").nextToken().type());
    }

    @Test
    void doubleQuotedBackslashNonSpecialKeptLiteral() {
        final Lexer list = this.lexer("\"\\a\"");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("\"\\a\"", tokn.text());
    }

    @Test
    void ioNumberBeforeInputRedirect() {
        final Lexer list = this.lexer("2<file");
        final Token tokfirst = list.nextToken();
        assertEquals(TokenType.IO_NUMBER, tokfirst.type());
        assertEquals("2", tokfirst.text());
        assertEquals(TokenType.LESS, list.nextToken().type());
    }

    @Test
    void digitsThenSpaceIsWordNotIoNumber() {
        final Lexer list = this.lexer("42 ");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("42", tokn.text());
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuote() {
        final Lexer list = this.lexer("\"a'b\"");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("\"a'b\"", tokn.text());
    }

    @Test
    void backslashNewlineLineContinuation() {
        final Lexer list = this.lexer(String.format("ab\\%ncd"));
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("abcd", tokn.text());
    }

    @Test
    void dollarParenStaysInWord() {
        final Lexer list = this.lexer("$(echo hi)");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("$(echo hi)", tokn.text());
    }

    @Test
    void dollarDoubleParenArithStaysInWord() {
        final Lexer list = this.lexer("$((1+2*3))");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("$((1+2*3))", tokn.text());
    }

    @Test
    void dollarParenWithNestedParensStaysInWord() {
        final Lexer list = this.lexer("$(echo $((1+2)))");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("$(echo $((1+2)))", tokn.text());
    }

    @Test
    void dollarBraceStaysInWord() {
        final Lexer list = this.lexer("${VAR}");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("${VAR}", tokn.text());
    }

    @Test
    void dollarBraceWithNestedBraceStaysInWord() {
        final Lexer list = this.lexer("${a:-${b}}");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("${a:-${b}}", tokn.text());
    }

    @Test
    void backtickStaysInWord() {
        final Lexer list = this.lexer("`echo hi`");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("`echo hi`", tokn.text());
    }

    @Test
    void backtickWithBackslashEscape() {
        final Lexer list = this.lexer("`echo \\`hi\\``");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("`echo \\`hi\\``", tokn.text());
    }

    @Test
    void wordWithDollarParenAndTrailingText() {
        final Lexer list = this.lexer("a$(echo b)c");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("a$(echo b)c", tokn.text());
    }

    @Test
    void wordWithBacktickAndTrailingText() {
        final Lexer list = this.lexer("a`b`c");
        final Token tokn = list.nextToken();
        assertEquals(TokenType.WORD, tokn.type());
        assertEquals("a`b`c", tokn.text());
    }

    @Test
    void readHereDocContentSkipsLeadingNewline() {
        assertEquals("hello", this.lexer(String.format("%nhello%nEOF%n")).readHereDocContent("EOF", false));
    }

    @Test
    void readHereDocContentEmptyBody() {
        assertEquals("", this.lexer(String.format("EOF%n")).readHereDocContent("EOF", false));
    }

    @Test
    void readHereDocContentMultipleLines() {
        assertEquals(String.format("first%nsecond%nthird"), this.lexer(String.format("%nfirst%nsecond%nthird%nEOF%n")).readHereDocContent("EOF", false));
    }

    private Lexer lexer(final String input) {
        return new Lexer(new CharReader(input));
    }

    private void assertOperator(final String op, final TokenType expected) {
        final Lexer list = this.lexer(op);
        final Token tokn = list.nextToken();
        assertEquals(expected, tokn.type(), String.format("Operator %s", op));
        assertEquals(op, tokn.text(), String.format("Operator text %s", op));
    }

}
