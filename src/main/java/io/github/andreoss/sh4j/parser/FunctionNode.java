/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.List;

/**
 * AST node for a function definition: {@code name() compound-command}.
 *
 * @since 1.0
 */
public final class FunctionNode implements Node {

    /** Function name as written in source. */
    private final String name;

    /** The compound command that runs when the function is invoked. */
    private final Node body;

    /** Redirections applied each time the function is invoked. */
    private final List<Redirection> redirections;

    /**
     * Creates a function-definition node.
     *
     * @param name Function name
     * @param body The function's compound body
     * @param redirections Redirections of the compound body
     */
    public FunctionNode(
        final String name, final Node body, final List<Redirection> redirections
    ) {
        this.name = name;
        this.body = body;
        this.redirections = redirections;
    }

    /**
     * Returns the function name.
     *
     * @return The function name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the function body.
     *
     * @return The function body
     */
    public Node getBody() {
        return this.body;
    }

    /**
     * Returns the redirections of the function body.
     *
     * @return Redirections in source order
     */
    public List<Redirection> getRedirections() {
        return this.redirections;
    }
}
