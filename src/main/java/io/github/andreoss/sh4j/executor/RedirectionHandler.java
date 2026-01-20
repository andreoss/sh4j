/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
    private static final String CLOSE = "-";

    /** Sentinel target value indicating fd 1. */
    private static final String STDOUT_FD = "1";

    /** Sentinel target value indicating fd 2. */
    private static final String STDERR_FD = "2";

    /** Path that acts as a black-hole sink for the close-fd duplicate. */
    private static final String DEV_NULL = "/dev/null";

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
        final RedirectionState state =
            new RedirectionState(
                streams.out(), streams.err(), context
            );
        for (final Redirection redir : redirs) {
            RedirectionHandler.applyRedirect(state, redir);
        }
        return state.swap();
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
        for (final Redirection redir : redirections) {
            RedirectionHandler.applyOne(builder, redir, context);
        }
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

    private static File createHeredocFile(final String content) {
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
            return temp;
        } catch (final IOException ex) {
            return RedirectionHandler.heredocFileIoFailure();
        }
    }

    private static void applyRedirect(
        final RedirectionState state,
        final Redirection redir
    ) {
        final RedirectionType type = redir.getType();
        if (type == RedirectionType.OUTPUT
            || type == RedirectionType.CLOBBER
            || type == RedirectionType.APPEND) {
            RedirectionHandler.applyOutput(state, redir);
        } else if (type == RedirectionType.DUP_OUTPUT) {
            RedirectionHandler.applyDup(state, redir);
        }
    }

    private static void applyOutput(
        final RedirectionState state,
        final Redirection redir
    ) {
        RedirectionHandler.assign(
            state,
            RedirectionHandler.openForWrite(
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
            RedirectionHandler.applyClose(state, desc);
        } else if (RedirectionHandler.STDOUT_FD.equals(target) && desc == 2) {
            state.dupErrToOut();
        } else if (RedirectionHandler.STDERR_FD.equals(target) && desc == 1) {
            state.dupOutToErr();
        } else {
            RedirectionHandler.applyBound(state, target, desc);
        }
    }

    private static void applyClose(
        final RedirectionState state, final int desc
    ) {
        RedirectionHandler.assign(
            state,
            RedirectionHandler.openForWrite(
                RedirectionHandler.DEV_NULL, false, state.base()
            ),
            desc
        );
    }

    private static void applyBound(
        final RedirectionState state,
        final String target, final int desc
    ) {
        RedirectionHandler.assign(
            state,
            RedirectionHandler.descriptor(state.descriptors(), target),
            desc
        );
    }

    private static void assign(
        final RedirectionState state,
        final PrintStream stream, final int desc
    ) {
        if (stream == null) {
            state.fail();
        } else if (desc == 1) {
            state.replaceOut(stream);
        } else if (desc == 2) {
            state.replaceErr(stream);
        }
    }

    @GeneratedDefensive
    private static File heredocFileIoFailure() {
        return null;
    }

    private static File resolve(final String target, final File base) {
        final File file = new File(target);
        final File result;
        if (base == null || file.isAbsolute()) {
            result = file;
        } else {
            result = new File(base, target);
        }
        return result;
    }

    private static PrintStream descriptor(
        final Map<Integer, PrintStream> descriptors, final String target
    ) {
        PrintStream result = null;
        try {
            result = descriptors.get(Integer.parseInt(target.trim()));
        } catch (final NumberFormatException ex) {
            result = null;
        }
        return result;
    }

    private static PrintStream openForWrite(
        final String target, final boolean append, final File base
    ) {
        PrintStream result;
        try {
            final OutputStream sink = RedirectionHandler.sink(
                RedirectionHandler.resolve(target, base).toPath(), append
            );
            result = new PrintStream(
                sink, false, StandardCharsets.UTF_8
            );
        } catch (final IOException ex) {
            result = null;
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

    private static void applyOne(
        final ProcessBuilder builder, final Redirection redir,
        final RedirectionHandler.RedirectContext context
    ) {
        final RedirectionType type = redir.getType();
        if (type == RedirectionType.DUP_INPUT) {
            RedirectionHandler.applyDupInput(builder, redir);
        } else if (type == RedirectionType.DUP_OUTPUT) {
            RedirectionHandler.applyDupOutput(builder, redir);
        } else if (type == RedirectionType.HERE_DOC
            || type == RedirectionType.HERE_DOC_DASH) {
            RedirectionHandler.applyHereDoc(builder, redir, context.expander());
        } else {
            RedirectionHandler.applyFileRedirect(
                builder, redir,
                RedirectionHandler.resolve(
                    context.expander().apply(redir.getTarget()), context.base()
                )
            );
        }
    }

    private static void applyDupInput(
        final ProcessBuilder builder, final Redirection redir
    ) {
        if (RedirectionHandler.CLOSE.equals(redir.getTarget())
            && redir.getFd() == 0) {
            builder.redirectInput(new File(RedirectionHandler.DEV_NULL));
        }
    }

    private static void applyDupOutput(
        final ProcessBuilder builder, final Redirection redir
    ) {
        final int desc = redir.getFd();
        final String target = redir.getTarget();
        if (RedirectionHandler.CLOSE.equals(target)) {
            if (desc == 1) {
                builder.redirectOutput(new File(RedirectionHandler.DEV_NULL));
            } else if (desc == 2) {
                builder.redirectError(new File(RedirectionHandler.DEV_NULL));
            }
        } else if (RedirectionHandler.STDOUT_FD.equals(target) && desc == 2) {
            builder.redirectError(builder.redirectOutput());
        } else if (RedirectionHandler.STDERR_FD.equals(target) && desc == 1) {
            builder.redirectOutput(builder.redirectError());
        }
    }

    private static void applyHereDoc(
        final ProcessBuilder builder, final Redirection redir,
        final UnaryOperator<String> expander
    ) {
        String content = redir.getHereDocContent();
        if (content != null && !redir.isHereDocQuoted()) {
            content = expander.apply(content);
        }
        final File src = RedirectionHandler.heredocFile(content);
        if (src != null && redir.getFd() == 0) {
            builder.redirectInput(src);
        }
    }

    private static void applyFileRedirect(
        final ProcessBuilder builder, final Redirection redir, final File file
    ) {
        final RedirectionType type = redir.getType();
        if (type == RedirectionType.READWRITE) {
            RedirectionHandler.applyReadWrite(builder, redir, file);
        } else if (type == RedirectionType.INPUT) {
            RedirectionHandler.applyInput(builder, redir, file);
        } else if (type == RedirectionType.OUTPUT
            || type == RedirectionType.CLOBBER) {
            RedirectionHandler.applyTruncate(builder, redir, file);
        } else if (type == RedirectionType.APPEND) {
            RedirectionHandler.applyAppend(builder, redir, file);
        }
    }

    private static void applyInput(
        final ProcessBuilder builder, final Redirection redir, final File file
    ) {
        if (redir.getFd() == 0) {
            builder.redirectInput(file);
        }
    }

    private static void applyReadWrite(
        final ProcessBuilder builder, final Redirection redir, final File file
    ) {
        final int desc = redir.getFd();
        if (desc == 0) {
            builder.redirectInput(file);
        } else if (desc == 1) {
            builder.redirectOutput(ProcessBuilder.Redirect.to(file));
        } else if (desc == 2) {
            builder.redirectError(ProcessBuilder.Redirect.to(file));
        }
    }

    private static void applyTruncate(
        final ProcessBuilder builder, final Redirection redir, final File file
    ) {
        final int desc = redir.getFd();
        if (desc == 1) {
            builder.redirectOutput(ProcessBuilder.Redirect.to(file));
        } else if (desc == 2) {
            builder.redirectError(ProcessBuilder.Redirect.to(file));
        }
    }

    private static void applyAppend(
        final ProcessBuilder builder, final Redirection redir, final File file
    ) {
        final int desc = redir.getFd();
        if (desc == 1) {
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
        } else if (desc == 2) {
            builder.redirectError(ProcessBuilder.Redirect.appendTo(file));
        }
    }

    /**
     * The executor's current stdout and stderr.
     *
     * @param out Current stdout
     * @param err Current stderr
     * @since 1.0
     */
    public record Streams(PrintStream out, PrintStream err) {
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
        Map<Integer, PrintStream> descriptors
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
        PrintStream out, PrintStream err, PrintStream savedout,
        PrintStream savederr, boolean failed
    ) {
    }
}
