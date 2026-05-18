/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Finds where quoted text and expansions nested inside a word end
 * (POSIX 2.2.3, 2.3, 2.6.2, 2.6.3). Quotes, backslashes, backquotes and
 * nested expansions are skipped, so a closing character inside any of
 * them does not end the enclosing construct.
 *
 * @since 1.0
 */
public final class Nesting {

    /** Source text. */
    private final String text;

    /**
     * Ctor.
     *
     * @param text Source text
     */
    public Nesting(final String text) {
        this.text = text;
    }

    /**
     * Finds the end of a command substitution.
     *
     * @param start Index just after the opening dollar and parenthesis
     * @return Index just after the matching parenthesis, or -1 when absent
     */
    public int paren(final int start) {
        return new CommandScan(this, this.text, start).close();
    }

    /**
     * Finds the end of a parameter expansion.
     *
     * @param start Index just after the opening dollar and brace
     * @param quoted Whether the expansion sits inside double quotes
     * @return Index just after the matching closing brace, or -1 when absent
     */
    public int brace(final int start, final boolean quoted) {
        int depth = 1;
        int pos = start;
        int result = -1;
        while (pos < this.text.length() && result < 0) {
            final char chr = this.text.charAt(pos);
            if (chr == '}') {
                depth = depth - 1;
                pos = pos + 1;
                if (depth == 0) {
                    result = pos;
                }
            } else if (chr == '{') {
                depth = depth + 1;
                pos = pos + 1;
            } else {
                pos = this.skip(pos, quoted);
            }
        }
        return result;
    }

    /**
     * Skips one character or one quoted or nested construct.
     *
     * @param pos Index of the construct
     * @param quoted Whether the position is inside double quotes
     * @return Index just after the construct, or the text length when it
     *  is not terminated
     */
    public int skip(final int pos, final boolean quoted) {
        final char chr = this.text.charAt(pos);
        final int result;
        if (chr == '\\') {
            result = Math.min(pos + 2, this.text.length());
        } else if (chr == '\'' && !quoted) {
            result = this.found(this.text.indexOf('\'', pos + 1) + 1);
        } else if (chr == '"') {
            result = this.doubled(pos + 1);
        } else if (chr == '`') {
            result = this.backquoted(pos + 1);
        } else if (chr == '$') {
            result = this.dollar(pos + 1, quoted);
        } else {
            result = pos + 1;
        }
        return result;
    }

    private int dollar(final int pos, final boolean quoted) {
        final int result;
        if (this.text.startsWith("(", pos)) {
            result = this.found(this.paren(pos + 1));
        } else if (this.text.startsWith("{", pos)) {
            result = this.found(this.brace(pos + 1, quoted));
        } else {
            result = pos;
        }
        return result;
    }

    private int doubled(final int start) {
        int pos = start;
        int result = -1;
        while (pos < this.text.length() && result < 0) {
            if (this.text.charAt(pos) == '"') {
                result = pos + 1;
            } else {
                pos = this.skip(pos, true);
            }
        }
        return this.found(result);
    }

    private int backquoted(final int start) {
        int pos = start;
        int result = -1;
        while (pos < this.text.length() && result < 0) {
            final char chr = this.text.charAt(pos);
            if (chr == '`') {
                result = pos + 1;
            } else if (chr == '\\') {
                pos = pos + 2;
            } else {
                pos = pos + 1;
            }
        }
        return this.found(result);
    }

    private int found(final int end) {
        final int result;
        if (end > 0) {
            result = end;
        } else {
            result = this.text.length();
        }
        return result;
    }
}
