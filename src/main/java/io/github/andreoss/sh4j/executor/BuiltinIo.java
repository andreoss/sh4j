/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.ListNode;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * I/O and command-dispatch operations exposed to built-ins.
 *
 * @since 1.0
 */
interface BuiltinIo {

    /**
     * Returns the stderr stream.
     *
     * @return The stderr stream the executor writes diagnostics to
     */
    PrintStream errStream();

    /**
     * Returns the stdout stream.
     *
     * @return The stdout stream the executor writes built-in output to
     */
    PrintStream outStream();

    /**
     * Returns the current working directory.
     *
     * @return The shell's current working directory
     */
    File currentDirectory();

    /**
     * Changes the executor's current working directory.
     *
     * @param dir New working directory
     */
    void setCurrentDirectory(File dir);

    /**
     * Checks whether a name is a built-in.
     *
     * @param name Command name
     * @return True when the dispatcher has a built-in registered under the name
     */
    boolean isBuiltinName(String name);

    /**
     * Resolves a command name to its absolute path via PATH.
     *
     * @param name The command name as written in source
     * @return The absolute path, or null when not found
     */
    String resolveCommand(String name);

    /**
     * Recursively executes another AST list (used by {@code eval}).
     *
     * @param list The list to execute
     * @return The list's exit status
     */
    int execute(ListNode list);

    /**
     * Runs a command bypassing function and alias lookup (used by the
     * {@code command} built-in).
     *
     * @param name Command name
     * @param args Raw argument words
     * @param env Expansion environment view
     * @return Exit status
     */
    int executeBypassingFunctions(String name, List<String> args, Map<String, String> env);
}
