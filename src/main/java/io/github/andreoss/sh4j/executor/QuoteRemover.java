/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Handles quote removal (POSIX 2.6.7).
 *
 * @since 1.0
 */
final class QuoteRemover {

    /** Whether quoted pattern characters keep a backslash. */
    private final boolean escaping;

    private QuoteRemover(final boolean escaping) {
        this.escaping = escaping;
    }

    static String removeQuotes(final String word) {
        return new QuoteRemover(false).remove(word);
    }

    /**
     * Removes quotes but escapes the quoted pattern characters with a
     * backslash, so that they match literally (POSIX 2.13.1).
     *
     * @param word The word after expansion
     * @return The pattern in escaped form
     */
    static String pattern(final String word) {
        return new QuoteRemover(true).remove(word);
    }

    private String remove(final String word) {
        boolean insingle = false;
        boolean indouble = false;
        final StringBuilder result = new StringBuilder();
        int idx = 0;
        while (idx < word.length()) {
            final char chr = word.charAt(idx);
            if (insingle) {
                idx = this.stepSingle(chr, idx, result);
                if (idx < 0) {
                    insingle = false;
                    idx = -idx;
                }
            } else if (indouble) {
                idx = this.appendDoubleQuoted(word, idx, result);
                if (idx < 0) {
                    indouble = false;
                    idx = -idx;
                }
            } else if (chr == '\'') {
                insingle = true;
                idx = idx + 1;
            } else if (chr == '"') {
                indouble = true;
                idx = idx + 1;
            } else if (QuoteRemover.isEscaped(word, idx)) {
                this.literal(result, word.charAt(idx + 1));
                idx = idx + 2;
            } else {
                QuoteRemover.unquoted(result, chr);
                idx = idx + 1;
            }
        }
        return result.toString();
    }

    private void literal(final StringBuilder result, final char chr) {
        if (this.escaping && "\\*?[]!-^".indexOf(chr) >= 0) {
            result.append('\\');
        }
        result.append(chr);
    }

    private int stepSingle(
        final char chr, final int idx, final StringBuilder result
    ) {
        final int next;
        if (chr == '\'') {
            next = -(idx + 1);
        } else {
            this.literal(result, chr);
            next = idx + 1;
        }
        return next;
    }

    private int appendDoubleQuoted(
        final String word, final int idx, final StringBuilder result
    ) {
        final char chr = word.charAt(idx);
        final int next;
        if (chr == '"') {
            next = -(idx + 1);
        } else if (QuoteRemover.isDqEscape(word, idx)) {
            this.literal(result, word.charAt(idx + 1));
            next = idx + 2;
        } else {
            this.literal(result, chr);
            next = idx + 1;
        }
        return next;
    }

    private static void unquoted(final StringBuilder result, final char chr) {
        if (chr != FieldSplitter.MARK) {
            result.append(chr);
        }
    }

    private static boolean isEscaped(final String word, final int idx) {
        return word.charAt(idx) == '\\' && idx + 1 < word.length();
    }

    private static boolean isDqEscape(final String word, final int idx) {
        final boolean result;
        if (word.charAt(idx) == '\\') {
            result = QuoteRemover.hasEscapableNext(word, idx);
        } else {
            result = false;
        }
        return result;
    }

    private static boolean hasEscapableNext(final String word, final int idx) {
        final boolean result;
        if (idx + 1 >= word.length()) {
            result = false;
        } else {
            final char nxt = word.charAt(idx + 1);
            result = nxt == '$' || nxt == '`' || nxt == '"' || nxt == '\\';
        }
        return result;
    }
}
