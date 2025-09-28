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
import java.util.Arrays;
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
     * @param args The CLI arguments
     */
    @GeneratedDefensive
    public static void main(final String[] args) {
        System.exit(exitCode(args));
    }

    /**
     * Computes the process exit code for the given CLI arguments.
     * @param args The CLI arguments (script path first, then positional args)
     * @return The exit status the JVM should report
     */
    static int exitCode(final String[] args) {
        int idx = 0;
        boolean readStdin = false;
        boolean forceInteractive = false;
        String command = null;
        while (idx < args.length) {
            final String arg = args[idx];
            if ("-c".equals(arg)) {
                if (idx + 1 < args.length) {
                    command = args[idx + 1];
                    idx = idx + 2;
                }
                break;
            } else if ("-s".equals(arg)) {
                readStdin = true;
                idx = idx + 1;
            } else if ("-i".equals(arg)) {
                forceInteractive = true;
                idx = idx + 1;
            } else if ("--".equals(arg)) {
                idx = idx + 1;
                break;
            } else if (arg.startsWith("-")) {
                idx = idx + 1;
            } else {
                break;
            }
        }
        final int code;
        if (command != null) {
            final List<String> params = new ArrayList<>();
            params.add(idx < args.length ? args[idx] : "sh");
            for (int pos = idx + 1; pos < args.length; pos = pos + 1) {
                params.add(args[pos]);
            }
            code = runSource(command, params);
        } else if (readStdin) {
            code = runSource(readAll(System.in), Arrays.asList("sh"));
        } else if (forceInteractive || idx >= args.length) {
            code = interactive();
        } else {
            final List<String> scriptArgs = new ArrayList<>();
            scriptArgs.add(args[idx]);
            for (int pos = idx + 1; pos < args.length; pos = pos + 1) {
                scriptArgs.add(args[pos]);
            }
            code = runScript(args[idx], scriptArgs);
        }
        return code;
    }

    /**
     * Parses and executes shell source text.
     * @param source The shell source
     * @param params Positional parameters ({@code $0} first)
     * @return Exit status
     */
    static int runSource(final String source, final List<String> params) {
        int code;
        final Executor executor = new Executor(System.getenv(), params);
        try {
            final ListNode ast = new Parser(
                new Lexer(new CharReader(source))
            ).parse();
            code = executor.execute(ast);
        } catch (final io.github.andreoss.sh4j.parser.ParseException ex) {
            System.err.println("sh: syntax error: " + ex.getMessage());
            code = 2;
        } finally {
            executor.runExitTrap();
        }
        return code;
    }

    /**
     * Reads all bytes from a stream as UTF-8 text.
     * @param stream Input stream
     * @return Decoded text
     */
    private static String readAll(final java.io.InputStream stream) {
        final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        final String text;
        try {
            int read = stream.read();
            while (read != -1) {
                buffer.write(read);
                read = stream.read();
            }
            text = buffer.toString("UTF-8");
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
        return text;
    }

    /**
     * Runs a script with the script name as its sole positional parameter.
     * @param filename The script path
     * @return The script's exit status
     */
    static int runScript(final String filename) {
        return runScript(filename, Arrays.asList(filename));
    }

    /**
     * Runs a script with the given positional parameters.
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
            code = runSource(content.toString(), scriptArgs);
        } catch (final IOException ex) {
            System.err.println("sh: " + filename + ": cannot open");
            code = 1;
        }
        return code;
    }

    /**
     * Runs the interactive REPL on stdin until EOF or an IO failure.
     * @return Always 0; failures are surfaced via the prompt
     */
    static int interactive() {
        return interactive(System.getenv("ENV"));
    }

    /**
     * Runs the interactive REPL with a specific ENV startup file.
     * @param envFile The ENV startup file path (may be null)
     * @return Always 0; failures are surfaced via the prompt
     */
    static int interactive(final String envFile) {
        final Executor executor = new Executor();
        if (envFile != null && !envFile.isEmpty()) {
            runStartupFile(executor, envFile);
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean running = true;
        boolean needMore = false;
        final StringBuilder buffer = new StringBuilder();
        while (running) {
            final String ps1 = executor.getVariable("PS1");
            final String ps2 = executor.getVariable("PS2");
            final String prompt = needMore
                ? (ps2 != null ? ps2 : "> ")
                : (ps1 != null ? ps1 : "$ ");
            System.out.print(prompt);
            try {
                final String line = reader.readLine();
                if (line == null) {
                    running = false;
                } else if (needMore) {
                    buffer.append(line).append('\n');
                    if (!isIncomplete(buffer.toString())) {
                        needMore = false;
                        executeLine(executor, buffer.toString());
                        buffer.setLength(0);
                    }
                } else if (!line.trim().isEmpty()) {
                    if (isIncomplete(line)) {
                        needMore = true;
                        buffer.setLength(0);
                        buffer.append(line).append('\n');
                    } else {
                        executeLine(executor, line);
                    }
                }
            } catch (final IOException ex) {
                running = false;
            }
        }
        return 0;
    }

    /**
     * Executes a single command line in the interactive shell.
     * @param executor The shell executor
     * @param line The command line to execute
     */
    static void executeLine(final Executor executor, final String line) {
        try {
            final Lexer lexer = new Lexer(new CharReader(line));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            executor.execute(ast);
        } catch (final io.github.andreoss.sh4j.parser.ParseException ex) {
            System.err.println("sh: syntax error: " + ex.getMessage());
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
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            final StringBuilder content = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                content.append(line).append('\n');
                line = reader.readLine();
            }
            final Lexer lexer = new Lexer(new CharReader(content.toString()));
            final Parser parser = new Parser(lexer);
            final ListNode ast = parser.parse();
            executor.execute(ast);
        } catch (final IOException ex) {
            System.err.println("sh: " + envFile + ": cannot open");
        } catch (final io.github.andreoss.sh4j.parser.ParseException ex) {
            System.err.println("sh: syntax error: " + ex.getMessage());
        }
    }
}
