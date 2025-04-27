/*
 * SPDX-FileCopyrightText: Copyright (c) 2024-2026
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package io.github.andreoss.sh4j.executor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.andreoss.sh4j.lexer.CharReader;
import io.github.andreoss.sh4j.lexer.Lexer;
import io.github.andreoss.sh4j.parser.ListNode;
import io.github.andreoss.sh4j.parser.Parser;

final class BuiltinDispatcher {
    private final Map<String, Builtin> registry = new HashMap<>();

    BuiltinDispatcher() {
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
        register("echo", (self, args, env, assigns) -> {
            boolean newline = true;
            int start = 0;
            if (!args.isEmpty() && "-n".equals(args.get(0))) {
                newline = false;
                start = 1;
            }
            boolean firstField = true;
            boolean error = false;
            for (int i = start; i < args.size(); i++) {
                String arg = args.get(i);
                WordExpander.ExpansionResult r = WordExpander.expandVariables(arg, env, self);
                error |= r.error();
                String expanded = r.value();
                if (WordExpander.isQuoted(arg)) {
                    if (!firstField) self.outStream().print(" ");
                    self.outStream().print(WordExpander.removeQuotes(expanded));
                    firstField = false;
                } else {
                    String ifsVal = env.getOrDefault("IFS", " \t\n");
                    List<String> fields = WordExpander.splitFields(expanded, ifsVal);
                    fields = WordExpander.pathnameExpand(fields);
                    for (String field : fields) {
                        if (!firstField) self.outStream().print(" ");
                        self.outStream().print(WordExpander.removeQuotes(field));
                        firstField = false;
                    }
                }
            }
            if (newline) self.outStream().println();
            return error ? 1 : 0;
        });
        register("eval", (self, args, env, assigns) -> {
            String input = String.join(" ", args);
            if (input.isEmpty()) return 0;
            ListNode ast = new Parser(new Lexer(new CharReader(input))).parse();
            return self.execute(ast);
        });
        register("exec", (self, args, env, assigns) -> {
            if (args.isEmpty()) return 0;
            String cmdName = self.expandVariables(args.get(0), env);
            String resolved = self.resolveCommand(cmdName);
            if (resolved == null) return Executor.EXIT_COMMAND_NOT_FOUND;
            List<String> cmdArgs = new ArrayList<>();
            cmdArgs.add(resolved);
            for (int i = 1; i < args.size(); i++) {
                String expanded = self.expandVariables(args.get(i), env);
                cmdArgs.add(WordExpander.removeQuotes(expanded));
            }
            ProcessBuilder pb = new ProcessBuilder(cmdArgs);
            pb.environment().putAll(env);
            pb.directory(new File(System.getProperty("user.dir")));
            try {
                Process process = pb.start();
                return process.waitFor();
            } catch (IOException e) {
                return self.exitCodeForIoException(e);
            } catch (InterruptedException e) {
                return 1;
            }
        });
        register("exit", (self, args, env, assigns) -> {
            if (args.isEmpty()) return 0;
            WordExpander.ExpansionResult r = WordExpander.expandVariables(args.get(0), env, self);
            if (r.error()) return 1;
            try {
                return Integer.parseInt(r.value());
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

    void register(String name, Builtin builtin) {
        registry.put(name, builtin);
    }

    boolean isBuiltin(String name) {
        return registry.containsKey(name);
    }

    Integer dispatch(String name, BuiltinContext self, List<String> arguments,
                            Map<String, String> env, List<String> assignments) {
        Builtin b = registry.get(name);
        if (b == null) return null;
        return b.invoke(self, arguments, env, assignments);
    }
}
