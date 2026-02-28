/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * Marker base type for every AST node produced by {@link Parser}.
 *
 * <p>The base intentionally exposes no behaviour: each subclass (e.g.
 * {@link SimpleCommand}, {@link Pipeline}, {@link IfNode}) defines its own
 * shape and is interpreted by the executor via a {@code switch} over
 * concrete types rather than via virtual dispatch.</p>
 *
 * @since 1.0
 */
public class Node {

    /**
     * Creates a new AST node.
     */
    public Node() {
        // nothing to initialise
    }

    /**
     * Returns the line number where this node was parsed.
     *
     * @return The 1-based line number where this node was parsed
     */
    public int getLineNumber() {
        return 0;
    }
}
