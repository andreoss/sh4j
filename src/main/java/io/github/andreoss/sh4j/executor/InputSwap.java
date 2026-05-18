/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

/**
 * Swaps the live stdin of a shell for the input redirections of a built-in.
 *
 * @since 1.0
 */
final class InputSwap {

    /** The shell whose stdin is swapped. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell whose stdin is swapped
     */
    InputSwap(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Resolves the target file of a redirection against the shell directory.
     *
     * @param shell The shell that expands the target
     * @param redir The redirection
     * @return The target file
     */
    static File target(final BuiltinContext shell, final Redirection redir) {
        final File raw = new File(
            shell.expandVariables(redir.getTarget(), shell.environmentMap())
        );
        final File result;
        if (raw.isAbsolute()) {
            result = raw;
        } else {
            result = new File(shell.currentDirectory(), raw.getPath());
        }
        return result;
    }

    /**
     * Closes a stdin left behind by a swap unless it is the process stdin.
     *
     * @param kept The stdin that was live before the swap
     */
    static void discard(final Optional<InputStream> kept) {
        kept.filter(stream -> !System.in.equals(stream)).ifPresent(Descriptors::close);
    }

    /**
     * Installs the stdin selected by the redirections.
     *
     * @param redirs The redirections of the command
     * @return The stdin that was live before, empty when nothing changed
     */
    Optional<InputStream> install(final List<Redirection> redirs) {
        Optional<InputStream> replacement = Optional.empty();
        for (final Redirection redir : redirs) {
            replacement = this.next(redir, replacement);
        }
        return replacement.map(this::swap);
    }

    /**
     * Puts back the stdin that was live before {@link #install(List)}.
     *
     * @param kept The stdin returned by {@link #install(List)}
     */
    void restore(final Optional<InputStream> kept) {
        kept.ifPresent(this::reinstate);
    }

    private InputStream swap(final InputStream replacement) {
        final InputStream previous = this.shell.inputStream();
        this.shell.setInputStream(replacement);
        return previous;
    }

    private void reinstate(final InputStream previous) {
        final Descriptors fds = this.shell.descriptors();
        if (!fds.borrowed()) {
            Descriptors.close(this.shell.inputStream());
        }
        fds.borrow(false);
        this.shell.setInputStream(previous);
    }

    private Optional<InputStream> next(
        final Redirection redir, final Optional<InputStream> current
    ) {
        final RedirectionType type = redir.getType();
        Optional<InputStream> result = current;
        if (type == RedirectionType.HERE_DOC || type == RedirectionType.HERE_DOC_DASH) {
            result = this.heredoc(redir, current);
        } else if (type == RedirectionType.DUP_INPUT) {
            result = this.borrowed(redir, current);
        } else if (type == RedirectionType.INPUT && redir.getFd() == 0) {
            result = InputSwap.open(InputSwap.target(this.shell, redir));
        }
        return result;
    }

    private Optional<InputStream> heredoc(
        final Redirection redir, final Optional<InputStream> current
    ) {
        final String content = redir.getHereDocContent();
        Optional<InputStream> result = current;
        if (content != null) {
            result = Optional.of(
                new ByteArrayInputStream(
                    this.body(redir, content).getBytes(StandardCharsets.UTF_8)
                )
            );
        }
        return result;
    }

    private String body(final Redirection redir, final String content) {
        final String body;
        if (redir.isHereDocQuoted()) {
            body = content;
        } else {
            body = new HereText(content).expanded(
                text -> this.shell.expandVariables(text, this.shell.environmentMap())
            );
        }
        return body;
    }

    private Optional<InputStream> borrowed(
        final Redirection redir, final Optional<InputStream> current
    ) {
        final Optional<InputStream> bound = Optional.ofNullable(
            this.shell.descriptors().inputs().get(
                InputSwap.descriptor(
                    this.shell.expandVariables(
                        redir.getTarget(), this.shell.environmentMap()
                    )
                )
            )
        );
        Optional<InputStream> result = current;
        if (bound.isPresent()) {
            result = bound;
            this.shell.descriptors().borrow(true);
        }
        return result;
    }

    private static Optional<InputStream> open(final File target) {
        Optional<InputStream> result;
        try {
            result = Optional.of(Files.newInputStream(target.toPath()));
        } catch (final IOException ex) {
            result = Optional.empty();
        }
        return result;
    }

    private static int descriptor(final String text) {
        int result;
        try {
            result = Integer.parseInt(text.trim());
        } catch (final NumberFormatException ex) {
            result = -1;
        }
        return result;
    }
}
