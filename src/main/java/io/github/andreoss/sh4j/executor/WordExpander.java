/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Stateless helpers that implement POSIX 2.6 word expansion: tilde,
 * parameter, command-substitution, arithmetic, field-splitting, pathname,
 * and quote-removal. The methods cooperate with {@link BuiltinContext} to
 * read the executor's environment and signal expansion errors.
 *
 * @since 1.0
 */
public final class WordExpander {

    @GeneratedDefensive
    private WordExpander() {
    }

    /**
     * Full POSIX word expansion: tilde, command/arithmetic substitution,
     * brace expansion, and bare variable references.
     *
     * @param arg The source word
     * @param env Environment view for lookups
     * @param ctx Callbacks into the executor
     * @return The expanded text
     */
    static String expandVariablesImpl(
        final String arg,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        String current = TildeExpander.expandTilde(arg, env);
        current = normaliseBackticks(current);
        final StringBuilder expanded = new StringBuilder();
        final ExpansionContext context = new ExpansionContext(env, ctx, expanded);
        boolean insingle = false;
        int idx = 0;
        while (idx < current.length()) {
            final char chr = current.charAt(idx);
            if (chr == '\'') {
                insingle = !insingle;
                expanded.append(chr);
                idx = idx + 1;
            } else if (insingle) {
                expanded.append(chr);
                idx = idx + 1;
            } else if (chr == '\\' && idx + 1 < current.length()) {
                expanded.append(chr).append(current.charAt(idx + 1));
                idx = idx + 2;
            } else {
                idx = idx + VariableExpander.appendOneExpansion(current, idx, context);
            }
        }
        return expanded.toString();
    }

    /**
     * Tilde-expansion (POSIX 2.6.1): {@code ~}, {@code ~+}.
     * Delegates to {@link TildeExpander}.
     *
     * @param arg The word to expand
     * @param env Environment view (HOME, PWD)
     * @return The word with the leading tilde resolved
     */
    static String expandTilde(final String arg, final Map<String, String> env) {
        return TildeExpander.expandTilde(arg, env);
    }

    /**
     * Rewrites backtick command substitutions as {@code $(...)} so they
     * can be handled by the rest of the expansion pipeline uniformly.
     *
     * @param arg The input word
     * @return The rewritten word
     */
    static String normaliseBackticks(final String arg) {
        final StringBuilder result = new StringBuilder();
        int idx = 0;
        boolean single = false;
        while (idx < arg.length()) {
            final char chr = arg.charAt(idx);
            if (chr == '\'') {
                single = !single;
                result.append(chr);
                idx = idx + 1;
            } else if (single) {
                result.append(chr);
                idx = idx + 1;
            } else {
                final int consumed = normaliseOne(arg, idx, result);
                idx = idx + consumed;
            }
        }
        return result.toString();
    }

    /**
     * Escapes characters in expansion-produced data so that downstream
     * {@link QuoteRemover#removeQuotes} treats them as literal. POSIX 2.6 says quote
     * removal acts only on quotes that were in the original word, not on
     * data produced by expansions. Doubles every backslash and prefixes
     * every literal single/double quote with a backslash.
     *
     * @param value Expansion-produced text
     * @return The escaped text safe to feed back through removeQuotes
     */
    static String escapeForQuoteRemoval(final String value) {
        final String result;
        if (value == null || value.isEmpty()) {
            result = value;
        } else {
            final StringBuilder out = new StringBuilder(value.length() + 4);
            for (int idx = 0; idx < value.length(); idx = idx + 1) {
                final char chr = value.charAt(idx);
                if (chr == '\\' || chr == '\'' || chr == '"') {
                    out.append('\\');
                }
                out.append(chr);
            }
            result = out.toString();
        }
        return result;
    }

    /**
     * Checks whether the word is quoted.
     *
     * @param arg Any word
     * @return True when the word is quoted
     */
    static boolean isQuoted(final String arg) {
        final boolean result;
        if (arg == null || arg.length() < 2) {
            result = false;
        } else {
            final char first = arg.charAt(0);
            final char last = arg.charAt(arg.length() - 1);
            result = first == '"' && last == '"' || first == '\'' && last == '\'';
        }
        return result;
    }

    /**
     * Field splitting (POSIX 2.6.5). Delegates to {@link FieldSplitter}.
     *
     * @param value The post-expansion text
     * @param ifs   The IFS value (each character is a separator)
     * @return One field per non-IFS run
     */
    static List<String> splitFields(final String value, final String ifs) {
        return FieldSplitter.splitFields(value, ifs);
    }

