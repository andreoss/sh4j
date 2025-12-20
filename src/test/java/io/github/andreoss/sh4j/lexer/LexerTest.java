/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for Lexer.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts", "PMD.ExcessivePublicCount"})
public final class LexerTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    public void eofOnEmptyInput() {
        final Lexer list = this.lexer("");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.END_OF_FILE));
        assertThat(tokn.text(), equalTo(""));
    }

    @Test
    public void newline() {
        final Lexer list = this.lexer(String.format("%n"));
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.NEWLINE));
        assertThat(tokn.text(), equalTo(String.format("%n")));
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
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    public void wordWithWhitespacePrefix() {
        final Lexer list = this.lexer("  ls");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("ls"));
    }

    @Test
    public void multipleTokens() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        final Token tokfirst = list.nextToken();
        assertThat(tokfirst.type(), equalTo(TokenType.WORD));
        assertThat(tokfirst.text(), equalTo(LexerTest.ECHO_CMD));
        final Token toksecond = list.nextToken();
        assertThat(toksecond.type(), equalTo(TokenType.WORD));
        assertThat(toksecond.text(), equalTo("hello"));
        assertThat(list.nextToken().type(), equalTo(TokenType.NEWLINE));
    }

    @Test
    public void operatorsSeparatedBySpaces() {
        final Lexer list = this.lexer("&& ||");
        assertThat(list.nextToken().type(), equalTo(TokenType.AND_IF));
        assertThat(list.nextToken().type(), equalTo(TokenType.OR_IF));
    }

    @Test
    public void wordThenOperator() {
        final Lexer list = this.lexer("cmd&");
        final Token tokfirst = list.nextToken();
        assertThat(tokfirst.type(), equalTo(TokenType.WORD));
        assertThat(tokfirst.text(), equalTo("cmd"));
        assertThat(list.nextToken().type(), equalTo(TokenType.AMP));
    }

    @Test
    public void operatorThenWord() {
        final Lexer list = this.lexer(">out");
        assertThat(list.nextToken().type(), equalTo(TokenType.GREAT));
        final Token toksecond = list.nextToken();
        assertThat(toksecond.type(), equalTo(TokenType.WORD));
        assertThat(toksecond.text(), equalTo("out"));
    }

    @Test
    public void consecutiveNewlines() {
        final Lexer list = this.lexer(String.format("%n%n"));
        assertThat(list.nextToken().type(), equalTo(TokenType.NEWLINE));
        assertThat(list.nextToken().type(), equalTo(TokenType.NEWLINE));
    }

    @Test
    public void tokenPositions() {
        final Lexer list = this.lexer("  word");
        final Token tokn = list.nextToken();
        assertThat(tokn.line(), equalTo(1));
        assertThat(tokn.column(), equalTo(2));
    }

    @Test
    public void eofAfterTokens() {
        final Lexer list = this.lexer("a");
        list.nextToken();
        assertThat(list.nextToken().type(), equalTo(TokenType.END_OF_FILE));
    }

    @Test
    public void whitespaceSkippedBetweenTokens() {
        final Lexer list = this.lexer("a   b");
        assertThat(list.nextToken().text(), equalTo("a"));
        assertThat(list.nextToken().text(), equalTo("b"));
    }

    @Test
    public void tabIsWhitespace() {
        final Lexer list = this.lexer("\tword");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("word"));
    }

    @Test
    public void equalsAloneIsWordNotAssignment() {
        final Lexer list = this.lexer("=");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("="));
    }

    @Test
    public void numberPrefixedNameIsNotAssignment() {
        assertThat(this.lexer("1a=b").nextToken().type(), equalTo(TokenType.WORD));
    }

    @Test
    public void invalidVarNameCharIsNotAssignment() {
        assertThat(this.lexer("a@b=c").nextToken().type(), equalTo(TokenType.WORD));
    }

    @Test
    public void wordBreaksAtLeftParen() {
        final Lexer list = this.lexer("echo(");
        final Token tokfirst = list.nextToken();
        assertThat(tokfirst.type(), equalTo(TokenType.WORD));
        assertThat(tokfirst.text(), equalTo(LexerTest.ECHO_CMD));
        assertThat(list.nextToken().type(), equalTo(TokenType.LPAREN));
    }

    @Test
    public void wordBreaksAtRightParen() {
        final Lexer list = this.lexer("echo)");
        final Token tokfirst = list.nextToken();
        assertThat(tokfirst.type(), equalTo(TokenType.WORD));
        assertThat(tokfirst.text(), equalTo(LexerTest.ECHO_CMD));
        assertThat(list.nextToken().type(), equalTo(TokenType.RPAREN));
    }

    @Test
    public void singleQuotedWord() {
        final Lexer list = this.lexer("'hello'");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("'hello'"));
    }

    @Test
    public void doubleQuotedWord() {
        final Lexer list = this.lexer("\"hello\"");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("\"hello\""));
    }

    @Test
    public void backslashEscapesInsideWord() {
        final Lexer list = this.lexer("a\\ b");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("a\\ b"));
    }

    @Test
    public void backslashContinuation() {
        assertThat(this.lexer(String.format("a\\%nb")).nextToken().text(), equalTo("ab"));
    }

    @Test
    public void mixedQuoting() {
        final Lexer list = this.lexer("a'b'c");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("a'b'c"));
    }

    @Test
    public void doubleQuotedBackslashDollar() {
        final Lexer list = this.lexer("\"\\$\"");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("\"\\$\""));
    }

    @Test
    public void doubleQuotedBackslashBacktick() {
        final Lexer list = this.lexer("\"\\`\"");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("\"\\`\""));
    }

    @Test
    public void doubleQuotedBackslashNewline() {
        final Lexer list = this.lexer(String.format("\"a\\%nb\""));
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("\"ab\""));
    }

    @Test
    public void doubleQuotedBackslashBackslash() {
        final Lexer list = this.lexer("\"a\\\\b\"");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("\"a\\\\b\""));
    }

    @Test
    public void doubleQuotedBackslashDoublequote() {
        final Lexer list = this.lexer("\"a\\\"b\"");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("\"a\\\"b\""));
    }

    @Test
    public void wordBreakAtAllOperators() {
        for (final String oper : new String[]{"&", "|", ";", "<", ">", "(", ")"}) {
            final Lexer list = this.lexer(String.format("a%s", oper));
            assertThat(list.nextToken().text(), equalTo("a"));
            assertThat(list.nextToken().text(), equalTo(oper));
        }
    }

    @Test
    public void assignmentWordEdgeCases() {
        assertThat(this.lexer("=value").nextToken().type(), equalTo(TokenType.WORD));
        assertThat(
            this.lexer("_=value").nextToken().type(),
            equalTo(TokenType.ASSIGNMENT_WORD)
        );
    }

    @Test
    public void backslashAtEof() {
        assertThat(this.lexer("a\\").nextToken().text(), equalTo("a\\"));
    }

    @Test
    public void equalsAloneIsWord() {
        assertThat(this.lexer("=value").nextToken().type(), equalTo(TokenType.WORD));
    }

    @Test
    public void doubleQuotedBackslashNonSpecialKeptLiteral() {
        final Lexer list = this.lexer("\"\\a\"");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("\"\\a\""));
    }

    @Test
    public void ioNumberBeforeInputRedirect() {
        final Lexer list = this.lexer("2<file");
        final Token tokfirst = list.nextToken();
        assertThat(tokfirst.type(), equalTo(TokenType.IO_NUMBER));
        assertThat(tokfirst.text(), equalTo("2"));
        assertThat(list.nextToken().type(), equalTo(TokenType.LESS));
    }

    @Test
    public void digitsThenSpaceIsWordNotIoNumber() {
        final Lexer list = this.lexer("42 ");
        final Token tokn = list.nextToken();
        assertThat(tokn.type(), equalTo(TokenType.WORD));
        assertThat(tokn.text(), equalTo("42"));
    }

    @Test
    public void singleQuoteLiteralInsideDoubleQuote() {
        assertThat(
            this.lexer("\"a'b\"").nextToken().text(),
            equalTo("\"a'b\"")
        );
        assertThat(
            this.lexer("'a\"b'").nextToken().text(),
            equalTo("'a\"b'")
        );
    }

    @Test
    public void escapedSingleQuoteOutsideQuotes() {
        assertThat(
            this.lexer("a\\'b").nextToken().text(),
            equalTo("a\\'b")
        );
    }

    @Test
    public void doubleQuotedEscapes() {
        assertThat(
            this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken().text(),
            equalTo("\"a\\$b\\`c\\\"d\\\\e\"")
        );
        assertThat(
            this.lexer("\"a\\qb\"").nextToken().text(),
            equalTo("\"a\\qb\"")
        );
        assertThat(
            this.lexer(String.format("\"a\\%nb\"")).nextToken().text(),
            equalTo("\"ab\"")
        );
    }

    @Test
    public void nestedCommandAndArithmeticSubstitutions() {
        assertThat(
            this.lexer("$((1+(2)))").nextToken().text(),
            equalTo("$((1+(2)))")
        );
        assertThat(
            this.lexer("$(echo (x))").nextToken().text(),
            equalTo("$(echo (x))")
        );
        assertThat(
            this.lexer("$(abc").nextToken().text(),
            equalTo("$(abc")
        );
    }

    @Test
    public void nestedAndUnbalancedBraces() {
        assertThat(
            this.lexer("${a${b}}").nextToken().text(),
            equalTo("${a${b}}")
        );
        assertThat(
            this.lexer("${a").nextToken().text(),
            equalTo("${a")
        );
    }

    @Test
    public void backtickWithEscapedBacktick() {
        assertThat(
            this.lexer("`a\\`b`").nextToken().text(),
            equalTo("`a\\`b`")
        );
    }

    @Test
    public void assignmentWordNameValidation() {
        assertThat(
            this.lexer("A1=x").nextToken().type(),
            equalTo(TokenType.ASSIGNMENT_WORD)
        );
        assertThat(
            this.lexer("_A=x").nextToken().type(),
            equalTo(TokenType.ASSIGNMENT_WORD)
        );
        assertThat(
            this.lexer("1A=x").nextToken().type(),
            equalTo(TokenType.WORD)
        );
        assertThat(
            this.lexer("A-B=x").nextToken().type(),
            equalTo(TokenType.WORD)
        );
    }

    @Test
    public void descriptorDigitsAreRecognised() {
        final Lexer digits = this.lexer("2>file");
        assertThat(digits.nextToken().type(), equalTo(TokenType.IO_NUMBER));
        assertThat(digits.nextToken().type(), equalTo(TokenType.GREAT));
        final Lexer mixed = this.lexer("2a>file");
        assertThat(mixed.nextToken().type(), equalTo(TokenType.WORD));
        assertThat(mixed.nextToken().type(), equalTo(TokenType.GREAT));
    }

    @Test
    public void heredocStripsOnlyLeadingTabs() {
        assertThat(
            this.lexer(
                String.format("%n\tfoo%n\t  bar%n\tEOF%n")
            ).readHereDocContent("EOF", true),
            equalTo(String.format("foo%n  bar%n"))
        );
    }

    @Test
    public void heredocKeepsTabsWhenNotStripped() {
        assertThat(
            this.lexer(
                String.format("%n\tfoo%nEOF%n")
            ).readHereDocContent("EOF", false),
            equalTo(String.format("\tfoo%n"))
        );
    }

    private Lexer lexer(final String input) {
        return new Lexer(new CharReader(input));
    }

    private void assertOperator(final String oper, final TokenType expected) {
        final Lexer list = this.lexer(oper);
        final Token tokn = list.nextToken();
        assertThat(String.format("Operator %s", oper), tokn.type(), equalTo(expected));
        assertThat(String.format("Operator text %s", oper), tokn.text(), equalTo(oper));
    }
}
