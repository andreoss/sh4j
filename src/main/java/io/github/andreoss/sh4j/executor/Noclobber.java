/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * The {@code noclobber} option (POSIX 2.7.2): with {@code set -C}, the
 * {@code >} redirection fails on an existing regular file.
 *
 * @since 1.0
 */
final class Noclobber {

    /** The shell whose option is checked. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell whose option is checked
     */
    Noclobber(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Tells whether the option refuses one of the redirections, reporting
     * the refused target on stderr.
     *
     * @param redirs The redirections of a command
     * @return True when a redirection would overwrite a regular file
     */
    boolean refuses(final List<Redirection> redirs) {
        Optional<File> refused = Optional.empty();
        if (this.shell.shellFlags().indexOf('C') >= 0) {
            refused = redirs.stream()
                .filter(redir -> redir.getType() == RedirectionType.OUTPUT)
                .map(redir -> InputSwap.target(this.shell, redir))
                .filter(File::isFile)
                .findFirst();
        }
        refused.ifPresent(
            file -> this.shell.errStream().println(
                String.format("sh: %s: cannot overwrite existing file", file.getPath())
            )
        );
        return refused.isPresent();
    }
}
