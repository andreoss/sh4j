/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class CharReader {
    private final Reader reader;
    private int nextChar;
    private int line;
    private int column;

    public CharReader(String input) {
        this(new StringReader(input));
    }

    public CharReader(Reader reader) {
        this.reader = reader;
        this.line = 1;
        this.column = 0;
        read();
    }

    private void read() {
        try {
            nextChar = reader.read();
        } catch (IOException e) {
            nextChar = -1;
        }
    }

    public int peek() {
        return nextChar;
    }

    public int consume() {
        int c = nextChar;
        if (c == '\n') {
            line++;
            column = 0;
        } else if (c != -1) {
            column++;
        }
        read();
        return c;
    }

    public boolean isEOF() {
        return nextChar == -1;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
