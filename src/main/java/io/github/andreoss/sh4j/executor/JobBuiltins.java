/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Built-ins that list and move background jobs.
 *
 * @since 1.0
 */
final class JobBuiltins {

    @GeneratedDefensive
    private JobBuiltins() {
    }


    /**
     * Implementation of {@code jobs [%n...]}.
     * POSIX 2.14: without args, lists all background jobs with their
     * state and command line.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code: 0
     */
    static int doJobs(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        for (final Job job : JobBuiltins.selectJobs(self.jobManager(), args)) {
            final String state = switch (job.getState()) {
                case RUNNING -> "Running";
                case DONE -> "Done";
                default -> "Stopped";
            };
            self.outStream().println(
                String.format("[%d] %s %s", job.getId(), state, job.getCommand())
            );
        }
        return 0;
    }

    /**
     * Implementation of {@code bg [%n...]}.
     * POSIX 2.14: resumes suspended jobs in the background. Without args,
     * operates on the most recent job. Prints a confirmation line.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The exit code: 0 on success, 1 if no matching job
     */
    static int doBg(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final JobManager mgr = self.jobManager();
        final Optional<List<Integer>> ids;
        if (args.isEmpty()) {
            ids = JobBuiltins.currentJob(self, mgr, "bg").map(List::of);
        } else {
            ids = JobBuiltins.namedJobs(self, mgr, args);
        }
        final int code;
        if (ids.isPresent()) {
            JobBuiltins.resumeAll(self, mgr, ids.get());
            code = 0;
        } else {
            code = 1;
        }
        return code;
    }

    /**
     * Implementation of {@code fg [%n]}.
     * POSIX 2.14: brings a background job to the foreground, waiting for
     * it to complete. Without args, operates on the most recent job.
     *
     * @param self The context
     * @param args The arguments
     * @param env The environment
     * @return The job's exit code, or 1 on error
     */
    static int doFg(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        final JobManager mgr = self.jobManager();
        final Optional<Integer> id;
        if (args.isEmpty()) {
            id = JobBuiltins.currentJob(self, mgr, "fg");
        } else {
            id = JobBuiltins.fgTarget(self, args.get(0));
        }
        final Optional<Job> job = id.flatMap(
            num -> JobBuiltins.existing(self, mgr, num)
        );
        final int code;
        if (job.isPresent()) {
            self.outStream().println(job.get().getCommand());
            code = mgr.waitForJob(id.get());
        } else {
            code = 1;
        }
        return code;
    }

    private static List<Job> selectJobs(
        final JobManager mgr, final List<String> args
    ) {
        final List<Job> all;
        if (args.isEmpty()) {
            all = mgr.listJobs();
        } else {
            all = new ArrayList<>(0);
            for (final String arg : args) {
                JobBuiltins.lookup(mgr, arg).ifPresent(all::add);
            }
        }
        return all;
    }

    private static Optional<Job> lookup(final JobManager mgr, final String arg) {
        final int id = JobBuiltins.parseJobId(arg);
        final Optional<Job> job;
        if (id > 0) {
            job = Optional.ofNullable(mgr.getJob(id));
        } else {
            job = Optional.empty();
        }
        return job;
    }

    private static Optional<Integer> currentJob(
        final BuiltinIo self, final JobManager mgr, final String name
    ) {
        final List<Job> all = mgr.listJobs();
        final Optional<Integer> id;
        if (all.isEmpty()) {
            self.errStream().println(String.format("sh: %s: no current job", name));
            id = Optional.empty();
        } else {
            id = Optional.of(all.get(all.size() - 1).getId());
        }
        return id;
    }

    private static Optional<List<Integer>> namedJobs(
        final BuiltinIo self, final JobManager mgr, final List<String> args
    ) {
        final List<Integer> ids = new ArrayList<>(args.size());
        boolean found = true;
        int idx = 0;
        while (found && idx < args.size()) {
            final String arg = args.get(idx);
            final int id = JobBuiltins.parseJobId(arg);
            found = id > 0 && mgr.getJob(id) != null;
            if (found) {
                ids.add(id);
            } else {
                self.errStream().println(String.format("sh: bg: %s: no such job", arg));
            }
            idx += 1;
        }
        final Optional<List<Integer>> result;
        if (found) {
            result = Optional.of(ids);
        } else {
            result = Optional.empty();
        }
        return result;
    }

    private static void resumeAll(
        final BuiltinIo self, final JobManager mgr, final List<Integer> ids
    ) {
        for (final int id : ids) {
            final Job job = mgr.getJob(id);
            final String command;
            if (job == null) {
                command = "";
            } else {
                command = job.getCommand();
                JobBuiltins.resumeStopped(job);
            }
            self.outStream().println(String.format("[%d] %s", id, command));
        }
    }

    private static void resumeStopped(final Job job) {
        if (job.getState() == JobManager.State.STOPPED) {
            job.resume();
        }
    }

    private static Optional<Integer> fgTarget(final BuiltinIo self, final String arg) {
        final int id = JobBuiltins.parseJobId(arg);
        final Optional<Integer> result;
        if (id > 0) {
            result = Optional.of(id);
        } else {
            self.errStream().println(String.format("sh: fg: %s: no such job", arg));
            result = Optional.empty();
        }
        return result;
    }

    private static Optional<Job> existing(
        final BuiltinIo self, final JobManager mgr, final int id
    ) {
        final Optional<Job> job = Optional.ofNullable(mgr.getJob(id));
        if (job.isEmpty()) {
            self.errStream().println(String.format("sh: fg: %d: no such job", id));
        }
        return job;
    }

    private static int parseJobId(final String arg) {
        final String digits;
        if (arg.startsWith("%")) {
            digits = arg.substring(1);
        } else {
            digits = arg;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(digits);
        } catch (final NumberFormatException ex) {
            parsed = -1;
        }
        return parsed;
    }
}
