/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.List;

/**
 * AST node for a compound command followed by redirections (POSIX
 * 2.9.4): the redirections apply to the whole command each time it runs.
 *
 * @since 1.0
 */
public final class RedirectedNode implements Node {

    /** The compound command. */
    private final Node body;

    /** Redirections applied around the compound command. */
    private final List<Redirection> redirections;

    /**
     * Creates a redirected compound command node.
     *
     * @param body The compound command
     * @param redirections Redirections in source order
     */
    public RedirectedNode(final Node body, final List<Redirection> redirections) {
        this.body = body;
        this.redirections = redirections;
    }

    /**
     * Returns the compound command.
     *
     * @return The compound command
     */
    public Node getBody() {
        return this.body;
    }

    /**
     * Returns the redirections of the compound command.
     *
     * @return Redirections in source order
     */
    public List<Redirection> getRedirections() {
        return this.redirections;
    }
}
