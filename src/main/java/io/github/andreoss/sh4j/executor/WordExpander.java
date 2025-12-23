/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

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
        String current = expandTilde(arg, env);
        current = normaliseBackticks(current);
        final StringBuilder expanded = new StringBuilder();
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
                idx = idx + appendOneExpansion(current, idx, env, ctx, expanded);
            }
        }
        return expanded.toString();
    }

    private static int appendOneExpansion(
        final String arg,
        final int idx,
        final Map<String, String> env,
        final BuiltinContext ctx,
        final StringBuilder expanded
    ) {
        final char chr = arg.charAt(idx);
        int consumed = 1;
        if (chr == '$' && idx + 1 < arg.length()) {
            final char next = arg.charAt(idx + 1);
            final String special = ctx.getSpecialParam(next);
            if (special != null) {
                expanded.append(WordExpander.escapeForQuoteRemoval(special));
                consumed = 2;
            } else if (next == '(') {
                consumed = appendDollarParen(arg, idx, env, ctx, expanded);
            } else if (next == '{' && idx + 2 < arg.length()) {
                consumed = appendDollarBrace(arg, idx, env, ctx, expanded);
            } else if (isIdentifierStart(next)) {
                int end = idx + 2;
                while (end < arg.length() && isIdentifierPart(arg.charAt(end))) {
                    end = end + 1;
                }
                final String value = ctx.lookupVariable(
                    arg.substring(idx + 1, end), env
                );
                expanded.append(
                    WordExpander.escapeForQuoteRemoval(value == null ? "" : value)
                );
                consumed = end - idx;
            } else {
                expanded.append(chr);
            }
        } else {
            expanded.append(chr);
        }
        return consumed;
    }

    private static boolean isIdentifierStart(final char chr) {
        return chr == '_' || chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z';
    }

    private static boolean isIdentifierPart(final char chr) {
        return isIdentifierStart(chr) || chr >= '0' && chr <= '9';
    }

    private static int appendDollarParen(
        final String arg,
        final int idx,
        final Map<String, String> env,
        final BuiltinContext ctx,
        final StringBuilder expanded
    ) {
        int consumed = 1;
        if (idx + 2 < arg.length() && arg.charAt(idx + 2) == '(') {
            final int end = findBalancedClose(arg, idx + 3, '(', ')', 2);
            if (end > 0) {
                expanded.append(
                    WordExpander.escapeForQuoteRemoval(
                        ctx.evalArithmetic(
                            WordExpander.removeQuotes(
                                WordExpander.expandVariablesImpl(
                                    arg.substring(idx + 3, end - 2), env, ctx
                                )
                            ), env
                        )
                    )
                );
                consumed = end - idx;
            } else {
                expanded.append(arg.charAt(idx));
            }
        } else {
            final int end = findBalancedClose(arg, idx + 2, '(', ')', 1);
            if (end > 0) {
                expanded.append(
                    WordExpander.escapeForQuoteRemoval(
                        ctx.executeCommandSubstitution(arg.substring(idx + 2, end - 1))
                    )
                );
                consumed = end - idx;
            } else {
                expanded.append(arg.charAt(idx));
            }
        }
        return consumed;
    }

    private static int findMatchingBrace(final String arg, final int start) {
        int depth = 1;
        int pos = start;
        while (pos < arg.length()) {
            final char chr = arg.charAt(pos);
            if (chr == '{' && pos > 0 && arg.charAt(pos - 1) == '$') {
                depth = depth + 1;
            } else if (chr == '{') {
                depth = depth + 1;
            } else if (chr == '}') {
                depth = depth - 1;
                if (depth == 0) {
                    return pos;
                }
            }
            pos = pos + 1;
        }
        return -1;
    }

    private static int appendDollarBrace(
        final String arg,
        final int idx,
        final Map<String, String> env,
        final BuiltinContext ctx,
        final StringBuilder expanded
    ) {
        int consumed = 1;
        final int close = findMatchingBrace(arg, idx + 2);
        if (close > idx + 2) {
            final String replacement = WordExpander.expandBracedImpl(
                arg.substring(idx + 2, close), env, ctx
            );
            if (replacement == null) {
                expanded.append(arg.charAt(idx));
            } else {
                expanded.append(WordExpander.escapeForQuoteRemoval(replacement));
                consumed = close - idx + 1;
            }
        } else {
            expanded.append(arg.charAt(idx));
        }
        return consumed;
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

    private static boolean isSpecialParamChar(final char chr) {
        return chr == '?' || chr == '-' || chr == '$' || chr == '!'
            || chr == '@' || chr == '*';
    }

    /**
     * Full implementation of {@code ${...}} brace expansion.
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
        if (inner.isEmpty()) {
            return null;
        }
        if (inner.charAt(0) == '#') {
            if (inner.length() == 1) {
                return ctx.getSpecialParam('#');
            }
            final String name = inner.substring(1);
            if ("*".equals(name) || "@".equals(name)) {
                return String.valueOf(ctx.positionalParamsList().size() - 1);
            }
            final String val = ctx.lookupVariable(name, env);
            final String result;
            if (val == null) {
                result = "0";
            } else {
                result = String.valueOf(val.length());
            }
            return result;
        }
        final int nameend = findNameEnd(inner);
        if (nameend == 0) {
            return null;
        }
        final String varname = inner.substring(0, nameend);
        final String rest = inner.substring(nameend);
        final String varvalue = ctx.lookupVariable(varname, env);
        if (rest.isEmpty()) {
            final String result;
            if (varvalue == null) {
                result = "";
            } else {
                result = varvalue;
            }
            return result;
        }
        final String defaulted = applyDefaultOps(rest, varvalue, varname, env, ctx);
        if (defaulted != null) {
            return defaulted;
        }
        final String erred = applyErrorOps(rest, varvalue, varname, ctx);
        if (erred != null) {
            return erred;
        }
        if (varvalue == null) {
            return "";
        }
        return applyPatternOps(rest, varvalue);
    }

    private static int findNameEnd(final String inner) {
        int nameend = 0;
        final char first = inner.charAt(0);
        if (isSpecialParamChar(first) || first == '_' || Character.isLetter(first)) {
            nameend = 1;
            if (!isSpecialParamChar(first)) {
                while (nameend < inner.length()
                    && (Character.isLetterOrDigit(inner.charAt(nameend))
                        || inner.charAt(nameend) == '_')) {
                    nameend = nameend + 1;
                }
            }
        } else if (Character.isDigit(first)) {
            while (nameend < inner.length() && Character.isDigit(inner.charAt(nameend))) {
                nameend = nameend + 1;
            }
        }
        return nameend;
    }

    private static String applyDefaultOps(
        final String rest,
        final String varvalue,
        final String varname,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (rest.startsWith(":-")) {
            result = defaultOnEmpty(varvalue, rest.substring(2), env, ctx);
        } else if (rest.startsWith("-")) {
            result = defaultOnNull(varvalue, rest.substring(1), env, ctx);
        } else if (rest.startsWith(":=")) {
            result = assignOnEmpty(varvalue, varname, rest.substring(2), env, ctx);
        } else if (rest.startsWith("=")) {
            result = assignOnNull(varvalue, varname, rest.substring(1), env, ctx);
        } else if (rest.startsWith(":+")) {
            result = altOnNonEmpty(varvalue, rest.substring(2), env, ctx);
        } else if (rest.startsWith("+")) {
            result = altOnNonNull(varvalue, rest.substring(1), env, ctx);
        } else {
            result = null;
        }
        return result;
    }

    private static String applyErrorOps(
        final String rest,
        final String varvalue,
        final String varname,
        final BuiltinContext ctx
    ) {
        final String result;
        if (rest.startsWith(":?")) {
            result = errorOnEmpty(varvalue, varname, rest.substring(2), ctx);
        } else if (rest.startsWith("?")) {
            result = errorOnNull(varvalue, varname, rest.substring(1), ctx);
        } else {
            result = null;
        }
        return result;
    }

    private static String applyPatternOps(final String rest, final String varvalue) {
        final String result;
        if (rest.startsWith("##")) {
            result = stripLongestPrefix(varvalue, rest.substring(2));
        } else if (rest.startsWith("#")) {
            result = stripShortestPrefix(varvalue, rest.substring(1));
        } else if (rest.startsWith("%%")) {
            result = stripLongestSuffix(varvalue, rest.substring(2));
        } else if (rest.startsWith("%")) {
            result = stripShortestSuffix(varvalue, rest.substring(1));
        } else {
            result = null;
        }
        return result;
    }

    private static String defaultOnEmpty(
        final String varvalue,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varvalue == null || varvalue.isEmpty()) {
            result = ctx.expandVariables(word, env);
        } else {
            result = varvalue;
        }
        return result;
    }

    private static String defaultOnNull(
        final String varvalue,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varvalue == null) {
            result = ctx.expandVariables(word, env);
        } else {
            result = varvalue;
        }
        return result;
    }

    private static String assignOnEmpty(
        final String varvalue,
        final String varname,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varvalue == null || varvalue.isEmpty()) {
            final String expanded = ctx.expandVariables(word, env);
            if (isAssignableName(varname)) {
                env.put(varname, expanded);
                ctx.environmentMap().put(varname, expanded);
            }
            result = expanded;
        } else {
            result = varvalue;
        }
        return result;
    }

    private static String assignOnNull(
        final String varvalue,
        final String varname,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varvalue == null) {
            final String expanded = ctx.expandVariables(word, env);
            if (isAssignableName(varname)) {
                env.put(varname, expanded);
                ctx.environmentMap().put(varname, expanded);
            }
            result = expanded;
        } else {
            result = varvalue;
        }
        return result;
    }

    private static String altOnNonEmpty(
        final String varvalue,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varvalue == null || varvalue.isEmpty()) {
            result = "";
        } else {
            result = ctx.expandVariables(word, env);
        }
        return result;
    }

    private static String altOnNonNull(
        final String varvalue,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varvalue == null) {
            result = "";
        } else {
            result = ctx.expandVariables(word, env);
        }
        return result;
    }

    private static String errorOnEmpty(
        final String varvalue,
        final String varname,
        final String word,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varvalue == null || varvalue.isEmpty()) {
            final String msg;
            if (word.isEmpty()) {
                msg = "parameter null or not set";
            } else {
                msg = word;
            }
            ctx.errStream().println(String.format("$%s: %s", varname, msg));
            throw new ExitSignal(1);
        } else {
            result = varvalue;
        }
        return result;
    }

    private static String errorOnNull(
        final String varvalue,
        final String varname,
        final String word,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varvalue == null) {
            final String msg;
            if (word.isEmpty()) {
                msg = "parameter not set";
            } else {
                msg = word;
            }
            ctx.errStream().println(String.format("$%s: %s", varname, msg));
            throw new ExitSignal(1);
        } else {
            result = varvalue;
        }
        return result;
    }

    private static String stripLongestPrefix(final String varvalue, final String pattern) {
        int longest = -1;
        for (int idx = 0; idx <= varvalue.length(); idx = idx + 1) {
            if (WordExpander.globMatches(pattern, varvalue.substring(0, idx))) {
                longest = idx;
            }
        }
        final String result;
        if (longest < 0) {
            result = varvalue;
        } else {
            result = varvalue.substring(longest);
        }
        return result;
    }

    private static String stripShortestPrefix(final String varvalue, final String pattern) {
        String result = varvalue;
        for (int idx = 0; idx <= varvalue.length(); idx = idx + 1) {
            if (WordExpander.globMatches(pattern, varvalue.substring(0, idx))) {
                result = varvalue.substring(idx);
                break;
            }
        }
        return result;
    }

    private static String stripLongestSuffix(final String varvalue, final String pattern) {
        int longest = -1;
        for (int idx = 0; idx <= varvalue.length(); idx = idx + 1) {
            if (WordExpander.globMatches(
                pattern, varvalue.substring(varvalue.length() - idx)
            )) {
                longest = idx;
            }
        }
        final String result;
        if (longest < 0) {
            result = varvalue;
        } else {
            result = varvalue.substring(0, varvalue.length() - longest);
        }
        return result;
    }

    private static String stripShortestSuffix(final String varvalue, final String pattern) {
        String result = varvalue;
        for (int idx = 0; idx <= varvalue.length(); idx = idx + 1) {
            if (WordExpander.globMatches(
                pattern, varvalue.substring(varvalue.length() - idx)
            )) {
                result = varvalue.substring(0, varvalue.length() - idx);
                break;
            }
        }
        return result;
    }

    private static boolean isAssignableName(final String varname) {
        return varname.chars()
            .allMatch(chr -> Character.isLetterOrDigit(chr) || chr == '_');
    }

    /**
     * Tilde-expansion (POSIX 2.6.1): {@code ~}, {@code ~+}.
     *
     * @param arg The word to expand
     * @param env Environment view (HOME, PWD)
     * @return The word with the leading tilde resolved
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
                result = env.getOrDefault("HOME", "/root") + rest;
            } else if ("~+".equals(tildepart)) {
                result = env.getOrDefault("PWD", System.getProperty("user.dir")) + rest;
            } else {
                result = arg;
            }
        }
        return result;
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

    private static int normaliseOne(final String arg, final int idx, final StringBuilder result) {
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

    /**
     * Field splitting (POSIX 2.6.5). Returns the fields {@code value} is
     * broken into according to the IFS characters.
     *
     * @param value The post-expansion text
     * @param ifs   The IFS value (each character is a separator)
     * @return One field per non-IFS run
     */
    static List<String> splitFields(final String value, final String ifs) {
        if (ifs == null || ifs.isEmpty()) {
            return Collections.singletonList(value);
        }
        final StringBuilder wsbuf = new StringBuilder();
        final StringBuilder nwsbuf = new StringBuilder();
        for (int idx = 0; idx < ifs.length(); idx = idx + 1) {
            final char chr = ifs.charAt(idx);
            if (chr == ' ' || chr == '\t' || chr == '\n') {
                wsbuf.append(chr);
            } else {
                nwsbuf.append(chr);
            }
        }
        final String wschars = wsbuf.toString();
        final String nwschars = nwsbuf.toString();
        final List<String> result = new ArrayList<>(0);
        StringBuilder field = new StringBuilder();
        boolean infield = false;
        for (int idx = 0; idx < value.length(); idx = idx + 1) {
            final char chr = value.charAt(idx);
            if (nwschars.indexOf(chr) >= 0) {
                result.add(field.toString());
                field = new StringBuilder();
                infield = false;
            } else if (wschars.indexOf(chr) >= 0) {
                if (infield) {
                    result.add(field.toString());
                    field = new StringBuilder();
                    infield = false;
                }
            } else {
                field.append(chr);
                infield = true;
            }
        }
        if (infield) {
            result.add(field.toString());
        } else if (!nwschars.isEmpty() && !value.isEmpty()
            && nwschars.indexOf(value.charAt(value.length() - 1)) >= 0) {
            result.add(field.toString());
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
            result = first == '"' && last == '"'
                || first == '\'' && last == '\'';
        }
        return result;
    }

    /**
     * Escapes characters in expansion-produced data so that downstream
     * {@link #removeQuotes} treats them as literal. POSIX 2.6 says quote
     * removal acts only on quotes that were in the original word, not on
     * data produced by expansions. Doubles every backslash and prefixes
     * every literal single/double quote with a backslash.
     *
     * @param value Expansion-produced text
     * @return The escaped text safe to feed back through removeQuotes
     */
    static String escapeForQuoteRemoval(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        final StringBuilder out = new StringBuilder(value.length() + 4);
        for (int idx = 0; idx < value.length(); idx = idx + 1) {
            final char chr = value.charAt(idx);
            if (chr == '\\' || chr == '\'' || chr == '"') {
                out.append('\\');
            }
            out.append(chr);
        }
        return out.toString();
    }

    /**
     * Quote removal (POSIX 2.6.7). Strips the outer single/double quotes
     * and resolves backslash-escapes inside double quotes.
     *
     * @param word The source word
     * @return The de-quoted text
     */
    static String removeQuotes(final String word) {
        final StringBuilder result = new StringBuilder();
        boolean insingle = false;
        boolean indouble = false;
        int idx = 0;
        while (idx < word.length()) {
            final char chr = word.charAt(idx);
            if (insingle) {
                if (chr == '\'') {
                    insingle = false;
                } else {
                    result.append(chr);
                }
                idx = idx + 1;
            } else if (indouble) {
                idx = appendDoubleQuoted(word, idx, result);
                if (idx < 0) {
                    indouble = false;
                    idx = -idx;
                }
            } else if (chr == '\'') {
                insingle = true;
                idx = idx + 1;
            } else if (chr == '"') {
                indouble = true;
                idx = idx + 1;
            } else if (chr == '\\' && idx + 1 < word.length()) {
                result.append(word.charAt(idx + 1));
                idx = idx + 2;
            } else {
                result.append(chr);
                idx = idx + 1;
            }
        }
        return result.toString();
    }

    private static int appendDoubleQuoted(
        final String word, final int idx, final StringBuilder result
    ) {
        final char chr = word.charAt(idx);
        final int next;
        if (chr == '"') {
            next = -(idx + 1);
        } else if (chr == '\\' && idx + 1 < word.length()
            && (word.charAt(idx + 1) == '$' || word.charAt(idx + 1) == '`'
                || word.charAt(idx + 1) == '"' || word.charAt(idx + 1) == '\\')) {
            result.append(word.charAt(idx + 1));
            next = idx + 2;
        } else {
            result.append(chr);
            next = idx + 1;
        }
        return next;
    }

    /**
     * Pathname expansion (POSIX 2.6.6): replaces any field containing
     * glob characters with the alphabetic list of matching files (or the
     * literal field when no match).
     *
     * @param fields The post-IFS fields
     * @return The post-glob fields
     */
    static List<String> pathnameExpand(final List<String> fields) {
        return pathnameExpand(fields, new File(System.getProperty("user.dir")));
    }

    /**
     * Pathname expansion relative to a specific directory. Dotfiles are
     * skipped unless the final pattern segment starts with a dot.
     *
     * @param fields The post-IFS fields
     * @param cwd Directory used for relative patterns
     * @return The post-glob fields
     */
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

    /**
     * Checks whether the string contains glob characters.
     *
     * @param str Any string
     * @return True when the string contains glob characters
     */
    static boolean containsGlobChars(final String str) {
        return str.indexOf('*') >= 0 || str.indexOf('?') >= 0 || str.indexOf('[') >= 0;
    }

    /**
     * Returns the files matching a glob pattern.
     *
     * @param pattern A glob pattern
     * @param cwd     Directory used for relative patterns
     * @return The matching files
     */
    static List<String> globFiles(final String pattern, final File cwd) {
        final int slash = pattern.lastIndexOf('/');
        final String prefix;
        if (slash < 0) {
            prefix = "";
        } else {
            prefix = pattern.substring(0, slash);
        }
        final String segment;
        if (slash < 0) {
            segment = pattern;
        } else {
            segment = pattern.substring(slash + 1);
        }
        final File dir;
        if (prefix.isEmpty()) {
            dir = cwd;
        } else if (prefix.startsWith("/")) {
            dir = new File(prefix);
        } else {
            dir = new File(cwd, prefix);
        }
        final String[] files = dir.list();
        final List<String> matches = new ArrayList<>(0);
        if (files == null) {
            return matches;
        }
        final PathMatcher matcher;
        try {
            matcher = FileSystems.getDefault().getPathMatcher(String.format("glob:%s", segment));
        } catch (final PatternSyntaxException ignored) {
            return matches;
        }
        final boolean dotpattern = segment.startsWith(".");
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

    /**
     * Checks whether a glob pattern matches a string.
     *
     * @param pattern A glob pattern
     * @param str     The candidate text
     * @return True when {@code pattern} matches {@code str}
     */
    static boolean globMatches(final String pattern, final String str) {
        final StringBuilder regex = new StringBuilder();
        int idx = 0;
        while (idx < pattern.length()) {
            final char chr = pattern.charAt(idx);
            if (chr == '*') {
                regex.append(".*");
            } else if (chr == '?') {
                regex.append('.');
            } else if (chr == '.' || chr == '(' || chr == ')' || chr == '+'
                || chr == '^' || chr == '$' || chr == '{' || chr == '}'
                || chr == '|' || chr == '\\') {
                regex.append('\\').append(chr);
            } else if (chr == '[') {
                final int end = pattern.indexOf(']', idx + 1);
                if (end > idx + 1) {
                    final String body = pattern.substring(idx + 1, end);
                    if (body.startsWith("!") && body.length() > 1) {
                        regex.append("[^").append(body.substring(1)).append(']');
                    } else {
                        regex.append('[').append(body).append(']');
                    }
                    idx = end;
                } else {
                    regex.append("\\[");
                }
            } else {
                regex.append(chr);
            }
            idx = idx + 1;
        }
        return str.matches(regex.toString());
    }
}
