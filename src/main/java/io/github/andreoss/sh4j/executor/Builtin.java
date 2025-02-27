/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface Builtin {
    int invoke(Executor self, List<String> arguments, Map<String, String> env, List<String> assignments);
}
