/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.lexer;

/**
 * Body of one here-document, filled once the line holding its operator
 * ends (POSIX 2.7.4).
 *
 * @since 1.0
 */
public final class HereBody {

    /** Body text, empty until read. */
    private String content;

    /**
     * Ctor.
     */
    HereBody() {
        this.content = "";
    }

    /**
     * Returns the body text.
     *
     * @return The body, empty when not read
     */
    public String text() {
        return this.content;
    }

    /**
     * Stores the body text.
     *
     * @param body The body
     */
    void fill(final String body) {
        this.content = body;
    }
}
