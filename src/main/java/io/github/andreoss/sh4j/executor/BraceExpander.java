/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Handles brace expansion (POSIX 2.6.2): ${...} parameter expansion.
 *
 * @since 1.0
 */
final class BraceExpander {

    /** Operators whose expansion the {@code -u} option does not apply to. */
    private static final Pattern SPARED = Pattern.compile(":?[-=+?]");

    /** First character of a variable name, as opposed to a parameter. */
    private static final Pattern VARIABLE = Pattern.compile("[\\p{L}_]");

    private BraceExpander() {
    }

    static String expandBracedImpl(
        final String inner,
        final Map<String, String> env,
        final BuiltinContext ctx
    ) {
        return BraceExpander.expandBracedImpl(
            inner, new ExpansionContext(env, ctx, new Expanded())
        );
    }

    static String expandBracedImpl(
        final String inner,
        final ExpansionContext context
    ) {
        final String result;
        if (inner.isEmpty()) {
            result = null;
        } else if (inner.charAt(0) == '#') {
            result = BraceExpander.expandLengthOp(inner, context);
        } else {
            result = BraceExpander.expandNamed(inner, context);
        }
        return result;
    }

    static String expandLengthOp(
        final String inner,
        final ExpansionContext context
    ) {
        final String result;
        if (inner.length() == 1) {
            result = context.ctx().getSpecialParam('#');
        } else {
            final String name = inner.substring(1);
            if ("*".equals(name) || "@".equals(name)) {
                result = String.valueOf(context.ctx().positionalParamsList().size() - 1);
            } else {
                result = BraceExpander.lengthOfVariable(name, context);
            }
        }
        return result;
    }

    private static String lengthOfVariable(
        final String name,
        final ExpansionContext context
    ) {
        final String val = context.ctx().lookupVariable(name, context.env());
        final String result;
        if (val == null) {
            result = "0";
        } else {
            result = String.valueOf(val.length());
        }
        return result;
    }

    private static String expandNamed(
        final String inner,
        final ExpansionContext context
    ) {
        final int nameend = BraceExpander.findNameEnd(inner);
        final String result;
        if (nameend == 0) {
            result = null;
        } else {
            final String varname = inner.substring(0, nameend);
            final String rest = inner.substring(nameend);
            result = BraceExpander.applyOps(
                rest, BraceExpander.value(varname, rest, context), varname, context
            );
        }
        return result;
    }

    private static String value(
        final String varname, final String rest, final ExpansionContext context
    ) {
        final String result;
        if ("@".equals(varname) && context.listing()) {
            result = Breaks.joined(context.ctx());
        } else if (BraceExpander.SPARED.matcher(rest).lookingAt()
            && BraceExpander.VARIABLE.matcher(varname).lookingAt()) {
            result = context.env().get(varname);
        } else {
            result = context.ctx().lookupVariable(varname, context.env());
        }
        return result;
    }

    private static String applyOps(
        final String rest,
        final String varvalue,
        final String varname,
        final ExpansionContext context
    ) {
        final String result;
        if (rest.isEmpty()) {
            result = BraceExpander.orEmpty(varvalue);
        } else {
            result = BraceExpander.applyNonEmptyOps(
                rest, varvalue, varname, context
            );
        }
        return result;
    }

    private static String applyNonEmptyOps(
        final String rest,
        final String varvalue,
        final String varname,
        final ExpansionContext context
    ) {
        final String result;
        final Parameter parameter = new Parameter(varname, varvalue);
        final String defaulted = BraceDefaults.apply(rest, parameter, context);
        final String erred = BraceErrors.apply(rest, parameter, context.ctx());
        if (defaulted != null) {
            result = defaulted;
        } else if (erred != null) {
            result = erred;
        } else {
            result = BraceExpander.patternOrEmpty(rest, varvalue, context);
        }
        return result;
    }

    private static String patternOrEmpty(
        final String rest, final String varvalue, final ExpansionContext context
    ) {
        final String result;
        if (varvalue == null) {
            result = "";
        } else {
            result = PatternRemoval.apply(rest, varvalue, context);
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
}
