/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for Lexer.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class LexerTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    public void eofOnEmptyInput() {
        final Lexer list = this.lexer("");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.END_OF_FILE, tokn.type());
        Assert.assertEquals("", tokn.text());
    }

    @Test
    public void newline() {
        final Lexer list = this.lexer(String.format("%n"));
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.NEWLINE, tokn.type());
        Assert.assertEquals(String.format("%n"), tokn.text());
    }

    @Test
    public void singleCharOperators() {
        this.assertOperator("|", TokenType.PIPE);
        this.assertOperator("&", TokenType.AMP);
        this.assertOperator(";", TokenType.SEMI);
        this.assertOperator("<", TokenType.LESS);
        this.assertOperator(">", TokenType.GREAT);
        this.assertOperator("(", TokenType.LPAREN);
        this.assertOperator(")", TokenType.RPAREN);
    }

    @Test
    public void multiCharOperators() {
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
    public void simpleWord() {
        final Lexer list = this.lexer(LexerTest.ECHO_CMD);
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals(LexerTest.ECHO_CMD, tokn.text());
    }

    @Test
    public void wordWithWhitespacePrefix() {
        final Lexer list = this.lexer("  ls");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("ls", tokn.text());
    }

    @Test
    public void multipleTokens() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        final Token tokfirst = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokfirst.type());
        Assert.assertEquals(LexerTest.ECHO_CMD, tokfirst.text());
        final Token toksecond = list.nextToken();
        Assert.assertEquals(TokenType.WORD, toksecond.type());
        Assert.assertEquals("hello", toksecond.text());
        Assert.assertEquals(TokenType.NEWLINE, list.nextToken().type());
    }

    @Test
    public void operatorsSeparatedBySpaces() {
        final Lexer list = this.lexer("&& ||");
        Assert.assertEquals(TokenType.AND_IF, list.nextToken().type());
        Assert.assertEquals(TokenType.OR_IF, list.nextToken().type());
    }

    @Test
    public void wordThenOperator() {
        final Lexer list = this.lexer("cmd&");
        final Token tokfirst = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokfirst.type());
        Assert.assertEquals("cmd", tokfirst.text());
        Assert.assertEquals(TokenType.AMP, list.nextToken().type());
    }

    @Test
    public void operatorThenWord() {
        final Lexer list = this.lexer(">out");
        Assert.assertEquals(TokenType.GREAT, list.nextToken().type());
        final Token toksecond = list.nextToken();
        Assert.assertEquals(TokenType.WORD, toksecond.type());
        Assert.assertEquals("out", toksecond.text());
    }

    @Test
    public void consecutiveNewlines() {
        final Lexer list = this.lexer(String.format("%n%n"));
        Assert.assertEquals(TokenType.NEWLINE, list.nextToken().type());
        Assert.assertEquals(TokenType.NEWLINE, list.nextToken().type());
    }

    @Test
    public void tokenPositions() {
        final Lexer list = this.lexer("  word");
        final Token tokn = list.nextToken();
        Assert.assertEquals(1, tokn.line());
        Assert.assertEquals(2, tokn.column());
    }

    @Test
    public void eofAfterTokens() {
        final Lexer list = this.lexer("a");
        list.nextToken();
        Assert.assertEquals(TokenType.END_OF_FILE, list.nextToken().type());
    }

    @Test
    public void whitespaceSkippedBetweenTokens() {
        final Lexer list = this.lexer("a   b");
        Assert.assertEquals("a", list.nextToken().text());
        Assert.assertEquals("b", list.nextToken().text());
    }

    @Test
    public void tabIsWhitespace() {
        final Lexer list = this.lexer("\tword");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("word", tokn.text());
    }

    @Test
    public void equalsAloneIsWordNotAssignment() {
        final Lexer list = this.lexer("=");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("=", tokn.text());
    }

    @Test
    public void numberPrefixedNameIsNotAssignment() {
        Assert.assertEquals(TokenType.WORD, this.lexer("1a=b").nextToken().type());
    }

    @Test
    public void invalidVarNameCharIsNotAssignment() {
        Assert.assertEquals(TokenType.WORD, this.lexer("a@b=c").nextToken().type());
    }

    @Test
    public void wordBreaksAtLeftParen() {
        final Lexer list = this.lexer("echo(");
        final Token tokfirst = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokfirst.type());
        Assert.assertEquals(LexerTest.ECHO_CMD, tokfirst.text());
        Assert.assertEquals(TokenType.LPAREN, list.nextToken().type());
    }

    @Test
    public void wordBreaksAtRightParen() {
        final Lexer list = this.lexer("echo)");
        final Token tokfirst = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokfirst.type());
        Assert.assertEquals(LexerTest.ECHO_CMD, tokfirst.text());
        Assert.assertEquals(TokenType.RPAREN, list.nextToken().type());
    }

    @Test
    public void singleQuotedWord() {
        final Lexer list = this.lexer("'hello'");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("'hello'", tokn.text());
    }

    @Test
    public void doubleQuotedWord() {
        final Lexer list = this.lexer("\"hello\"");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("\"hello\"", tokn.text());
    }

    @Test
    public void backslashEscapesInsideWord() {
        final Lexer list = this.lexer("a\\ b");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("a\\ b", tokn.text());
    }

    @Test
    public void backslashContinuation() {
        Assert.assertEquals("ab", this.lexer(String.format("a\\%nb")).nextToken().text());
    }

    @Test
    public void mixedQuoting() {
        final Lexer list = this.lexer("a'b'c");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("a'b'c", tokn.text());
    }

    @Test
    public void doubleQuotedBackslashDollar() {
        final Lexer list = this.lexer("\"\\$\"");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("\"\\$\"", tokn.text());
    }

    @Test
    public void doubleQuotedBackslashBacktick() {
        final Lexer list = this.lexer("\"\\`\"");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("\"\\`\"", tokn.text());
    }

    @Test
    public void doubleQuotedBackslashNewline() {
        final Lexer list = this.lexer(String.format("\"a\\%nb\""));
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("\"ab\"", tokn.text());
    }

    @Test
    public void doubleQuotedBackslashBackslash() {
        final Lexer list = this.lexer("\"a\\\\b\"");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("\"a\\\\b\"", tokn.text());
    }

    @Test
    public void doubleQuotedBackslashDoublequote() {
        final Lexer list = this.lexer("\"a\\\"b\"");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("\"a\\\"b\"", tokn.text());
    }

    @Test
    public void wordBreakAtAllOperators() {
        for (final String oper : new String[]{"&", "|", ";", "<", ">", "(", ")"}) {
            final Lexer list = this.lexer(String.format("a%s", oper));
            Assert.assertEquals("a", list.nextToken().text());
            Assert.assertEquals(oper, list.nextToken().text());
        }
    }

    @Test
    public void assignmentWordEdgeCases() {
        Assert.assertEquals(TokenType.WORD, this.lexer("=value").nextToken().type());
        Assert.assertEquals(
            TokenType.ASSIGNMENT_WORD,
            this.lexer("_=value").nextToken().type()
        );
    }

    @Test
    public void backslashAtEof() {
        Assert.assertEquals("a\\", this.lexer("a\\").nextToken().text());
    }

    @Test
    public void equalsAloneIsWord() {
        Assert.assertEquals(TokenType.WORD, this.lexer("=value").nextToken().type());
    }

    @Test
    public void doubleQuotedBackslashNonSpecialKeptLiteral() {
        final Lexer list = this.lexer("\"\\a\"");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("\"\\a\"", tokn.text());
    }

    @Test
    public void ioNumberBeforeInputRedirect() {
        final Lexer list = this.lexer("2<file");
        final Token tokfirst = list.nextToken();
        Assert.assertEquals(TokenType.IO_NUMBER, tokfirst.type());
        Assert.assertEquals("2", tokfirst.text());
        Assert.assertEquals(TokenType.LESS, list.nextToken().type());
    }

    @Test
    public void digitsThenSpaceIsWordNotIoNumber() {
        final Lexer list = this.lexer("42 ");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("42", tokn.text());
    }

    @Test
    public void singleQuoteLiteralInsideDoubleQuote() {
        final Lexer list = this.lexer("\"a'b\"");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("\"a'b\"", tokn.text());
    }

    @Test
    public void backslashNewlineLineContinuation() {
        final Lexer list = this.lexer(String.format("ab\\%ncd"));
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("abcd", tokn.text());
    }

    @Test
    public void dollarParenStaysInWord() {
        final Lexer list = this.lexer("$(echo hi)");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("$(echo hi)", tokn.text());
    }

    @Test
    public void dollarDoubleParenArithStaysInWord() {
        final Lexer list = this.lexer("$((1+2*3))");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("$((1+2*3))", tokn.text());
    }

    @Test
    public void dollarParenWithNestedParensStaysInWord() {
        final Lexer list = this.lexer("$(echo $((1+2)))");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("$(echo $((1+2)))", tokn.text());
    }

    @Test
    public void dollarBraceStaysInWord() {
        final Lexer list = this.lexer("${VAR}");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("${VAR}", tokn.text());
    }

    @Test
    public void dollarBraceWithNestedBraceStaysInWord() {
        final Lexer list = this.lexer("${a:-${b}}");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("${a:-${b}}", tokn.text());
    }

    @Test
    public void backtickStaysInWord() {
        final Lexer list = this.lexer("`echo hi`");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("`echo hi`", tokn.text());
    }

    @Test
    public void backtickWithBackslashEscape() {
        final Lexer list = this.lexer("`echo \\`hi\\``");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("`echo \\`hi\\``", tokn.text());
    }

    @Test
    public void wordWithDollarParenAndTrailingText() {
        final Lexer list = this.lexer("a$(echo b)c");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("a$(echo b)c", tokn.text());
    }

    @Test
    public void wordWithBacktickAndTrailingText() {
        final Lexer list = this.lexer("a`b`c");
        final Token tokn = list.nextToken();
        Assert.assertEquals(TokenType.WORD, tokn.type());
        Assert.assertEquals("a`b`c", tokn.text());
    }

    @Test
    public void readHereDocContentSkipsLeadingNewline() {
        Assert.assertEquals(
            String.format("hello%n"),
            this.lexer(String.format("%nhello%nEOF%n")).readHereDocContent("EOF", false)
        );
    }

    @Test
    public void readHereDocContentEmptyBody() {
        Assert.assertEquals(
            "",
            this.lexer(String.format("EOF%n")).readHereDocContent("EOF", false)
        );
    }

    @Test
    public void readHereDocContentMultipleLines() {
        Assert.assertEquals(
            String.format("first%nsecond%nthird%n"),
            this.lexer(
                String.format("%nfirst%nsecond%nthird%nEOF%n")
            ).readHereDocContent("EOF", false)
        );
    }

    private Lexer lexer(final String input) {
        return new Lexer(new CharReader(input));
    }

    private void assertOperator(final String oper, final TokenType expected) {
        final Lexer list = this.lexer(oper);
        final Token tokn = list.nextToken();
        Assert.assertEquals(String.format("Operator %s", oper), expected, tokn.type());
        Assert.assertEquals(String.format("Operator text %s", oper), oper, tokn.text());
    }
}
