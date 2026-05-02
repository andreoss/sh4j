/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.Nesting;
import io.github.andreoss.sh4j.lexer.QuoteState;

/**
 * Rewrites backquoted command substitutions in a word as {@code $(...)}
 * (POSIX 2.6.3), leaving quoted text and other expansions untouched.
 *
 * @since 1.0
 */
final class Backquotes {

    /** The word to rewrite. */
    private final String text;

    /**
     * Ctor.
     *
     * @param text The word to rewrite
     */
    Backquotes(final String text) {
        this.text = text;
    }

    /**
     * Rewrites the word.
     *
     * @return The word with every backquoted substitution as {@code $(...)}
     */
    String normalised() {
        final StringBuilder result = new StringBuilder(this.text.length());
        final QuoteState quotes = new QuoteState();
        int idx = 0;
        while (idx < this.text.length()) {
            final char chr = this.text.charAt(idx);
            if (Quotes.literal(quotes, chr)) {
                result.append(chr);
                idx = idx + 1;
            } else if (chr == '\\') {
                final int end = Math.min(idx + 2, this.text.length());
                result.append(this.text, idx, end);
                idx = end;
            } else if (chr == '$') {
                idx = idx + this.dollar(idx, result, quotes.doubled());
            } else if (chr == '`') {
                idx = idx + this.rewrite(idx, result, quotes.doubled());
            } else {
                result.append(chr);
                idx = idx + 1;
            }
        }
        return result.toString();
    }

    private int dollar(final int idx, final StringBuilder result, final boolean quoted) {
        int end = -1;
        if (this.text.startsWith("$(", idx)) {
            end = new Nesting(this.text).paren(idx + 2);
        } else if (this.text.startsWith("${", idx)) {
            end = new Nesting(this.text).brace(idx + 2, quoted);
        }
        final int consumed;
        if (end > 0) {
            result.append(this.text, idx, end);
            consumed = end - idx;
        } else {
            result.append('$');
            consumed = 1;
        }
        return consumed;
    }

    private static void unescape(
        final StringBuilder content, final char next, final boolean quoted
    ) {
        if ("`$\\".indexOf(next) < 0 && !(quoted && next == '"')) {
            content.append('\\');
        }
        content.append(next);
    }

    private int rewrite(final int idx, final StringBuilder result, final boolean quoted) {
        int cur = idx + 1;
        final StringBuilder content = new StringBuilder();
        boolean found = false;
        while (cur < this.text.length() && !found) {
            final char chr = this.text.charAt(cur);
            if (chr == '\\' && cur + 1 < this.text.length()) {
                Backquotes.unescape(content, this.text.charAt(cur + 1), quoted);
                cur = cur + 2;
            } else if (chr == '`') {
                found = true;
                cur = cur + 1;
            } else {
                content.append(chr);
                cur = cur + 1;
            }
        }
        if (found) {
            result.append("$(").append(content).append(')');
        } else {
            result.append('`').append(content);
        }
        return cur - idx;
    }
}
