/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

/**
 * A parameter named in a braced expansion and the value it holds, null when
 * unset.
 *
 * @param name The parameter name
 * @param value The value, null when the parameter is unset
 * @since 1.0
 */
record Parameter(String name, String value) {

    boolean unset() {
        return this.value == null;
    }

    boolean blank() {
        return this.value == null || this.value.isEmpty();
    }
}
