/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.io.IOException;
import java.io.Reader;
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
    void consumeReturnsCharsInOrder() {
        final CharReader result = new CharReader("ab");
        MatcherAssert.assertThat(result.consume(), CoreMatchers.is(97));
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
    void eofOnEmptyString() {
        final CharReader result = new CharReader("");
        MatcherAssert.assertThat(result.isEof(), CoreMatchers.is(true));
        MatcherAssert.assertThat(result.peek(), CoreMatchers.is(-1));
    }

    @Test
    void eofAfterFullConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        MatcherAssert.assertThat(result.isEof(), CoreMatchers.is(true));
        MatcherAssert.assertThat(result.peek(), CoreMatchers.is(-1));
    }

    @Test
    void consumeEofReturnsMinusOne() {
        MatcherAssert.assertThat(new CharReader("").consume(), CoreMatchers.is(-1));
    }

    @Test
    void lineColumnInitialValues() {
        final CharReader result = new CharReader("a");
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(1));
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(0));
    }

    @Test
    void lineColumnAfterConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(1));
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(1));
    }

    @Test
    void newlineIncrementsLineAndResetsColumn() {
        final CharReader result = new CharReader(String.format("%n"));
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(2));
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(0));
    }

    @Test
    void multipleLines() {
        final CharReader result = new CharReader(String.format("a%nb"));
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(1));
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(0));
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(1));
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(1));
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(2));
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(0));
        result.consume();
        MatcherAssert.assertThat(result.getLine(), CoreMatchers.is(2));
        MatcherAssert.assertThat(result.getColumn(), CoreMatchers.is(1));
    }

    @Test
    void ioExceptionTreatedAsEof() {
        final CharReader reader = new CharReader(
            new Reader() {

                @Override
                public int read() throws IOException {
                    throw new IOException();
                }

                @Override
                public int read(final char[] cbuf, final int off, final int len)
                    throws IOException {
                    throw new IOException();
                }

                @Override
                public void close() {
                    // intentional no-op
                }
            }
        );
        MatcherAssert.assertThat(reader.isEof(), CoreMatchers.is(true));
        MatcherAssert.assertThat(reader.peek(), CoreMatchers.is(-1));
        MatcherAssert.assertThat(reader.consume(), CoreMatchers.is(-1));
    }

    @Test
    void eofStaysEofOnMultipleCalls() {
        final CharReader result = new CharReader("");
        MatcherAssert.assertThat(result.isEof(), CoreMatchers.is(true));
        MatcherAssert.assertThat(result.peek(), CoreMatchers.is(-1));
        MatcherAssert.assertThat(result.consume(), CoreMatchers.is(-1));
        MatcherAssert.assertThat(result.isEof(), CoreMatchers.is(true));
        MatcherAssert.assertThat(result.peek(), CoreMatchers.is(-1));
    }
}
