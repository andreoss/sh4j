/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * AST node for a brace-group command list, written as
 * <code>&#123; list; &#125;</code> in POSIX. Unlike {@link Subshell}, the
 * list executes in the current shell environment.
 *
 * @since 1.0
 */
public final class Grouping implements Node {

    /** The command list to run inside the group. */
    private final ListNode list;

    /**
     * Creates a grouping node wrapping the given command list.
     *
     * @param list The list to execute
     */
    public Grouping(final ListNode list) {
        this.list = list;
    }

    /**
     * Returns the wrapped command list.
     *
     * @return The wrapped command list
     */
    public ListNode getList() {
        return this.list;
    }
}
