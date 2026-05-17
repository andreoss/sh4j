/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.QuoteState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Handles IFS field splitting (POSIX 2.6.5). IFS white space next to a
 * non-white-space IFS character belongs to that delimiter, and quoted or
 * escaped characters never delimit.
 *
 * @since 1.0
 */
final class FieldSplitter {

    /**
     * Marks the next character as literal text of the word, which never
     * delimits a field; quote removal drops the mark.
     */
    static final char MARK = '';

    /**
     * Ends a field between two positional parameters of a double-quoted
     * {@code $@}; field splitting drops it (POSIX 2.5.2).
     */
    static final char BREAK = '';

    /** The text to split. */
    private final String value;

    /** The IFS white-space characters. */
    private final String white;

    /** The other IFS characters. */
    private final String other;

    /**
     * Ctor.
     *
     * @param value The text to split
     * @param white The IFS white-space characters
     * @param other The other IFS characters
     */
    private FieldSplitter(final String value, final String white, final String other) {
        this.value = value;
        this.white = white;
        this.other = other;
    }

    /**
     * Splits a value into fields according to IFS.
     *
     * @param value The post-expansion text
     * @param ifs   The IFS value (each character is a separator)
     * @return One field per non-IFS run
     */
    static List<String> splitFields(final String value, final String ifs) {
        final String text = new Breaks(value).closed();
        final List<String> result;
        if ((ifs == null || ifs.isEmpty()) && text.indexOf(FieldSplitter.BREAK) < 0) {
            result = Collections.singletonList(value);
        } else {
            final String chars = Objects.toString(ifs, "");
            final StringBuilder wsbuf = new StringBuilder();
            final StringBuilder nwsbuf = new StringBuilder();
            for (int idx = 0; idx < chars.length(); idx = idx + 1) {
                final char chr = chars.charAt(idx);
                if (chr == ' ' || chr == '\t' || chr == '\n') {
                    wsbuf.append(chr);
                } else {
                    nwsbuf.append(chr);
                }
            }
            result = new FieldSplitter(text, wsbuf.toString(), nwsbuf.toString()).fields();
        }
        return result;
    }

    private List<String> fields() {
        final List<String> result = new ArrayList<>(0);
        int pos = this.skip(0);
        while (pos < this.value.length()) {
            final int end = this.end(pos);
            result.add(this.value.substring(pos, end));
            pos = this.delimiter(end);
        }
        return Collections.unmodifiableList(result);
    }

    private int end(final int start) {
        final QuoteState quotes = new QuoteState();
        int pos = start;
        while (pos < this.value.length() && !this.delimits(quotes, pos)) {
            final char chr = this.value.charAt(pos);
            if (!Quotes.literal(quotes, chr) && (chr == '\\' || chr == FieldSplitter.MARK)) {
                pos = pos + 1;
            }
            pos = pos + 1;
        }
        return Math.min(pos, this.value.length());
    }

    private boolean delimits(final QuoteState quotes, final int pos) {
        return !quotes.single() && !quotes.doubled()
            && this.separator(this.value.charAt(pos));
    }

    private boolean separator(final char chr) {
        return chr == FieldSplitter.BREAK
            || this.white.indexOf(chr) >= 0 || this.other.indexOf(chr) >= 0;
    }

    private int delimiter(final int start) {
        int pos = this.skip(start);
        if (pos < this.value.length() && this.value.charAt(pos) == FieldSplitter.BREAK) {
            pos = pos + 1;
        } else if (pos < this.value.length()
            && this.other.indexOf(this.value.charAt(pos)) >= 0) {
            pos = this.skip(pos + 1);
        }
        return pos;
    }

    private int skip(final int start) {
        int pos = start;
        while (pos < this.value.length() && this.white.indexOf(this.value.charAt(pos)) >= 0) {
            pos = pos + 1;
        }
        return pos;
    }
}
