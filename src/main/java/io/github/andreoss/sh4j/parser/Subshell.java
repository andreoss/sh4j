/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * AST node for a subshell command list, written as {@code (list)} in
 * POSIX. The list is executed in a forked environment in real shells; sh4j
 * approximates that by running the list in the same JVM but with a copied
 * environment view.
 * @since 1.0
 */
public final class Subshell extends Node {

    /** The command list to run inside the subshell. */
    private final ListNode list;

    /**
     * Creates a subshell node wrapping the given command list.
     * @param list The list to execute
     */
    public Subshell(final ListNode list) {
        super();
        this.list = list;
    }

    /**
     * Returns the wrapped command list.
     * @return The wrapped command list
     */
    public ListNode getList() {
        return this.list;
    }
}
