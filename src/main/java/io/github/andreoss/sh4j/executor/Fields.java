/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Expands a word into fields, with optional pathname expansion.
 *
 * @since 1.0
 */
final class Fields {

    /** The shell that expands the words. */
    private final BuiltinContext shell;

    /**
     * Ctor.
     *
     * @param shell The shell that expands the words
     */
    Fields(final BuiltinContext shell) {
        this.shell = shell;
    }

    /**
     * Expands a word into fields followed by pathname expansion.
     *
     * @param arg The raw word
     * @param env The environment to expand against
     * @return The resulting fields
     */
    List<String> arguments(final String arg, final Map<String, String> env) {
        List<String> fields = this.split(arg, env);
        if (arg != null && !WordExpander.isQuoted(arg)
            && this.shell.shellFlags().indexOf('f') < 0) {
            fields = WordExpander.pathnameExpand(
                fields, this.shell.currentDirectory()
            );
        }
        return fields;
    }

    /**
     * Expands a word into fields without pathname expansion.
     *
     * @param arg The raw word
     * @param env The environment to expand against
     * @return The resulting fields
     */
    List<String> split(final String arg, final Map<String, String> env) {
        final List<String> fields = new ArrayList<>(0);
        if ("\"$@\"".equals(arg) || "\"${@}\"".equals(arg)) {
            final List<String> params = this.shell.positionalParamsList();
            if (params.size() > 1) {
                fields.addAll(params.subList(1, params.size()));
            }
        } else if (arg != null) {
            fields.addAll(this.expanded(arg, env));
        }
        return fields;
    }

    private List<String> expanded(final String arg, final Map<String, String> env) {
        final List<String> fields = new ArrayList<>(0);
        final String expanded = this.shell.expandVariables(arg, env);
        if (WordExpander.isQuoted(arg)) {
            fields.add(WordExpander.removeQuotes(expanded));
        } else {
            final String ifsvalue = env.getOrDefault("IFS", Variables.DEFAULT_IFS);
            for (final String field : WordExpander.splitFields(expanded, ifsvalue)) {
                fields.add(WordExpander.removeQuotes(field));
            }
        }
        return fields;
    }
}
