/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * AST element describing one I/O redirection attached to a
 * {@link SimpleCommand}: file descriptor, kind of operation, target word,
 * and optional here-document body.
 * @since 1.0
 */
public final class Redirection {

    /** File descriptor the redirection acts on. */
    private final int desc;
    /** Kind of redirection. */
    private final RedirectionType type;
    /** Target word (file path, fd number, or null). */
    private final String target;
    /** Inline here-document text when {@link #type} is HERE_DOC*; else null. */
    private final String heredoc;

    /**
     * Creates a redirection with no here-document body.
     * @param desc   The file descriptor
     * @param type   The kind of redirection
     * @param target Target word (file path, fd, ...)
     */
    public Redirection(final int desc, final RedirectionType type, final String target) {
        this(desc, type, target, null);
    }

    /**
     * Creates a redirection, including any inline here-document.
     * @param desc    The file descriptor
     * @param type    The kind of redirection
     * @param target  Target word (file path, fd, ...)
     * @param heredoc The here-document body, or null
     */
    public Redirection(
        final int desc,
        final RedirectionType type,
        final String target,
        final String heredoc
    ) {
        this.desc = desc;
        this.type = type;
        this.target = target;
        this.heredoc = heredoc;
    }

    /**
     * @return The file descriptor this redirection targets
     */
    public int getFd() {
        return this.desc;
    }

    /**
     * @return The redirection kind
     */
    public RedirectionType getType() {
        return this.type;
    }

    /**
     * @return The target word
     */
    public String getTarget() {
        return this.target;
    }

    /**
     * @return Inline here-document body, or null
     */
    public String getHereDocContent() {
        return this.heredoc;
    }
}
