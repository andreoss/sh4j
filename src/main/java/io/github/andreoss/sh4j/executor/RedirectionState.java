/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import java.io.File;
import java.util.Map;

/**
 * Mutable stream-swap state used by {@link RedirectionHandler} to translate
 * redirections into concrete I/O wiring for built-ins.
 *
 * @since 1.0
 */
final class RedirectionState {

    /** Current live stdout. */
    private PrintSink out;

    /** Current live stderr. */
    private PrintSink err;

    /** Saved stdout, or null when not swapped. */
    private PrintSink savedout;

    /** Saved stderr, or null when not swapped. */
    private PrintSink savederr;

    /** True when a redirection target could not be opened. */
    private boolean failed;

    /** Expansion, base directory, and bound descriptors. */
    private final RedirectionHandler.RedirectContext context;

    /**
     * Creates a state from the current streams.
     *
     * @param stdout Current stdout
     * @param stderr Current stderr
     * @param context Expansion, base directory, and bound descriptors
     */
    RedirectionState(
        final PrintSink stdout, final PrintSink stderr,
        final RedirectionHandler.RedirectContext context
    ) {
        this.out = stdout;
        this.err = stderr;
        this.failed = false;
        this.context = context;
    }

    /**
     * Expands a redirection target.
     *
     * @param target Raw target
     * @return Expanded target
     */
    String expand(final String target) {
        return this.context.expander().apply(target);
    }

    /**
     * Returns the directory for relative targets.
     *
     * @return Base directory (null = process cwd)
     */
    File base() {
        return this.context.base();
    }

    /**
     * Returns the descriptors bound by previous redirections.
     *
     * @return Bound descriptors
     */
    Map<Integer, PrintSink> descriptors() {
        return this.context.descriptors();
    }

    /**
     * Replaces the live stdout, remembering the previous one.
     *
     * @param value New stdout
     */
    void replaceOut(final PrintSink value) {
        if (this.savedout == null) {
            this.savedout = this.out;
        }
        this.out = value;
    }

    /**
     * Replaces the live stderr, remembering the previous one.
     *
     * @param value New stderr
     */
    void replaceErr(final PrintSink value) {
        if (this.savederr == null) {
            this.savederr = this.err;
        }
        this.err = value;
    }

    /**
     * Makes stderr point at stdout.
     */
    void dupErrToOut() {
        if (this.savederr == null) {
            this.savederr = this.err;
        }
        this.err = this.out;
    }

    /**
     * Makes stdout point at stderr.
     */
    void dupOutToErr() {
        if (this.savedout == null) {
            this.savedout = this.out;
        }
        this.out = this.err;
    }

    /**
     * Marks the redirection as failed.
     */
    void fail() {
        this.failed = true;
    }

    /**
     * Reports a failed redirection on the live stderr.
     *
     * @param message The message
     */
    void complain(final String message) {
        this.err.println(message);
    }

    /**
     * Returns the immutable swap record.
     *
     * @return The swap record
     */
    RedirectionHandler.StreamSwap swap() {
        return new RedirectionHandler.StreamSwap(
            this.out, this.err, this.savedout, this.savederr, this.failed
        );
    }
}
