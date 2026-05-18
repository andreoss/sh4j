/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.Nesting;
import java.util.function.UnaryOperator;

/**
 * Body of an unquoted here-document (POSIX 2.7.4): expanded as if it were
 * in double quotes, except that a double quote of the body is an ordinary
 * character.
 *
 * @since 1.0
 */
final class HereText {

    /** The body. */
    private final String text;

    /**
     * Ctor.
     *
     * @param text The body
     */
    HereText(final String text) {
        this.text = text;
    }

    /**
     * Expands the body.
     *
     * @param expander Word expansion that keeps the quotes of the word
     * @return The expanded body
     */
    String expanded(final UnaryOperator<String> expander) {
        return QuoteRemover.removeQuotes(expander.apply(this.word()));
    }

    /**
     * Rewrites the body as a double-quoted word: each double quote of the
     * body outside expansions is escaped, and so is a backslash before it.
     *
     * @return The double-quoted word
     */
    String word() {
        final StringBuilder out = new StringBuilder(this.text.length() + 2).append('"');
        final Nesting nesting = new Nesting(this.text);
        int idx = 0;
        while (idx < this.text.length()) {
            final char chr = this.text.charAt(idx);
            final int end;
            if (this.text.startsWith("\\\"", idx)) {
                out.append("\\\\");
                end = idx + 1;
            } else if (chr == '\\' || chr == '$' || chr == '`') {
                end = nesting.skip(idx, true);
                out.append(this.text, idx, end);
            } else {
                if (chr == '"') {
                    out.append('\\');
                }
                out.append(chr);
                end = idx + 1;
            }
            idx = end;
        }
        return out.append('"').toString();
    }
}
