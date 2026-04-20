/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.executor.Executor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The read–eval–print loop used by interactive mode.
 *
 * @since 1.0
 */
final class Loop {

    /** Executor used by the loop. */
    private final Executor executor;

    /** Reader for user input. */
    private final BufferedReader reader;

    /** True while the loop should keep running. */
    private boolean ongoing;

    /**
     * Creates the loop.
     *
     * @param executor Shell executor
     */
    Loop(final Executor executor) {
        this(executor, Loop.console());
    }

    /**
     * Creates the loop over a given input.
     *
     * @param executor Shell executor
     * @param reader Reader for user input
     */
    private Loop(final Executor executor, final BufferedReader reader) {
        this.executor = executor;
        this.reader = reader;
    }

    /** Runs until EOF or an IO failure. */
    void run() {
        final StringBuilder buffer = new StringBuilder();
        boolean running = true;
        while (running) {
            MainRepl.out().print(this.prompt());
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

    private void handle(final String line, final StringBuilder buffer) {
        if (this.ongoing) {
            buffer.append(line).append('\n');
            if (!MainRepl.isIncomplete(buffer.toString())) {
                this.ongoing = false;
                MainRepl.executeLine(this.executor, buffer.toString());
                buffer.setLength(0);
            }
        } else if (!line.isBlank()) {
            if (MainRepl.isIncomplete(line)) {
                this.ongoing = true;
                buffer.setLength(0);
                buffer.append(line).append('\n');
            } else {
                MainRepl.executeLine(this.executor, line);
            }
        }
    }

    private String prompt() {
        final String first = this.executor.getVariable("PS1");
        final String next = this.executor.getVariable("PS2");
        final String prompt;
        if (this.ongoing) {
            if (next == null) {
                prompt = "> ";
            } else {
                prompt = next;
            }
        } else if (first == null) {
            prompt = "$ ";
        } else {
            prompt = first;
        }
        return prompt;
    }

    private static BufferedReader console() {
        return new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8)
        );
    }
}
