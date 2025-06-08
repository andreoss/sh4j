/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.io.IOException;
import java.io.Reader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CharReaderTest {

    @Test
    void peekReturnsFirstChar() {
        assertEquals('a', new CharReader("a").peek());
    }

    @Test
    void consumeReturnsCharsInOrder() {
        final CharReader result = new CharReader("ab");
        assertEquals('a', result.consume());
        assertEquals('b', result.consume());
    }

    @Test
    void consumeAdvancesPosition() {
        final CharReader result = new CharReader("ab");
        result.consume();
        assertEquals('b', result.peek());
    }

    @Test
    void isEOFReturnsFalseWhenCharsRemain() {
        assertFalse(new CharReader("abc").isEof());
    }

    @Test
    void eofOnEmptyString() {
        final CharReader result = new CharReader("");
        assertTrue(result.isEof());
        assertEquals(-1, result.peek());
    }

    @Test
    void eofAfterFullConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        assertTrue(result.isEof());
        assertEquals(-1, result.peek());
    }

    @Test
    void consumeEofReturnsMinusOne() {
        assertEquals(-1, new CharReader("").consume());
    }

    @Test
    void lineColumnInitialValues() {
        final CharReader result = new CharReader("a");
        assertEquals(1, result.getLine());
        assertEquals(0, result.getColumn());
    }

    @Test
    void lineColumnAfterConsume() {
        final CharReader result = new CharReader("a");
        result.consume();
        assertEquals(1, result.getLine());
        assertEquals(1, result.getColumn());
    }

    @Test
    void newlineIncrementsLineAndResetsColumn() {
        final CharReader result = new CharReader(String.format("%n"));
        result.consume();
        assertEquals(2, result.getLine());
        assertEquals(0, result.getColumn());
    }

    @Test
    void multipleLines() {
        final CharReader result = new CharReader(String.format("a%nb"));
        assertEquals(1, result.getLine());
        assertEquals(0, result.getColumn());
        result.consume(); // 'a'
        assertEquals(1, result.getLine());
        assertEquals(1, result.getColumn());
        result.consume(); // '\n'
        assertEquals(2, result.getLine());
        assertEquals(0, result.getColumn());
        result.consume(); // 'b'
        assertEquals(2, result.getLine());
        assertEquals(1, result.getColumn());
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
        assertTrue(r.isEof());
        assertEquals(-1, r.peek());
        assertEquals(-1, r.consume());
    }

    @Test
    void eofStaysEofOnMultipleCalls() {
        final CharReader result = new CharReader("");
        assertTrue(result.isEof());
        assertEquals(-1, result.peek());
        assertEquals(-1, result.consume());
        assertTrue(result.isEof());
        assertEquals(-1, result.peek());
    }
}
