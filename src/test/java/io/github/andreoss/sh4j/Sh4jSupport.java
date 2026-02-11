/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.executor.Executor;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import io.github.andreoss.sh4j.PrintSink;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared implementation of the integration testing API.
 *
 * @since 1.0
 */
public final class Sh4jSupport {

    /** Default environment variables. */
    static final Map<String, String> DEFAULT_ENV = new HashMap<>();

    static {
        Sh4jSupport.DEFAULT_ENV.put("PATH", System.getenv("PATH"));
        Sh4jSupport.DEFAULT_ENV.put("HOME", System.getProperty("user.home", "/root"));
    }

    /** Environment used when no explicit environment is supplied. */
    private final Map<String, String> defaultenv = Sh4jSupport.DEFAULT_ENV;

    /**
     * Runs commands with a custom environment and stdin.
     *
     * @param env The environment variables
     * @param stdin The stdin input (may be null)
     * @param commands The commands to execute
     * @return The exit code of the last command
     */
    public int run(
        final Map<String, String> env,
        final InputStream stdin,
        final String... commands
    ) {
        final Executor executor = new Executor(new HashMap<>(this.resolveEnv(env)));
        if (stdin != null) {
            executor.setInputStream(stdin);
        }
        int code = 0;
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            code = executor.execute(ast);
            if (Sh4jSupport.errexit(executor, code)) {
                break;
            }
        }
        executor.runExitTrap(code);
        return code;
    }

    /**
     * Runs commands with a custom environment and captures stdout.
     *
     * @param env The environment variables
     * @param commands The commands to execute
     * @return The captured stdout
     */
    public String out(final Map<String, String> env, final String... commands) {
        final Executor executor = new Executor(new HashMap<>(this.resolveEnv(env)));
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setOutputStream(new PrintSink(bos, StandardCharsets.UTF_8));
        int code = 0;
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            code = executor.execute(ast);
            if (Sh4jSupport.errexit(executor, code)) {
                break;
            }
        }
        executor.runExitTrap(code);
        return bos.toString(StandardCharsets.UTF_8);
    }

    /**
     * Runs commands with a custom environment and captures stderr.
     *
     * @param env The environment variables
     * @param commands The commands to execute
     * @return The captured stderr
     */
    public String err(final Map<String, String> env, final String... commands) {
        final Executor executor = new Executor(new HashMap<>(this.resolveEnv(env)));
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        executor.setErrorStream(new PrintSink(bos, StandardCharsets.UTF_8));
        int code = 0;
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            code = executor.execute(ast);
            if (Sh4jSupport.errexit(executor, code)) {
                break;
            }
        }
        executor.runExitTrap(code);
        return bos.toString(StandardCharsets.UTF_8);
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
        final Executor executor = new Executor(new HashMap<>(this.resolveEnv(env)));
        int code = 0;
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            code = executor.execute(ast);
            if (Sh4jSupport.errexit(executor, code)) {
                break;
            }
        }
        executor.runExitTrap(code);
        return executor.getVariable(varname);
    }

    /**
     * Runs commands and returns the full environment after execution.
     *
     * @param commands The commands to execute
     * @return An unmodifiable view of the environment
     */
    public Map<String, String> env(final String... commands) {
        final Executor executor = new Executor(new HashMap<>(this.defaultenv));
        int code = 0;
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            code = executor.execute(ast);
            if (Sh4jSupport.errexit(executor, code)) {
                break;
            }
        }
        executor.runExitTrap(code);
        return new HashMap<>(executor.environmentMap());
    }

    /**
     * Runs commands with custom environment, stdin, and returns a full result.
     *
     * @param env The environment variables
     * @param stdin The stdin input (may be null)
     * @param commands The commands to execute
     * @return The result containing exit code, stdout, stderr, and environment
     */
    public String stdout(final InputStream stdin, final String... commands) {
        return this.runFull(Sh4jSupport.DEFAULT_ENV, stdin, commands).stdout();
    }

    public Sh4j.Result runFull(
        final Map<String, String> env,
        final InputStream stdin,
        final String... commands
    ) {
        final Executor executor = new Executor(new HashMap<>(this.resolveEnv(env)));
        if (stdin != null) {
            executor.setInputStream(stdin);
        }
        final ByteArrayOutputStream outbuffer = new ByteArrayOutputStream();
        final ByteArrayOutputStream errbuffer = new ByteArrayOutputStream();
        executor.setCaptureStream(outbuffer);
        executor.setErrorStream(new PrintSink(errbuffer, StandardCharsets.UTF_8));
        int code = 0;
        for (final String cmd : commands) {
            final Lexer lexer = new Lexer(new CharReader(cmd));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            code = executor.execute(ast);
            if (Sh4jSupport.errexit(executor, code)) {
                break;
            }
        }
        executor.runExitTrap(code);
        return new Sh4j.Result(
            code,
            outbuffer.toString(StandardCharsets.UTF_8),
            errbuffer.toString(StandardCharsets.UTF_8)
        ).environment(new HashMap<>(executor.environmentMap()));
    }

    private Map<String, String> resolveEnv(final Map<String, String> env) {
        final Map<String, String> resolved;
        if (env == null) {
            resolved = this.defaultenv;
        } else {
            resolved = env;
        }
        return resolved;
    }

    private static boolean errexit(final Executor executor, final int code) {
        return executor.shellFlags().indexOf('e') >= 0 && code != 0;
    }
}
