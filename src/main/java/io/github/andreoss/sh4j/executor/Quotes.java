/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.QuoteState;

/**
 * Tracks quoting while a word is scanned (POSIX 2.2.2, 2.2.3): a single
 * quote inside double quotes is an ordinary character, and a double
 * quote inside single quotes is too.
 *
 * @since 1.0
 */
final class Quotes {

    private Quotes() {
    }

    /**
     * Updates the quote state with the next character of a word.
     *
     * @param quotes Quote state of the word so far
     * @param chr The next character
     * @return True when the character is a quote or is single-quoted, so
     *  it is copied as is
     */
    static boolean literal(final QuoteState quotes, final char chr) {
        boolean result = true;
        if (quotes.single()) {
            if (chr == '\'') {
                quotes.leaveSingle();
            }
        } else if (chr == '\'' && !quotes.doubled()) {
            quotes.enterSingle();
        } else if (chr == '"' && quotes.doubled()) {
            quotes.leaveDouble();
        } else if (chr == '"') {
            quotes.enterDouble();
        } else {
            result = false;
        }
        return result;
    }
}
