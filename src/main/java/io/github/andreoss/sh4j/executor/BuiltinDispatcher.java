/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BuiltinDispatcher {
    private final Map<String, Builtin> registry = new HashMap<>();

    public BuiltinDispatcher() {
        register("true", (self, args, env, assigns) -> 0);
        register("false", (self, args, env, assigns) -> 1);
        register(":", (self, args, env, assigns) -> 0);
    }

    public void register(String name, Builtin builtin) {
        registry.put(name, builtin);
    }

    public boolean isBuiltin(String name) {
        return registry.containsKey(name);
    }

    public Integer dispatch(String name, Executor self, List<String> arguments,
                            Map<String, String> env, List<String> assignments) {
        Builtin b = registry.get(name);
        if (b == null) return null;
        return b.invoke(self, arguments, env, assignments);
    }
}
