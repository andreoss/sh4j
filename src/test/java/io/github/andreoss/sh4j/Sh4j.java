/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.andreoss.sh4j.executor.Executor;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;

/**
 * Convenient API for integration testing sh4j.
 * Usage: {@code Sh4j.run("set -e", "false", "echo 1")} returns exit code 1.
 * @since 1.0
 */
public final class Sh4j {

    /** Default environment variables. */
    private static final Map<String, String> DEFAULT_ENV = new HashMap<String, String>();

    static {
        DEFAULT_ENV.put("PATH", System.getenv("PATH"));
        DEFAULT_ENV.put("HOME", System.getProperty("user.home", "/root"));
    }

    /**
     * Utility class; prevent instantiation.
     */
    private Sh4j() {
        super();
    }

    /**
     * Runs a sequence of shell commands and returns the exit code.
     * @param commands The commands to execute, one per argument
     * @return The exit code of the last command
     */
    public static int run(final String... commands) {
        return run(DEFAULT_ENV, commands);
    }

    /**
     * Runs a sequence of shell commands with custom environment.
     * @param env The environment variables
     * @param commands The commands to execute, one per argument
     * @return The exit code of the last command
     */
    public static int run(final Map<String, String> env, final String... commands) {
        return run(env, null, commands);
    }

    /**
     * Runs a sequence of shell commands with custom environment and stdin.
     * @param env The environment variables
     * @param stdin The stdin input (may be null)
     * @param commands The commands to execute, one per argument
     * @return The exit code of the last command
     */
    public static int run(
        final Map<String, String> env,
        final InputStream stdin,
        final String... commands
    ) {
        final Executor executor = new Executor(new HashMap<String, String>(env));
        if (stdin != null) {
            executor.setInputStream(stdin);
        }
        int code = 0;
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            code = executor.execute(ast);
            if (errexit(executor, code)) {
                break;
            }
        }
        return code;
    }

    /**
     * Checks if errexit is active and should stop execution.
     * @param executor The executor with shell flags
     * @param code The exit code of the last command
     * @return True if execution should stop
     */
    private static boolean errexit(final Executor executor, final int code) {
        return executor.shellFlags().indexOf('e') >= 0 && code != 0;
    }

    /**
     * Runs commands and captures stdout.
     * @param commands The commands to execute
     * @return The captured stdout
     */
    public static String out(final String... commands) {
        return out(DEFAULT_ENV, commands);
    }

    /**
     * Runs commands with custom environment and captures stdout.
     * @param env The environment variables
     * @param commands The commands to execute
     * @return The captured stdout
     */
    public static String out(final Map<String, String> env, final String... commands) {
        final Executor executor = new Executor(new HashMap<String, String>(env));
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setOutputStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            final int code = executor.execute(ast);
            if (errexit(executor, code)) {
                break;
            }
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

    /**
     * Runs commands and captures stderr.
     * @param commands The commands to execute
     * @return The captured stderr
     */
    public static String err(final String... commands) {
        return err(DEFAULT_ENV, commands);
    }

    /**
     * Runs commands with custom environment and captures stderr.
     * @param env The environment variables
     * @param commands The commands to execute
     * @return The captured stderr
     */
    public static String err(final Map<String, String> env, final String... commands) {
        final Executor executor = new Executor(new HashMap<String, String>(env));
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setErrorStream(new PrintStream(bos, true, StandardCharsets.UTF_8));
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            final int code = executor.execute(ast);
            if (errexit(executor, code)) {
                break;
            }
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

    /**
     * Runs commands and returns the value of a variable after execution.
     * @param varName The variable name to retrieve
     * @param commands The commands to execute
     * @return The variable value, or null if not set
     */
    public static String getVar(final String varName, final String... commands) {
        return getVar(DEFAULT_ENV, varName, commands);
    }

    /**
     * Runs commands with custom environment and returns a variable value.
     * @param env The environment variables
     * @param varName The variable name to retrieve
     * @param commands The commands to execute
     * @return The variable value, or null if not set
     */
    public static String getVar(
        final Map<String, String> env,
        final String varName,
        final String... commands
    ) {
        final Executor executor = new Executor(new HashMap<String, String>(env));
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            final int code = executor.execute(ast);
            if (errexit(executor, code)) {
                break;
            }
        }
        return executor.getVariable(varName);
    }

    /**
     * Runs commands and returns the full environment after execution.
     * @param commands The commands to execute
     * @return An unmodifiable view of the environment
     */
    public static Map<String, String> env(final String... commands) {
        final Executor executor = new Executor(new HashMap<String, String>(DEFAULT_ENV));
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            final int code = executor.execute(ast);
            if (errexit(executor, code)) {
                break;
            }
        }
        return new HashMap<String, String>(executor.environmentMap());
    }

    /**
     * Result of a run: exit code, stdout, stderr, and environment.
     * @since 1.0
     */
    public static final class Result {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final Map<String, String> environment;

        Result(
            final int exitCode,
            final String stdout,
            final String stderr,
            final Map<String, String> environment
        ) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.environment = environment;
        }

        public int exitCode() {
            return this.exitCode;
        }

        public String stdout() {
            return this.stdout;
        }

        public String stderr() {
            return this.stderr;
        }

        public Map<String, String> environment() {
            return this.environment;
        }
    }

    /**
     * Runs commands and returns a full result object.
     * @param commands The commands to execute
     * @return The result containing exit code, stdout, stderr, and environment
     */
    public static Result runFull(final String... commands) {
        return runFull(DEFAULT_ENV, null, commands);
    }

    /**
     * Runs commands with stdin and returns a full result object.
     * @param stdin The stdin input (may be null)
     * @param commands The commands to execute
     * @return The result containing exit code, stdout, stderr, and environment
     */
    public static Result runFull(final InputStream stdin, final String... commands) {
        return runFull(DEFAULT_ENV, stdin, commands);
    }

    /**
     * Runs commands with custom environment, stdin, and returns a full result.
     * @param env The environment variables
     * @param stdin The stdin input (may be null)
     * @param commands The commands to execute
     * @return The result containing exit code, stdout, stderr, and environment
     */
    public static Result runFull(
        final Map<String, String> env,
        final InputStream stdin,
        final String... commands
    ) {
        final Executor executor = new Executor(new HashMap<String, String>(env));
        if (stdin != null) {
            executor.setInputStream(stdin);
        }
        final ByteArrayOutputStream outBos = new ByteArrayOutputStream();
        final ByteArrayOutputStream errBos = new ByteArrayOutputStream();
        executor.setCaptureStream(outBos);
        executor.setErrorStream(new PrintStream(errBos, true, StandardCharsets.UTF_8));
        int code = 0;
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            code = executor.execute(ast);
            if (errexit(executor, code)) {
                break;
            }
        }
        return new Result(
            code,
            outBos.toString(StandardCharsets.UTF_8),
            errBos.toString(StandardCharsets.UTF_8),
            new HashMap<String, String>(executor.environmentMap())
        );
    }
}
