/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * Common type of every AST node produced by {@link Parser}.
 *
 * <p>Each node type (e.g.
 * {@link SimpleCommand}, {@link Pipeline}, {@link IfNode}) defines its own
 * shape and is interpreted by the executor via a {@code switch} over
 * concrete types rather than via virtual dispatch.</p>
 *
 * @since 1.0
 */
public interface Node {

    /**
     * Returns the line number where this node was parsed.
     *
     * @return The 1-based line number, or zero when not tracked
     */
    default int getLineNumber() {
        return 0;
    }
}
