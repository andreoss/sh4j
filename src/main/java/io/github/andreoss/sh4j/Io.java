/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Factories for the byte, character and line sources the shell consumes.
 *
 * <p>Each factory hands ownership of the created stream to its caller, so a
 * resource's lifetime is stated where it is opened rather than implied by an
 * enclosing block.</p>
 *
 * @since 1.0
 */
public final class Io {

    private Io() {
    }

    /**
     * Opens a file for reading.
     *
     * @param file The file to open
     * @return A stream over the file contents
     * @throws IOException When the file cannot be opened
     */
    public static InputStream input(final File file) throws IOException {
        return new FileInputStream(file);
    }

    /**
     * Opens a file for writing, optionally in append mode.
     *
     * @param file The target file
     * @param append Whether existing content is preserved
     * @return A stream into the file
     * @throws IOException When the file cannot be opened
     */
    public static OutputStream output(final File file, final boolean append)
        throws IOException {
        return new FileOutputStream(file, append);
    }

    /**
     * Wraps a byte sink in a UTF-8 print stream.
     *
     * @param sink The destination
     * @param autoflush Whether each write flushes
     * @return The print stream
     */
    public static PrintStream printer(
        final ByteArrayOutputStream sink, final boolean autoflush
    ) {
        return Io.printer(sink, StandardCharsets.UTF_8, autoflush);
    }

    /**
     * Wraps a byte sink in a print stream over the given charset.
     *
     * @param sink The destination
     * @param encoding The character encoding
     * @param autoflush Whether each write flushes
     * @return The print stream
     */
    public static PrintStream printer(
        final OutputStream sink, final Charset encoding, final boolean autoflush
    ) {
        return new PrintStream(sink, autoflush, encoding);
    }

    /**
     * Creates a reader over a string.
     *
     * @param text The text to read
     * @return A character reader
     */
    public static Reader text(final String text) {
        return new StringReader(text);
    }

    /**
     * Buffers a character reader for line-oriented use.
     *
     * @param source The underlying reader
     * @return A buffered reader
     */
    public static BufferedReader buffered(final Reader source) {
        return new BufferedReader(source);
    }

    /**
     * Decodes a byte stream into characters.
     *
     * @param source The byte stream
     * @param encoding The character encoding
     * @return A character reader
     */
    public static Reader decoded(final InputStream source, final Charset encoding) {
        return new InputStreamReader(source, encoding);
    }

    /**
     * Creates a reader over standard input.
     *
     * @return A reader on {@code System.in}
     */
    public static BufferedReader standardInput() {
        return Io.buffered(
            Io.decoded(System.in, StandardCharsets.UTF_8)
        );
    }

    /**
     * Creates an in-memory stream over already-decoded text.
     *
     * @param body The text to expose
     * @return A stream over the encoded bytes
     */
    public static InputStream memory(final String body) {
        return new ByteArrayInputStream(
            body.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Opens a file for writing through the NIO provider.
     *
     * @param path Where to write
     * @param append Whether existing content is preserved
     * @return A stream into the file
     * @throws IOException When the file cannot be opened
     */
    public static OutputStream appending(final Path path, final boolean append)
        throws IOException {
        final OpenOption[] options;
        if (append) {
            options = new OpenOption[]{
                StandardOpenOption.CREATE, StandardOpenOption.APPEND
            };
        } else {
            options = new OpenOption[]{
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            };
        }
        return Files.newOutputStream(path, options);
    }
}
