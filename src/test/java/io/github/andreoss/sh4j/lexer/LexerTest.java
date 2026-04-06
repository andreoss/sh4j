/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for Lexer.
 *
 * @since 1.0
 */
final class LexerTest {

    /** ECHO_CMD constant. */
    private static final String ECHO_CMD = "echo";

    @Test
    void eofOnEmptyInputCheck1() {
        final Token tokn = this.lexer("").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.END_OF_FILE));
    }

    @Test
    void eofOnEmptyInputCheck2() {
        final Token tokn = this.lexer("").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.END_OF_FILE);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(""));
    }

    @Test
    void newlineCheck1() {
        final Token tokn = this.lexer(String.format("%n")).nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.NEWLINE));
    }

    @Test
    void newlineCheck2() {
        final Token tokn = this.lexer(String.format("%n")).nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.NEWLINE);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(String.format("%n")));
    }

    @Test
    void singleCharOperatorsCheck1() {
        this.assertOperator("|", TokenType.PIPE);
    }

    @Test
    void singleCharOperatorsCheck2() {
        this.assertOperator("&", TokenType.AMP);
    }

    @Test
    void singleCharOperatorsCheck3() {
        this.assertOperator(";", TokenType.SEMI);
    }

    @Test
    void singleCharOperatorsCheck4() {
        this.assertOperator("<", TokenType.LESS);
    }

    @Test
    void singleCharOperatorsCheck5() {
        this.assertOperator(">", TokenType.GREAT);
    }

    @Test
    void singleCharOperatorsCheck6() {
        this.assertOperator("(", TokenType.LPAREN);
    }

    @Test
    void singleCharOperatorsCheck7() {
        this.assertOperator(")", TokenType.RPAREN);
    }

    @Test
    void multiCharOperatorsCheck1() {
        this.assertOperator("&&", TokenType.AND_IF);
    }

    @Test
    void multiCharOperatorsCheck2() {
        this.assertOperator("||", TokenType.OR_IF);
    }

    @Test
    void multiCharOperatorsCheck3() {
        this.assertOperator(";;", TokenType.DSEMI);
    }

    @Test
    void multiCharOperatorsCheck4() {
        this.assertOperator("<<", TokenType.DLESS);
    }

    @Test
    void multiCharOperatorsCheck5() {
        this.assertOperator(">>", TokenType.DGREATER);
    }

    @Test
    void multiCharOperatorsCheck6() {
        this.assertOperator("<&", TokenType.LESSAND);
    }

    @Test
    void multiCharOperatorsCheck7() {
        this.assertOperator(">&", TokenType.GREATAND);
    }

    @Test
    void multiCharOperatorsCheck8() {
        this.assertOperator("<>", TokenType.LESSGREATER);
    }

    @Test
    void multiCharOperatorsCheck9() {
        this.assertOperator("<<-", TokenType.DLESSDASH);
    }

    @Test
    void multiCharOperatorsCheck10() {
        this.assertOperator(">|", TokenType.CLOBBER);
    }

    @Test
    void simpleWordCheck1() {
        final Token tokn = this.lexer(LexerTest.ECHO_CMD).nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void simpleWordCheck2() {
        final Token tokn = this.lexer(LexerTest.ECHO_CMD).nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    void wordWithWhitespacePrefixCheck1() {
        final Token tokn = this.lexer("  ls").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void wordWithWhitespacePrefixCheck2() {
        final Token tokn = this.lexer("  ls").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("ls"));
    }

    @Test
    void multipleTokensCheck1() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void multipleTokensCheck2() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    void multipleTokensCheck3() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokfirst.text();
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        final Token toksecond = list.nextToken();
        MatcherAssert.assertThat(toksecond.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void multipleTokensCheck4() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokfirst.text();
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        final Token toksecond = list.nextToken();
        toksecond.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(toksecond.text(), CoreMatchers.equalTo("hello"));
    }

    @Test
    void multipleTokensCheck5() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokfirst.text();
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        final Token toksecond = list.nextToken();
        toksecond.type();
        CoreMatchers.equalTo(TokenType.WORD);
        toksecond.text();
        CoreMatchers.equalTo("hello");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
    }

    @Test
    void operatorsSeparatedBySpacesCheck1() {
        final Lexer list = this.lexer("&& ||");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.AND_IF));
    }

    @Test
    void operatorsSeparatedBySpacesCheck2() {
        final Lexer list = this.lexer("&& ||");
        list.nextToken().type();
        CoreMatchers.equalTo(TokenType.AND_IF);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.OR_IF));
    }

    @Test
    void wordThenOperatorCheck1() {
        final Lexer list = this.lexer("cmd&");
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void wordThenOperatorCheck2() {
        final Lexer list = this.lexer("cmd&");
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo("cmd"));
    }

    @Test
    void wordThenOperatorCheck3() {
        final Lexer list = this.lexer("cmd&");
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokfirst.text();
        CoreMatchers.equalTo("cmd");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.AMP));
    }

    @Test
    void operatorThenWordCheck1() {
        final Lexer list = this.lexer(">out");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.GREAT));
    }

    @Test
    void operatorThenWordCheck2() {
        final Lexer list = this.lexer(">out");
        list.nextToken().type();
        CoreMatchers.equalTo(TokenType.GREAT);
        final Token toksecond = list.nextToken();
        MatcherAssert.assertThat(toksecond.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void operatorThenWordCheck3() {
        final Lexer list = this.lexer(">out");
        list.nextToken().type();
        CoreMatchers.equalTo(TokenType.GREAT);
        final Token toksecond = list.nextToken();
        toksecond.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(toksecond.text(), CoreMatchers.equalTo("out"));
    }

    @Test
    void consecutiveNewlinesCheck1() {
        final Lexer list = this.lexer(String.format("%n%n"));
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
    }

    @Test
    void consecutiveNewlinesCheck2() {
        final Lexer list = this.lexer(String.format("%n%n"));
        list.nextToken().type();
        CoreMatchers.equalTo(TokenType.NEWLINE);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
    }

    @Test
    void tokenPositionsCheck1() {
        final Token tokn = this.lexer("  word").nextToken();
        MatcherAssert.assertThat(tokn.line(), CoreMatchers.equalTo(1));
    }

    @Test
    void tokenPositionsCheck2() {
        final Token tokn = this.lexer("  word").nextToken();
        tokn.line();
        CoreMatchers.equalTo(1);
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
    void whitespaceSkippedBetweenTokensCheck1() {
        final Lexer list = this.lexer("a   b");
        MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo("a"));
    }

    @Test
    void whitespaceSkippedBetweenTokensCheck2() {
        final Lexer list = this.lexer("a   b");
        list.nextToken().text();
        CoreMatchers.equalTo("a");
        MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo("b"));
    }

    @Test
    void tabIsWhitespaceCheck1() {
        final Token tokn = this.lexer("\tword").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void tabIsWhitespaceCheck2() {
        final Token tokn = this.lexer("\tword").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("word"));
    }

    @Test
    void equalsAloneIsWordNotAssignmentCheck1() {
        final Token tokn = this.lexer("=").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void equalsAloneIsWordNotAssignmentCheck2() {
        final Token tokn = this.lexer("=").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
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
    void wordBreaksAtLeftParenCheck1() {
        final Lexer list = this.lexer("echo(");
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void wordBreaksAtLeftParenCheck2() {
        final Lexer list = this.lexer("echo(");
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    void wordBreaksAtLeftParenCheck3() {
        final Lexer list = this.lexer("echo(");
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokfirst.text();
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.LPAREN));
    }

    @Test
    void wordBreaksAtRightParenCheck1() {
        final Lexer list = this.lexer("echo)");
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void wordBreaksAtRightParenCheck2() {
        final Lexer list = this.lexer("echo)");
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    void wordBreaksAtRightParenCheck3() {
        final Lexer list = this.lexer("echo)");
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.WORD);
        tokfirst.text();
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.RPAREN));
    }

    @Test
    void singleQuotedWordCheck1() {
        final Token tokn = this.lexer("'hello'").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void singleQuotedWordCheck2() {
        final Token tokn = this.lexer("'hello'").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("'hello'"));
    }

    @Test
    void doubleQuotedWordCheck1() {
        final Token tokn = this.lexer("\"hello\"").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void doubleQuotedWordCheck2() {
        final Token tokn = this.lexer("\"hello\"").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"hello\""));
    }

    @Test
    void backslashEscapesInsideWordCheck1() {
        final Token tokn = this.lexer("a\\ b").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void backslashEscapesInsideWordCheck2() {
        final Token tokn = this.lexer("a\\ b").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
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
    void mixedQuotingCheck1() {
        final Token tokn = this.lexer("a'b'c").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void mixedQuotingCheck2() {
        final Token tokn = this.lexer("a'b'c").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("a'b'c"));
    }

    @Test
    void doubleQuotedBackslashDollarCheck1() {
        final Token tokn = this.lexer("\"\\$\"").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void doubleQuotedBackslashDollarCheck2() {
        final Token tokn = this.lexer("\"\\$\"").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"\\$\""));
    }

    @Test
    void doubleQuotedBackslashBacktickCheck1() {
        final Token tokn = this.lexer("\"\\`\"").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void doubleQuotedBackslashBacktickCheck2() {
        final Token tokn = this.lexer("\"\\`\"").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"\\`\""));
    }

    @Test
    void doubleQuotedBackslashNewlineCheck1() {
        final Token tokn = this.lexer(String.format("\"a\\%nb\"")).nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void doubleQuotedBackslashNewlineCheck2() {
        final Token tokn = this.lexer(String.format("\"a\\%nb\"")).nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"ab\""));
    }

    @Test
    void doubleQuotedBackslashBackslashCheck1() {
        final Token tokn = this.lexer("\"a\\\\b\"").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void doubleQuotedBackslashBackslashCheck2() {
        final Token tokn = this.lexer("\"a\\\\b\"").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"a\\\\b\""));
    }

    @Test
    void doubleQuotedBackslashDoublequoteCheck1() {
        final Token tokn = this.lexer("\"a\\\"b\"").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void doubleQuotedBackslashDoublequoteCheck2() {
        final Token tokn = this.lexer("\"a\\\"b\"").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"a\\\"b\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"&", "|", ";", "<", ">", "(", ")"})
    void wordBreakKeepsWordBeforeOperator(final String oper) {
        MatcherAssert.assertThat(
            this.lexer(String.format("a%s", oper)).nextToken().text(),
            CoreMatchers.equalTo("a")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"&", "|", ";", "<", ">", "(", ")"})
    void wordBreakEmitsOperator(final String oper) {
        final Lexer list = this.lexer(String.format("a%s", oper));
        list.nextToken();
        MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo(oper));
    }

    @Test
    void assignmentWordEdgeCasesCheck1() {
        MatcherAssert.assertThat(
            this.lexer("=value").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void assignmentWordEdgeCasesCheck2() {
        this.lexer("=value").nextToken().type();
        CoreMatchers.equalTo(TokenType.WORD);
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
    void doubleQuotedBackslashNonSpecialKeptLiteralCheck1() {
        final Token tokn = this.lexer("\"\\a\"").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void doubleQuotedBackslashNonSpecialKeptLiteralCheck2() {
        final Token tokn = this.lexer("\"\\a\"").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"\\a\""));
    }

    @Test
    void ioNumberBeforeInputRedirectCheck1() {
        final Lexer list = this.lexer("2<file");
        final Token tokfirst = list.nextToken();
        MatcherAssert.assertThat(tokfirst.type(), CoreMatchers.equalTo(TokenType.IO_NUMBER));
    }

    @Test
    void ioNumberBeforeInputRedirectCheck2() {
        final Lexer list = this.lexer("2<file");
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo("2"));
    }

    @Test
    void ioNumberBeforeInputRedirectCheck3() {
        final Lexer list = this.lexer("2<file");
        final Token tokfirst = list.nextToken();
        tokfirst.type();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        tokfirst.text();
        CoreMatchers.equalTo("2");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.LESS));
    }

    @Test
    void digitsThenSpaceIsWordNotIoNumberCheck1() {
        final Token tokn = this.lexer("42 ").nextToken();
        MatcherAssert.assertThat(tokn.type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void digitsThenSpaceIsWordNotIoNumberCheck2() {
        final Token tokn = this.lexer("42 ").nextToken();
        tokn.type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("42"));
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuoteCheck1() {
        MatcherAssert.assertThat(
            this.lexer("\"a'b\"").nextToken().text(),
            CoreMatchers.equalTo("\"a'b\"")
        );
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuoteCheck2() {
        this.lexer("\"a'b\"").nextToken().text();
        CoreMatchers.equalTo("\"a'b\"");
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
    void doubleQuotedEscapesCheck1() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\$b\\`c\\\"d\\\\e\"")
        );
    }

    @Test
    void doubleQuotedEscapesCheck2() {
        this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken().text();
        CoreMatchers.equalTo("\"a\\$b\\`c\\\"d\\\\e\"");
        MatcherAssert.assertThat(
            this.lexer("\"a\\qb\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\qb\"")
        );
    }

    @Test
    void doubleQuotedEscapesCheck3() {
        this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken().text();
        CoreMatchers.equalTo("\"a\\$b\\`c\\\"d\\\\e\"");
        this.lexer("\"a\\qb\"").nextToken().text();
        CoreMatchers.equalTo("\"a\\qb\"");
        MatcherAssert.assertThat(
            this.lexer(String.format("\"a\\%nb\"")).nextToken().text(),
            CoreMatchers.equalTo("\"ab\"")
        );
    }

    @Test
    void nestedCommandAndArithmeticSubstitutionsCheck1() {
        MatcherAssert.assertThat(
            this.lexer("$((1+(2)))").nextToken().text(),
            CoreMatchers.equalTo("$((1+(2)))")
        );
    }

    @Test
    void nestedCommandAndArithmeticSubstitutionsCheck2() {
        this.lexer("$((1+(2)))").nextToken().text();
        CoreMatchers.equalTo("$((1+(2)))");
        MatcherAssert.assertThat(
            this.lexer("$(echo (x))").nextToken().text(),
            CoreMatchers.equalTo("$(echo (x))")
        );
    }

    @Test
    void nestedCommandAndArithmeticSubstitutionsCheck3() {
        this.lexer("$((1+(2)))").nextToken().text();
        CoreMatchers.equalTo("$((1+(2)))");
        this.lexer("$(echo (x))").nextToken().text();
        CoreMatchers.equalTo("$(echo (x))");
        MatcherAssert.assertThat(
            this.lexer("$(abc").nextToken().text(),
            CoreMatchers.equalTo("$(abc")
        );
    }

    @Test
    void nestedAndUnbalancedBracesCheck1() {
        MatcherAssert.assertThat(
            this.lexer("${a${b}}").nextToken().text(),
            CoreMatchers.equalTo("${a${b}}")
        );
    }

    @Test
    void nestedAndUnbalancedBracesCheck2() {
        this.lexer("${a${b}}").nextToken().text();
        CoreMatchers.equalTo("${a${b}}");
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
    void assignmentWordNameValidationCheck1() {
        MatcherAssert.assertThat(
            this.lexer("A1=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
    }

    @Test
    void assignmentWordNameValidationCheck2() {
        this.lexer("A1=x").nextToken().type();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        MatcherAssert.assertThat(
            this.lexer("_A=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
    }

    @Test
    void assignmentWordNameValidationCheck3() {
        this.lexer("A1=x").nextToken().type();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        this.lexer("_A=x").nextToken().type();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        MatcherAssert.assertThat(
            this.lexer("1A=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void assignmentWordNameValidationCheck4() {
        this.lexer("A1=x").nextToken().type();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        this.lexer("_A=x").nextToken().type();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        this.lexer("1A=x").nextToken().type();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(
            this.lexer("A-B=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void descriptorDigitsAreRecognisedCheck1() {
        final Lexer digits = this.lexer("2>file");
        MatcherAssert.assertThat(
            digits.nextToken().type(),
            CoreMatchers.equalTo(TokenType.IO_NUMBER)
        );
    }

    @Test
    void descriptorDigitsAreRecognisedCheck2() {
        final Lexer digits = this.lexer("2>file");
        digits.nextToken().type();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        MatcherAssert.assertThat(digits.nextToken().type(), CoreMatchers.equalTo(TokenType.GREAT));
    }

    @Test
    void descriptorDigitsAreRecognisedCheck3() {
        final Lexer digits = this.lexer("2>file");
        digits.nextToken().type();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        digits.nextToken().type();
        CoreMatchers.equalTo(TokenType.GREAT);
        final Lexer mixed = this.lexer("2a>file");
        MatcherAssert.assertThat(mixed.nextToken().type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void descriptorDigitsAreRecognisedCheck4() {
        final Lexer digits = this.lexer("2>file");
        digits.nextToken().type();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        digits.nextToken().type();
        CoreMatchers.equalTo(TokenType.GREAT);
        final Lexer mixed = this.lexer("2a>file");
        mixed.nextToken().type();
        CoreMatchers.equalTo(TokenType.WORD);
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
