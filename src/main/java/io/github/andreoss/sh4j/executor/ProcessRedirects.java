/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import java.io.File;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Applies redirections to a {@link ProcessBuilder} for an external command.
 *
 * @since 1.0
 */
final class ProcessRedirects {

    @GeneratedDefensive
    private ProcessRedirects() {
    }

    /**
     * Applies redirections in source order.
     *
     * @param builder The process builder to mutate
     * @param redirections The redirections in source order
     * @param context Expansion, base directory, and bound descriptors
     */
    static void apply(
        final ProcessBuilder builder, final List<Redirection> redirections,
        final RedirectionHandler.RedirectContext context
    ) {
        for (final Redirection redir : redirections) {
            ProcessRedirects.applyOne(builder, redir, context);
        }
    }

    private static void applyOne(
        final ProcessBuilder builder, final Redirection redir,
        final RedirectionHandler.RedirectContext context
    ) {
        final RedirectionType type = redir.getType();
        if (type == RedirectionType.DUP_INPUT) {
            ProcessRedirects.applyDupInput(builder, redir);
        } else if (type == RedirectionType.DUP_OUTPUT) {
            ProcessRedirects.applyDupOutput(builder, redir);
        } else if (type == RedirectionType.HERE_DOC
            || type == RedirectionType.HERE_DOC_DASH) {
            ProcessRedirects.applyHereDoc(builder, redir, context.expander());
        } else {
            ProcessRedirects.applyFileRedirect(
                builder, redir,
                RedirectionHandler.resolve(
                    RedirectWord.expand(context.expander(), redir.getTarget()),
                    context.base()
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
            content = new HereText(content).expanded(expander);
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
            ProcessRedirects.applyReadWrite(builder, redir, file);
        } else if (type == RedirectionType.INPUT) {
            ProcessRedirects.applyInput(builder, redir, file);
        } else if (type == RedirectionType.OUTPUT
            || type == RedirectionType.CLOBBER) {
            ProcessRedirects.applyTruncate(builder, redir, file);
        } else if (type == RedirectionType.APPEND) {
            ProcessRedirects.applyAppend(builder, redir, file);
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
}
