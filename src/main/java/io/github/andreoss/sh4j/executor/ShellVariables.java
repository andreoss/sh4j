/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Live shell-variable table with its export and read-only markers.
 *
 * @since 1.0
 */
final class ShellVariables {

    /** Live shell-variable table. */
    private final Map<String, String> table;

    /** Names of variables visible to child processes. */
    private final Set<String> exported;

    /** Names of read-only variables. */
    private final Set<String> readonly;

    /**
     * Ctor.
     *
     * @param table Live shell-variable table
     * @param exported Names of variables visible to child processes
     */
    ShellVariables(final Map<String, String> table, final Set<String> exported) {
        this(table, exported, new HashSet<>());
    }

    private ShellVariables(
        final Map<String, String> table, final Set<String> exported,
        final Set<String> readonly
    ) {
        this.table = table;
        this.exported = exported;
        this.readonly = readonly;
    }

    /**
     * Returns the live variable table.
     *
     * @return The live variable table
     */
    Map<String, String> table() {
        return this.table;
    }

    /**
     * Returns the values of the exported variables.
     *
     * @return A fresh map of exported names to their values
     */
    Map<String, String> exports() {
        final Map<String, String> result = new HashMap<>();
        for (final String name : this.exported) {
            final String value = this.table.get(name);
            if (value != null) {
                result.put(name, value);
            }
        }
        return result;
    }

    /**
     * Marks a variable as exported.
     *
     * @param name The variable name
     */
    void export(final String name) {
        this.exported.add(name);
    }

    /**
     * Marks a variable as read-only.
     *
     * @param name The variable name
     */
    void protect(final String name) {
        this.readonly.add(name);
    }

    /**
     * Tells whether a variable is read-only.
     *
     * @param name The variable name
     * @return True when the variable is read-only
     */
    boolean locked(final String name) {
        return this.readonly.contains(name);
    }

    /**
     * Removes a variable unless it is read-only.
     *
     * @param name The variable name
     * @return True when the variable was removed
     */
    boolean unset(final String name) {
        final boolean removed;
        if (this.readonly.contains(name)) {
            removed = false;
        } else {
            this.table.remove(name);
            this.exported.remove(name);
            removed = true;
        }
        return removed;
    }

    /**
     * Replaces this table with a copy of another one.
     *
     * @param parent The table to copy
     */
    void inherit(final ShellVariables parent) {
        this.table.clear();
        this.table.putAll(parent.table);
        this.exported.clear();
        this.exported.addAll(parent.exported);
        this.readonly.addAll(parent.readonly);
    }
}
