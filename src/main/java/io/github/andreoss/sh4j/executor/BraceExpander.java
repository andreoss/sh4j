/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;

/**
 * Handles brace expansion (POSIX 2.6.2): ${...} parameter expansion.
 *
 * @since 1.0
 */
@SuppressWarnings("PMD.GodClass")
final class BraceExpander {

    private BraceExpander() {
    }

    static String expandBracedImpl(
        final String inner,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (inner.isEmpty()) {
            result = null;
        } else if (inner.charAt(0) == '#') {
            result = BraceExpander.expandLengthOp(inner, env, ctx);
        } else {
            result = BraceExpander.expandNamed(inner, env, ctx);
        }
        return result;
    }

    static String expandLengthOp(
        final String inner,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (inner.length() == 1) {
            result = ctx.getSpecialParam('#');
        } else {
            final String name = inner.substring(1);
            if ("*".equals(name) || "@".equals(name)) {
                result = String.valueOf(ctx.positionalParamsList().size() - 1);
            } else {
                final String val = ctx.lookupVariable(name, env);
                if (val == null) {
                    result = "0";
                } else {
                    result = String.valueOf(val.length());
                }
            }
        }
        return result;
    }

    private static String expandNamed(
        final String inner,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final int nameend = BraceExpander.findNameEnd(inner);
        final String result;
        if (nameend == 0) {
            result = null;
        } else {
            final String varname = inner.substring(0, nameend);
            result = BraceExpander.applyOps(
                inner.substring(nameend),
                ctx.lookupVariable(varname, env), varname, env, ctx
            );
        }
        return result;
    }

    private static String applyOps(
        final String rest,
        final String varvalue,
        final String varname,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        if (rest.isEmpty()) {
            result = BraceExpander.orEmpty(varvalue);
        } else {
            result = BraceExpander.applyNonEmptyOps(
                rest, varvalue, varname, env, ctx
            );
        }
        return result;
    }

    private static String applyNonEmptyOps(
        final String rest,
        final String varvalue,
        final String varname,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String result;
        final String defaulted = BraceExpander.applyDefaultOps(
            rest, varvalue, varname, env, ctx
        );
        final String erred = BraceExpander.applyErrorOps(
            rest, varvalue, varname, ctx
        );
        if (defaulted != null) {
            result = defaulted;
        } else if (erred != null) {
            result = erred;
        } else {
            result = BraceExpander.patternOrEmpty(rest, varvalue);
        }
        return result;
    }

    private static String patternOrEmpty(
        final String rest, final String varvalue
    ) {
        final String result;
        if (varvalue == null) {
            result = "";
        } else {
            result = BraceExpander.applyPatternOps(rest, varvalue);
        }
        return result;
    }

    private static String orEmpty(final String value) {
        final String result;
        if (value == null) {
            result = "";
        } else {
            result = value;
        }
        return result;
    }

    private static int findNameEnd(final String inner) {
        int nameend = 0;
        final char first = inner.charAt(0);
        if (BraceExpander.isSpecialParamChar(first)
            || first == '_' || Character.isLetter(first)) {
            nameend = 1;
            if (!BraceExpander.isSpecialParamChar(first)) {
                nameend = BraceExpander.scanIdentifier(inner);
            }
        } else if (Character.isDigit(first)) {
            nameend = BraceExpander.scanDigits(inner);
        }
        return nameend;
    }

    private static int scanIdentifier(final String inner) {
        int end = 1;
        while (end < inner.length()
            && (Character.isLetterOrDigit(inner.charAt(end))
                || inner.charAt(end) == '_')) {
            end = end + 1;
        }
        return end;
    }

    private static int scanDigits(final String inner) {
        int end = 0;
        while (end < inner.length() && Character.isDigit(inner.charAt(end))) {
            end = end + 1;
        }
        return end;
    }

