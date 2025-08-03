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
        this.register("wait", BuiltinDispatcher::doWait);
        this.register("hash", BuiltinDispatcher::doHash);
        this.register("kill", BuiltinDispatcher::doKill);
        this.register("umask", BuiltinDispatcher::doUmask);
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
     * Implementation of {@code umask [-S] [mode]}.
     * POSIX 2.14. JDK has no portable umask call, so the value is
     * tracked in the environment under the {@code SH4J_UMASK} key but
     * is NOT actually enforced on file creations (documented stub).
     * Without args, prints the current mask. With a mode, sets it
     * (octal only — symbolic mode is rejected with exit 1). {@code -S}
     * formats the mask symbolically (POSIX 2.14 "Symbolic Output").
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 on parse error
     */
    private static int doUmask(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        final Map<String, String> environment = self.environmentMap();
        final String current = environment.getOrDefault("SH4J_UMASK", "0022");
        int code = 0;
        boolean symbolic = false;
        int idx = 0;
        if (!args.isEmpty() && "-S".equals(args.get(0))) {
            symbolic = true;
            idx = 1;
        }
        if (idx >= args.size()) {
            if (symbolic) {
                self.outStream().println(umaskSymbolic(current));
            } else {
                self.outStream().println(current);
            }
        } else {
            final String mode = self.expandVariables(args.get(idx), env);
            try {
                final int value = Integer.parseInt(mode, 8);
                environment.put("SH4J_UMASK", String.format("%04o", value));
            } catch (final NumberFormatException ex) {
                code = 1;
            }
        }
        return code;
    }

    /**
     * Formats an octal umask string as POSIX symbolic mode.
     * @param mask Octal 4-digit umask (e.g. "0022")
     * @return Symbolic representation (e.g. "u=rwx,g=rx,o=rx")
     */
    private static String umaskSymbolic(final String mask) {
        final int value;
        try {
            value = Integer.parseInt(mask, 8);
        } catch (final NumberFormatException ex) {
            return "u=,g=,o=";
        }
        final int allBits = 7;
        final int user = allBits & ~((value >> 6) & allBits);
        final int group = allBits & ~((value >> 3) & allBits);
        final int other = allBits & ~(value & allBits);
        return String.format(
            "u=%s,g=%s,o=%s",
            umaskBitsToString(user), umaskBitsToString(group), umaskBitsToString(other)
        );
    }

    /**
     * Maps a 3-bit permission triple to {@code rwx} chars.
     * @param bits 0..7
     * @return Permission string
     */
    private static String umaskBitsToString(final int bits) {
        final StringBuilder out = new StringBuilder(3);
        final int rbit = 4;
        final int wbit = 2;
        final int xbit = 1;
        if ((bits & rbit) != 0) {
            out.append('r');
        }
        if ((bits & wbit) != 0) {
            out.append('w');
        }
        if ((bits & xbit) != 0) {
            out.append('x');
        }
        return out.toString();
    }

    /**
     * Implementation of {@code kill [-s SIG | -SIG] PID... | -l}.
     * POSIX 2.14. JDK constraint: ProcessHandle only supports TERM
     * (destroy) and KILL (destroyForcibly); other signals are
     * accepted at the command line but mapped to TERM (which is the
     * POSIX default) and a soft warning is not emitted.
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 1 on usage error, 127 if any PID cannot be signalled
     */
    private static int doKill(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        if (args.isEmpty()) {
            code = 1;
        } else if ("-l".equals(args.get(0))) {
            self.outStream().println("HUP INT QUIT KILL TERM");
        } else {
            boolean forcibly = false;
            int idx = 0;
            if ("-s".equals(args.get(0)) && args.size() >= 2) {
                forcibly = "KILL".equals(args.get(1)) || "9".equals(args.get(1));
                idx = 2;
            } else if (args.get(0).startsWith("-") && args.get(0).length() > 1) {
                final String sig = args.get(0).substring(1);
                forcibly = "KILL".equals(sig) || "9".equals(sig);
                idx = 1;
            }
            if (idx >= args.size()) {
                code = 1;
            }
            while (idx < args.size() && code == 0) {
                code = signalOne(
                    self.expandVariables(args.get(idx), env), forcibly
                );
                idx += 1;
            }
        }
        return code;
    }

    /**
     * Helper for {@link #doKill}: send a signal to a single PID.
     * @param pidStr   Decimal PID
     * @param forcibly true for SIGKILL, false for SIGTERM
     * @return 0 on success, 127 if no such process or non-numeric PID
     */
    private static int signalOne(final String pidStr, final boolean forcibly) {
        int code;
        try {
            final long pid = Long.parseLong(pidStr);
            final java.util.Optional<ProcessHandle> handle = ProcessHandle.of(pid);
            if (handle.isPresent()) {
                final boolean sent;
                if (forcibly) {
                    sent = handle.get().destroyForcibly();
                } else {
                    sent = handle.get().destroy();
                }
                code = sent ? 0 : 127;
            } else {
                code = 127;
            }
        } catch (final NumberFormatException ex) {
            code = 127;
        }
        return code;
    }

    /**
     * Implementation of {@code hash [-r] [name...]}.
     * POSIX 2.14. sh4j has no command-lookup cache, so:
     *   - {@code hash} with no args prints nothing and returns 0
     *     (cache is empty by definition)
     *   - {@code hash -r} returns 0 (no-op)
     *   - {@code hash NAME ...} verifies each NAME resolves on PATH;
     *     returns 0 if all do, 127 if any cannot be found
     * @param self    Builtin context
     * @param args    Args after the command name
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 127 when a named utility is not found
     */
    private static int doHash(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        for (final String arg : args) {
            if ("-r".equals(arg)) {
                continue;
            }
            final String name = self.expandVariables(arg, env);
            if (!Executor.isBuiltin(name) && self.resolveCommand(name) == null) {
                code = 127;
            }
        }
        return code;
    }

    /**
     * Implementation of {@code wait [PID...]}.
     * POSIX 2.14: without args, waits for all background jobs (exit 0).
     * With PID, waits for that bg job if known (exit 0), else returns 127
     * (POSIX "unknown process" status).
     * @param self    Builtin context
     * @param args    Args after the command name (PIDs)
     * @param env     Environment for word expansion
     * @param assigns Inline assignments (unused)
     * @return 0 on success, 127 when a named PID is not a known bg job
     */
    private static int doWait(
        final BuiltinContext self,
        final List<String> args,
        final Map<String, String> env,
        final List<String> assigns
    ) {
        int code = 0;
        if (args.isEmpty()) {
            self.waitForBackgroundJobs();
        } else {
            final int known = self.lastBackgroundPid();
            for (final String arg : args) {
                final String expanded = self.expandVariables(arg, env);
                try {
                    final int pid = Integer.parseInt(expanded);
                    if (pid == known && known > 0) {
                        self.waitForBackgroundJobs();
                    } else {
                        code = 127;
                    }
                } catch (final NumberFormatException ex) {
                    code = 127;
                }
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
