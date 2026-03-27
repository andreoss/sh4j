/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits one input line of {@code read} into fields by IFS.
 *
 * @since 1.0
 */
final class ReadSplitter {

    /** The line being split. */
    private final String line;

    /** The IFS characters. */
    private final String ifs;

    /** Fields found so far. */
    private final List<String> fields;

    /** Index of the next character. */
    private int pos;

    /** Index where the field being built starts. */
    private int start;

    /**
     * Ctor.
     *
     * @param line The line being split
     * @param ifs The IFS characters, at least one
     */
    ReadSplitter(final String line, final String ifs) {
        this.line = line;
        this.ifs = ifs;
        this.fields = new ArrayList<>(0);
        this.pos = 0;
        this.start = 0;
    }

    /**
     * Splits the line.
     *
     * @return The fields in order
     */
    List<String> split() {
        this.skipBlanks();
        this.start = this.pos;
        while (this.pos < this.line.length()) {
            this.step(this.line.charAt(this.pos));
            this.pos += 1;
        }
        if (!this.trailingBlank() || this.fields.isEmpty()) {
            this.fields.add(this.line.substring(this.start));
        }
        return this.fields;
    }

    private static boolean blank(final char chr) {
        return chr == ' ' || chr == '\t';
    }

    private void skipBlanks() {
        while (this.pos < this.line.length()
            && this.ifs.indexOf(this.line.charAt(this.pos)) >= 0
            && ReadSplitter.blank(this.line.charAt(this.pos))) {
            this.pos += 1;
        }
    }

    private void step(final char chr) {
        final char first = this.ifs.charAt(0);
        if (chr == first && !ReadSplitter.blank(first)) {
            this.cut();
        } else if (this.ifs.indexOf(chr) >= 0) {
            this.cut();
            this.skipSeparators();
            this.start = this.pos + 1;
        }
    }

    private void cut() {
        this.fields.add(this.line.substring(this.start, this.pos));
        this.start = this.pos + 1;
    }

    private void skipSeparators() {
        while (this.pos + 1 < this.line.length()
            && this.ifs.indexOf(this.line.charAt(this.pos + 1)) >= 0) {
            this.pos += 1;
        }
    }

    private boolean trailingBlank() {
        return this.start >= this.pos && this.pos > 0
            && ReadSplitter.blank(this.line.charAt(this.pos - 1));
    }
}
