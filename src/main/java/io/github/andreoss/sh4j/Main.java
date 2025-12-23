/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.executor.Executor;
import java.util.List;

/**
 * Command-line entry point for sh4j. Dispatches to either script mode (a
 * single filename argument plus positional parameters) or interactive
 * mode (read-eval-print loop over stdin).
 *
 * @since 1.0
 */
public final class Main {

    @GeneratedDefensive
    private Main() {
    }

    /**
     * Standard {@code main} entry. Delegates to {@link #exitCode(String[])}
     * and feeds the result to {@link System#exit(int)}.
     *
     * @param args The CLI arguments
     */
    @GeneratedDefensive
    public static void main(final String[] args) {
        System.exit(exitCode(args));
    }

    /**
     * Computes the process exit code for the given CLI arguments.
     *
     * @param args The CLI arguments (script path first, then positional args)
     * @return The exit status the JVM should report
     */
    static int exitCode(final String... args) {
        return MainArgs.exitCode(args);
    }

    /**
     * Parses and executes shell source text.
     *
     * @param source The shell source
     * @param params Positional parameters ({@code $0} first)
     * @return Exit status
     */
    static int runSource(final String source, final List<String> params) {
        return MainRunner.runSource(source, params);
    }

    /**
     * Runs a script with the script name as its sole positional parameter.
     *
     * @param filename The script path
     * @return The script's exit status
     */
    static int runScript(final String filename) {
        return MainRunner.runScript(filename);
    }

    /**
     * Runs a script with the given positional parameters.
     *
     * @param filename   The script path
     * @param args The positional parameters (including {@code $0})
     * @return The script's exit status
     */
    static int runScript(final String filename, final List<String> args) {
        return MainRunner.runScript(filename, args);
    }

    /**
     * Runs the interactive REPL with a specific ENV startup file.
     *
     * @param envpath The ENV startup file path (may be null)
     * @return Always 0; failures are surfaced via the prompt
     */
    static int interactive(final String envpath) {
        return MainRepl.interactive(envpath);
    }

    /**
     * Executes a single command line in the interactive shell.
     *
     * @param executor The shell executor
     * @param line The command line to execute
     */
    static void executeLine(final Executor executor, final String line) {
        MainRepl.executeLine(executor, line);
    }

    /**
     * Checks if a command string is incomplete (needs more input).
     *
     * @param input The command text accumulated so far
     * @return True when the input appears to be an incomplete command
     */
    static boolean isIncomplete(final String input) {
        return MainRepl.isIncomplete(input);
    }

    /**
     * Executes the ENV startup file if it exists.
     *
     * @param executor The executor to run the file with
     * @param envpath  The path to the startup file
     */
    static void runStartupFile(final Executor executor, final String envpath) {
        MainRepl.runStartupFile(executor, envpath);
    }
}
