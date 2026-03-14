/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies output redirections of a built-in as a stdout/stderr swap.
 *
 * @since 1.0
 */
final class BuiltinRedirects {

    @GeneratedDefensive
    private BuiltinRedirects() {
    }

    /**
     * Computes the stream swap for the redirections of a built-in.
     *
     * @param redirs The redirections attached to the invocation
     * @param streams The executor's current stdout and stderr
     * @param context Expansion, base directory, and bound descriptors
     * @return The new live and saved stream pair
     */
    static RedirectionHandler.StreamSwap swap(
        final List<Redirection> redirs,
        final RedirectionHandler.Streams streams,
        final RedirectionHandler.RedirectContext context
    ) {
        final RedirectionState state =
            new RedirectionState(
                streams.out(), streams.err(), context
            );
        for (final Redirection redir : redirs) {
            BuiltinRedirects.applyRedirect(state, redir);
        }
        return state.swap();
    }

    private static void applyRedirect(
        final RedirectionState state,
        final Redirection redir
    ) {
        final RedirectionType type = redir.getType();
        if (type == RedirectionType.OUTPUT
            || type == RedirectionType.CLOBBER
            || type == RedirectionType.APPEND) {
            BuiltinRedirects.applyOutput(state, redir);
        } else if (type == RedirectionType.DUP_OUTPUT) {
            BuiltinRedirects.applyDup(state, redir);
        }
    }

    private static void applyOutput(
        final RedirectionState state,
        final Redirection redir
    ) {
        BuiltinRedirects.assign(
            state,
            BuiltinRedirects.openForWrite(
                state.expand(redir.getTarget()),
                redir.getType() == RedirectionType.APPEND,
                state.base()
            ),
            redir.getFd()
        );
    }

    private static void applyDup(
        final RedirectionState state,
        final Redirection redir
    ) {
        final String target = state.expand(redir.getTarget());
        final int desc = redir.getFd();
        if (RedirectionHandler.CLOSE.equals(target)) {
            BuiltinRedirects.applyClose(state, desc);
        } else if (RedirectionHandler.STDOUT_FD.equals(target) && desc == 2) {
            state.dupErrToOut();
        } else if (RedirectionHandler.STDERR_FD.equals(target) && desc == 1) {
            state.dupOutToErr();
        } else {
            BuiltinRedirects.applyBound(state, target, desc);
        }
    }

    private static void applyClose(
        final RedirectionState state, final int desc
    ) {
        BuiltinRedirects.assign(
            state,
            BuiltinRedirects.openForWrite(
                RedirectionHandler.DEV_NULL, false, state.base()
            ),
            desc
        );
    }

    private static void applyBound(
        final RedirectionState state,
        final String target, final int desc
    ) {
        BuiltinRedirects.assign(
            state,
            BuiltinRedirects.descriptor(state.descriptors(), target),
            desc
        );
    }

    private static void assign(
        final RedirectionState state,
        final Optional<PrintSink> stream, final int desc
    ) {
        if (stream.isEmpty()) {
            state.fail();
        } else if (desc == 1) {
            state.replaceOut(stream.get());
        } else if (desc == 2) {
            state.replaceErr(stream.get());
        }
    }

    private static Optional<PrintSink> descriptor(
        final Map<Integer, PrintSink> descriptors, final String target
    ) {
        Optional<PrintSink> result;
        try {
            result = Optional.ofNullable(
                descriptors.get(Integer.parseInt(target.trim()))
            );
        } catch (final NumberFormatException ex) {
            result = Optional.empty();
        }
        return result;
    }

    private static Optional<PrintSink> openForWrite(
        final String target, final boolean append, final File base
    ) {
        Optional<PrintSink> result;
        try {
            result = Optional.of(
                new PrintSink(
                    BuiltinRedirects.sink(
                        RedirectionHandler.resolve(target, base).toPath(),
                        append
                    ),
                    StandardCharsets.UTF_8
                )
            );
        } catch (final IOException ex) {
            result = Optional.empty();
        }
        return result;
    }

    private static OutputStream sink(final Path path, final boolean append)
        throws IOException {
        final OpenOption[] modes;
        if (append) {
            modes = new OpenOption[]{
                StandardOpenOption.CREATE, StandardOpenOption.APPEND,
            };
        } else {
            modes = new OpenOption[]{
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
            };
        }
        return Files.newOutputStream(path, modes);
    }
}
