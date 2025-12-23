/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for Lexer.
 *
 * @since 1.0
 */
final class LexerTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    void eofOnEmptyInput() {
        final Lexer list = this.lexer("");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.END_OF_FILE));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(""));
    }

    @Test
    void newline() {
        final Lexer list = this.lexer(String.format("%n"));
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.NEWLINE));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(String.format("%n")));
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
        final Lexer list = this.lexer(LexerTest.ECHO_CMD);
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    void wordWithWhitespacePrefix() {
        final Lexer list = this.lexer("  ls");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("ls"));
    }

    @Test
    void multipleTokens() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
        final Token toksecond = list.nextToken();
        MatcherAssert.assertThat(toksecond.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(toksecond.text(), CoreMatchers.equalTo("hello"));
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
    }

    @Test
    void operatorsSeparatedBySpaces() {
        final Lexer list = this.lexer("&& ||");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.AND_IF));
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.OR_IF));
    }

    @Test
    void wordThenOperator() {
        final Lexer list = this.lexer("cmd&");
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo("cmd"));
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.AMP));
    }

    @Test
    void operatorThenWord() {
        final Lexer list = this.lexer(">out");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.GREAT));
        final Token toksecond = list.nextToken();
        MatcherAssert.assertThat(toksecond.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(toksecond.text(), CoreMatchers.equalTo("out"));
    }

    @Test
    void consecutiveNewlines() {
        final Lexer list = this.lexer(String.format("%n%n"));
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
    }

    @Test
    void tokenPositions() {
        final Lexer list = this.lexer("  word");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.line(), CoreMatchers.equalTo(1));
        MatcherAssert.assertThat(tokn.column(), CoreMatchers.equalTo(2));
    }

    @Test
    void eofAfterTokens() {
        final Lexer list = this.lexer("a");
        list.nextToken();
        MatcherAssert.assertThat(
            list.nextToken().type(),
            CoreMatchers.equalTo(TokenType.END_OF_FILE)
        );
    }

    @Test
    void whitespaceSkippedBetweenTokens() {
        final Lexer list = this.lexer("a   b");
        MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo("a"));
        MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo("b"));
    }

    @Test
    void tabIsWhitespace() {
        final Lexer list = this.lexer("\tword");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("word"));
    }

    @Test
    void equalsAloneIsWordNotAssignment() {
        final Lexer list = this.lexer("=");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("="));
    }

    @Test
    void numberPrefixedNameIsNotAssignment() {
        MatcherAssert.assertThat(
            this.lexer("1a=b").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void invalidVarNameCharIsNotAssignment() {
        MatcherAssert.assertThat(
            this.lexer("a@b=c").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void wordBreaksAtLeftParen() {
        final Lexer list = this.lexer("echo(");
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.LPAREN));
    }

    @Test
    void wordBreaksAtRightParen() {
        final Lexer list = this.lexer("echo)");
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.RPAREN));
    }

    @Test
    void singleQuotedWord() {
        final Lexer list = this.lexer("'hello'");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("'hello'"));
    }

    @Test
    void doubleQuotedWord() {
        final Lexer list = this.lexer("\"hello\"");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"hello\""));
    }

    @Test
    void backslashEscapesInsideWord() {
        final Lexer list = this.lexer("a\\ b");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("a\\ b"));
    }

    @Test
    void backslashContinuation() {
        MatcherAssert.assertThat(
            this.lexer(String.format("a\\%nb")).nextToken().text(),
            CoreMatchers.equalTo("ab")
        );
    }

    @Test
    void mixedQuoting() {
        final Lexer list = this.lexer("a'b'c");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("a'b'c"));
    }

    @Test
    void doubleQuotedBackslashDollar() {
        final Lexer list = this.lexer("\"\\$\"");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"\\$\""));
    }

    @Test
    void doubleQuotedBackslashBacktick() {
        final Lexer list = this.lexer("\"\\`\"");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"\\`\""));
    }

    @Test
    void doubleQuotedBackslashNewline() {
        final Lexer list = this.lexer(String.format("\"a\\%nb\""));
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"ab\""));
    }

    @Test
    void doubleQuotedBackslashBackslash() {
        final Lexer list = this.lexer("\"a\\\\b\"");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"a\\\\b\""));
    }

    @Test
    void doubleQuotedBackslashDoublequote() {
        final Lexer list = this.lexer("\"a\\\"b\"");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"a\\\"b\""));
    }

    @Test
    void wordBreakAtAllOperators() {
        for (final String oper : new String[]{"&", "|", ";", "<", ">", "(", ")"}) {
            final Lexer list = this.lexer(String.format("a%s", oper));
            MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo("a"));
            MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo(oper));
        }
    }

    @Test
    void assignmentWordEdgeCases() {
        MatcherAssert.assertThat(
            this.lexer("=value").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
        MatcherAssert.assertThat(
            this.lexer("_=value").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
    }

    @Test
    void backslashAtEof() {
        MatcherAssert.assertThat(this.lexer("a\\").nextToken().text(), CoreMatchers.equalTo("a\\"));
    }

    @Test
    void equalsAloneIsWord() {
        MatcherAssert.assertThat(
            this.lexer("=value").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashNonSpecialKeptLiteral() {
        final Lexer list = this.lexer("\"\\a\"");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"\\a\""));
    }

    @Test
    void ioNumberBeforeInputRedirect() {
        final Lexer list = this.lexer("2<file");
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.IO_NUMBER));
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo("2"));
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.LESS));
    }

    @Test
    void digitsThenSpaceIsWordNotIoNumber() {
        final Lexer list = this.lexer("42 ");
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("42"));
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuote() {
        MatcherAssert.assertThat(
            this.lexer("\"a'b\"").nextToken().text(),
            CoreMatchers.equalTo("\"a'b\"")
        );
        MatcherAssert.assertThat(
            this.lexer("'a\"b'").nextToken().text(),
            CoreMatchers.equalTo("'a\"b'")
        );
    }

    @Test
    void escapedSingleQuoteOutsideQuotes() {
        MatcherAssert.assertThat(
            this.lexer("a\\'b").nextToken().text(),
            CoreMatchers.equalTo("a\\'b")
        );
    }

    @Test
    void doubleQuotedEscapes() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\$b\\`c\\\"d\\\\e\"")
        );
        MatcherAssert.assertThat(
            this.lexer("\"a\\qb\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\qb\"")
        );
        MatcherAssert.assertThat(
            this.lexer(String.format("\"a\\%nb\"")).nextToken().text(),
            CoreMatchers.equalTo("\"ab\"")
        );
    }

    @Test
    void nestedCommandAndArithmeticSubstitutions() {
        MatcherAssert.assertThat(
            this.lexer("$((1+(2)))").nextToken().text(),
            CoreMatchers.equalTo("$((1+(2)))")
        );
        MatcherAssert.assertThat(
            this.lexer("$(echo (x))").nextToken().text(),
            CoreMatchers.equalTo("$(echo (x))")
        );
        MatcherAssert.assertThat(
            this.lexer("$(abc").nextToken().text(),
            CoreMatchers.equalTo("$(abc")
        );
    }

    @Test
    void nestedAndUnbalancedBraces() {
        MatcherAssert.assertThat(
            this.lexer("${a${b}}").nextToken().text(),
            CoreMatchers.equalTo("${a${b}}")
        );
        MatcherAssert.assertThat(
            this.lexer("${a").nextToken().text(),
            CoreMatchers.equalTo("${a")
        );
    }

    @Test
    void backtickWithEscapedBacktick() {
        MatcherAssert.assertThat(
            this.lexer("`a\\`b`").nextToken().text(),
            CoreMatchers.equalTo("`a\\`b`")
        );
    }

    @Test
    void assignmentWordNameValidation() {
        MatcherAssert.assertThat(
            this.lexer("A1=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
        MatcherAssert.assertThat(
            this.lexer("_A=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
        MatcherAssert.assertThat(
            this.lexer("1A=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
        MatcherAssert.assertThat(
            this.lexer("A-B=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void descriptorDigitsAreRecognised() {
        final Lexer digits = this.lexer("2>file");
        MatcherAssert.assertThat(
            digits.nextToken().type(),
            CoreMatchers.equalTo(TokenType.IO_NUMBER)
        );
        MatcherAssert.assertThat(digits.nextToken().type(), CoreMatchers.equalTo(TokenType.GREAT));
        final Lexer mixed = this.lexer("2a>file");
        MatcherAssert.assertThat(mixed.nextToken().type(), CoreMatchers.equalTo(TokenType.WORD));
        MatcherAssert.assertThat(mixed.nextToken().type(), CoreMatchers.equalTo(TokenType.GREAT));
    }

    @Test
    void heredocStripsOnlyLeadingTabs() {
        MatcherAssert.assertThat(
            this.lexer(
                String.format("%n\tfoo%n\t  bar%n\tEOF%n")
            ).readHereDocContent("EOF", true),
            CoreMatchers.equalTo(String.format("foo%n  bar%n"))
        );
    }

    @Test
    void heredocKeepsTabsWhenNotStripped() {
        MatcherAssert.assertThat(
            this.lexer(
                String.format("%n\tfoo%nEOF%n")
            ).readHereDocContent("EOF", false),
            CoreMatchers.equalTo(String.format("\tfoo%n"))
        );
    }

    private Lexer lexer(final String input) {
        return new Lexer(new CharReader(input));
    }

    private void assertOperator(final String oper, final TokenType expected) {
        final Lexer list = this.lexer(oper);
        final Token tokn = list.nextToken();
        MatcherAssert.assertThat(
            String.format("Operator %s", oper),
            tokn.type(),
            CoreMatchers.equalTo(expected)
        );
        MatcherAssert.assertThat(
            String.format("Operator text %s", oper),
            tokn.text(),
            CoreMatchers.equalTo(oper)
        );
    }
}
