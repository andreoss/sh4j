/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * AST node for a sequence of pipelines joined by separators. This is the
 * top-level shape returned by {@link Parser#parse()}; nested constructs
 * (then-bodies, loop bodies, case-arm bodies, ...) are also ListNodes.
 *
 * @since 1.0
 */
public final class ListNode implements Node {

    /**
     * How two pipelines are joined.
     *
     * @since 1.0
     */
    public enum Separator {

        /** No explicit separator (last pipeline of the list). */
        NONE,

        /** Sequential execution: {@code ;}. */
        SEMI,

        /** Background execution: {@code &}. */
        AMP,

        /** Short-circuit AND: {@code &&}. */
        AND_IF,

        /** Short-circuit OR: {@code ||}. */
        OR_IF
    }

    /** Pipelines in source order. */
    private final List<Pipeline> pipelines;

    /** Separator that follows each pipeline (parallel to {@link #pipelines}). */
    private final List<Separator> separators;

    /**
     * Creates an empty list that callers populate via
     * {@link #addPipeline(Pipeline, Separator)}.
     */
    public ListNode() {
        this.pipelines = new ArrayList<>(0);
        this.separators = new ArrayList<>(0);
    }

    /**
     * Returns the pipelines in source order.
     *
     * @return The pipelines in source order
     */
    public List<Pipeline> getPipelines() {
        return this.pipelines;
    }

    /**
     * Returns the separator slots.
     *
     * @return Separator slots, parallel to {@link #getPipelines()}
     */
    public List<Separator> getSeparators() {
        return this.separators;
    }

    /**
     * Appends a pipeline and its trailing separator.
     *
     * @param pipeline The pipeline to append
     * @param sep      The separator that follows it (or {@link Separator#NONE})
     */
    public void addPipeline(final Pipeline pipeline, final Separator sep) {
        this.pipelines.add(pipeline);
        this.separators.add(sep);
    }
}
