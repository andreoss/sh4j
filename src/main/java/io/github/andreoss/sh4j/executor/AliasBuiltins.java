/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Built-ins that define, print and remove aliases.
 *
 * @since 1.0
 */
final class AliasBuiltins {

    @GeneratedDefensive
    private AliasBuiltins() {
    }

    /**
     * Implementation of {@code alias [name[=value]...]}.
     * POSIX 2.14: without args, prints all aliases.
     * With name=value, sets an alias. With plain name, prints that alias.
     *
     * @param self The context
     * @param call Arguments, environment and inline assignments
     * @return The exit code: 0 on success, 1 if any named alias is undefined
     */
    static int doAlias(final BuiltinContext self, final Invocation call) {
        final List<String> args = call.arguments();
        final List<String> assigns = call.assignments();
        final Map<String, String> aliases = self.aliases();
        int code = 0;
        if (args.isEmpty() && assigns.isEmpty()) {
            for (final Map.Entry<String, String> entry : aliases.entrySet()) {
                self.outStream().println(
                    String.format("%s='%s'", entry.getKey(), entry.getValue())
                );
            }
        } else {
            final List<String> all = new ArrayList<>(args);
            all.addAll(assigns);
            for (final String arg : all) {
                if (!AliasBuiltins.applyAliasArg(self, aliases, arg)) {
                    code = 1;
                }
            }
        }
        return code;
    }

    /**
     * Implementation of {@code unalias [-a] name...}.
     * POSIX 2.14: removes named aliases, or all aliases with -a.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code: 0 on success, 1 if any named alias doesn't exist
     */
    static int doUnalias(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final Map<String, String> aliases = self.aliases();
        int code = 0;
        boolean all = false;
        int idx = 0;
        if (!args.isEmpty() && "-a".equals(args.get(0))) {
            all = true;
            idx = 1;
        }
        if (all) {
            aliases.clear();
        } else {
            for (; idx < args.size(); idx = idx + 1) {
                final String name = args.get(idx);
                if (aliases.containsKey(name)) {
                    aliases.remove(name);
                } else {
                    code = 1;
                }
            }
        }
        return code;
    }

    private static boolean applyAliasArg(
        final BuiltinIo self, final Map<String, String> aliases, final String arg
    ) {
        boolean result = true;
        final int eqidx = arg.indexOf('=');
        if (eqidx >= 0) {
            String value = arg.substring(eqidx + 1);
            if (AliasBuiltins.isQuoteWrapped(value)) {
                value = value.substring(1, value.length() - 1);
            }
            aliases.put(arg.substring(0, eqidx), value);
        } else if (aliases.containsKey(arg)) {
            self.outStream().println(
                String.format("%s='%s'", arg, aliases.get(arg))
            );
        } else {
            result = false;
        }
        return result;
    }

    private static boolean isQuoteWrapped(final String text) {
        final int edge = text.length() - 1;
        return edge >= 1
            && (text.charAt(0) == '\'' || text.charAt(0) == '"')
            && text.charAt(edge) == text.charAt(0);
    }
}
