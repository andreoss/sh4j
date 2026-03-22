/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Handles pathname expansion (POSIX 2.6.6): glob pattern matching.
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
        final boolean dotpattern;
        if (slash < 0) {
            prefix = "";
            segment = pattern;
        } else {
            prefix = pattern.substring(0, slash);
            segment = pattern.substring(slash + 1);
        }
        dotpattern = segment.startsWith(".");
        final File dir = resolveGlobDir(prefix, cwd);
        final String[] files = dir.list();
        final List<String> result;
        final PathMatcher matcher = createPathMatcher(segment);
        if (files == null || matcher == null) {
            result = new ArrayList<>(0);
        } else {
            result = collectMatches(files, prefix, dotpattern, matcher);
        }
        return result;
    }

    static boolean globMatches(final String pattern, final String str) {
        return str.matches(GlobExpander.globPatternToRegex(pattern));
    }

    static List<String> pathnameExpand(final List<String> fields) {
        return pathnameExpand(fields, new File(System.getProperty("user.dir")));
    }

    static List<String> pathnameExpand(
        final List<String> fields, final File cwd
    ) {
        final List<String> result = new ArrayList<>(0);
        for (final String field : fields) {
            if (containsGlobChars(field)) {
                final List<String> matches = globFiles(field, cwd);
                if (matches.isEmpty()) {
                    result.add(field);
                } else {
                    Collections.sort(matches);
                    result.addAll(matches);
                }
            } else {
                result.add(field);
            }
        }
        return result;
    }

    static boolean containsGlobChars(final String str) {
        return str.indexOf('*') >= 0 || str.indexOf('?') >= 0 || str.indexOf('[') >= 0;
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

    private static PathMatcher createPathMatcher(final String segment) {
        PathMatcher result;
        try {
            result = FileSystems.getDefault().getPathMatcher(
                String.format("glob:%s", segment)
            );
        } catch (final PatternSyntaxException ignored) {
            result = GlobExpander.noMatcher();
        }
        return result;
    }

    private static PathMatcher noMatcher() {
        return null;
    }

    private static List<String> collectMatches(
        final String[] files,
        final String prefix,
        final boolean dotpattern,
        final PathMatcher matcher
    ) {
        final List<String> matches = new ArrayList<>(0);
        for (final String name : files) {
            if (name.startsWith(".") && !dotpattern) {
                continue;
            }
            if (matcher.matches(Paths.get(name))) {
                if (prefix.isEmpty()) {
                    matches.add(name);
                } else {
                    matches.add(String.format("%s/%s", prefix, name));
                }
            }
        }
        return matches;
    }

    private static String globPatternToRegex(final String pattern) {
        final StringBuilder regex = new StringBuilder();
        int idx = 0;
        while (idx < pattern.length()) {
            idx = appendRegexForChar(pattern, idx, regex);
        }
        return regex.toString();
    }

    private static int appendRegexForChar(
        final String pattern, final int idx, final StringBuilder regex
    ) {
        final char chr = pattern.charAt(idx);
        final int next;
        if (chr == '*') {
            regex.append(".*");
            next = idx + 1;
        } else if (chr == '?') {
            regex.append('.');
            next = idx + 1;
        } else if (isRegexSpecial(chr)) {
            regex.append('\\').append(chr);
            next = idx + 1;
        } else if (chr == '[') {
            next = appendCharacterClass(pattern, idx, regex);
        } else {
            regex.append(chr);
            next = idx + 1;
        }
        return next;
    }

    private static boolean isRegexSpecial(final char chr) {
        return ".()+^${}|\\".indexOf(chr) >= 0;
    }

    private static int appendCharacterClass(
        final String pattern, final int idx, final StringBuilder regex
    ) {
        final int end = pattern.indexOf(']', idx + 1);
        final int next;
        if (end <= idx + 1) {
            regex.append("\\[");
            next = idx + 1;
        } else {
            final String body = pattern.substring(idx + 1, end);
            if (body.startsWith("!") && body.length() > 1) {
                regex.append("[^").append(body.substring(1)).append(']');
            } else {
                regex.append('[').append(body).append(']');
            }
            next = end + 1;
        }
        return next;
    }
}
