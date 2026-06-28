/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output of a descriptor closed by {@code >&-}.
 *
 * @since 1.0
 */
final class ClosedOutput extends OutputStream {

    /** True when writes fail rather than vanish. */
    private final boolean strict;

    /**
     * Ctor.
     *
     * @param strict True when writes fail rather than vanish
     */
    private ClosedOutput(final boolean strict) {
        super();
        this.strict = strict;
    }

    @Override
    public void write(final int data) throws IOException {
        if (this.strict) {
            throw new IOException(ClosedInput.BAD);
        }
    }

    /**
     * Makes an output whose writes fail.
     *
     * @return The output
     */
    static OutputStream failing() {
        return new ClosedOutput(true);
    }

    /**
     * Makes an output whose writes are lost.
     *
     * @return The output
     */
    static OutputStream dropping() {
        return new ClosedOutput(false);
    }
}
