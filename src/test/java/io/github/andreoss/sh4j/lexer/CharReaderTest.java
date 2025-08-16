/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.io.IOException;
import java.io.Reader;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for CharReader.
 * @since 1.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
public final class CharReaderTest {

    @Test
    public void peekReturnsFirstChar() {
        Assert.assertEquals('a', new CharReader("a").peek());
    }

    @Test
    public void consumeReturnsCharsInOrder() {
        final CharReader result = new CharReader("ab");
        Assert.assertEquals('a', result.consume());
        Assert.assertEquals('b', result.consume());
    }

    @Test
    public void consumeAdvancesPosition() {
        final CharReader result = new CharReader("ab");
        result.consume();
        Assert.assertEquals('b', result.peek());
    }

    @Test
    public void isEofReturnsFalseWhenCharsRemain() {
        Assert.assertFalse(new CharReader("abc").isEof());
    }

    @Test
    public void eofOnEmptyString() {
        final CharReader result = new CharReader("");
        Assert.assertTrue(result.isEof());
        Assert.assertEquals(-1, result.peek());
    }

    @Test
    public void eofAfterFullConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        Assert.assertTrue(result.isEof());
        Assert.assertEquals(-1, result.peek());
    }

    @Test
    public void consumeEofReturnsMinusOne() {
        Assert.assertEquals(-1, new CharReader("").consume());
    }

    @Test
    public void lineColumnInitialValues() {
        final CharReader result = new CharReader("a");
        Assert.assertEquals(1, result.getLine());
        Assert.assertEquals(0, result.getColumn());
    }

    @Test
    public void lineColumnAfterConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        Assert.assertEquals(1, result.getLine());
        Assert.assertEquals(1, result.getColumn());
    }

    @Test
    public void newlineIncrementsLineAndResetsColumn() {
        final CharReader result = new CharReader(String.format("%n"));
        result.consume();
        Assert.assertEquals(2, result.getLine());
        Assert.assertEquals(0, result.getColumn());
    }

    @Test
    public void multipleLines() {
        final CharReader result = new CharReader(String.format("a%nb"));
        Assert.assertEquals(1, result.getLine());
        Assert.assertEquals(0, result.getColumn());
        result.consume();
        Assert.assertEquals(1, result.getLine());
        Assert.assertEquals(1, result.getColumn());
        result.consume();
        Assert.assertEquals(2, result.getLine());
        Assert.assertEquals(0, result.getColumn());
        result.consume();
        Assert.assertEquals(2, result.getLine());
        Assert.assertEquals(1, result.getColumn());
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
        Assert.assertTrue(reader.isEof());
        Assert.assertEquals(-1, reader.peek());
        Assert.assertEquals(-1, reader.consume());
    }

    @Test
    public void eofStaysEofOnMultipleCalls() {
        final CharReader result = new CharReader("");
        Assert.assertTrue(result.isEof());
        Assert.assertEquals(-1, result.peek());
        Assert.assertEquals(-1, result.consume());
        Assert.assertTrue(result.isEof());
        Assert.assertEquals(-1, result.peek());
    }
}
