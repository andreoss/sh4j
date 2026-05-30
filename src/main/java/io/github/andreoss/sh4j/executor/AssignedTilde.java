/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.QuoteState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tilde expansion of an assignment value (POSIX 2.6.1): a tilde-prefix
 * also follows each unquoted colon and ends at an unquoted slash or colon.
 *
 * @since 1.0
 */
final class AssignedTilde {

    private AssignedTilde() {
    }

    /**
     * Expands the tilde-prefixes at the start of the value and after each
     * unquoted colon outside nested substitutions.
     *
     * @param value The raw assignment value
     * @param env Environment view (HOME, PWD)
     * @return The value with its tilde-prefixes resolved
     */
    static String expand(final String value, final Map<String, String> env) {
        final StringBuilder out = new StringBuilder(value.length());
        int start = 0;
        for (final int colon : AssignedTilde.colons(value)) {
            out.append(TildeExpander.expandTilde(value.substring(start, colon), env))
                .append(':');
            start = colon + 1;
        }
        out.append(TildeExpander.expandTilde(value.substring(start), env));
        return out.toString();
    }

    private static List<Integer> colons(final String value) {
        final List<Integer> found = new ArrayList<>(1);
        final QuoteState quotes = new QuoteState();
        int depth = 0;
        boolean tick = false;
        int idx = 0;
        while (idx < value.length()) {
            final char chr = value.charAt(idx);
            int step = 1;
            if (chr == '\\' && !quotes.single()) {
                step = 2;
            } else if (!Quotes.literal(quotes, chr) && !quotes.doubled()) {
                if (chr == '`') {
                    tick = !tick;
                }
                if (chr == ':' && depth == 0 && !tick) {
                    found.add(idx);
                }
                depth = depth + AssignedTilde.nesting(value, idx, depth);
            }
            idx = idx + step;
        }
        return found;
    }

    private static int nesting(final String value, final int idx, final int depth) {
        final char chr = value.charAt(idx);
        final int delta;
        if (chr == '(' || chr == '{' && AssignedTilde.braced(value, idx, depth)) {
            delta = 1;
        } else if (depth > 0 && (chr == ')' || chr == '}')) {
            delta = -1;
        } else {
            delta = 0;
        }
        return delta;
    }

    private static boolean braced(final String value, final int idx, final int depth) {
        return depth > 0 || idx > 0 && value.charAt(idx - 1) == '$';
    }
}
