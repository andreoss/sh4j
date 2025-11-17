/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Immutable lexer output. Carries the token's category plus its raw text
 * and the source position it came from.
 * @since 1.0
 */
public final class Token {

    /** The token's syntactic category. */
    private final TokenType type;
    /** The verbatim characters that produced the token. */
    private final String text;
    /** 1-based source line where the token starts. */
    private final int line;
    /** 1-based source column where the token starts. */
    private final int column;

    /**
     * Creates a token.
     * @param type   The token's syntactic category
     * @param text   The verbatim characters that produced the token
     * @param line   1-based source line where the token starts
     * @param column 1-based source column where the token starts
     */
    public Token(final TokenType type, final String text, final int line, final int column) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
    }

    /**
     * Builds a token, reading its position from the given reader.
     * @param type   The token's syntactic category
     * @param text   The verbatim characters that produced the token
     * @param reader The lexer's input cursor, queried for line/column
     * @return A token at the reader's current position
     */
    static Token of(final TokenType type, final String text, final CharReader reader) {
        return new Token(type, text, reader.getLine(), reader.getColumn());
    }

    /**
     * Returns the token's syntactic category.
     * @return The token's syntactic category
     */
    public TokenType type() {
        return this.type;
    }

    /**
     * Returns the verbatim characters that produced the token.
     * @return The verbatim characters that produced the token
     */
    public String text() {
        return this.text;
    }

    /**
     * Returns the 1-based source line where the token starts.
     * @return 1-based source line where the token starts
     */
    public int line() {
        return this.line;
    }

    /**
     * Returns the 1-based source column where the token starts.
     * @return 1-based source column where the token starts
     */
    public int column() {
        return this.column;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Token other)) {
            return false;
        }
        return this.type == other.type
            && this.line == other.line
            && this.column == other.column
            && (this.text == null ? other.text == null : this.text.equals(other.text));
    }

    @Override
    public int hashCode() {
        int result = this.type != null ? this.type.hashCode() : 0;
        result = 31 * result + (this.text != null ? this.text.hashCode() : 0);
        result = 31 * result + this.line;
        result = 31 * result + this.column;
        return result;
    }

    @Override
    public String toString() {
        return "Token[type=" + this.type + ", text=" + this.text
            + ", line=" + this.line + ", column=" + this.column + "]";
    }
}
