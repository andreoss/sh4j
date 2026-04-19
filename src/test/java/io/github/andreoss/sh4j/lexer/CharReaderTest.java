/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for CharReader.
 *
 * @since 1.0
 */
final class CharReaderTest {

    @Test
    void peekReturnsFirstChar() {
        MatcherAssert.assertThat(new CharReader("a").peek(), CoreMatchers.is(97));
    }

    @Test
    void consumeReturnsCharsInOrderFirst() {
        MatcherAssert.assertThat(new CharReader("ab").consume(), CoreMatchers.is(97));
    }

    @Test
    void consumeReturnsCharsInOrderSecond() {
        final CharReader result = new CharReader("ab");
        result.consume();
        MatcherAssert.assertThat(result.consume(), CoreMatchers.is(98));
    }

    @Test
    void consumeAdvancesPosition() {
        final CharReader result = new CharReader("ab");
        result.consume();
        MatcherAssert.assertThat(result.peek(), CoreMatchers.is(98));
    }

    @Test
    void isEofReturnsFalseWhenCharsRemain() {
        MatcherAssert.assertThat(new CharReader("abc").isEof(), CoreMatchers.is(false));
    }

    @Test
    void eofOnEmptyStringFirst() {
        MatcherAssert.assertThat(new CharReader("").isEof(), CoreMatchers.is(true));
    }

    @Test
    void eofOnEmptyStringSecond() {
        MatcherAssert.assertThat(new CharReader("").peek(), CoreMatchers.is(-1));
    }

    @Test
    void eofAfterFullConsumeFirst() {
        final CharReader result = new CharReader("a");
        result.consume();
        MatcherAssert.assertThat(result.isEof(), CoreMatchers.is(true));
    }

    @Test
    void eofAfterFullConsumeSecond() {
        final CharReader result = new CharReader("a");
        result.consume();
        MatcherAssert.assertThat(result.peek(), CoreMatchers.is(-1));
    }

    @Test
    void consumeEofReturnsMinusOne() {
        MatcherAssert.assertThat(new CharReader("").consume(), CoreMatchers.is(-1));
    }

    @Test
    void lineColumnInitialValuesFirst() {
        MatcherAssert.assertThat(new CharReader("a").getLine(), CoreMatchers.is(1));
    }

    @Test
    void lineColumnInitialValuesSecond() {
        MatcherAssert.assertThat(new CharReader("a").getColumn(), CoreMatchers.is(0));
    }

    @Test
    void lineColumnAfterConsumeFirst() {
        final CharReader result = new CharReader("a");
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(1));
    }

    @Test
    void lineColumnAfterConsumeSecond() {
        final CharReader result = new CharReader("a");
        result.consume();
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(1));
    }

    @Test
    void newlineIncrementsLineAndResetsColumnFirst() {
        final CharReader result = new CharReader(String.format("%n"));
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(2));
    }

    @Test
    void newlineIncrementsLineAndResetsColumnSecond() {
        final CharReader result = new CharReader(String.format("%n"));
        result.consume();
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(0));
    }

    @Test
    void multipleLinesFirst() {
        MatcherAssert.assertThat(
            new CharReader(String.format("a%nb")).getLine(),
            CoreMatchers.is(1)
        );
    }

    @Test
    void multipleLinesSecond() {
        MatcherAssert.assertThat(
            new CharReader(String.format("a%nb")).getColumn(),
            CoreMatchers.is(0)
        );
    }

    @Test
    void multipleLinesThird() {
        final CharReader result = new CharReader(String.format("a%nb"));
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(1));
    }

    @Test
    void multipleLinesFourth() {
        final CharReader result = new CharReader(String.format("a%nb"));
        result.consume();
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(1));
    }

    @Test
    void multipleLinesFifth() {
        final CharReader result = new CharReader(String.format("a%nb"));
        result.consume();
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(2));
    }

    @Test
    void multipleLinesSixth() {
        final CharReader result = new CharReader(String.format("a%nb"));
        result.consume();
        result.consume();
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(0));
    }

    @Test
    void multipleLinesSeventh() {
        final CharReader result = new CharReader(String.format("a%nb"));
        result.consume();
        result.consume();
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(2));
    }

    @Test
    void multipleLinesEighth() {
        final CharReader result = new CharReader(String.format("a%nb"));
        result.consume();
        result.consume();
        result.consume();
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(1));
    }

    @Test
    void ioExceptionTreatedAsEofFirst() throws IOException {
        try (Reader failing = new CharReaderTest.Failing()) {
            MatcherAssert.assertThat(new CharReader(failing).isEof(), CoreMatchers.is(true));
        }
    }

    @Test
    void ioExceptionTreatedAsEofSecond() throws IOException {
        try (Reader failing = new CharReaderTest.Failing()) {
            MatcherAssert.assertThat(new CharReader(failing).peek(), CoreMatchers.is(-1));
        }
    }

    @Test
    void ioExceptionTreatedAsEofThird() throws IOException {
        try (Reader failing = new CharReaderTest.Failing()) {
            final CharReader reader = new CharReader(failing);
            reader.peek();
            MatcherAssert.assertThat(reader.consume(), CoreMatchers.is(-1));
        }
    }

    @Test
    void eofStaysEofOnMultipleCallsFirst() {
        MatcherAssert.assertThat(new CharReader("").isEof(), CoreMatchers.is(true));
    }

    @Test
    void eofStaysEofOnMultipleCallsSecond() {
        MatcherAssert.assertThat(new CharReader("").peek(), CoreMatchers.is(-1));
    }

    @Test
    void eofStaysEofOnMultipleCallsThird() {
        final CharReader result = new CharReader("");
        result.peek();
        MatcherAssert.assertThat(result.consume(), CoreMatchers.is(-1));
    }

    @Test
    void eofStaysEofOnMultipleCallsFourth() {
        final CharReader result = new CharReader("");
        result.peek();
        result.consume();
        MatcherAssert.assertThat(result.isEof(), CoreMatchers.is(true));
    }

    @Test
    void eofStaysEofOnMultipleCallsFifth() {
        final CharReader result = new CharReader("");
        result.peek();
        result.consume();
        MatcherAssert.assertThat(result.peek(), CoreMatchers.is(-1));
    }

    /**
     * Reader whose every read fails.
     *
     * @since 1.0
     */
    private static final class Failing extends StringReader {

        /**
         * Ctor.
         */
        Failing() {
            super("");
        }

        @Override
        public int read() throws IOException {
            throw new IOException();
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len)
            throws IOException {
            throw new IOException();
        }
    }
}
