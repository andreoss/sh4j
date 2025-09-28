/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.io.InputStream;
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
 * @since 1.0
 */
interface BuiltinContext {

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
     * Looks up a variable, taking positional parameters into account.
     * @param name Variable name
     * @param env  Environment view
     * @return The variable's current value, or empty string when unset
     */
    String lookupVariable(String name, Map<String, String> env);

    /**
     * @return The stderr stream the executor writes diagnostics to
     */
    PrintStream errStream();

    /**
     * Signals that a word expansion failed (e.g. {@code ${var:?}}).
     */
    void setWordExpansionError();

    /**
     * @return The live shell-variable map (mutating it propagates to the executor)
     */
    Map<String, String> environmentMap();

    /**
     * @return A copy containing only variables marked for export
     */
    Map<String, String> exportedEnvironment();

    /**
     * Marks a variable for export to child processes.
     * @param name Variable name
     */
    void markExported(String name);

    /**
     * @param name Variable name
     * @return True when the variable is exported
     */
    boolean isExported(String name);

    /**
     * Marks a variable read-only.
     * @param name Variable name
     */
    void markReadonly(String name);

    /**
     * @param name Variable name
     * @return True when the variable is read-only
     */
    boolean isReadonly(String name);

    /**
     * Removes a variable unless it is read-only.
     * @param name Variable name
     * @return True when the variable was removed
     */
    boolean unsetVariable(String name);

    /**
     * @return The executor's current working directory
     */
    File currentDirectory();

    /**
     * Changes the executor's current working directory.
     * @param dir New working directory
     */
    void setCurrentDirectory(File dir);

    /**
     * @param name Command name
     * @return True when the dispatcher has a built-in registered under the name
     */
    boolean isBuiltinName(String name);

    /**
     * Reads one line from the executor's stdin without buffering past the
     * newline.
     * @return The line without its terminator, or null on EOF
     */
    String readInputLine();

    /**
     * Stores a trap action.
     * @param signal Signal name (for example EXIT)
     * @param action Shell text to run, or null to remove the trap
     */
    void setTrap(String signal, String action);

    /**
     * @return A live view of the trap table
     */
    Map<String, String> traps();

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
     * Runs a command bypassing function and alias lookup (used by the
     * {@code command} built-in).
     * @param name Command name
     * @param args Raw argument words
     * @param env Expansion environment view
     * @return Exit status
     */
    int executeBypassingFunctions(String name, List<String> args, Map<String, String> env);

    /**
     * @return The live positional-parameter list
     */
    List<String> positionalParamsList();

    /**
     * @return The live function-name to {@link FunctionNode} map
     */
    Map<String, FunctionNode> functionsMap();

    /**
     * @return The live alias table (name → replacement text)
     */
    Map<String, String> aliases();

    /**
     * @return The job manager for background job control
     */
    JobManager jobManager();

    /**
     * @return The command history for fc built-in
     */
    CommandHistory history();

    /**
     * @return The stdout stream the executor writes built-in output to
     */
    PrintStream outStream();

    /**
     * @return The stdin stream the executor reads from
     */
    InputStream stdin();

    /**
     * Resolves a command name to its absolute path via PATH.
     * @param name The command name as written in source
     * @return The absolute path, or null when not found
     */
    String resolveCommand(String name);

    /**
     * @return Whether a prior word expansion set the error flag
     */
    boolean wordExpansionErrorOccurred();

    /**
     * Clears the word-expansion error flag.
     */
    void clearWordExpansionError();

    /**
     * Maps an IO failure to the POSIX exit code the shell should return.
     * @param exc The exception caught while invoking an external command
     * @return 127 (not found) or 126 (not executable)
     */
    int exitCodeForIoException(IOException exc);

    /**
     * Recursively executes another AST list (used by {@code eval}).
     * @param list The list to execute
     * @return The list's exit status
     */
    int execute(ListNode list);

    /**
     * Waits for all background jobs spawned by this executor to complete.
     * Used by the {@code wait} built-in.
     */
    void waitForBackgroundJobs();

    /**
     * @return The identifier reported by {@code $!} for the most recent
     *     background job, or 0 when no background job has run yet
     */
    int lastBackgroundPid();

    /**
     * @return The current shell flags string (e.g. "e", "xu", etc.)
     */
    String shellFlags();

    /**
     * Sets the shell flags string.
     * @param flags The new flags (e.g. "e", "xu")
     */
    void setShellFlags(String flags);

    /**
     * Replaces the positional parameters with the given list.
     * @param params The new positional parameters (excluding $0)
     */
    void setPositionalParams(List<String> params);
}
