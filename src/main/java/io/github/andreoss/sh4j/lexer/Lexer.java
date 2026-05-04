/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Tokeniser for the POSIX shell grammar (section 2.10). Pulls characters
 * from a {@link CharReader} and emits {@link Token} instances; one call
 * to {@link #nextToken()} yields the next token (or
 * {@link TokenType#END_OF_FILE} when the input is exhausted).
 *
 * @since 1.0
 */
public final class Lexer {

    /** Lookahead cursor over the input. */
    private final CharReader reader;

    /** Here-documents of the current line. */
    private final HereDocs docs;

    /** Type of the token emitted last. */
    private TokenType previous;

    /**
     * Creates a lexer over the given character source.
     *
     * @param reader The input cursor
     */
    public Lexer(final CharReader reader) {
        this(reader, new HereDocs());
    }

    /**
     * Creates a lexer that records here-documents in the given registry.
     *
     * @param reader The input cursor
     * @param docs Here-documents of the current line
     */
    Lexer(final CharReader reader, final HereDocs docs) {
        this.reader = reader;
        this.docs = docs;
        this.previous = TokenType.NEWLINE;
    }

    /**
     * Returns the next token. Skips whitespace before classifying.
     *
     * @return The next token, or an END_OF_FILE token at end-of-input
     */
    public Token nextToken() {
        this.skipWhitespace();
        this.skipComment();
        final Token result;
        if (this.reader.isEof()) {
            result = Token.ofReader(TokenType.END_OF_FILE, "", this.reader);
        } else if (this.reader.peek() == '\n') {
            this.reader.consume();
            result = Token.ofReader(TokenType.NEWLINE, String.format("%n"), this.reader);
            this.docs.flush(this.reader);
        } else {
            final Token operator = LexerOperators.tryOperator(this.reader);
            if (operator == null) {
                result = LexerWords.readWord(this.reader);
            } else {
                result = operator;
            }
        }
        this.docs.watch(this.previous, result);
        this.previous = result.type();
        return result;
    }

    /**
     * Hands out the body of the next here-document, in the order of
     * the operators; it is filled once the line holding them ends.
     *
     * @return The body
     */
    public HereBody hereDoc() {
        return this.docs.next();
    }

    private void skipComment() {
        while (!this.reader.isEof() && this.reader.peek() == '#') {
            while (!this.reader.isEof() && this.reader.peek() != '\n') {
                this.reader.consume();
            }
            this.skipWhitespace();
        }
    }

    private void skipWhitespace() {
        while (!this.reader.isEof()
            && (this.reader.peek() == ' ' || this.reader.peek() == '\t')) {
            this.reader.consume();
        }
    }
}
