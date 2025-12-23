/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.parser;

/**
 * AST element describing one I/O redirection attached to a
 * {@link SimpleCommand}: file descriptor, kind of operation, target word,
 * and optional here-document body.
 *
 * @since 1.0
 */
public final class Redirection {

    /** File descriptor the redirection acts on. */
    private final int desc;

    /** Kind of redirection. */
    private final RedirectionType type;

    /** Target word plus the optional here-document payload. */
    private final HereDoc doc;

    /**
     * Creates a redirection with no here-document body.
     *
     * @param desc The file descriptor
     * @param type The kind of redirection
     * @param target Target word (file path, fd, ...)
     */
    public Redirection(final int desc, final RedirectionType type, final String target) {
        this(desc, type, new HereDoc(target, null, false));
    }

    /**
     * Creates a redirection, including its here-document payload.
     *
     * @param desc The file descriptor
     * @param type The kind of redirection
     * @param doc Target word plus the optional here-document body
     */
    public Redirection(final int desc, final RedirectionType type, final HereDoc doc) {
        this.desc = desc;
        this.type = type;
        this.doc = doc;
    }

    /**
     * Returns the target file descriptor.
     *
     * @return The file descriptor this redirection targets
     */
    public int getFd() {
        return this.desc;
    }

    /**
     * Returns the redirection kind.
     *
     * @return The redirection kind
     */
    public RedirectionType getType() {
        return this.type;
    }

    /**
     * Returns the target word.
     *
     * @return The target word
     */
    public String getTarget() {
        return this.doc.target();
    }

    /**
     * Returns the inline here-document body.
     *
     * @return Inline here-document body, or null
     */
    public String getHereDocContent() {
        return this.doc.body();
    }

    /**
     * Checks whether the here-document delimiter was quoted.
     *
     * @return True when the delimiter was quoted, so the body is not expanded
     */
    public boolean isHereDocQuoted() {
        return this.doc.quoted();
    }

    /**
     * Target word plus the optional here-document payload.
     *
     * @param target Target word (file path, fd number, or null)
     * @param body Here-document body, or null
     * @param quoted True when the here-document delimiter was quoted
     * @since 1.0
     */
    public record HereDoc(String target, String body, boolean quoted) {
    }
}
