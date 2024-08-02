/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

public class Subshell extends Node {
    private final ListNode list;

    public Subshell(ListNode list) {
        this.list = list;
    }

    public ListNode getList() {
        return list;
    }
}
