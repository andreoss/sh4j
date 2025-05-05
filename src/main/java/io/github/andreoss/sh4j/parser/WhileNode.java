/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * AST node for {@code while} and {@code until} loops. The two share their
 * structure; {@link #isUntil()} distinguishes them.
 *
 * @since 1.0
 */
public final class WhileNode extends Node {

    /** Loop condition; re-evaluated each iteration. */
    private final ListNode condition;
    /** Loop body. */
    private final ListNode body;
    /** True when the keyword was {@code until} (inverts the predicate). */
    private final boolean until;

    /**
     * Creates a while-loop or until-loop node.
     *
     * @param condition Predicate command list
     * @param body      List to run while the predicate matches
     * @param until     True for {@code until} semantics, false for {@code while}
     */
    public WhileNode(final ListNode condition, final ListNode body, final boolean until) {
        super();
        this.condition = condition;
        this.body = body;
        this.until = until;
    }

    /**
     * @return The loop's predicate
     */
    public ListNode getCondition() {
        return this.condition;
    }

    /**
     * @return The loop body
     */
    public ListNode getBody() {
        return this.body;
    }

    /**
     * @return True when this is an {@code until} loop
     */
    public boolean isUntil() {
        return this.until;
    }
}
