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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.andreoss.sh4j.annotation.GeneratedDefensive;

/**
 * Stateless helpers that implement POSIX 2.6 word expansion: tilde,
 * parameter, command-substitution, arithmetic, field-splitting, pathname,
 * and quote-removal. The methods cooperate with {@link BuiltinContext} to
 * read the executor's environment and signal expansion errors.
 *
 * @since 1.0
 */
public final class WordExpander {

    /** Pattern matching bare {@code $NAME} or {@code ${NAME}} references. */
    static final Pattern VAR_PATTERN =
        Pattern.compile("\\$\\{?([a-zA-Z_][a-zA-Z0-9_]*)\\}?");

    /** Default fallback used when {@code HOME} is unset. */
    private static final String DEFAULT_HOME = "/root";

    /** Whitespace characters considered field separators by default. */
    private static final String DEFAULT_IFS_WS = " \t\n";

    /**
     * Utility class; no instances.
     */
    @GeneratedDefensive
    private WordExpander() {
        super();
    }

    /**
     * The result of a word expansion: the produced text and whether a
     * sub-expansion (e.g. {@code ${var:?}}) flagged an error.
     *
     * @param value The expanded text
     * @param error True when at least one sub-expansion failed
     */
    public record ExpansionResult(String value, boolean error) {
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
    public static String expandVariablesImpl(
        final String arg,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        String current = expandTilde(arg, env);
        current = normaliseBackticks(current);
        final StringBuilder expanded = new StringBuilder();
        int idx = 0;
        while (idx < current.length()) {
            final int consumed = appendOneExpansion(current, idx, env, ctx, expanded);
            idx = idx + consumed;
        }
        current = expanded.toString();
        final Matcher matcher = VAR_PATTERN.matcher(current);
        final StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            final String varName = matcher.group(1);
            final String replacement = env.getOrDefault(varName, "");
            matcher.appendReplacement(buf, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    /**
     * Consumes the expansion that starts at {@code idx} (if any) and
     * appends its result to {@code expanded}.
     *
     * @return The number of source characters consumed
     */
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
                expanded.append(special);
                consumed = 2;
            } else if (next == '(') {
                consumed = appendDollarParen(arg, idx, env, ctx, expanded);
            } else if (next == '{' && idx + 2 < arg.length()) {
                consumed = appendDollarBrace(arg, idx, env, ctx, expanded);
            } else {
                expanded.append(chr);
            }
        } else {
            expanded.append(chr);
        }
        return consumed;
    }

