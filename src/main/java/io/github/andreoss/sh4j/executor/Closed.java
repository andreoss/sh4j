/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.annotation.GeneratedDefensive;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Streams that stand for a standard descriptor closed by {@code >&-} or
 * {@code <&-}.
 *
 * @since 1.0
 */
final class Closed {

    /** File a child process gets for a closed output: every write fails. */
    static final File SINK = new File("/dev/full");

    /** File a child process gets for a closed input: every read fails. */
    static final File SOURCE = new File("/proc/self/mem");

    @GeneratedDefensive
    private Closed() {
    }

    /**
     * Makes a stdout whose writes fail.
     *
     * @return The closed stdout
     */
    static PrintSink output() {
        return new PrintSink(ClosedOutput.failing(), StandardCharsets.UTF_8);
    }

    /**
     * Makes a stderr whose writes are lost, so shell diagnostics vanish.
     *
     * @return The closed stderr
     */
    static PrintSink error() {
        return new PrintSink(ClosedOutput.dropping(), StandardCharsets.UTF_8);
    }

    /**
     * Makes a stdin whose reads fail.
     *
     * @return The closed stdin
     */
    static InputStream input() {
        return ClosedInput.make();
    }

    /**
     * Tells whether a sink stands for a closed descriptor.
     *
     * @param sink The sink
     * @return True when it was made by this class
     */
    static boolean closed(final PrintSink sink) {
        return sink.bytes() instanceof ClosedOutput;
    }

    /**
     * Tells whether a stream stands for a closed stdin.
     *
     * @param stream The stream
     * @return True when it was made by this class
     */
    static boolean closed(final InputStream stream) {
        return stream instanceof ClosedInput;
    }

    /**
     * Points the piped standard streams of a child process at files that
     * fail like the closed descriptors of the shell.
     *
     * @param builder The process builder to mutate
     * @param shell The shell that launches the process
     */
    static void attach(final ProcessBuilder builder, final Executor shell) {
        if (Closed.piped(builder.redirectOutput()) && Closed.closed(shell.outStream())) {
            builder.redirectOutput(Closed.SINK);
        }
        if (Closed.piped(builder.redirectError()) && Closed.closed(shell.errStream())) {
            builder.redirectError(Closed.SINK);
        }
        if (Closed.piped(builder.redirectInput()) && Closed.closed(shell.inputStream())) {
            builder.redirectInput(Closed.SOURCE);
        }
    }

    private static boolean piped(final ProcessBuilder.Redirect redirect) {
        return ProcessBuilder.Redirect.PIPE.equals(redirect);
    }
}
