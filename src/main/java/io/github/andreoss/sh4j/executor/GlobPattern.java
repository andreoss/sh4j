/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Pattern matching notation (POSIX 2.13) translated to a regular expression.
 * A backslash makes the next character literal, which is how quoted pattern
 * characters reach the matcher.
 *
 * @since 1.0
 */
final class GlobPattern {

    /** The pattern text. */
    private final String text;

    /**
     * Ctor.
     *
     * @param text The pattern, with backslash-escaped literal characters
     */
    GlobPattern(final String text) {
        this.text = text;
    }

    /**
     * Checks whether the pattern matches the whole string.
     *
     * @param str The string to match
     * @return True when the pattern matches
     */
    boolean matches(final String str) {
        boolean result;
        try {
            result = Pattern.compile(this.regex(), Pattern.DOTALL).matcher(str).matches();
        } catch (final PatternSyntaxException ignored) {
            result = false;
        }
        return result;
    }

    /**
     * Checks whether the pattern has an unescaped special character.
     *
     * @return True when the pattern contains {@code *}, {@code ?} or {@code [}
     */
    boolean special() {
        boolean found = false;
        int idx = 0;
        while (!found && idx < this.text.length()) {
            final char chr = this.text.charAt(idx);
            found = chr == '*' || chr == '?' || chr == '[';
            idx = this.skip(idx);
        }
        return found;
    }

    /**
     * Removes the escaping backslashes.
     *
     * @return The literal text the pattern stands for
     */
    String literal() {
        final StringBuilder out = new StringBuilder(this.text.length());
        int idx = 0;
        while (idx < this.text.length()) {
            final int next = this.skip(idx);
            out.append(this.text.charAt(next - 1));
            idx = next;
        }
        return out.toString();
    }

    /**
     * Escapes every backslash so that the text has no escapes.
     *
     * @param raw Pattern where a backslash is an ordinary character
     * @return The pattern in escaped form
     */
    static String plain(final String raw) {
        return raw.replace("\\", "\\\\");
    }

    private String regex() {
        final StringBuilder out = new StringBuilder(this.text.length() * 4);
        int idx = 0;
        while (idx < this.text.length()) {
            final char chr = this.text.charAt(idx);
            if (chr == '*') {
                out.append(".*");
                idx = idx + 1;
            } else if (chr == '?') {
                out.append('.');
                idx = idx + 1;
            } else if (chr == '[') {
                idx = this.bracket(idx, out);
            } else {
                idx = this.skip(idx);
                out.append(GlobPattern.quote(this.text.charAt(idx - 1)));
            }
        }
        return out.toString();
    }

    private int bracket(final int start, final StringBuilder out) {
        final StringBuilder set = new StringBuilder(16);
        int idx = start + 1;
        if (idx < this.text.length() && "!^".indexOf(this.text.charAt(idx)) >= 0) {
            set.append('^');
            idx = idx + 1;
        }
        final int first = idx;
        boolean closed = false;
        while (!closed && idx >= 0 && idx < this.text.length()) {
            closed = this.text.charAt(idx) == ']' && idx > first;
            if (closed) {
                idx = idx + 1;
            } else {
                idx = this.element(idx, set);
            }
        }
        final int next;
        if (closed) {
            out.append('[').append(set).append(']');
            next = idx;
        } else {
            out.append(GlobPattern.quote('['));
            next = start + 1;
        }
        return next;
    }

    private int element(final int idx, final StringBuilder set) {
        final int next;
        if (this.text.startsWith("[:", idx)) {
            next = this.named(idx, set);
        } else {
            final int after = this.skip(idx);
            final char low = this.text.charAt(after - 1);
            if (after + 1 < this.text.length() && this.text.charAt(after) == '-'
                && this.text.charAt(after + 1) != ']') {
                final int end = this.skip(after + 1);
                set.append(GlobPattern.quote(low)).append('-')
                    .append(GlobPattern.quote(this.text.charAt(end - 1)));
                next = end;
            } else {
                set.append(GlobPattern.quote(low));
                next = after;
            }
        }
        return next;
    }

    private int named(final int idx, final StringBuilder set) {
        final int end = this.text.indexOf(":]", idx + 2);
        final int next;
        if (end < 0) {
            set.append(GlobPattern.quote('['));
            next = idx + 1;
        } else {
            final String name = this.text.substring(idx + 2, end);
            if (" alpha digit upper lower alnum space punct blank cntrl graph print xdigit "
                .contains(String.format(" %s ", name))) {
                set.append("\\p{").append(GlobPattern.property(name)).append('}');
                next = end + 2;
            } else {
                next = -1;
            }
        }
        return next;
    }

    private int skip(final int idx) {
        final int next;
        if (this.text.charAt(idx) == '\\' && idx + 1 < this.text.length()) {
            next = idx + 2;
        } else {
            next = idx + 1;
        }
        return next;
    }

    private static String property(final String name) {
        final String result;
        if ("xdigit".equals(name)) {
            result = "XDigit";
        } else {
            result = String.join(
                "", name.substring(0, 1).toUpperCase(Locale.ROOT), name.substring(1)
            );
        }
        return result;
    }

    private static String quote(final char chr) {
        return String.format("\\x{%x}", (int) chr);
    }
}
