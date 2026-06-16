/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Pipeline;
import java.util.function.Supplier;

/**
 * Runs one pipeline of an asynchronous list in the subshell environment
 * copied when the list was started (POSIX 2.9.3.1).
 *
 * @since 1.0
 */
final class BackgroundTask implements Supplier<Integer> {

    /** Subshell environment copied when the list was started. */
    private final Executor child;

    /** The pipeline to run. */
    private final Pipeline pipeline;

    /**
     * Ctor.
     *
     * @param child Subshell environment copied when the list was started
     * @param pipeline The pipeline to run
     */
    BackgroundTask(final Executor child, final Pipeline pipeline) {
        this.child = child;
        this.pipeline = pipeline;
    }

    @Override
    public Integer get() {
        final ListNode job = new ListNode();
        job.addPipeline(this.pipeline, ListNode.Separator.NONE);
        return this.child.leave(this.child.execute(job));
    }
}
