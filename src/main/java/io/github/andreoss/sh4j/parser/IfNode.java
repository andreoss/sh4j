/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * AST node for an {@code if/then/else/fi} compound command. The else
 * branch is the same shape as the then branch; {@code elif} chains are
 * desugared by the parser into nested {@code IfNode} instances in the
 * else slot.
 *
 * @since 1.0
 */
public final class IfNode extends Node {

    /** The list whose exit status decides the branch. */
    private final ListNode condition;
    /** Body executed when the condition's last command exits 0. */
    private final ListNode then;
    /** Body executed otherwise; null when no else / elif clause exists. */
    private final ListNode otherwise;

    /**
     * Creates an if-node.
     *
     * @param condition list whose final exit status drives the branch
     * @param then      body for the success branch
     * @param otherwise body for the failure branch (may be null)
     */
    public IfNode(final ListNode condition, final ListNode then, final ListNode otherwise) {
        super();
        this.condition = condition;
        this.then = then;
        this.otherwise = otherwise;
    }

    /**
     * @return the if-condition's command list
     */
    public ListNode getCondition() {
        return this.condition;
    }

    /**
     * @return the then-branch command list
     */
    public ListNode getThenBody() {
        return this.then;
    }

    /**
     * @return the else-branch command list, or null
     */
    public ListNode getElseBody() {
        return this.otherwise;
    }
}
