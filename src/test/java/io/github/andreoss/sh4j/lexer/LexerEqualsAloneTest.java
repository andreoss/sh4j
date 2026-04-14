/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Lexer}: equals alone.
 *
 * @since 1.0
 */
final class LexerEqualsAloneTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    void equalsAloneIsWordNotAssignmentSecond() {
        final Token tokn = this.lexer("=").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("="));
    }

    @Test
    void equalsAloneIsWord() {
        MatcherAssert.assertThat(
            this.lexer("=value").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
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
    void heredocStripsOnlyLeadingTabs() {
        MatcherAssert.assertThat(
            this.lexer(String.format("%n\tfoo%n\t  bar%n\tEOF%n")).readHereDocContent("EOF", true),
            CoreMatchers.equalTo(String.format("foo%n  bar%n"))
        );
    }

    @Test
    void heredocKeepsTabsWhenNotStripped() {
        MatcherAssert.assertThat(
            this.lexer(String.format("%n\tfoo%nEOF%n")).readHereDocContent("EOF", false),
            CoreMatchers.equalTo(String.format("\tfoo%n"))
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
    void ioNumberBeforeInputRedirectFirst() {
        MatcherAssert.assertThat(
            this.lexer("2<file").nextToken().type(),
            CoreMatchers.equalTo(TokenType.IO_NUMBER)
        );
    }

    @Test
    void ioNumberBeforeInputRedirectSecond() {
        final Token tokfirst = this.lexer("2<file").nextToken();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo("2"));
    }

    @Test
    void ioNumberBeforeInputRedirectThird() {
        final Lexer list = this.lexer("2<file");
        list.nextToken();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        CoreMatchers.equalTo("2");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.LESS));
    }

    @Test
    void mixedQuotingFirst() {
        MatcherAssert.assertThat(
            this.lexer("a'b'c").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void mixedQuotingSecond() {
        final Token tokn = this.lexer("a'b'c").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("a'b'c"));
    }

    @Test
    void multiCharOperatorsFirst() {
        this.assertOperator("&&", TokenType.AND_IF);
    }

    @Test
    void multiCharOperatorsSecond() {
        this.assertOperator("||", TokenType.OR_IF);
    }

    @Test
    void multiCharOperatorsThird() {
        this.assertOperator(";;", TokenType.DSEMI);
    }

    @Test
    void multiCharOperatorsFourth() {
        this.assertOperator("<<", TokenType.DLESS);
    }

    @Test
    void multiCharOperatorsFifth() {
        this.assertOperator(">>", TokenType.DGREATER);
    }

    @Test
    void multiCharOperatorsSixth() {
        this.assertOperator("<&", TokenType.LESSAND);
    }

    @Test
    void multiCharOperatorsSeventh() {
        this.assertOperator(">&", TokenType.GREATAND);
    }

    @Test
    void multiCharOperatorsEighth() {
        this.assertOperator("<>", TokenType.LESSGREATER);
    }

    @Test
    void multiCharOperatorsNinth() {
        this.assertOperator("<<-", TokenType.DLESSDASH);
    }

    @Test
    void multiCharOperatorsTenth() {
        this.assertOperator(">|", TokenType.CLOBBER);
    }

    @Test
    void multipleTokensFirst() {
        MatcherAssert.assertThat(
            this.lexer(String.format("echo hello%n")).nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void multipleTokensSecond() {
        final Token tokfirst = this.lexer(String.format("echo hello%n")).nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(
            tokfirst.text(),
            CoreMatchers.equalTo(LexerEqualsAloneTest.ECHO_CMD)
        );
    }

    @Test
    void multipleTokensThird() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo(LexerEqualsAloneTest.ECHO_CMD);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void multipleTokensFourth() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo(LexerEqualsAloneTest.ECHO_CMD);
        final Token toksecond = list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(toksecond.text(), CoreMatchers.equalTo("hello"));
    }

    @Test
    void multipleTokensFifth() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo(LexerEqualsAloneTest.ECHO_CMD);
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo("hello");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
    }

    @Test
    void nestedCommandAndArithmeticSubstitutionsFirst() {
        MatcherAssert.assertThat(
            this.lexer("$((1+(2)))").nextToken().text(),
            CoreMatchers.equalTo("$((1+(2)))")
        );
    }

    @Test
    void nestedCommandAndArithmeticSubstitutionsSecond() {
        this.lexer("$((1+(2)))").nextToken();
        CoreMatchers.equalTo("$((1+(2)))");
        MatcherAssert.assertThat(
            this.lexer("$(echo (x))").nextToken().text(),
            CoreMatchers.equalTo("$(echo (x))")
        );
    }

    @Test
    void nestedCommandAndArithmeticSubstitutionsThird() {
        this.lexer("$((1+(2)))").nextToken();
        CoreMatchers.equalTo("$((1+(2)))");
        this.lexer("$(echo (x))").nextToken();
        CoreMatchers.equalTo("$(echo (x))");
        MatcherAssert.assertThat(
            this.lexer("$(abc").nextToken().text(),
            CoreMatchers.equalTo("$(abc")
        );
    }

    @Test
    void nestedAndUnbalancedBracesFirst() {
        MatcherAssert.assertThat(
            this.lexer("${a${b}}").nextToken().text(),
            CoreMatchers.equalTo("${a${b}}")
        );
    }

    @Test
    void nestedAndUnbalancedBracesSecond() {
        this.lexer("${a${b}}").nextToken();
        CoreMatchers.equalTo("${a${b}}");
        MatcherAssert.assertThat(this.lexer("${a").nextToken().text(), CoreMatchers.equalTo("${a"));
    }

    @Test
    void newlineFirst() {
        MatcherAssert.assertThat(
            this.lexer(String.format("%n")).nextToken().type(),
            CoreMatchers.equalTo(TokenType.NEWLINE)
        );
    }

    @Test
    void newlineSecond() {
        final Token tokn = this.lexer(String.format("%n")).nextToken();
        CoreMatchers.equalTo(TokenType.NEWLINE);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(String.format("%n")));
    }

    @Test
    void numberPrefixedNameIsNotAssignment() {
        MatcherAssert.assertThat(
            this.lexer("1a=b").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void operatorThenWordFirst() {
        MatcherAssert.assertThat(
            this.lexer(">out").nextToken().type(),
            CoreMatchers.equalTo(TokenType.GREAT)
        );
    }

    @Test
    void operatorThenWordSecond() {
        final Lexer list = this.lexer(">out");
        list.nextToken();
        CoreMatchers.equalTo(TokenType.GREAT);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void operatorThenWordThird() {
        final Lexer list = this.lexer(">out");
        list.nextToken();
        CoreMatchers.equalTo(TokenType.GREAT);
        final Token toksecond = list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(toksecond.text(), CoreMatchers.equalTo("out"));
    }

    @Test
    void operatorsSeparatedBySpacesFirst() {
        MatcherAssert.assertThat(
            this.lexer("&& ||").nextToken().type(),
            CoreMatchers.equalTo(TokenType.AND_IF)
        );
    }

    @Test
    void operatorsSeparatedBySpacesSecond() {
        final Lexer list = this.lexer("&& ||");
        list.nextToken();
        CoreMatchers.equalTo(TokenType.AND_IF);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.OR_IF));
    }

    @Test
    void simpleWordFirst() {
        MatcherAssert.assertThat(
            this.lexer(LexerEqualsAloneTest.ECHO_CMD).nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    private Lexer lexer(final String input) {
        return new Lexer(new CharReader(input));
    }

    private void assertOperator(final String oper, final TokenType expected) {
        final Token tokn = this.lexer(oper).nextToken();
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
