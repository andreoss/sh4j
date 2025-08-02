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

/**
 * Holds the executor's table of built-in commands and routes calls to the
 * matching {@link Builtin}. Every built-in registered here keeps its own
 * lambda body; the dispatcher itself stays generic.
 * @since 1.0
 */
final class BuiltinDispatcher {

    /** Default IFS value (space, tab, newline). */
    private static final String DEFAULT_IFS = " \t\n";
    /** {@code -} sentinel used by {@code cd}. */
    private static final String DASH = "-";

    /** Map from built-in name to its implementation. */
    private final Map<String, Builtin> registry;

    /**
     * Builds the dispatcher and registers every shell built-in.
     */
    BuiltinDispatcher() {
        super();
        this.registry = new HashMap<>();
        this.register("true", (self, args, env, assigns) -> 0);
        this.register("false", (self, args, env, assigns) -> 1);
        this.register(":", (self, args, env, assigns) -> 0);
        this.register("break", (self, args, env, assigns) -> {
            throw new Executor.BreakSignal();
        });
        this.register("continue", (self, args, env, assigns) -> {
            throw new Executor.ContinueSignal();
        });
        this.register("unset", BuiltinDispatcher::doUnset);
        this.register("export", BuiltinDispatcher::doExport);
        this.register("cd", BuiltinDispatcher::doCd);
        this.register("pwd", BuiltinDispatcher::doPwd);
        this.register("echo", BuiltinDispatcher::doEcho);
        this.register("eval", BuiltinDispatcher::doEval);
        this.register("exec", BuiltinDispatcher::doExec);
        this.register("exit", BuiltinDispatcher::doExit);
        this.register("type", BuiltinDispatcher::doType);
        this.register("command", BuiltinDispatcher::doCommand);
        this.register("shift", BuiltinDispatcher::doShift);
    }

    /**
     * Registers a built-in by name.
     * @param name    The command name (as written in source)
     * @param builtin The implementation
     */
    void register(final String name, final Builtin builtin) {
        this.registry.put(name, builtin);
    }

    /**
     * @param name A command name
     * @return True when a built-in is registered under {@code name}
     */
    boolean isBuiltin(final String name) {
        return this.registry.containsKey(name);
    }

    /**
     * Dispatches to the matching built-in, or returns null when no entry
     * exists (the executor then resolves an external command).
     * @param name        The command name
     * @param self        Narrow façade over the executor
     * @param arguments   Command arguments (excluding the name)
     * @param env         The live environment view
     * @param assignments Inline assignments attached to this call
     * @return The built-in's exit code, or null when not a built-in
     */
    Integer dispatch(
        final String name,
        final BuiltinContext self,
        final List<String> arguments,
        final Map<String, String> env,
        final List<String> assignments
    ) {
        final Builtin builtin = this.registry.get(name);
        final Integer code;
        if (builtin == null) {
            code = null;
        } else {
            code = builtin.invoke(self, arguments, env, assignments);
        }
        return code;
    }

    /**
     * Implementation of {@code unset NAME ...}.
     */
    private static int doUnset(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        for (final String arg : args) {
            self.environmentMap().remove(arg);
        }
        return 0;
    }

