/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Input descriptor a subshell inherits from its parent: reads share the
 * offset of the parent stream, and closing it detaches the subshell
 * without closing the stream of the parent.
 *
 * @since 1.0
 */
final class SharedInput extends FilterInputStream {

    /**
     * Ctor.
     *
     * @param origin The input stream of the parent shell
     */
    private SharedInput(final InputStream origin) {
        super(origin);
    }

    @Override
    public void close() {
        this.in = InputStream.nullInputStream();
    }

    /**
     * Makes an input that shares a stream of the parent shell.
     *
     * @param origin The input stream of the parent shell
     * @return The input
     */
    static InputStream make(final InputStream origin) {
        return new SharedInput(origin);
    }
}
