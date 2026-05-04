/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.util.ArrayList;
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
        final int slash = pattern.lastIndexOf('/');
        final String prefix;
        final String segment;
        if (slash < 0) {
            prefix = "";
            segment = pattern;
        } else {
            prefix = new GlobPattern(pattern.substring(0, slash)).literal();
            segment = pattern.substring(slash + 1);
        }
        final String[] files = GlobExpander.resolveGlobDir(prefix, cwd).list();
        final List<String> result;
        if (files == null) {
            result = new ArrayList<>(0);
        } else {
            result = GlobExpander.collectMatches(files, prefix, segment);
        }
        return result;
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

    private static File resolveGlobDir(final String prefix, final File cwd) {
        final File result;
        if (prefix.isEmpty()) {
            result = cwd;
        } else if (prefix.startsWith("/")) {
            result = new File(prefix);
        } else {
            result = new File(cwd, prefix);
        }
        return result;
    }

    private static List<String> collectMatches(
        final String[] files, final String prefix, final String segment
    ) {
        final GlobPattern glob = new GlobPattern(segment);
        final boolean dotpattern = segment.startsWith(".");
        final List<String> matches = new ArrayList<>(0);
        for (final String name : files) {
            if (name.startsWith(".") && !dotpattern) {
                continue;
            }
            if (glob.matches(name)) {
                if (prefix.isEmpty()) {
                    matches.add(name);
                } else {
                    matches.add(String.format("%s/%s", prefix, name));
                }
            }
        }
        return matches;
    }
}
