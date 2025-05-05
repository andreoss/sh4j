/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.List;

/**
 * AST node for a {@code case word in pat) body;; esac} compound command.
 *
 * @since 1.0
 */
public final class CaseNode extends Node {

    /** The word to match against each case-arm's patterns. */
    private final String word;
    /** The list of case-arms in source order. */
    private final List<CaseItem> items;

    /**
     * Creates a case-node.
     *
     * @param word  The word the executor matches against the patterns
     * @param items The case-arms, in source order
     */
    public CaseNode(final String word, final List<CaseItem> items) {
        super();
        this.word = word;
        this.items = items;
    }

    /**
     * @return The case-word
     */
    public String getWord() {
        return this.word;
    }

    /**
     * @return The case-arms
     */
    public List<CaseItem> getItems() {
        return this.items;
    }

    /**
     * A single case-arm: one or more patterns plus a body to execute when
     * any pattern matches.
     *
     * @since 1.0
     */
    public static final class CaseItem {

        /** Alternative patterns for this arm. */
        private final List<String> patterns;
        /** Body to execute when any pattern matches. */
        private final ListNode body;

        /**
         * Creates a case-arm.
         *
         * @param patterns Alternative patterns for this arm
         * @param body     Body to execute on match
         */
        public CaseItem(final List<String> patterns, final ListNode body) {
            this.patterns = patterns;
            this.body = body;
        }

        /**
         * @return This arm's alternative patterns
         */
        public List<String> getPatterns() {
            return this.patterns;
        }

        /**
         * @return This arm's body
         */
        public ListNode getBody() {
            return this.body;
        }
    }
}
