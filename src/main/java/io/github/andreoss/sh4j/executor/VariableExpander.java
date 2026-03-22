/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * Handles variable and command substitution expansion.
 *
 * @since 1.0
 */
final class VariableExpander {

    private VariableExpander() {
    }

    static int appendOneExpansion(
        final String arg,
        final int idx,
        final ExpansionContext context
    ) {
        final char chr = arg.charAt(idx);
        final int consumed;
        if (chr != '$' || idx + 1 >= arg.length()) {
            context.expanded().append(chr);
            consumed = 1;
        } else {
            final char next = arg.charAt(idx + 1);
            final String special = context.ctx().getSpecialParam(next);
            if (special != null) {
                context.expanded().append(
                    WordExpander.escapeForQuoteRemoval(special)
                );
                consumed = 2;
            } else if (next == '(') {
                consumed = VariableExpander.appendDollarParen(arg, idx, context);
            } else if (next == '{' && idx + 2 < arg.length()) {
                consumed = VariableExpander.appendDollarBrace(arg, idx, context);
            } else if (VariableExpander.isIdentifierStart(next)) {
                consumed = VariableExpander.appendVariableExpansion(
                    arg, idx, context
                );
            } else {
                context.expanded().append(chr);
                consumed = 1;
            }
        }
        return consumed;
    }

    static int appendVariableExpansion(
        final String arg,
        final int idx,
        final ExpansionContext context
    ) {
        int end = idx + 2;
        while (end < arg.length() && VariableExpander.isIdentifierPart(arg.charAt(end))) {
            end = end + 1;
        }
        final String value = context.ctx().lookupVariable(
            arg.substring(idx + 1, end), context.env()
        );
        final String safe;
        if (value == null) {
            safe = "";
        } else {
            safe = value;
        }
        context.expanded().append(WordExpander.escapeForQuoteRemoval(safe));
        return end - idx;
    }

    static int appendDollarParen(
        final String arg,
        final int idx,
        final ExpansionContext context
    ) {
        final int consumed;
        if (idx + 2 < arg.length() && arg.charAt(idx + 2) == '(') {
            final int end = VariableExpander.findBalancedClose(arg, idx + 3, '(', ')', 2);
            if (end > 0) {
                context.expanded().append(
                    WordExpander.escapeForQuoteRemoval(
                        context.ctx().evalArithmetic(
                            QuoteRemover.removeQuotes(
                                WordExpander.expandVariablesImpl(
                                    arg.substring(idx + 3, end - 2),
                                    context.env(),
                                    context.ctx()
                                )
                            ), context.env()
                        )
                    )
                );
                consumed = end - idx;
            } else {
                context.expanded().append(arg.charAt(idx));
                consumed = 1;
            }
        } else {
            final int end = VariableExpander.findBalancedClose(arg, idx + 2, '(', ')', 1);
            if (end > 0) {
                context.expanded().append(
                    WordExpander.escapeForQuoteRemoval(
                        context.ctx().executeCommandSubstitution(
                            arg.substring(idx + 2, end - 1)
                        )
                    )
                );
                consumed = end - idx;
            } else {
                context.expanded().append(arg.charAt(idx));
                consumed = 1;
            }
        }
        return consumed;
    }

    static int appendDollarBrace(
        final String arg,
        final int idx,
        final ExpansionContext context
    ) {
        final int consumed;
        final int close = VariableExpander.findMatchingBrace(arg, idx + 2);
        if (close > idx + 2) {
            final String replacement = BraceExpander.expandBracedImpl(
                arg.substring(idx + 2, close), context.env(), context.ctx()
            );
            if (replacement == null) {
                context.expanded().append(arg.charAt(idx));
                consumed = 1;
            } else {
                context.expanded().append(
                    WordExpander.escapeForQuoteRemoval(replacement)
                );
                consumed = close - idx + 1;
            }
        } else {
            context.expanded().append(arg.charAt(idx));
            consumed = 1;
        }
        return consumed;
    }

    private static int findMatchingBrace(final String arg, final int start) {
        int depth = 1;
        int pos = start;
        int found = -1;
        while (pos < arg.length()) {
            final char chr = arg.charAt(pos);
            if (chr == '{') {
                depth = depth + 1;
            } else if (chr == '}') {
                depth = depth - 1;
                if (depth == 0) {
                    found = pos;
                    pos = arg.length();
                }
            }
            pos = pos + 1;
        }
        return found;
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

    private static boolean isIdentifierStart(final char chr) {
        return "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .indexOf(chr) >= 0;
    }

    private static boolean isIdentifierPart(final char chr) {
        return VariableExpander.isIdentifierStart(chr) || chr >= '0' && chr <= '9';
    }
}
