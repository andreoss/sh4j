/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.QuoteState;
import java.util.List;

/**
 * Field breaks between positional parameters: a double-quoted {@code $@}
 * inside a parameter expansion joins the parameters with
 * {@link FieldSplitter#BREAK}, and field splitting ends a field at each
 * break (POSIX 2.5.2).
 *
 * @since 1.0
 */
final class Breaks {

    /** The expanded text. */
    private final String value;

    /**
     * Ctor.
     *
     * @param value The expanded text
     */
    Breaks(final String value) {
        this.value = value;
    }

    /**
     * Joins the positional parameters with field breaks.
     *
     * @param ctx The shell holding the positional parameters
     * @return The parameters, without {@code $0}, joined by breaks
     */
    static String joined(final BuiltinContext ctx) {
        final List<String> params = ctx.positionalParamsList();
        return String.join(
            String.valueOf(FieldSplitter.BREAK), params.subList(1, params.size())
        );
    }

    /**
     * Closes the double quotes around each double-quoted break, so that every break
     * sits outside quotes and the quoting of the parameters stays intact.
     *
     * @return The text with each double-quoted break as {@code "BREAK"}
     */
    String closed() {
        final StringBuilder out = new StringBuilder(this.value.length());
        final QuoteState quotes = new QuoteState();
        int pos = 0;
        while (pos < this.value.length()) {
            final char chr = this.value.charAt(pos);
            if (Quotes.literal(quotes, chr)) {
                out.append(chr);
            } else if (chr == '\\') {
                out.append(this.value, pos, Math.min(pos + 2, this.value.length()));
                pos = pos + 1;
            } else if (chr == FieldSplitter.BREAK && quotes.doubled()) {
                out.append('"').append(chr).append('"');
            } else {
                out.append(chr);
            }
            pos = pos + 1;
        }
        return out.toString();
    }
}
