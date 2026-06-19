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
import java.util.ArrayList;
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
     * @param origin The {@code exec} command
     * @param cenv The command environment
     * @return Zero after binding descriptors
     */
    int run(final SimpleCommand origin, final Map<String, String> cenv) {
        final SimpleCommand cmd = new ExpandedRedirects(this.shell).of(origin);
        if (new Noclobber(this.shell).refuses(cmd.getRedirections())) {
            throw new ExitSignal(1);
        }
        final List<String> words = new ArrayList<>(0);
        for (final String arg : cmd.getArguments()) {
            words.addAll(this.shell.expandArguments(arg, cenv));
        }
        if (words.isEmpty()) {
            this.bind(cmd.getRedirections());
        } else {
            this.replace(words, cmd, cenv);
        }
        return 0;
    }

    private void bind(final List<Redirection> redirs) {
        boolean bound = true;
        for (final Redirection redir : redirs) {
            final boolean attached = this.applied(redir);
            bound = bound && attached;
        }
        if (!bound) {
            throw new ExitSignal(1);
        }
    }

    private boolean applied(final Redirection redir) {
        final List<Redirection> single = List.of(redir);
        final RedirectionHandler.StreamSwap swap = OutputSwap.apply(this.shell, single);
        final InputSwap input = new InputSwap(this.shell);
        InputSwap.discard(input.install(single));
        return this.attach(redir) && !swap.failed() && !input.failed();
    }

    private void replace(
        final List<String> words, final SimpleCommand cmd, final Map<String, String> cenv
    ) {
        final String name = words.get(0);
        final SimpleCommand external = new SimpleCommand(name);
        for (final String word : words.subList(1, words.size())) {
            external.addArgument(Quotes.word(word));
        }
        for (final Redirection redir : cmd.getRedirections()) {
            external.addRedirection(redir);
        }
        throw new ExitSignal(
            new Redirected(this.shell).run(
                external.getRedirections(),
                () -> new ExternalCommand(this.shell).run(external, name, cenv)
            )
        );
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
        } else if (type == RedirectionType.DUP_OUTPUT) {
            this.duplicate(redir);
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
            InputSwap.complain(this.shell, target);
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

    private void duplicate(final Redirection redir) {
        final Optional<PrintSink> sink = this.bound(redir.getTarget());
        if (redir.getFd() > 2 && sink.isPresent()) {
            this.shell.descriptors().outputs().put(redir.getFd(), sink.get());
        }
    }

    private Optional<PrintSink> bound(final String target) {
        final Optional<PrintSink> result;
        if (RedirectionHandler.STDOUT_FD.equals(target)) {
            result = Optional.of(this.shell.outStream());
        } else if (RedirectionHandler.STDERR_FD.equals(target)) {
            result = Optional.of(this.shell.errStream());
        } else {
            result = BuiltinRedirects.descriptor(this.shell.descriptors().outputs(), target);
        }
        return result;
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
