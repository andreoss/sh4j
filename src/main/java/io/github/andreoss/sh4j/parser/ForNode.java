/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.List;

public class ForNode extends Node {
    private final String variable;
    private final List<String> words;
    private final ListNode body;

    public ForNode(String variable, List<String> words, ListNode body) {
        this.variable = variable;
        this.words = words;
        this.body = body;
    }

    public String getVariable() {
        return variable;
    }

    public List<String> getWords() {
        return words;
    }

    public ListNode getBody() {
        return body;
    }
}
