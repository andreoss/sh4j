/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.parser.Redirection;
import io.github.andreoss.sh4j.parser.RedirectionType;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The {@code exec} special built-in: binds descriptors when it has no
 * operands, or replaces the shell with an external command.
 *
 * @since 1.0
 */
final class ExecCommand {

    /** Redirections that bind an input descriptor. */
    private static final Set<RedirectionType> READS = EnumSet.of(
        RedirectionType.READWRITE, RedirectionType.INPUT
    );

    /** Redirections that bind an output descriptor. */
    private static final Set<RedirectionType> WRITES = EnumSet.of(
        RedirectionType.OUTPUT, RedirectionType.CLOBBER, RedirectionType.APPEND
    );

    /** Target that closes a duplicated descriptor. */
    private static final String CLOSE = "-";

    /** The shell that runs {@code exec}. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell that runs {@code exec}
     */
    ExecCommand(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Runs {@code exec}.
     *
     * @param cmd The {@code exec} command
     * @param cenv The command environment
     * @return Zero after binding descriptors
     */
    int run(final SimpleCommand cmd, final Map<String, String> cenv) {
        if (new Noclobber(this.shell).refuses(cmd.getRedirections())) {
            throw new ExitSignal(1);
        }
        if (cmd.getArguments().isEmpty()) {
            this.bind(cmd.getRedirections());
        } else {
            this.replace(cmd, cenv);
        }
        return 0;
    }

    private void bind(final List<Redirection> redirs) {
        final RedirectionHandler.StreamSwap swap = OutputSwap.apply(this.shell, redirs);
        InputSwap.discard(new InputSwap(this.shell).install(redirs));
        boolean bound = !swap.failed();
        for (final Redirection redir : redirs) {
            final boolean attached = this.attach(redir);
            bound = bound && attached;
        }
        if (!bound) {
            throw new ExitSignal(1);
        }
    }

    private void replace(final SimpleCommand cmd, final Map<String, String> cenv) {
        final List<String> args = cmd.getArguments();
        final String name = this.shell.expandVariables(args.get(0), cenv);
        final SimpleCommand external = new SimpleCommand(name);
        for (int idx = 1; idx < args.size(); idx = idx + 1) {
            external.addArgument(args.get(idx));
        }
        for (final Redirection redir : cmd.getRedirections()) {
            external.addRedirection(redir);
        }
        throw new ExitSignal(new ExternalCommand(this.shell).run(external, name, cenv));
    }

    private boolean attach(final Redirection redir) {
        boolean attached = true;
        if (redir.getFd() > 0) {
            attached = this.descriptor(redir);
        }
        return attached;
    }

    private boolean descriptor(final Redirection redir) {
        final RedirectionType type = redir.getType();
        boolean attached = true;
        if (ExecCommand.READS.contains(type)) {
            attached = this.read(redir);
        } else if (ExecCommand.WRITES.contains(type)) {
            attached = this.write(redir);
        } else if (type == RedirectionType.DUP_INPUT
            && ExecCommand.CLOSE.equals(redir.getTarget())) {
            attached = this.closeInput(redir.getFd());
        } else if (type == RedirectionType.DUP_OUTPUT
            && ExecCommand.CLOSE.equals(redir.getTarget())) {
            this.shell.descriptors().outputs().remove(redir.getFd());
        }
        return attached;
    }

    private boolean read(final Redirection redir) {
        final File target = InputSwap.target(this.shell, redir);
        boolean attached = true;
        try {
            if (redir.getType() == RedirectionType.READWRITE) {
                this.writer(redir.getFd(), target, true);
            }
            this.shell.descriptors().inputs().putAll(
                Map.of(redir.getFd(), Files.newInputStream(target.toPath()))
            );
        } catch (final IOException ex) {
            attached = false;
        }
        return attached;
    }

    private boolean write(final Redirection redir) {
        final File target = InputSwap.target(this.shell, redir);
        boolean attached = true;
        try {
            this.writer(
                redir.getFd(), target, redir.getType() == RedirectionType.APPEND
            );
        } catch (final IOException ex) {
            attached = false;
        }
        return attached;
    }

    private void writer(final int desc, final File target, final boolean append)
        throws IOException {
        this.shell.descriptors().outputs().put(
            desc,
            new PrintSink(ExecCommand.sink(target.toPath(), append), StandardCharsets.UTF_8)
        );
    }

    private boolean closeInput(final int desc) {
        return Optional.ofNullable(this.shell.descriptors().inputs().remove(desc))
            .map(Descriptors::close)
            .orElse(true);
    }

    private static OutputStream sink(final Path path, final boolean append)
        throws IOException {
        final OpenOption[] modes;
        if (append) {
            modes = new OpenOption[]{
                StandardOpenOption.CREATE, StandardOpenOption.APPEND,
            };
        } else {
            modes = new OpenOption[0];
        }
        return Files.newOutputStream(path, modes);
    }
}
