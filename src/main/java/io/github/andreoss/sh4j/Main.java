/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import io.github.andreoss.sh4j.executor.Executor;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;

/**
 * Command-line entry point for sh4j. Dispatches to either script mode (a
 * single filename argument plus positional parameters) or interactive
 * mode (read-eval-print loop over stdin).
 *
 * @since 1.0
 */
public final class Main {

    /**
     * Utility class; the constructor exists only to enforce non-instantiation
     * and is annotated as defensive code for JaCoCo's filter.
     */
    @GeneratedDefensive
    private Main() {
        super();
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
    static int exitCode(final String[] args) {
        final int code;
        if (args.length > 0) {
            final List<String> scriptArgs = new ArrayList<>(args.length);
            for (final String arg : args) {
                scriptArgs.add(arg);
            }
            code = runScript(args[0], scriptArgs);
        } else {
            code = interactive();
        }
        return code;
    }

    /**
     * Runs a script with the script name as its sole positional parameter.
     *
     * @param filename The script path
     * @return The script's exit status
     */
    static int runScript(final String filename) {
        return runScript(filename, List.of(filename));
    }

    /**
     * Runs a script with the given positional parameters.
     *
     * @param filename   The script path
     * @param scriptArgs The positional parameters (including {@code $0})
     * @return The script's exit status
     */
    static int runScript(final String filename, final List<String> scriptArgs) {
        final StringBuilder content = new StringBuilder();
        int code;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine();
            while (line != null) {
                content.append(line).append('\n');
                line = reader.readLine();
            }
            final CharReader charReader = new CharReader(content.toString());
            final Lexer lexer = new Lexer(charReader);
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            final Executor executor = new Executor(System.getenv(), scriptArgs);
            code = executor.execute(ast);
        } catch (final IOException ex) {
            code = 1;
        }
        return code;
    }

    /**
     * Runs the interactive REPL on stdin until EOF or an IO failure.
     *
     * @return Always 0; failures are surfaced via the prompt
     */
    static int interactive() {
        final Executor executor = new Executor();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean running = true;
        while (running) {
            System.out.print("$ ");
            try {
                final String line = reader.readLine();
                if (line == null) {
                    running = false;
                } else if (!line.trim().isEmpty()) {
                    final Lexer lexer = new Lexer(new CharReader(line));
                    final Parser parser = new Parser(lexer);
                    final ListNode ast = parser.parse();
                    executor.execute(ast);
                }
            } catch (final IOException ex) {
                running = false;
            }
        }
        return 0;
    }
}
