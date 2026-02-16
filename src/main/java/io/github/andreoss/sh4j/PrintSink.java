/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * A non-closeable text sink for redirected shell output.
 *
 * <p>A shell keeps a redirection target open for the lifetime of a descriptor,
 * so those streams cannot be bound to a try-with-resources block. This type
 * exposes only the print operations the interpreter needs and never claims
 * ownership of the bytes travelling through it.</p>
 *
 * @since 1.0
 */
public final class PrintSink {

    /** Underlying byte sink. */
    private final OutputStream target;

    /** Character encoding used for text. */
    private final Charset encoding;

    /**
     * Creates a writer.
     *
     * @param sink The byte destination
     * @param charset The character encoding
     */
    public PrintSink(final OutputStream sink, final Charset charset) {
        this.target = sink;
        this.encoding = charset;
    }

    /**
     * Writes text followed by a line separator.
     *
     * @param text The text to print
     */
    public void println(final String text) {
        this.print(text);
        this.print(System.lineSeparator());
    }

    /** Writes a line separator. */
    public void println() {
        this.print(System.lineSeparator());
    }

    /**
     * Writes text.
     *
     * @param text The text to print
     */
    public void print(final String text) {
        final byte[] encoded = Objects.requireNonNull(text)
            .getBytes(this.encoding);
        this.write(encoded);
    }

    /** Flushes buffered bytes. */
    public void flush() {
        this.write(new byte[0]);
    }

    @GeneratedDefensive
    private void write(final byte[] encoded) {
        try {
            if (encoded.length > 0) {
                this.target.write(encoded);
            }
            this.target.flush();
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Exposes the byte destination for raw copying.
     *
     * @return The stream that receives everything this writer prints
     */
    public OutputStream bytes() {
        return this.target;
    }
}
