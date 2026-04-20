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
public final class CaseNode implements Node {

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
        this.word = word;
        this.items = items;
    }

    /**
     * Returns the case-word.
     *
     * @return The case-word
     */
    public String getWord() {
        return this.word;
    }

    /**
     * Returns the case-arms.
     *
     * @return The case-arms
     */
    public List<CaseItem> getItems() {
        return this.items;
    }
}