    private static boolean isSpecialParamChar(final char chr) {
        return "?-$!*@".indexOf(chr) >= 0;
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
            result = BraceExpander.defaultOnEmpty(
                varvalue, rest.substring(2), env, ctx
            );
        } else if (rest.startsWith("-")) {
            result = BraceExpander.defaultOnNull(
                varvalue, rest.substring(1), env, ctx
            );
        } else if (rest.startsWith(":=")) {
            result = BraceExpander.assignOnEmpty(
                varvalue, varname, rest.substring(2), env, ctx
            );
        } else if (rest.startsWith("=")) {
            result = BraceExpander.assignOnNull(
                varvalue, varname, rest.substring(1), env, ctx
            );
        } else if (rest.startsWith(":+")) {
            result = BraceExpander.altOnNonEmpty(
                varvalue, rest.substring(2), env, ctx
            );
        } else if (rest.startsWith("+")) {
            result = BraceExpander.altOnNonNull(
                varvalue, rest.substring(1), env, ctx
            );
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
            result = BraceExpander.errorOnEmpty(
                varvalue, varname, rest.substring(2), ctx
            );
        } else if (rest.startsWith("?")) {
            result = BraceExpander.errorOnNull(
                varvalue, varname, rest.substring(1), ctx
            );
        } else {
            result = null;
        }
        return result;
    }

    private static String applyPatternOps(
        final String rest, final String varvalue
    ) {
        final String result;
        if (rest.startsWith("##")) {
            result = BraceExpander.stripLongestPrefix(
                varvalue, rest.substring(2)
            );
        } else if (rest.startsWith("#")) {
            result = BraceExpander.stripShortestPrefix(
                varvalue, rest.substring(1)
            );
        } else if (rest.startsWith("%%")) {
            result = BraceExpander.stripLongestSuffix(
                varvalue, rest.substring(2)
            );
        } else if (rest.startsWith("%")) {
            result = BraceExpander.stripShortestSuffix(
                varvalue, rest.substring(1)
            );
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
            result = BraceExpander.assign(varname, word, env, ctx);
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
            result = BraceExpander.assign(varname, word, env, ctx);
        } else {
            result = varvalue;
        }
        return result;
    }

    private static String assign(
        final String varname,
        final String word,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        final String expanded = ctx.expandVariables(word, env);
        if (BraceExpander.isAssignableName(varname)) {
            env.put(varname, expanded);
            ctx.environmentMap().put(varname, expanded);
        }
        return expanded;
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
        if (varvalue == null || varvalue.isEmpty()) {
            BraceExpander.reportMissing(
                ctx, varname, word, "parameter null or not set"
            );
        }
        return varvalue;
    }

    private static void reportMissing(
        final BuiltinContext ctx, final String varname,
        final String word, final String fallback
    ) {
        final String msg;
        if (word.isEmpty()) {
            msg = fallback;
        } else {
            msg = word;
        }
        ctx.errStream().println(String.format("$%s: %s", varname, msg));
        throw new ExitSignal(1);
    }

    private static String errorOnNull(
        final String varvalue,
        final String varname,
        final String word,
        final BuiltinContext ctx
    ) {
        if (varvalue == null) {
            BraceExpander.reportMissing(ctx, varname, word, "parameter not set");
        }
        return varvalue;
    }

    private static String stripLongestPrefix(
        final String varvalue, final String pattern
    ) {
        int longest = -1;
        for (int idx = 0; idx <= varvalue.length(); idx = idx + 1) {
            if (GlobExpander.globMatches(pattern, varvalue.substring(0, idx))) {
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

    private static String stripShortestPrefix(
        final String varvalue, final String pattern
    ) {
        int match = -1;
        for (int idx = 0; idx <= varvalue.length(); idx = idx + 1) {
            if (match < 0
                && GlobExpander.globMatches(pattern, varvalue.substring(0, idx))) {
                match = idx;
            }
        }
        final String result;
        if (match < 0) {
            result = varvalue;
        } else {
            result = varvalue.substring(match);
        }
        return result;
    }

    private static String stripLongestSuffix(
        final String varvalue, final String pattern
    ) {
        int longest = -1;
        for (int idx = 0; idx <= varvalue.length(); idx = idx + 1) {
            if (GlobExpander.globMatches(
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

    private static String stripShortestSuffix(
        final String varvalue, final String pattern
    ) {
        int match = -1;
        for (int idx = 0; idx <= varvalue.length(); idx = idx + 1) {
            if (match < 0 && GlobExpander.globMatches(
                pattern, varvalue.substring(varvalue.length() - idx)
            )) {
                match = idx;
            }
        }
        final String result;
        if (match < 0) {
            result = varvalue;
        } else {
            result = varvalue.substring(0, varvalue.length() - match);
        }
        return result;
    }

    private static boolean isAssignableName(final String varname) {
        return varname.chars()
            .allMatch(chr -> Character.isLetterOrDigit(chr) || chr == '_');
    }
}