    /**
     * Implementation of {@code export NAME[=VALUE] ...}.
     */
    private static int doExport(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final Map<String, String> environment = self.environmentMap();
        if (!assigns.isEmpty()) {
            environment.putAll(env);
        }
        for (final String arg : args) {
            final int eqIdx = arg.indexOf('=');
            if (eqIdx >= 0) {
                environment.put(arg.substring(0, eqIdx), arg.substring(eqIdx + 1));
            } else {
                environment.putIfAbsent(arg, "");
            }
        }
        if (assigns.isEmpty() && args.isEmpty()) {
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
                self.outStream().println("export " + entry.getKey() + "=" + entry.getValue());
            }
        }
        return 0;
    }

    /**
     * Implementation of {@code cd [target]}.
     */
    private static int doCd(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final Map<String, String> environment = self.environmentMap();
        String target;
        int code = 0;
        if (args.isEmpty()) {
            target = environment.get("HOME");
            if (target == null) {
                code = 1;
            }
        } else {
            target = self.expandVariables(args.get(0), env);
            if (DASH.equals(target)) {
                final String old = environment.get("OLDPWD");
                if (old == null) {
                    code = 1;
                } else {
                    self.outStream().println(old);
                    target = old;
                }
            }
        }
        if (code == 0) {
            final File dir = new File(target);
            if (dir.isDirectory()) {
                code = applyChdir(environment, dir);
            } else {
                code = 1;
            }
        }
        return code;
    }

    /**
     * pwd built-in. POSIX 2.14: writes the current working directory.
     * {@code -L} (default) prefers the {@code PWD} env value; {@code -P}
     * resolves symlinks via {@link java.nio.file.Path#toRealPath}.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 on IO error
     */
    private static int doPwd(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        boolean physical = false;
        for (final String arg : args) {
            if ("-P".equals(arg)) {
                physical = true;
            } else if ("-L".equals(arg)) {
                physical = false;
            }
        }
        final Map<String, String> environment = self.environmentMap();
        final String logical = environment.getOrDefault(
            "PWD", System.getProperty("user.dir", "/")
        );
        int code = 0;
        if (physical) {
            try {
                self.outStream().println(
                    java.nio.file.Path.of(logical).toRealPath().toString()
                );
            } catch (final IOException ex) {
                code = 1;
            }
        } else {
            self.outStream().println(logical);
        }
        return code;
    }

    /**
     * Mutates PWD/OLDPWD and {@code user.dir} for a successful cd.
     * @param environment The live env map
     * @param dir         The directory being entered
     * @return The cd exit code (always 0 here)
     */
    private static int applyChdir(final Map<String, String> environment, final File dir) {
        String abs;
        try {
            abs = dir.getCanonicalPath();
        } catch (final IOException ex) {
            abs = dir.getAbsolutePath();
        }
        final String oldPwd = environment.get("PWD");
        if (oldPwd != null) {
            environment.put("OLDPWD", oldPwd);
        }
        environment.put("PWD", abs);
        System.setProperty("user.dir", abs);
        return 0;
    }

    /**
     * Implementation of {@code echo [-n] ARG ...}.
     */
    private static int doEcho(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        boolean newline = true;
        int start = 0;
        if (!args.isEmpty() && "-n".equals(args.get(0))) {
            newline = false;
            start = 1;
        }
        boolean firstField = true;
        boolean error = false;
        for (int idx = start; idx < args.size(); idx = idx + 1) {
            final String arg = args.get(idx);
            final WordExpander.ExpansionResult res = WordExpander.expandVariables(arg, env, self);
            error = error || res.error();
            final String expanded = res.value();
            if (WordExpander.isQuoted(arg)) {
                if (!firstField) {
                    self.outStream().print(" ");
                }
                self.outStream().print(WordExpander.removeQuotes(expanded));
                firstField = false;
            } else {
                final String ifsVal = env.getOrDefault("IFS", DEFAULT_IFS);
                final List<String> fields = WordExpander.pathnameExpand(
                    WordExpander.splitFields(expanded, ifsVal)
                );
                for (final String field : fields) {
                    if (!firstField) {
                        self.outStream().print(" ");
                    }
                    self.outStream().print(WordExpander.removeQuotes(field));
                    firstField = false;
                }
            }
        }
        if (newline) {
            self.outStream().println();
        }
        return error ? 1 : 0;
    }

    /**
     * Implementation of {@code eval STRING ...}.
     */
    private static int doEval(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final String input = String.join(" ", args);
        final int code;
        if (input.isEmpty()) {
            code = 0;
        } else {
            final ListNode ast = new Parser(new Lexer(new CharReader(input))).parse();
            code = self.execute(ast);
        }
        return code;
    }

    /**
     * Implementation of {@code exec COMMAND ARG ...}.
     */
    private static int doExec(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code;
        if (args.isEmpty()) {
            code = 0;
        } else {
            final String cmdName = self.expandVariables(args.get(0), env);
            final String resolved = self.resolveCommand(cmdName);
            if (resolved == null) {
                code = Executor.EXIT_COMMAND_NOT_FOUND;
            } else {
                code = runExec(self, args, env, resolved);
            }
        }
        return code;
    }

    /**
     * Forks the external command and waits for it.
     * @param self     Façade for env / expansion / IO-exception mapping
     * @param args     The original args (with {@code args[0]} the command name)
     * @param env      Live environment
     * @param resolved Absolute path of the resolved command
     * @return The child's exit code, or a POSIX mapping of an IO failure
     */
    private static int runExec(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final String resolved
    ) {
        final List<String> cmdArgs = new ArrayList<>(args.size());
        cmdArgs.add(resolved);
        for (int idx = 1; idx < args.size(); idx = idx + 1) {
            final String expanded = self.expandVariables(args.get(idx), env);
            cmdArgs.add(WordExpander.removeQuotes(expanded));
        }
        final ProcessBuilder builder = new ProcessBuilder(cmdArgs);
        builder.environment().putAll(env);
        builder.directory(new File(System.getProperty("user.dir")));
        int code;
        try {
            final Process process = builder.start();
            code = process.waitFor();
        } catch (final IOException ex) {
            code = self.exitCodeForIoException(ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            code = 1;
        }
        return code;
    }

    /**
     * Implementation of {@code exit [STATUS]}.
     */
    private static int doExit(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        if (!args.isEmpty()) {
            final WordExpander.ExpansionResult res = WordExpander.expandVariables(
                args.get(0), env, self
            );
            if (res.error()) {
                code = 1;
            } else {
                try {
                    code = Integer.parseInt(res.value());
                } catch (final NumberFormatException ex) {
                    code = 1;
                }
            }
        }
        return code;
    }

    /**
     * Implementation of {@code type NAME ...}.
     */
    private static int doType(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        for (final String arg : args) {
            final String expanded = self.expandVariables(arg, env);
            if (Executor.isBuiltin(expanded)) {
                self.outStream().println(expanded + " is a shell built-in");
            } else if (self.functionsMap().containsKey(expanded)) {
                self.outStream().println(expanded + " is a function");
            } else {
                final String resolved = self.resolveCommand(expanded);
                if (resolved == null) {
                    code = 1;
                } else {
                    self.outStream().println(expanded + " is " + resolved);
                }
            }
        }
        return code;
    }

    /**
     * Implementation of {@code command [-v|-V|-p] name [args...]}.
     * POSIX 2.14. {@code -v} prints location of NAME; {@code -V}
     * prints a verbose description; without flags, invokes NAME
     * bypassing function lookup. {@code -p} (default-PATH) is
     * accepted but treated as a no-op (the configured PATH is used).
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return Resolved command exit code (1 if NAME not found with -v/-V)
     */
    private static int doCommand(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        int idx = 0;
        boolean verbose = false;
        boolean locate = false;
        while (idx < args.size() && args.get(idx).startsWith("-")) {
            final String flag = args.get(idx);
            if ("-v".equals(flag)) {
                locate = true;
            } else if ("-V".equals(flag)) {
                verbose = true;
            }
            idx += 1;
        }
        if (idx >= args.size()) {
            code = locate || verbose ? 1 : 0;
        } else {
            final String name = self.expandVariables(args.get(idx), env);
            if (locate || verbose) {
                code = describeCommand(self, name, verbose);
            } else {
                final StringBuilder line = new StringBuilder(name);
                for (int i = idx + 1; i < args.size(); i += 1) {
                    line.append(' ').append(args.get(i));
                }
                final ListNode ast = new Parser(
                    new Lexer(new CharReader(line.toString()))
                ).parse();
                code = self.execute(ast);
            }
        }
        return code;
    }

    /**
     * Helper for {@code command -v} / {@code command -V}.
     * @param self    Builtin context
     * @param name    Command name
     * @param verbose true for -V (descriptive), false for -v (location)
     * @return 0 if found, 1 if not
     */
    private static int describeCommand(
        final BuiltinContext self, final String name, final boolean verbose
    ) {
        int code = 0;
        if (Executor.isBuiltin(name)) {
            if (verbose) {
                self.outStream().println(name + " is a shell built-in");
            } else {
                self.outStream().println(name);
            }
        } else if (self.functionsMap().containsKey(name)) {
            if (verbose) {
                self.outStream().println(name + " is a function");
            } else {
                self.outStream().println(name);
            }
        } else {
            final String resolved = self.resolveCommand(name);
            if (resolved == null) {
                code = 1;
            } else if (verbose) {
                self.outStream().println(name + " is " + resolved);
            } else {
                self.outStream().println(resolved);
            }
        }
        return code;
    }

    /**
     * Implementation of {@code shift [N]}.
     */
    private static int doShift(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int shifts = 1;
        int code = 0;
        if (!args.isEmpty()) {
            try {
                shifts = Integer.parseInt(self.expandVariables(args.get(0), env));
            } catch (final NumberFormatException ex) {
                code = 1;
            }
        }
        if (code == 0 && shifts < 0) {
            code = 1;
        }
        if (code == 0 && shifts > 0) {
            final List<String> pos = self.positionalParamsList();
            final int maxShift = pos.size() - 1;
            if (shifts >= maxShift) {
                pos.subList(1, pos.size()).clear();
            } else {
                pos.subList(1, 1 + shifts).clear();
            }
        }
        return code;
    }
}
