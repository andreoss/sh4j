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
     * True when the here-document delimiter was quoted; per POSIX 2.7.4
     * the body shall NOT undergo variable / command / arithmetic expansion.
     */
    private final boolean quoted;

    /**
     * Creates a redirection with no here-document body.
     * @param desc   The file descriptor
     * @param type   The kind of redirection
     * @param target Target word (file path, fd, ...)
     */
    public Redirection(final int desc, final RedirectionType type, final String target) {
        this(desc, type, target, null, false);
    }

    /**
     * Creates a redirection, including here-doc body and quoted-delimiter
     * flag.
     * @param desc    The file descriptor
     * @param type    The kind of redirection
     * @param target  Target word (file path, fd, ...)
     * @param heredoc The here-document body, or null
     * @param quoted  True when the heredoc delimiter was quoted (no expansion)
     */
    public Redirection(
        final int desc,
        final RedirectionType type,
        final String target,
        final String heredoc,
        final boolean quoted
    ) {
        this.desc = desc;
        this.type = type;
        this.target = target;
        this.heredoc = heredoc;
        this.quoted = quoted;
    }

    /**
     * Returns the target file descriptor.
     * @return The file descriptor this redirection targets
     */
    public int getFd() {
        return this.desc;
    }

    /**
     * Returns the redirection kind.
     * @return The redirection kind
     */
    public RedirectionType getType() {
        return this.type;
    }

    /**
     * Returns the target word.
     * @return The target word
     */
    public String getTarget() {
        return this.target;
    }

    /**
     * Returns the inline here-document body.
     * @return Inline here-document body, or null
     */
    public String getHereDocContent() {
        return this.heredoc;
    }

    /**
     * Checks whether the here-document delimiter was quoted.
     * @return True when the heredoc delimiter was quoted (POSIX 2.7.4: skip
     *         variable / command / arithmetic expansion of the body)
     */
    public boolean isHereDocQuoted() {
        return this.quoted;
    }
}
