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
 * Built-ins that signal, wait for and move jobs.
 *
 * @since 1.0
 */
final class JobBuiltins {

    @GeneratedDefensive
    private JobBuiltins() {
    }

    /**
     * Implementation of {@code kill [-s SIG | -SIG] PID... | -l}.
     * POSIX 2.14. JDK constraint: ProcessHandle only supports TERM
     * (destroy) and KILL (destroyForcibly); other signals are
     * accepted at the command line but mapped to TERM (which is the
     * POSIX default) and a soft warning is not emitted.
     *
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @return The exit code: 0 on success, 1 on usage error, 127 if any PID cannot be signalled
     */
    static int doKill(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        if (args.isEmpty()) {
            code = 1;
        } else if ("-l".equals(args.get(0))) {
            self.outStream().println("HUP INT QUIT KILL TERM");
        } else {
            boolean forcibly = false;
            int idx = 0;
            if ("-s".equals(args.get(0)) && args.size() >= 2) {
                forcibly = "KILL".equals(args.get(1)) || "9".equals(args.get(1));
                idx = 2;
            } else if (args.get(0).startsWith("-") && args.get(0).length() > 1) {
                final String sig = args.get(0).substring(1);
                forcibly = "KILL".equals(sig) || "9".equals(sig);
                idx = 1;
            }
            if (idx >= args.size()) {
                code = 1;
            }
            while (idx < args.size() && code == 0) {
                code = signalOne(
                    self.expandVariables(args.get(idx), env), forcibly
                );
                idx += 1;
            }
        }
        return code;
    }

    private static int signalOne(final String pidstr, final boolean forcibly) {
        int code;
        try {
            final long pid = Long.parseLong(pidstr);
            if (JobManager.isJobPid(pid)) {
                code = 127;
            } else {
                final Optional<ProcessHandle> handle = ProcessHandle.of(pid);
                if (handle.isPresent()) {
                    final boolean sent;
                    if (forcibly) {
                        sent = handle.get().destroyForcibly();
                    } else {
                        sent = handle.get().destroy();
                    }
                    if (sent) {
                        code = 0;
                    } else {
                        code = 127;
                    }
                } else {
                    code = 127;
                }
            }
        } catch (final NumberFormatException ex) {
            code = 127;
        }
        return code;
    }

    /**
     * Implementation of {@code wait [PID...]}.
     * POSIX 2.14: without args, waits for all background jobs (exit 0).
     * With PID, waits for that bg job if known (exit 0), else returns 127
     * (POSIX "unknown process" status).
     *
     * @param self    Builtin context
     * @param args    Args after the command name (PIDs)
     * @param env     Environment for word expansion
     * @return The exit code: 0 on success, 127 when a named PID is not a known bg job
     */
    static int doWait(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env
    ) {
        int code = 0;
        if (args.isEmpty()) {
            self.waitForBackgroundJobs();
        } else {
            for (final String arg : args) {
                final String expanded = self.expandVariables(arg, env);
                try {
                    code = self.jobManager().waitForPid(Long.parseLong(expanded));
                } catch (final NumberFormatException ex) {
                    code = 127;
                }
            }
        }
        return code;
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
        final JobManager mgr = self.jobManager();
        final List<Job> all;
        if (args.isEmpty()) {
            all = mgr.listJobs();
        } else {
            all = new ArrayList<>(0);
            for (final String arg : args) {
                final int id = parseJobId(arg);
                if (id > 0) {
                    final Job job = mgr.getJob(id);
                    if (job != null) {
                        all.add(job);
                    }
                }
            }
        }
        for (final Job job : all) {
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
        final List<Integer> ids = new ArrayList<>(0);
        if (args.isEmpty()) {
            final List<Job> all = mgr.listJobs();
            if (all.isEmpty()) {
                self.errStream().println("sh: bg: no current job");
                return 1;
            }
            ids.add(all.get(all.size() - 1).getId());
        } else {
            for (final String arg : args) {
                final int id = parseJobId(arg);
                if (id <= 0 || mgr.getJob(id) == null) {
                    self.errStream().println(String.format("sh: bg: %s: no such job", arg));
                    return 1;
                }
                ids.add(id);
            }
        }
        for (final int id : ids) {
            final Job job = mgr.getJob(id);
            if (job != null && job.getState() == JobManager.State.STOPPED) {
                job.resume();
            }
            final String command;
            if (job == null) {
                command = "";
            } else {
                command = job.getCommand();
            }
            self.outStream().println(String.format("[%d] %s", id, command));
        }
        return 0;
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
        final int id;
        if (args.isEmpty()) {
            final List<Job> all = mgr.listJobs();
            if (all.isEmpty()) {
                self.errStream().println("sh: fg: no current job");
                return 1;
            }
            id = all.get(all.size() - 1).getId();
        } else {
            id = parseJobId(args.get(0));
            if (id <= 0) {
                self.errStream().println(String.format("sh: fg: %s: no such job", args.get(0)));
                return 1;
            }
        }
        final Job job = mgr.getJob(id);
        if (job == null) {
            self.errStream().println(String.format("sh: fg: %d: no such job", id));
            return 1;
        }
        self.outStream().println(job.getCommand());
        return mgr.waitForJob(id);
    }

    private static int parseJobId(final String arg) {
        final String digits;
        if (arg.startsWith("%")) {
            digits = arg.substring(1);
        } else {
            digits = arg;
        }
        int parsed = -1;
        try {
            parsed = Integer.parseInt(digits);
        } catch (final NumberFormatException ex) {
            parsed = -1;
        }
        return parsed;
    }
}
