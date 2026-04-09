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
    void eofOnEmptyInputFirst() {
        MatcherAssert.assertThat(
            this.lexer("").nextToken().type(),
            CoreMatchers.equalTo(TokenType.END_OF_FILE)
        );
    }

    @Test
    void eofOnEmptyInputSecond() {
        final Token tokn = this.lexer("").nextToken();
        CoreMatchers.equalTo(TokenType.END_OF_FILE);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(""));
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
    void singleCharOperatorsFirst() {
        this.assertOperator("|", TokenType.PIPE);
    }

    @Test
    void singleCharOperatorsSecond() {
        this.assertOperator("&", TokenType.AMP);
    }

    @Test
    void singleCharOperatorsThird() {
        this.assertOperator(";", TokenType.SEMI);
    }

    @Test
    void singleCharOperatorsFourth() {
        this.assertOperator("<", TokenType.LESS);
    }

    @Test
    void singleCharOperatorsFifth() {
        this.assertOperator(">", TokenType.GREAT);
    }

    @Test
    void singleCharOperatorsSixth() {
        this.assertOperator("(", TokenType.LPAREN);
    }

    @Test
    void singleCharOperatorsSeventh() {
        this.assertOperator(")", TokenType.RPAREN);
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
    void simpleWordFirst() {
        MatcherAssert.assertThat(
            this.lexer(LexerTest.ECHO_CMD).nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void simpleWordSecond() {
        final Token tokn = this.lexer(LexerTest.ECHO_CMD).nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    void wordWithWhitespacePrefixFirst() {
        MatcherAssert.assertThat(
            this.lexer("  ls").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void wordWithWhitespacePrefixSecond() {
        final Token tokn = this.lexer("  ls").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("ls"));
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
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    void multipleTokensThird() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.WORD));
    }

    @Test
    void multipleTokensFourth() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        final Token toksecond = list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(toksecond.text(), CoreMatchers.equalTo("hello"));
    }

    @Test
    void multipleTokensFifth() {
        final Lexer list = this.lexer(String.format("echo hello%n"));
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo("hello");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
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
    void wordThenOperatorFirst() {
        MatcherAssert.assertThat(
            this.lexer("cmd&").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void wordThenOperatorSecond() {
        final Token tokfirst = this.lexer("cmd&").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo("cmd"));
    }

    @Test
    void wordThenOperatorThird() {
        final Lexer list = this.lexer("cmd&");
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo("cmd");
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.AMP));
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
    void consecutiveNewlinesFirst() {
        MatcherAssert.assertThat(
            this.lexer(String.format("%n%n")).nextToken().type(),
            CoreMatchers.equalTo(TokenType.NEWLINE)
        );
    }

    @Test
    void consecutiveNewlinesSecond() {
        final Lexer list = this.lexer(String.format("%n%n"));
        list.nextToken();
        CoreMatchers.equalTo(TokenType.NEWLINE);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.NEWLINE));
    }

    @Test
    void tokenPositionsFirst() {
        MatcherAssert.assertThat(this.lexer("  word").nextToken().line(), CoreMatchers.equalTo(1));
    }

    @Test
    void tokenPositionsSecond() {
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
    void whitespaceSkippedBetweenTokensFirst() {
        MatcherAssert.assertThat(this.lexer("a   b").nextToken().text(), CoreMatchers.equalTo("a"));
    }

    @Test
    void whitespaceSkippedBetweenTokensSecond() {
        final Lexer list = this.lexer("a   b");
        list.nextToken();
        CoreMatchers.equalTo("a");
        MatcherAssert.assertThat(list.nextToken().text(), CoreMatchers.equalTo("b"));
    }

    @Test
    void tabIsWhitespaceFirst() {
        MatcherAssert.assertThat(
            this.lexer("\tword").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void tabIsWhitespaceSecond() {
        final Token tokn = this.lexer("\tword").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("word"));
    }

    @Test
    void equalsAloneIsWordNotAssignmentFirst() {
        MatcherAssert.assertThat(
            this.lexer("=").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void equalsAloneIsWordNotAssignmentSecond() {
        final Token tokn = this.lexer("=").nextToken();
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
    void wordBreaksAtLeftParenFirst() {
        MatcherAssert.assertThat(
            this.lexer("echo(").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void wordBreaksAtLeftParenSecond() {
        final Token tokfirst = this.lexer("echo(").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    void wordBreaksAtLeftParenThird() {
        final Lexer list = this.lexer("echo(");
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.LPAREN));
    }

    @Test
    void wordBreaksAtRightParenFirst() {
        MatcherAssert.assertThat(
            this.lexer("echo)").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void wordBreaksAtRightParenSecond() {
        final Token tokfirst = this.lexer("echo)").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokfirst.text(), CoreMatchers.equalTo(LexerTest.ECHO_CMD));
    }

    @Test
    void wordBreaksAtRightParenThird() {
        final Lexer list = this.lexer("echo)");
        list.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        CoreMatchers.equalTo(LexerTest.ECHO_CMD);
        MatcherAssert.assertThat(list.nextToken().type(), CoreMatchers.equalTo(TokenType.RPAREN));
    }

    @Test
    void singleQuotedWordFirst() {
        MatcherAssert.assertThat(
            this.lexer("'hello'").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void singleQuotedWordSecond() {
        final Token tokn = this.lexer("'hello'").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("'hello'"));
    }

    @Test
    void doubleQuotedWordFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"hello\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedWordSecond() {
        final Token tokn = this.lexer("\"hello\"").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"hello\""));
    }

    @Test
    void backslashEscapesInsideWordFirst() {
        MatcherAssert.assertThat(
            this.lexer("a\\ b").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void backslashEscapesInsideWordSecond() {
        final Token tokn = this.lexer("a\\ b").nextToken();
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
    void doubleQuotedBackslashDollarFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"\\$\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashDollarSecond() {
        final Token tokn = this.lexer("\"\\$\"").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"\\$\""));
    }

    @Test
    void doubleQuotedBackslashBacktickFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"\\`\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashBacktickSecond() {
        final Token tokn = this.lexer("\"\\`\"").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"\\`\""));
    }

    @Test
    void doubleQuotedBackslashNewlineFirst() {
        MatcherAssert.assertThat(
            this.lexer(String.format("\"a\\%nb\"")).nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashNewlineSecond() {
        final Token tokn = this.lexer(String.format("\"a\\%nb\"")).nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"ab\""));
    }

    @Test
    void doubleQuotedBackslashBackslashFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\\\b\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashBackslashSecond() {
        final Token tokn = this.lexer("\"a\\\\b\"").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"a\\\\b\""));
    }

    @Test
    void doubleQuotedBackslashDoublequoteFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\\"b\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashDoublequoteSecond() {
        final Token tokn = this.lexer("\"a\\\"b\"").nextToken();
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
    void assignmentWordEdgeCasesFirst() {
        MatcherAssert.assertThat(
            this.lexer("=value").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void assignmentWordEdgeCasesSecond() {
        this.lexer("=value").nextToken();
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
    void doubleQuotedBackslashNonSpecialKeptLiteralFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"\\a\"").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void doubleQuotedBackslashNonSpecialKeptLiteralSecond() {
        final Token tokn = this.lexer("\"\\a\"").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("\"\\a\""));
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
    void digitsThenSpaceIsWordNotIoNumberFirst() {
        MatcherAssert.assertThat(
            this.lexer("42 ").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void digitsThenSpaceIsWordNotIoNumberSecond() {
        final Token tokn = this.lexer("42 ").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(tokn.text(), CoreMatchers.equalTo("42"));
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuoteFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"a'b\"").nextToken().text(),
            CoreMatchers.equalTo("\"a'b\"")
        );
    }

    @Test
    void singleQuoteLiteralInsideDoubleQuoteSecond() {
        this.lexer("\"a'b\"").nextToken();
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
    void doubleQuotedEscapesFirst() {
        MatcherAssert.assertThat(
            this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\$b\\`c\\\"d\\\\e\"")
        );
    }

    @Test
    void doubleQuotedEscapesSecond() {
        this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken();
        CoreMatchers.equalTo("\"a\\$b\\`c\\\"d\\\\e\"");
        MatcherAssert.assertThat(
            this.lexer("\"a\\qb\"").nextToken().text(),
            CoreMatchers.equalTo("\"a\\qb\"")
        );
    }

    @Test
    void doubleQuotedEscapesThird() {
        this.lexer("\"a\\$b\\`c\\\"d\\\\e\"").nextToken();
        CoreMatchers.equalTo("\"a\\$b\\`c\\\"d\\\\e\"");
        this.lexer("\"a\\qb\"").nextToken();
        CoreMatchers.equalTo("\"a\\qb\"");
        MatcherAssert.assertThat(
            this.lexer(String.format("\"a\\%nb\"")).nextToken().text(),
            CoreMatchers.equalTo("\"ab\"")
        );
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
    void backtickWithEscapedBacktick() {
        MatcherAssert.assertThat(
            this.lexer("`a\\`b`").nextToken().text(),
            CoreMatchers.equalTo("`a\\`b`")
        );
    }

    @Test
    void assignmentWordNameValidationFirst() {
        MatcherAssert.assertThat(
            this.lexer("A1=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
    }

    @Test
    void assignmentWordNameValidationSecond() {
        this.lexer("A1=x").nextToken();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        MatcherAssert.assertThat(
            this.lexer("_A=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD)
        );
    }

    @Test
    void assignmentWordNameValidationThird() {
        this.lexer("A1=x").nextToken();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        this.lexer("_A=x").nextToken();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        MatcherAssert.assertThat(
            this.lexer("1A=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void assignmentWordNameValidationFourth() {
        this.lexer("A1=x").nextToken();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        this.lexer("_A=x").nextToken();
        CoreMatchers.equalTo(TokenType.ASSIGNMENT_WORD);
        this.lexer("1A=x").nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(
            this.lexer("A-B=x").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void descriptorDigitsAreRecognisedFirst() {
        MatcherAssert.assertThat(
            this.lexer("2>file").nextToken().type(),
            CoreMatchers.equalTo(TokenType.IO_NUMBER)
        );
    }

    @Test
    void descriptorDigitsAreRecognisedSecond() {
        final Lexer digits = this.lexer("2>file");
        digits.nextToken();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        MatcherAssert.assertThat(digits.nextToken().type(), CoreMatchers.equalTo(TokenType.GREAT));
    }

    @Test
    void descriptorDigitsAreRecognisedThird() {
        final Lexer digits = this.lexer("2>file");
        digits.nextToken();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        digits.nextToken();
        CoreMatchers.equalTo(TokenType.GREAT);
        MatcherAssert.assertThat(
            this.lexer("2a>file").nextToken().type(),
            CoreMatchers.equalTo(TokenType.WORD)
        );
    }

    @Test
    void descriptorDigitsAreRecognisedFourth() {
        final Lexer digits = this.lexer("2>file");
        digits.nextToken();
        CoreMatchers.equalTo(TokenType.IO_NUMBER);
        digits.nextToken();
        CoreMatchers.equalTo(TokenType.GREAT);
        final Lexer mixed = this.lexer("2a>file");
        mixed.nextToken();
        CoreMatchers.equalTo(TokenType.WORD);
        MatcherAssert.assertThat(mixed.nextToken().type(), CoreMatchers.equalTo(TokenType.GREAT));
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
