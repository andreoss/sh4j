/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.parser.Redirection;
import java.util.List;

/**
 * Swaps the live stdout and stderr of a shell for the output redirections
 * of a built-in.
 *
 * @since 1.0
 */
final class OutputSwap {

    @GeneratedDefensive
    private OutputSwap() {
    }

    /**
     * Applies the output redirections to the live streams of the shell.
     *
     * @param shell The shell whose streams are swapped
     * @param redirs The redirections of the command
     * @return The swap that {@link #restore(Executor, RedirectionHandler.StreamSwap)}
     *  undoes
     */
    static RedirectionHandler.StreamSwap apply(
        final Executor shell, final List<Redirection> redirs
    ) {
        final RedirectionHandler.StreamSwap swap =
            RedirectionHandler.redirectOutputForBuiltin(
                redirs,
                new RedirectionHandler.Streams(shell.outStream(), shell.errStream()),
                new RedirectionHandler.RedirectContext(
                    text -> shell.expandVariables(text, shell.environmentMap()),
                    shell.currentDirectory(), shell.descriptors().outputs()
                )
            );
        shell.setOutputStream(swap.out());
        shell.setErrorStream(swap.err());
        return swap;
    }

    /**
     * Puts back the streams replaced by
     * {@link #apply(Executor, List)}.
     *
     * @param shell The shell whose streams were swapped
     * @param swap The swap returned by {@link #apply(Executor, List)}
     */
    static void restore(final Executor shell, final RedirectionHandler.StreamSwap swap) {
        if (swap.savedout() != null) {
            shell.setOutputStream(swap.savedout());
        }
        if (swap.savederr() != null) {
            shell.setErrorStream(swap.savederr());
        }
    }
}
