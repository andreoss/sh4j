/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;

/**
 * One {@code jobs [-l|-p] [job...]} call (POSIX 2.14 jobs): lists the
 * background jobs with their state and command line; an unknown job is
 * reported and gives status 2.
 *
 * @since 1.0
 */
final class JobsBuiltin {

    /** The context. */
    private final BuiltinContext self;

    /**
     * Ctor.
     *
     * @param self The context
     */
    JobsBuiltin(final BuiltinContext self) {
        this.self = self;
    }

    /**
     * Lists the jobs.
     *
     * @param args Arguments after the command name
     * @return The exit code: 0, or 2 for an illegal option or unknown job
     */
    int run(final List<String> args) {
        final Options opts = new Options(this.self, "jobs", "lp");
        int code = 2;
        if (opts.read(args)) {
            code = this.list(args.subList(opts.first(), args.size()));
        }
        return code;
    }

    private int list(final List<String> ops) {
        final JobManager mgr = this.self.jobManager();
        int code = 0;
        if (ops.isEmpty()) {
            for (final Job job : mgr.listJobs()) {
                this.print(job);
            }
        } else {
            for (final String arg : ops) {
                final Job job = mgr.getJob(JobBuiltins.parseJobId(arg));
                if (job == null) {
                    this.self.errStream().println(
                        String.format("sh: jobs: %s: no such job", arg)
                    );
                    code = 2;
                } else {
                    this.print(job);
                }
            }
        }
        return code;
    }

    private void print(final Job job) {
        this.self.outStream().println(
            String.format(
                "[%d] %s %s",
                job.getId(),
                switch (job.getState()) {
                    case RUNNING -> "Running";
                    case DONE -> "Done";
                    default -> "Stopped";
                },
                job.getCommand()
            )
        );
    }
}
