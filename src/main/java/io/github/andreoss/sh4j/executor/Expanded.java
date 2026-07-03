/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.ArrayList;
import java.util.List;

/**
 * Text accumulated by one expansion, piece by piece.
 *
 * @since 1.0
 */
final class Expanded {

    /** Pieces appended so far, in order. */
    private final List<String> parts;

    Expanded() {
        this(new ArrayList<>(0));
    }

    private Expanded(final List<String> parts) {
        this.parts = parts;
    }

    @Override
    public String toString() {
        return String.join("", this.parts);
    }

    Expanded append(final char chr) {
        this.parts.add(Character.toString(chr));
        return this;
    }

    Expanded append(final CharSequence text) {
        this.parts.add(text.toString());
        return this;
    }
}
