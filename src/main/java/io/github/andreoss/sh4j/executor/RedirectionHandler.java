/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.parser.Redirection;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Stateless helpers that translate {@link Redirection} AST nodes into the
 * concrete I/O wiring the executor needs: stream swaps for built-ins and
 * {@link ProcessBuilder} redirects for external commands.
 *
 * @since 1.0
 */
public final class RedirectionHandler {

    /** Sentinel target value indicating {@code &-} (close fd). */
    static final String CLOSE = "-";

    /** Sentinel target value indicating fd 1. */
    static final String STDOUT_FD = "1";

    /** Sentinel target value indicating fd 2. */
    static final String STDERR_FD = "2";

    @GeneratedDefensive
    private RedirectionHandler() {
    }

    /**
     * Computes the stdout/stderr swap that an executor built-in must apply
     * before running, given the redirections attached to the command.
     *
     * @param redirs The redirections attached to the built-in invocation
     * @param streams The executor's current stdout and stderr
     * @param expander The executor's word-expansion callback
     * @return The new live and saved stream pair
     */
    static StreamSwap redirectOutputForBuiltin(
        final List<Redirection> redirs, final Streams streams,
        final UnaryOperator<String> expander
    ) {
        return RedirectionHandler.redirectOutputForBuiltin(
            redirs, streams,
            new RedirectionHandler.RedirectContext(
                expander, null, Collections.emptyMap()
            )
        );
    }

    /**
     * Same as the three-argument form, but resolves relative redirection
     * targets against {@code base} and accepts descriptors bound by a
     * previous redirection-only {@code exec}.
     *
     * @param redirs The redirections attached to the invocation
     * @param streams The executor's current stdout and stderr
     * @param context Expansion, base directory, and bound descriptors
     * @return The new live and saved stream pair
     */
    static StreamSwap redirectOutputForBuiltin(
        final List<Redirection> redirs, final Streams streams,
        final RedirectionHandler.RedirectContext context
    ) {
        return BuiltinRedirects.swap(redirs, streams, context);
    }

    /**
     * Applies redirections to a {@link ProcessBuilder} so an external
     * command honours them.
     *
     * @param builder The process builder to mutate
     * @param redirections The redirections in source order
     * @param expander Word-expansion callback for the targets
     */
    static void setupRedirections(
        final ProcessBuilder builder, final List<Redirection> redirections,
        final UnaryOperator<String> expander
    ) {
        RedirectionHandler.setupRedirections(
            builder, redirections,
            new RedirectionHandler.RedirectContext(
                expander, null, Collections.emptyMap()
            )
        );
    }

    /**
     * Same as the three-argument form, resolving relative targets against
     * {@code base}.
     *
     * @param builder The process builder to mutate
     * @param redirections The redirections in source order
     * @param context Expansion, base directory, and bound descriptors
     */
    static void setupRedirections(
        final ProcessBuilder builder, final List<Redirection> redirections,
        final RedirectionHandler.RedirectContext context
    ) {
        ProcessRedirects.apply(builder, redirections, context);
    }

    /**
     * Writes the heredoc body to a temp file the caller can pass to
     * {@link ProcessBuilder#redirectInput(File)}.
     *
     * @param content The heredoc body (may be null)
     * @return The temp file, or null when content is null or IO failed
     */
    static File heredocFile(final String content) {
        final File result;
        if (content != null) {
            result = RedirectionHandler.createHeredocFile(content);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Resolves a redirection target against a base directory.
     *
     * @param target The expanded target path
     * @param base Directory for relative targets (null = process cwd)
     * @return The file to open
     */
    static File resolve(final String target, final File base) {
        final File file = new File(target);
        final File result;
        if (base == null || file.isAbsolute()) {
            result = file;
        } else {
            result = new File(base, target);
        }
        return result;
    }

    private static File createHeredocFile(final String content) {
        File result;
        try {
            final File temp = File.createTempFile("sh4j_heredoc", ".tmp");
            temp.deleteOnExit();
            try (
                Writer writer = Files.newBufferedWriter(
                    temp.toPath(),
                    StandardCharsets.UTF_8
                )
            ) {
                writer.write(content);
            }
            result = temp;
        } catch (final IOException ex) {
            result = RedirectionHandler.heredocFileIoFailure();
        }
        return result;
    }

    @GeneratedDefensive
    private static File heredocFileIoFailure() {
        return null;
    }

    /**
     * The executor's current stdout and stderr.
     *
     * @param out Current stdout
     * @param err Current stderr
     * @since 1.0
     */
    public record Streams(PrintSink out, PrintSink err) {
    }

    /**
     * Redirection configuration shared by the built-in and process-builder
     * paths.
     *
     * @param expander Word-expansion callback for the targets
     * @param base Directory for relative targets (null = process cwd)
     * @param descriptors Output descriptors bound by previous redirections
     * @since 1.0
     */
    public record RedirectContext(
        UnaryOperator<String> expander, File base,
        Map<Integer, PrintSink> descriptors
    ) {
    }

    /**
     * The new and saved stdout/stderr pair returned by the built-in path.
     *
     * @param out The new live stdout
     * @param err The new live stderr
     * @param savedout The pre-call stdout (null when not swapped)
     * @param savederr The pre-call stderr (null when not swapped)
     * @param failed True when a redirection target could not be opened
     * @since 1.0
     */
    public record StreamSwap(
        PrintSink out, PrintSink err, PrintSink savedout,
        PrintSink savederr, boolean failed
    ) {
    }
}
