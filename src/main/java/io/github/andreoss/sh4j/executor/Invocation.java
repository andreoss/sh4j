/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import java.util.Map;

/**
 * One built-in call: its arguments, the live environment view, and the
 * inline assignments attached to the call.
 *
 * @param arguments Command arguments (excluding the built-in name)
 * @param env The live environment view, which built-ins may mutate
 * @param assignments Inline {@code KEY=VALUE} pairs attached to this call
 * @since 1.0
 */
record Invocation(
    List<String> arguments, Map<String, String> env, List<String> assignments
) {
}
