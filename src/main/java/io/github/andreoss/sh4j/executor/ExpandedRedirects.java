/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Expands the file targets of redirections exactly once (POSIX 2.7): each
 * target is replaced by a quoted word that every later expansion, the
 * {@code noclobber} check and the open alike, yields unchanged.
 *
 * @since 1.0
 */
final class ExpandedRedirects {

    /** Redirections whose target names a file. */
    private static final Set<RedirectionType> FILES = EnumSet.of(
        RedirectionType.INPUT, RedirectionType.OUTPUT, RedirectionType.CLOBBER,
        RedirectionType.APPEND, RedirectionType.READWRITE
    );

    /** The shell that expands the targets. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell that expands the targets
     */
    ExpandedRedirects(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Expands the file targets of redirections.
     *
     * @param redirs The redirections in source order
     * @return The redirections with their file targets expanded and quoted
     */
    List<Redirection> of(final List<Redirection> redirs) {
        return redirs.stream().map(this::expanded).collect(Collectors.toList());
    }

    /**
     * Copies a simple command with its file targets expanded.
     *
     * @param cmd The command
     * @return The copy whose redirections have their file targets expanded
     */
    SimpleCommand of(final SimpleCommand cmd) {
        final SimpleCommand copy = new SimpleCommand(cmd.getName());
        copy.setLineNumber(cmd.getLineNumber());
        cmd.getArguments().forEach(copy::addArgument);
        cmd.getAssignments().forEach(copy::addAssignment);
        this.of(cmd.getRedirections()).forEach(copy::addRedirection);
        return copy;
    }

    private Redirection expanded(final Redirection redir) {
        Redirection result = redir;
        if (ExpandedRedirects.FILES.contains(redir.getType())) {
            result = new Redirection(
                redir.getFd(),
                redir.getType(),
                Quotes.word(
                    RedirectWord.expand(
                        text -> this.shell.expandVariables(text, this.shell.environmentMap()),
                        redir.getTarget()
                    )
                )
            );
        }
        return result;
    }
}
