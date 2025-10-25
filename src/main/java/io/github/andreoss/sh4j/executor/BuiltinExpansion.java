/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import java.util.Map;

/**
 * Word-expansion operations exposed to built-ins.
 * @since 1.0
 */
interface BuiltinExpansion {

    /**
     * Performs POSIX 2.6 word expansion on a single word.
     * @param arg Word to expand
     * @param env Environment view for variable lookup
     * @return The expanded text
     */
    String expandVariables(String arg, Map<String, String> env);

    /**
     * Resolves a one-character special parameter ({@code $?}, {@code $#}, ...).
     * @param name The character following the dollar sign
     * @return The parameter's current value
     */
    String getSpecialParam(char name);

    /**
     * Runs a command substitution and captures its stdout.
     * @param text The body between {@code $(} and {@code )}
     * @return The captured stdout, trailing newline stripped
     */
    String executeCommandSubstitution(String text);

    /**
     * Evaluates an arithmetic expansion.
     * @param expr The body between {@code $((} and {@code ))}
     * @param env  Environment view for variable lookup
     * @return The textual result
     */
    String evalArithmetic(String expr, Map<String, String> env);

    /**
     * Expands one word into zero or more fields, honouring quoted
     * {@code "$@"} boundaries and field splitting.
     * @param arg Source word
     * @param env Expansion environment view
     * @return The expanded fields
     */
    List<String> expandFields(String arg, Map<String, String> env);

    /**
     * Expands one word into fields and applies pathname expansion when the
     * word is unquoted and globbing is enabled.
     * @param arg Source word
     * @param env Expansion environment view
     * @return The expanded fields
     */
    List<String> expandArguments(String arg, Map<String, String> env);

    /**
     * Looks up a variable, taking positional parameters into account.
     * @param name Variable name
     * @param env  Environment view
     * @return The variable's current value, or empty string when unset
     */
    String lookupVariable(String name, Map<String, String> env);

    /**
     * Returns the live shell-variable map.
     * @return The live shell-variable map (mutating it propagates to the executor)
     */
    Map<String, String> environmentMap();

    /**
     * Returns a copy containing only variables marked for export.
     * @return A copy containing only variables marked for export
     */
    Map<String, String> exportedEnvironment();
}
