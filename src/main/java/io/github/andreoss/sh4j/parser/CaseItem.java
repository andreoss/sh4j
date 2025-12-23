/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

import java.util.List;

/**
 * A single case-arm: one or more patterns plus a body to execute when
 * any pattern matches.
 *
 * @since 1.0
 */
public final class CaseItem {

    /** Alternative patterns for this arm. */
    private final List<String> patterns;

    /** Body to execute when any pattern matches. */
    private final ListNode body;

    /**
     * Creates a case-arm.
     *
     * @param patterns Alternative patterns for this arm
     * @param body     Body to execute on match
     */
    public CaseItem(final List<String> patterns, final ListNode body) {
        this.patterns = patterns;
        this.body = body;
    }

    /**
     * Returns this arm's alternative patterns.
     *
     * @return This arm's alternative patterns
     */
    public List<String> getPatterns() {
        return this.patterns;
    }

    /**
     * Returns this arm's body.
     *
     * @return This arm's body
     */
    public ListNode getBody() {
        return this.body;
    }
}
