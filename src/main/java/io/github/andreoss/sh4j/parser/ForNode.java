/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.List;

/**
 * AST node for the POSIX {@code for var in words; do body; done} loop.
 *
 * @since 1.0
 */
public final class ForNode extends Node {

    /** Loop variable name. */
    private final String variable;
    /** Words the loop iterates over (each iteration assigns one). */
    private final List<String> words;
    /** Body executed once per word. */
    private final ListNode body;

    /**
     * Creates a for-loop node.
     *
     * @param variable name of the loop variable
     * @param words    words to iterate (already lexed, not yet expanded)
     * @param body     body to execute per iteration
     */
    public ForNode(final String variable, final List<String> words, final ListNode body) {
        super();
        this.variable = variable;
        this.words = words;
        this.body = body;
    }

    /**
     * @return the loop variable name
     */
    public String getVariable() {
        return this.variable;
    }

    /**
     * @return the iteration words
     */
    public List<String> getWords() {
        return this.words;
    }

    /**
     * @return the loop body
     */
    public ListNode getBody() {
        return this.body;
    }
}
