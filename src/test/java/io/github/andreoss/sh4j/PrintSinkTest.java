/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PrintSink}.
 *
 * @since 1.0
 */
final class PrintSinkTest {

    @Test
    void writesTextAsBytes() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new PrintSink(out, StandardCharsets.UTF_8).print("abc");
        MatcherAssert.assertThat(
            out.toString(StandardCharsets.UTF_8),
            CoreMatchers.equalTo("abc")
        );
    }

    @Test
    void reportsFailedWrite() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new PrintSink(new PrintSinkTest.Broken(), StandardCharsets.UTF_8)
                .print("x")
        );
    }

    @Test
    void reportsFailedFlush() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new PrintSink(new PrintSinkTest.Broken(), StandardCharsets.UTF_8)
                .flush()
        );
    }

    /**
     * Output stream whose every operation fails.
     *
     * @since 1.0
     */
    private static final class Broken extends OutputStream {

        @Override
        public void write(final int data) throws IOException {
            throw new IOException("broken");
        }

        @Override
        public void flush() throws IOException {
            throw new IOException("broken");
        }
    }
}
