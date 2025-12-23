/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.executor.Executor;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ParseException;
import io.github.andreoss.sh4j.parser.Parser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Interactive REPL support for {@link Main}.
 *
 * @since 1.0
 */
final class MainRepl {

    private MainRepl() {
    }

    /**
     * Runs the interactive REPL with a specific ENV startup file.
     *
     * @param envpath The ENV startup file path (may be null)
     * @return Always 0; failures are surfaced via the prompt
     */
    static int interactive(final String envpath) {
        final Executor executor = new Executor();
        if (envpath != null && !envpath.isEmpty()) {
            MainRepl.runStartupFile(executor, envpath);
        }
        new Loop(executor).run();
        return 0;
    }

    /**
     * Executes a single command line in the interactive shell.
     *
     * @param executor The shell executor
     * @param line The command line to execute
     */
    static void executeLine(final Executor executor, final String line) {
        try {
            executor.execute(
                new Parser(new Lexer(new CharReader(line))).parse()
            );
        } catch (final ParseException ex) {
            MainRepl.err().println(String.format("sh: syntax error: %s", ex.getMessage()));
        }
    }

    /**
     * Checks if a command string is incomplete (needs more input).
     *
     * @param input The command text accumulated so far
     * @return True when the input appears to be an incomplete command
     */
    static boolean isIncomplete(final String input) {
        int quotes = 0;
        int dquotes = 0;
        boolean escaped = false;
        for (int idx = 0; idx < input.length(); idx = idx + 1) {
            final char chr = input.charAt(idx);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (chr == '\\') {
                escaped = true;
                continue;
            }
            if (chr == '\'' && dquotes % 2 == 0) {
                quotes = quotes + 1;
            } else if (chr == '"' && quotes % 2 == 0) {
                dquotes = dquotes + 1;
            }
        }
        return quotes % 2 != 0 || dquotes % 2 != 0 || escaped;
    }

    /**
     * Executes the ENV startup file if it exists.
     *
     * @param executor The executor to run the file with
     * @param envpath  The path to the startup file
     */
    static void runStartupFile(final Executor executor, final String envpath) {
        try (
            BufferedReader reader = Files.newBufferedReader(
                Paths.get(envpath), StandardCharsets.UTF_8
            )
        ) {
            final StringBuilder content = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                content.append(line).append('\n');
                line = reader.readLine();
            }
            executor.execute(
                new Parser(new Lexer(new CharReader(content.toString()))).parse()
            );
        } catch (final IOException ex) {
            MainRepl.err().println(String.format("sh: %s: cannot open", envpath));
        } catch (final ParseException ex) {
            MainRepl.err().println(String.format("sh: syntax error: %s", ex.getMessage()));
        }
    }

    static PrintStream out() {
        return System.out;
    }

    private static PrintStream err() {
        return System.err;
    }
}
