/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

public class WhileNode extends Node {
    private final ListNode condition;
    private final ListNode body;
    private final boolean until;

    public WhileNode(ListNode condition, ListNode body, boolean until) {
        this.condition = condition;
        this.body = body;
        this.until = until;
    }

    public ListNode getCondition() {
        return condition;
    }

    public ListNode getBody() {
        return body;
    }

    public boolean isUntil() {
        return until;
    }
}
