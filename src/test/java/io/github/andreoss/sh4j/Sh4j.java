/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenient API for integration testing sh4j.
 * Usage: {@code Sh4j.INSTANCE.run("set -e", "false", "echo 1")} returns exit code 1.
 *
 * @since 1.0
 */
public final class Sh4j {

    /**
     * Shared instance of the API.
     */
    public static final Sh4j INSTANCE = new Sh4j();

    /** Support helper backing this instance. */
    private final Sh4jSupport support;

    private Sh4j() {
        this.support = new Sh4jSupport();
    }

    /**
     * Runs a sequence of shell commands and returns the exit code.
     *
     * @param commands The commands to execute, one per argument
     * @return The exit code of the last command
     */
    public int run(final String... commands) {
        return this.support.run(Sh4jSupport.DEFAULT_ENV, null, commands);
    }

    /**
     * Runs a sequence of shell commands with custom environment.
     *
     * @param env The environment variables
     * @param commands The commands to execute, one per argument
     * @return The exit code of the last command
     */
    public int run(final Map<String, String> env, final String... commands) {
        return this.support.run(env, null, commands);
    }

    /**
     * Runs commands and captures stdout.
     *
     * @param commands The commands to execute
     * @return The captured stdout
     */
    public String out(final String... commands) {
        return this.support.out(Sh4jSupport.DEFAULT_ENV, commands);
    }

    /**
     * Runs commands with a custom environment and captures stdout.
     *
     * @param env The environment variables
     * @param commands The commands to execute
     * @return The captured stdout
     */
    public String out(final Map<String, String> env, final String... commands) {
        return this.support.out(env, commands);
    }

    /**
     * Runs commands and returns the value of a variable after execution.
     *
     * @param varname The variable name to retrieve
     * @param commands The commands to execute
     * @return The variable value, or null if not set
     */
    public String getVar(final String varname, final String... commands) {
        return this.support.getVar(Sh4jSupport.DEFAULT_ENV, varname, commands);
    }

    /**
     * Runs commands with a custom environment and returns a variable value.
     *
     * @param env The environment variables
     * @param varname The variable name to retrieve
     * @param commands The commands to execute
     * @return The variable value, or null if not set
     */
    public String getVar(
        final Map<String, String> env,
        final String varname,
        final String... commands
    ) {
        return this.support.getVar(env, varname, commands);
    }

    /**
     * Runs commands and returns a full result object.
     *
     * @param commands The commands to execute
     * @return The result containing exit code, stdout, stderr, and environment
     */
    public Result runFull(final String... commands) {
        return this.support.runFull(Sh4jSupport.DEFAULT_ENV, null, commands);
    }

    /**
     * Runs commands with stdin and returns a full result object.
     *
     * @param stdin The stdin input (may be null)
     * @param commands The commands to execute
     * @return The result containing exit code, stdout, stderr, and environment
     */
    public Result runFull(final InputStream stdin, final String... commands) {
        return this.support.runFull(Sh4jSupport.DEFAULT_ENV, stdin, commands);
    }

    /**
     * Runs commands with custom environment, stdin, and returns a full result.
     *
     * @param env The environment variables
     * @param stdin The stdin input (may be null)
     * @param commands The commands to execute
     * @return The result containing exit code, stdout, stderr, and environment
     */
    public Result runFull(
        final Map<String, String> env,
        final InputStream stdin,
        final String... commands
    ) {
        return this.support.runFull(env, stdin, commands);
    }

    /**
     * Result of a run: exit code, stdout, stderr, and environment.
     *
     * @param exitcode The exit code
     * @param stdout The standard output
     * @param stderr The standard error
     * @param environment The environment variables
     * @since 1.0
     */
    public record Result(
        int exitcode,
        String stdout,
        String stderr,
        Map<String, String> environment
    ) {

        /**
         * Creates a result without an environment snapshot.
         *
         * @param code The exit code
         * @param output The standard output
         * @param error The standard error
         */
        Result(final int code, final String output, final String error) {
            this(code, output, error, new HashMap<>());
        }

        /**
         * Returns the exit code.
         *
         * @return The exit code
         */
        public int exitCode() {
            return this.exitcode;
        }

        /**
         * Replaces the environment snapshot.
         *
         * @param vars The environment variables
         * @return This result
         */
        public Result environment(final Map<String, String> vars) {
            this.environment.putAll(vars);
            return this;
        }
    }
}
