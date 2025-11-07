/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * AST node for a pipeline: one or more commands joined by {@code |}, with
 * an optional leading {@code !} that inverts the exit status.
 * @since 1.0
 */
public final class Pipeline extends Node {

    /** Stages of the pipeline in source order. */
    private final List<Node> commands;
    /** True when the pipeline was prefixed with {@code !}. */
    private final boolean bang;

    /**
     * Creates an empty pipeline that callers populate via
     * {@link #addCommand(Node)}.
     * @param bang Whether to invert the final exit status
     */
    public Pipeline(final boolean bang) {
        super();
        this.commands = new ArrayList<>();
        this.bang = bang;
    }

    /**
     * Returns the list of pipeline stages.
     * @return The list of pipeline stages
     */
    public List<Node> getCommands() {
        return this.commands;
    }

    /**
     * Checks whether the pipeline is logically negated.
     * @return True when the pipeline is logically negated
     */
    public boolean isBang() {
        return this.bang;
    }

    /**
     * Appends a stage to this pipeline.
     * @param cmd The AST node for the next stage
     */
    public void addCommand(final Node cmd) {
        this.commands.add(cmd);
    }
}
