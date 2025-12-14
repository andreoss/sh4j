/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.io.IOException;
import java.io.Reader;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for CharReader.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class CharReaderTest {

    @Test
    public void peekReturnsFirstChar() {
        assertThat(new CharReader("a").peek(), is(97));
    }

    @Test
    public void consumeReturnsCharsInOrder() {
        final CharReader result = new CharReader("ab");
        assertThat(result.consume(), is(97));
        assertThat(result.consume(), is(98));
    }

    @Test
    public void consumeAdvancesPosition() {
        final CharReader result = new CharReader("ab");
        result.consume();
        assertThat(result.peek(), is(98));
    }

    @Test
    public void isEofReturnsFalseWhenCharsRemain() {
        assertThat(new CharReader("abc").isEof(), is(false));
    }

    @Test
    public void eofOnEmptyString() {
        final CharReader result = new CharReader("");
        assertThat(result.isEof(), is(true));
        assertThat(result.peek(), is(-1));
    }

    @Test
    public void eofAfterFullConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        assertThat(result.isEof(), is(true));
        assertThat(result.peek(), is(-1));
    }

    @Test
    public void consumeEofReturnsMinusOne() {
        assertThat(new CharReader("").consume(), is(-1));
    }

    @Test
    public void lineColumnInitialValues() {
        final CharReader result = new CharReader("a");
        assertThat(result.getLine(), is(1));
        assertThat(result.getColumn(), is(0));
    }

    @Test
    public void lineColumnAfterConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        assertThat(result.getLine(), is(1));
        assertThat(result.getColumn(), is(1));
    }

    @Test
    public void newlineIncrementsLineAndResetsColumn() {
        final CharReader result = new CharReader(String.format("%n"));
        result.consume();
        assertThat(result.getLine(), is(2));
        assertThat(result.getColumn(), is(0));
    }

    @Test
    public void multipleLines() {
        final CharReader result = new CharReader(String.format("a%nb"));
        assertThat(result.getLine(), is(1));
        assertThat(result.getColumn(), is(0));
        result.consume();
        assertThat(result.getLine(), is(1));
        assertThat(result.getColumn(), is(1));
        result.consume();
        assertThat(result.getLine(), is(2));
        assertThat(result.getColumn(), is(0));
        result.consume();
        assertThat(result.getLine(), is(2));
        assertThat(result.getColumn(), is(1));
    }

    @Test
    public void ioExceptionTreatedAsEof() {
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
        assertThat(reader.isEof(), is(true));
        assertThat(reader.peek(), is(-1));
        assertThat(reader.consume(), is(-1));
    }

    @Test
    public void eofStaysEofOnMultipleCalls() {
        final CharReader result = new CharReader("");
        assertThat(result.isEof(), is(true));
        assertThat(result.peek(), is(-1));
        assertThat(result.consume(), is(-1));
        assertThat(result.isEof(), is(true));
        assertThat(result.peek(), is(-1));
    }
}
