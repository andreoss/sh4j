/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Remove smallest or largest prefix or suffix pattern (POSIX 2.6.2). The
 * pattern word is expanded and its quoted characters match literally.
 *
 * @since 1.0
 */
final class PatternRemoval {

    private PatternRemoval() {
    }

    /**
     * Applies a {@code #}, {@code ##}, {@code %} or {@code %%} operator.
     *
     * @param rest The operator followed by the pattern word
     * @param value The parameter value
     * @param context The expansion context
     * @return The value without the matched part, or null for no operator
     */
    static String apply(
        final String rest, final String value, final ExpansionContext context
    ) {
        final String result;
        if (rest.startsWith("##")) {
            result = PatternRemoval.longestPrefix(
                value, PatternRemoval.glob(rest.substring(2), context)
            );
        } else if (rest.startsWith("#")) {
            result = PatternRemoval.shortestPrefix(
                value, PatternRemoval.glob(rest.substring(1), context)
            );
        } else if (rest.startsWith("%%")) {
            result = PatternRemoval.longestSuffix(
                value, PatternRemoval.glob(rest.substring(2), context)
            );
        } else if (rest.startsWith("%")) {
            result = PatternRemoval.shortestSuffix(
                value, PatternRemoval.glob(rest.substring(1), context)
            );
        } else {
            result = null;
        }
        return result;
    }

    private static GlobPattern glob(final String word, final ExpansionContext context) {
        return new GlobPattern(
            QuoteRemover.pattern(context.ctx().expandVariables(word, context.env()))
        );
    }

    private static String longestPrefix(final String value, final GlobPattern glob) {
        int longest = -1;
        for (int idx = 0; idx <= value.length(); idx = idx + 1) {
            if (glob.matches(value.substring(0, idx))) {
                longest = idx;
            }
        }
        final String result;
        if (longest < 0) {
            result = value;
        } else {
            result = value.substring(longest);
        }
        return result;
    }

    private static String shortestPrefix(final String value, final GlobPattern glob) {
        int match = -1;
        for (int idx = 0; idx <= value.length(); idx = idx + 1) {
            if (match < 0 && glob.matches(value.substring(0, idx))) {
                match = idx;
            }
        }
        final String result;
        if (match < 0) {
            result = value;
        } else {
            result = value.substring(match);
        }
        return result;
    }

    private static String longestSuffix(final String value, final GlobPattern glob) {
        int longest = -1;
        for (int idx = 0; idx <= value.length(); idx = idx + 1) {
            if (glob.matches(value.substring(value.length() - idx))) {
                longest = idx;
            }
        }
        final String result;
        if (longest < 0) {
            result = value;
        } else {
            result = value.substring(0, value.length() - longest);
        }
        return result;
    }

    private static String shortestSuffix(final String value, final GlobPattern glob) {
        int match = -1;
        for (int idx = 0; idx <= value.length(); idx = idx + 1) {
            if (match < 0 && glob.matches(value.substring(value.length() - idx))) {
                match = idx;
            }
        }
        final String result;
        if (match < 0) {
            result = value;
        } else {
            result = value.substring(0, value.length() - match);
        }
        return result;
    }
}
