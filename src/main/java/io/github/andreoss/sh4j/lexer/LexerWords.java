/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Word tokenisation for {@link Lexer}.
 * @since 1.0
 */
final class LexerWords {

    /**
     * Utility class; no instances.
     */
    private LexerWords() {
        super();
    }

    /**
     * Reads a single WORD token.
     * @param reader Input cursor
     * @return The next word token
     */
    static Token readWord(final CharReader reader) {
        final Position pos = new Position(reader.getLine(), reader.getColumn());
        final StringBuilder buf = new StringBuilder();
        final QuoteState quotes = new QuoteState();
        boolean done = false;
        while (!done && !reader.isEof()) {
            done = LexerWords.readWordChar(reader, buf, quotes);
        }
        final String text = buf.toString();
        final TokenType type;
        if (LexerSupport.isAllDigits(text) && !reader.isEof()
            && (reader.peek() == '<' || reader.peek() == '>')) {
            type = TokenType.IO_NUMBER;
        } else if (LexerSupport.isAssignmentWord(text)) {
            type = TokenType.ASSIGNMENT_WORD;
        } else {
            type = TokenType.WORD;
        }
        return new Token(type, text, pos.line(), pos.column());
    }

    /**
     * Consumes one character into the word buffer.
     * @param reader Input cursor
     * @param buf Accumulator
     * @param quotes Quote-state flags
     * @return True when the word ends
     */
    private static boolean readWordChar(
        final CharReader reader, final StringBuilder buf,
        final QuoteState quotes
    ) {
        final int chr = reader.peek();
        boolean done = false;
        if (chr == '\\' && !quotes.inSingle && !quotes.inDouble) {
            LexerWordChars.consumeBackslash(reader, buf);
        } else if (chr == '\'' && !quotes.inDouble) {
            reader.consume();
            buf.append('\'');
            quotes.inSingle = !quotes.inSingle;
        } else if (chr == '"' && !quotes.inSingle) {
            reader.consume();
            buf.append('"');
            quotes.inDouble = !quotes.inDouble;
        } else if (quotes.inSingle) {
            buf.append((char) reader.consume());
        } else if (quotes.inDouble) {
            LexerWordChars.consumeDoubleQuoted(reader, buf, chr);
        } else if (chr == '$') {
            LexerWordChars.consumeDollar(reader, buf);
        } else if (chr == '`') {
            LexerWordChars.consumeBackticks(reader, buf);
        } else if (chr == '\n' || chr == ' ' || chr == '\t'
            || LexerSupport.isOperatorChar(chr)) {
            done = true;
        } else {
            buf.append((char) reader.consume());
        }
        return done;
    }

    /**
     * Captured source position.
     * @param line Source line
     * @param column Source column
     */
    private record Position(int line, int column) {
    }

    /**
     * Mutable holder for the current quoting state.
     * @since 1.0
     */
    private static final class QuoteState {

        /**
         * True while inside single quotes.
         */
        private boolean inSingle;
        /**
         * True while inside double quotes.
         */
        private boolean inDouble;

        /**
         * Default constructor; both flags start false.
         */
        QuoteState() {
            super();
        }
    }
}
