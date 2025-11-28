/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CLI argument parsing for {@link Main}.
 * @since 1.0
 */
final class MainArgs {

    /**
     * Utility class; no instances.
     */
    private MainArgs() {
        super();
    }

    /**
     * Computes the process exit code for the given CLI arguments.
     * @param args The CLI arguments
     * @return The exit status the JVM should report
     */
    static int exitCode(final String... args) {
        final Options opts = MainArgs.parse(args);
        final int code;
        if (opts.command() != null) {
            code = MainRunner.runSource(opts.command(), opts.params());
        } else if (opts.readStdin()) {
            code = MainRunner.runSource(
                MainRunner.readAll(System.in), Collections.singletonList("sh")
            );
        } else if (opts.forceInteractive() || opts.script() == null) {
            code = MainRepl.interactive(System.getenv("ENV"));
        } else {
            code = MainRunner.runScript(opts.script(), opts.params());
        }
        return code;
    }

    /**
     * Parses CLI flags and positional arguments.
     * @param args Raw CLI arguments
     * @return Parsed options
     */
    private static Options parse(final String... args) {
        int idx = 0;
        String command = null;
        boolean readStdin = false;
        boolean forceInteractive = false;
        while (idx < args.length) {
            final String arg = args[idx];
            if ("--".equals(arg)) {
                idx = idx + 1;
                break;
            }
            if ("-c".equals(arg)) {
                if (idx + 1 < args.length) {
                    command = args[idx + 1];
                    idx = idx + 2;
                }
                break;
            }
            if ("-s".equals(arg)) {
                readStdin = true;
                idx = idx + 1;
            } else if ("-i".equals(arg)) {
                forceInteractive = true;
                idx = idx + 1;
            } else if (arg.startsWith("-")) {
                idx = idx + 1;
            } else {
                break;
            }
        }
        return MainArgs.options(args, idx, command, readStdin, forceInteractive);
    }

    /**
     * Builds options from the scan position.
     * @param args Raw CLI arguments
     * @param idx Current index
     * @param command Command text from {@code -c}, or null
     * @param readStdin Whether {@code -s} was given
     * @param forceInteractive Whether {@code -i} was given
     * @return Parsed options
     */
    private static Options options(
        final String[] args, final int idx, final String command,
        final boolean readStdin, final boolean forceInteractive
    ) {
        final String script;
        final List<String> params;
        if (command != null) {
            script = null;
            params = MainArgs.params(args, idx, "sh");
        } else if (readStdin) {
            script = null;
            params = Collections.singletonList("sh");
        } else if (forceInteractive || idx >= args.length) {
            script = null;
            params = Collections.emptyList();
        } else {
            script = args[idx];
            params = MainArgs.params(args, idx, args[idx]);
        }
        return new Options(command, readStdin, forceInteractive, script, params);
    }

    /**
     * Builds positional parameters starting at the given index.
     * @param args Raw CLI arguments
     * @param start Index of the first positional argument
     * @param first Value for {@code $0}
     * @return Positional parameters
     */
    private static List<String> params(
        final String[] args, final int start, final String first
    ) {
        final List<String> params = new ArrayList<>();
        params.add(first);
        for (int pos = start + 1; pos < args.length; pos = pos + 1) {
            params.add(args[pos]);
        }
        return params;
    }

    /**
     * Parsed CLI options.
     * @param command Command text from {@code -c}, or null
     * @param readStdin Whether {@code -s} was given
     * @param forceInteractive Whether {@code -i} was given
     * @param script Script path, or null
     * @param params Positional parameters
     * @since 1.0
     */
    private record Options(
        String command, boolean readStdin, boolean forceInteractive,
        String script, List<String> params
    ) {
    }
}
