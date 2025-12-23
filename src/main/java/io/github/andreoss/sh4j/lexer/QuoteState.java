/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Tracks the state of single and double quotes.
 *
 * @since 1.0
 */
public final class QuoteState {

    /** True while inside a single quote. */
    private boolean squote;

    /** True while inside a double quote. */
    private boolean dquote;

    /**
     * Creates a new quote state with both quotes cleared.
     */
    public QuoteState() {
    }

    /**
     * Resets both quote flags to false.
     */
    public void reset() {
        this.squote = false;
        this.dquote = false;
    }

    /**
     * Checks whether no quotes are currently active.
     *
     * @return True when both flags are false
     */
    public boolean isEmpty() {
        return !this.squote && !this.dquote;
    }

    /**
     * Returns whether we're inside a single quote.
     *
     * @return True when inside a single quote
     */
    public boolean isSquote() {
        return this.squote;
    }

    /**
     * Sets whether we're inside a single quote.
     *
     * @param single True when inside a single quote
     */
    public void setSquote(final boolean single) {
        this.squote = single;
    }

    /**
     * Returns whether we're inside a double quote.
     *
     * @return True when inside a double quote
     */
    public boolean isDquote() {
        return this.dquote;
    }

    /**
     * Sets whether we're inside a double quote.
     *
     * @param doubleq True when inside a double quote
     */
    public void setDquote(final boolean doubleq) {
        this.dquote = doubleq;
    }
}
