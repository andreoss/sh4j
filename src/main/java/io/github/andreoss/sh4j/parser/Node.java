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
public abstract class Node {

    /**
     * Default constructor for subclasses.
     */
    protected Node() {
        super();
    }
}
