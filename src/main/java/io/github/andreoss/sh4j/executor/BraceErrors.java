/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * The error operators of parameter expansion (POSIX 2.6.2): {@code :?} and
 * {@code ?}, which report a missing value and end the shell.
 *
 * @since 1.0
 */
final class BraceErrors {

    private BraceErrors() {
    }

    static String apply(
        final String rest,
        final Parameter parameter,
        final BuiltinContext ctx
    ) {
        final String result;
        if (rest.startsWith(":?")) {
            result = BraceErrors.errorOnEmpty(parameter, rest.substring(2), ctx);
        } else if (rest.startsWith("?")) {
            result = BraceErrors.errorOnNull(parameter, rest.substring(1), ctx);
        } else {
            result = null;
        }
        return result;
    }

    private static String errorOnEmpty(
        final Parameter parameter,
        final String word,
        final BuiltinContext ctx
    ) {
        if (parameter.blank()) {
            BraceErrors.reportMissing(
                ctx, parameter.name(), word, "parameter null or not set"
            );
        }
        return parameter.value();
    }

    private static String errorOnNull(
        final Parameter parameter,
        final String word,
        final BuiltinContext ctx
    ) {
        if (parameter.unset()) {
            BraceErrors.reportMissing(ctx, parameter.name(), word, "parameter not set");
        }
        return parameter.value();
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
}
