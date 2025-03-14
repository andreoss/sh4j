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
import java.util.Map;

public final class WordExpander {

    public record ExpansionResult(String value, boolean error) {}

    /**
     * Performs variable / arithmetic / command substitution / brace expansion on
     * {@code arg} using {@code ctx} for the stateful dependencies. Returns the
     * expanded string AND the error flag as a pure record; the context's
     * wordExpansionError flag is consumed (cleared) so callers can OR the
     * returned {@code error} into their own accumulator without double-counting.
     */
    public static ExpansionResult expandVariables(String arg, Map<String, String> env, BuiltinContext ctx) {
        String value = ctx.expandVariables(arg, env);
        boolean error = ctx.wordExpansionErrorOccurred();
        if (error) ctx.clearWordExpansionError();
        return new ExpansionResult(value, error);
    }


    public static String expandTilde(String arg, Map<String, String> env) {
        if (arg.isEmpty() || arg.charAt(0) != '~') {
            return arg;
        }
        int slash = arg.indexOf('/');
        String tildePart = slash >= 0 ? arg.substring(0, slash) : arg;
        String rest = slash >= 0 ? arg.substring(slash) : "";

        if ("~".equals(tildePart)) {
            return env.getOrDefault("HOME", "/root") + rest;
        }
        if ("~+".equals(tildePart)) {
            return env.getOrDefault("PWD", System.getProperty("user.dir")) + rest;
        }
        return arg;
    }

    public static String normaliseBackticks(String arg) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < arg.length()) {
            char c = arg.charAt(i);
            if (c == '$' && i + 1 < arg.length()) {
                char next = arg.charAt(i + 1);
                if (next == '(') {
                    int depth = 1;
                    int j = i + 2;
                    while (j < arg.length() && depth > 0) {
                        char nc = arg.charAt(j);
                        if (nc == '(') depth++;
                        else if (nc == ')') depth--;
                        j++;
                    }
                    result.append(arg, i, j);
                    i = j;
                    continue;
                } else if (next == '{') {
                    int depth = 1;
                    int j = i + 2;
                    while (j < arg.length() && depth > 0) {
                        char nc = arg.charAt(j);
                        if (nc == '{') depth++;
                        else if (nc == '}') depth--;
                        j++;
                    }
                    if (depth == 0) {
                        result.append(arg, i, j);
                        i = j;
                        continue;
                    }
                }
            }
            if (c == '`') {
                int j = i + 1;
                StringBuilder content = new StringBuilder();
                boolean found = false;
                while (j < arg.length()) {
                    char nc = arg.charAt(j);
                    if (nc == '\\' && j + 1 < arg.length()) {
                        char nextc = arg.charAt(j + 1);
                        if (nextc == '`' || nextc == '$' || nextc == '\\') {
                            content.append(nextc);
                            j += 2;
                        } else {
                            content.append(nc);
                            content.append(nextc);
                            j += 2;
                        }
                    } else if (nc == '`') {
                        result.append("$(");
                        result.append(content);
                        result.append(')');
                        j++;
                        found = true;
                        break;
                    } else {
                        content.append(nc);
                        j++;
                    }
                }
                if (!found) {
                    result.append('`');
                    result.append(content);
                }
                i = j;
                continue;
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    public static List<String> splitFields(String value, String ifs) {
        if (ifs == null || ifs.isEmpty()) {
            return List.of(value);
        }
        String wsChars = "";
        String nwsChars = "";
        for (int i = 0; i < ifs.length(); i++) {
            char c = ifs.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n') wsChars += c;
            else nwsChars += c;
        }
        List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inField = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (nwsChars.indexOf(c) >= 0) {
                result.add(field.toString());
                field = new StringBuilder();
                inField = false;
            } else if (wsChars.indexOf(c) >= 0) {
                if (inField) {
                    result.add(field.toString());
                    field = new StringBuilder();
                    inField = false;
                }
            } else {
                field.append(c);
                inField = true;
            }
        }
        if (inField) {
            result.add(field.toString());
        } else if (!nwsChars.isEmpty() && value.length() > 0
                && nwsChars.indexOf(value.charAt(value.length() - 1)) >= 0) {
            result.add(field.toString());
        }
        return result;
    }

    public static boolean isQuoted(String arg) {
        if (arg == null || arg.length() < 2) return false;
        char first = arg.charAt(0);
        char last = arg.charAt(arg.length() - 1);
        return (first == '"' && last == '"') || (first == '\'' && last == '\'');
    }

    public static String removeQuotes(String word) {
        StringBuilder result = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (inSingle) {
                if (c == '\'') {
                    inSingle = false;
                } else {
                    result.append(c);
                }
            } else if (inDouble) {
                if (c == '"') {
                    inDouble = false;
                } else if (c == '\\' && i + 1 < word.length()
                        && (word.charAt(i + 1) == '$' || word.charAt(i + 1) == '`'
                        || word.charAt(i + 1) == '"' || word.charAt(i + 1) == '\\')) {
                    result.append(word.charAt(i + 1));
                    i++;
                } else {
                    result.append(c);
                }
            } else {
                if (c == '\'') {
                    inSingle = true;
                } else if (c == '"') {
                    inDouble = true;
                } else if (c == '\\' && i + 1 < word.length()) {
                    result.append(word.charAt(i + 1));
                    i++;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    public static List<String> pathnameExpand(List<String> fields) {
        List<String> result = new ArrayList<>();
        for (String field : fields) {
            if (containsGlobChars(field)) {
                List<String> matches = globFiles(field);
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

    public static boolean containsGlobChars(String s) {
        return s.indexOf('*') >= 0 || s.indexOf('?') >= 0 || s.indexOf('[') >= 0;
    }

    static List<String> globFiles(String pattern) {
        File cwd = new File(System.getProperty("user.dir"));
        String[] files = cwd.list();
        if (files == null) return List.of();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<String> matches = new ArrayList<>();
        for (String f : files) {
            if (matcher.matches(Paths.get(f))) {
                matches.add(f);
            }
        }
        return matches;
    }

    public static boolean globMatches(String pattern, String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if (c == '?') {
                sb.append('.');
            } else if (c == '.' || c == '(' || c == ')' || c == '+' || c == '^'
                    || c == '$' || c == '{' || c == '}' || c == '|' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '[') {
                int end = pattern.indexOf(']', i);
                if (end > i) {
                    sb.append(pattern, i, end + 1);
                    i = end;
                } else {
                    sb.append("\\[");
                }
            } else {
                sb.append(c);
            }
        }
        return s.matches(sb.toString());
    }
}
