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

    private QuoteRemover() {
    }

    static String removeQuotes(final String word) {
        boolean insingle = false;
        boolean indouble = false;
        final StringBuilder result = new StringBuilder();
        int idx = 0;
        while (idx < word.length()) {
            final char chr = word.charAt(idx);
            if (insingle) {
                idx = QuoteRemover.stepSingle(chr, idx, result);
                if (idx < 0) {
                    insingle = false;
                    idx = -idx;
                }
            } else if (indouble) {
                idx = QuoteRemover.stepDouble(word, idx, result);
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
                result.append(word.charAt(idx + 1));
                idx = idx + 2;
            } else {
                result.append(chr);
                idx = idx + 1;
            }
        }
        return result.toString();
    }

    private static boolean isEscaped(final String word, final int idx) {
        return word.charAt(idx) == '\\' && idx + 1 < word.length();
    }

    private static int stepSingle(
        final char chr, final int idx, final StringBuilder result
    ) {
        final int next;
        if (chr == '\'') {
            next = -(idx + 1);
        } else {
            result.append(chr);
            next = idx + 1;
        }
        return next;
    }

    private static int stepDouble(
        final String word, final int idx, final StringBuilder result
    ) {
        return QuoteRemover.appendDoubleQuoted(word, idx, result);
    }

    private static int appendDoubleQuoted(
        final String word, final int idx, final StringBuilder result
    ) {
        final char chr = word.charAt(idx);
        final int next;
        if (chr == '"') {
            next = -(idx + 1);
        } else if (QuoteRemover.isDqEscape(word, idx)) {
            result.append(word.charAt(idx + 1));
            next = idx + 2;
        } else {
            result.append(chr);
            next = idx + 1;
        }
        return next;
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
