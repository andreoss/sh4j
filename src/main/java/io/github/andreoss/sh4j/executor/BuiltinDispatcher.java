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
        register("break", (self, args, env, assigns) -> { throw new Executor.BreakSignal(); });
        register("continue", (self, args, env, assigns) -> { throw new Executor.ContinueSignal(); });
        register("unset", (self, args, env, assigns) -> {
            for (String arg : args) self.environmentMap().remove(arg);
            return 0;
        });
        register("exit", (self, args, env, assigns) -> {
            if (args.isEmpty()) return 0;
            try {
                int code = Integer.parseInt(self.expandVariables(args.get(0), env));
                if (self.wordExpansionErrorOccurred()) {
                    self.clearWordExpansionError();
                    return 1;
                }
                return code;
            } catch (NumberFormatException e) {
                return 1;
            }
        });
        register("type", (self, args, env, assigns) -> {
            int ret = 0;
            for (String arg : args) {
                String expanded = self.expandVariables(arg, env);
                if (Executor.isBuiltin(expanded)) {
                    self.outStream().println(expanded + " is a shell built-in");
                } else if (self.functionsMap().containsKey(expanded)) {
                    self.outStream().println(expanded + " is a function");
                } else {
                    String resolved = self.resolveCommand(expanded);
                    if (resolved != null) {
                        self.outStream().println(expanded + " is " + resolved);
                    } else {
                        ret = 1;
                    }
                }
            }
            return ret;
        });
        register("shift", (self, args, env, assigns) -> {
            int n = 1;
            if (!args.isEmpty()) {
                try { n = Integer.parseInt(self.expandVariables(args.get(0), env)); }
                catch (NumberFormatException e) { return 1; }
            }
            if (n < 0) return 1;
            List<String> pos = self.positionalParamsList();
            int maxShift = pos.size() - 1;
            if (n == 0) return 0;
            if (n >= maxShift) {
                pos.subList(1, pos.size()).clear();
            } else {
                pos.subList(1, 1 + n).clear();
            }
            return 0;
        });
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
