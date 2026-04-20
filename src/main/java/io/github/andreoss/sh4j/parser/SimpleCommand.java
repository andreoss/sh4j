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
public final class SimpleCommand implements Node {

    /** Command name as parsed; may be null for assignment-only commands. */
    private final String name;

    /** Positional arguments in source order. */
    private final List<String> arguments;

    /** Redirections in source order. */
    private final List<Redirection> redirections;

    /** Inline {@code KEY=VALUE} assignments in source order. */
    private final List<String> assignments;

    /** One-based source line number where this command starts. */
    private int line;

    /**
     * Creates a simple-command node.
     *
     * @param name The command name (may be null for assignment-only commands)
     */
    public SimpleCommand(final String name) {
        this.name = name;
        this.arguments = new ArrayList<>(0);
        this.redirections = new ArrayList<>(0);
        this.assignments = new ArrayList<>(0);
        this.line = 1;
    }

    /**
     * Returns the command name.
     *
     * @return The command name (may be null)
     */
    public String getName() {
        return this.name;
    }

    @Override
    public int getLineNumber() {
        return this.line;
    }

    /**
     * Sets the source line number.
     *
     * @param value One-based line number
     */
    public void setLineNumber(final int value) {
        this.line = value;
    }

    /**
     * Returns the positional arguments.
     *
     * @return Positional arguments
     */
    public List<String> getArguments() {
        return this.arguments;
    }

    /**
     * Returns the inline redirections.
     *
     * @return Inline redirections
     */
    public List<Redirection> getRedirections() {
        return this.redirections;
    }

    /**
     * Returns the inline assignments.
     *
     * @return Inline assignments ({@code KEY=VALUE} strings)
     */
    public List<String> getAssignments() {
        return this.assignments;
    }

    /**
     * Appends a positional argument.
     *
     * @param arg The argument text
     */
    public void addArgument(final String arg) {
        this.arguments.add(arg);
    }

    /**
     * Appends a redirection.
     *
     * @param redir The redirection to attach
     */
    public void addRedirection(final Redirection redir) {
        this.redirections.add(redir);
    }

    /**
     * Appends an inline assignment.
     *
     * @param assignment The {@code KEY=VALUE} string
     */
    public void addAssignment(final String assignment) {
        this.assignments.add(assignment);
    }

    @Override
    public String toString() {
        final StringBuilder out = new StringBuilder();
        if (this.name != null) {
            out.append(this.name);
        }
        for (final String arg : this.arguments) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(arg);
        }
        return out.toString();
    }
}
