/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Node;
import io.github.andreoss.sh4j.parser.Pipeline;
import io.github.andreoss.sh4j.parser.RedirectedNode;
import io.github.andreoss.sh4j.parser.SimpleCommand;
import io.github.andreoss.sh4j.parser.Subshell;
import java.util.List;

/**
 * Runs and-or lists of pipelines, in the foreground or as background jobs.
 *
 * @since 1.0
 */
final class Pipelines {

    /** The shell that runs the pipelines. */
    private final Executor shell;

    /**
     * Ctor.
     *
     * @param shell The shell that runs the pipelines
     */
    Pipelines(final Executor shell) {
        this.shell = shell;
    }

    /**
     * Runs a list of pipelines.
     *
     * @param list The list
     * @return The exit status of the last pipeline that ran, 0 when empty
     */
    int run(final ListNode list) {
        int code = 0;
        if (!list.getPipelines().isEmpty()) {
            code = this.sequence(list);
        }
        return code;
    }

    /**
     * Runs one pipeline in the foreground.
     *
     * @param pipeline The pipeline
     * @return The exit status, negated for a {@code !} pipeline
     */
    int pipeline(final Pipeline pipeline) {
        final List<Node> commands = pipeline.getCommands();
        int code = 0;
        if (!commands.isEmpty() && commands.get(0) != null) {
            code = Pipelines.negate(pipeline.isBang(), this.exit(commands));
        }
        return code;
    }

    private int sequence(final ListNode list) {
        final List<Pipeline> pipelines = list.getPipelines();
        final List<ListNode.Separator> separators = list.getSeparators();
        for (int idx = 0; idx < pipelines.size(); idx = idx + 1) {
            if (idx == 0 || Pipelines.shouldRunNext(
                separators.get(idx - 1), this.shell.status()
            )) {
                this.step(pipelines.get(idx), separators.get(idx));
            }
        }
        return this.shell.status();
    }

    private void step(final Pipeline pipeline, final ListNode.Separator sep) {
        final boolean noexec = this.shell.shellFlags().indexOf('n') >= 0;
        if (!noexec && sep == ListNode.Separator.AMP) {
            this.async(pipeline);
            this.shell.status(0);
        } else if (!noexec) {
            this.foreground(pipeline, sep);
        }
    }

    private void foreground(final Pipeline pipeline, final ListNode.Separator sep) {
        final boolean ignored = pipeline.isBang()
            || sep == ListNode.Separator.AND_IF
            || sep == ListNode.Separator.OR_IF;
        try {
            final int status = this.outcome(pipeline, ignored);
            this.shell.status(status);
            this.shell.history().add(Pipelines.text(pipeline));
            if (!ignored && Pipelines.simple(pipeline)) {
                this.shell.errexit().check(this.shell.shellFlags(), status);
            }
        } catch (final BreakSignal ex) {
            this.shell.requireLoop(ex, "break");
        } catch (final ContinueSignal ex) {
            this.shell.requireLoop(ex, "continue");
        }
    }

    private int outcome(final Pipeline pipeline, final boolean ignored) {
        final int status;
        if (ignored) {
            status = this.shell.errexit().ignoring(() -> this.pipeline(pipeline));
        } else {
            status = this.pipeline(pipeline);
        }
        return status;
    }

    private void async(final Pipeline pipeline) {
        final String cmdtext = Pipelines.text(pipeline);
        final Executor parent = this.shell;
        this.shell.background(
            Math.toIntExact(
                JobManager.JOB_PID_BASE + parent.jobManager().submit(
                    () -> {
                        final Executor bgexecutor = parent.subshell();
                        bgexecutor.setOutputStream(parent.outStream());
                        bgexecutor.setErrorStream(parent.errStream());
                        return bgexecutor.leave(new Pipelines(bgexecutor).pipeline(pipeline));
                    },
                    cmdtext
                )
            )
        );
    }

    private int exit(final List<Node> commands) {
        final int exit;
        if (commands.size() == 1) {
            exit = this.shell.executeNode(commands.get(0));
        } else {
            exit = new PipeChain(this.shell, commands).run();
        }
        return exit;
    }

    private static int negate(final boolean bang, final int exit) {
        int code = exit;
        if (bang && exit == 0) {
            code = 1;
        } else if (bang) {
            code = 0;
        }
        return code;
    }

    private static boolean simple(final Pipeline pipeline) {
        final List<Node> commands = pipeline.getCommands();
        return commands.size() != 1 || Pipelines.plain(commands.get(0));
    }

    private static boolean plain(final Node command) {
        Node first = command;
        if (first instanceof RedirectedNode red) {
            first = red.getBody();
        }
        return first instanceof SimpleCommand || first instanceof Subshell;
    }

    private static boolean shouldRunNext(
        final ListNode.Separator psep, final int pexit
    ) {
        final boolean run;
        if (psep == ListNode.Separator.AND_IF) {
            run = pexit == 0;
        } else if (psep == ListNode.Separator.OR_IF) {
            run = pexit != 0;
        } else {
            run = true;
        }
        return run;
    }

    private static String text(final Pipeline pipeline) {
        final StringBuilder line = new StringBuilder();
        for (final Node node : pipeline.getCommands()) {
            if (node == null) {
                continue;
            }
            if (line.length() > 0) {
                line.append(" | ");
            }
            line.append(node.toString());
        }
        return line.toString();
    }
}
