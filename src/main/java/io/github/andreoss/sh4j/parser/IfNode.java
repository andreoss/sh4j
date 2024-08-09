/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

public class IfNode extends Node {
    private final ListNode condition;
    private final ListNode thenBody;
    private final ListNode elseBody;

    public IfNode(ListNode condition, ListNode thenBody, ListNode elseBody) {
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    public ListNode getCondition() {
        return condition;
    }

    public ListNode getThenBody() {
        return thenBody;
    }

    public ListNode getElseBody() {
        return elseBody;
    }
}