    /**
     * Handles {@code $(...)} or {@code $((...))} starting at {@code idx}.
     *
     * @return Number of source characters consumed (or 1 on no match)
     */
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
                expanded.append(ctx.evalArithmetic(arg.substring(idx + 3, end - 2), env));
                consumed = end - idx;
            } else {
                expanded.append(arg.charAt(idx));
            }
        } else {
            final int end = findBalancedClose(arg, idx + 2, '(', ')', 1);
            if (end > 0) {
                expanded.append(ctx.executeCommandSubstitution(arg.substring(idx + 2, end - 1)));
                consumed = end - idx;
            } else {
                expanded.append(arg.charAt(idx));
            }
        }
        return consumed;
    }

    /**
     * Handles {@code ${...}} starting at {@code idx}.
     *
     * @return Number of source characters consumed (or 1 on no match)
     */
    private static int appendDollarBrace(
        final String arg,
        final int idx,
        final Map<String, String> env,
        final BuiltinContext ctx,
        final StringBuilder expanded
    ) {
        int consumed = 1;
        final int close = arg.indexOf('}', idx + 2);
        if (close > idx + 2) {
            final String inner = arg.substring(idx + 2, close);
            final String replacement = expandBracedImpl(inner, env, ctx);
            if (replacement == null) {
                expanded.append(arg.charAt(idx));
            } else {
                expanded.append(replacement);
                consumed = close - idx + 1;
            }
        } else {
            expanded.append(arg.charAt(idx));
        }
        return consumed;
    }

    /**
     * Finds the matching close-character by counting balanced open/close.
     *
     * @param arg       The source string
     * @param start     Position immediately after the opener(s)
     * @param open      Open character
     * @param close     Close character
     * @param initial   Initial depth (1 for {@code $(}, 2 for {@code $((})
     * @return The position immediately after the closing character(s), or
     *         -1 when unbalanced
     */
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

    /**
     * Thin wrapper around {@link #expandVariablesImpl} that returns the
     * post-call error flag in a record.
     */
    public static ExpansionResult expandVariables(
        final String arg,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String value = ctx.expandVariables(arg, env);
        final boolean error = ctx.wordExpansionErrorOccurred();
        if (error) {
            ctx.clearWordExpansionError();
        }
        return new ExpansionResult(value, error);
    }

    /**
     * @param chr A character
     * @return True when {@code chr} is one of POSIX's special parameter
     *         characters ({@code ?}, {@code -}, {@code $}, ...)
     */
    private static boolean isSpecialParamChar(final char chr) {
        return chr == '?' || chr == '-' || chr == '$' || chr == '!'
            || chr == '@' || chr == '*';
    }

    /**
     * Full implementation of {@code ${...}} brace expansion.
     *
     * @param inner The text between {@code ${} and {@code }}
     * @param env   Environment view
     * @param ctx   Executor callbacks
     * @return The expansion, or null when the inner text doesn't match any
     *         recognised operator
     */
    public static String expandBracedImpl(
        final String inner,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        if (inner.isEmpty()) {
            return null;
        }
        if (inner.charAt(0) == '#') {
            final String val = ctx.lookupVariable(inner.substring(1), env);
            return val == null ? "0" : String.valueOf(val.length());
        }
        final int nameEnd = findNameEnd(inner);
        if (nameEnd == 0) {
            return null;
        }
        final String varName = inner.substring(0, nameEnd);
        final String rest = inner.substring(nameEnd);
        final String varValue = ctx.lookupVariable(varName, env);
        if (rest.isEmpty()) {
            return varValue == null ? "" : varValue;
        }
        final String defaulted = applyDefaultOps(rest, varValue, varName, env, ctx);
        if (defaulted != null) {
            return defaulted;
        }
        final String erred = applyErrorOps(rest, varValue, varName, ctx);
        if (erred != null) {
            return erred;
        }
        if (varValue == null) {
            return "";
        }
        return applyPatternOps(rest, varValue);
    }

    /**
     * Finds the end of the variable-name portion of an inner-brace string.
     *
     * @param inner The inner-brace text
     * @return The index just past the name (0 when no valid name was found)
     */
    private static int findNameEnd(final String inner) {
        int nameEnd = 0;
        final char first = inner.charAt(0);
        if (isSpecialParamChar(first) || first == '_' || Character.isLetter(first)) {
            nameEnd = 1;
            if (!isSpecialParamChar(first)) {
                while (nameEnd < inner.length()
                    && (Character.isLetterOrDigit(inner.charAt(nameEnd))
                        || inner.charAt(nameEnd) == '_')) {
                    nameEnd = nameEnd + 1;
                }
            }
        } else if (Character.isDigit(first)) {
            while (nameEnd < inner.length() && Character.isDigit(inner.charAt(nameEnd))) {
                nameEnd = nameEnd + 1;
            }
        }
        return nameEnd;
    }

    /**
     * Applies the {@code :-}, {@code -}, {@code :=}, {@code =},
     * {@code :+}, {@code +} operators.
     *
     * @return The expansion, or null when no default-class operator matches
     */
    private static String applyDefaultOps(
        final String rest,
        final String varValue,
        final String varName,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (rest.startsWith(":-")) {
            result = defaultOnEmpty(varValue, rest.substring(2), env, ctx);
        } else if (rest.startsWith("-")) {
            result = defaultOnNull(varValue, rest.substring(1), env, ctx);
        } else if (rest.startsWith(":=")) {
            result = assignOnEmpty(varValue, varName, rest.substring(2), env, ctx);
        } else if (rest.startsWith("=")) {
            result = assignOnNull(varValue, varName, rest.substring(1), env, ctx);
        } else if (rest.startsWith(":+")) {
            result = altOnNonEmpty(varValue, rest.substring(2), env, ctx);
        } else if (rest.startsWith("+")) {
            result = altOnNonNull(varValue, rest.substring(1), env, ctx);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Applies the {@code :?} and {@code ?} error operators.
     *
     * @return The expansion, or null when no error-class operator matches
     */
    private static String applyErrorOps(
        final String rest,
        final String varValue,
        final String varName,
        final BuiltinContext ctx
    ) {
        final String result;
        if (rest.startsWith(":?")) {
            result = errorOnEmpty(varValue, varName, rest.substring(2), ctx);
        } else if (rest.startsWith("?")) {
            result = errorOnNull(varValue, varName, rest.substring(1), ctx);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Applies the {@code #}, {@code ##}, {@code %}, {@code %%}
     * prefix/suffix-removal operators.
     */
    private static String applyPatternOps(final String rest, final String varValue) {
        final String result;
        if (rest.startsWith("##")) {
            result = stripLongestPrefix(varValue, rest.substring(2));
        } else if (rest.startsWith("#")) {
            result = stripShortestPrefix(varValue, rest.substring(1));
        } else if (rest.startsWith("%%")) {
            result = stripLongestSuffix(varValue, rest.substring(2));
        } else if (rest.startsWith("%")) {
            result = stripShortestSuffix(varValue, rest.substring(1));
        } else {
            result = null;
        }
        return result;
    }

    private static String defaultOnEmpty(
        final String varValue,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varValue == null || varValue.isEmpty()) {
            result = ctx.expandVariables(word, env);
        } else {
            result = varValue;
        }
        return result;
    }

    private static String defaultOnNull(
        final String varValue,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varValue == null) {
            result = ctx.expandVariables(word, env);
        } else {
            result = varValue;
        }
        return result;
    }

    private static String assignOnEmpty(
        final String varValue,
        final String varName,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varValue == null || varValue.isEmpty()) {
            final String expanded = ctx.expandVariables(word, env);
            if (isAssignableName(varName)) {
                env.put(varName, expanded);
            }
            result = expanded;
        } else {
            result = varValue;
        }
        return result;
    }

    private static String assignOnNull(
        final String varValue,
        final String varName,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varValue == null) {
            final String expanded = ctx.expandVariables(word, env);
            if (isAssignableName(varName)) {
                env.put(varName, expanded);
            }
            result = expanded;
        } else {
            result = varValue;
        }
        return result;
    }

    private static String altOnNonEmpty(
        final String varValue,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varValue == null || varValue.isEmpty()) {
            result = "";
        } else {
            result = ctx.expandVariables(word, env);
        }
        return result;
    }

    private static String altOnNonNull(
        final String varValue,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varValue == null) {
            result = "";
        } else {
            result = ctx.expandVariables(word, env);
        }
        return result;
    }

    private static String errorOnEmpty(
        final String varValue,
        final String varName,
        final String word,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varValue == null || varValue.isEmpty()) {
            final String msg;
            if (word.isEmpty()) {
                msg = "parameter null or not set";
            } else {
                msg = word;
            }
            ctx.errStream().println("$" + varName + ": " + msg);
            ctx.setWordExpansionError();
            result = "";
        } else {
            result = varValue;
        }
        return result;
    }

    private static String errorOnNull(
        final String varValue,
        final String varName,
        final String word,
        final BuiltinContext ctx
    ) {
        final String result;
        if (varValue == null) {
            final String msg;
            if (word.isEmpty()) {
                msg = "parameter not set";
            } else {
                msg = word;
            }
            ctx.errStream().println("$" + varName + ": " + msg);
            ctx.setWordExpansionError();
            result = "";
        } else {
            result = varValue;
        }
        return result;
    }

    private static String stripLongestPrefix(final String varValue, final String pattern) {
        int longest = -1;
        for (int idx = 0; idx <= varValue.length(); idx = idx + 1) {
            if (globMatches(pattern, varValue.substring(0, idx))) {
                longest = idx;
            }
        }
        return longest < 0 ? varValue : varValue.substring(longest);
    }

    private static String stripShortestPrefix(final String varValue, final String pattern) {
        String result = varValue;
        for (int idx = 0; idx <= varValue.length(); idx = idx + 1) {
            if (globMatches(pattern, varValue.substring(0, idx))) {
                result = varValue.substring(idx);
                break;
            }
        }
        return result;
    }

    private static String stripLongestSuffix(final String varValue, final String pattern) {
        int longest = -1;
        for (int idx = 0; idx <= varValue.length(); idx = idx + 1) {
            if (globMatches(pattern, varValue.substring(varValue.length() - idx))) {
                longest = idx;
            }
        }
        return longest < 0 ? varValue : varValue.substring(0, varValue.length() - longest);
    }

    private static String stripShortestSuffix(final String varValue, final String pattern) {
        String result = varValue;
        for (int idx = 0; idx <= varValue.length(); idx = idx + 1) {
            if (globMatches(pattern, varValue.substring(varValue.length() - idx))) {
                result = varValue.substring(0, varValue.length() - idx);
                break;
            }
        }
        return result;
    }

    private static boolean isAssignableName(final String varName) {
        return varName.chars()
            .allMatch(chr -> Character.isLetterOrDigit(chr) || chr == '_');
    }

    /**
     * Wrapper around {@link #expandBracedImpl} that returns the
     * error flag in a record.
     */
    public static ExpansionResult expandBraced(
        final String inner,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String value = expandBracedImpl(inner, env, ctx);
        final boolean error = ctx.wordExpansionErrorOccurred();
        if (error) {
            ctx.clearWordExpansionError();
        }
        return new ExpansionResult(value, error);
    }

    /**
     * Tilde-expansion (POSIX 2.6.1): {@code ~}, {@code ~+}.
     *
     * @param arg The word to expand
     * @param env Environment view (HOME, PWD)
     * @return The word with the leading tilde resolved
     */
    public static String expandTilde(final String arg, final Map<String, String> env) {
        final String result;
        if (arg.isEmpty() || arg.charAt(0) != '~') {
            result = arg;
        } else {
            final int slash = arg.indexOf('/');
            final String tildePart;
            final String rest;
            if (slash >= 0) {
                tildePart = arg.substring(0, slash);
                rest = arg.substring(slash);
            } else {
                tildePart = arg;
                rest = "";
            }
            if ("~".equals(tildePart)) {
                result = env.getOrDefault("HOME", DEFAULT_HOME) + rest;
            } else if ("~+".equals(tildePart)) {
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
    public static String normaliseBackticks(final String arg) {
        final StringBuilder result = new StringBuilder();
        int idx = 0;
        while (idx < arg.length()) {
            final int consumed = normaliseOne(arg, idx, result);
            idx = idx + consumed;
        }
        return result.toString();
    }

    /**
     * Normalises one source position (a backtick, a {@code $(...)} chunk,
     * a {@code ${...}} chunk, or a literal character).
     *
     * @return Number of source characters consumed
     */
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

    /**
     * Copies a balanced {@code $(...)} or {@code ${...}} chunk verbatim
     * into the result buffer.
     *
     * @return Number of source characters consumed
     */
    private static int copyBalanced(
        final String arg,
        final int innerStart,
        final char open,
        final char close,
        final StringBuilder result,
        final String full,
        final int chunkStart
    ) {
        final int end = findBalancedClose(arg, innerStart, open, close, 1);
        final int consumed;
        if (end > 0) {
            result.append(full, chunkStart, end);
            consumed = end - chunkStart;
        } else {
            result.append(full.charAt(chunkStart));
            consumed = 1;
        }
        return consumed;
    }

    /**
     * Rewrites one backtick chunk as a {@code $(...)} substitution.
     *
     * @return Number of source characters consumed (always 1+content_length)
     */
    private static int rewriteBacktick(final String arg, final int idx, final StringBuilder result) {
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
                    content.append(chr);
                    content.append(nextc);
                }
                cur = cur + 2;
            } else if (chr == '`') {
                result.append("$(");
                result.append(content);
                result.append(')');
                cur = cur + 1;
                found = true;
                break;
            } else {
                content.append(chr);
                cur = cur + 1;
            }
        }
        if (!found) {
            result.append('`');
            result.append(content);
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
    public static List<String> splitFields(final String value, final String ifs) {
        if (ifs == null || ifs.isEmpty()) {
            return List.of(value);
        }
        final StringBuilder wsBuf = new StringBuilder();
        final StringBuilder nwsBuf = new StringBuilder();
        for (int idx = 0; idx < ifs.length(); idx = idx + 1) {
            final char chr = ifs.charAt(idx);
            if (chr == ' ' || chr == '\t' || chr == '\n') {
                wsBuf.append(chr);
            } else {
                nwsBuf.append(chr);
            }
        }
        final String wsChars = wsBuf.toString();
        final String nwsChars = nwsBuf.toString();
        final List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inField = false;
        for (int idx = 0; idx < value.length(); idx = idx + 1) {
            final char chr = value.charAt(idx);
            if (nwsChars.indexOf(chr) >= 0) {
                result.add(field.toString());
                field = new StringBuilder();
                inField = false;
            } else if (wsChars.indexOf(chr) >= 0) {
                if (inField) {
                    result.add(field.toString());
                    field = new StringBuilder();
                    inField = false;
                }
            } else {
                field.append(chr);
                inField = true;
            }
        }
        if (inField) {
            result.add(field.toString());
        } else if (!nwsChars.isEmpty() && !value.isEmpty()
            && nwsChars.indexOf(value.charAt(value.length() - 1)) >= 0) {
            result.add(field.toString());
        }
        return result;
    }

    /**
     * @param arg Any word
     * @return True when the word is surrounded by matching single or double
     *         quotes
     */
    public static boolean isQuoted(final String arg) {
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
     * Quote removal (POSIX 2.6.7). Strips the outer single/double quotes
     * and resolves backslash-escapes inside double quotes.
     *
     * @param word The source word
     * @return The de-quoted text
     */
    public static String removeQuotes(final String word) {
        final StringBuilder result = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        int idx = 0;
        while (idx < word.length()) {
            final char chr = word.charAt(idx);
            if (inSingle) {
                if (chr == '\'') {
                    inSingle = false;
                } else {
                    result.append(chr);
                }
                idx = idx + 1;
            } else if (inDouble) {
                idx = appendDoubleQuoted(word, idx, result);
                if (idx < 0) {
                    inDouble = false;
                    idx = -idx;
                }
            } else if (chr == '\'') {
                inSingle = true;
                idx = idx + 1;
            } else if (chr == '"') {
                inDouble = true;
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

    /**
     * Appends one character of a double-quoted span; returns the new index
     * (negated when the closing quote was consumed).
     */
    private static int appendDoubleQuoted(final String word, final int idx, final StringBuilder result) {
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
    public static List<String> pathnameExpand(final List<String> fields) {
        final List<String> result = new ArrayList<>();
        for (final String field : fields) {
            if (containsGlobChars(field)) {
                final List<String> matches = globFiles(field);
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
     * @param str Any string
     * @return True when {@code str} contains any of {@code *}, {@code ?},
     *         or {@code [}
     */
    public static boolean containsGlobChars(final String str) {
        return str.indexOf('*') >= 0 || str.indexOf('?') >= 0 || str.indexOf('[') >= 0;
    }

    /**
     * @param pattern A glob pattern
     * @return The matching files in the current directory
     */
    static List<String> globFiles(final String pattern) {
        final File cwd = new File(System.getProperty("user.dir"));
        final String[] files = cwd.list();
        final List<String> matches;
        if (files == null) {
            matches = List.of();
        } else {
            final PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            matches = new ArrayList<>();
            for (final String name : files) {
                if (matcher.matches(Paths.get(name))) {
                    matches.add(name);
                }
            }
        }
        return matches;
    }

    /**
     * @param pattern A glob pattern
     * @param str     The candidate text
     * @return True when {@code pattern} matches {@code str}
     */
    public static boolean globMatches(final String pattern, final String str) {
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
                final int end = pattern.indexOf(']', idx);
                if (end > idx) {
                    regex.append(pattern, idx, end + 1);
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
