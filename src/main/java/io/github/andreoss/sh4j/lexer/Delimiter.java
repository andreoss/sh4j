/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Here-document delimiter word as written after {@code <<}.
 *
 * @since 1.0
 */
public final class Delimiter {

    /** The raw delimiter word. */
    private final String word;

    /**
     * Ctor.
     *
     * @param word The raw delimiter word
     */
    public Delimiter(final String word) {
        this.word = word;
    }

    /**
     * Tells whether the delimiter is quoted, which disables expansion.
     *
     * @return True when quoted
     */
    public boolean quoted() {
        return this.word.length() >= 2
            && (Delimiter.isQuoted(this.word, '\'') || Delimiter.isQuoted(this.word, '"')
                || this.word.indexOf('\\') >= 0);
    }

    /**
     * Returns the delimiter line text, unquoted when the word is quoted.
     *
     * @return The delimiter text
     */
    public String text() {
        final String text;
        if (this.quoted()) {
            text = this.unquoted();
        } else {
            text = this.word;
        }
        return text;
    }

    private String unquoted() {
        final StringBuilder out = new StringBuilder(this.word.length());
        boolean escaped = false;
        for (int idx = 0; idx < this.word.length(); idx = idx + 1) {
            final char chr = this.word.charAt(idx);
            if (escaped) {
                out.append(chr);
                escaped = false;
            } else if (chr == '\\') {
                escaped = true;
            } else if (chr != '\'' && chr != '"') {
                out.append(chr);
            }
        }
        return out.toString();
    }

    private static boolean isQuoted(final String word, final char quote) {
        return word.charAt(0) == quote && word.charAt(word.length() - 1) == quote;
    }
}
