/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import io.github.andreoss.sh4j.PrintSink;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Descriptors bound by a redirection-only {@code exec}, with the flag that
 * tells whether the live stdin was borrowed from one of them.
 *
 * @since 1.0
 */
final class Descriptors {

    /** Input streams bound to descriptors. */
    private final Map<Integer, InputStream> inputs;

    /** Output streams bound to descriptors. */
    private final Map<Integer, PrintSink> outputs;

    /** True when the current input swap borrowed a descriptor stream. */
    private boolean borrowed;

    /**
     * Ctor.
     */
    Descriptors() {
        this(new HashMap<>(), new HashMap<>());
    }

    private Descriptors(
        final Map<Integer, InputStream> inputs,
        final Map<Integer, PrintSink> outputs
    ) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Closes a stream, ignoring a failure.
     *
     * @param stream The stream to close
     * @return True when the stream closed without a failure
     */
    static boolean close(final InputStream stream) {
        boolean closed = true;
        try {
            stream.close();
        } catch (final IOException ex) {
            closed = false;
        }
        return closed;
    }

    /**
     * Copies the descriptors of a parent shell into this subshell.
     *
     * @param parent The descriptors of the parent shell
     */
    void inherit(final Descriptors parent) {
        this.outputs.putAll(parent.outputs);
        parent.inputs.forEach(
            (desc, stream) -> this.inputs.putAll(Map.of(desc, SharedInput.make(stream)))
        );
    }

    /**
     * Returns the input streams bound to descriptors.
     *
     * @return The live map from descriptor to input stream
     */
    Map<Integer, InputStream> inputs() {
        return this.inputs;
    }

    /**
     * Returns the output streams bound to descriptors.
     *
     * @return The live map from descriptor to output sink
     */
    Map<Integer, PrintSink> outputs() {
        return this.outputs;
    }

    /**
     * Tells whether the live stdin was borrowed from a descriptor.
     *
     * @return True when the live stdin is a borrowed descriptor stream
     */
    boolean borrowed() {
        return this.borrowed;
    }

    /**
     * Records whether the live stdin was borrowed from a descriptor.
     *
     * @param flag True when the live stdin is a borrowed descriptor stream
     */
    void borrow(final boolean flag) {
        this.borrowed = flag;
    }
}
