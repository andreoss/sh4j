/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CLI argument parsing for {@link Main}.
 *
 * @since 1.0
 */
final class MainArgs {

    /**
     * Utility class; no instances.
     */
    private MainArgs() {
    }

    /**
     * Computes the process exit code for the given CLI arguments.
     *
     * @param args The CLI arguments
     * @return The exit status the JVM should report
     */
    static int exitCode(final String... args) {
        final Options opts = MainArgs.parse(args);
        final int code;
        if (opts.dashed()) {
            if (opts.command() == null) {
                code = MainArgs.missingCommand();
            } else {
                code = MainRunner.runSource(opts.command(), opts.params(), opts.flags());
            }
        } else if (opts.stdind()) {
            code = MainRunner.runSource(
                MainRunner.readAll(System.in), Collections.singletonList("sh"), opts.flags()
            );
        } else if (opts.inter() || opts.script() == null) {
            code = MainRepl.interactive(System.getenv("ENV"));
        } else {
            code = MainRunner.runScript(opts.script(), opts.params(), opts.flags());
        }
        return code;
    }

    private static PrintStream err() {
        return System.err;
    }

    private static int missingCommand() {
        MainArgs.err().println("sh: -c: option requires an argument");
        return 1;
    }

    private static String letters(final String arg) {
        final StringBuilder letters = new StringBuilder();
        for (final char opt : arg.substring(1).toCharArray()) {
            if ("aCefnuvx".indexOf(opt) >= 0) {
                letters.append(opt);
            }
        }
        return letters.toString();
    }

    private static Options parse(final String... args) {
        int idx = 0;
        String command = null;
        boolean dashed = false;
        boolean stdind = false;
        boolean inter = false;
        String flags = "";
        while (idx < args.length) {
            final String arg = args[idx];
            if ("--".equals(arg)) {
                idx = idx + 1;
                break;
            }
            if ("-c".equals(arg)) {
                dashed = true;
                if (idx + 1 < args.length) {
                    command = args[idx + 1];
                    idx = idx + 2;
                }
                break;
            }
            if ("-s".equals(arg)) {
                stdind = true;
                idx = idx + 1;
            } else if ("-i".equals(arg)) {
                inter = true;
                idx = idx + 1;
            } else if (arg.startsWith("-")) {
                flags = flags.concat(MainArgs.letters(arg));
                idx = idx + 1;
            } else {
                break;
            }
        }
        return MainArgs.options(args, idx, dashed, command, stdind, inter).flagged(flags);
    }

    private static Options options(
        final String[] args, final int idx, final boolean dashed,
        final String command, final boolean stdind,
        final boolean inter
    ) {
        final String script;
        final List<String> params;
        if (dashed) {
            script = null;
            params = MainArgs.positional(args, idx, "sh");
        } else if (stdind) {
            script = null;
            params = Collections.singletonList("sh");
        } else if (inter || idx >= args.length) {
            script = null;
            params = Collections.emptyList();
        } else {
            script = args[idx];
            params = MainArgs.positional(args, idx, args[idx]);
        }
        return new Options(
            dashed, command, stdind, inter, script, params, ""
        );
    }

    private static List<String> positional(
        final String[] args, final int start, final String first
    ) {
        final List<String> params = new ArrayList<>(0);
        params.add(first);
        for (int pos = start + 1; pos < args.length; pos = pos + 1) {
            params.add(args[pos]);
        }
        return params;
    }

    private record Options(
        boolean dashed, String command, boolean stdind,
        boolean inter, String script, List<String> params, String flags
    ) {

        /**
         * Copies these options with the given shell option letters.
         *
         * @param letters The shell option letters, as for {@code set}
         * @return The options with the letters
         */
        Options flagged(final String letters) {
            return new Options(
                this.dashed(), this.command(), this.stdind(),
                this.inter(), this.script(), this.params(), letters
            );
        }
    }
}
