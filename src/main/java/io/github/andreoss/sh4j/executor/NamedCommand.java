/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.FunctionNode;
import io.github.andreoss.sh4j.parser.Parser;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Runs a named command: an alias, {@code exec}, a function, a built-in, or
 * an external command, in that order of lookup.
 *
 * @since 1.0
 */
final class NamedCommand {

    /**
     * POSIX 2.14 special built-in utilities - these take priority over user-
     * defined functions in command search (POSIX 2.9.1.1).
     */
    private static final Set<String> SPECIAL = Set.of(
        ":", ".", "break", "continue", "eval", "exec", "exit", "export",
        "readonly", "return", "set", "shift", "times", "trap", "unset"
    );

    /** The shell that runs the command. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell that runs the command
     */
    NamedCommand(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Runs the command.
     *
     * @param call The call
     * @return The exit status
     */
    int run(final CommandCall call) {
        final Optional<String> alias = this.alias(call);
        final int code;
        if (alias.isPresent()) {
            code = this.expand(alias.get(), call.cmd());
        } else if ("exec".equals(call.name())) {
            code = new ExecCommand(this.shell).run(call.cmd(), call.env());
        } else {
            code = this.redirected(call);
        }
        return code;
    }

    /**
     * Runs a built-in or an external command, bypassing aliases and
     * functions.
     *
     * @param name The command name
     * @param args The command arguments
     * @param env The command environment (copied)
     * @return The exit status
     */
    int bypass(
        final String name, final List<String> args, final Map<String, String> env
    ) {
        final SimpleCommand command = new SimpleCommand(name);
        for (final String arg : args) {
            command.addArgument(arg);
        }
        return this.run(new CommandCall(command, name, new HashMap<>(env), true));
    }

    private Optional<String> alias(final CommandCall call) {
        Optional<String> alias = Optional.empty();
        if (!call.bypass()) {
            alias = Optional.ofNullable(this.shell.snapshot().table().get(call.name()));
        }
        return alias;
    }

    private int expand(final String alias, final SimpleCommand cmd) {
        final StringBuilder line = new StringBuilder(alias);
        for (final String arg : cmd.getArguments()) {
            line.append(' ').append(arg);
        }
        return this.shell.execute(
            new Parser(new Lexer(new CharReader(line.toString()))).parse()
        );
    }

    private int redirected(final CommandCall call) {
        final RedirectionHandler.StreamSwap swap =
            OutputSwap.apply(this.shell, call.cmd().getRedirections());
        final InputSwap input = new InputSwap(this.shell);
        final Optional<InputStream> kept = input.install(call.cmd().getRedirections());
        final int code;
        try {
            code = this.dispatch(call, swap.failed());
        } finally {
            input.restore(kept);
            OutputSwap.restore(this.shell, swap);
        }
        return code;
    }

    private int dispatch(final CommandCall call, final boolean failed) {
        final Map<String, FunctionNode> functions = this.shell.functionsMap();
        final int code;
        if (failed) {
            code = 1;
        } else if (!call.bypass() && functions.containsKey(call.name())
            && !NamedCommand.SPECIAL.contains(call.name())) {
            this.shell.environmentMap().putAll(call.env());
            code = this.invoke(functions.get(call.name()), call);
        } else {
            code = this.builtin(call);
        }
        return code;
    }

    private int builtin(final CommandCall call) {
        final SimpleCommand cmd = call.cmd();
        if (NamedCommand.SPECIAL.contains(call.name())) {
            this.apply(cmd.getAssignments(), call.env());
        }
        final Integer result = this.shell.builtin(
            call.name(),
            new Invocation(cmd.getArguments(), call.env(), cmd.getAssignments())
        );
        final int code;
        if (result == null) {
            code = new ExternalCommand(this.shell).run(cmd, call.name(), call.env());
        } else {
            code = result;
        }
        return code;
    }

    private int invoke(final FunctionNode func, final CommandCall call) {
        final List<String> params = this.shell.positionalParamsList();
        final List<String> keptargs = new ArrayList<>(params);
        final List<String> fargs = new ArrayList<>(0);
        for (final String arg : call.cmd().getArguments()) {
            fargs.addAll(this.shell.expandArguments(arg, call.env()));
        }
        this.shell.setPositionalParams(fargs);
        int code;
        try {
            code = this.shell.executeNode(func.getBody());
        } catch (final ReturnSignal ex) {
            code = ex.getCode();
        } finally {
            params.clear();
            params.addAll(keptargs);
        }
        return code;
    }

    private void apply(
        final List<String> assignments, final Map<String, String> source
    ) {
        for (final String assignment : assignments) {
            final int equals = assignment.indexOf('=');
            if (equals > 0) {
                final String name = assignment.substring(0, equals);
                final String value = source.get(name);
                if (value != null) {
                    this.shell.environmentMap().put(name, value);
                }
            }
        }
    }
}
