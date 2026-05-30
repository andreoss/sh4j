/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * POSIX 2.6 word expansion of built-in operands, done once before the
 * built-in runs. Assignment operands of {@code export} and {@code readonly}
 * expand as assignments: no field splitting and no pathname expansion.
 *
 * @since 1.0
 */
final class BuiltinWords {

    /**
     * Built-ins whose assignment operands expand as assignments.
     */
    private static final Set<String> DECLARING = Set.of("export", "readonly");

    /**
     * An assignment word: a name followed by an equals sign.
     */
    private static final Pattern ASSIGNMENT = Pattern.compile(
        "[A-Za-z_][A-Za-z0-9_]*=.*", Pattern.DOTALL
    );

    /**
     * The shell that expands the words.
     */
    private final BuiltinContext shell;

    /**
     * Ctor.
     *
     * @param shell The shell that expands the words
     */
    BuiltinWords(final BuiltinContext shell) {
        this.shell = shell;
    }

    /**
     * Expands the operand words of a built-in.
     *
     * @param name The built-in name
     * @param words The raw operand words
     * @param env The environment to expand against
     * @return The expanded operands
     */
    List<String> expand(
        final String name, final List<String> words, final Map<String, String> env
    ) {
        final boolean declaring = BuiltinWords.DECLARING.contains(name);
        final List<String> fields = new ArrayList<>(words.size());
        for (final String word : words) {
            if (declaring && BuiltinWords.ASSIGNMENT.matcher(word).matches()) {
                fields.add(this.assignment(word, env));
            } else {
                fields.addAll(this.shell.expandArguments(word, env));
            }
        }
        return fields;
    }

    private String assignment(final String word, final Map<String, String> env) {
        final int eqidx = word.indexOf('=');
        return String.join(
            "=",
            word.substring(0, eqidx),
            WordExpander.removeQuotes(
                this.shell.expandVariables(
                    AssignedTilde.expand(word.substring(eqidx + 1), env), env
                )
            )
        );
    }
}
