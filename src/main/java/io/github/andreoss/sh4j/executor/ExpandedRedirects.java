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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Expands the targets of redirections exactly once (POSIX 2.7): each file
 * target is replaced by a quoted word that every later expansion, the
 * {@code noclobber} check and the open alike, yields unchanged; a
 * duplication target that is a number or {@code -} is kept bare, so that
 * it is read as a descriptor or as a close.
 *
 * @since 1.0
 */
final class ExpandedRedirects {

    /** Redirections whose target names a file. */
    private static final Set<RedirectionType> FILES = EnumSet.of(
        RedirectionType.INPUT, RedirectionType.OUTPUT, RedirectionType.CLOBBER,
        RedirectionType.APPEND, RedirectionType.READWRITE
    );

    /** Redirections whose target names a descriptor. */
    private static final Set<RedirectionType> DUPS = EnumSet.of(
        RedirectionType.DUP_INPUT, RedirectionType.DUP_OUTPUT
    );

    /** A duplication target read as a descriptor or as a close. */
    private static final Pattern BARE = Pattern.compile("-|[0-9]+");

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
     * Expands the targets of redirections.
     *
     * @param redirs The redirections in source order
     * @return The redirections with their targets expanded
     */
    List<Redirection> of(final List<Redirection> redirs) {
        return redirs.stream().map(this::expanded).collect(Collectors.toList());
    }

    /**
     * Copies a simple command with its redirection targets expanded.
     *
     * @param cmd The command
     * @return The copy whose redirections have their targets expanded
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
                redir.getFd(), redir.getType(), Quotes.word(this.target(redir))
            );
        } else if (ExpandedRedirects.DUPS.contains(redir.getType())) {
            result = new Redirection(
                redir.getFd(), redir.getType(),
                ExpandedRedirects.descriptor(this.target(redir))
            );
        }
        return result;
    }

    private String target(final Redirection redir) {
        return RedirectWord.expand(
            text -> this.shell.expandVariables(text, this.shell.environmentMap()),
            redir.getTarget()
        );
    }

    private static String descriptor(final String text) {
        String result = Quotes.word(text);
        if (ExpandedRedirects.BARE.matcher(text).matches()) {
            result = text;
        }
        return result;
    }
}