    /**
     * Quote removal (POSIX 2.6.7). Delegates to {@link QuoteRemover}.
     *
     * @param word The source word
     * @return The de-quoted text
     */
    public static String removeQuotes(final String word) {
        return QuoteRemover.removeQuotes(word);
    }

    /**
     * Pathname expansion (POSIX 2.6.6): replaces any field containing
     * glob characters with the alphabetic list of matching files (or the
     * literal field when no match). Delegates to {@link GlobExpander}.
     *
     * @param fields The post-IFS fields
     * @return The post-glob fields
     */
    static List<String> pathnameExpand(final List<String> fields) {
        return GlobExpander.pathnameExpand(fields);
    }

    /**
     * Pathname expansion relative to a specific directory. Delegates to {@link GlobExpander}.
     *
     * @param fields The post-IFS fields
     * @param cwd Directory used for relative patterns
     * @return The post-glob fields
     */
    static List<String> pathnameExpand(
        final List<String> fields, final File cwd
    ) {
        return GlobExpander.pathnameExpand(fields, cwd);
    }

    /**
     * Checks whether the string contains glob characters. Delegates to {@link GlobExpander}.
     *
     * @param str Any string
     * @return True when the string contains glob characters
     */
    static boolean containsGlobChars(final String str) {
        return GlobExpander.containsGlobChars(str);
    }

    /**
     * Checks whether a glob pattern matches a string. Delegates to {@link GlobExpander}.
     *
     * @param pattern A glob pattern
     * @param str     The candidate text
     * @return True when {@code pattern} matches {@code str}
     */
    static boolean globMatches(final String pattern, final String str) {
        return GlobExpander.globMatches(pattern, str);
    }

    /**
     * Full implementation of {@code ${...}} brace expansion.
     * Delegates to {@link BraceExpander}.
     *
     * @param inner The text inside the braces
     * @param env   Environment view
     * @param ctx   Executor callbacks
     * @return The expansion, or null when no operator matches
     */
    static String expandBracedImpl(
        final String inner,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        return BraceExpander.expandBracedImpl(inner, env, ctx);
    }

    private static int normaliseOne(
        final String arg,
        final int idx,
        final StringBuilder result
    ) {
        final char chr = arg.charAt(idx);
        int consumed = 1;
        if (chr == '$' && idx + 1 < arg.length()) {
            final char next = arg.charAt(idx + 1);
            if (next == '(') {
                consumed = copyBalanced(arg, idx + 2, '(', ')', result, arg, idx);
            } else if (next == '{') {
                consumed = copyBalanced(arg, idx + 2, '{', '}', result, arg, idx);
            } else {
                result.append(chr);
            }
        } else if (chr == '`') {
            consumed = rewriteBacktick(arg, idx, result);
        } else {
            result.append(chr);
        }
        return consumed;
    }

    private static int copyBalanced(
        final String arg,
        final int innerstart,
        final char open,
        final char close,
        final StringBuilder result,
        final String full,
        final int chunkstart
    ) {
        final int end = findBalancedClose(arg, innerstart, open, close, 1);
        final int consumed;
        if (end > 0) {
            result.append(full, chunkstart, end);
            consumed = end - chunkstart;
        } else {
            result.append(full.charAt(chunkstart));
            consumed = 1;
        }
        return consumed;
    }

    private static int rewriteBacktick(
        final String arg, final int idx, final StringBuilder result
    ) {
        int cur = idx + 1;
        final StringBuilder content = new StringBuilder();
        boolean found = false;
        while (cur < arg.length()) {
            final char chr = arg.charAt(cur);
            if (chr == '\\' && cur + 1 < arg.length()) {
                final char nextc = arg.charAt(cur + 1);
                if (nextc == '`' || nextc == '$' || nextc == '\\') {
                    content.append(nextc);
                } else {
                    content.append(chr).append(nextc);
                }
                cur = cur + 2;
            } else if (chr == '`') {
                result.append("$(").append(content).append(')');
                cur = cur + 1;
                found = true;
                break;
            } else {
                content.append(chr);
                cur = cur + 1;
            }
        }
        if (!found) {
            result.append('`').append(content);
        }
        return cur - idx;
    }

    private static int findBalancedClose(
        final String arg,
        final int start,
        final char open,
        final char close,
        final int initial
    ) {
        int depth = initial;
        int idx = start;
        while (idx < arg.length() && depth > 0) {
            final char chr = arg.charAt(idx);
            if (chr == open) {
                depth = depth + 1;
            } else if (chr == close) {
                depth = depth - 1;
            }
            idx = idx + 1;
        }
        final int result;
        if (depth == 0) {
            result = idx;
        } else {
            result = -1;
        }
        return result;
    }
}
