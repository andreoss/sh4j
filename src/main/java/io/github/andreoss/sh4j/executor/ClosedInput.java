/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input of a descriptor closed by {@code <&-}: every read fails.
 *
 * @since 1.0
 */
final class ClosedInput extends InputStream {

    /** Message of a failed read or write. */
    static final String BAD = "Bad file descriptor";

    /**
     * Ctor.
     */
    private ClosedInput() {
        super();
    }

    @Override
    public int read() throws IOException {
        throw new IOException(ClosedInput.BAD);
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length)
        throws IOException {
        throw new IOException(ClosedInput.BAD);
    }

    /**
     * Makes an input whose reads fail.
     *
     * @return The input
     */
    static InputStream make() {
        return new ClosedInput();
    }
}
