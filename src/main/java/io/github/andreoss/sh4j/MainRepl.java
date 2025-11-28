/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import io.github.andreoss.sh4j.executor.Executor;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.Parser;

import static java.lang.System.err;
import static java.lang.System.out;

/**
 * Interactive REPL support for {@link Main}.
 * @since 1.0
 */
final class MainRepl {

    /**
     * Utility class; no instances.
     */
    private MainRepl() {
        super();
    }

    /**
     * Runs the interactive REPL with a specific ENV startup file.
     * @param envFile The ENV startup file path (may be null)
     * @return Always 0; failures are surfaced via the prompt
     */
    static int interactive(final String envFile) {
        final Executor executor = new Executor();
        if (envFile != null && !envFile.isEmpty()) {
            MainRepl.runStartupFile(executor, envFile);
        }
        new MainRepl.Loop(executor).run();
        return 0;
    }

    /**
     * Executes a single command line in the interactive shell.
     * @param executor The shell executor
     * @param line The command line to execute
     */
    static void executeLine(final Executor executor, final String line) {
        try {
            executor.execute(
                new Parser(new Lexer(new CharReader(line))).parse()
            );
        } catch (final io.github.andreoss.sh4j.parser.ParseException ex) {
            err.println("sh: syntax error: " + ex.getMessage());
        }
    }

    /**
     * Checks if a command string is incomplete (needs more input).
     * @param input The command text accumulated so far
     * @return True when the input appears to be an incomplete command
     */
    static boolean isIncomplete(final String input) {
        int singleQuotes = 0;
        int doubleQuotes = 0;
        boolean escaped = false;
        for (int idx = 0; idx < input.length(); idx = idx + 1) {
            final char ch = input.charAt(idx);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '\'' && doubleQuotes % 2 == 0) {
                singleQuotes = singleQuotes + 1;
            } else if (ch == '"' && singleQuotes % 2 == 0) {
                doubleQuotes = doubleQuotes + 1;
            }
        }
        return singleQuotes % 2 != 0 || doubleQuotes % 2 != 0 || escaped;
    }

    /**
     * Executes the ENV startup file if it exists.
     * @param executor The executor to run the file with
     * @param envFile  The path to the startup file
     */
    static void runStartupFile(final Executor executor, final String envFile) {
        try (
            BufferedReader reader = Files.newBufferedReader(
                Paths.get(envFile), StandardCharsets.UTF_8
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
            err.println("sh: " + envFile + ": cannot open");
        } catch (final io.github.andreoss.sh4j.parser.ParseException ex) {
            err.println("sh: syntax error: " + ex.getMessage());
        }
    }

    /**
     * Read-eval-print loop state.
     * @since 1.0
     */
    private static final class Loop {

        /**
         * Shell executor.
         */
        private final Executor executor;
        /**
         * stdin reader.
         */
        private final BufferedReader reader;
        /**
         * Whether more input is needed.
         */
        private boolean needMore;

        /**
         * Creates the loop.
         * @param executor Shell executor
         */
        Loop(final Executor executor) {
            this.executor = executor;
            this.reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
            );
        }

        /**
         * Runs until EOF or an IO failure.
         */
        void run() {
            final StringBuilder buffer = new StringBuilder();
            boolean running = true;
            while (running) {
                out.print(this.prompt());
                try {
                    final String line = this.reader.readLine();
                    if (line == null) {
                        running = false;
                    } else {
                        this.handle(line, buffer);
                    }
                } catch (final IOException ex) {
                    running = false;
                }
            }
        }

        /**
         * Handles one input line.
         * @param line Input line
         * @param buffer Accumulated incomplete command
         */
        private void handle(final String line, final StringBuilder buffer) {
            if (this.needMore) {
                buffer.append(line).append('\n');
                if (!MainRepl.isIncomplete(buffer.toString())) {
                    this.needMore = false;
                    MainRepl.executeLine(this.executor, buffer.toString());
                    buffer.setLength(0);
                }
            } else if (!line.isBlank()) {
                if (MainRepl.isIncomplete(line)) {
                    this.needMore = true;
                    buffer.setLength(0);
                    buffer.append(line).append('\n');
                } else {
                    MainRepl.executeLine(this.executor, line);
                }
            }
        }

        /**
         * Selects the prompt for the next line.
         * @return Prompt text
         */
        private String prompt() {
            final String ps1 = this.executor.getVariable("PS1");
            final String ps2 = this.executor.getVariable("PS2");
            final String prompt;
            if (this.needMore) {
                prompt = ps2 != null ? ps2 : "> ";
            } else {
                prompt = ps1 != null ? ps1 : "$ ";
            }
            return prompt;
        }
    }
}
