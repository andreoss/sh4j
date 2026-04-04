/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import io.github.andreoss.sh4j.parser.Node;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the commands of a multi-command pipeline concurrently, connected by
 * in-memory pipes.
 *
 * @since 1.0
 */
final class PipeChain {

    /** Capacity of each pipe in bytes. */
    private static final int CAPACITY = 65_536;

    /** The shell that runs the pipeline. */
    private final Executor shell;

    /** The commands of the pipeline. */
    private final List<Node> commands;

    /**
     * Ctor.
     *
     * @param shell The shell that runs the pipeline
     * @param commands The commands of the pipeline
     */
    PipeChain(final Executor shell, final List<Node> commands) {
        this.shell = shell;
        this.commands = commands;
    }

    /**
     * Runs the pipeline.
     *
     * @return The exit status of the last command
     */
    int run() {
        final int count = this.commands.size();
        final PipedInputStream[] inputs = new PipedInputStream[count];
        final PipedOutputStream[] outputs = new PipedOutputStream[count];
        int code;
        try {
            for (int idx = 0; idx < count - 1; idx = idx + 1) {
                inputs[idx + 1] = PipeChain.reader();
                outputs[idx] = PipeChain.writer(inputs, idx + 1);
            }
            code = this.start(inputs, outputs);
        } catch (final IOException ex) {
            code = 1;
        }
        return code;
    }

    private int start(
        final PipedInputStream[] inputs, final PipedOutputStream[] outputs
    ) {
        final int count = this.commands.size();
        final List<PipeStage> stages = new ArrayList<>(count);
        final List<Thread> threads = new ArrayList<>(count);
        for (int idx = 0; idx < count; idx = idx + 1) {
            final PipeStage stage = this.stage(idx, inputs, outputs);
            stages.add(stage);
            threads.add(new Thread(stage, String.format("sh-pipe-%d", idx)));
        }
        for (final Thread thread : threads) {
            thread.start();
        }
        int code;
        try {
            for (final Thread thread : threads) {
                thread.join();
            }
            code = stages.get(count - 1).code();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            code = 1;
        }
        return code;
    }

    private PipeStage stage(
        final int idx, final PipedInputStream[] inputs,
        final PipedOutputStream[] outputs
    ) {
        final Executor child = this.shell.subshell();
        if (idx == 0) {
            child.setInputStream(this.shell.inputStream());
        } else {
            child.setInputStream(inputs[idx]);
        }
        final PipeStage stage;
        if (idx == this.commands.size() - 1) {
            child.setOutputStream(this.shell.outStream());
            stage = new PipeStage(
                child, this.commands.get(idx), OutputStream.nullOutputStream()
            );
        } else {
            child.setOutputStream(new PrintSink(outputs[idx], StandardCharsets.UTF_8));
            stage = new PipeStage(child, this.commands.get(idx), outputs[idx]);
        }
        child.setErrorStream(this.shell.errStream());
        return stage;
    }

    private static PipedInputStream reader() {
        return new PipedInputStream(PipeChain.CAPACITY);
    }

    private static PipedOutputStream writer(
        final PipedInputStream[] inputs, final int idx
    ) throws IOException {
        return new PipedOutputStream(inputs[idx]);
    }
}
