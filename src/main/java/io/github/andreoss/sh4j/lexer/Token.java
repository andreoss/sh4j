/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Immutable lexer output. Carries the token's category plus its raw text
 * and the source position it came from.
 *
 * @since 1.0
 */
public final class Token {

    /** The token's syntactic category. */
    private final TokenType type;

    /** The verbatim characters that produced the token. */
    private final String text;

    /** Source position where the token starts. */
    private final Position position;

    /**
     * Creates a token.
     *
     * @param type The token's syntactic category
     * @param text The verbatim characters that produced the token
     * @param position Source position where the token starts
     */
    public Token(final TokenType type, final String text, final Position position) {
        this.type = type;
        this.text = text;
        this.position = position;
    }

    /**
     * Returns the token's syntactic category.
     *
     * @return The token's syntactic category
     */
    public TokenType type() {
        return this.type;
    }

    /**
     * Returns the verbatim characters that produced the token.
     *
     * @return The verbatim characters that produced the token
     */
    public String text() {
        return this.text;
    }

    /**
     * Returns the source line where the token starts.
     *
     * @return Source line where the token starts
     */
    public int line() {
        return this.position.line();
    }

    /**
     * Returns the source column where the token starts.
     *
     * @return Source column where the token starts
     */
    public int column() {
        return this.position.column();
    }

    @Override
    public boolean equals(final Object obj) {
        boolean equal = this == obj;
        if (!equal && obj instanceof Token other) {
            equal = this.type == other.type
                && this.line() == other.line()
                && this.column() == other.column()
                && Token.sameText(this.text, other.text);
        }
        return equal;
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (this.type != null) {
            result = this.type.hashCode();
        }
        result = 31 * result + Token.hashText(this.text);
        result = 31 * result + this.line();
        result = 31 * result + this.column();
        return result;
    }

    @Override
    public String toString() {
        return String.format(
            "Token[type=%s, text=%s, line=%d, column=%d]",
            this.type, this.text, this.line(), this.column()
        );
    }

    /**
     * Builds a token, reading its position from the given reader.
     *
     * @param type The token's syntactic category
     * @param text The verbatim characters that produced the token
     * @param reader The lexer's input cursor, queried for line and column
     * @return A token at the reader's current position
     */
    static Token ofReader(final TokenType type, final String text, final CharReader reader) {
        return new Token(type, text, new Position(reader.getLine(), reader.getColumn()));
    }

    private static boolean sameText(final String left, final String right) {
        boolean same = left == null;
        if (same) {
            same = right == null;
        } else {
            same = left.equals(right);
        }
        return same;
    }

    private static int hashText(final String text) {
        int hash = 0;
        if (text != null) {
            hash = text.hashCode();
        }
        return hash;
    }
}
