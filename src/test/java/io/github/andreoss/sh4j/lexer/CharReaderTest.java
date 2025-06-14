/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.io.IOException;
import java.io.Reader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for CharReader.
 * @since 1.0
 */
final class CharReaderTest {

    @Test
    void peekReturnsFirstChar() {
        Assertions.assertEquals('a', new CharReader("a").peek());
    }

    @Test
    void consumeReturnsCharsInOrder() {
        final CharReader result = new CharReader("ab");
        Assertions.assertEquals('a', result.consume());
        Assertions.assertEquals('b', result.consume());
    }

    @Test
    void consumeAdvancesPosition() {
        final CharReader result = new CharReader("ab");
        result.consume();
        Assertions.assertEquals('b', result.peek());
    }

    @Test
    void isEOFReturnsFalseWhenCharsRemain() {
        Assertions.assertFalse(new CharReader("abc").isEof());
    }

    @Test
    void eofOnEmptyString() {
        final CharReader result = new CharReader("");
        Assertions.assertTrue(result.isEof());
        Assertions.assertEquals(-1, result.peek());
    }

    @Test
    void eofAfterFullConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        Assertions.assertTrue(result.isEof());
        Assertions.assertEquals(-1, result.peek());
    }

    @Test
    void consumeEofReturnsMinusOne() {
        Assertions.assertEquals(-1, new CharReader("").consume());
    }

    @Test
    void lineColumnInitialValues() {
        final CharReader result = new CharReader("a");
        Assertions.assertEquals(1, result.getLine());
        Assertions.assertEquals(0, result.getColumn());
    }

    @Test
    void lineColumnAfterConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        Assertions.assertEquals(1, result.getLine());
        Assertions.assertEquals(1, result.getColumn());
    }

    @Test
    void newlineIncrementsLineAndResetsColumn() {
        final CharReader result = new CharReader(String.format("%n"));
        result.consume();
        Assertions.assertEquals(2, result.getLine());
        Assertions.assertEquals(0, result.getColumn());
    }

    @Test
    void multipleLines() {
        final CharReader result = new CharReader(String.format("a%nb"));
        Assertions.assertEquals(1, result.getLine());
        Assertions.assertEquals(0, result.getColumn());
        result.consume(); // 'a'
        Assertions.assertEquals(1, result.getLine());
        Assertions.assertEquals(1, result.getColumn());
        result.consume(); // '\n'
        Assertions.assertEquals(2, result.getLine());
        Assertions.assertEquals(0, result.getColumn());
        result.consume(); // 'b'
        Assertions.assertEquals(2, result.getLine());
        Assertions.assertEquals(1, result.getColumn());
    }

    @Test
    void ioExceptionTreatedAsEof() {
        final Reader broken = new Reader() {

            @Override

            public int read() throws IOException {
                throw new IOException();
            }

            @Override

            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                throw new IOException();
            }

            @Override

            public void close() {}
        };
        final CharReader r = new CharReader(broken);
        Assertions.assertTrue(r.isEof());
        Assertions.assertEquals(-1, r.peek());
        Assertions.assertEquals(-1, r.consume());
    }

    @Test
    void eofStaysEofOnMultipleCalls() {
        final CharReader result = new CharReader("");
        Assertions.assertTrue(result.isEof());
        Assertions.assertEquals(-1, result.peek());
        Assertions.assertEquals(-1, result.consume());
        Assertions.assertTrue(result.isEof());
        Assertions.assertEquals(-1, result.peek());
    }
}
