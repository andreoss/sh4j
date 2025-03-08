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

public interface BuiltinContext {
    String expandVariables(String arg, Map<String, String> env);
    Map<String, String> environmentMap();
    List<String> positionalParamsList();
    Map<String, FunctionNode> functionsMap();
    PrintStream outStream();
    String resolveCommand(String name);
    boolean wordExpansionErrorOccurred();
    void clearWordExpansionError();
    int exitCodeForIOException(IOException e);
    int execute(ListNode list);
}
