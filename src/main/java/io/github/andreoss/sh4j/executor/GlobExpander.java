/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles pathname expansion (POSIX 2.6.6). Fields arrive in the escaped
 * pattern form of {@link QuoteRemover#pattern}.
 *
 * @since 1.0
 */
final class GlobExpander {

    private GlobExpander() {
    }

    static List<String> globFiles(final String pattern, final File cwd) {
        return new GlobWalk(Arrays.asList(pattern.split("/", -1)), cwd).matches();
    }

    /**
     * Matches a pattern in which a backslash is an ordinary character.
     *
     * @param pattern The pattern
     * @param str The string to match
     * @return True when the whole string matches
     */
    static boolean globMatches(final String pattern, final String str) {
        return new GlobPattern(GlobPattern.plain(pattern)).matches(str);
    }

    /**
     * Matches a pattern in the escaped form of {@link QuoteRemover#pattern}.
     *
     * @param pattern The escaped pattern
     * @param str The string to match
     * @return True when the whole string matches
     */
    static boolean escapedMatches(final String pattern, final String str) {
        return new GlobPattern(pattern).matches(str);
    }

    static List<String> pathnameExpand(final List<String> fields) {
        return pathnameExpand(fields, new File(System.getProperty("user.dir")));
    }

    static List<String> pathnameExpand(
        final List<String> fields, final File cwd
    ) {
        final List<String> result = new ArrayList<>(0);
        for (final String field : fields) {
            final GlobPattern glob = new GlobPattern(field);
            final List<String> matches;
            if (glob.special()) {
                matches = globFiles(field, cwd);
            } else {
                matches = new ArrayList<>(0);
            }
            if (matches.isEmpty()) {
                result.add(glob.literal());
            } else {
                Collections.sort(matches);
                result.addAll(matches);
            }
        }
        return result;
    }

    static boolean containsGlobChars(final String str) {
        return new GlobPattern(GlobPattern.plain(str)).special();
    }
}
