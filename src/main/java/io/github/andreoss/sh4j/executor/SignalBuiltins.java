/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.util.List;
import java.util.Map;

/**
 * Built-ins that signal and wait for processes.
 *
 * @since 1.0
 */
final class SignalBuiltins {

    @GeneratedDefensive
    private SignalBuiltins() {
    }

    /**
     * Implementation of {@code kill [-s SIG | -SIG] PID... | -l}.
     * Signals the shell's own background jobs by job id or pseudo pid,
     * which stop and report 128 plus the signal number; other processes
     * get TERM, or KILL for signal 9; an unknown name means TERM.
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
        final int code;
        if (args.isEmpty()) {
            code = 1;
        } else if ("-l".equals(args.get(0))) {
            self.outStream().println("HUP INT QUIT KILL TERM");
            code = 0;
        } else {
            code = SignalBuiltins.killAll(self, args);
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
            for (final String expanded : args) {
                try {
                    code = self.jobManager().waitForPid(Long.parseLong(expanded));
                } catch (final NumberFormatException ex) {
                    self.errStream().println(
                        String.format("sh: wait: %s: not a pid or job", expanded)
                    );
                    code = 127;
                }
            }
        }
        return code;
    }

    private static int killAll(final BuiltinContext self, final List<String> args) {
        final int first = SignalBuiltins.firstPid(args);
        final Kill kill = new Kill(self, KillSignal.number(SignalBuiltins.spec(args, first)));
        int code = 0;
        if (first >= args.size()) {
            code = 1;
        }
        int idx = first;
        while (idx < args.size() && code == 0) {
            code = SignalBuiltins.signalOne(self, kill, args.get(idx));
            idx += 1;
        }
        return code;
    }

    private static int firstPid(final List<String> args) {
        final int idx;
        if ("-s".equals(args.get(0)) && args.size() >= 2) {
            idx = 2;
        } else if (args.get(0).startsWith("-") && args.get(0).length() > 1) {
            idx = 1;
        } else {
            idx = 0;
        }
        return idx;
    }

    private static String spec(final List<String> args, final int first) {
        final String sig;
        if (first == 2) {
            sig = args.get(1);
        } else if (first == 1) {
            sig = args.get(0).substring(1);
        } else {
            sig = "";
        }
        return sig;
    }

    private static int signalOne(final BuiltinIo self, final Kill kill, final String target) {
        int code = 0;
        if (!kill.send(target)) {
            self.errStream().println(String.format("sh: kill: %s: no such process", target));
            code = 127;
        }
        return code;
    }
}
