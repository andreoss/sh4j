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
        this.squote = false;
        this.dquote = false;
    }

    /**
     * Tells whether a single quote is open.
     *
     * @return True when inside a single quote
     */
    public boolean single() {
        return this.squote;
    }

    /**
     * Tells whether a double quote is open.
     *
     * @return True when inside a double quote
     */
    public boolean doubled() {
        return this.dquote;
    }

    /** Opens a single quote. */
    public void enterSingle() {
        this.squote = true;
    }

    /** Closes a single quote. */
    public void leaveSingle() {
        this.squote = false;
    }

    /** Opens a double quote. */
    public void enterDouble() {
        this.dquote = true;
    }

    /** Closes a double quote. */
    public void leaveDouble() {
        this.dquote = false;
    }
}
