/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Node;
import io.github.andreoss.sh4j.parser.Pipeline;
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
        boolean stopped = false;
        for (int idx = 0; !stopped && idx < pipelines.size(); idx = idx + 1) {
            if (idx == 0 || Pipelines.shouldRunNext(
                separators.get(idx - 1), this.shell.status()
            )) {
                stopped = this.step(pipelines.get(idx), separators.get(idx));
            }
        }
        return this.shell.status();
    }

    private boolean step(final Pipeline pipeline, final ListNode.Separator sep) {
        boolean stopped = false;
        if (sep == ListNode.Separator.AMP) {
            this.async(pipeline);
            this.shell.status(0);
        } else {
            stopped = this.foreground(pipeline, sep);
        }
        return stopped;
    }

    private boolean foreground(final Pipeline pipeline, final ListNode.Separator sep) {
        boolean stopped = false;
        try {
            final int status = this.pipeline(pipeline);
            this.shell.status(status);
            this.shell.history().add(Pipelines.text(pipeline));
            stopped = this.shell.shellFlags().indexOf('e') >= 0 && status != 0
                && sep != ListNode.Separator.AND_IF
                && sep != ListNode.Separator.OR_IF;
        } catch (final BreakSignal ex) {
            this.shell.requireLoop(ex, "break");
        } catch (final ContinueSignal ex) {
            this.shell.requireLoop(ex, "continue");
        }
        return stopped;
    }

    private void async(final Pipeline pipeline) {
        final String cmdtext = Pipelines.text(pipeline);
        final Executor parent = this.shell;
        final int jobnum = parent.jobManager().submit(
            () -> {
                final Executor bgexecutor = parent.subshell();
                bgexecutor.setOutputStream(parent.outStream());
                bgexecutor.setErrorStream(parent.errStream());
                return new Pipelines(bgexecutor).pipeline(pipeline);
            },
            cmdtext
        );
        this.shell.background((int) (JobManager.JOB_PID_BASE + jobnum));
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
