/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.Map;

/**
 * Bundles the environment, builtin context, and output buffer used during
 * variable expansion so helper methods take fewer parameters.
 *
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidStringBufferField")
final class ExpansionContext {

    /** Environment variable bindings. */
    private final Map<String, String> env;

    /** Builtin execution context. */
    private final BuiltinContext ctx;

    /** Accumulator for expanded output. */
    private final StringBuilder expanded;

    /** Whether the current expansion sits inside double quotes. */
    private boolean dquote;

    /** Whether the expanded text is field split and quote removed later. */
    private boolean split;

    /** Whether the last braced expansion kept the quotes of its word. */
    private boolean kept;

    /** Whether unquoted literal characters of the word are marked. */
    private boolean mark;

    /** Whether the word sits inside a double-quoted braced expansion. */
    private boolean inner;

    /**
     * Creates an expansion context.
     *
     * @param env Environment variable bindings
     * @param ctx Builtin execution context
     * @param expanded Accumulator for expanded output
     */
    ExpansionContext(
        final Map<String, String> env,
        final BuiltinContext ctx,
        final StringBuilder expanded
    ) {
        this.env = env;
        this.ctx = ctx;
        this.expanded = expanded;
        this.dquote = false;
        this.split = false;
        this.kept = false;
        this.mark = false;
        this.inner = false;
    }

    /**
     * Returns the environment bindings.
     *
     * @return Environment variable bindings
     */
    Map<String, String> env() {
        return this.env;
    }

    /**
     * Returns the builtin execution context.
     *
     * @return Builtin execution context
     */
    BuiltinContext ctx() {
        return this.ctx;
    }

    /**
     * Returns the output accumulator.
     *
     * @return Output accumulator
     */
    StringBuilder expanded() {
        return this.expanded;
    }

    /**
     * Tells whether the current expansion sits inside double quotes.
     *
     * @return True inside double quotes
     */
    boolean quoted() {
        return this.dquote;
    }

    /**
     * Records whether the current expansion sits inside double quotes.
     *
     * @param inside True inside double quotes
     */
    void quote(final boolean inside) {
        this.dquote = inside;
    }

    /**
     * Tells whether the expanded text is field split and quote removed later.
     *
     * @return True when the caller splits the text into fields
     */
    boolean splitting() {
        return this.split;
    }

    /**
     * Records whether the expanded text is field split and quote removed later.
     *
     * @param fields True when the caller splits the text into fields
     */
    void splitting(final boolean fields) {
        this.split = fields;
    }

    /**
     * Tells whether the last braced expansion kept the quotes of its word,
     * so its text must not be escaped again.
     *
     * @return True when the text keeps its quotes
     */
    boolean verbatim() {
        return this.kept;
    }

    /**
     * Records whether the last braced expansion kept the quotes of its word.
     *
     * @param raw True when the text keeps its quotes
     */
    void verbatim(final boolean raw) {
        this.kept = raw;
    }

    /**
     * Tells whether unquoted literal characters of the word are marked, so
     * that field splitting never delimits at them.
     *
     * @return True when literal characters are marked
     */
    boolean marking() {
        return this.mark;
    }

    /**
     * Records whether unquoted literal characters of the word are marked.
     *
     * @param literal True when literal characters are marked
     */
    void marking(final boolean literal) {
        this.mark = literal;
    }

    /**
     * Records whether the word sits inside a double-quoted braced
     * expansion, so that a {@code $@} in it counts as double-quoted.
     *
     * @param within True inside a double-quoted braced expansion
     */
    void nested(final boolean within) {
        this.inner = within;
    }

    /**
     * Tells whether a {@code $@} expands to one field per positional
     * parameter, joined by {@link FieldSplitter#BREAK}.
     *
     * @return True when the text is split later and {@code $@} is quoted
     */
    boolean listing() {
        return this.split && (this.dquote || this.inner);
    }
}
