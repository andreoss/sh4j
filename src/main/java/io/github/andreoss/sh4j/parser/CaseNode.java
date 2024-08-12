/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.List;

public class CaseNode extends Node {
    private final String word;
    private final List<CaseItem> items;

    public CaseNode(String word, List<CaseItem> items) {
        this.word = word;
        this.items = items;
    }

    public String getWord() {
        return word;
    }

    public List<CaseItem> getItems() {
        return items;
    }

    public static class CaseItem {
        private final List<String> patterns;
        private final ListNode body;

        public CaseItem(List<String> patterns, ListNode body) {
            this.patterns = patterns;
            this.body = body;
        }

        public List<String> getPatterns() {
            return patterns;
        }

        public ListNode getBody() {
            return body;
        }
    }
}
