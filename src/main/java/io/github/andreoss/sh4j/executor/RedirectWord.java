/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.function.UnaryOperator;

/**
 * Expansion of the word after a redirection operator (POSIX 2.7): tilde,
 * parameter, command substitution and arithmetic expansion followed by
 * quote removal, done once and without field splitting.
 *
 * @since 1.0
 */
final class RedirectWord {

    @GeneratedDefensive
    private RedirectWord() {
    }

    /**
     * Expands a redirection target.
     *
     * @param expander Word expansion that keeps the quotes of the word
     * @param word The raw target word
     * @return The expanded target with its quotes removed
     */
    static String expand(final UnaryOperator<String> expander, final String word) {
        return WordExpander.removeQuotes(expander.apply(word));
    }
}
