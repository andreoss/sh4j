/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import io.github.andreoss.sh4j.executor.Executor;
import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.Parser;

import static java.lang.System.err;

/**
 * Script execution and source reading for {@link Main}.
 * @since 1.0
 */
final class MainRunner {

    /**
     * Utility class; no instances.
     */
    private MainRunner() {
        super();
    }

    /**
     * Parses and executes shell source text.
     * @param source The shell source
     * @param params Positional parameters ({@code $0} first)
     * @return Exit status
     */
    static int runSource(final String source, final List<String> params) {
        int code = 0;
        final Executor executor = new Executor(System.getenv(), params);
        try {
            code = executor.execute(
                new Parser(new Lexer(new CharReader(source))).parse()
            );
        } catch (final io.github.andreoss.sh4j.parser.ParseException ex) {
            err.println("sh: syntax error: " + ex.getMessage());
            code = 2;
        } finally {
            executor.runExitTrap(code);
        }
        return code;
    }

    /**
     * Reads all bytes from a stream as UTF-8 text.
     * @param stream Input stream
     * @return Decoded text
     */
    static String readAll(final InputStream stream) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final String text;
        try {
            int read = stream.read();
            while (read != -1) {
                buffer.write(read);
                read = stream.read();
            }
            text = buffer.toString(StandardCharsets.UTF_8);
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
        return MainRunner.runScript(filename, Collections.singletonList(filename));
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
        try (
            BufferedReader reader = Files.newBufferedReader(
                Paths.get(filename), StandardCharsets.UTF_8
            )
        ) {
            String line = reader.readLine();
            while (line != null) {
                content.append(line).append('\n');
                line = reader.readLine();
            }
            code = MainRunner.runSource(content.toString(), scriptArgs);
        } catch (final IOException ex) {
            err.println("sh: " + filename + ": cannot open");
            code = 1;
        }
        return code;
    }
}
