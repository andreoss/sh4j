/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Reads one newline-terminated line from a byte stream.
 *
 * @since 1.0
 */
final class InputLine {

    @GeneratedDefensive
    private InputLine() {
    }

    /**
     * Reads one line with its terminating newline, if any.
     *
     * @param input The stream to read from
     * @return The line, without a newline when end of input came first,
     *  or null when nothing was read or on a read failure
     */
    static String read(final InputStream input) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Optional<String> line;
        try {
            int read = input.read();
            while (read != -1 && read != '\n') {
                buffer.write(read);
                read = input.read();
            }
            if (read == '\n') {
                buffer.write(read);
            }
            line = InputLine.text(buffer);
        } catch (final IOException ex) {
            line = Optional.empty();
        }
        return line.orElse(null);
    }

    private static Optional<String> text(final ByteArrayOutputStream buffer) {
        Optional<String> line = Optional.empty();
        if (buffer.size() > 0) {
            line = Optional.of(buffer.toString(StandardCharsets.UTF_8));
        }
        return line;
    }
}
