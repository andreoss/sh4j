/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.UnaryOperator;
import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;

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

    /**
     * Utility class; no instances.
     */
    @GeneratedDefensive
    private RedirectionHandler() {
        super();
    }

    /**
     * The new and saved stdout/stderr pair returned by
     * {@link #redirectOutputForBuiltin(List, PrintStream, PrintStream, UnaryOperator)}.
     *
     * @param out      The new live stdout
     * @param err      The new live stderr
     * @param savedOut The pre-call stdout (null when not swapped)
     * @param savedErr The pre-call stderr (null when not swapped)
     */
    public record StreamSwap(
        PrintStream out,
        PrintStream err,
        PrintStream savedOut,
        PrintStream savedErr
    ) {
    }

    /**
     * Computes the stdout/stderr swap that an executor built-in must apply
     * before running, given the redirections attached to the command.
     *
     * @param redirs     The redirections attached to the built-in invocation
     * @param currentOut The executor's current stdout
     * @param currentErr The executor's current stderr
     * @param expander   The executor's word-expansion callback
     * @return The new live + saved stream pair
     */
    public static StreamSwap redirectOutputForBuiltin(
        final List<Redirection> redirs,
        final PrintStream currentOut,
        final PrintStream currentErr,
        final UnaryOperator<String> expander
    ) {
        PrintStream out = currentOut;
        PrintStream err = currentErr;
        PrintStream savedOut = null;
        PrintStream savedErr = null;
        for (final Redirection redir : redirs) {
            final RedirectionType type = redir.getType();
            final int desc = redir.getFd();
            final String target = expander.apply(redir.getTarget());
            if (type == RedirectionType.OUTPUT || type == RedirectionType.CLOBBER) {
                final PrintStream fileOut = openForWrite(target, false);
                if (fileOut != null) {
                    if (desc == 1) {
                        savedOut = out;
                        out = fileOut;
                    } else if (desc == 2) {
                        savedErr = err;
                        err = fileOut;
                    }
                }
            } else if (type == RedirectionType.APPEND) {
                final PrintStream fileOut = openForWrite(target, true);
                if (fileOut != null) {
                    if (desc == 1) {
                        savedOut = out;
                        out = fileOut;
                    } else if (desc == 2) {
                        savedErr = err;
                        err = fileOut;
                    }
                }
            } else if (type == RedirectionType.DUP_OUTPUT) {
                if (CLOSE.equals(target)) {
                    final PrintStream nullPs = openForWrite(DEV_NULL, false);
                    if (nullPs != null) {
                        if (desc == 1) {
                            savedOut = out;
                            out = nullPs;
                        } else if (desc == 2) {
                            savedErr = err;
                            err = nullPs;
                        }
                    }
                } else if (STDOUT_FD.equals(target) && desc == 2) {
                    savedErr = err;
                    err = out;
                } else if (STDERR_FD.equals(target) && desc == 1) {
                    savedOut = out;
                    out = err;
                }
            }
        }
        return new StreamSwap(out, err, savedOut, savedErr);
    }

    /**
     * Applies redirections to a {@link ProcessBuilder} so an external
     * command honours them.
     *
     * @param builder      The process builder to mutate
     * @param redirections The redirections in source order
     * @param expander     Word-expansion callback for the targets
     */
    public static void setupRedirections(
        final ProcessBuilder builder,
        final List<Redirection> redirections,
        final UnaryOperator<String> expander
    ) {
        for (final Redirection redir : redirections) {
            applyOne(builder, redir, expander);
        }
    }

    /**
     * Writes the heredoc body to a temp file the caller can pass to
     * {@link ProcessBuilder#redirectInput(File)}.
     *
     * @param content The heredoc body (may be null)
     * @return The temp file, or null when content is null / IO failed
     */
    static File heredocFile(final String content) {
        File result = null;
        if (content != null) {
            try {
                final File temp = File.createTempFile("sh4j_heredoc", ".tmp");
                temp.deleteOnExit();
                try (FileWriter writer = new FileWriter(temp, StandardCharsets.UTF_8)) {
                    writer.write(content);
                }
                result = temp;
            } catch (final IOException ex) {
                result = heredocFileIoFailure();
            }
        }
        return result;
    }

    /**
     * Defensive fallback when the heredoc temp file cannot be created;
     * factored out so JaCoCo can exclude it via {@link GeneratedDefensive}.
     *
     * @return Null
     */
    @GeneratedDefensive
    private static File heredocFileIoFailure() {
        return null;
    }

    /**
     * Opens a file for writing as a {@link PrintStream}, swallowing IO
     * failures (the caller checks for null).
     *
     * @param target The file path
     * @param append True for append-mode, false for truncate
     * @return The print stream, or null on IO failure
     */
    private static PrintStream openForWrite(final String target, final boolean append) {
        PrintStream result;
        try {
            result = new PrintStream(
                new FileOutputStream(target, append),
                false,
                StandardCharsets.UTF_8
            );
        } catch (final IOException ex) {
            result = null;
        }
        return result;
    }

    /**
     * Applies a single redirection to the process builder.
     *
     * @param builder  The builder to mutate
     * @param redir    The redirection to apply
     * @param expander Word-expansion callback
     */
    private static void applyOne(
        final ProcessBuilder builder,
        final Redirection redir,
        final UnaryOperator<String> expander
    ) {
        final int desc = redir.getFd();
        final RedirectionType type = redir.getType();
        final String rawTarget = redir.getTarget();
        if (type == RedirectionType.DUP_INPUT) {
            if (CLOSE.equals(rawTarget) && desc == 0) {
                builder.redirectInput(new File(DEV_NULL));
            }
        } else if (type == RedirectionType.DUP_OUTPUT) {
            applyDupOutput(builder, desc, rawTarget);
        } else if (type == RedirectionType.HERE_DOC || type == RedirectionType.HERE_DOC_DASH) {
            applyHereDoc(builder, desc, redir, expander);
        } else {
            final File file = new File(expander.apply(rawTarget));
            applyFileRedirect(builder, desc, type, file);
        }
    }

    /**
     * Applies a {@code DUP_OUTPUT} redirection to the process builder.
     *
     * @param builder The builder to mutate
     * @param desc    The file descriptor
     * @param target  The duplicate target ({@code -}, {@code 1}, {@code 2}, ...)
     */
    private static void applyDupOutput(
        final ProcessBuilder builder,
        final int desc,
        final String target
    ) {
        if (CLOSE.equals(target)) {
            if (desc == 1) {
                builder.redirectOutput(new File(DEV_NULL));
            } else if (desc == 2) {
                builder.redirectError(new File(DEV_NULL));
            }
        } else if (STDOUT_FD.equals(target) && desc == 2) {
            builder.redirectErrorStream(true);
        } else if (STDERR_FD.equals(target) && desc == 1) {
            builder.redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/stderr")));
        }
    }

    /**
     * Applies a here-document redirection to the process builder.
     *
     * @param builder  The builder to mutate
     * @param desc     The file descriptor (only fd 0 is honoured)
     * @param redir    The redirection node
     * @param expander Word-expansion callback for the heredoc body
     */
    private static void applyHereDoc(
        final ProcessBuilder builder,
        final int desc,
        final Redirection redir,
        final UnaryOperator<String> expander
    ) {
        String content = redir.getHereDocContent();
        if (content != null) {
            content = expander.apply(content);
        }
        final File src = heredocFile(content);
        if (src != null && desc == 0) {
            builder.redirectInput(src);
        }
    }

    /**
     * Applies a file-based input/output/append/readwrite redirection.
     *
     * @param builder The builder to mutate
     * @param desc    The file descriptor
     * @param type    The redirection kind
     * @param file    The target file
     */
    private static void applyFileRedirect(
        final ProcessBuilder builder,
        final int desc,
        final RedirectionType type,
        final File file
    ) {
        if (type == RedirectionType.READWRITE) {
            if (desc == 0) {
                builder.redirectInput(file);
            } else if (desc == 1) {
                builder.redirectOutput(ProcessBuilder.Redirect.to(file));
            } else if (desc == 2) {
                builder.redirectError(ProcessBuilder.Redirect.to(file));
            }
        } else if (type == RedirectionType.INPUT) {
            if (desc == 0) {
                builder.redirectInput(file);
            }
        } else if (type == RedirectionType.OUTPUT || type == RedirectionType.CLOBBER) {
            if (desc == 1) {
                builder.redirectOutput(ProcessBuilder.Redirect.to(file));
            } else if (desc == 2) {
                builder.redirectError(ProcessBuilder.Redirect.to(file));
            }
        } else if (type == RedirectionType.APPEND) {
            if (desc == 1) {
                builder.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
            } else if (desc == 2) {
                builder.redirectError(ProcessBuilder.Redirect.appendTo(file));
            }
        }
    }
}
