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
                result = env.getOrDefault("HOME", "/root") + rest;
            } else if ("~+".equals(tildepart)) {
                result = env.getOrDefault("PWD", System.getProperty("user.dir")) + rest;
            } else {
                result = arg;
            }
        }
        return result;
    }
}
