/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import io.github.andreoss.sh4j.parser.FunctionNode;
import io.github.andreoss.sh4j.parser.ListNode;

/**
 * Narrow façade that built-ins use to reach back into the executor for
 * shared resources (environment, I/O streams, expansion helpers, ...)
 * without having to depend on the full {@code Executor} class.
 *
 * @since 1.0
 */
interface BuiltinContext {

    /**
     * Performs POSIX 2.6 word expansion on a single word.
     *
     * @param arg word to expand
     * @param env environment view for variable lookup
     * @return the expanded text
     */
    String expandVariables(String arg, Map<String, String> env);

    /**
     * Resolves a one-character special parameter ({@code $?}, {@code $#}, ...).
     *
     * @param name the character following the dollar sign
     * @return the parameter's current value
     */
    String getSpecialParam(char name);

    /**
     * Runs a command substitution and captures its stdout.
     *
     * @param text the body between {@code $(} and {@code )}
     * @return the captured stdout, trailing newline stripped
     */
    String executeCommandSubstitution(String text);

    /**
     * Evaluates an arithmetic expansion.
     *
     * @param expr the body between {@code $((} and {@code ))}
     * @param env  environment view for variable lookup
     * @return the textual result
     */
    String evalArithmetic(String expr, Map<String, String> env);

    /**
     * Looks up a variable, taking positional parameters into account.
     *
     * @param name variable name
     * @param env  environment view
     * @return the variable's current value, or empty string when unset
     */
    String lookupVariable(String name, Map<String, String> env);

    /**
     * @return the stderr stream the executor writes diagnostics to
     */
    PrintStream errStream();

    /**
     * Signals that a word expansion failed (e.g. {@code ${var:?}}).
     */
    void setWordExpansionError();

    /**
     * @return the live environment map (mutating it propagates to the executor)
     */
    Map<String, String> environmentMap();

    /**
     * @return the live positional-parameter list
     */
    List<String> positionalParamsList();

    /**
     * @return the live function-name to {@link FunctionNode} map
     */
    Map<String, FunctionNode> functionsMap();

    /**
     * @return the stdout stream the executor writes built-in output to
     */
    PrintStream outStream();

    /**
     * Resolves a command name to its absolute path via PATH.
     *
     * @param name the command name as written in source
     * @return the absolute path, or null when not found
     */
    String resolveCommand(String name);

    /**
     * @return whether a prior word expansion set the error flag
     */
    boolean wordExpansionErrorOccurred();

    /**
     * Clears the word-expansion error flag.
     */
    void clearWordExpansionError();

    /**
     * Maps an IO failure to the POSIX exit code the shell should return.
     *
     * @param exc the exception caught while invoking an external command
     * @return 127 (not found) or 126 (not executable)
     */
    int exitCodeForIoException(IOException exc);

    /**
     * Recursively executes another AST list (used by {@code eval}).
     *
     * @param list the list to execute
     * @return the list's exit status
     */
    int execute(ListNode list);
}
