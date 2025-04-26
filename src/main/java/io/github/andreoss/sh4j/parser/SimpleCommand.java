/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * AST node for a single command invocation: a name, its arguments, any
 * inline assignments, and any redirections.
 *
 * @since 1.0
 */
public final class SimpleCommand extends Node {

    /** Command name as parsed; may be null for assignment-only commands. */
    private final String name;
    /** Positional arguments in source order. */
    private final List<String> arguments;
    /** Redirections in source order. */
    private final List<Redirection> redirections;
    /** Inline {@code KEY=VALUE} assignments in source order. */
    private final List<String> assignments;

    /**
     * Creates a simple-command node.
     *
     * @param name the command name (may be null for assignment-only commands)
     */
    public SimpleCommand(final String name) {
        super();
        this.name = name;
        this.arguments = new ArrayList<>();
        this.redirections = new ArrayList<>();
        this.assignments = new ArrayList<>();
    }

    /**
     * @return the command name (may be null)
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return positional arguments
     */
    public List<String> getArguments() {
        return this.arguments;
    }

    /**
     * @return inline redirections
     */
    public List<Redirection> getRedirections() {
        return this.redirections;
    }

    /**
     * @return inline assignments ({@code KEY=VALUE} strings)
     */
    public List<String> getAssignments() {
        return this.assignments;
    }

    /**
     * Appends a positional argument.
     *
     * @param arg the argument text
     */
    public void addArgument(final String arg) {
        this.arguments.add(arg);
    }

    /**
     * Appends a redirection.
     *
     * @param redir the redirection to attach
     */
    public void addRedirection(final Redirection redir) {
        this.redirections.add(redir);
    }

    /**
     * Appends an inline assignment.
     *
     * @param assignment the {@code KEY=VALUE} string
     */
    public void addAssignment(final String assignment) {
        this.assignments.add(assignment);
    }
}
