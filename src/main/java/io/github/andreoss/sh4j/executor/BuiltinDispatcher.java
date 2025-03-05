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
        register("export", (self, args, env, assigns) -> {
            Map<String, String> environment = self.environmentMap();
            if (!assigns.isEmpty()) {
                environment.putAll(env);
            }
            for (String arg : args) {
                int eqIdx = arg.indexOf('=');
                if (eqIdx >= 0) {
                    environment.put(arg.substring(0, eqIdx), arg.substring(eqIdx + 1));
                } else {
                    environment.putIfAbsent(arg, "");
                }
            }
            if (assigns.isEmpty() && args.isEmpty()) {
                for (Map.Entry<String, String> e : environment.entrySet()) {
                    self.outStream().println("export " + e.getKey() + "=" + e.getValue());
                }
            }
            return 0;
        });
        register("cd", (self, args, env, assigns) -> {
            Map<String, String> environment = self.environmentMap();
            String target;
            if (args.isEmpty()) {
                target = environment.get("HOME");
                if (target == null) return 1;
            } else {
                target = self.expandVariables(args.get(0), env);
                if ("-".equals(target)) {
                    String old = environment.get("OLDPWD");
                    if (old == null) return 1;
                    self.outStream().println(old);
                    target = old;
                }
            }
            java.io.File dir = new java.io.File(target);
            if (!dir.isDirectory()) return 1;
            String absPath;
            try { absPath = dir.getCanonicalPath(); }
            catch (java.io.IOException e) { absPath = dir.getAbsolutePath(); }
            String oldPwd = environment.get("PWD");
            if (oldPwd != null) environment.put("OLDPWD", oldPwd);
            environment.put("PWD", absPath);
            System.setProperty("user.dir", absPath);
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
