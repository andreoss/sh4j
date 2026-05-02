/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Word tokenisation for {@link Lexer}.
 *
 * @since 1.0
 */
final class LexerWords {

    private LexerWords() {
    }

    /**
     * Reads a single WORD token.
     *
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
        return new Token(type, text, new Position(pos.line(), pos.column()));
    }

    private static boolean readWordChar(
        final CharReader reader, final StringBuilder buf, final QuoteState quotes
    ) {
        final boolean done;
        if (quotes.single()) {
            done = LexerWords.readInSingle(reader, buf, quotes);
        } else if (quotes.doubled()) {
            done = LexerWords.readInDouble(reader, buf, quotes);
        } else {
            done = LexerWords.readBare(reader, buf, quotes);
        }
        return done;
    }

    private static boolean readInSingle(
        final CharReader reader, final StringBuilder buf, final QuoteState quotes
    ) {
        final int chr = reader.peek();
        reader.consume();
        buf.append((char) chr);
        if (chr == '\'') {
            quotes.leaveSingle();
        }
        return false;
    }

    private static boolean readInDouble(
        final CharReader reader, final StringBuilder buf, final QuoteState quotes
    ) {
        final int chr = reader.peek();
        if (chr == '"') {
            reader.consume();
            buf.append('"');
            quotes.leaveDouble();
        } else if (chr == '$') {
            LexerWordChars.consumeDollar(reader, buf, true);
        } else if (chr == '`') {
            LexerWordChars.consumeBackticks(reader, buf);
        } else {
            LexerWordChars.consumeDoubleQuoted(reader, buf, chr);
        }
        return false;
    }

    private static boolean readBare(
        final CharReader reader, final StringBuilder buf, final QuoteState quotes
    ) {
        final int chr = reader.peek();
        final boolean done;
        if (chr == '\\') {
            LexerWordChars.consumeBackslash(reader, buf);
            done = false;
        } else if (chr == '\'') {
            reader.consume();
            buf.append('\'');
            quotes.enterSingle();
            done = false;
        } else if (chr == '"') {
            reader.consume();
            buf.append('"');
            quotes.enterDouble();
            done = false;
        } else if (chr == '$') {
            LexerWordChars.consumeDollar(reader, buf, false);
            done = false;
        } else if (chr == '`') {
            LexerWordChars.consumeBackticks(reader, buf);
            done = false;
        } else if (LexerWords.isTerminator(chr)) {
            done = true;
        } else {
            buf.append((char) reader.consume());
            done = false;
        }
        return done;
    }

    private static boolean isTerminator(final int chr) {
        return chr == '\n' || chr == ' ' || chr == '\t'
            || LexerSupport.isOperatorChar(chr);
    }
}
