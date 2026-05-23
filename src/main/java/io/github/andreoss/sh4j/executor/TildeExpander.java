/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;

/**
 * Handles tilde expansion (POSIX 2.6.1): ~ and ~+.
 *
 * @since 1.0
 */
final class TildeExpander {

    private TildeExpander() {
    }

    /**
     * Expands a leading tilde-prefix. The result is treated as if quoted:
     * it is neither field split nor taken as a pattern.
     *
     * @param arg The word to expand
     * @param env Environment view (HOME, PWD)
     * @return The word with the leading tilde-prefix resolved
     */
    static String expandTilde(final String arg, final Map<String, String> env) {
        final String result;
        if (arg.isEmpty() || arg.charAt(0) != '~') {
            result = arg;
        } else {
            final int slash = arg.indexOf('/');
            final String tildepart;
            final String rest;
            if (slash >= 0) {
                tildepart = arg.substring(0, slash);
                rest = arg.substring(slash);
            } else {
                tildepart = arg;
                rest = "";
            }
            if ("~".equals(tildepart)) {
                result = TildeExpander.quoted(env.getOrDefault("HOME", "/root")) + rest;
            } else if ("~+".equals(tildepart)) {
                result = TildeExpander.quoted(
                    env.getOrDefault("PWD", System.getProperty("user.dir"))
                ) + rest;
            } else {
                result = arg;
            }
        }
        return result;
    }

    private static String quoted(final String value) {
        final StringBuilder out = new StringBuilder(value.length());
        for (int idx = 0; idx < value.length(); idx = idx + 1) {
            final char chr = value.charAt(idx);
            if (!Character.isLetterOrDigit(chr) && "/._-".indexOf(chr) < 0) {
                out.append('\\');
            }
            out.append(chr);
        }
        return out.toString();
    }
}
