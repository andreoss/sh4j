/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.Nesting;

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
                    VariableExpander.escape(context, special)
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
        context.expanded().append(VariableExpander.escape(context, safe));
        return end - idx;
    }

    static int appendDollarParen(
        final String arg,
        final int idx,
        final ExpansionContext context
    ) {
        final int consumed;
        if (idx + 2 < arg.length() && arg.charAt(idx + 2) == '(') {
            final int end = new Nesting(arg).paren(idx + 2);
            if (end > 0) {
                context.expanded().append(
                    VariableExpander.escape(
                        context,
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
            final int end = new Nesting(arg).paren(idx + 2);
            if (end > 0) {
                context.expanded().append(
                    VariableExpander.escape(
                        context,
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
        final int close = new Nesting(arg).brace(idx + 2, context.quoted()) - 1;
        if (close > idx + 2) {
            context.verbatim(false);
            final String replacement = BraceExpander.expandBracedImpl(
                arg.substring(idx + 2, close), context
            );
            if (replacement == null) {
                context.expanded().append(arg.charAt(idx));
                consumed = 1;
            } else {
                context.expanded().append(
                    VariableExpander.braced(context, replacement)
                );
                consumed = close - idx + 1;
            }
        } else {
            context.expanded().append(arg.charAt(idx));
            consumed = 1;
        }
        return consumed;
    }

    private static String braced(final ExpansionContext context, final String value) {
        final String result;
        if (context.verbatim()) {
            result = value;
        } else {
            result = VariableExpander.escape(context, value);
        }
        return result;
    }

    private static String escape(final ExpansionContext context, final String value) {
        final String result;
        if (context.quoted()) {
            result = value.replace("\\", "\\\\").replace("\"", "\\\"");
        } else {
            result = WordExpander.escapeForQuoteRemoval(value);
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
