/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Reader;

import org.junit.jupiter.api.Test;

class CharReaderTest {
    @Test
    void peekReturnsFirstChar() {
        CharReader r = new CharReader("a");
        assertEquals('a', r.peek());
    }

    @Test
    void consumeReturnsCharsInOrder() {
        CharReader r = new CharReader("ab");
        assertEquals('a', r.consume());
        assertEquals('b', r.consume());
    }

    @Test
    void consumeAdvancesPosition() {
        CharReader r = new CharReader("ab");
        r.consume();
        assertEquals('b', r.peek());
    }

    @Test
    void isEOFReturnsFalseWhenCharsRemain() {
        CharReader r = new CharReader("abc");
        assertFalse(r.isEOF());
    }

    @Test
    void eofOnEmptyString() {
        CharReader r = new CharReader("");
        assertTrue(r.isEOF());
        assertEquals(-1, r.peek());
    }

    @Test
    void eofAfterFullConsume() {
        CharReader r = new CharReader("a");
        r.consume();
        assertTrue(r.isEOF());
        assertEquals(-1, r.peek());
    }

    @Test
    void consumeEofReturnsMinusOne() {
        CharReader r = new CharReader("");
        assertEquals(-1, r.consume());
    }

    @Test
    void lineColumnInitialValues() {
        CharReader r = new CharReader("a");
        assertEquals(1, r.getLine());
        assertEquals(0, r.getColumn());
    }

    @Test
    void lineColumnAfterConsume() {
        CharReader r = new CharReader("a");
        r.consume();
        assertEquals(1, r.getLine());
        assertEquals(1, r.getColumn());
    }

    @Test
    void newlineIncrementsLineAndResetsColumn() {
        CharReader r = new CharReader("\n");
        r.consume();
        assertEquals(2, r.getLine());
        assertEquals(0, r.getColumn());
    }

    @Test
    void multipleLines() {
        CharReader r = new CharReader("a\nb");
        assertEquals(1, r.getLine());
        assertEquals(0, r.getColumn());
        r.consume(); // 'a'
        assertEquals(1, r.getLine());
        assertEquals(1, r.getColumn());
        r.consume(); // '\n'
        assertEquals(2, r.getLine());
        assertEquals(0, r.getColumn());
        r.consume(); // 'b'
        assertEquals(2, r.getLine());
        assertEquals(1, r.getColumn());
    }

    @Test
    void ioExceptionTreatedAsEof() {
        Reader broken = new Reader() {
            @Override
            public int read() throws IOException {
                throw new IOException();
            }
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException();
            }
            @Override
            public void close() {}
        };
        CharReader r = new CharReader(broken);
        assertTrue(r.isEOF());
        assertEquals(-1, r.peek());
        assertEquals(-1, r.consume());
    }

    @Test
    void eofStaysEofOnMultipleCalls() {
        CharReader r = new CharReader("");
        assertTrue(r.isEOF());
        assertEquals(-1, r.peek());
        assertEquals(-1, r.consume());
        assertTrue(r.isEOF());
        assertEquals(-1, r.peek());
    }
}
