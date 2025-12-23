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

    /**
     * Creates a lexer over the given character source.
     *
     * @param reader The input cursor
     */
    public Lexer(final CharReader reader) {
        this.reader = reader;
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
        } else {
            final Token operator = LexerOperators.tryOperator(this.reader);
            if (operator == null) {
                result = LexerWords.readWord(this.reader);
            } else {
                result = operator;
            }
        }
        return result;
    }

    /**
     * Reads the body of a here-document up to (but not including) the
     * closing delimiter line. POSIX 2.7.4: each line ends with a newline,
     * including the last line before the delimiter.
     *
     * @param delimiter The heredoc delimiter
     * @param strip True for {@code <<-} (strip leading tabs on each line)
     * @return The heredoc content with trailing newline per POSIX
     */
    public String readHereDocContent(final String delimiter, final boolean strip) {
        final StringBuilder content = new StringBuilder();
        if (this.reader.peek() == '\n') {
            this.reader.consume();
        }
        boolean done = false;
        while (!done && !this.reader.isEof()) {
            final String text = this.readHereDocLine(strip);
            if (!this.reader.isEof()) {
                this.reader.consume();
            }
            if (text.equals(delimiter)) {
                done = true;
            } else {
                content.append(text).append('\n');
            }
        }
        return content.toString();
    }

    private String readHereDocLine(final boolean strip) {
        final StringBuilder line = new StringBuilder();
        boolean stripping = strip;
        boolean exit = false;
        while (!exit && !this.reader.isEof()) {
            final int chr = this.reader.peek();
            if (chr == '\n') {
                exit = true;
            } else if (stripping && chr == '\t') {
                this.reader.consume();
            } else {
                stripping = false;
                line.append((char) this.reader.consume());
            }
        }
        return line.toString();
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
