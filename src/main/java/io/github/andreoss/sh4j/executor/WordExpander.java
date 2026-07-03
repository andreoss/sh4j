/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.lexer.QuoteState;
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
        return WordExpander.expand(arg, new ExpansionContext(env, ctx, new Expanded()));
    }

    /**
     * Word expansion of a word that is field split and quote removed
     * afterwards: the word of an unquoted {@code ${p:-word}} keeps its
     * quotes, so its quoted parts are neither split nor globbed.
     *
     * @param arg The source word
     * @param env Environment view for lookups
     * @param ctx Callbacks into the executor
     * @return The expanded text, still carrying the quotes of the word
     */
    static String expandFieldsImpl(
        final String arg,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final ExpansionContext context = new ExpansionContext(env, ctx, new Expanded());
        context.splitting(true);
        return WordExpander.expand(arg, context);
    }

    /**
     * Word expansion of a word that is field split and quote removed
     * afterwards, with every unquoted literal character of the word marked
     * by {@link FieldSplitter#MARK}: field splitting applies only to the
     * results of expansions (POSIX 2.6.5).
     *
     * @param arg The source word
     * @param env Environment view for lookups
     * @param ctx Callbacks into the executor
     * @return The expanded text, still carrying the quotes of the word
     */
    static String expandMarkedImpl(
        final String arg,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final ExpansionContext context = new ExpansionContext(env, ctx, new Expanded());
        context.splitting(true);
        context.marking(true);
        return WordExpander.expand(arg, context);
    }

    /**
     * Word expansion of the double-quoted word of a double-quoted braced
     * expansion whose text is field split later: each {@code $@} of the
     * word joins the positional parameters by {@link FieldSplitter#BREAK}.
     *
     * @param arg The source word, in double quotes
     * @param env Environment view for lookups
     * @param ctx Callbacks into the executor
     * @return The expanded text, still carrying the quotes of the word
     */
    static String expandNestedImpl(
        final String arg,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final ExpansionContext context = new ExpansionContext(env, ctx, new Expanded());
        context.splitting(true);
        context.nested(true);
        return WordExpander.expand(arg, context);
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
        return new Backquotes(arg).normalised();
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
                if (chr == '\\' || chr == '\'' || chr == '"' || chr == FieldSplitter.MARK) {
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
    static String removeQuotes(final String word) {
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

    private static String expand(final String arg, final ExpansionContext context) {
        String current = TildeExpander.expandTilde(arg, context.env());
        current = normaliseBackticks(current);
        final Expanded expanded = context.expanded();
        final QuoteState quotes = new QuoteState();
        int idx = 0;
        while (idx < current.length()) {
            final char chr = current.charAt(idx);
            if (Quotes.literal(quotes, chr)) {
                expanded.append(chr);
                idx = idx + 1;
            } else if (chr == '\\' && idx + 1 < current.length()) {
                expanded.append(chr).append(current.charAt(idx + 1));
                idx = idx + 2;
            } else {
                context.quote(quotes.doubled());
                idx = idx + VariableExpander.appendOneExpansion(current, idx, context);
            }
        }
        return expanded.toString();
    }
}
