/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.FunctionNode;
import java.util.List;
import java.util.Map;

/**
 * Variable and parameter state exposed to built-ins.
 *
 * @since 1.0
 */
interface BuiltinVariables {

    /**
     * Marks a variable for export to child processes.
     *
     * @param name Variable name
     */
    void markExported(String name);

    /**
     * Marks a variable read-only.
     *
     * @param name Variable name
     */
    void markReadonly(String name);

    /**
     * Checks whether a variable is read-only.
     *
     * @param name Variable name
     * @return True when the variable is read-only
     */
    boolean isReadonly(String name);

    /**
     * Removes a variable unless it is read-only.
     *
     * @param name Variable name
     * @return True when the variable was removed
     */
    boolean unsetVariable(String name);

    /**
     * Returns the live positional-parameter list.
     *
     * @return The live positional-parameter list
     */
    List<String> positionalParamsList();

    /**
     * Replaces the positional parameters with the given list.
     *
     * @param params The new positional parameters (excluding $0)
     */
    void setPositionalParams(List<String> params);

    /**
     * Returns the live function map.
     *
     * @return The live function-name to {@link FunctionNode} map
     */
    Map<String, FunctionNode> functionsMap();

    /**
     * Returns the live alias table.
     *
     * @return The live alias table (name to replacement text)
     */
    Map<String, String> aliases();

    /**
     * Reads one line from the executor's stdin without buffering past the
     * newline.
     *
     * @return The line with its newline, without one when EOF came first,
     *  or null when nothing was read
     */
    String readInputLine();
}
