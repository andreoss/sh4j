/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits one input line of {@code read} into the values of its variables
 * by IFS (POSIX read, 2.6.5 Field Splitting). Characters quoted by a
 * backslash never delimit fields.
 *
 * @since 1.0
 */
final class ReadSplitter {

    /** The line being split. */
    private final ReadText line;

    /** The IFS characters. */
    private final String ifs;

    /**
     * Ctor.
     *
     * @param line The line being split
     * @param ifs The IFS characters
     */
    ReadSplitter(final ReadText line, final String ifs) {
        this.line = line;
        this.ifs = ifs;
    }

    /**
     * Splits the line into values for the variables.
     *
     * @param count The number of variables, at least one
     * @return The values in order, exactly {@code count} of them
     */
    List<String> split(final int count) {
        final String text = this.line.text();
        final List<String> values = new ArrayList<>(count);
        int pos = 0;
        while (pos < text.length() && this.blank(pos)) {
            pos = pos + 1;
        }
        while (values.size() < count - 1) {
            final int end = this.field(pos);
            values.add(text.substring(pos, end));
            pos = this.delimiter(end);
        }
        values.add(this.last(pos));
        return values;
    }

    private String last(final int pos) {
        final String text = this.line.text();
        int stop = text.length();
        while (stop > pos && this.blank(stop - 1)) {
            stop = stop - 1;
        }
        final int first = this.field(pos);
        if (this.delimiter(first) == text.length()) {
            stop = first;
        }
        return text.substring(pos, stop);
    }

    private int field(final int pos) {
        int end = pos;
        while (end < this.line.text().length() && !this.separator(end)) {
            end = end + 1;
        }
        return end;
    }

    private int delimiter(final int pos) {
        int end = pos;
        if (end < this.line.text().length()) {
            boolean spaces = this.blank(end);
            end = end + 1;
            while (end < this.line.text().length() && this.separator(end)
                && (spaces || this.blank(end))) {
                spaces = spaces && this.blank(end);
                end = end + 1;
            }
        }
        return end;
    }

    private boolean separator(final int pos) {
        return !this.line.quoted(pos) && this.ifs.indexOf(this.line.text().charAt(pos)) >= 0;
    }

    private boolean blank(final int pos) {
        return this.separator(pos)
            && String.format(" \t%n").indexOf(this.line.text().charAt(pos)) >= 0;
    }
}
