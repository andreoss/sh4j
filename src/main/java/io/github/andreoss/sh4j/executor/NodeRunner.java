/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.Grouping;
import io.github.andreoss.sh4j.parser.Node;
import io.github.andreoss.sh4j.parser.RedirectedNode;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import io.github.andreoss.sh4j.parser.Subshell;

/**
 * Runs one command node: a simple command, a subshell, a group, or a
 * compound command.
 *
 * @since 1.0
 */
final class NodeRunner {

    /** The shell that runs the node. */
    private final Executor shell;

    /** Runner of the compound commands of the shell. */
    private final Compound compound;

    /**
     * Ctor.
     *
     * @param shell The shell that runs the node
     * @param compound Runner of the compound commands of the shell
     */
    NodeRunner(final Executor shell, final Compound compound) {
        this.shell = shell;
        this.compound = compound;
    }

    /**
     * Runs the node.
     *
     * @param node The node
     * @return The exit status
     */
    int run(final Node node) {
        final int code;
        if (node instanceof SimpleCommand cmd) {
            code = this.simple(cmd);
        } else if (node instanceof Subshell sub) {
            code = this.subshell(sub);
        } else if (node instanceof Grouping group) {
            code = this.shell.execute(group.getList());
        } else if (node instanceof RedirectedNode red) {
            code = new Redirected(this.shell).run(
                red.getRedirections(), () -> this.run(red.getBody())
            );
        } else {
            code = this.compound.run(node);
        }
        return code;
    }

    private int simple(final SimpleCommand cmd) {
        this.shell.snapshot().refresh(cmd.getLineNumber(), this.shell.aliases());
        this.shell.environmentMap().put("LINENO", String.valueOf(cmd.getLineNumber()));
        if (this.shell.shellFlags().indexOf('x') >= 0) {
            this.shell.errStream().println(String.format("+ %s", cmd));
        }
        return new Command(this.shell).run(cmd);
    }

    private int subshell(final Subshell sub) {
        final Executor child = this.shell.subshell();
        int code;
        try {
            code = child.execute(sub.getList());
        } catch (final ExitSignal ex) {
            code = ex.getCode();
        }
        return code;
    }
}
