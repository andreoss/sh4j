/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * The default, assignment and alternative operators of parameter expansion
 * (POSIX 2.6.2): {@code :-}, {@code -}, {@code :=}, {@code =}, {@code :+}
 * and {@code +}.
 *
 * @since 1.0
 */
final class BraceDefaults {

    private BraceDefaults() {
    }

    static String apply(
        final String rest,
        final Parameter parameter,
        final ExpansionContext context
    ) {
        final String result;
        if (rest.startsWith(":-")) {
            result = BraceDefaults.defaultOnEmpty(parameter, rest.substring(2), context);
        } else if (rest.startsWith("-")) {
            result = BraceDefaults.defaultOnNull(parameter, rest.substring(1), context);
        } else if (rest.startsWith(":=")) {
            result = BraceDefaults.assignOnEmpty(parameter, rest.substring(2), context);
        } else if (rest.startsWith("=")) {
            result = BraceDefaults.assignOnNull(parameter, rest.substring(1), context);
        } else if (rest.startsWith(":+")) {
            result = BraceDefaults.altOnNonEmpty(parameter, rest.substring(2), context);
        } else if (rest.startsWith("+")) {
            result = BraceDefaults.altOnNonNull(parameter, rest.substring(1), context);
        } else {
            result = null;
        }
        return result;
    }

    private static String defaultOnEmpty(
        final Parameter parameter,
        final String word,
        final ExpansionContext context
    ) {
        final String result;
        if (parameter.blank()) {
            result = BraceDefaults.alternative(word, context);
        } else {
            result = parameter.value();
        }
        return result;
    }

    private static String defaultOnNull(
        final Parameter parameter,
        final String word,
        final ExpansionContext context
    ) {
        final String result;
        if (parameter.unset()) {
            result = BraceDefaults.alternative(word, context);
        } else {
            result = parameter.value();
        }
        return result;
    }

    private static String assignOnEmpty(
        final Parameter parameter,
        final String word,
        final ExpansionContext context
    ) {
        final String result;
        if (parameter.blank()) {
            result = BraceDefaults.assign(parameter.name(), word, context);
        } else {
            result = parameter.value();
        }
        return result;
    }

    private static String assignOnNull(
        final Parameter parameter,
        final String word,
        final ExpansionContext context
    ) {
        final String result;
        if (parameter.unset()) {
            result = BraceDefaults.assign(parameter.name(), word, context);
        } else {
            result = parameter.value();
        }
        return result;
    }

    private static String assign(
        final String varname,
        final String word,
        final ExpansionContext context
    ) {
        final String expanded = BraceDefaults.word(word, context);
        if (BraceDefaults.isAssignableName(varname)) {
            context.env().put(varname, expanded);
            context.ctx().environmentMap().put(varname, expanded);
        }
        return expanded;
    }

    private static String altOnNonEmpty(
        final Parameter parameter,
        final String word,
        final ExpansionContext context
    ) {
        final String result;
        if (parameter.blank()) {
            result = "";
        } else {
            result = BraceDefaults.alternative(word, context);
        }
        return result;
    }

    private static String altOnNonNull(
        final Parameter parameter,
        final String word,
        final ExpansionContext context
    ) {
        final String result;
        if (parameter.unset()) {
            result = "";
        } else {
            result = BraceDefaults.alternative(word, context);
        }
        return result;
    }

    private static String alternative(final String word, final ExpansionContext context) {
        final String result;
        if (context.splitting() && !context.quoted()) {
            result = WordExpander.expandFieldsImpl(word, context.env(), context.ctx());
            context.verbatim(true);
        } else if (context.splitting()) {
            result = QuoteRemover.removeQuotes(
                WordExpander.expandNestedImpl(
                    String.join("", "\"", word, "\""), context.env(), context.ctx()
                )
            );
        } else {
            result = BraceDefaults.word(word, context);
        }
        return result;
    }

    private static String word(final String word, final ExpansionContext context) {
        final String text;
        if (context.quoted()) {
            text = String.join("", "\"", word, "\"");
        } else {
            text = word;
        }
        return QuoteRemover.removeQuotes(
            context.ctx().expandVariables(text, context.env())
        );
    }

    private static boolean isAssignableName(final String varname) {
        return varname.chars()
            .allMatch(chr -> Character.isLetterOrDigit(chr) || chr == '_');
    }
}
