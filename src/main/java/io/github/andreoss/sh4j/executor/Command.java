/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.SimpleCommand;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a simple command: its assignments, then the named command if any.
 *
 * @since 1.0
 */
final class Command {

    /** The shell that runs the command. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell that runs the command
     */
    Command(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Runs the command.
     *
     * @param cmd The command
     * @return The exit status
     */
    int run(final SimpleCommand cmd) {
        final Map<String, String> cenv = new HashMap<>(this.shell.environmentMap());
        this.shell.substitution(-1);
        final int assign = this.assign(cmd.getAssignments(), cenv);
        final String name = cmd.getName();
        final int code;
        if (name == null || name.isEmpty()) {
            this.persist(cmd.getAssignments(), cenv);
            code = this.bare(assign);
        } else {
            code = new NamedCommand(this.shell).run(
                new CommandCall(cmd, name, cenv, false)
            );
        }
        return code;
    }

    private void persist(final List<String> assignments, final Map<String, String> tenv) {
        final Map<String, String> table = this.shell.environmentMap();
        table.putAll(tenv);
        for (final String assignment : assignments) {
            final int eqpos = assignment.indexOf('=');
            if (eqpos > 0) {
                final String vname = assignment.substring(0, eqpos);
                table.put(vname, tenv.get(vname));
            }
        }
    }

    private int bare(final int assign) {
        final int code;
        if (this.shell.substitution() >= 0) {
            code = this.shell.substitution();
        } else {
            code = assign;
        }
        return code;
    }

    private int assign(
        final List<String> assignments, final Map<String, String> tenv
    ) {
        for (final String assignment : assignments) {
            final int eqpos = assignment.indexOf('=');
            if (eqpos > 0) {
                final String vname = assignment.substring(0, eqpos);
                if (this.shell.isReadonly(vname)) {
                    this.shell.errStream().println(
                        String.format("sh: %s: readonly variable", vname)
                    );
                    throw new ExitSignal(1);
                }
                final String rvalue = AssignedTilde.expand(
                    assignment.substring(eqpos + 1), tenv
                );
                final String expanded = this.shell.expandVariables(rvalue, tenv);
                tenv.put(vname, WordExpander.removeQuotes(expanded));
            }
        }
        return 0;
    }
}
