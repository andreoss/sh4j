/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * AST node for a function definition: {@code name() compound-command}.
 *
 * @since 1.0
 */
public final class FunctionNode extends Node {

    /** Function name as written in source. */
    private final String name;
    /** The compound command that runs when the function is invoked. */
    private final Node body;

    /**
     * Creates a function-definition node.
     *
     * @param name function name
     * @param body the function's compound body
     */
    public FunctionNode(final String name, final Node body) {
        super();
        this.name = name;
        this.body = body;
    }

    /**
     * @return the function name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the function body
     */
    public Node getBody() {
        return this.body;
    }
}
