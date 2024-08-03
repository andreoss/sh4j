/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

public class Grouping extends Node {
    private final ListNode list;

    public Grouping(ListNode list) {
        this.list = list;
    }

    public ListNode getList() {
        return list;
    }
}
